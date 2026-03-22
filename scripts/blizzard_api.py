"""
Shared Blizzard API helpers for ladder fetch scripts.

Provides OAuth authentication, HTTP helpers, and common API calls.
Credentials are read from environment variables or secrets.properties.
"""

import json
import os
import urllib.request
import urllib.parse
import base64
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_BASE = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "ladder"
SECRETS_FILE = REPO_ROOT / "secrets.properties"

OAUTH_HOST = "https://oauth.battle.net"

API_HOSTS = {
    "us": "https://us.api.blizzard.com",
    "eu": "https://eu.api.blizzard.com",
    "kr": "https://kr.api.blizzard.com",
    "tw": "https://tw.api.blizzard.com",
}

# Legacy talent tree names → normalized spec ID slugs
SPEC_NAME_ALIASES = {
    "druid_feralcombat": "druid_feral",
}

BLIZZARD_CLASS_MAP = {
    1: "warrior", 2: "paladin", 3: "hunter", 4: "rogue",
    5: "priest", 6: "deathknight", 7: "shaman", 8: "mage",
    9: "warlock", 10: "monk", 11: "druid", 12: "demonhunter",
    13: "evoker",
}


def load_secrets_properties():
    """Load key=value pairs from secrets.properties into env vars (if not already set)."""
    if not SECRETS_FILE.exists():
        return
    for line in SECRETS_FILE.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, _, value = line.partition("=")
        key, value = key.strip(), value.strip()
        if key and value and key not in os.environ:
            os.environ[key] = value


def http_get_json(url, token=None):
    """Perform a GET request and return parsed JSON."""
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def get_access_token() -> str:
    """Obtain an OAuth access token using client credentials flow."""
    load_secrets_properties()
    client_id = os.environ.get("BLIZZARD_CLIENT_ID", "")
    client_secret = os.environ.get("BLIZZARD_CLIENT_SECRET", "")
    if not client_id or not client_secret:
        raise RuntimeError("BLIZZARD_CLIENT_ID and BLIZZARD_CLIENT_SECRET must be set.")
    credentials = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
    encoded = urllib.parse.urlencode({"grant_type": "client_credentials"}).encode()
    req = urllib.request.Request(
        f"{OAUTH_HOST}/token", data=encoded,
        headers={"Authorization": f"Basic {credentials}"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())["access_token"]


def get_current_season_id(api_host: str, namespace: str, token: str) -> int:
    url = f"{api_host}/data/wow/pvp-season/index?namespace={namespace}&locale=en_US"
    return http_get_json(url, token)["current_season"]["id"]


def get_pvp_rewards(api_host: str, namespace: str, season_id: int, bracket_filter: str, token: str) -> dict:
    """Fetch PvP reward cutoffs for a season, filtered to a specific bracket type."""
    url = f"{api_host}/data/wow/pvp-season/{season_id}/pvp-reward/index?namespace={namespace}&locale=en_US"
    try:
        data = http_get_json(url, token)
    except Exception:
        return {}

    bracket_type_map = {"2v2": "ARENA_2v2", "3v3": "ARENA_3v3", "5v5": "ARENA_5v5"}
    target_type = bracket_type_map.get(bracket_filter, "")

    cutoffs = {}
    for reward in data.get("rewards", []):
        if target_type and reward.get("bracket", {}).get("type", "") != target_type:
            continue
        rating_cutoff = reward.get("rating_cutoff", 0)
        name = reward.get("achievement", {}).get("name", "").lower()
        for title in ["gladiator", "duelist", "rival", "challenger", "combatant"]:
            if title in name:
                if title not in cutoffs or rating_cutoff > cutoffs[title]:
                    cutoffs[title] = rating_cutoff
                break

    return cutoffs


def get_leaderboard(api_host: str, namespace: str, season_id: int, bracket: str, token: str) -> list:
    url = f"{api_host}/data/wow/pvp-season/{season_id}/pvp-leaderboard/{bracket}?namespace={namespace}&locale=en_US"
    return http_get_json(url, token).get("entries", [])


def get_leaderboard_index(api_host: str, namespace: str, season_id: int, token: str) -> list:
    url = f"{api_host}/data/wow/pvp-season/{season_id}/pvp-leaderboard/index?namespace={namespace}&locale=en_US"
    return [lb.get("name", "") for lb in http_get_json(url, token).get("leaderboards", [])]


def resolve_character_profile(
    api_host: str, profile_namespace: str, realm_slug: str, char_name: str, token: str
) -> dict | None:
    """
    Fetch all character detail in a single profile call.
    Returns a dict with class, spec, race, guild, faction, or None on failure.

    For TBC: spec is resolved from the specializations endpoint (extra call).
    For retail: active_spec is on the profile directly (no extra call).
    """
    name_lower = char_name.lower()
    base_url = f"{api_host}/profile/wow/character/{realm_slug}/{urllib.parse.quote(name_lower)}"
    try:
        profile = http_get_json(f"{base_url}?namespace={profile_namespace}&locale=en_US", token)
    except Exception:
        return None

    class_id_num = profile.get("character_class", {}).get("id")
    class_slug = BLIZZARD_CLASS_MAP.get(class_id_num) if class_id_num else None
    if not class_slug:
        return None

    result = {
        "classId": class_slug,
        "className": profile.get("character_class", {}).get("name"),
        "race": profile.get("race", {}).get("name"),
        "guild": profile.get("guild", {}).get("name"),
        "faction": profile.get("faction", {}).get("type"),
        "level": profile.get("level"),
        "specId": None,
    }

    # Retail: active_spec is inline
    active_spec = profile.get("active_spec")
    if active_spec:
        spec_name = active_spec.get("name", "").lower().replace(" ", "")
        result["specId"] = SPEC_NAME_ALIASES.get(f"{class_slug}_{spec_name}", f"{class_slug}_{spec_name}")
    else:
        # Classic/TBC: fetch specializations endpoint
        try:
            spec_data = http_get_json(f"{base_url}/specializations?namespace={profile_namespace}&locale=en_US", token)
            for group in spec_data.get("specialization_groups", []):
                if group.get("is_active"):
                    best_name, best_pts = None, -1
                    for spec in group.get("specializations", []):
                        pts = spec.get("spent_points", 0)
                        if pts > best_pts:
                            best_pts = pts
                            name = spec.get("specialization_name", "").lower().replace(" ", "")
                            if name:
                                best_name = name
                    if best_name:
                        slug = f"{class_slug}_{best_name}"
                        result["specId"] = SPEC_NAME_ALIASES.get(slug, slug)
                    break
        except Exception:
            pass

    return result


def fetch_full_player_profile(
    api_host: str, profile_namespace: str, realm_slug: str, char_name: str,
    token: str, brackets: list[str] = ("2v2", "3v3", "5v5")
) -> dict | None:
    """
    Fetch complete player profile: base profile + equipment + specializations + pvp brackets.
    Returns a full player dict or None on failure.
    Calls: profile (1) + equipment (1) + specializations (1) + brackets (2-3) = 5-6 total.
    """
    name_lower = char_name.lower()
    base_url = f"{api_host}/profile/wow/character/{realm_slug}/{urllib.parse.quote(name_lower)}"
    ns_param = f"namespace={profile_namespace}&locale=en_US"

    # 1. Base profile
    try:
        profile = http_get_json(f"{base_url}?{ns_param}", token)
    except Exception:
        return None

    class_id_num = profile.get("character_class", {}).get("id")
    class_slug = BLIZZARD_CLASS_MAP.get(class_id_num) if class_id_num else None
    if not class_slug:
        return None

    result = {
        "name": profile.get("name", char_name),
        "realmSlug": realm_slug,
        "classId": class_slug,
        "specId": None,
        "race": profile.get("race", {}).get("name"),
        "guild": profile.get("guild", {}).get("name"),
        "faction": profile.get("faction", {}).get("type"),
        "level": profile.get("level"),
        "equipment": [],
        "talentGroups": [],
        "pvpBrackets": {},
    }

    # Resolve spec from active_spec (retail) or specializations (classic)
    active_spec = profile.get("active_spec")
    if active_spec:
        spec_name = active_spec.get("name", "").lower().replace(" ", "")
        slug = f"{class_slug}_{spec_name}"
        result["specId"] = SPEC_NAME_ALIASES.get(slug, slug)

    # 2. Equipment
    try:
        equip = http_get_json(f"{base_url}/equipment?{ns_param}", token)
        for item in equip.get("equipped_items", []):
            slot = item.get("slot", {}).get("type", "UNKNOWN")
            enchants = item.get("enchantments", [])
            enchant_name = None
            gems = []
            for e in enchants:
                slot_type = e.get("enchantment_slot", {}).get("type")
                source = e.get("source_item", {}).get("name")
                if slot_type == "PERMANENT" and source:
                    enchant_name = source
                elif source and not slot_type:
                    gems.append(source)

            result["equipment"].append({
                "slot": slot,
                "itemId": item.get("item", {}).get("id", 0),
                "name": item.get("name", "Unknown"),
                "quality": item.get("quality", {}).get("type"),
                "enchant": enchant_name,
                "gems": gems,
            })

            # Collect full item tooltip data for dedup storage
            item_id = item.get("item", {}).get("id", 0)
            if item_id and "_items" not in result:
                result["_items"] = {}
            if item_id and item_id not in result.get("_items", {}):
                weapon_data = item.get("weapon")
                result.setdefault("_items", {})[item_id] = {
                    "itemId": item_id,
                    "name": item.get("name", "Unknown"),
                    "quality": item.get("quality", {}).get("type"),
                    "slotName": item.get("slot", {}).get("name"),
                    "itemSubclass": item.get("item_subclass", {}).get("name"),
                    "binding": item.get("binding", {}).get("name"),
                    "armor": item.get("armor", {}).get("value") if item.get("armor") else None,
                    "stats": [
                        s.get("display", {}).get("display_string", "")
                        for s in item.get("stats", [])
                    ],
                    "spells": [
                        s.get("description", "")
                        for s in item.get("spells", [])
                    ],
                    "weaponDamage": weapon_data.get("damage", {}).get("display_string") if weapon_data else None,
                    "weaponSpeed": weapon_data.get("attack_speed", {}).get("display_string") if weapon_data else None,
                    "weaponDps": weapon_data.get("dps", {}).get("display_string") if weapon_data else None,
                    "setName": item.get("set", {}).get("item_set", {}).get("name") if item.get("set") else None,
                    "setEffects": [
                        e.get("display_string", "")
                        for e in item.get("set", {}).get("effects", [])
                    ] if item.get("set") else [],
                    "requiredLevel": item.get("requirements", {}).get("level", {}).get("display_string"),
                    "requiredClasses": item.get("requirements", {}).get("playable_classes", {}).get("display_string"),
                }
    except Exception:
        pass

    # 3. Specializations (talents)
    try:
        spec_data = http_get_json(f"{base_url}/specializations?{ns_param}", token)
        for group in spec_data.get("specialization_groups", []):
            talent_group = {
                "isActive": group.get("is_active", False),
                "specializations": [],
            }
            for spec in group.get("specializations", []):
                tree_name = spec.get("specialization_name", "Unknown")
                spent = spec.get("spent_points", 0)
                talent_group["specializations"].append({
                    "treeName": tree_name,
                    "spentPoints": spent,
                })

                # Resolve specId from active group
                if talent_group["isActive"] and result["specId"] is None:
                    # Best tree = most points
                    current_best = max(
                        talent_group["specializations"],
                        key=lambda s: s["spentPoints"]
                    )
                    if current_best["spentPoints"] > 0:
                        slug = f"{class_slug}_{current_best['treeName'].lower().replace(' ', '')}"
                        result["specId"] = SPEC_NAME_ALIASES.get(slug, slug)

            result["talentGroups"].append(talent_group)
    except Exception:
        pass

    # 4. PvP brackets
    for bracket in brackets:
        try:
            data = http_get_json(f"{base_url}/pvp-bracket/{bracket}?{ns_param}", token)
            stats = data.get("season_match_statistics", {})
            result["pvpBrackets"][bracket] = {
                "rating": data.get("rating", 0),
                "wins": stats.get("won", 0),
                "losses": stats.get("lost", 0),
            }
        except Exception:
            pass  # Player doesn't have this bracket

    return result


def resolve_item_icons(items: dict) -> None:
    """
    Resolve icon names for items using the Wowhead tooltip API.
    No auth needed, no Blizzard rate limit impact.
    Modifies items dict in place (adds 'icon' field, removes '_media_href').
    """
    to_resolve = [(k, v) for k, v in items.items() if not v.get("icon")]
    if not to_resolve:
        return

    resolved = 0
    for i, (item_id_str, item_data) in enumerate(to_resolve):
        item_data.pop("_media_href", None)
        try:
            url = f"https://nether.wowhead.com/tooltip/item/{item_id_str}"
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                icon = data.get("icon")
                if icon:
                    item_data["icon"] = icon
                    resolved += 1
        except Exception:
            pass
        if (i + 1) % 100 == 0:
            print(f"    ... {i + 1}/{len(to_resolve)} icons resolved")

    # Clean up temp fields
    for v in items.values():
        v.pop("_media_href", None)

    print(f"    Resolved {resolved}/{len(to_resolve)} item icons")


def write_index(addon_dir: Path):
    """Write an index.json listing all snapshot files in the addon directory."""
    index = []
    for f in sorted(addon_dir.glob("*_*.json")):
        if f.name == "index.json":
            continue
        parts = f.stem.split("_", 1)
        if len(parts) == 2:
            index.append({"region": parts[0], "bracket": parts[1], "file": f.name})

    with open(addon_dir / "index.json", "w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)

    return index

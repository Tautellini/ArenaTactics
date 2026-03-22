#!/usr/bin/env python3
"""
Fetch PvP leaderboard data from the Blizzard API and write static JSON files.

Usage:
    python scripts/fetch_ladder.py

Credentials (checked in order):
    1. Environment variables: BLIZZARD_CLIENT_ID, BLIZZARD_CLIENT_SECRET
    2. secrets.properties file in project root (key=value, gitignored)

Output:
    composeApp/src/commonMain/composeResources/files/ladder/{addonId}/index.json
    composeApp/src/commonMain/composeResources/files/ladder/{addonId}/{region}_{bracket}.json
"""

import json
import os
import sys
import urllib.request
import urllib.parse
import base64
from datetime import datetime, timezone
from pathlib import Path

# ── Configuration ────────────────────────────────────────────────────────────

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_BASE = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "ladder"
SECRETS_FILE = REPO_ROOT / "secrets.properties"

OAUTH_HOST = "https://oauth.battle.net"

# How many top entries to store per bracket (with class data resolved)
TOP_ENTRIES_LIMIT = 500

# Blizzard API base URLs per region
API_HOSTS = {
    "us": "https://us.api.blizzard.com",
    "eu": "https://eu.api.blizzard.com",
    "kr": "https://kr.api.blizzard.com",
    "tw": "https://tw.api.blizzard.com",
}

# ── Addon definitions ────────────────────────────────────────────────────────

ADDONS = [
    {
        "id": "tbc_anniversary",
        "namespace_prefix": "dynamic-classicann",
        "profile_namespace_prefix": "profile-classicann",
        "regions": ["us", "eu"],
        "brackets": ["2v2", "3v3", "5v5"],
        "has_per_spec_shuffle": False,
    },
    {
        "id": "midnight",
        "namespace_prefix": "dynamic",
        "profile_namespace_prefix": "profile",
        "regions": ["us", "eu"],
        "brackets": ["2v2", "3v3"],
        "has_per_spec_shuffle": True,
    },
]

# Blizzard class ID → our class ID slug
BLIZZARD_CLASS_MAP = {
    1: "warrior", 2: "paladin", 3: "hunter", 4: "rogue",
    5: "priest", 6: "deathknight", 7: "shaman", 8: "mage",
    9: "warlock", 10: "monk", 11: "druid", 12: "demonhunter",
    13: "evoker",
}

# Blizzard shuffle bracket name → our spec ID mapping (retail only)
SHUFFLE_SPEC_MAP = {
    "deathknight-blood": "deathknight_blood", "deathknight-frost": "deathknight_frost",
    "deathknight-unholy": "deathknight_unholy", "demonhunter-havoc": "demonhunter_havoc",
    "demonhunter-vengeance": "demonhunter_vengeance", "demonhunter-devourer": "demonhunter_havoc",
    "druid-balance": "druid_balance", "druid-feral": "druid_feral",
    "druid-guardian": "druid_guardian", "druid-restoration": "druid_restoration",
    "evoker-augmentation": "evoker_augmentation", "evoker-devastation": "evoker_devastation",
    "evoker-preservation": "evoker_preservation",
    "hunter-beastmastery": "hunter_beastmastery", "hunter-marksmanship": "hunter_marksmanship",
    "hunter-survival": "hunter_survival",
    "mage-arcane": "mage_arcane", "mage-fire": "mage_fire", "mage-frost": "mage_frost",
    "monk-brewmaster": "monk_brewmaster", "monk-mistweaver": "monk_mistweaver",
    "monk-windwalker": "monk_windwalker",
    "paladin-holy": "paladin_holy", "paladin-protection": "paladin_protection",
    "paladin-retribution": "paladin_retribution",
    "priest-discipline": "priest_discipline", "priest-holy": "priest_holy",
    "priest-shadow": "priest_shadow",
    "rogue-assassination": "rogue_assassination", "rogue-outlaw": "rogue_outlaw",
    "rogue-subtlety": "rogue_subtlety",
    "shaman-elemental": "shaman_elemental", "shaman-enhancement": "shaman_enhancement",
    "shaman-restoration": "shaman_restoration",
    "warlock-affliction": "warlock_affliction", "warlock-demonology": "warlock_demonology",
    "warlock-destruction": "warlock_destruction",
    "warrior-arms": "warrior_arms", "warrior-fury": "warrior_fury",
    "warrior-protection": "warrior_protection",
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


# ── HTTP helpers ─────────────────────────────────────────────────────────────

def http_get_json(url, headers=None, token=None):
    """Perform a GET request and return parsed JSON."""
    h = dict(headers or {})
    if token:
        h["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=h)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def http_post_form(url, data, headers=None):
    """Perform a POST with form-encoded data and return parsed JSON."""
    encoded = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(url, data=encoded, headers=headers or {}, method="POST")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


# ── Blizzard API ─────────────────────────────────────────────────────────────

def get_access_token(client_id: str, client_secret: str) -> str:
    """Obtain an OAuth access token using client credentials flow."""
    credentials = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
    result = http_post_form(
        f"{OAUTH_HOST}/token",
        data={"grant_type": "client_credentials"},
        headers={"Authorization": f"Basic {credentials}"}
    )
    return result["access_token"]


def get_current_season_id(api_host: str, namespace: str, token: str) -> int:
    """Fetch the current PvP season ID."""
    url = f"{api_host}/data/wow/pvp-season/index?namespace={namespace}&locale=en_US"
    data = http_get_json(url, token=token)
    return data["current_season"]["id"]


def get_pvp_rewards(api_host: str, namespace: str, season_id: int, bracket_filter: str, token: str) -> dict:
    """Fetch PvP reward cutoffs for a season, filtered to a specific bracket type."""
    url = (
        f"{api_host}/data/wow/pvp-season/{season_id}/pvp-reward/index"
        f"?namespace={namespace}&locale=en_US"
    )
    try:
        data = http_get_json(url, token=token)
    except Exception:
        return {}

    bracket_type_map = {"2v2": "ARENA_2v2", "3v3": "ARENA_3v3", "5v5": "ARENA_5v5"}
    target_type = bracket_type_map.get(bracket_filter, "")

    cutoffs = {}
    for reward in data.get("rewards", []):
        bracket_info = reward.get("bracket", {})
        reward_bracket_type = bracket_info.get("type", "")
        if target_type and reward_bracket_type != target_type:
            continue

        rating_cutoff = reward.get("rating_cutoff", 0)
        achievement = reward.get("achievement", {})
        name = achievement.get("name", "").lower()

        for title in ["gladiator", "duelist", "rival", "challenger", "combatant"]:
            if title in name:
                if title not in cutoffs or rating_cutoff > cutoffs[title]:
                    cutoffs[title] = rating_cutoff
                break

    return cutoffs


def get_leaderboard(api_host: str, namespace: str, season_id: int, bracket: str, token: str) -> list:
    """Fetch the PvP leaderboard for a bracket. Returns raw entries list."""
    url = (
        f"{api_host}/data/wow/pvp-season/{season_id}/pvp-leaderboard/{bracket}"
        f"?namespace={namespace}&locale=en_US"
    )
    data = http_get_json(url, token=token)
    return data.get("entries", [])


def get_leaderboard_index(api_host: str, namespace: str, season_id: int, token: str) -> list:
    """Fetch the list of available leaderboard brackets for a season."""
    url = (
        f"{api_host}/data/wow/pvp-season/{season_id}/pvp-leaderboard/index"
        f"?namespace={namespace}&locale=en_US"
    )
    data = http_get_json(url, token=token)
    return [lb.get("name", "") for lb in data.get("leaderboards", [])]


def get_character_class(api_host: str, profile_namespace: str, realm_slug: str, char_name: str, token: str) -> str | None:
    """Fetch a character's class from the profile API. Returns our class ID slug or None."""
    name_lower = char_name.lower()
    url = (
        f"{api_host}/profile/wow/character/{realm_slug}/{urllib.parse.quote(name_lower)}"
        f"?namespace={profile_namespace}&locale=en_US"
    )
    try:
        data = http_get_json(url, token=token)
        class_id = data.get("character_class", {}).get("id")
        if class_id and class_id in BLIZZARD_CLASS_MAP:
            return BLIZZARD_CLASS_MAP[class_id]
    except Exception:
        pass
    return None


def fetch_spec_distribution(api_host: str, namespace: str, season_id: int, token: str) -> list:
    """
    Derive spec distribution by counting entries in per-spec shuffle leaderboards.
    Only works for retail (Classic has no per-spec shuffle brackets).
    """
    all_brackets = get_leaderboard_index(api_host, namespace, season_id, token)
    spec_brackets = [b for b in all_brackets if b.startswith("shuffle-") and b != "shuffle-overall"]

    if not spec_brackets:
        return []

    spec_counts = {}
    total = 0

    for bracket_name in spec_brackets:
        spec_part = bracket_name.removeprefix("shuffle-")
        spec_id = SHUFFLE_SPEC_MAP.get(spec_part)
        if not spec_id:
            continue

        try:
            entries = get_leaderboard(api_host, namespace, season_id, bracket_name, token)
            count = len(entries)
            spec_counts[spec_id] = spec_counts.get(spec_id, 0) + count
            total += count
        except Exception as e:
            print(f"    Warning: failed to fetch {bracket_name}: {e}")

    distribution = []
    for spec_id, count in sorted(spec_counts.items(), key=lambda x: -x[1]):
        distribution.append({
            "specId": spec_id,
            "count": count,
            "percentage": round(count / total * 100, 2) if total > 0 else 0.0,
        })

    return distribution


def resolve_classes_for_entries(
    entries: list,
    api_host: str,
    profile_namespace: str,
    token: str,
    class_cache: dict,
    limit: int = TOP_ENTRIES_LIMIT,
) -> list:
    """
    Resolve character classes for the top N leaderboard entries.
    Uses class_cache (keyed by character ID) for deduplication across brackets.
    Returns enriched entry dicts with classId set.
    """
    result = []
    lookups = 0
    cache_hits = 0

    for entry in entries[:limit]:
        character = entry.get("character", {})
        realm = entry.get("realm") or character.get("realm", {})
        stats = entry.get("season_match_statistics", {})
        char_id = character.get("id")
        char_name = character.get("name", "Unknown")
        realm_slug = realm.get("slug", "")

        class_id = None
        if char_id and char_id in class_cache:
            class_id = class_cache[char_id]
            cache_hits += 1
        elif char_id and realm_slug and char_name != "Unknown":
            class_id = get_character_class(api_host, profile_namespace, realm_slug, char_name, token)
            if class_id:
                class_cache[char_id] = class_id
            lookups += 1

        result.append({
            "rank": entry.get("rank", 0),
            "characterName": char_name,
            "realmSlug": realm_slug,
            "rating": entry.get("rating", 0),
            "wins": stats.get("won", 0),
            "losses": stats.get("lost", 0),
            "classId": class_id,
        })

    print(f"    Resolved {lookups} profiles ({cache_hits} cache hits)")
    return result


def build_snapshot(
    region: str, bracket: str, season_id: int,
    total_entries: int, top_entries: list,
    cutoffs: dict, spec_distribution: list | None = None
) -> dict:
    """Build a ladder snapshot dict."""
    return {
        "region": region,
        "bracket": bracket,
        "seasonId": season_id,
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "totalEntries": total_entries,
        "ratingCutoffs": cutoffs,
        "specDistribution": spec_distribution or [],
        "topEntries": top_entries,
    }


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    load_secrets_properties()

    client_id = os.environ.get("BLIZZARD_CLIENT_ID", "")
    client_secret = os.environ.get("BLIZZARD_CLIENT_SECRET", "")

    if not client_id or not client_secret:
        print("ERROR: BLIZZARD_CLIENT_ID and BLIZZARD_CLIENT_SECRET must be set.", file=sys.stderr)
        sys.exit(1)

    print("Authenticating with Battle.net...")
    token = get_access_token(client_id, client_secret)
    print("Authenticated successfully.\n")

    for addon in ADDONS:
        addon_id = addon["id"]
        ns_prefix = addon["namespace_prefix"]
        profile_ns_prefix = addon["profile_namespace_prefix"]
        regions = addon["regions"]
        brackets = addon["brackets"]
        has_shuffle = addon["has_per_spec_shuffle"]

        addon_dir = OUTPUT_BASE / addon_id
        addon_dir.mkdir(parents=True, exist_ok=True)

        print(f"=== {addon_id} (namespace: {ns_prefix}) ===")

        for region in regions:
            region = region.strip()
            if region not in API_HOSTS:
                print(f"  [{region}] WARNING: Unknown region, skipping.")
                continue

            api_host = API_HOSTS[region]
            namespace = f"{ns_prefix}-{region}"
            profile_namespace = f"{profile_ns_prefix}-{region}"

            # Class cache shared across brackets within this region
            # Key: character ID (int), Value: class slug (str)
            class_cache: dict[int, str] = {}

            # Determine season
            print(f"  [{region}] Detecting current season...")
            try:
                season_id = get_current_season_id(api_host, namespace, token)
            except Exception as e:
                print(f"  [{region}] ERROR detecting season: {e}")
                continue

            print(f"  [{region}] Season ID: {season_id}")

            # Fetch reward cutoffs
            print(f"  [{region}] Fetching PvP reward cutoffs...")

            # Fetch spec distribution (retail only)
            spec_distribution = []
            if has_shuffle:
                print(f"  [{region}] Fetching spec distribution from shuffle leaderboards...")
                try:
                    spec_distribution = fetch_spec_distribution(api_host, namespace, season_id, token)
                    print(f"  [{region}] Spec distribution: {len(spec_distribution)} specs tracked")
                except Exception as e:
                    print(f"  [{region}] WARNING: Failed to fetch spec distribution: {e}")

            for bracket in brackets:
                bracket = bracket.strip()

                cutoffs = get_pvp_rewards(api_host, namespace, season_id, bracket, token)
                print(f"  [{region}] {bracket} cutoffs: {cutoffs}")

                print(f"  [{region}] Fetching {bracket} leaderboard...")
                try:
                    raw_entries = get_leaderboard(api_host, namespace, season_id, bracket, token)
                except Exception as e:
                    print(f"  [{region}] ERROR fetching {bracket}: {e}")
                    continue

                total = len(raw_entries)
                print(f"  [{region}] {bracket}: {total} entries, resolving top {TOP_ENTRIES_LIMIT} classes...")

                top_entries = resolve_classes_for_entries(
                    raw_entries, api_host, profile_namespace, token, class_cache
                )

                snapshot = build_snapshot(
                    region, bracket, season_id, total, top_entries, cutoffs, spec_distribution
                )

                out_file = addon_dir / f"{region}_{bracket}.json"
                with open(out_file, "w", encoding="utf-8") as f:
                    json.dump(snapshot, f, indent=2, ensure_ascii=False)

                print(f"  [{region}] Wrote {out_file.name}")

            print(f"  [{region}] Class cache: {len(class_cache)} unique characters resolved")

        # Write index for this addon
        index = []
        for f in sorted(addon_dir.glob("*_*.json")):
            if f.name == "index.json":
                continue
            parts = f.stem.split("_", 1)
            if len(parts) == 2:
                index.append({"region": parts[0], "bracket": parts[1], "file": f.name})

        index_file = addon_dir / "index.json"
        with open(index_file, "w", encoding="utf-8") as f:
            json.dump(index, f, indent=2, ensure_ascii=False)

        print(f"  Wrote {len(index)} snapshot(s) + index.json\n")

    print("Done.")


if __name__ == "__main__":
    main()

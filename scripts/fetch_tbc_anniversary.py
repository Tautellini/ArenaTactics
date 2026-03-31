#!/usr/bin/env python3
"""
Fetch TBC Anniversary PvP ladder data from the Blizzard API
and push it to the Arena Tactics backend via admin API.

Usage:
    python scripts/fetch_tbc_anniversary.py

Credentials:
    BLIZZARD_CLIENT_ID / BLIZZARD_CLIENT_SECRET — Blizzard API
    ADMIN_API_KEY — Arena Tactics admin endpoint auth
    All can be set in secrets.properties or as env vars.

Output: Data is pushed directly to Firestore via the backend admin API.
"""

import json
import os
import time
import urllib.request
import urllib.parse
from collections import Counter
from datetime import datetime, timezone

from blizzard_api import (
    API_HOSTS, log_progress, load_secrets_properties,
    get_access_token, get_current_season_id, get_pvp_rewards,
    get_leaderboard, fetch_full_player_profile, resolve_item_icons,
)

ADDON_ID = "tbc_anniversary"
NAMESPACE_PREFIX = "dynamic-classicann"
PROFILE_NAMESPACE_PREFIX = "profile-classicann"
REGIONS = ["us", "eu"]
BRACKETS = ["2v2", "3v3", "5v5"]
TOP_ENTRIES_LIMIT = 500

# Backend admin API
BACKEND_URL = "https://backend-532845578761.europe-west3.run.app"
ADMIN_BASE = f"{BACKEND_URL}/api/v1/admin/arena-tactics"
API_BASE = f"{BACKEND_URL}/api/v1/arena-tactics"


def get_admin_key():
    load_secrets_properties()
    key = os.environ.get("ADMIN_API_KEY", "")
    if not key:
        raise RuntimeError("ADMIN_API_KEY must be set in env or secrets.properties")
    return key


def admin_put(endpoint, data, admin_key):
    """PUT JSON data to an admin endpoint."""
    url = f"{ADMIN_BASE}/{endpoint}"
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="PUT", headers={
        "Authorization": f"Bearer {admin_key}",
        "Content-Type": "application/json",
    })
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        print(f"    WARN: admin PUT {endpoint} → {e.code}: {e.read().decode()[:200]}")
        return None


def api_get(endpoint):
    """GET from the public read API (no auth needed)."""
    url = f"{API_BASE}/{endpoint}"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None


def get_existing_snapshot(region, bracket):
    """Fetch the current snapshot from our backend to compare against."""
    return api_get(f"ladder/{ADDON_ID}/snapshots/{region}/{bracket}")


def build_player_rating_map(snapshot):
    """Build a map of characterId → (rating, wins, losses) from an existing snapshot."""
    if not snapshot or "topEntries" not in snapshot:
        return {}
    return {
        entry["characterId"]: (entry["rating"], entry.get("wins", 0), entry.get("losses", 0))
        for entry in snapshot["topEntries"]
        if entry.get("characterId")
    }


def compute_spec_distribution(entries):
    """Compute spec distribution from leaderboard entries."""
    spec_counts = Counter()
    for entry in entries:
        spec_id = entry.get("specId")
        if spec_id:
            spec_counts[spec_id] += 1
    total = sum(spec_counts.values()) or 1
    return [
        {
            "specId": spec_id,
            "count": count,
            "percentage": round(count * 100.0 / total, 1),
        }
        for spec_id, count in spec_counts.most_common()
    ]


def compute_spec_meta(spec_id, players):
    """Compute aggregated SpecMeta for a single spec from player profiles."""
    matching = [p for p in players if p.get("specId") == spec_id]
    total = len(matching)
    if total == 0:
        return None

    SLOT_ORDER = [
        "HEAD", "NECK", "SHOULDER", "BACK", "CHEST", "WRIST",
        "HANDS", "WAIST", "LEGS", "FEET",
        "FINGER_1", "FINGER_2", "TRINKET_1", "TRINKET_2",
        "MAIN_HAND", "OFF_HAND", "RANGED",
    ]

    # Slot breakdowns
    slot_items = {}
    slot_qualities = {}
    slot_enchants = {}

    for player in matching:
        for item in player.get("equipment", []):
            slot = item.get("slot", "UNKNOWN")
            item_id = item.get("itemId", 0)
            name = item.get("name", "Unknown")
            slot_items.setdefault(slot, []).append((item_id, name))
            slot_qualities[(slot, item_id)] = item.get("quality")
            enchant = item.get("enchant")
            if enchant:
                slot_enchants.setdefault(slot, []).append(enchant)

    slot_breakdowns = []
    for slot in SLOT_ORDER:
        items = slot_items.get(slot)
        if not items:
            continue
        item_counts = Counter(items)
        top_items = [
            {
                "itemId": pair[0],
                "name": pair[1],
                "quality": slot_qualities.get((slot, pair[0])),
                "count": count,
                "percentage": round(count * 100.0 / total, 1),
            }
            for pair, count in item_counts.most_common(10)
        ]
        enchants_list = slot_enchants.get(slot, [])
        enchant_counts = Counter(enchants_list)
        top_enchants = [
            {"name": name, "count": count, "percentage": round(count * 100.0 / total, 1)}
            for name, count in enchant_counts.most_common(5)
        ]
        slot_breakdowns.append({"slot": slot, "items": top_items, "enchants": top_enchants})

    # Talent builds
    build_counts = {}
    for player in matching:
        for group in player.get("talentGroups", []):
            if not group.get("isActive"):
                continue
            trees = [
                {"treeName": s["treeName"], "spentPoints": s["spentPoints"]}
                for s in group.get("specializations", [])
            ]
            label = " / ".join(f"{t['treeName']} ({t['spentPoints']})" for t in trees)
            if label not in build_counts:
                talents = []
                for s in group.get("specializations", []):
                    talents.extend(s.get("talents", []))
                build_counts[label] = {"trees": trees, "count": 1, "talents": talents}
            else:
                build_counts[label]["count"] += 1
            break

    talent_builds = sorted(build_counts.values(), key=lambda x: -x["count"])[:5]
    popular_builds = [
        {
            "label": " / ".join(f"{t['treeName']} ({t['spentPoints']})" for t in b["trees"]),
            "trees": b["trees"],
            "count": b["count"],
            "percentage": round(b["count"] * 100.0 / total, 1),
            "talentSelections": b["talents"],
        }
        for b in talent_builds
    ]

    return {
        "specId": spec_id,
        "sampleSize": total,
        "slotBreakdowns": slot_breakdowns,
        "popularTalentBuilds": popular_builds,
    }


def main():
    print("Authenticating with Battle.net...")
    token = get_access_token()
    admin_key = get_admin_key()
    print("Authenticated.\n")

    print(f"=== {ADDON_ID} ===")

    all_players_all_regions = []  # For SpecMeta computation across all regions

    for region in REGIONS:
        if region not in API_HOSTS:
            continue

        api_host = API_HOSTS[region]
        namespace = f"{NAMESPACE_PREFIX}-{region}"
        profile_ns = f"{PROFILE_NAMESPACE_PREFIX}-{region}"

        print(f"\n  [{region}] Detecting current season...")
        try:
            season_id = get_current_season_id(api_host, namespace, token)
        except Exception as e:
            print(f"  [{region}] ERROR: {e}")
            continue
        print(f"  [{region}] Season {season_id}")

        # ── Fetch existing snapshots from our backend for comparison ──
        existing_ratings = {}
        for bracket in BRACKETS:
            snapshot = get_existing_snapshot(region, bracket)
            existing_ratings[bracket] = build_player_rating_map(snapshot)
        total_existing = sum(len(v) for v in existing_ratings.values())
        print(f"  [{region}] Loaded {total_existing} existing player ratings for comparison")

        # ── Collect unique characters across all brackets ──
        char_brackets = {}
        char_info = {}
        bracket_totals = {}

        for bracket in BRACKETS:
            print(f"  [{region}] Fetching {bracket} leaderboard...")
            try:
                entries = get_leaderboard(api_host, namespace, season_id, bracket, token)
            except Exception as e:
                print(f"  [{region}] ERROR fetching {bracket}: {e}")
                continue

            bracket_totals[bracket] = len(entries)
            print(f"  [{region}] {bracket}: {len(entries)} entries")

            for entry in entries[:TOP_ENTRIES_LIMIT]:
                char = entry.get("character", {})
                char_id = char.get("id")
                if not char_id:
                    continue
                realm = entry.get("realm") or char.get("realm", {})
                stats = entry.get("season_match_statistics", {})

                char_info.setdefault(char_id, (char.get("name", "Unknown"), realm.get("slug", "")))
                char_brackets.setdefault(char_id, []).append({
                    "bracket": bracket,
                    "rank": entry.get("rank", 0),
                    "rating": entry.get("rating", 0),
                    "wins": stats.get("won", 0),
                    "losses": stats.get("lost", 0),
                })

        # ── Determine which players need fresh profile fetches ──
        unique_ids = list(char_info.keys())
        changed_ids = []
        unchanged_ids = []

        for char_id in unique_ids:
            is_changed = False
            for app in char_brackets.get(char_id, []):
                bracket = app["bracket"]
                existing = existing_ratings.get(bracket, {}).get(char_id)
                if existing is None:
                    is_changed = True  # New player
                    break
                old_rating, old_wins, old_losses = existing
                if app["rating"] != old_rating or app["wins"] != old_wins or app["losses"] != old_losses:
                    is_changed = True
                    break
            if is_changed:
                changed_ids.append(char_id)
            else:
                unchanged_ids.append(char_id)

        print(f"  [{region}] {len(changed_ids)} changed/new, {len(unchanged_ids)} unchanged — skipping unchanged profiles")

        # ── Fetch profiles for CHANGED players only ──
        player_profiles = {}

        # Reuse existing profiles for unchanged players
        for char_id in unchanged_ids:
            name, realm_slug = char_info[char_id]
            existing_profile = api_get(f"ladder/{ADDON_ID}/players/{region}/{char_id}")
            if existing_profile:
                player_profiles[str(char_id)] = existing_profile
            else:
                changed_ids.append(char_id)  # Couldn't fetch existing, refetch from Blizzard

        print(f"  [{region}] Fetching profiles for {len(changed_ids)} characters (3 API calls each)...")
        t0 = time.time()
        for i, char_id in enumerate(changed_ids):
            name, realm_slug = char_info[char_id]
            if name == "Unknown" or not realm_slug:
                continue

            profile = fetch_full_player_profile(
                api_host, profile_ns, realm_slug, name, token, brackets=[]
            )
            if profile:
                profile["characterId"] = char_id
                pvp = {}
                for app in char_brackets.get(char_id, []):
                    pvp[app["bracket"]] = {
                        "rating": app["rating"],
                        "wins": app["wins"],
                        "losses": app["losses"],
                        "rank": app["rank"],
                    }
                profile["pvpBrackets"] = pvp
                player_profiles[str(char_id)] = profile

            if (i + 1) % 50 == 0:
                log_progress(i, len(changed_ids), t0, f"  [{region}] Profiles: ")

        elapsed = time.time() - t0
        if changed_ids:
            print(f"  [{region}] Fetched {len([c for c in changed_ids if str(c) in player_profiles])}/{len(changed_ids)} profiles in {elapsed:.0f}s")

        # ── Collect and deduplicate items ──
        all_items = {}
        for profile in player_profiles.values():
            items = profile.pop("_items", None) or {}
            for item_id, item_data in items.items():
                all_items.setdefault(int(item_id), item_data)

        # ── Resolve item icons ──
        if all_items:
            print(f"  [{region}] Resolving item icons for {len(all_items)} unique items...")
            resolve_item_icons(all_items)

        # ── Push players to admin API ──
        print(f"  [{region}] Uploading {len(player_profiles)} players...")
        resp = admin_put(f"ladder/{ADDON_ID}/players/{region}", player_profiles, admin_key)
        if resp:
            print(f"  [{region}] Players uploaded: {resp}")

        # ── Push items to admin API ──
        if all_items:
            items_str_keys = {str(k): v for k, v in all_items.items()}
            print(f"  [{region}] Uploading {len(items_str_keys)} items...")
            resp = admin_put(f"ladder/{ADDON_ID}/items/{region}", items_str_keys, admin_key)
            if resp:
                print(f"  [{region}] Items uploaded: {resp}")

        # ── Build and push snapshots per bracket ──
        for bracket in BRACKETS:
            cutoffs = get_pvp_rewards(api_host, namespace, season_id, bracket, token)
            print(f"  [{region}] {bracket} cutoffs: {cutoffs}")

            bracket_entries = []
            for char_id, appearances in char_brackets.items():
                for app in appearances:
                    if app["bracket"] == bracket:
                        name, realm_slug = char_info[char_id]
                        profile = player_profiles.get(str(char_id), {})
                        bracket_entries.append({
                            "rank": app["rank"],
                            "characterName": name,
                            "realmSlug": realm_slug,
                            "characterId": char_id,
                            "rating": app["rating"],
                            "wins": app["wins"],
                            "losses": app["losses"],
                            "classId": profile.get("classId"),
                            "specId": profile.get("specId"),
                            "race": profile.get("race"),
                            "guild": profile.get("guild"),
                            "faction": profile.get("faction"),
                        })

            bracket_entries.sort(key=lambda e: e["rank"])

            snapshot = {
                "region": region,
                "bracket": bracket,
                "seasonId": season_id,
                "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "totalEntries": bracket_totals.get(bracket, 0),
                "ratingCutoffs": cutoffs,
                "specDistribution": compute_spec_distribution(bracket_entries),
                "topEntries": bracket_entries,
            }

            print(f"  [{region}] Uploading {bracket} snapshot ({len(bracket_entries)} entries)...")
            resp = admin_put(f"ladder/{ADDON_ID}/snapshot", snapshot, admin_key)
            if resp:
                print(f"  [{region}] Snapshot uploaded: {resp}")

        # Collect all players for SpecMeta computation
        all_players_all_regions.extend(player_profiles.values())

    # ── Compute and push SpecMeta ──
    print(f"\nComputing SpecMeta from {len(all_players_all_regions)} players...")
    spec_ids = set()
    for player in all_players_all_regions:
        if player.get("specId"):
            spec_ids.add(player["specId"])

    meta_count = 0
    for spec_id in sorted(spec_ids):
        meta = compute_spec_meta(spec_id, all_players_all_regions)
        if meta and meta["sampleSize"] > 0:
            resp = admin_put(f"spec-meta/{spec_id}", meta, admin_key)
            if resp:
                print(f"  {spec_id}: {meta['sampleSize']} players")
                meta_count += 1

    print(f"\nUploaded {meta_count} SpecMeta entries.")
    print("Done.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Fetch TBC Anniversary PvP ladder data from the Blizzard API.

Usage:
    python scripts/fetch_tbc_anniversary.py

Credentials: secrets.properties or BLIZZARD_CLIENT_ID / BLIZZARD_CLIENT_SECRET env vars.
Output: composeApp/src/commonMain/composeResources/files/ladder/tbc_anniversary/
"""

import json
import sys
from datetime import datetime, timezone

from blizzard_api import (
    API_HOSTS, OUTPUT_BASE,
    get_access_token, get_current_season_id, get_pvp_rewards,
    get_leaderboard, resolve_character_profile, write_index,
)

ADDON_ID = "tbc_anniversary"
NAMESPACE_PREFIX = "dynamic-classicann"
PROFILE_NAMESPACE_PREFIX = "profile-classicann"
REGIONS = ["us", "eu"]
BRACKETS = ["2v2", "3v3", "5v5"]
TOP_ENTRIES_LIMIT = 500


def build_snapshot(region, bracket, season_id, total_entries, top_entries, cutoffs):
    return {
        "region": region,
        "bracket": bracket,
        "seasonId": season_id,
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "totalEntries": total_entries,
        "ratingCutoffs": cutoffs,
        "specDistribution": [],  # computed at runtime from topEntries
        "topEntries": top_entries,
    }


def main():
    print("Authenticating with Battle.net...")
    token = get_access_token()
    print("Authenticated.\n")

    addon_dir = OUTPUT_BASE / ADDON_ID
    addon_dir.mkdir(parents=True, exist_ok=True)

    print(f"=== {ADDON_ID} ===")

    for region in REGIONS:
        if region not in API_HOSTS:
            continue

        api_host = API_HOSTS[region]
        namespace = f"{NAMESPACE_PREFIX}-{region}"
        profile_ns = f"{PROFILE_NAMESPACE_PREFIX}-{region}"

        print(f"  [{region}] Detecting current season...")
        try:
            season_id = get_current_season_id(api_host, namespace, token)
        except Exception as e:
            print(f"  [{region}] ERROR: {e}")
            continue
        print(f"  [{region}] Season {season_id}")

        # ── Collect unique characters across all brackets ──
        # Key: character ID, Value: list of (bracket, rank, rating, wins, losses)
        char_brackets: dict[int, list] = {}
        # Key: character ID, Value: (name, realm_slug)
        char_info: dict[int, tuple] = {}
        # Total entries per bracket (full leaderboard size, not just top N)
        bracket_totals: dict[str, int] = {}

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

        # ── Resolve profiles for unique characters (once each) ──
        unique_ids = list(char_info.keys())
        print(f"  [{region}] Resolving {len(unique_ids)} unique character profiles...")

        profile_cache: dict[int, dict] = {}
        for i, char_id in enumerate(unique_ids):
            name, realm_slug = char_info[char_id]
            if name == "Unknown" or not realm_slug:
                continue
            profile = resolve_character_profile(api_host, profile_ns, realm_slug, name, token)
            if profile:
                profile_cache[char_id] = profile
            if (i + 1) % 100 == 0:
                print(f"    ... {i + 1}/{len(unique_ids)} resolved")

        print(f"  [{region}] Resolved {len(profile_cache)}/{len(unique_ids)} profiles")

        # ── Build snapshot per bracket ──
        for bracket in BRACKETS:
            cutoffs = get_pvp_rewards(api_host, namespace, season_id, bracket, token)
            print(f"  [{region}] {bracket} cutoffs: {cutoffs}")

            # Reconstruct top entries for this bracket
            bracket_entries = []
            for char_id, appearances in char_brackets.items():
                for app in appearances:
                    if app["bracket"] == bracket:
                        name, realm_slug = char_info[char_id]
                        profile = profile_cache.get(char_id, {})
                        bracket_entries.append({
                            "rank": app["rank"],
                            "characterName": name,
                            "realmSlug": realm_slug,
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

            snapshot = build_snapshot(region, bracket, season_id, bracket_totals.get(bracket, 0), bracket_entries, cutoffs)

            out_file = addon_dir / f"{region}_{bracket}.json"
            with open(out_file, "w", encoding="utf-8") as f:
                json.dump(snapshot, f, indent=2, ensure_ascii=False)
            print(f"  [{region}] Wrote {out_file.name} ({len(bracket_entries)} entries)")

    index = write_index(addon_dir)
    print(f"\n  Wrote {len(index)} snapshot(s) + index.json")
    print("Done.")


if __name__ == "__main__":
    main()

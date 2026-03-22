#!/usr/bin/env python3
"""
Fetch Retail (Midnight) PvP ladder data from the Blizzard API.

Currently INACTIVE — no retail data is needed at this time.
To activate, uncomment the main() call at the bottom.

Usage:
    python scripts/fetch_midnight.py
"""

import json
import sys
from datetime import datetime, timezone

from blizzard_api import (
    API_HOSTS, OUTPUT_BASE,
    get_access_token, get_current_season_id, get_pvp_rewards,
    get_leaderboard, get_leaderboard_index, resolve_character_profile,
    write_index,
)

ADDON_ID = "midnight"
NAMESPACE_PREFIX = "dynamic"
PROFILE_NAMESPACE_PREFIX = "profile"
REGIONS = ["us", "eu"]
BRACKETS = ["2v2", "3v3"]
TOP_ENTRIES_LIMIT = 500

# Blizzard shuffle bracket name → our spec ID mapping
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


def fetch_spec_distribution(api_host, namespace, season_id, token):
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

    return [
        {"specId": sid, "count": c, "percentage": round(c / total * 100, 2) if total > 0 else 0.0}
        for sid, c in sorted(spec_counts.items(), key=lambda x: -x[1])
    ]


def main():
    print("Retail (Midnight) fetch is currently INACTIVE.")
    print("To activate, edit this script and uncomment the main() call.")
    # Uncomment below to enable:
    # _run()


def _run():
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

        # Spec distribution from shuffle brackets
        print(f"  [{region}] Fetching spec distribution...")
        try:
            spec_distribution = fetch_spec_distribution(api_host, namespace, season_id, token)
            print(f"  [{region}] {len(spec_distribution)} specs tracked")
        except Exception as e:
            print(f"  [{region}] WARNING: {e}")
            spec_distribution = []

        # Collect unique characters
        char_brackets: dict[int, list] = {}
        char_info: dict[int, tuple] = {}
        bracket_totals: dict[str, int] = {}

        for bracket in BRACKETS:
            print(f"  [{region}] Fetching {bracket} leaderboard...")
            try:
                entries = get_leaderboard(api_host, namespace, season_id, bracket, token)
            except Exception as e:
                print(f"  [{region}] ERROR: {e}")
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

        # Resolve profiles
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
                print(f"    ... {i + 1}/{len(unique_ids)}")

        print(f"  [{region}] Resolved {len(profile_cache)}/{len(unique_ids)} profiles")

        # Build snapshots
        for bracket in BRACKETS:
            cutoffs = get_pvp_rewards(api_host, namespace, season_id, bracket, token)
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
            snapshot = {
                "region": region,
                "bracket": bracket,
                "seasonId": season_id,
                "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "totalEntries": bracket_totals.get(bracket, 0),
                "ratingCutoffs": cutoffs,
                "specDistribution": spec_distribution,
                "topEntries": bracket_entries,
            }
            out_file = addon_dir / f"{region}_{bracket}.json"
            with open(out_file, "w", encoding="utf-8") as f:
                json.dump(snapshot, f, indent=2, ensure_ascii=False)
            print(f"  [{region}] Wrote {out_file.name}")

    index = write_index(addon_dir)
    print(f"\n  Wrote {len(index)} snapshot(s) + index.json")
    print("Done.")


if __name__ == "__main__":
    main()

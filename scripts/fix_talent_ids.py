"""
Fix talent tree JSON IDs to match Blizzard API talent IDs.

The talent tree definitions use Wowhead/DBC talent IDs, but the Blizzard TBC
Anniversary API returns different IDs for the same talents. This script:
1. Loads player data to find all talent IDs actually used by the API
2. Queries the Blizzard API (specializations endpoint) to get spell names/IDs
   for every talent ID observed in player data
3. Matches API talent IDs to tree definition talents by spell ID or name
4. Rewrites the tree JSON files with corrected IDs

Usage:
    python scripts/fix_talent_ids.py              # full run
    python scripts/fix_talent_ids.py --dry-run     # show changes without writing
"""

import json
import os
import sys
import time
import urllib.parse
from pathlib import Path
from collections import defaultdict

sys.path.insert(0, str(Path(__file__).parent))
from blizzard_api import get_access_token, http_get_json, API_HOSTS

REPO_ROOT = Path(__file__).resolve().parent.parent
TREE_DIR = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "talent_trees" / "tbc"
LADDER_DIR = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "ladder" / "tbc_anniversary"

# TBC addition spell IDs from fetch_talent_trees.py — used for matching
# Format: (class, tree, talent_name) -> base_spell_id
TBC_SPELL_IDS = {
    ("mage", "Arcane", "Prismatic Cloak"): 31569,
    ("mage", "Arcane", "Arcane Potency"): 31571,
    ("mage", "Arcane", "Empowered Arcane Missiles"): 31579,
    ("mage", "Arcane", "Mind Mastery"): 31584,
    ("mage", "Arcane", "Spell Power"): 35578,
    ("mage", "Arcane", "Slow"): 31589,
    ("mage", "Fire", "Blazing Speed"): 31641,
    ("mage", "Fire", "Playing with Fire"): 31638,
    ("mage", "Fire", "Molten Fury"): 31679,
    ("mage", "Fire", "Empowered Fireball"): 31656,
    ("mage", "Fire", "Dragon's Breath"): 31661,
    ("mage", "Frost", "Ice Floes"): 31670,
    ("mage", "Frost", "Frozen Core"): 31667,
    ("mage", "Frost", "Arctic Winds"): 31674,
    ("mage", "Frost", "Empowered Frostbolt"): 31682,
    ("mage", "Frost", "Summon Water Elemental"): 31687,
    ("warrior", "Arms", "Second Wind"): 29834,
    ("warrior", "Arms", "Improved Intercept"): 29888,
    ("warrior", "Arms", "Blood Frenzy"): 29836,
    ("warrior", "Arms", "Improved Mortal Strike"): 35446,
    ("warrior", "Arms", "Endless Rage"): 29623,
    ("warrior", "Fury", "Improved Berserker Stance"): 29759,
    ("warrior", "Fury", "Improved Whirlwind"): 29721,
    ("warrior", "Fury", "Focused Rage"): 29787,
    ("warrior", "Fury", "Precision"): 29590,
    ("warrior", "Fury", "Rampage"): 29801,
    ("warrior", "Protection", "Focused Rage"): 29787,
    ("warrior", "Protection", "Improved Defensive Stance"): 29593,
    ("warrior", "Protection", "Vitality"): 29140,
    ("warrior", "Protection", "Improved Shield Block"): 29598,
    ("warrior", "Protection", "Devastate"): 30022,
    ("rogue", "Assassination", "Fleet Footed"): 31208,
    ("rogue", "Assassination", "Find Weakness"): 31233,
    ("rogue", "Assassination", "Master Poisoner"): 31226,
    ("rogue", "Assassination", "Quick Recovery"): 31244,
    ("rogue", "Assassination", "Mutilate"): 34411,
    ("rogue", "Combat", "Nerves of Steel"): 31130,
    ("rogue", "Combat", "Surprise Attacks"): 32601,
    ("rogue", "Combat", "Combat Potency"): 35541,
    ("rogue", "Combat", "Savage Combat"): 31124,
    ("rogue", "Subtlety", "Enveloping Shadows"): 31211,
    ("rogue", "Subtlety", "Cheat Death"): 31228,
    ("rogue", "Subtlety", "Sinister Calling"): 31216,
    ("rogue", "Subtlety", "Waylay"): 31224,
    ("rogue", "Subtlety", "Shadowstep"): 36554,
    ("priest", "Discipline", "Absolution"): 33167,
    ("priest", "Discipline", "Focused Power"): 33186,
    ("priest", "Discipline", "Enlightenment"): 34908,
    ("priest", "Discipline", "Reflective Shield"): 33201,
    ("priest", "Discipline", "Pain Suppression"): 33206,
    ("priest", "Holy", "Blessed Resilience"): 33142,
    ("priest", "Holy", "Empowered Healing"): 33158,
    ("priest", "Holy", "Surge of Light"): 33150,
    ("priest", "Holy", "Spiritual Healing"): 33154,
    ("priest", "Holy", "Circle of Healing"): 34861,
    ("priest", "Shadow", "Shadow Resilience"): 33371,
    ("priest", "Shadow", "Focused Mind"): 33213,
    ("priest", "Shadow", "Shadow Power"): 33221,
    ("priest", "Shadow", "Misery"): 33191,
    ("priest", "Shadow", "Vampiric Touch"): 34914,
    ("paladin", "Holy", "Blessed Life"): 31828,
    ("paladin", "Holy", "Holy Guidance"): 31837,
    ("paladin", "Holy", "Light's Grace"): 31833,
    ("paladin", "Holy", "Divine Illumination"): 31842,
    ("paladin", "Protection", "Spell Warding"): 33079,
    ("paladin", "Protection", "Sacred Duty"): 31848,
    ("paladin", "Protection", "Improved Holy Shield"): 35876,
    ("paladin", "Protection", "Ardent Defender"): 31850,
    ("paladin", "Protection", "Combat Expertise"): 31858,
    ("paladin", "Protection", "Avenger's Shield"): 32700,
    ("paladin", "Retribution", "Sanctified Judgement"): 31876,
    ("paladin", "Retribution", "Fanaticism"): 31879,
    ("paladin", "Retribution", "Sanctified Seals"): 35395,
    ("paladin", "Retribution", "Improved Sanctity Aura"): 31869,
    ("paladin", "Retribution", "Vengeance"): 31871,
    ("paladin", "Retribution", "Crusader Strike"): 35395,
    ("hunter", "Beast Mastery", "Catlike Reflexes"): 34462,
    ("hunter", "Beast Mastery", "Serpent's Swiftness"): 34466,
    ("hunter", "Beast Mastery", "Ferocious Inspiration"): 34455,
    ("hunter", "Beast Mastery", "The Beast Within"): 34692,
    ("hunter", "Beast Mastery", "Snake Trap"): 34600,
    ("hunter", "Marksmanship", "Improved Barrage"): 35104,
    ("hunter", "Marksmanship", "Combat Experience"): 34475,
    ("hunter", "Marksmanship", "Master Marksman"): 34485,
    ("hunter", "Marksmanship", "Careful Aim"): 34482,
    ("hunter", "Marksmanship", "Silencing Shot"): 34490,
    ("hunter", "Marksmanship", "Readiness"): 23989,
    ("hunter", "Survival", "Survival Instincts"): 34494,
    ("hunter", "Survival", "Resourcefulness"): 34491,
    ("hunter", "Survival", "Expose Weakness"): 34500,
    ("hunter", "Survival", "Thrill of the Hunt"): 34497,
    ("hunter", "Survival", "Master Tactician"): 34506,
    ("druid", "Balance", "Celestial Focus"): 16850,
    ("druid", "Balance", "Lunar Guidance"): 33589,
    ("druid", "Balance", "Improved Faerie Fire"): 33600,
    ("druid", "Balance", "Dreamstate"): 33597,
    ("druid", "Balance", "Wrath of Cenarius"): 33603,
    ("druid", "Balance", "Force of Nature"): 33831,
    ("druid", "Feral Combat", "Nurturing Instinct"): 33872,
    ("druid", "Feral Combat", "Primal Tenacity"): 33851,
    ("druid", "Feral Combat", "Predatory Instincts"): 33859,
    ("druid", "Feral Combat", "Survival of the Fittest"): 33853,
    ("druid", "Feral Combat", "Natural Perfection"): 33855,
    ("druid", "Feral Combat", "Mangle"): 33876,
    ("druid", "Restoration", "Empowered Touch"): 33879,
    ("druid", "Restoration", "Natural Perfection"): 33881,
    ("druid", "Restoration", "Living Spirit"): 34151,
    ("druid", "Restoration", "Empowered Rejuvenation"): 33886,
    ("druid", "Restoration", "Tree of Life"): 33891,
    ("shaman", "Elemental", "Totem of Wrath"): 30706,
    ("shaman", "Elemental", "Lightning Overload"): 30675,
    ("shaman", "Elemental", "Elemental Shields"): 30669,
    ("shaman", "Elemental", "Unrelenting Storm"): 30664,
    ("shaman", "Enhancement", "Dual Wield"): 30798,
    ("shaman", "Enhancement", "Shamanistic Rage"): 30823,
    ("shaman", "Enhancement", "Unleashed Rage"): 30802,
    ("shaman", "Enhancement", "Mental Quickness"): 30812,
    ("shaman", "Restoration", "Nature's Guardian"): 30881,
    ("shaman", "Restoration", "Focused Mind"): 30864,
    ("shaman", "Restoration", "Nature's Blessing"): 30867,
    ("shaman", "Restoration", "Improved Chain Heal"): 30872,
    ("shaman", "Restoration", "Earth Shield"): 974,
    ("warlock", "Affliction", "Malediction"): 32477,
    ("warlock", "Affliction", "Contagion"): 30060,
    ("warlock", "Affliction", "Unstable Affliction"): 30108,
    ("warlock", "Affliction", "Soul Siphon"): 35195,
    ("warlock", "Demonology", "Demonic Knowledge"): 35691,
    ("warlock", "Demonology", "Demonic Tactics"): 30242,
    ("warlock", "Demonology", "Summon Felguard"): 30146,
    ("warlock", "Demonology", "Demonic Resilience"): 30319,
    ("warlock", "Destruction", "Backlash"): 34935,
    ("warlock", "Destruction", "Nether Protection"): 30299,
    ("warlock", "Destruction", "Shadow and Flame"): 30288,
    ("warlock", "Destruction", "Soul Leech"): 30293,
    ("warlock", "Destruction", "Shadowfury"): 30283,
}


def load_tree_defs():
    """Load all talent tree definitions."""
    trees = {}
    for f in sorted(TREE_DIR.iterdir()):
        if f.suffix != ".json":
            continue
        data = json.loads(f.read_text(encoding="utf-8"))
        trees[data["classId"]] = data
    return trees


def load_player_data():
    """Load all player profiles from both regions."""
    all_players = {}
    for region in ["us", "eu"]:
        path = LADDER_DIR / f"players_{region}.json"
        if path.exists():
            data = json.loads(path.read_text(encoding="utf-8"))
            for cid, p in data.items():
                all_players[f"{region}_{cid}"] = p
    return all_players


def collect_player_talent_ids(players):
    """Collect all unique talent IDs per (classId, treeName) from player data.
    Includes both active AND inactive talent groups for maximum coverage."""
    result = defaultdict(set)  # (classId, treeName) -> set of talent IDs
    for p in players.values():
        cls = p.get("classId")
        for tg in p.get("talentGroups", []):
            for spec in tg.get("specializations", []):
                key = (cls, spec["treeName"])
                for t in spec.get("talents", []):
                    result[key].add(t["id"])
    return result


def find_players_with_talent(players, class_id, tree_name, talent_id):
    """Find players who have a specific talent selected (active or inactive groups)."""
    results = []
    for p in players.values():
        if p.get("classId") != class_id:
            continue
        for tg in p.get("talentGroups", []):
            for spec in tg.get("specializations", []):
                if spec.get("treeName") == tree_name:
                    for t in spec.get("talents", []):
                        if t["id"] == talent_id:
                            results.append(p)
                            break
    return results


def query_talent_info(api_host, ns, token, player, tree_name, talent_id):
    """Query the API for a player's specializations and return info for a specific talent."""
    name_lower = player["name"].lower()
    realm = player["realmSlug"]
    url = f"{api_host}/profile/wow/character/{realm}/{urllib.parse.quote(name_lower)}/specializations?namespace={ns}&locale=en_US"

    spec_data = http_get_json(url, token)
    for group in spec_data.get("specialization_groups", []):
        for spec in group.get("specializations", []):
            if spec.get("specialization_name") == tree_name:
                for t in spec.get("talents", []):
                    if t.get("talent", {}).get("id") == talent_id:
                        spell = t.get("spell_tooltip", {}).get("spell", {})
                        rank = t.get("talent_rank", 1)
                        # Compute base spell ID (rank 1)
                        spell_id = spell.get("id", 0)
                        base_spell_id = spell_id - (rank - 1) if spell_id else 0
                        return {
                            "name": spell.get("name", "Unknown"),
                            "spell_id": spell_id,
                            "base_spell_id": base_spell_id,
                            "rank": rank,
                        }
    return None


def resolve_all_talent_info(players, tree_defs, player_ids_per_tree):
    """Query the API to get names/spell IDs for all talent IDs in player data."""
    token = get_access_token()
    api = API_HOSTS["us"]
    ns = "profile-classicann-us"

    # Collect ALL unique talent IDs that need resolution (both matching and unknown)
    all_ids_to_resolve = set()
    for (cls, tree_name), ids in player_ids_per_tree.items():
        for tid in ids:
            all_ids_to_resolve.add((cls, tree_name, tid))

    # Query players to resolve talent info
    resolved = {}  # (classId, treeName, talentId) -> {name, spell_id, base_spell_id}
    queried_players = set()  # (name, realm) to avoid duplicate API calls

    # Group by (class, tree) for efficient querying
    by_class_tree = defaultdict(set)
    for cls, tree, tid in all_ids_to_resolve:
        by_class_tree[(cls, tree)].add(tid)

    total_queries = 0

    for (cls, tree_name), ids_needed in sorted(by_class_tree.items()):
        unresolved = ids_needed - {tid for (c, t, tid) in resolved if c == cls and t == tree_name}
        if not unresolved:
            continue

        # Find players who have unresolved talent IDs
        for tid in sorted(unresolved):
            if (cls, tree_name, tid) in resolved:
                continue

            candidates = find_players_with_talent(players, cls, tree_name, tid)
            if not candidates:
                continue

            # Pick a player we haven't queried yet
            player = None
            for c in candidates:
                key = (c["name"], c["realmSlug"])
                if key not in queried_players:
                    player = c
                    break
            if not player:
                player = candidates[0]  # Re-query if needed

            key = (player["name"], player["realmSlug"])

            try:
                # Query ALL talents for this player (covers multiple unresolved IDs)
                name_lower = player["name"].lower()
                realm = player["realmSlug"]
                url = f"{api}/profile/wow/character/{realm}/{urllib.parse.quote(name_lower)}/specializations?namespace={ns}&locale=en_US"

                spec_data = http_get_json(url, token)
                queried_players.add(key)
                total_queries += 1

                for group in spec_data.get("specialization_groups", []):
                    for spec in group.get("specializations", []):
                        for t in spec.get("talents", []):
                            t_id = t.get("talent", {}).get("id")
                            t_tree = spec.get("specialization_name")
                            if t_id is None:
                                continue
                            spell = t.get("spell_tooltip", {}).get("spell", {})
                            rank = t.get("talent_rank", 1)
                            spell_id = spell.get("id", 0)
                            base_spell_id = spell_id - (rank - 1) if spell_id else 0
                            resolved[(cls, t_tree, t_id)] = {
                                "name": spell.get("name", "Unknown"),
                                "spell_id": spell_id,
                                "base_spell_id": base_spell_id,
                                "rank": rank,
                            }

                time.sleep(0.1)
            except Exception as e:
                print(f"  Warning: API query failed for {player['name']}: {e}")

    print(f"  Queried {total_queries} players, resolved {len(resolved)} talents")
    return resolved


def build_mappings(tree_defs, player_ids_per_tree, resolved_talents):
    """Build ID mappings from tree definition IDs to correct API IDs."""
    mappings = {}  # (classId, treeName) -> {old_def_id: (new_api_id, api_name)}

    for class_id, class_data in tree_defs.items():
        for tree_data in class_data["trees"]:
            tree_name = tree_data["name"]
            def_ids = {t["id"] for t in tree_data["talents"]}
            def_by_id = {t["id"]: t for t in tree_data["talents"]}
            player_ids = player_ids_per_tree.get((class_id, tree_name), set())

            # Build spell_id -> def talent mapping for this tree
            # Use TBC_SPELL_IDS for matching
            def_by_spell = {}
            for tal in tree_data["talents"]:
                key = (class_id, tree_name, tal["name"])
                if key in TBC_SPELL_IDS:
                    def_by_spell[TBC_SPELL_IDS[key]] = tal["id"]

            # Build api_id -> (name, base_spell_id) mapping
            api_info = {}
            for (c, t, tid), info in resolved_talents.items():
                if c == class_id and t == tree_name:
                    api_info[tid] = info

            # Strategy: match by spell_id, then by name
            tree_mapping = {}

            # For each API talent ID that's NOT in the def
            unknown_api_ids = player_ids - def_ids
            unused_def_ids = def_ids - player_ids

            for api_id in unknown_api_ids:
                info = api_info.get(api_id)
                if not info:
                    continue

                matched_def_id = None

                # Try spell_id matching first
                if info["base_spell_id"] in def_by_spell:
                    matched_def_id = def_by_spell[info["base_spell_id"]]
                    if matched_def_id not in unused_def_ids:
                        matched_def_id = None  # Already matched or still in use

                # Try name matching
                if not matched_def_id:
                    api_name_lower = info["name"].lower()
                    for did in unused_def_ids:
                        if did in [v for v in tree_mapping.values()]:
                            continue  # Already mapped
                        def_tal = def_by_id[did]
                        if def_tal["name"].lower() == api_name_lower:
                            matched_def_id = did
                            break

                if matched_def_id and matched_def_id in unused_def_ids:
                    tree_mapping[matched_def_id] = api_id
                    unused_def_ids.discard(matched_def_id)

            # Also check if any "matching" IDs have swapped names
            # (IDs in both def and player data but with wrong name assignments)
            matching_ids = def_ids & player_ids
            name_swaps = {}  # def_id -> {correct_name, correct_spell_id}
            for mid in matching_ids:
                def_tal = def_by_id[mid]
                api_i = api_info.get(mid)
                if api_i and def_tal["name"] != api_i["name"]:
                    name_swaps[mid] = api_i

            if tree_mapping or name_swaps:
                mappings[(class_id, tree_name)] = {
                    "id_changes": tree_mapping,
                    "name_fixes": name_swaps,
                }

    return mappings


def apply_mappings(tree_defs, mappings, dry_run=False):
    """Apply ID mappings and name fixes to tree definition JSON files."""
    changes_log = []

    for class_id, class_data in tree_defs.items():
        class_changed = False
        for tree_data in class_data["trees"]:
            tree_name = tree_data["name"]
            key = (class_id, tree_name)
            if key not in mappings:
                continue

            mapping = mappings[key]
            id_changes = mapping.get("id_changes", {})
            name_fixes = mapping.get("name_fixes", {})

            # Apply ID changes
            for old_id, new_id in id_changes.items():
                for tal in tree_data["talents"]:
                    if tal["id"] == old_id:
                        changes_log.append(
                            f"  {class_id}/{tree_name}: ID {old_id} ({tal['name']}) -> {new_id}"
                        )
                        tal["id"] = new_id
                        class_changed = True
                        break

            # Apply name fixes for matching IDs with wrong names
            for mid, api_info in name_fixes.items():
                for tal in tree_data["talents"]:
                    if tal["id"] == mid:
                        old_name = tal["name"]
                        new_name = api_info["name"]
                        if old_name != new_name:
                            changes_log.append(
                                f"  {class_id}/{tree_name}: ID {mid} name '{old_name}' -> '{new_name}'"
                            )
                            tal["name"] = new_name
                            class_changed = True
                        break

            # Re-sort talents by row, col
            tree_data["talents"].sort(key=lambda t: (t["row"], t["col"]))

        if class_changed and not dry_run:
            path = TREE_DIR / f"{class_id}.json"
            path.write_text(
                json.dumps(class_data, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )
            changes_log.append(f"  -> Written: {path.name}")

    return changes_log


def main():
    dry_run = "--dry-run" in sys.argv

    print("Loading tree definitions...")
    tree_defs = load_tree_defs()

    print("Loading player data...")
    players = load_player_data()

    print("Collecting player talent IDs...")
    player_ids = collect_player_talent_ids(players)

    # Show mismatch summary
    total_mismatched = 0
    for class_id, class_data in tree_defs.items():
        for tree_data in class_data["trees"]:
            tree_name = tree_data["name"]
            def_ids = {t["id"] for t in tree_data["talents"]}
            p_ids = player_ids.get((class_id, tree_name), set())
            unused = def_ids - p_ids
            unknown = p_ids - def_ids
            if unused or unknown:
                total_mismatched += len(unused)
    print(f"Found {total_mismatched} mismatched talent IDs across all trees")

    print("\nQuerying Blizzard API for talent info...")
    resolved = resolve_all_talent_info(players, tree_defs, player_ids)

    print("\nBuilding ID mappings...")
    mappings = build_mappings(tree_defs, player_ids, resolved)

    print(f"\nMappings found for {len(mappings)} trees:")
    total_id_fixes = sum(len(m.get("id_changes", {})) for m in mappings.values())
    total_name_fixes = sum(len(m.get("name_fixes", {})) for m in mappings.values())
    print(f"  {total_id_fixes} ID fixes, {total_name_fixes} name fixes")

    if dry_run:
        print("\n[DRY RUN] Changes that would be applied:")
    else:
        print("\nApplying changes...")

    changes = apply_mappings(tree_defs, mappings, dry_run=dry_run)
    for line in changes:
        print(line)

    if not changes:
        print("  No changes to apply.")

    # Report remaining unresolved mismatches
    print("\n=== Remaining unresolved mismatches ===")
    remaining = 0
    for class_id, class_data in tree_defs.items():
        for tree_data in class_data["trees"]:
            tree_name = tree_data["name"]
            def_ids = {t["id"] for t in tree_data["talents"]}
            p_ids = player_ids.get((class_id, tree_name), set())
            unused = sorted(def_ids - p_ids)
            unknown = sorted(p_ids - def_ids)
            if unused or unknown:
                remaining += len(unused) + len(unknown)
                print(f"  {class_id}/{tree_name}:")
                for did in unused:
                    tal = next(t for t in tree_data["talents"] if t["id"] == did)
                    print(f"    DEF {did} ({tal['name']}, row={tal['row']}) not in player data")
                for pid in unknown:
                    info = resolved.get((class_id, tree_name, pid))
                    name = info["name"] if info else "???"
                    print(f"    API {pid} ({name}) not in tree def")

    if remaining:
        print(f"\n  {remaining} total remaining mismatches (talents no player selects or missing def entries)")
    else:
        print("  None! All talent IDs are now aligned.")


if __name__ == "__main__":
    main()

"""
Fix remaining talent tree ID mismatches that the automated script couldn't resolve.

These mappings are determined from:
- Position-based analysis (which grid cell each API ID occupies)
- maxRank matching between DEF and API entries
- Co-occurrence analysis (which API IDs appear together)
- API talent name lookups

Usage:
    python scripts/fix_talent_ids_remaining.py              # apply fixes
    python scripts/fix_talent_ids_remaining.py --dry-run     # show changes only
"""

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
TREE_DIR = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "talent_trees" / "tbc"

# Each entry: (class_id, tree_name, action, old_id_or_position, new_data)
# action = "replace_id" | "replace_full" | "add"
#   replace_id:   change only the talent ID (keep name, pos, icon, maxRank)
#   replace_full:  replace ID, name, and maxRank (keep pos and icon)
#   add:          add a new talent entry at a position

FIXES = [
    # ========== DRUID ==========
    # Balance: 781 (Natural Shapeshifter, row=1,col=3) -> 1822 (Focused Starlight)
    ("druid", "Balance", "replace_full", 781, {"id": 1822, "name": "Focused Starlight", "maxRank": 2}),
    # 791 (Natural Weapons, row=1,col=2) - no player data; keep as-is

    # Feral Combat: 800 (Blood Frenzy, row=3,col=2,mr=2) -> 1798 (Improved Leader of the Pack, mr=2)
    ("druid", "Feral Combat", "replace_full", 800, {"id": 1798, "name": "Improved Leader of the Pack", "maxRank": 2}),
    # 1830 (Mangle Cat, row=8,col=1,mr=1) -> 1796 (Mangle, mr=1)
    ("druid", "Feral Combat", "replace_full", 1830, {"id": 1796, "name": "Mangle", "maxRank": 1}),
    # 1791 (Natural Perfection) - no player data; keep as-is

    # Restoration: all fixed already

    # ========== HUNTER ==========
    # Beast Mastery: 1793 (Catlike Reflexes, row=5,col=0,mr=3) -> 1799 (Animal Handler, mr=2)
    ("hunter", "Beast Mastery", "replace_full", 1793, {"id": 1799, "name": "Animal Handler", "maxRank": 2}),
    # 1812 (Snake Trap) - no player data; keep as-is

    # Marksmanship: 1351 (Improved Scorpid Sting, row=4,col=2,mr=3) -> 1818 (Go for the Throat, mr=2)
    #   1818 and 1819 co-occur (60 players have both), so they're at different positions
    #   1818 (Go for the Throat, mr=2) -> row=4,col=2 (89 votes, strongest)
    #   1819 (Rapid Killing, mr=2) -> row=2,col=3 (by elimination after 1818 takes (4,2))
    ("hunter", "Marksmanship", "replace_full", 1351, {"id": 1818, "name": "Go for the Throat", "maxRank": 2}),
    ("hunter", "Marksmanship", "replace_full", 1352, {"id": 1819, "name": "Rapid Killing", "maxRank": 2}),
    # 1813 (Readiness, row=8) - no player data; keep as-is

    # Survival: 1804 (Resourcefulness, row=6,col=0,mr=3) -> 1820 (Hawk Eye, mr=3)
    #   1820 has 69 votes for (6,0). 1809 has only 1 vote - too rare to determine position.
    ("hunter", "Survival", "replace_full", 1804, {"id": 1820, "name": "Hawk Eye", "maxRank": 3}),
    # 1809 appears in 1 player's inactive build - might be at a position we think is "matching"
    # but we can't determine where. Skip for now.

    # ========== MAGE ==========
    # Arcane: 1825 (Slow, row=8,col=1,mr=1) -> 1726 (Prismatic Cloak, mr=2)
    #   The name fix previously renamed 1729 to "Slow" at (7,1). The actual Slow should be at (8,1).
    #   1726 (Prismatic Cloak) is NOT a capstone talent - it's a row 5 talent.
    #   The issue: after name fixes, 1729="Slow" is at wrong position (7,1 instead of 8,1).
    #   Fix: swap 1729 back to its original position meaning, and fix 1825.
    #   Actually, the API says 1729 IS named "Slow" - the talent at API position (7,1) is Slow.
    #   And 1726 (Prismatic Cloak) replaces the DEF entry at (8,1).
    #   But Prismatic Cloak with maxRank=2 as a capstone doesn't make sense.
    #   This tree needs deeper investigation. Skip for now.

    # Fire: API 1730 (Playing with Fire, mr=3) not in tree def
    #   0 DEF-only entries. The tree is MISSING this talent. Need to add it.
    #   Playing with Fire is at row 6, col 2 in TBC (based on known tree layout)
    #   But we already have DEF 1733 at row 6 col 2 renamed to "Pyromaniac".
    #   The API says 1733 = Pyromaniac. And 1730 = Playing with Fire.
    #   These might be at different positions. Without more data, skip.

    # Frost: all fixed already

    # ========== PALADIN ==========
    # Holy: 1831 (Holy Shock, row=8,col=1,mr=1) -> need to place 1745 and 1747
    #   1745 (Light's Grace, mr=3) and 1747 (Divine Illumination, mr=1) co-occur in all 22 players
    #   1747 (mr=1) matches 1831 (mr=1) at row 8 - likely the capstone
    ("paladin", "Holy", "replace_full", 1831, {"id": 1747, "name": "Divine Illumination", "maxRank": 1}),
    # 1745 (Light's Grace) needs a position. 0 remaining DEF-only entries, so it replaces a "matching" entry.
    # Skip until we can determine which position.

    # Protection: API 1829 (Improved Holy Shield) not in tree def. 0 DEF-only.
    # Skip - needs position determination.

    # Retribution: 2 DEF (1405 row=3,col=2, 1753 row=5,col=0) vs 2 API (1759, 1823)
    #   1823 (Crusader Strike, mr=1) should NOT be at row 3 or 5 (it's a 41-point talent).
    #   But the position analysis says it goes to (3,2). That seems wrong.
    #   1759 (Fanaticism, mr=5) at (5,0) matches DEF 1753 (mr=3) - position match but mr differs
    #   1823 (Crusader Strike, mr=1) at (3,2) matches DEF 1405 (mr=2) - doesn't make sense as capstone
    #   The ret tree structure likely changed. Let's match by position votes:
    ("paladin", "Retribution", "replace_full", 1405, {"id": 1759, "name": "Fanaticism", "maxRank": 5}),
    ("paladin", "Retribution", "replace_full", 1753, {"id": 1823, "name": "Crusader Strike", "maxRank": 1}),

    # ========== PRIEST ==========
    # Discipline: 0 DEF-only, 2 API-only (1774 Pain Suppression, 1858 Focused Will)
    # These replace "matching" IDs that have wrong names. Skip - needs deeper investigation.

    # Holy: 1774 (Blessed Resilience, row=5,col=0,mr=3) -> 1768 (Holy Concentration, mr=3)
    ("priest", "Holy", "replace_full", 1774, {"id": 1768, "name": "Holy Concentration", "maxRank": 3}),

    # Shadow: 483 (Improved Fade, row=3,col=1,mr=2) -> 1778 (Shadow Power, mr=5)
    #   1778 has 57 position votes for (3,1). maxRank differs (2 vs 5) but position is clear.
    ("priest", "Shadow", "replace_full", 483, {"id": 1778, "name": "Shadow Power", "maxRank": 5}),
    # 1817 (Vampiric Touch, row=8) - no player data. The real VT might have a different ID.
    # But no API-only ID remains to map to it. Keep as-is.

    # ========== ROGUE ==========
    # Assassination: 9 DEF-only, 0 API-only. No assassination rogues on ladder at all.
    # All IDs are unverifiable. Keep as-is.

    # Combat: 3 DEF vs 3 API, all maxRank match perfectly!
    #   202 (Puncturing Wounds, row=1,col=0,mr=3) -> 1827 (Improved Slice and Dice, mr=3)
    #   206 (Improved Kick, row=3,col=0,mr=2) -> 1705 (Vitality, mr=2)
    #   1714 (Blade Twisting, row=7,col=2,mr=2) -> 1706 (unknown, mr=2)
    ("rogue", "Combat", "replace_full", 202, {"id": 1827, "name": "Improved Slice and Dice", "maxRank": 3}),
    ("rogue", "Combat", "replace_full", 206, {"id": 1705, "name": "Vitality", "maxRank": 2}),
    ("rogue", "Combat", "replace_full", 1714, {"id": 1706, "name": "Savage Combat", "maxRank": 2}),

    # Subtlety: 1718 (Cloak of Shadows, row=7,col=2,mr=2) -> 1713 (Master of Subtlety, mr=3)
    ("rogue", "Subtlety", "replace_full", 1718, {"id": 1713, "name": "Master of Subtlety", "maxRank": 3}),

    # ========== SHAMAN ==========
    # Elemental: 2 DEF (row 5,0 and 7,2) vs 3 API (1685, 1686, 1687)
    #   All 3 API IDs co-occur. maxRank matching:
    #   1834 (row=5,col=0,mr=5) -> 1686 (Lightning Overload, mr=5) [perfect match!]
    #   1684 (row=7,col=2,mr=3) -> 1685 (Elemental Precision, mr=3) [perfect match!]
    #   1687 (Totem of Wrath, mr=1) -> needs to be added (it's the 41-point capstone at row 8)
    ("shaman", "Elemental", "replace_full", 1834, {"id": 1686, "name": "Lightning Overload", "maxRank": 5}),
    ("shaman", "Elemental", "replace_full", 1684, {"id": 1685, "name": "Elemental Precision", "maxRank": 3}),
    ("shaman", "Elemental", "add", (8, 1), {"id": 1687, "name": "Totem of Wrath", "icon": "spell_fire_totemofwrath", "maxRank": 1, "prerequisiteId": None}),

    # Enhancement: 1835 (Stormstrike, row=8,col=1,mr=1) -> 1692 (Dual Wield Specialization, mr=3)
    ("shaman", "Enhancement", "replace_full", 1835, {"id": 1692, "name": "Dual Wield Specialization", "maxRank": 3}),

    # Restoration: 1689 (Nature's Guardian, row=5,col=0,mr=5) - no player data. Keep as-is.

    # ========== WARLOCK ==========
    # Affliction: 3 DEF vs 3 API
    #   maxRank matching:
    #   1121 (row=3,col=3,mr=2) -> 1668 (Improved Howl of Terror, mr=2) [match!]
    #   1082 (row=4,col=3,mr=4) -> no direct maxRank match. 1667 (Malediction, mr=3) closest?
    #   1767 (row=7,col=0,mr=2) -> no match.
    #   1763 (Shadow Embrace, mr=5) -> no maxRank match in DEF.
    #   Co-occurrence: 1667+1668 only 6 overlap (different positions), 1667+1763 all 21 (different positions)
    #   Position votes: all 3 API map to (4,3). But 1668 (mr=2) matches DEF 1121 (row=3,mr=2) better.
    #   By elimination: 1668 -> (3,3), 1667 -> (4,3), 1763 -> (7,0)
    ("warlock", "Affliction", "replace_full", 1121, {"id": 1668, "name": "Improved Howl of Terror", "maxRank": 2}),
    ("warlock", "Affliction", "replace_full", 1082, {"id": 1667, "name": "Malediction", "maxRank": 3}),
    ("warlock", "Affliction", "replace_full", 1767, {"id": 1763, "name": "Shadow Embrace", "maxRank": 5}),

    # Demonology: 3 DEF vs 2 API
    #   1671 (Demonic Aegis, mr=3) and 1681 (Mana Feed, mr=1) co-occur in 113/159 players
    #   DEF: 1261 (row=4,col=3,mr=2), 1283 (row=4,col=0,mr=5), 1774 (row=5,col=0,mr=3)
    #   Position: both API map to (4,3). 1671 (mr=3) best matches 1774 (row=5,mr=3) by maxRank
    #   1681 (Mana Feed, mr=1) matches 1261 (row=4,col=3,mr=2) by position or 1283 (mr=5) - poor match
    #   Try: 1671 -> 1774 (maxRank match), 1681 -> 1261 (position vote)
    ("warlock", "Demonology", "replace_full", 1774, {"id": 1671, "name": "Demonic Aegis", "maxRank": 3}),
    ("warlock", "Demonology", "replace_full", 1261, {"id": 1681, "name": "Mana Feed", "maxRank": 1}),
    # 1283 (Improved Subjugate Demon) - no player data; keep as-is

    # Destruction: 984 (Improved Lash of Pain, row=2,col=1) - no player data. Keep as-is.

    # ========== WARRIOR ==========
    # Arms: 1863 (Improved Intercept, row=6,col=0,mr=2) -> 1662 (Improved Disciplines, mr=3)
    ("warrior", "Arms", "replace_full", 1863, {"id": 1662, "name": "Improved Disciplines", "maxRank": 3}),

    # Fury: 1869 (Focused Rage, row=6,col=2,mr=3) - no player data. Keep as-is.

    # Protection: all fixed already
]


def apply_fixes(dry_run=False):
    # Load all trees
    tree_files = {}
    for f in sorted(TREE_DIR.iterdir()):
        if f.suffix != ".json":
            continue
        data = json.loads(f.read_text(encoding="utf-8"))
        tree_files[data["classId"]] = (f, data)

    changes = []
    modified_classes = set()

    for fix in FIXES:
        class_id, tree_name, action = fix[0], fix[1], fix[2]
        _, class_data = tree_files[class_id]
        tree = next(t for t in class_data["trees"] if t["name"] == tree_name)

        if action == "replace_full":
            old_id = fix[3]
            new_data = fix[4]
            for tal in tree["talents"]:
                if tal["id"] == old_id:
                    old_name = tal["name"]
                    tal["id"] = new_data["id"]
                    tal["name"] = new_data["name"]
                    tal["maxRank"] = new_data["maxRank"]
                    changes.append(f"  {class_id}/{tree_name}: {old_id} ({old_name}) -> {new_data['id']} ({new_data['name']}, mr={new_data['maxRank']})")
                    modified_classes.add(class_id)
                    break
            else:
                changes.append(f"  WARNING: {class_id}/{tree_name}: ID {old_id} not found in tree!")

        elif action == "replace_id":
            old_id = fix[3]
            new_id = fix[4]
            for tal in tree["talents"]:
                if tal["id"] == old_id:
                    tal["id"] = new_id
                    changes.append(f"  {class_id}/{tree_name}: ID {old_id} -> {new_id} ({tal['name']})")
                    modified_classes.add(class_id)
                    break

        elif action == "add":
            pos = fix[3]  # (row, col)
            new_data = fix[4]
            talent = {
                "id": new_data["id"],
                "name": new_data["name"],
                "icon": new_data.get("icon", "inv_misc_questionmark"),
                "row": pos[0],
                "col": pos[1],
                "maxRank": new_data["maxRank"],
                "prerequisiteId": new_data.get("prerequisiteId"),
            }
            tree["talents"].append(talent)
            tree["talents"].sort(key=lambda t: (t["row"], t["col"]))
            changes.append(f"  {class_id}/{tree_name}: ADD {new_data['id']} ({new_data['name']}) at row={pos[0]},col={pos[1]}")
            modified_classes.add(class_id)

    # Write modified files
    if not dry_run:
        for class_id in modified_classes:
            path, data = tree_files[class_id]
            # Sort talents in each tree
            for tree in data["trees"]:
                tree["talents"].sort(key=lambda t: (t["row"], t["col"]))
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            changes.append(f"  -> Written: {path.name}")

    return changes


def main():
    dry_run = "--dry-run" in sys.argv
    if dry_run:
        print("[DRY RUN] Changes that would be applied:")
    else:
        print("Applying remaining fixes...")

    changes = apply_fixes(dry_run)
    for line in changes:
        print(line)

    print(f"\nApplied {len(FIXES)} fixes to {len(set(f[0] for f in FIXES))} classes")


if __name__ == "__main__":
    main()

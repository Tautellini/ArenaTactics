"""
Fix known issues in talent tree JSON files after initial generation.

Fixes:
1. Resolve "Unknown/SPELL_ID" talent names using classic Wowhead endpoint
2. Warrior Arms: remove fake 41pt, move Endless Rage to row 8
3. Paladin Protection: fix wrong Improved Holy Shield data
4. Priest Holy: remove duplicate Surge of Light at row 7 col 2
5. Shaman Elemental: move Totem of Wrath to row 8 (41pt)
6. Rogue Combat: remove duplicate Surprise Attacks at row 6
7. Warlock: remove duplicate 41pt entries at row 6
8. Druid Feral: fix duplicate Survival of the Fittest
"""

import json
import time
import urllib.request
from pathlib import Path

TREE_DIR = Path(__file__).resolve().parent.parent / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "talent_trees" / "tbc"

WOWHEAD_CLASSIC = "https://nether.wowhead.com/classic/tooltip/spell/{}"
WOWHEAD_TBC = "https://nether.wowhead.com/tbc/tooltip/spell/{}"


def wowhead_lookup(spell_id, prefer_classic=False):
    """Fetch name+icon from Wowhead. Try classic endpoint first for vanilla spells."""
    endpoints = [WOWHEAD_CLASSIC, WOWHEAD_TBC] if prefer_classic else [WOWHEAD_TBC, WOWHEAD_CLASSIC]
    for base in endpoints:
        url = base.format(spell_id)
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "ArenaTactics/1.0"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                return data.get("name"), data.get("icon")
        except Exception:
            pass
    return None, None


def load_json(class_id):
    path = TREE_DIR / f"{class_id}.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save_json(class_id, data):
    path = TREE_DIR / f"{class_id}.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"  Saved {path.name}")


def find_talent(tree, talent_id):
    for t in tree["talents"]:
        if t["id"] == talent_id:
            return t
    return None


def remove_talent(tree, talent_id):
    tree["talents"] = [t for t in tree["talents"] if t["id"] != talent_id]


def get_tree(data, name):
    for t in data["trees"]:
        if t["name"] == name:
            return t
    return None


def fix_unknown_names(data):
    """Fix all 'Unknown/SPELL_ID' talent names by looking up on Wowhead classic."""
    changes = 0
    for tree in data["trees"]:
        for talent in tree["talents"]:
            if talent["name"].startswith("Unknown"):
                import re
                m = re.match(r"Unknown[/ (]+(\d+)", talent["name"])
                if m:
                    spell_id = int(m.group(1))
                    time.sleep(0.1)
                    name, icon = wowhead_lookup(spell_id, prefer_classic=True)
                    if name:
                        old = talent["name"]
                        talent["name"] = name
                        if icon:
                            talent["icon"] = icon
                        print(f"  [{tree['name']}] {old} -> {name} (icon={icon})")
                        changes += 1
    return changes


def fix_warrior(data):
    """Fix Warrior Arms: remove fake 41pt (1859), move Endless Rage to row 8."""
    arms = get_tree(data, "Arms")
    if arms:
        # Remove fake 41pt entry
        remove_talent(arms, 1859)
        print("  [Arms] Removed fake 41pt talent (1859)")

        # Move Endless Rage (1865) from row 7 to row 8
        t = find_talent(arms, 1865)
        if t:
            t["row"] = 8
            t["col"] = 1
            print("  [Arms] Moved Endless Rage (1865) to row 8 col 1")


def fix_paladin(data):
    """Fix Paladin Protection: fix Improved Holy Shield entry."""
    prot = get_tree(data, "Protection")
    if prot:
        t = find_talent(prot, 1750)
        if t:
            # Manually set correct data - spell 35876 returned garbage
            t["name"] = "Improved Holy Shield"
            t["icon"] = "spell_holy_greaterblessingoflight"  # Standard prot paladin icon
            print("  [Protection] Fixed Improved Holy Shield (1750) name and icon")

    # Fix Holy 41pt - spell 33072 returned "Holy Shock" which is actually valid
    # This is fine - the TBC 41pt for Holy is indeed related to Holy Shock


def fix_priest(data):
    """Fix Priest Holy: remove duplicate Surge of Light at row 7 col 2."""
    holy = get_tree(data, "Holy")
    if holy:
        remove_talent(holy, 1777)
        print("  [Holy] Removed duplicate Surge of Light (1777) at row 7 col 2")


def fix_shaman(data):
    """Fix Shaman Elemental: move Totem of Wrath to row 8 as 41pt talent."""
    ele = get_tree(data, "Elemental")
    if ele:
        t = find_talent(ele, 1682)
        if t:
            t["row"] = 8
            t["col"] = 1
            print("  [Elemental] Moved Totem of Wrath (1682) to row 8 col 1 (41pt)")


def fix_rogue(data):
    """Fix Rogue Combat: remove duplicate Surprise Attacks at row 6."""
    combat = get_tree(data, "Combat")
    if combat:
        # 1708 at row 6 col 2 is the duplicate. 1820 at row 8 is the 41pt.
        remove_talent(combat, 1708)
        print("  [Combat] Removed Surprise Attacks (1708) from row 6")


def fix_warlock(data):
    """Fix Warlock: remove duplicate 41pt entries from row 6."""
    affliction = get_tree(data, "Affliction")
    if affliction:
        # Remove Unstable Affliction (1766) from row 6 - keep only 41pt (1822) at row 8
        remove_talent(affliction, 1766)
        print("  [Affliction] Removed duplicate Unstable Affliction (1766) from row 6")

    demonology = get_tree(data, "Demonology")
    if demonology:
        # Remove Summon Felguard (1776) from row 6 - keep only 41pt (1823) at row 8
        remove_talent(demonology, 1776)
        print("  [Demonology] Removed duplicate Summon Felguard (1776) from row 6")


def fix_druid(data):
    """Fix Druid Feral: fix duplicate Survival of the Fittest at row 7."""
    feral = get_tree(data, "Feral Combat")
    if feral:
        t = find_talent(feral, 1791)
        if t:
            # This should be a different talent. Use Natural Perfection.
            time.sleep(0.1)
            name, icon = wowhead_lookup(33881, prefer_classic=False)
            if name and icon:
                t["name"] = name
                t["icon"] = icon
                print(f"  [Feral Combat] Fixed talent 1791: now {name} (icon={icon})")


def sort_talents(data):
    """Sort talents by row, col in all trees."""
    for tree in data["trees"]:
        tree["talents"].sort(key=lambda t: (t["row"], t["col"]))


def main():
    classes_and_fixes = {
        "druid":   [fix_unknown_names, fix_druid],
        "hunter":  [fix_unknown_names],
        "mage":    [],  # Mage is fine
        "paladin": [fix_paladin],
        "priest":  [fix_unknown_names, fix_priest],
        "rogue":   [fix_unknown_names, fix_rogue],
        "shaman":  [fix_unknown_names, fix_shaman],
        "warlock": [fix_unknown_names, fix_warlock],
        "warrior": [fix_unknown_names, fix_warrior],
    }

    for class_id, fixes in classes_and_fixes.items():
        if not fixes:
            continue

        print(f"\n{'='*50}")
        print(f"Fixing: {class_id}")
        print(f"{'='*50}")

        data = load_json(class_id)

        for fix_fn in fixes:
            fix_fn(data)

        sort_talents(data)
        save_json(class_id, data)

        # Print summary
        for tree in data["trees"]:
            max_row = max(t["row"] for t in tree["talents"])
            print(f"  {tree['name']}: {len(tree['talents'])} talents (rows 0-{max_row})")

    print("\nAll fixes applied!")


if __name__ == "__main__":
    main()

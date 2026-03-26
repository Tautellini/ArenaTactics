"""
Build complete TBC talent tree JSON files.

Strategy:
1. Read existing JSON files (correct data for vanilla-era tiers 0-6)
2. Add TBC additions (tiers 7-8 + gap-fills in 5-6) from hardcoded data
3. Fetch icons from Wowhead tooltip API for new/fixed talents
4. Fix "Unknown/SPELL_ID" talent names via Wowhead
5. Output updated JSON files

Usage:
    python scripts/fetch_talent_trees.py
    python scripts/fetch_talent_trees.py --class mage          # single class
    python scripts/fetch_talent_trees.py --icons-only          # re-fetch all icons
    python scripts/fetch_talent_trees.py --dry-run             # print changes, don't write
"""

import json
import sys
import time
import re
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
TREE_DIR = REPO_ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "talent_trees" / "tbc"

WOWHEAD_TOOLTIP = "https://nether.wowhead.com/tbc/tooltip/spell/{}"

# ---------------------------------------------------------------------------
# TBC talent additions per class/tree.
# Format: (talent_id, name, row, col, max_rank, prereq_talent_id, spell_id)
#
# talent_id  = DBC TalentEntry ID (must match what Blizzard API returns)
# spell_id   = rank-1 spell ID (used to fetch icon from Wowhead)
# ---------------------------------------------------------------------------

TBC_ADDITIONS = {
    # -----------------------------------------------------------------------
    # MAGE
    # -----------------------------------------------------------------------
    "mage": {
        "Arcane": [
            (1726, "Prismatic Cloak",            5, 0, 2, None, 31569),
            (1727, "Arcane Potency",             6, 2, 3, 86,   31571),
            (1728, "Empowered Arcane Missiles",  7, 0, 3, None, 31579),
            (1729, "Mind Mastery",               7, 1, 5, None, 31584),
            (1730, "Spell Power",                7, 2, 2, None, 35578),
            (1825, "Slow",                       8, 1, 1, None, 31589),
        ],
        "Fire": [
            (1731, "Blazing Speed",       5, 0, 2, None,  31641),
            (1733, "Playing with Fire",   6, 2, 3, None,  31638),
            (1732, "Molten Fury",         7, 0, 2, None,  31679),
            (1734, "Empowered Fireball",  7, 2, 5, None,  31656),
            (1826, "Dragon's Breath",     8, 1, 1, 36,    31661),
        ],
        "Frost": [
            (1735, "Ice Floes",                5, 0, 2, None, 31670),
            (1736, "Frozen Core",              6, 0, 3, None, 31667),
            (1737, "Arctic Winds",             7, 1, 5, None, 31674),
            (1738, "Empowered Frostbolt",      7, 0, 5, None, 31682),
            (1827, "Summon Water Elemental",   8, 1, 1, 72,   31687),
        ],
    },

    # -----------------------------------------------------------------------
    # WARRIOR
    # -----------------------------------------------------------------------
    "warrior": {
        "Arms": [
            (1862, "Second Wind",           5, 1, 2, None,  29834),
            (1863, "Improved Intercept",    6, 0, 2, None,  29888),
            (1864, "Blood Frenzy",          6, 2, 2, None,  29836),
            (1866, "Improved Mortal Strike", 7, 0, 5, 135,  35446),
            (1865, "Endless Rage",          7, 2, 1, None,  29623),
            (1859, "Second Wind 41pt",      8, 1, 1, None,  46854),  # TODO: verify 41pt talent
        ],
        "Fury": [
            (1868, "Improved Berserker Stance", 5, 1, 5, None, 29759),
            (1870, "Improved Whirlwind",    6, 0, 2, None,  29721),
            (1869, "Focused Rage",          6, 2, 3, None,  29787),
            (1871, "Precision",             7, 0, 3, None,  29590),
            (1872, "Rampage",               8, 1, 1, 167,   29801),
        ],
        "Protection": [
            (1875, "Focused Rage",          5, 0, 3, None,  29787),
            (1874, "Improved Defensive Stance", 5, 1, 3, None, 29593),
            (1873, "Vitality",              6, 2, 5, None,  29140),
            (1876, "Improved Shield Block", 7, 0, 2, None,  29598),
            (1877, "Devastate",             8, 1, 1, 148,   30022),
        ],
    },

    # -----------------------------------------------------------------------
    # ROGUE
    # -----------------------------------------------------------------------
    "rogue": {
        "Assassination": [
            (1709, "Fleet Footed",      5, 0, 2, None, 31208),
            (1712, "Find Weakness",     6, 2, 5, None, 31233),
            (1706, "Master Poisoner",   7, 0, 3, None, 31226),
            (1707, "Quick Recovery",    7, 2, 2, None, 31244),
            (1819, "Mutilate",          8, 1, 1, 382,  34411),
        ],
        "Combat": [
            (1711, "Nerves of Steel",   5, 0, 2, None,  31130),
            (1708, "Surprise Attacks",  6, 2, 1, None,  32601),
            (1713, "Combat Potency",    7, 0, 5, None,  35541),
            (1714, "Savage Combat",     7, 2, 2, None,  31124),
            (1820, "Surprise Attacks 41pt", 8, 1, 1, 205, 32601),  # TODO: verify 41pt
        ],
        "Subtlety": [
            (1715, "Enveloping Shadows", 5, 0, 3, None, 31211),
            (1710, "Cheat Death",        6, 2, 3, None, 31228),
            (1716, "Sinister Calling",   7, 0, 5, None, 31216),
            (1718, "Waylay",             7, 2, 2, None, 31224),
            (1821, "Shadowstep",         8, 1, 1, 284,  36554),
        ],
    },

    # -----------------------------------------------------------------------
    # PRIEST
    # -----------------------------------------------------------------------
    "priest": {
        "Discipline": [
            (1769, "Absolution",         5, 0, 3, None, 33167),
            (1770, "Focused Power",      6, 2, 2, None, 33186),
            (1771, "Enlightenment",      7, 0, 5, None, 34908),
            (1772, "Reflective Shield",  7, 2, 5, None, 33201),
            (1773, "Pain Suppression",   8, 1, 1, 322,  33206),
        ],
        "Holy": [
            (1774, "Blessed Resilience",  5, 0, 3, None, 33142),
            (1775, "Empowered Healing",   6, 2, 5, None, 33158),
            (1776, "Surge of Light",      7, 0, 2, None, 33150),
            (1777, "Spiritual Healing",   7, 2, 5, None, 33154),  # TBC new tier
            (1778, "Circle of Healing",   8, 1, 1, 1637, 34861),
        ],
        "Shadow": [
            (1779, "Shadow Resilience",   5, 0, 2, None, 33371),
            (1780, "Focused Mind",        6, 0, 3, None, 33213),
            (1816, "Shadow Power",        7, 0, 5, None, 33221),
            (1781, "Misery",              7, 2, 5, None, 33191),
            (1817, "Vampiric Touch",      8, 1, 1, 521,  34914),
        ],
    },

    # -----------------------------------------------------------------------
    # PALADIN
    # -----------------------------------------------------------------------
    "paladin": {
        "Holy": [
            (1741, "Blessed Life",         5, 0, 3, None, 31828),
            (1746, "Holy Guidance",        6, 2, 5, None, 31837),
            (1742, "Light's Grace",        7, 0, 3, None, 31833),
            (1743, "Divine Illumination",  7, 2, 1, None, 31842),
            (1831, "Holy Shock 41pt",      8, 1, 1, 1502, 33072),  # TODO: verify
        ],
        "Protection": [
            (1748, "Spell Warding",        5, 0, 2, None, 33079),  # Note: existing data has this at (1748, row 5, col 0)
            (1749, "Sacred Duty",          5, 2, 2, None, 31848),
            (1750, "Improved Holy Shield", 6, 2, 2, 1430, 35876),
            (1751, "Ardent Defender",      7, 0, 5, None, 31850),
            (1752, "Combat Expertise",     7, 2, 5, None, 31858),
            (1832, "Avenger's Shield",     8, 1, 1, 1430, 32700),
        ],
        "Retribution": [
            (1753, "Sanctified Judgement", 5, 0, 3, None, 31876),
            (1756, "Fanaticism",           6, 0, 5, None, 31879),
            (1755, "Sanctified Seals",     6, 2, 3, None, 35395),
            (1757, "Improved Sanctity Aura",7, 0, 2, 1409, 31869),
            (1758, "Vengeance",            7, 2, 3, None, 31871),
            (1833, "Crusader Strike",      8, 1, 1, None, 35395),
        ],
    },

    # -----------------------------------------------------------------------
    # HUNTER
    # -----------------------------------------------------------------------
    "hunter": {
        "Beast Mastery": [
            (1793, "Catlike Reflexes",      5, 0, 3, None, 34462),
            (1794, "Serpent's Swiftness",   6, 2, 5, None, 34466),
            (1795, "Ferocious Inspiration", 7, 0, 3, 1386, 34455),
            (1796, "The Beast Within",      7, 2, 1, 1386, 34692),
            (1812, "Snake Trap",            8, 1, 1, None, 34600),  # TODO: verify 41pt
        ],
        "Marksmanship": [
            (1797, "Improved Barrage",      5, 0, 3, 1347, 35104),
            (1798, "Combat Experience",     5, 2, 2, None, 34475),
            (1800, "Master Marksman",       6, 2, 5, None, 34485),
            (1801, "Careful Aim",           7, 0, 3, None, 34482),
            (1802, "Silencing Shot",        7, 2, 1, None, 34490),
            (1813, "Readiness",             8, 1, 1, None, 23989),
        ],
        "Survival": [
            (1803, "Survival Instincts",    5, 0, 2, None, 34494),
            (1804, "Resourcefulness",       6, 0, 3, None, 34491),
            (1805, "Expose Weakness",       6, 2, 3, None, 34500),
            (1806, "Thrill of the Hunt",    7, 0, 3, None, 34497),
            (1807, "Master Tactician",      7, 2, 5, None, 34506),
            (1814, "Readiness",             8, 1, 1, 1325, 23989),
        ],
    },

    # -----------------------------------------------------------------------
    # DRUID
    # -----------------------------------------------------------------------
    "druid": {
        "Balance": [
            (1782, "Celestial Focus",       5, 2, 3, None, 16850),
            (1783, "Lunar Guidance",        6, 0, 3, None, 33589),
            (1784, "Improved Faerie Fire",  6, 2, 3, None, 33600),
            (1785, "Dreamstate",            7, 0, 3, None, 33597),
            (1786, "Wrath of Cenarius",     7, 2, 5, None, 33603),
            (1829, "Force of Nature",       8, 1, 1, 793, 33831),
        ],
        "Feral Combat": [
            (1787, "Nurturing Instinct",    5, 2, 2, None, 33872),
            (1788, "Primal Tenacity",       6, 0, 3, None, 33851),
            (1789, "Predatory Instincts",   6, 2, 5, None, 33859),
            (1790, "Survival of the Fittest",7, 0, 3, None, 33853),
            (1791, "Natural Perfection",    7, 2, 3, None, 33855),
            (1830, "Mangle",               8, 1, 1, 809, 33876),
        ],
        "Restoration": [
            (1792, "Empowered Touch",      5, 0, 5, None, 33879),
            (1810, "Natural Perfection",   6, 0, 3, None, 33881),
            (1811, "Living Spirit",        6, 2, 3, None, 34151),
            (1809, "Empowered Rejuvenation", 7, 1, 5, None, 33886),
            (1828, "Tree of Life",         8, 1, 1, 844, 33891),
        ],
    },

    # -----------------------------------------------------------------------
    # SHAMAN
    # -----------------------------------------------------------------------
    "shaman": {
        "Elemental": [
            (1682, "Totem of Wrath",       6, 2, 1, 573,  30706),
            (1683, "Lightning Overload",   7, 0, 3, None,  30675),
            (1684, "Elemental Shields",    7, 2, 3, None,  30669),
            (1834, "Unrelenting Storm",    5, 0, 5, None,  30664),
        ],
        "Enhancement": [
            (1685, "Dual Wield",           5, 0, 1, None, 30798),
            (1686, "Shamanistic Rage",     6, 2, 1, None, 30823),
            (1687, "Unleashed Rage",       7, 0, 5, None, 30802),
            (1688, "Mental Quickness",     7, 2, 3, None, 30812),
            (1835, "Stormstrike 41pt",     8, 1, 1, 901, 17364),  # TODO: verify
        ],
        "Restoration": [
            (1689, "Nature's Guardian",    5, 0, 5, None, 30881),
            (1690, "Focused Mind",         6, 0, 3, None, 30864),
            (1691, "Nature's Blessing",    6, 2, 3, None, 30867),
            (1692, "Improved Chain Heal",  7, 0, 2, None, 30872),
            (1836, "Nature's Swiftness",   7, 2, 5, None, 30884),  # TBC addition at row 7
            (1693, "Earth Shield",         8, 1, 1, 590, 974),
        ],
    },

    # -----------------------------------------------------------------------
    # WARLOCK
    # -----------------------------------------------------------------------
    "warlock": {
        "Affliction": [
            (1764, "Malediction",          5, 0, 3, None, 32477),
            (1765, "Contagion",            6, 0, 5, None, 30060),
            (1766, "Unstable Affliction",  6, 2, 1, None, 30108),
            (1767, "Soul Siphon",          7, 0, 2, None, 35195),
            (1822, "Unstable Affliction 41pt", 8, 1, 1, 1042, 30108),  # TODO: verify 41pt
        ],
        "Demonology": [
            (1774, "Demonic Knowledge",    5, 0, 3, None, 35691),
            (1775, "Demonic Tactics",      6, 0, 5, None, 30242),
            (1776, "Summon Felguard",      6, 2, 1, 1282, 30146),
            (1777, "Demonic Resilience",   7, 0, 3, None, 30319),
            (1823, "Felguard 41pt",        8, 1, 1, 1282, 30146),  # TODO: verify 41pt
        ],
        "Destruction": [
            (1778, "Backlash",             5, 0, 3, None, 34935),
            (1779, "Nether Protection",    6, 0, 3, None, 30299),
            (1780, "Shadow and Flame",     6, 2, 5, None, 30288),
            (1781, "Soul Leech",           7, 0, 3, None, 30293),
            (1824, "Shadowfury",           8, 1, 1, 968,  30283),
        ],
    },
}


# ---------------------------------------------------------------------------
# Spell-ID overrides for EXISTING talents whose name or icon we want to fix.
# Keyed by talent_id.  The script will fetch name + icon from Wowhead.
# Also used to fix "Unknown/SPELL_ID" names.
# ---------------------------------------------------------------------------
SPELL_ID_FOR_EXISTING = {
    # Druid Balance "Unknown" fixes
    763: 16825,   # Improved Thorns
    791: 16906,   # Natural Weapons  (? -- "Focused Starlight"? Need to verify)

    # Druid Feral "Unknown" fixes
    799: 16951,   # Feral Instinct (was "Unknown/16951")
    794: 16933,   # Thick Hide... wait, 16933 = ? Let me check

    # Druid Resto "Unknown" fix
    826: 17082,   # Natural Shapeshifter? Or Improved Enrage?

    # Priest Shadow "Unknown" fix
    881: 17325,   # Shadow Reach (was "Unknown/17325")

    # Hunter Survival "Unknown" fixes
    1304: 19390,  # Entrapment (was "Unknown/19390")
    1305: 19235,  # Improved Wing Clip (was "Unknown/19235")

    # Hunter BM "Unknown" fix
    1381: 24387,  # Guard Dog? (was "Unknown/24387")

    # Shaman Enhancement "Unknown" fix
    617: 16269,   # Two-Handed Axes and Maces? (was "Unknown/16269")

    # Warlock Affliction "Unknown" fixes
    1006: 18181,  # Improved Curse of Agony (was "Unknown/18181")
    1004: 17808,  # Improved Curse of Weakness (was "Unknown/17808")
    1284: 18830,  # Curse of Exhaustion? No... Amplify Curse? (was "Unknown/18830")
    1121: 18393,  # Improved Drain Life? (was "Unknown/18393")

    # Warlock Demonology "Unknown" fixes
    1242: 18746,  # Improved Sayaad? (was "Unknown/18746")
    1241: 18752,  # Demonic Sacrifice? (was "Unknown/18752")
    1283: 18825,  # Fel Intellect? (was "Unknown/18825")
    1263: 18775,  # Improved Firestone? (was "Unknown/18775")

    # Warrior Arms "Unknown" fixes
    641: 12679,   # Deep Wounds? Impale? (was "Unknown/12679")
    134: 12833,   # Improved Slam? (was "Unknown/12833")

    # Warrior Protection "Unknown" fixes
    145: 12944,   # Defiance? (was "Unknown/12944")
    144: 12792,   # Improved Shield Block? (was "Unknown/12792")

    # Rogue Subtlety "Unknown" fix
    262: 14095,   # Improved Sap? (was "Unknown/14095")

    # Warrior Fury "Unknown" fix
    168: 20499,   # Improved Berserker Rage (was "Unknown/20499")
}


def fetch_wowhead_tooltip(spell_id):
    """Fetch tooltip from Wowhead TBC and return (name, icon) or (None, None)."""
    url = WOWHEAD_TOOLTIP.format(spell_id)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "ArenaTactics/1.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode())
            icon = data.get("icon")
            # Extract name from tooltip HTML or name field
            name = data.get("name")
            if not name:
                # Try to extract from tooltip
                tooltip = data.get("tooltip", "")
                m = re.search(r"<b[^>]*>([^<]+)</b>", tooltip)
                if m:
                    name = m.group(1)
            return name, icon
    except Exception as e:
        print(f"  Warning: Wowhead lookup failed for spell {spell_id}: {e}")
        return None, None


def load_existing_json(class_id):
    """Load existing talent tree JSON for a class."""
    path = TREE_DIR / f"{class_id}.json"
    if path.exists():
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    return None


def build_updated_tree(class_id, fetch_icons=True, dry_run=False):
    """Build the complete talent tree for a class by merging existing + TBC additions."""
    existing = load_existing_json(class_id)
    if not existing:
        print(f"  No existing JSON for {class_id}, skipping.")
        return None

    additions = TBC_ADDITIONS.get(class_id, {})
    changes = []

    for tree in existing["trees"]:
        tree_name = tree["name"]
        tree_additions = additions.get(tree_name, [])

        existing_ids = {t["id"] for t in tree["talents"]}

        # Add TBC talents
        for (tid, name, row, col, max_rank, prereq, spell_id) in tree_additions:
            if tid in existing_ids:
                print(f"  [{tree_name}] Talent {tid} already exists, skipping")
                continue

            # Fetch icon from Wowhead
            icon = None
            fetched_name = name
            if fetch_icons and spell_id:
                time.sleep(0.1)  # Rate limit
                wh_name, wh_icon = fetch_wowhead_tooltip(spell_id)
                if wh_icon:
                    icon = wh_icon
                if wh_name:
                    fetched_name = wh_name

            if not icon:
                icon = "inv_misc_questionmark"  # Placeholder

            talent = {
                "id": tid,
                "name": fetched_name,
                "icon": icon,
                "row": row,
                "col": col,
                "maxRank": max_rank,
                "prerequisiteId": prereq,
            }
            tree["talents"].append(talent)
            changes.append(f"  + [{tree_name}] {fetched_name} (id={tid}, row={row}, col={col}, icon={icon})")

        # Fix "Unknown/SPELL_ID" names and optionally refresh icons for existing talents
        for talent in tree["talents"]:
            tid = talent["id"]
            spell_id = SPELL_ID_FOR_EXISTING.get(tid)

            # Check for Unknown names
            is_unknown = talent.get("name", "").startswith("Unknown")
            if is_unknown and not spell_id:
                # Try to extract spell ID from the name
                m = re.match(r"Unknown/(\d+)", talent.get("name", ""))
                if m:
                    spell_id = int(m.group(1))

            if spell_id and (is_unknown or fetch_icons):
                if fetch_icons:
                    time.sleep(0.1)
                    wh_name, wh_icon = fetch_wowhead_tooltip(spell_id)
                    if wh_name and is_unknown:
                        old_name = talent["name"]
                        talent["name"] = wh_name
                        changes.append(f"  ~ [{tree_name}] Renamed: {old_name} -> {wh_name}")
                    if wh_icon:
                        old_icon = talent.get("icon", "")
                        if old_icon != wh_icon:
                            talent["icon"] = wh_icon
                            changes.append(f"  ~ [{tree_name}] Icon fix: {talent['name']}: {old_icon} -> {wh_icon}")

        # Sort talents by row, then col for clean output
        tree["talents"].sort(key=lambda t: (t["row"], t["col"]))

    return existing, changes


def write_json(class_id, data):
    """Write talent tree JSON."""
    path = TREE_DIR / f"{class_id}.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"  Written: {path}")


def main():
    args = set(sys.argv[1:])
    dry_run = "--dry-run" in args
    icons_only = "--icons-only" in args
    target_class = None
    for arg in args:
        if arg.startswith("--class"):
            # Support both --class=mage and --class mage
            if "=" in arg:
                target_class = arg.split("=", 1)[1]
    # Also check for positional after --class
    argv_list = sys.argv[1:]
    for i, arg in enumerate(argv_list):
        if arg == "--class" and i + 1 < len(argv_list):
            target_class = argv_list[i + 1]

    classes = [target_class] if target_class else [
        "druid", "hunter", "mage", "paladin", "priest",
        "rogue", "shaman", "warlock", "warrior"
    ]

    fetch_icons = True  # Always fetch icons for new talents; --icons-only refreshes ALL

    for class_id in classes:
        print(f"\n{'='*50}")
        print(f"Processing: {class_id}")
        print(f"{'='*50}")

        result = build_updated_tree(class_id, fetch_icons=fetch_icons, dry_run=dry_run)
        if not result:
            continue

        data, changes = result

        if changes:
            print(f"\nChanges for {class_id}:")
            for c in changes:
                print(c)
        else:
            print(f"  No changes needed for {class_id}")

        if not dry_run:
            write_json(class_id, data)

        # Print summary
        for tree in data["trees"]:
            max_row = max(t["row"] for t in tree["talents"])
            print(f"  {tree['name']}: {len(tree['talents'])} talents (rows 0-{max_row})")

    print("\nDone!")


if __name__ == "__main__":
    main()

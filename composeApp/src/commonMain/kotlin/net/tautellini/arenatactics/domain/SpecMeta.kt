package net.tautellini.arenatactics.domain

import net.tautellini.arenatactics.data.model.PlayerProfile
import net.tautellini.arenatactics.data.model.TalentSelection

/**
 * Aggregated meta data for a specialization, computed at runtime from player profiles.
 */
data class SpecMeta(
    val specId: String,
    val sampleSize: Int,
    val slotBreakdowns: List<SlotBreakdown>,
    val popularTalentBuilds: List<TalentBuildEntry>
)

data class SlotBreakdown(
    val slot: String,
    val items: List<ItemUsage>,
    val enchants: List<EnchantUsage>
)

data class ItemUsage(
    val itemId: Int,
    val name: String,
    val quality: String?,
    val count: Int,
    val percentage: Double
)

data class EnchantUsage(
    val name: String,
    val count: Int,
    val percentage: Double
)

data class TalentBuildEntry(
    val label: String,          // e.g. "Frost (44) / Arcane (17)"
    val trees: List<Pair<String, Int>>,  // treeName to spentPoints
    val count: Int,
    val percentage: Double,
    val talentSelections: List<TalentSelection> = emptyList()  // representative build's individual talents
)

private val SLOT_ORDER = listOf(
    "HEAD", "NECK", "SHOULDER", "BACK", "CHEST", "WRIST",
    "HANDS", "WAIST", "LEGS", "FEET",
    "FINGER_1", "FINGER_2", "TRINKET_1", "TRINKET_2",
    "MAIN_HAND", "OFF_HAND", "RANGED"
)

fun computeSpecMeta(specId: String, players: List<PlayerProfile>): SpecMeta {
    val matching = players.filter { it.specId == specId }
    val total = matching.size
    if (total == 0) return SpecMeta(specId, 0, emptyList(), emptyList())

    // ── Slot breakdowns ──
    val slotItems = mutableMapOf<String, MutableList<Pair<Int, String>>>()     // slot → (itemId, name) list
    val slotQualities = mutableMapOf<Pair<String, Int>, String?>()              // (slot, itemId) → quality
    val slotEnchants = mutableMapOf<String, MutableList<String>>()              // slot → enchant names

    for (player in matching) {
        for (item in player.equipment) {
            slotItems.getOrPut(item.slot) { mutableListOf() }.add(item.itemId to item.name)
            slotQualities[item.slot to item.itemId] = item.quality
            if (!item.enchant.isNullOrEmpty()) {
                slotEnchants.getOrPut(item.slot) { mutableListOf() }.add(item.enchant!!)
            }
        }
    }

    val slotBreakdowns = SLOT_ORDER.mapNotNull { slot ->
        val items = slotItems[slot] ?: return@mapNotNull null
        val itemCounts = items.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10)
            .map { (pair, count) ->
                ItemUsage(
                    itemId = pair.first,
                    name = pair.second,
                    quality = slotQualities[slot to pair.first],
                    count = count,
                    percentage = (count * 1000L / total) / 10.0
                )
            }

        val enchants = slotEnchants[slot]?.let { list ->
            list.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { (name, count) ->
                    EnchantUsage(name, count, (count * 1000L / total) / 10.0)
                }
        } ?: emptyList()

        SlotBreakdown(slot, itemCounts, enchants)
    }

    // ── Talent builds ──
    data class BuildData(
        val trees: List<Pair<String, Int>>,
        val count: Int,
        val talents: List<TalentSelection>  // representative build (first seen)
    )
    val buildCounts = mutableMapOf<String, BuildData>()
    for (player in matching) {
        val activeGroup = player.talentGroups.firstOrNull { it.isActive } ?: continue
        val trees = activeGroup.specializations.map { it.treeName to it.spentPoints }
        val label = trees.joinToString(" / ") { "${it.first} (${it.second})" }
        val existing = buildCounts[label]
        if (existing != null) {
            buildCounts[label] = existing.copy(count = existing.count + 1)
        } else {
            val allTalents = activeGroup.specializations.flatMap { it.talents }
            buildCounts[label] = BuildData(trees, 1, allTalents)
        }
    }

    val talentBuilds = buildCounts.entries
        .sortedByDescending { it.value.count }
        .take(5)
        .map { (label, data) ->
            TalentBuildEntry(label, data.trees, data.count, (data.count * 1000L / total) / 10.0, data.talents)
        }

    return SpecMeta(specId, total, slotBreakdowns, talentBuilds)
}

package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.EquippedItem
import net.tautellini.arenatactics.data.model.PlayerProfile
import net.tautellini.arenatactics.data.model.PvpBracketRating
import net.tautellini.arenatactics.data.model.TalentGroup
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.PlayerDetailState
import net.tautellini.arenatactics.presentation.PlayerDetailViewModel
import net.tautellini.arenatactics.presentation.theme.*

// ─── Icon maps (shared with LadderScreen via convention — extract later if needed) ──

private val CLASS_ICON_NAMES = mapOf(
    "warrior" to "classicon_warrior", "paladin" to "classicon_paladin",
    "hunter" to "classicon_hunter", "rogue" to "classicon_rogue",
    "priest" to "classicon_priest", "deathknight" to "classicon_deathknight",
    "shaman" to "classicon_shaman", "mage" to "classicon_mage",
    "warlock" to "classicon_warlock", "monk" to "classicon_monk",
    "druid" to "classicon_druid", "demonhunter" to "classicon_demonhunter",
    "evoker" to "classicon_evoker",
)

private val SPEC_ICON_NAMES = mapOf(
    "druid_balance" to "spell_nature_starfall", "druid_feral" to "ability_druid_catform",
    "druid_restoration" to "spell_nature_healingtouch",
    "hunter_beastmastery" to "ability_hunter_bestialdiscipline",
    "hunter_marksmanship" to "ability_hunter_focusedaim", "hunter_survival" to "ability_hunter_camouflage",
    "mage_arcane" to "spell_holy_magicalsentry", "mage_fire" to "spell_fire_firebolt02",
    "mage_frost" to "spell_frost_frostbolt02",
    "paladin_holy" to "spell_holy_holybolt", "paladin_protection" to "ability_paladin_shieldofthetemplar",
    "paladin_retribution" to "spell_holy_auraoflight",
    "priest_discipline" to "spell_holy_powerwordshield", "priest_holy" to "spell_holy_guardianspirit",
    "priest_shadow" to "spell_shadow_shadowwordpain",
    "rogue_assassination" to "ability_rogue_deadlybrew", "rogue_combat" to "ability_backstab",
    "rogue_outlaw" to "ability_rogue_waylay", "rogue_subtlety" to "ability_stealth",
    "shaman_elemental" to "spell_nature_lightning", "shaman_enhancement" to "spell_shaman_improvedstormstrike",
    "shaman_restoration" to "spell_nature_magicimmunity",
    "warlock_affliction" to "spell_shadow_deathcoil", "warlock_demonology" to "spell_shadow_metamorphosis",
    "warlock_destruction" to "spell_shadow_rainoffire",
    "warrior_arms" to "ability_warrior_savageblow", "warrior_fury" to "ability_warrior_innerrage",
    "warrior_protection" to "ability_warrior_defensivestance",
)

private val QualityColors = mapOf(
    "LEGENDARY" to Color(0xFFFF8000),
    "EPIC" to Color(0xFFA335EE),
    "RARE" to Color(0xFF0070DD),
    "UNCOMMON" to Color(0xFF1EFF00),
    "COMMON" to Color(0xFFFFFFFF),
)

private fun specDisplayName(specId: String): String {
    val parts = specId.split("_", limit = 2)
    if (parts.size < 2) return specId
    return "${parts[1].replaceFirstChar { it.uppercase() }} ${parts[0].replaceFirstChar { it.uppercase() }}"
}

// ─── Slot ordering for equipment display ──

private val SLOT_ORDER = listOf(
    "HEAD", "NECK", "SHOULDER", "BACK", "CHEST", "SHIRT", "TABARD",
    "WRIST", "HANDS", "WAIST", "LEGS", "FEET",
    "FINGER_1", "FINGER_2", "TRINKET_1", "TRINKET_2",
    "MAIN_HAND", "OFF_HAND", "RANGED"
)

private fun slotDisplayName(slot: String) = slot.lowercase()
    .replace("_", " ")
    .replace("finger 1", "Ring 1").replace("finger 2", "Ring 2")
    .replace("trinket 1", "Trinket 1").replace("trinket 2", "Trinket 2")
    .replace("main hand", "Main Hand").replace("off hand", "Off Hand")
    .replaceFirstChar { it.uppercase() }

// ─── Screen ──

@Composable
fun PlayerDetailScreen(viewModel: PlayerDetailViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is PlayerDetailState.Loading -> Box(
            Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Primary) }

        is PlayerDetailState.Error -> Box(
            Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) { Text(s.message, color = TextSecondary) }

        is PlayerDetailState.Success -> PlayerDetailContent(s.player)
    }
}

@Composable
private fun PlayerDetailContent(player: PlayerProfile) {
    val classClr = player.classId?.let { classColor(it) } ?: TextPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Player Header ──
        PlayerHeader(player, classClr)

        // ── PvP Ratings ──
        if (player.pvpBrackets.isNotEmpty()) {
            SectionTitle("PvP Ratings")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                player.pvpBrackets.forEach { (bracket, rating) ->
                    BracketRatingCard(bracket, rating)
                }
            }
        }

        // ── Equipment ──
        if (player.equipment.isNotEmpty()) {
            SectionTitle("Equipment")
            val sorted = player.equipment.sortedBy { SLOT_ORDER.indexOf(it.slot).let { i -> if (i < 0) 99 else i } }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorted.forEach { item ->
                    EquipmentItemCard(item)
                }
            }
        }

        // ── Talents ──
        val activeGroup = player.talentGroups.firstOrNull { it.isActive }
        if (activeGroup != null && activeGroup.specializations.isNotEmpty()) {
            SectionTitle("Talents")
            TalentTreeCard(activeGroup)

            // Show secondary spec if it exists
            val secondaryGroup = player.talentGroups.firstOrNull { !it.isActive }
            if (secondaryGroup != null && secondaryGroup.specializations.isNotEmpty()) {
                Text("Secondary Spec", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                TalentTreeCard(secondaryGroup)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerHeader(player: PlayerProfile, classClr: Color) {
    val shape = RoundedCornerShape(16.dp)
    Surface(color = CardColor, shape = shape) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spec icon
            val specIcon = player.specId?.let { SPEC_ICON_NAMES[it] }
                ?: player.classId?.let { CLASS_ICON_NAMES[it] }
            if (specIcon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(classClr)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = WowheadIcons.large(specIcon),
                        contentDescription = player.specId ?: player.classId,
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(player.name, color = classClr, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                val subtitle = buildList {
                    player.specId?.let { add(specDisplayName(it)) }
                        ?: player.classId?.let { add(it.replaceFirstChar { c -> c.uppercase() }) }
                    player.race?.let { add(it) }
                    if (player.realmSlug.isNotEmpty()) {
                        add(player.realmSlug.replace("-", " ").split(" ").joinToString(" ") {
                            it.replaceFirstChar { c -> c.uppercase() }
                        })
                    }
                }.joinToString(" · ")
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)

                if (!player.guild.isNullOrEmpty()) {
                    Text("< ${player.guild} >", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp)
                }

                if (!player.faction.isNullOrEmpty()) {
                    val factionColor = if (player.faction == "ALLIANCE") Color(0xFF3B82F6) else Color(0xFFEF4444)
                    Text(
                        player.faction!!.lowercase().replaceFirstChar { it.uppercase() },
                        color = factionColor, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BracketRatingCard(bracket: String, rating: PvpBracketRating) {
    val shape = RoundedCornerShape(12.dp)
    Surface(color = CardColor, shape = shape) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(bracket.uppercase(), color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            Text("${rating.rating}", color = Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            val total = rating.wins + rating.losses
            val winRate = if (total > 0) "${((rating.wins * 1000L / total) / 10.0)}%" else "-"
            if (rating.rank != null) {
                Text("Rank #${rating.rank}", color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Text("${rating.wins}W ${rating.losses}L", color = TextSecondary, fontSize = 12.sp)
            Text(winRate, color = TextSecondary.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun EquipmentItemCard(item: EquippedItem) {
    val qualityColor = QualityColors[item.quality] ?: TextPrimary
    val shape = RoundedCornerShape(10.dp)

    Surface(color = CardColor, shape = shape, modifier = Modifier.widthIn(min = 260.dp, max = 400.dp)) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item icon from Wowhead
            if (item.itemId > 0) {
                AsyncImage(
                    model = WowheadIcons.medium("inv_misc_questionmark"),
                    contentDescription = item.name,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                        .border(1.dp, qualityColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    slotDisplayName(item.slot),
                    color = TextSecondary, fontSize = 10.sp
                )
                Text(
                    item.name,
                    color = qualityColor, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!item.enchant.isNullOrEmpty()) {
                    Text(item.enchant!!, color = Color(0xFF1EFF00), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (item.gems.isNotEmpty()) {
                    Text(
                        item.gems.joinToString(", "),
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TalentTreeCard(group: TalentGroup) {
    val shape = RoundedCornerShape(12.dp)
    val totalPoints = group.specializations.sumOf { it.spentPoints }
    val maxPoints = group.specializations.maxOfOrNull { it.spentPoints } ?: 1

    Surface(color = CardColor, shape = shape) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary line: "Frost (40) / Arcane (21)"
            Text(
                group.specializations.joinToString(" / ") { "${it.treeName} (${it.spentPoints})" },
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )

            // Visual bars per tree
            group.specializations.forEach { spec ->
                if (spec.spentPoints > 0) {
                    val fraction = spec.spentPoints.toFloat() / totalPoints.coerceAtLeast(1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    ) {
                        Text(
                            spec.treeName,
                            color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f).height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Primary.copy(alpha = 0.7f))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${spec.spentPoints}",
                            color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                    }
                }
            }
        }
    }
}

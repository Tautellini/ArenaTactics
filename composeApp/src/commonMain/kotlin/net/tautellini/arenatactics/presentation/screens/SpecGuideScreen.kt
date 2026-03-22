package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
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
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.domain.EnchantUsage
import net.tautellini.arenatactics.domain.ItemUsage
import net.tautellini.arenatactics.domain.SlotBreakdown
import net.tautellini.arenatactics.domain.SpecMeta
import net.tautellini.arenatactics.domain.TalentBuildEntry
import net.tautellini.arenatactics.presentation.SpecGuideState
import net.tautellini.arenatactics.presentation.SpecGuideViewModel
import net.tautellini.arenatactics.presentation.theme.*

private val QualityColors = mapOf(
    "LEGENDARY" to Color(0xFFFF8000),
    "EPIC" to Color(0xFFA335EE),
    "RARE" to Color(0xFF0070DD),
    "UNCOMMON" to Color(0xFF1EFF00),
    "COMMON" to Color(0xFFFFFFFF),
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

private fun slotDisplayName(slot: String) = slot.lowercase()
    .replace("_", " ")
    .replace("finger 1", "Ring 1").replace("finger 2", "Ring 2")
    .replace("trinket 1", "Trinket 1").replace("trinket 2", "Trinket 2")
    .replace("main hand", "Main Hand").replace("off hand", "Off Hand")
    .replaceFirstChar { it.uppercase() }

@Composable
fun SpecGuideScreen(viewModel: SpecGuideViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SpecGuideState.Loading -> Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        is SpecGuideState.Error -> Box(Modifier.fillMaxSize().background(Background).padding(24.dp)) {
            Text(s.message, color = TextSecondary)
        }
        is SpecGuideState.Success -> SpecGuideContent(s.spec, s.wowClass, s.meta)
    }
}

@Composable
private fun SpecGuideContent(spec: WowSpec, wowClass: WowClass, meta: SpecMeta) {
    val classClr = classColor(wowClass.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val specIcon = SPEC_ICON_NAMES[spec.id]
            if (specIcon != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(classClr)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = WowheadIcons.large(specIcon),
                        contentDescription = spec.name,
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            Column {
                Text("${spec.name} ${wowClass.name}", color = classClr, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Based on ${meta.sampleSize} top-rated players (US + EU)",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
        }

        if (meta.sampleSize == 0) {
            Text("No player data available for this spec yet.", color = TextSecondary)
            return@Column
        }

        // Talent builds
        if (meta.popularTalentBuilds.isNotEmpty()) {
            SectionTitle("Popular Talent Builds")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                meta.popularTalentBuilds.forEach { build ->
                    TalentBuildRow(build, meta.sampleSize)
                }
            }
        }

        // Equipment per slot
        if (meta.slotBreakdowns.isNotEmpty()) {
            SectionTitle("Equipment by Slot")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                meta.slotBreakdowns.forEach { slot ->
                    SlotCard(slot)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun TalentBuildRow(build: TalentBuildEntry, total: Int) {
    val shape = RoundedCornerShape(12.dp)
    Surface(color = CardColor, shape = shape, modifier = Modifier.widthIn(min = 240.dp, max = 360.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(build.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${build.percentage}%", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Visual bars
            val maxPts = build.trees.maxOfOrNull { it.second } ?: 1
            build.trees.forEach { (tree, pts) ->
                if (pts > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    ) {
                        Text(tree, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        Box(
                            modifier = Modifier.weight(1f).height(12.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Background)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxHeight()
                                    .fillMaxWidth(pts.toFloat() / maxPts.coerceAtLeast(1))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.7f))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$pts", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(slot: SlotBreakdown) {
    val shape = RoundedCornerShape(12.dp)
    Surface(color = CardColor, shape = shape, modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                slotDisplayName(slot.slot),
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )

            // Items
            slot.items.forEach { item ->
                ItemRow(item)
            }

            // Enchants
            if (slot.enchants.isNotEmpty()) {
                Text("Enchants", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                slot.enchants.forEach { enchant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(enchant.name, color = Color(0xFF1EFF00), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${enchant.percentage}%", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemUsage) {
    val qualityColor = QualityColors[item.quality] ?: TextPrimary
    val fraction = (item.percentage / 100.0).coerceIn(0.0, 1.0).toFloat()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Usage bar (background)
        Box(modifier = Modifier.weight(1f)) {
            // Bar background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(6.dp))
                        .background(qualityColor.copy(alpha = 0.15f))
                )
            }
            // Item name overlay
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(28.dp).padding(horizontal = 8.dp)
            ) {
                Text(
                    item.name,
                    color = qualityColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            "${item.percentage}%",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(44.dp)
        )
    }
}

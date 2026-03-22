package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.LadderEntry
import net.tautellini.arenatactics.data.model.LadderSnapshot
import net.tautellini.arenatactics.data.model.SpecDistribution
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.LadderState
import net.tautellini.arenatactics.presentation.LadderViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun LadderScreen(viewModel: LadderViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        when (val s = state) {
            is LadderState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            is LadderState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = TextSecondary, textAlign = TextAlign.Center)
            }
            is LadderState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = TextSecondary)
            }
            is LadderState.Success -> LadderContent(
                state = s,
                onRegionSelect = viewModel::selectRegion,
                onBracketSelect = viewModel::selectBracket
            )
        }
    }
}

@Composable
private fun LadderContent(
    state: LadderState.Success,
    onRegionSelect: (String) -> Unit,
    onBracketSelect: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Region & Bracket selectors
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedSelector(
                    options = state.availableRegions,
                    selected = state.selectedRegion,
                    label = { it.uppercase() },
                    onSelect = onRegionSelect
                )
                Spacer(Modifier.width(8.dp))
                SegmentedSelector(
                    options = state.availableBrackets,
                    selected = state.selectedBracket,
                    label = { it },
                    onSelect = onBracketSelect
                )
            }
        }

        val snapshot = state.currentSnapshot
        if (snapshot == null) {
            item {
                Text("No data available for this region/bracket.", color = TextSecondary)
            }
            return@LazyColumn
        }

        // Fetch date
        item {
            Text(
                "Last updated: ${snapshot.fetchedAt.take(10)}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Rating cutoffs
        if (snapshot.ratingCutoffs.isNotEmpty()) {
            item {
                CutoffsCard(snapshot)
            }
        }

        // Spec distribution
        if (snapshot.specDistribution.isNotEmpty()) {
            item {
                Text(
                    "Spec Popularity",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                SpecDistributionSection(snapshot.specDistribution)
            }
        }

        // Top players
        if (snapshot.topEntries.isNotEmpty()) {
            item {
                TopPlayersSection(snapshot.topEntries)
            }
        }
    }
}

@Composable
private fun SegmentedSelector(
    options: List<String>,
    selected: String,
    label: (String) -> String,
    onSelect: (String) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(CardColor)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Primary else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) Background else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private val CutoffColors = mapOf(
    "gladiator" to Color(0xFFFFD700),
    "duelist" to Color(0xFFA855F7),
    "rival" to Color(0xFF3B82F6),
    "challenger" to Color(0xFF22C55E),
    "combatant" to Color(0xFF94A3B8)
)

@Composable
private fun CutoffsCard(snapshot: LadderSnapshot) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = CardColor,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Rating Cutoffs",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Season ${snapshot.seasonId} \u2022 ${snapshot.region.uppercase()} ${snapshot.bracket}",
                color = TextSecondary,
                fontSize = 12.sp
            )

            val orderedTitles = listOf("gladiator", "duelist", "rival", "challenger", "combatant")
            val cutoffs = orderedTitles.mapNotNull { title ->
                snapshot.ratingCutoffs[title]?.let { title to it }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cutoffs.forEach { (title, rating) ->
                    CutoffChip(title, rating)
                }
            }
        }
    }
}

@Composable
private fun CutoffChip(title: String, rating: Int) {
    val color = CutoffColors[title] ?: TextSecondary
    val shape = RoundedCornerShape(12.dp)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = shape,
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.3f), shape)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = title.replaceFirstChar { it.uppercase() },
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$rating+",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Spec icon name lookup — maps our specId slugs to Wowhead icon names.
// Only needed for specs that appear in ladder data (primarily retail).
private val SPEC_ICON_NAMES = mapOf(
    "deathknight_blood" to "spell_deathknight_bloodpresence",
    "deathknight_frost" to "spell_deathknight_frostpresence",
    "deathknight_unholy" to "spell_deathknight_unholypresence",
    "demonhunter_havoc" to "ability_demonhunter_specdragonstrikegreen",
    "demonhunter_vengeance" to "ability_demonhunter_spectank",
    "druid_balance" to "spell_nature_starfall",
    "druid_feral" to "ability_druid_catform",
    "druid_guardian" to "ability_racial_bearform",
    "druid_restoration" to "spell_nature_healingtouch",
    "evoker_devastation" to "classicon_evoker_devastation",
    "evoker_preservation" to "classicon_evoker_preservation",
    "evoker_augmentation" to "classicon_evoker_augmentation",
    "hunter_beastmastery" to "ability_hunter_bestialdiscipline",
    "hunter_marksmanship" to "ability_hunter_focusedaim",
    "hunter_survival" to "ability_hunter_camouflage",
    "mage_arcane" to "spell_holy_magicalsentry",
    "mage_fire" to "spell_fire_firebolt02",
    "mage_frost" to "spell_frost_frostbolt02",
    "monk_brewmaster" to "spell_monk_brewmaster_spec",
    "monk_mistweaver" to "spell_monk_mistweaver_spec",
    "monk_windwalker" to "spell_monk_windwalker_spec",
    "paladin_holy" to "spell_holy_holybolt",
    "paladin_protection" to "ability_paladin_shieldofthetemplar",
    "paladin_retribution" to "spell_holy_auraoflight",
    "priest_discipline" to "spell_holy_powerwordshield",
    "priest_holy" to "spell_holy_guardianspirit",
    "priest_shadow" to "spell_shadow_shadowwordpain",
    "rogue_assassination" to "ability_rogue_deadlybrew",
    "rogue_outlaw" to "ability_rogue_waylay",
    "rogue_subtlety" to "ability_stealth",
    "shaman_elemental" to "spell_nature_lightning",
    "shaman_enhancement" to "spell_shaman_improvedstormstrike",
    "shaman_restoration" to "spell_nature_magicimmunity",
    "warlock_affliction" to "spell_shadow_deathcoil",
    "warlock_demonology" to "spell_shadow_metamorphosis",
    "warlock_destruction" to "spell_shadow_rainoffire",
    "warrior_arms" to "ability_warrior_savageblow",
    "warrior_fury" to "ability_warrior_innerrage",
    "warrior_protection" to "ability_warrior_defensivestance",
)

private fun specDisplayName(specId: String): String {
    val parts = specId.split("_", limit = 2)
    if (parts.size < 2) return specId
    val className = parts[0].replaceFirstChar { it.uppercase() }
    val specName = parts[1].replaceFirstChar { it.uppercase() }
    return "$specName $className"
}

@Composable
private fun SpecDistributionSection(distribution: List<SpecDistribution>) {
    val maxCount = distribution.maxOfOrNull { it.count } ?: 1
    val shape = RoundedCornerShape(16.dp)

    Surface(
        color = CardColor,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            distribution.forEach { spec ->
                SpecDistributionBar(spec, maxCount)
            }
        }
    }
}

@Composable
private fun SpecDistributionBar(spec: SpecDistribution, maxCount: Int) {
    val fraction = spec.count.toFloat() / maxCount.coerceAtLeast(1)
    val classId = spec.specId.substringBefore("_")
    val barColor = classColor(classId)
    val iconName = SPEC_ICON_NAMES[spec.specId]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(32.dp)
    ) {
        // Spec icon
        if (iconName != null) {
            AsyncImage(
                model = WowheadIcons.medium(iconName),
                contentDescription = spec.specId,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
        }

        // Spec name
        Text(
            text = specDisplayName(spec.specId),
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(140.dp)
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor.copy(alpha = 0.7f))
            )
        }

        Spacer(Modifier.width(8.dp))

        // Percentage
        Text(
            text = "${spec.percentage}%",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

private const val PAGE_SIZE = 100

@Composable
private fun TopPlayersSection(entries: List<LadderEntry>) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = (entries.size + PAGE_SIZE - 1) / PAGE_SIZE
    val pageEntries = entries.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Top Players",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Header
        TopEntryRow(entry = null)

        pageEntries.forEach { entry ->
            TopEntryRow(entry = entry)
        }

        // Pagination
        if (totalPages > 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                (0 until totalPages).forEach { page ->
                    val isSelected = page == currentPage
                    val from = page * PAGE_SIZE + 1
                    val to = minOf((page + 1) * PAGE_SIZE, entries.size)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Primary else CardColor)
                            .clickable { currentPage = page }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$from–$to",
                            color = if (isSelected) Background else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (page < totalPages - 1) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

private val CLASS_ICON_NAMES = mapOf(
    "warrior" to "classicon_warrior",
    "paladin" to "classicon_paladin",
    "hunter" to "classicon_hunter",
    "rogue" to "classicon_rogue",
    "priest" to "classicon_priest",
    "deathknight" to "classicon_deathknight",
    "shaman" to "classicon_shaman",
    "mage" to "classicon_mage",
    "warlock" to "classicon_warlock",
    "monk" to "classicon_monk",
    "druid" to "classicon_druid",
    "demonhunter" to "classicon_demonhunter",
    "evoker" to "classicon_evoker",
)

@Composable
private fun TopEntryRow(entry: LadderEntry?) {
    val isHeader = entry == null
    val textColor = if (isHeader) TextSecondary else TextPrimary
    val weight = if (isHeader) FontWeight.Medium else FontWeight.Normal
    val fontSize = if (isHeader) 11.sp else 13.sp
    val nameColor = if (isHeader) TextSecondary else {
        entry?.classId?.let { classColor(it) } ?: TextPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isHeader) Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardColor.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isHeader) "#" else "${entry!!.rank}",
            color = textColor, fontWeight = weight, fontSize = fontSize,
            modifier = Modifier.width(40.dp)
        )

        // Class icon
        if (!isHeader && entry!!.classId != null) {
            val iconName = CLASS_ICON_NAMES[entry.classId]
            if (iconName != null) {
                AsyncImage(
                    model = WowheadIcons.medium(iconName),
                    contentDescription = entry.classId,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Text(
            text = if (isHeader) "Player" else entry!!.characterName,
            color = nameColor, fontWeight = weight, fontSize = fontSize,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isHeader) "Rating" else "${entry!!.rating}",
            color = if (isHeader) textColor else Primary,
            fontWeight = if (isHeader) weight else FontWeight.Bold,
            fontSize = fontSize,
            modifier = Modifier.width(60.dp), textAlign = TextAlign.End
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (isHeader) "W / L" else "${entry!!.wins} / ${entry.losses}",
            color = textColor, fontWeight = weight, fontSize = fontSize,
            modifier = Modifier.width(70.dp), textAlign = TextAlign.End
        )
    }
}

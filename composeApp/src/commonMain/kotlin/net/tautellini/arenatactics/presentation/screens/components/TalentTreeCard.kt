package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.TalentDefinition
import net.tautellini.arenatactics.data.model.TalentGroup
import net.tautellini.arenatactics.data.model.TalentTreeData
import net.tautellini.arenatactics.data.model.TalentTreeDefinition
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.domain.TalentBuildEntry
import net.tautellini.arenatactics.presentation.theme.*

private val TalentMaxed = Color(0xFFFFD100)
private val TalentPartial = Color(0xFF4ADE80)
private val TalentEmpty = Color(0xFF333333)

@Composable
fun TalentTreeCard(
    talentTree: TalentTreeDefinition,
    builds: List<TalentBuildEntry>
) {
    var selectedBuildIndex by remember { mutableStateOf(0) }
    var selectedTreeIndex by remember { mutableStateOf(0) }
    val selectedBuild = builds.getOrNull(selectedBuildIndex) ?: return

    val talentRanks = remember(selectedBuildIndex) {
        selectedBuild.talentSelections.associate { it.id to it.rank }
    }

    // Find the tree with most points for auto-select when switching builds
    LaunchedEffect(selectedBuildIndex) {
        val bestTree = selectedBuild.trees.maxByOrNull { it.second }?.first
        if (bestTree != null) {
            val idx = talentTree.trees.indexOfFirst { it.name == bestTree }
            if (idx >= 0) selectedTreeIndex = idx
        }
    }

    val shape = RoundedCornerShape(12.dp)
    val currentTree = talentTree.trees.getOrNull(selectedTreeIndex) ?: return

    Surface(color = CardColor, shape = shape, modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Talent Build", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${selectedBuild.percentage}%", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Build label
            Text(selectedBuild.label, color = TextSecondary, fontSize = 11.sp)

            // Tree tab switcher
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                talentTree.trees.forEachIndexed { index, tree ->
                    val isActive = index == selectedTreeIndex
                    val pts = selectedBuild.trees.find { it.first == tree.name }?.second ?: 0
                    TreeTab(
                        tree = tree,
                        points = pts,
                        isSelected = isActive,
                        onClick = { selectedTreeIndex = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Single talent tree panel
            TalentTreePanel(
                tree = currentTree,
                talentRanks = talentRanks
            )

            // Build switcher
            if (builds.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    builds.forEachIndexed { index, build ->
                        BuildOption(
                            percentage = build.percentage,
                            isSelected = index == selectedBuildIndex,
                            onClick = { selectedBuildIndex = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeTab(
    tree: TalentTreeData,
    points: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabShape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        when {
            isSelected -> CardElevated
            isHovered -> CardElevated
            else -> Background
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        when {
            isSelected -> Primary
            isHovered -> Primary.copy(alpha = 0.3f)
            else -> CardBorder
        },
        animationSpec = tween(200)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(tabShape)
            .hoverable(interactionSource)
            .background(bg)
            .border(1.dp, borderColor, tabShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.medium(tree.icon),
            contentDescription = tree.name,
            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            tree.name,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (points > 0) {
            Spacer(Modifier.width(3.dp))
            Text(
                "($points)",
                color = if (isSelected) Primary else TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun TalentTreePanel(
    tree: TalentTreeData,
    talentRanks: Map<Int, Int>
) {
    val maxRow = tree.talents.maxOfOrNull { it.row } ?: 0
    val talentsByRow = tree.talents.groupBy { it.row }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0..maxRow) {
            val rowTalents = talentsByRow[row] ?: emptyList()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..3) {
                    val talent = rowTalents.find { it.col == col }
                    if (talent != null) {
                        val rank = talentRanks[talent.id] ?: 0
                        TalentSlot(talent = talent, rank = rank)
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TalentSlot(talent: TalentDefinition, rank: Int) {
    val borderColor = when {
        rank >= talent.maxRank -> TalentMaxed
        rank > 0 -> TalentPartial
        else -> TalentEmpty
    }
    val slotAlpha = if (rank > 0) 1f else 0.4f

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier.size(36.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.medium(talent.icon),
            contentDescription = talent.name,
            modifier = Modifier
                .size(34.dp)
                .alpha(slotAlpha)
                .clip(RoundedCornerShape(4.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
        )

        if (rank > 0) {
            Text(
                text = "$rank",
                color = if (rank >= talent.maxRank) TalentMaxed else TalentPartial,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun BuildOption(
    percentage: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        when {
            isSelected -> CardElevated
            isHovered -> CardElevated
            else -> Background
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        when {
            isSelected -> Primary
            isHovered -> Primary.copy(alpha = 0.3f)
            else -> CardBorder
        },
        animationSpec = tween(200)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .hoverable(interactionSource)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(
            text = "${percentage}%",
            color = if (isSelected) Primary else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Talent tree card for a single player's talent group.
 * Shows one tree at a time with tab switcher — no build switcher.
 */
@Composable
fun PlayerTalentTreeCard(
    talentTree: TalentTreeDefinition,
    group: TalentGroup
) {
    var selectedTreeIndex by remember { mutableStateOf(0) }

    // Auto-select the tree with most points
    LaunchedEffect(group) {
        val bestTree = group.specializations.maxByOrNull { it.spentPoints }?.treeName
        if (bestTree != null) {
            val idx = talentTree.trees.indexOfFirst { it.name == bestTree }
            if (idx >= 0) selectedTreeIndex = idx
        }
    }

    // Build talent ID -> rank map from the player's talent group
    val talentRanks = remember(group) {
        group.specializations.flatMap { it.talents }.associate { it.id to it.rank }
    }

    val shape = RoundedCornerShape(12.dp)
    val currentTree = talentTree.trees.getOrNull(selectedTreeIndex) ?: return

    Surface(color = CardColor, shape = shape, modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Build label
            Text(
                group.specializations.joinToString(" / ") { "${it.treeName} (${it.spentPoints})" },
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Tree tab switcher
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                talentTree.trees.forEachIndexed { index, tree ->
                    val isActive = index == selectedTreeIndex
                    val pts = group.specializations.find { it.treeName == tree.name }?.spentPoints ?: 0
                    TreeTab(
                        tree = tree,
                        points = pts,
                        isSelected = isActive,
                        onClick = { selectedTreeIndex = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Talent tree panel
            TalentTreePanel(
                tree = currentTree,
                talentRanks = talentRanks
            )
        }
    }
}

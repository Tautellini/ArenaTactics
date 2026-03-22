package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import net.tautellini.arenatactics.data.model.ItemTooltipData

private val QualityColors = mapOf(
    "LEGENDARY" to Color(0xFFFF8000),
    "EPIC" to Color(0xFFA335EE),
    "RARE" to Color(0xFF0070DD),
    "UNCOMMON" to Color(0xFF1EFF00),
    "COMMON" to Color(0xFFFFFFFF),
)

private val GreenText = Color(0xFF1EFF00)
private val GrayText = Color(0xFF9D9D9D)
private val YellowText = Color(0xFFFFD100)
private val TooltipBg = Color(0xFF121012)

/**
 * Wraps content with a hover-triggered item tooltip.
 * Shows WoW-style tooltip near the mouse on hover.
 * The content itself handles click (e.g., navigate to Wowhead).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WithItemTooltip(
    tooltipData: ItemTooltipData?,
    playerEnchant: String? = null,
    playerGems: List<String> = emptyList(),
    content: @Composable () -> Unit
) {
    if (tooltipData == null) {
        content()
        return
    }

    var isHovered by remember { mutableStateOf(false) }
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .onPointerEvent(PointerEventType.Move) {
                val pos = it.changes.firstOrNull()?.position
                if (pos != null) pointerOffset = pos
            }
    ) {
        content()

        if (isHovered) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(pointerOffset.x.toInt() + 16, pointerOffset.y.toInt() - 8)
            ) {
                ItemTooltipContent(tooltipData, playerEnchant, playerGems)
            }
        }
    }
}

@Composable
fun ItemTooltipContent(
    item: ItemTooltipData,
    playerEnchant: String? = null,
    playerGems: List<String> = emptyList()
) {
    val qualityColor = QualityColors[item.quality] ?: Color.White
    val shape = RoundedCornerShape(8.dp)

    Surface(
        color = TooltipBg,
        shape = shape,
        shadowElevation = 8.dp,
        modifier = Modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .border(1.dp, qualityColor.copy(alpha = 0.5f), shape)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(item.name, color = qualityColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            val slotLine = listOfNotNull(item.slotName, item.itemSubclass).joinToString(" · ")
            if (slotLine.isNotEmpty()) {
                Text(slotLine, color = Color.White, fontSize = 11.sp)
            }

            if (!item.binding.isNullOrEmpty()) {
                Text(item.binding!!, color = Color.White, fontSize = 11.sp)
            }

            if (item.weaponDamage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.weaponDamage!!, color = Color.White, fontSize = 11.sp)
                    if (item.weaponSpeed != null) {
                        Text(item.weaponSpeed!!, color = Color.White, fontSize = 11.sp)
                    }
                }
                if (item.weaponDps != null) {
                    Text(item.weaponDps!!, color = Color.White, fontSize = 11.sp)
                }
            }

            if (item.armor != null && item.armor > 0) {
                Text("${item.armor} Armor", color = Color.White, fontSize = 11.sp)
            }

            item.stats.forEach { stat ->
                val isEquip = stat.startsWith("Equip:")
                Text(stat, color = if (isEquip) GreenText else Color.White, fontSize = 11.sp)
            }

            item.spells.forEach { spell ->
                Text(spell, color = GreenText, fontSize = 11.sp)
            }

            if (!playerEnchant.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("Enchanted: $playerEnchant", color = GreenText, fontSize = 11.sp)
            }

            if (playerGems.isNotEmpty()) {
                playerGems.forEach { gem ->
                    Text(gem, color = GreenText, fontSize = 11.sp)
                }
            }

            if (!item.setName.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(item.setName!!, color = YellowText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                item.setEffects.forEach { effect ->
                    Text(effect, color = GrayText, fontSize = 10.sp)
                }
            }

            if (!item.requiredLevel.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(item.requiredLevel!!, color = Color.White, fontSize = 10.sp)
            }
            if (!item.requiredClasses.isNullOrEmpty()) {
                Text(item.requiredClasses!!, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

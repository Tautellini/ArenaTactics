package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
private val TooltipBg = Color(0xFF1A1A2E)
private val TooltipBorder = Color(0xFF4A4A6A)

@Composable
fun ItemTooltipPopup(
    item: ItemTooltipData,
    playerEnchant: String? = null,
    playerGems: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onWowheadClick: (() -> Unit)? = null
) {
    Popup(onDismissRequest = onDismiss) {
        ItemTooltipContent(item, playerEnchant, playerGems, onWowheadClick)
    }
}

@Composable
fun ItemTooltipContent(
    item: ItemTooltipData,
    playerEnchant: String? = null,
    playerGems: List<String> = emptyList(),
    onWowheadClick: (() -> Unit)? = null
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
            .then(if (onWowheadClick != null) Modifier.clickable(onClick = onWowheadClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Item name
            Text(item.name, color = qualityColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            // Slot + subclass (e.g. "Head · Cloth")
            val slotLine = listOfNotNull(item.slotName, item.itemSubclass).joinToString(" · ")
            if (slotLine.isNotEmpty()) {
                Text(slotLine, color = Color.White, fontSize = 11.sp)
            }

            // Binding
            if (!item.binding.isNullOrEmpty()) {
                Text(item.binding!!, color = Color.White, fontSize = 11.sp)
            }

            // Weapon damage / speed
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

            // Armor
            if (item.armor != null && item.armor > 0) {
                Text("${item.armor} Armor", color = Color.White, fontSize = 11.sp)
            }

            // Stats
            item.stats.forEach { stat ->
                val isEquip = stat.startsWith("Equip:")
                Text(stat, color = if (isEquip) GreenText else Color.White, fontSize = 11.sp)
            }

            // Spells (equip effects)
            item.spells.forEach { spell ->
                Text(spell, color = GreenText, fontSize = 11.sp)
            }

            // Player enchant
            if (!playerEnchant.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("Enchanted: $playerEnchant", color = GreenText, fontSize = 11.sp)
            }

            // Player gems
            if (playerGems.isNotEmpty()) {
                playerGems.forEach { gem ->
                    Text(gem, color = GreenText, fontSize = 11.sp)
                }
            }

            // Set
            if (!item.setName.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(item.setName!!, color = YellowText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                item.setEffects.forEach { effect ->
                    Text(effect, color = GrayText, fontSize = 10.sp)
                }
            }

            // Requirements
            if (!item.requiredLevel.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(item.requiredLevel!!, color = Color.White, fontSize = 10.sp)
            }
            if (!item.requiredClasses.isNullOrEmpty()) {
                Text(item.requiredClasses!!, color = Color.White, fontSize = 10.sp)
            }

            // Wowhead link hint
            if (onWowheadClick != null) {
                Spacer(Modifier.height(4.dp))
                Text("Click for Wowhead →", color = qualityColor.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}

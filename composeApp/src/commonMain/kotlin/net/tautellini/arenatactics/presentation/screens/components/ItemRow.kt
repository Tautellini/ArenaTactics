package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun ItemRow(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { openUrl(wowheadUrl) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.slot,
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = item.name,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (item.enchant != null || item.gems.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.enchant != null) {
                    Text(
                        text = "✦ ${item.enchant}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                if (item.gems.isNotEmpty()) {
                    Text(
                        text = item.gems.joinToString(" · ") { "◆ $it" },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

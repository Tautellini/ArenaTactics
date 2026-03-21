package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRow(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = { Text(item.name, fontWeight = FontWeight.SemiBold) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Slot: ${item.slot}", fontSize = 12.sp)
                    if (item.enchant != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = Primary)
                            Text(item.enchant, fontSize = 12.sp)
                        }
                    }
                    if (item.gems.isNotEmpty()) {
                        item.gems.forEach { gem ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Diamond, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                                Text(gem, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View on Wowhead", fontSize = 11.sp, color = Primary)
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(11.dp), tint = Primary)
                    }
                }
            }
        },
        state = rememberTooltipState()
    ) {
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
                    color = Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (item.enchant != null || item.gems.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.enchant != null) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(11.dp), tint = Primary)
                        Text(item.enchant, color = TextSecondary, fontSize = 11.sp)
                    }
                    if (item.gems.isNotEmpty()) {
                        Icon(Icons.Rounded.Diamond, contentDescription = null, modifier = Modifier.size(11.dp), tint = TextSecondary)
                        Text(
                            text = item.gems.joinToString(" · "),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

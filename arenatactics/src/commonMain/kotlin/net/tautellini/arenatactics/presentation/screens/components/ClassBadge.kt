package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun ClassBadge(classId: String, className: String, modifier: Modifier = Modifier) {
    val color = classColor(classId)
    Text(
        text = className,
        color = Background,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

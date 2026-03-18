package net.tautellini.arenatactics.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Background layers
val Background   = Color(0xFF0D0D0F)
val Surface      = Color(0xFF16161A)
val CardColor    = Color(0xFF1C1C21)
val CardElevated = Color(0xFF222228)

// Text
val TextPrimary   = Color(0xFFE8E1D6)
val TextSecondary = Color(0xFF8A8490)

// Accent
val Accent = Color(0xFFC89B3C)

// Divider
val DividerColor = Color(0xFF2A2A32)

// WoW class colors
val ClassColors = mapOf(
    "druid"   to Color(0xFFFF7D0A),
    "hunter"  to Color(0xFFABD473),
    "mage"    to Color(0xFF69CCF0),
    "paladin" to Color(0xFFF58CBA),
    "priest"  to Color(0xFFFFFFFF),
    "rogue"   to Color(0xFFFFF569),
    "shaman"  to Color(0xFF0070DE),
    "warlock" to Color(0xFF9482C9),
    "warrior" to Color(0xFFC79C6E)
)

fun classColor(classId: String): Color = ClassColors[classId] ?: TextPrimary

private val DarkColors = darkColorScheme(
    background = Background,
    surface = Surface,
    primary = Accent,
    onPrimary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ArenaTacticsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}

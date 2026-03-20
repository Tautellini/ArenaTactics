package net.tautellini.arenatactics.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import arenatactics.composeapp.generated.resources.NotoSans_Bold
import arenatactics.composeapp.generated.resources.NotoSans_Medium
import arenatactics.composeapp.generated.resources.NotoSans_Regular
import arenatactics.composeapp.generated.resources.NotoSans_SemiBold
import arenatactics.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

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
    val notoSans = FontFamily(
        Font(Res.font.NotoSans_Regular, FontWeight.Normal),
        Font(Res.font.NotoSans_Medium, FontWeight.Medium),
        Font(Res.font.NotoSans_SemiBold, FontWeight.SemiBold),
        Font(Res.font.NotoSans_Bold, FontWeight.Bold),
    )
    val typography = Typography(
        bodyLarge   = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium  = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall   = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge  = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall  = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.Medium, fontSize = 11.sp),
        titleLarge  = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall  = TextStyle(fontFamily = notoSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
    MaterialTheme(
        colorScheme = DarkColors,
        typography = typography,
        content = content
    )
}

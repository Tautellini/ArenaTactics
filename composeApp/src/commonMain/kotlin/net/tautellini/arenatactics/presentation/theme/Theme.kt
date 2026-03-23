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
import arenatactics.composeapp.generated.resources.CinzelDecorative_Bold
import arenatactics.composeapp.generated.resources.CinzelDecorative_Regular
import arenatactics.composeapp.generated.resources.NotoSans_Bold
import arenatactics.composeapp.generated.resources.NotoSans_Medium
import arenatactics.composeapp.generated.resources.NotoSans_Regular
import arenatactics.composeapp.generated.resources.NotoSans_SemiBold
import arenatactics.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

val Primary = Color(0xFF27E0E0)
val Secondary = Color(0xFF0F5959)
val Background   = Color(0xFF042326)
val Surface      = Color(0xFF0A3A40)
val TextPrimary   = Color(0xFFE8F4F4)
val TextSecondary = Color(0xFF7AABAA)

val CardColor    = Color(0xFF0F4A52)
val CardElevated = Color(0xFF1D6066)
val CardBorder   = Color(0xFF0F5959)

val DividerColor = Color(0xFF0F5959)

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
    primary = Primary,
    secondary = Secondary,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun cinzelDecorative() = FontFamily(
    Font(Res.font.CinzelDecorative_Regular, FontWeight.Normal),
    Font(Res.font.CinzelDecorative_Bold, FontWeight.Bold),
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

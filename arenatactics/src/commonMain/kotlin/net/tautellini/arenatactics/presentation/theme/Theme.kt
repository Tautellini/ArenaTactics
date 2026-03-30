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
import tautellini.arenatactics.generated.resources.*
import org.jetbrains.compose.resources.Font

// ── Core palette ──
val Primary       = Color(0xFF2DE8D0)
val PrimaryBright  = Color(0xFF5EFCE8)
val PrimaryDim     = Color(0x1F2DE8D0)  // ~12% alpha
val Secondary     = Color(0xFF0F5959)

// ── Backgrounds (darkest → lightest) ──
val BgVoid    = Color(0xFF03090B)
val BgAbyss   = Color(0xFF051215)
val BgDeep    = Color(0xFF0A1E22)
val BgStone   = Color(0xFF0E2A2F)

// ── Surfaces ──
val Surface      = Color(0xFF112F35)
val SurfaceLight = Color(0xFF163940)

// ── Cards ──
val CardColor    = Color(0xBF0E2A2F)  // ~75% alpha
val CardHover    = Color(0xD9124B55)   // ~85% alpha
val CardElevated = Color(0xFF1D6066)
val CardBorder   = Color(0x0F2DE8D0)   // ~6% alpha
val CardBorderHover = Color(0x2E2DE8D0) // ~18% alpha

// ── Text ──
val TextHero      = Color(0xFFF0F8F7)
val TextPrimary   = Color(0xFFD4E8E6)
val TextSecondary = Color(0xFF7DA8A5)
val TextMuted     = Color(0xFF4A7572)
val TextDim       = Color(0xFF2E504D)

// ── Divider ──
val DividerColor = Color(0x0F2DE8D0)  // matches CardBorder

// ── Accent colors ──
val Gold       = Color(0xFFD4A032)
val GoldBright = Color(0xFFF5CC5A)
val GoldDim    = Color(0x1FD4A032)
val Ember      = Color(0xFFC4562A)
val EmberGlow  = Color(0x4DC4562A)

// ── Backward-compatible aliases ──
val Background = BgVoid

// ── WoW class colors ──
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

// ── Item quality colors ──
val QualityCommon    = Color(0xFF9D9D9D)
val QualityUncommon  = Color(0xFF1EFF00)
val QualityRare      = Color(0xFF0070DD)
val QualityEpic      = Color(0xFFA335EE)
val QualityLegendary = Color(0xFFFF8000)

// ── Rank colors ──
val RankGladiator  = Color(0xFFFF8C00)
val RankDuelist    = Color(0xFFFFD700)
val RankRival      = Color(0xFFA335EE)
val RankChallenger = Color(0xFF0070DD)
val RankCombatant  = Color(0xFF1EFF00)

private val DarkColors = darkColorScheme(
    background = BgVoid,
    surface = Surface,
    primary = Primary,
    secondary = Secondary,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun cinzel() = FontFamily(
    Font(Res.font.Cinzel_Regular, FontWeight.Normal),
    Font(Res.font.Cinzel_SemiBold, FontWeight.SemiBold),
    Font(Res.font.Cinzel_Bold, FontWeight.Bold),
    Font(Res.font.Cinzel_ExtraBold, FontWeight.ExtraBold),
)

@Composable
fun cinzelDecorative() = FontFamily(
    Font(Res.font.CinzelDecorative_Regular, FontWeight.Normal),
    Font(Res.font.CinzelDecorative_Bold, FontWeight.Bold),
)

@Composable
fun outfit() = FontFamily(
    Font(Res.font.Outfit_Light, FontWeight.Light),
    Font(Res.font.Outfit_Regular, FontWeight.Normal),
    Font(Res.font.Outfit_Medium, FontWeight.Medium),
    Font(Res.font.Outfit_SemiBold, FontWeight.SemiBold),
    Font(Res.font.Outfit_Bold, FontWeight.Bold),
)

@Composable
fun ArenaTacticsTheme(content: @Composable () -> Unit) {
    val outfitFamily = outfit()
    val typography = Typography(
        bodyLarge   = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium  = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall   = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge  = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall  = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
        titleLarge  = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall  = TextStyle(fontFamily = outfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
    MaterialTheme(
        colorScheme = DarkColors,
        typography = typography,
        content = content
    )
}

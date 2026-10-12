package com.wayfarer.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.wayfarer.resources.*
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Design tokens from the Wayfarer mockups (canvas "Wayfarer Mockups").
object WfColors {
    val Ground = Color(0xFFF2F5F3)
    val Ink = Color(0xFF12262B)
    val InkSoft = Color(0xFF4A5E63)
    val Teal = Color(0xFF1F6F78)
    val Amber = Color(0xFFE8A33D)
    val Danger = Color(0xFF9A3B2E)
    val Outline = Color(0xFFC5D0CB)
    val Surface = Color.White
    val PhotoPlaceholder = Color(0xFF2E5B63)
}

private val scheme = lightColorScheme(
    primary = WfColors.Ink, onPrimary = WfColors.Ground,
    secondary = WfColors.Teal, tertiary = WfColors.Amber, onTertiary = WfColors.Ink,
    background = WfColors.Ground, onBackground = WfColors.Ink,
    surface = WfColors.Surface, onSurface = WfColors.Ink, onSurfaceVariant = WfColors.InkSoft,
    outline = WfColors.Outline, error = WfColors.Danger,
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(18.dp),
)

// Typefaces from the mockups, bundled as Compose resources (OFL licensed; see composeResources/font/OFL-*.txt).
@Composable
private fun wayfarerTypography(): Typography {
    val body = FontFamily(
        Font(Res.font.dm_sans_regular, FontWeight.Normal),
        Font(Res.font.dm_sans_medium, FontWeight.Medium),
        Font(Res.font.dm_sans_semibold, FontWeight.SemiBold),
    )
    val heading = FontFamily(
        Font(Res.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
        Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
    )
    return remember(body, heading) {
        val base = Typography()
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = heading),
            displayMedium = base.displayMedium.copy(fontFamily = heading),
            displaySmall = base.displaySmall.copy(fontFamily = heading),
            headlineLarge = base.headlineLarge.copy(fontFamily = heading),
            headlineMedium = base.headlineMedium.copy(fontFamily = heading),
            headlineSmall = base.headlineSmall.copy(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp),
            titleLarge = base.titleLarge.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
            titleMedium = base.titleMedium.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 25.sp),
            titleSmall = base.titleSmall.copy(fontFamily = body),
            bodyLarge = base.bodyLarge.copy(fontFamily = body),
            bodyMedium = base.bodyMedium.copy(fontFamily = body),
            bodySmall = base.bodySmall.copy(fontFamily = body),
            labelLarge = base.labelLarge.copy(fontFamily = body),
            labelMedium = base.labelMedium.copy(fontFamily = body),
            labelSmall = base.labelSmall.copy(fontFamily = body),
        )
    }
}

@Composable
fun WayfarerTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, shapes = shapes, typography = wayfarerTypography(), content = content)

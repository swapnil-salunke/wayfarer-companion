package com.wayfarer.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

private val typography = Typography().let {
    // Mockups use Bricolage Grotesque (headings) + DM Sans; bundle them as Compose resources to match exactly.
    it.copy(
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 25.sp),
    )
}

@Composable
fun WayfarerTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, shapes = shapes, typography = typography, content = content)

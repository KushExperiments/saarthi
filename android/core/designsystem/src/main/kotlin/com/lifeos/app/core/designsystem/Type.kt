package com.lifeos.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Elder-readable type scale — deliberately larger than Material3's defaults
 * at every level. Display roles use a serif family — Juno's "speaking
 * voice," reserved for greetings and anything Juno says out loud — while
 * everything else stays on the default humanist sans, so lists, labels,
 * and buttons read as calm UI rather than as Juno "talking." The approved
 * redesign spec's direction is Fraunces specifically; a system serif
 * stands in here rather than a bundled font file for now (see
 * docs/adr/0001 — swapping in the real family is a deliberate fast-follow,
 * not an oversight).
 */
private val DisplayFont = FontFamily.Serif

val LifeOSTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 25.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
)

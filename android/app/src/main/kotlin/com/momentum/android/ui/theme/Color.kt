package com.momentum.android.ui.theme

import androidx.compose.ui.graphics.Color

// Ported verbatim from frontend/src/styles/index.css's CSS custom
// properties -- both apps must agree on every hex value here, light and
// dark, so a chart/badge means the same thing regardless of which client
// you're looking at. Update both files together if either changes.

// Surfaces / text / hairlines
val SurfaceLight = Color(0xFFFCFCFB)
val PageLight = Color(0xFFF9F9F7)
val TextPrimaryLight = Color(0xFF0B0B0B)
val TextSecondaryLight = Color(0xFF52514E)
val TextMutedLight = Color(0xFF898781)
val HairlineLight = Color(0xFFE1E0D9)

val SurfaceDark = Color(0xFF1A1A19)
val PageDark = Color(0xFF0D0D0D)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFC3C2B7)
val TextMutedDark = Color(0xFF898781)
val HairlineDark = Color(0xFF2C2C2A)

// Categorical series -- fixed order, never cycled. series1 = Training,
// series2 = Fuel, series3 = Recovery, series4 = Body, series5 = Goals
// (matches TimelineEntryItem's kind->color mapping on web).
val Series1Light = Color(0xFF2A78D6)
val Series2Light = Color(0xFFEB6834)
val Series3Light = Color(0xFF1BAF7A)
val Series4Light = Color(0xFFEDA100)
val Series5Light = Color(0xFFE87BA4)
val Series6Light = Color(0xFF008300)
val Series7Light = Color(0xFF4A3AA7)
val Series8Light = Color(0xFFE34948)

val Series1Dark = Color(0xFF3987E5)
val Series2Dark = Color(0xFFD95926)
val Series3Dark = Color(0xFF199E70)
val Series4Dark = Color(0xFFC98500)
val Series5Dark = Color(0xFFD55181)
val Series6Dark = Color(0xFF008300)
val Series7Dark = Color(0xFF9085E9)
val Series8Dark = Color(0xFFE66767)

// Status palette -- reserved for state (good/warning/serious/critical),
// never reused as a categorical series color.
val StatusGoodLight = Color(0xFF0CA30C)
val StatusWarningLight = Color(0xFFFAB219)
val StatusSeriousLight = Color(0xFFEC835A)
val StatusCriticalLight = Color(0xFFD03B3B)

// The web CSS's dark-mode blocks never redefine --status-* at all -- they
// inherit the light values unchanged, so these intentionally match exactly.
val StatusGoodDark = StatusGoodLight
val StatusWarningDark = StatusWarningLight
val StatusSeriousDark = StatusSeriousLight
val StatusCriticalDark = StatusCriticalLight

// Kept for the pre-parity theme's primary-color reference.
val MomentumBlue = Series1Light
val MomentumBlueDark = Series1Dark

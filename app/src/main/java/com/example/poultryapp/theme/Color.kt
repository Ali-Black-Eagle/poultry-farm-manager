package com.example.poultryapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Farm Management System - Forest Green & Natural Soil Palette
 * Compliant with WCAG AA (contrast >= 4.5:1 for body, >= 3:1 for large text).
 */

// Forest Green Core (Light & Vibrant Emerald Dark)
val FarmForestGreen = Color(0xFF1E4620)
val FarmForestGreenDark = Color(0xFF22C55E) // Radiant Emerald-500 for Dark Theme
val FarmForestGreenContainer = Color(0xFFE8F5E9)
val FarmForestGreenContainerDark = Color(0xFF0B381E) // Deep Forest Emerald
val FarmForestGreenOnContainer = Color(0xFF0D2F12)
val FarmForestGreenOnContainerDark = Color(0xFF86EFAC) // Mint Luminous Text

// Secondary Sage / Olive (Light) & Cool Modern Teal (Dark)
val FarmSageSecondary = Color(0xFF386641)
val FarmSageSecondaryDark = Color(0xFF2DD4BF) // Radiant Teal-400 for Dark Theme
val FarmSageContainer = Color(0xFFF2F7F2)
val FarmSageContainerDark = Color(0xFF134E4A) // Deep Teal
val FarmSageOnContainer = Color(0xFF122816)
val FarmSageOnContainerDark = Color(0xFF99F6E4) // Luminous Teal Text

// Neutral Surfaces & Backgrounds - Warm White & Light Grey
val FarmWarmBackgroundLight = Color(0xFFF9FAF8)
val FarmSurfaceLight = Color(0xFFFFFFFF)
val FarmSurfaceVariantLight = Color(0xFFEFF3EE)
val FarmBorderLight = Color(0xFFE1E7E0)

// Text Neutrals
val FarmTextPrimaryLight = Color(0xFF171A17)
val FarmTextSecondaryLight = Color(0xFF4F594E)
val FarmTextTertiaryLight = Color(0xFF7E8A7D)

// Dark Theme Neutrals (Obsidian Slate Palette with High Contrast & Depth)
val FarmBackgroundDark = Color(0xFF0C100E)
val FarmSurfaceDark = Color(0xFF161E1A)
val FarmSurfaceVariantDark = Color(0xFF202A24)
val FarmBorderDark = Color(0xFF2C3B32)
val FarmTextPrimaryDark = Color(0xFFF8FAFC)
val FarmTextSecondaryDark = Color(0xFFCBD5E1)
val FarmTextTertiaryDark = Color(0xFF94A3B8)

// Semantic: Feed (خوراک و دان) - Amber
val FeedAmber = Color(0xFFD97706)
val FeedAmberDark = Color(0xFFFBBF24)
val FeedAmberContainer = Color(0xFFFEF3C7)
val FeedAmberContainerDark = Color(0xFF451A03)
val FeedAmberText = Color(0xFF92400E)
val FeedAmberLight = Color(0xFFFEF3C7) // Alias

// Semantic: Water (آب) - Blue
val WaterBlue = Color(0xFF0284C7)
val WaterBlueDark = Color(0xFF38BDF8)
val WaterBlueContainer = Color(0xFFE0F2FE)
val WaterBlueContainerDark = Color(0xFF082F49)
val WaterBlueText = Color(0xFF075985)
val WaterBlueTextDark = Color(0xFF7DD3FC)
val WaterBlueLight = Color(0xFFE0F2FE) // Alias

// Semantic: Health Status
val HealthGreen = Color(0xFF16A34A)
val HealthGreenDark = Color(0xFF34D399)
val HealthGreenContainer = Color(0xFFDCFCE7)
val HealthGreenContainerDark = Color(0xFF0D281E)
val HealthGreenText = Color(0xFF14532D)
val HealthGreenTextDark = Color(0xFF6EE7B7)

val HealthYellow = Color(0xFFCA8A04)
val HealthYellowDark = Color(0xFFFACC15)
val HealthYellowContainer = Color(0xFFFEF9C3)
val HealthYellowContainerDark = Color(0xFF352608)

val HealthRed = Color(0xFFDC2626)
val HealthRedDark = Color(0xFFF87171)
val HealthRedContainer = Color(0xFFFEE2E2)
val HealthRedContainerDark = Color(0xFF2E1216)
val HealthRedText = Color(0xFF991B1B)
val HealthRedTextDark = Color(0xFFFCA5A5)

// Error
val FarmError = Color(0xFFDC2626)
val FarmOnError = Color(0xFFFFFFFF)
val FarmErrorContainer = Color(0xFFFEE2E2)
val FarmOnErrorContainer = Color(0xFF991B1B)

val FarmErrorDark = Color(0xFFF87171)
val FarmOnErrorDark = Color(0xFF450A0A)
val FarmErrorContainerDark = Color(0xFF2E1216)
val FarmOnErrorContainerDark = Color(0xFFFCA5A5)

// Legacy Aliases for seamless backward-compatibility
val FarmGreenPrimary = FarmForestGreen
val FarmGreenOnPrimary = Color(0xFFFFFFFF)
val FarmGreenPrimaryContainer = FarmForestGreenContainer
val FarmGreenOnPrimaryContainer = FarmForestGreenOnContainer
val FarmGreenSecondary = FarmSageSecondary
val FarmGreenOnSecondary = Color(0xFFFFFFFF)
val FarmGreenSecondaryContainer = FarmSageContainer
val FarmGreenOnSecondaryContainer = FarmSageOnContainer
val FarmAmberTertiary = FeedAmber
val FarmAmberOnTertiary = Color(0xFFFFFFFF)
val FarmAmberTertiaryContainer = FeedAmberContainer
val FarmAmberOnTertiaryContainer = FeedAmberText
val FarmBackgroundLight = FarmWarmBackgroundLight
val FarmOnBackgroundLight = FarmTextPrimaryLight
val FarmOnSurfaceLight = FarmTextPrimaryLight
val FarmSurfaceVariantLightOld = FarmSurfaceVariantLight
val FarmOnSurfaceVariantLight = FarmTextSecondaryLight

val FarmGreenPrimaryDark = FarmForestGreenDark
val FarmGreenOnPrimaryDark = Color(0xFF022C14)
val FarmGreenPrimaryContainerDark = FarmForestGreenContainerDark
val FarmGreenOnPrimaryContainerDark = FarmForestGreenOnContainerDark
val FarmGreenSecondaryDark = FarmSageSecondaryDark
val FarmGreenOnSecondaryDark = Color(0xFF042F2E)
val FarmGreenSecondaryContainerDark = FarmSageContainerDark
val FarmGreenOnSecondaryContainerDark = FarmSageOnContainerDark
val FarmAmberTertiaryDark = FeedAmberDark
val FarmAmberOnTertiaryDark = Color(0xFF331400)
val FarmAmberTertiaryContainerDark = FeedAmberContainerDark
val FarmAmberOnTertiaryContainerDark = Color(0xFFFDE68A)
val FarmOnBackgroundDark = FarmTextPrimaryDark
val FarmOnSurfaceDark = FarmTextPrimaryDark
val FarmOnSurfaceVariantDark = FarmTextSecondaryDark

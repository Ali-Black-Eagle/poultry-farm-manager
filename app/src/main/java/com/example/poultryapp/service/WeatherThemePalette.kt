package com.example.poultryapp.service

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Calendar

data class WeatherCardTheme(
    val backgroundBrush: Brush,
    val borderBrush: Brush,
    val iconBackground: Color,
    val iconTint: Color,
    val titleColor: Color,
    val temperatureColor: Color,
    val badgeBackground: Color,
    val badgeTextColor: Color,
    val atmosphericGlowColor: Color,
    val ambientEmoji: String,
    val isCardDark: Boolean = false
)

object WeatherThemePalette {

    fun getThemeForWeather(
        condition: WeatherConditionType,
        tempCelsius: Double?,
        isDarkTheme: Boolean,
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): WeatherCardTheme {
        val isNight = hourOfDay < 6 || hourOfDay >= 19
        val isExtremeHeat = (tempCelsius ?: 20.0) >= 35.0
        val isExtremeCold = (tempCelsius ?: 20.0) <= -2.0

        return when {
            // ۱. شرایط گرمای شدید (موج گرما بالای ۳۵ درجه)
            isExtremeHeat -> {
                if (isDarkTheme) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFF9A3412))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFFF97316), Color(0xFFEF4444))
                        ),
                        iconBackground = Color(0xFFEF4444).copy(alpha = 0.25f),
                        iconTint = Color(0xFFFDBA74),
                        titleColor = Color(0xFFFFEDD5),
                        temperatureColor = Color(0xFFF97316),
                        badgeBackground = Color(0xFF7F1D1D).copy(alpha = 0.7f),
                        badgeTextColor = Color(0xFFFED7AA),
                        atmosphericGlowColor = Color(0xFFEA580C).copy(alpha = 0.2f),
                        ambientEmoji = "🔥",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFED7AA))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFFF97316).copy(alpha = 0.6f), Color(0xFFEA580C).copy(alpha = 0.4f))
                        ),
                        iconBackground = Color(0xFFFED7AA),
                        iconTint = Color(0xFFC2410C),
                        titleColor = Color(0xFF7C2D12),
                        temperatureColor = Color(0xFFC2410C),
                        badgeBackground = Color(0xFFFFEDD5),
                        badgeTextColor = Color(0xFF9A3412),
                        atmosphericGlowColor = Color(0xFFF97316).copy(alpha = 0.15f),
                        ambientEmoji = "☀️",
                        isCardDark = false
                    )
                }
            }

            // ۲. طوفان و رعد و برق
            condition == WeatherConditionType.THUNDERSTORM -> {
                if (isDarkTheme || isNight) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF1E1035), Color(0xFF2E1065), Color(0xFF172554))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFFA855F7), Color(0xFF60A5FA))
                        ),
                        iconBackground = Color(0xFFA855F7).copy(alpha = 0.25f),
                        iconTint = Color(0xFFC084FC),
                        titleColor = Color(0xFFF3E8FF),
                        temperatureColor = Color(0xFFE9D5FF),
                        badgeBackground = Color(0xFF3B0764).copy(alpha = 0.7f),
                        badgeTextColor = Color(0xFFE9D5FF),
                        atmosphericGlowColor = Color(0xFFA855F7).copy(alpha = 0.25f),
                        ambientEmoji = "⚡",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE), Color(0xFFE0E7FF))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF9333EA).copy(alpha = 0.45f), Color(0xFF4F46E5).copy(alpha = 0.35f))
                        ),
                        iconBackground = Color(0xFFDDD6FE),
                        iconTint = Color(0xFF6B21A8),
                        titleColor = Color(0xFF3B0764),
                        temperatureColor = Color(0xFF581C87),
                        badgeBackground = Color(0xFFEDE9FE),
                        badgeTextColor = Color(0xFF6B21A8),
                        atmosphericGlowColor = Color(0xFF9333EA).copy(alpha = 0.15f),
                        ambientEmoji = "🌩️",
                        isCardDark = false
                    )
                }
            }

            // ۳. باران، باران شدید، نم‌نم
            condition == WeatherConditionType.RAIN ||
            condition == WeatherConditionType.HEAVY_RAIN ||
            condition == WeatherConditionType.DRIZZLE -> {
                if (isDarkTheme || isNight) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF082F49), Color(0xFF0C4A6E), Color(0xFF164E63))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF22D3EE))
                        ),
                        iconBackground = Color(0xFF0284C7).copy(alpha = 0.3f),
                        iconTint = Color(0xFF38BDF8),
                        titleColor = Color(0xFFE0F2FE),
                        temperatureColor = Color(0xFF7DD3FC),
                        badgeBackground = Color(0xFF075985).copy(alpha = 0.6f),
                        badgeTextColor = Color(0xFFBAE6FD),
                        atmosphericGlowColor = Color(0xFF0284C7).copy(alpha = 0.25f),
                        ambientEmoji = "🌧️",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFCFFAFE))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF0284C7).copy(alpha = 0.5f), Color(0xFF0891B2).copy(alpha = 0.35f))
                        ),
                        iconBackground = Color(0xFFBAE6FD),
                        iconTint = Color(0xFF0369A1),
                        titleColor = Color(0xFF0C4A6E),
                        temperatureColor = Color(0xFF0369A1),
                        badgeBackground = Color(0xFFE0F2FE),
                        badgeTextColor = Color(0xFF075985),
                        atmosphericGlowColor = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        ambientEmoji = "💧",
                        isCardDark = false
                    )
                }
            }

            // ۴. برف و یخبندان
            condition == WeatherConditionType.SNOW || isExtremeCold -> {
                if (isDarkTheme || isNight) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF042F2E), Color(0xFF0F766E), Color(0xFF083344))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF2DD4BF), Color(0xFF38BDF8))
                        ),
                        iconBackground = Color(0xFF14B8A6).copy(alpha = 0.25f),
                        iconTint = Color(0xFF5EEAD4),
                        titleColor = Color(0xFFCCFBF1),
                        temperatureColor = Color(0xFF99F6E4),
                        badgeBackground = Color(0xFF115E59).copy(alpha = 0.6f),
                        badgeTextColor = Color(0xFFA7F3D0),
                        atmosphericGlowColor = Color(0xFF14B8A6).copy(alpha = 0.25f),
                        ambientEmoji = "❄️",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFFE0F2FE))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF0D9488).copy(alpha = 0.45f), Color(0xFF0284C7).copy(alpha = 0.35f))
                        ),
                        iconBackground = Color(0xFFCCFBF1),
                        iconTint = Color(0xFF0F766E),
                        titleColor = Color(0xFF134E4A),
                        temperatureColor = Color(0xFF0F766E),
                        badgeBackground = Color(0xFFE6FFFA),
                        badgeTextColor = Color(0xFF115E59),
                        atmosphericGlowColor = Color(0xFF2DD4BF).copy(alpha = 0.18f),
                        ambientEmoji = "⛄",
                        isCardDark = false
                    )
                }
            }

            // ۵. باد شدید
            condition == WeatherConditionType.WINDY -> {
                if (isDarkTheme || isNight) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF064E3B), Color(0xFF0F766E), Color(0xFF134E4A))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF34D399), Color(0xFF2DD4BF))
                        ),
                        iconBackground = Color(0xFF10B981).copy(alpha = 0.25f),
                        iconTint = Color(0xFF6EE7B7),
                        titleColor = Color(0xFFD1FAE5),
                        temperatureColor = Color(0xFFA7F3D0),
                        badgeBackground = Color(0xFF065F46).copy(alpha = 0.6f),
                        badgeTextColor = Color(0xFFA7F3D0),
                        atmosphericGlowColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        ambientEmoji = "💨",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFE0F2FE))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF059669).copy(alpha = 0.45f), Color(0xFF0D9488).copy(alpha = 0.35f))
                        ),
                        iconBackground = Color(0xFFA7F3D0),
                        iconTint = Color(0xFF047857),
                        titleColor = Color(0xFF064E3B),
                        temperatureColor = Color(0xFF047857),
                        badgeBackground = Color(0xFFD1FAE5),
                        badgeTextColor = Color(0xFF065F46),
                        atmosphericGlowColor = Color(0xFF34D399).copy(alpha = 0.15f),
                        ambientEmoji = "🍃",
                        isCardDark = false
                    )
                }
            }

            // ۶. ابری / نیمه‌ابری / مه‌آلود
            condition == WeatherConditionType.CLOUDY ||
            condition == WeatherConditionType.PARTLY_CLOUDY ||
            condition == WeatherConditionType.FOGGY -> {
                if (isDarkTheme || isNight) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF1E293B))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                        ),
                        iconBackground = Color(0xFF475569).copy(alpha = 0.5f),
                        iconTint = Color(0xFFCBD5E1),
                        titleColor = Color(0xFFF1F5F9),
                        temperatureColor = Color(0xFFE2E8F0),
                        badgeBackground = Color(0xFF334155).copy(alpha = 0.7f),
                        badgeTextColor = Color(0xFFCBD5E1),
                        atmosphericGlowColor = Color(0xFF94A3B8).copy(alpha = 0.15f),
                        ambientEmoji = if (condition == WeatherConditionType.FOGGY) "🌫️" else "⛅",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFE0F2FE))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF64748B).copy(alpha = 0.45f), Color(0xFF0284C7).copy(alpha = 0.3f))
                        ),
                        iconBackground = Color(0xFFCBD5E1),
                        iconTint = Color(0xFF334155),
                        titleColor = Color(0xFF0F172A),
                        temperatureColor = Color(0xFF1E293B),
                        badgeBackground = Color(0xFFE2E8F0),
                        badgeTextColor = Color(0xFF334155),
                        atmosphericGlowColor = Color(0xFF64748B).copy(alpha = 0.12f),
                        ambientEmoji = if (condition == WeatherConditionType.FOGGY) "🌫️" else "☁️",
                        isCardDark = false
                    )
                }
            }

            // ۷. صاف و آفتابی شب (Clear Night)
            isNight -> {
                WeatherCardTheme(
                    backgroundBrush = Brush.linearGradient(
                        listOf(Color(0xFF0B1329), Color(0xFF1E1B4B), Color(0xFF0F172A))
                    ),
                    borderBrush = Brush.linearGradient(
                        listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                    ),
                    iconBackground = Color(0xFF312E81).copy(alpha = 0.5f),
                    iconTint = Color(0xFF818CF8),
                    titleColor = Color(0xFFEEF2FF),
                    temperatureColor = Color(0xFFC7D2FE),
                    badgeBackground = Color(0xFF1E1B4B).copy(alpha = 0.7f),
                    badgeTextColor = Color(0xFFC7D2FE),
                    atmosphericGlowColor = Color(0xFF6366F1).copy(alpha = 0.25f),
                    ambientEmoji = "🌙",
                    isCardDark = true
                )
            }

            // ۸. صاف و آفتابی روز (Clear Day - Default)
            else -> {
                if (isDarkTheme) {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFFFBBF24))
                        ),
                        iconBackground = Color(0xFF0284C7).copy(alpha = 0.3f),
                        iconTint = Color(0xFF38BDF8),
                        titleColor = Color(0xFFF0FDF4),
                        temperatureColor = Color(0xFFFDE047),
                        badgeBackground = Color(0xFF1E293B).copy(alpha = 0.6f),
                        badgeTextColor = Color(0xFFFEF08A),
                        atmosphericGlowColor = Color(0xFF0284C7).copy(alpha = 0.2f),
                        ambientEmoji = "☀️",
                        isCardDark = true
                    )
                } else {
                    WeatherCardTheme(
                        backgroundBrush = Brush.linearGradient(
                            listOf(Color(0xFFE0F2FE), Color(0xFFF0FDF4), Color(0xFFFEF9C3))
                        ),
                        borderBrush = Brush.linearGradient(
                            listOf(Color(0xFF0284C7).copy(alpha = 0.45f), Color(0xFFEAB308).copy(alpha = 0.45f))
                        ),
                        iconBackground = Color(0xFFBAE6FD),
                        iconTint = Color(0xFF0284C7),
                        titleColor = Color(0xFF0F172A),
                        temperatureColor = Color(0xFFD97706),
                        badgeBackground = Color(0xFFFEF3C7),
                        badgeTextColor = Color(0xFF92400E),
                        atmosphericGlowColor = Color(0xFFFBBF24).copy(alpha = 0.15f),
                        ambientEmoji = "☀️",
                        isCardDark = false
                    )
                }
            }
        }
    }
}

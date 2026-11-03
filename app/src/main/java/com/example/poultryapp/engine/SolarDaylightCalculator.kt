package com.example.poultryapp.engine

import java.util.Calendar
import java.util.Locale

data class DaylightCalculationResult(
    val daylightHours: Double,
    val formattedDurationFa: String,
    val sunriseApproxFa: String,
    val sunsetApproxFa: String,
    val supplementalLightHoursForLayersFa: String,
    val cityName: String,
    val latitude: Double
)

object SolarDaylightCalculator {

    /**
     * Calculates the daylight photoperiod (in hours and minutes) for a given geographic latitude
     * and day of year using standard astronomical solar equations (NOAA / Spencer formulation).
     *
     * @param latitudeDeg Latitude of the location in degrees (positive for Northern Hemisphere)
     * @param dayOfYear Day of year (1-366). Defaults to current day of year.
     * @param cityName Optional city name for display purposes
     */
    fun calculateDaylight(
        latitudeDeg: Double,
        dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR),
        cityName: String = "ایستگاه فارم"
    ): DaylightCalculationResult {
        val latRad = Math.toRadians(latitudeDeg)

        // Solar declination angle: delta (approximate Spencer / Cooper model)
        // delta = 23.45° * sin(360/365 * (284 + dayOfYear))
        val dayAngle = Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))
        val declinationRad = Math.toRadians(23.45 * Math.sin(dayAngle))

        // Solar hour angle at sunrise/sunset: cos(omega_0) = -tan(phi) * tan(delta)
        val tanProduct = -Math.tan(latRad) * Math.tan(declinationRad)
        val hours = when {
            tanProduct >= 1.0 -> 0.0 // Polar night (not applicable in Iran)
            tanProduct <= -1.0 -> 24.0 // Polar day (not applicable in Iran)
            else -> {
                val omega0 = Math.acos(tanProduct)
                (24.0 / Math.PI) * omega0
            }
        }

        val totalMinutes = (hours * 60.0).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        val formattedDurationFa = "${toPersianDigits(h)} ساعت و ${toPersianDigits(m)} دقیقه"

        // Solar noon approximation around 12:05 local standard time for Iran central meridian (52.5°E)
        val solarNoonHour = 12.08
        val halfDay = hours / 2.0
        val sunriseDec = solarNoonHour - halfDay
        val sunsetDec = solarNoonHour + halfDay

        val riseH = sunriseDec.toInt().coerceIn(0, 23)
        val riseM = ((sunriseDec - riseH) * 60).toInt().coerceIn(0, 59)
        val setH = sunsetDec.toInt().coerceIn(0, 23)
        val setM = ((sunsetDec - setH) * 60).toInt().coerceIn(0, 59)

        val sunriseFa = String.format(Locale.US, "%02d:%02d", riseH, riseM).let { toPersianDigits(it) }
        val sunsetFa = String.format(Locale.US, "%02d:%02d", setH, setM).let { toPersianDigits(it) }

        // Laying hens require 16 hours of light per day for optimal egg production
        val supplemental = (16.0 - hours).coerceAtLeast(0.0)
        val suppH = supplemental.toInt()
        val suppM = ((supplemental - suppH) * 60).toInt()
        val suppFa = if (supplemental > 0.1) {
            "${toPersianDigits(suppH)} ساعت و ${toPersianDigits(suppM)} دقیقه"
        } else {
            "نور طبیعی کافی است"
        }

        return DaylightCalculationResult(
            daylightHours = hours,
            formattedDurationFa = formattedDurationFa,
            sunriseApproxFa = sunriseFa,
            sunsetApproxFa = sunsetFa,
            supplementalLightHoursForLayersFa = suppFa,
            cityName = cityName,
            latitude = latitudeDeg
        )
    }

    /**
     * Converts seconds from Open-Meteo daylight_duration into Persian formatted string
     */
    fun formatSecondsToHoursMinutesFa(seconds: Double): String {
        val totalMinutes = (seconds / 60.0).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "${toPersianDigits(h)} ساعت و ${toPersianDigits(m)} دقیقه"
    }
}


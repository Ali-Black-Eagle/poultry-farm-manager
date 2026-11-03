package com.example.poultryapp.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.poultryapp.data.local.AppSettings
import com.example.poultryapp.data.model.IranCitiesCatalog
import com.example.poultryapp.data.model.IranCity
import com.example.poultryapp.engine.FarmSeason
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class WeatherConditionType(val titleFa: String) {
    CLEAR("آفتابی و صاف"),
    PARTLY_CLOUDY("کمی تا قسمتی ابری"),
    CLOUDY("تمام ابری و مه‌گرفته"),
    FOGGY("مه‌آلود"),
    DRIZZLE("باران نم‌نم"),
    RAIN("بارش باران"),
    HEAVY_RAIN("باران رگباری شدید"),
    SNOW("بارش برف و کولاک"),
    THUNDERSTORM("طوفان و رعد و برق"),
    WINDY("وزش باد شدید")
}

data class WeatherForecastDay(
    val dateIso: String,
    val dayNameFa: String,
    val weatherCode: Int,
    val conditionType: WeatherConditionType,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSumMm: Double,
    val precipitationProbPercent: Int,
    val windSpeedMaxKmH: Double
)

enum class WeatherAlertCategory {
    RAIN,
    COLD_FROST,
    HEAT_WAVE,
    STORM_WIND,
    FAVORABLE
}

data class WeatherEventAlert(
    val category: WeatherAlertCategory,
    val titleFa: String,
    val descriptionFa: String,
    val dayLabelFa: String,
    val isSevere: Boolean = false
)

data class WeatherResult(
    val temperatureCelsius: Double,
    val isOnline: Boolean,
    val cityName: String,
    val province: String,
    val sourceDescription: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val humidityPercent: Int? = null,
    val weatherCode: Int? = null,
    val conditionType: WeatherConditionType = WeatherConditionType.CLEAR,
    val windSpeedKmH: Double? = null,
    val forecastDays: List<WeatherForecastDay> = emptyList(),
    val upcomingAlerts: List<WeatherEventAlert> = emptyList(),
    val daylightDurationSeconds: Double? = null,
    val daylightDurationFa: String? = null,
    val sunriseFa: String? = null,
    val sunsetFa: String? = null,
    val supplementalLightingFa: String? = null
)

data class OpenMeteoPayload(
    val currentTemp: Double,
    val currentHumidity: Int?,
    val currentWeatherCode: Int?,
    val currentWindSpeed: Double?,
    val forecastDays: List<WeatherForecastDay>,
    val alerts: List<WeatherEventAlert>,
    val daylightDurationSeconds: Double? = null,
    val sunriseIso: String? = null,
    val sunsetIso: String? = null
)

object WeatherAndLocationService {

    // بروزرسانی هر ۵ دقیقه یکبار
    const val WEATHER_CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes

    fun mapWmoCodeToCondition(code: Int): WeatherConditionType {
        return when (code) {
            0 -> WeatherConditionType.CLEAR
            1, 2 -> WeatherConditionType.PARTLY_CLOUDY
            3 -> WeatherConditionType.CLOUDY
            45, 48 -> WeatherConditionType.FOGGY
            51, 53, 55 -> WeatherConditionType.DRIZZLE
            61, 63 -> WeatherConditionType.RAIN
            65, 80, 81, 82 -> WeatherConditionType.HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> WeatherConditionType.SNOW
            95, 96, 99 -> WeatherConditionType.THUNDERSTORM
            else -> WeatherConditionType.CLEAR
        }
    }

    /**
     * دریافت همزمان دمای لحظه‌ای و پیش‌بینی چندروزه رویدادهای جوی از Open-Meteo
     */
    suspend fun fetchOnlineWeatherFull(latitude: Double, longitude: Double): OpenMeteoPayload? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,weather_code,precipitation,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,daylight_duration,sunrise,sunset&timezone=auto&forecast_days=4",
                latitude,
                longitude
            )
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6500
                readTimeout = 6500
                setRequestProperty("User-Agent", "PoultryFarmApp-Android/2.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val rootJson = JSONObject(response)

                var currentTemp = 22.0
                var currentHumidity: Int? = null
                var currentWeatherCode: Int? = null
                var currentWindSpeed: Double? = null

                if (rootJson.has("current")) {
                    val currentObj = rootJson.getJSONObject("current")
                    if (currentObj.has("temperature_2m")) {
                        currentTemp = currentObj.getDouble("temperature_2m")
                    }
                    if (currentObj.has("relative_humidity_2m")) {
                        currentHumidity = currentObj.getInt("relative_humidity_2m")
                    }
                    if (currentObj.has("weather_code")) {
                        currentWeatherCode = currentObj.getInt("weather_code")
                    }
                    if (currentObj.has("wind_speed_10m")) {
                        currentWindSpeed = currentObj.getDouble("wind_speed_10m")
                    }
                }

                val forecastList = mutableListOf<WeatherForecastDay>()
                val alertsList = mutableListOf<WeatherEventAlert>()

                var todayDaylightSec: Double? = null
                var todaySunriseIso: String? = null
                var todaySunsetIso: String? = null

                if (rootJson.has("daily")) {
                    val dailyObj = rootJson.getJSONObject("daily")
                    val timeArr = dailyObj.optJSONArray("time")
                    val codeArr = dailyObj.optJSONArray("weather_code")
                    val maxArr = dailyObj.optJSONArray("temperature_2m_max")
                    val minArr = dailyObj.optJSONArray("temperature_2m_min")
                    val precipSumArr = dailyObj.optJSONArray("precipitation_sum")
                    val precipProbArr = dailyObj.optJSONArray("precipitation_probability_max")
                    val windArr = dailyObj.optJSONArray("wind_speed_10m_max")

                    val daylightArr = dailyObj.optJSONArray("daylight_duration")
                    if (daylightArr != null && daylightArr.length() > 0) {
                        todayDaylightSec = daylightArr.optDouble(0)
                    }
                    val sunriseArr = dailyObj.optJSONArray("sunrise")
                    if (sunriseArr != null && sunriseArr.length() > 0) {
                        todaySunriseIso = sunriseArr.optString(0)
                    }
                    val sunsetArr = dailyObj.optJSONArray("sunset")
                    if (sunsetArr != null && sunsetArr.length() > 0) {
                        todaySunsetIso = sunsetArr.optString(0)
                    }

                    val count = timeArr?.length() ?: 0
                    for (i in 0 until count) {
                        val dateIso = timeArr?.optString(i) ?: ""
                        val wCode = codeArr?.optInt(i, 0) ?: 0
                        val maxT = maxArr?.optDouble(i, currentTemp + 3) ?: (currentTemp + 3)
                        val minT = minArr?.optDouble(i, currentTemp - 4) ?: (currentTemp - 4)
                        val pSum = precipSumArr?.optDouble(i, 0.0) ?: 0.0
                        val pProb = precipProbArr?.optInt(i, 0) ?: 0
                        val wind = windArr?.optDouble(i, 12.0) ?: 12.0

                        val dayLabel = when (i) {
                            0 -> "امروز"
                            1 -> "فردا"
                            2 -> "پس‌فردا"
                            3 -> "۳ روز آینده"
                            else -> "روز ${i + 1}"
                        }

                        val cond = mapWmoCodeToCondition(wCode)
                        val forecastDay = WeatherForecastDay(
                            dateIso = dateIso,
                            dayNameFa = dayLabel,
                            weatherCode = wCode,
                            conditionType = cond,
                            tempMax = maxT,
                            tempMin = minT,
                            precipitationSumMm = pSum,
                            precipitationProbPercent = pProb,
                            windSpeedMaxKmH = wind
                        )
                        forecastList.add(forecastDay)

                        // تحلیل هوشمند رویدادهای جوی برای مرغداری
                        if (pProb >= 35 || pSum >= 1.0) {
                            val pSumStr = String.format(Locale.US, "%.1f", pSum)
                            alertsList.add(
                                WeatherEventAlert(
                                    category = WeatherAlertCategory.RAIN,
                                    titleFa = "احتمال بارندگی (احتمال ${toPersianDigits(pProb)}٪)",
                                    descriptionFa = "بارش باران (${toPersianDigits(pSumStr)} میلی‌متر) در $dayLabel؛ مراقبت از رطوبت بستر سالن، آب‌بندی درزها و جلوگیری از رطوبت‌زدگی انبار دان ضروری است.",
                                    dayLabelFa = dayLabel,
                                    isSevere = pSum >= 5.0 || pProb >= 70
                                )
                            )
                        }

                        if (minT <= 5.0) {
                            alertsList.add(
                                WeatherEventAlert(
                                    category = WeatherAlertCategory.COLD_FROST,
                                    titleFa = "سرمای شبانه (حداقل ${toPersianDigits(minT.toInt())} درجه)",
                                    descriptionFa = "سرمای هوا در $dayLabel؛ کنترل ترموستات هیترها، افزایش ضخامت پوشال بستر و پیشگیری از کوران باد روی پرندگان توصیه می‌شود.",
                                    dayLabelFa = dayLabel,
                                    isSevere = minT <= 2.0
                                )
                            )
                        }

                        if (maxT >= 31.0) {
                            alertsList.add(
                                WeatherEventAlert(
                                    category = WeatherAlertCategory.HEAT_WAVE,
                                    titleFa = "گرمای شدید هوا (حداکثر ${toPersianDigits(maxT.toInt())} درجه)",
                                    descriptionFa = "افزایش گرما در $dayLabel؛ فعال‌سازی پد سلولزی و مه‌پاش، قطع دان در ساعات اوج گرما و افزودن الکترولیت به آب آشامیدنی ضروری است.",
                                    dayLabelFa = dayLabel,
                                    isSevere = maxT >= 34.0
                                )
                            )
                        }

                        if (wind >= 38.0) {
                            alertsList.add(
                                WeatherEventAlert(
                                    category = WeatherAlertCategory.STORM_WIND,
                                    titleFa = "وزش تندباد و طوفان (${toPersianDigits(wind.toInt())} کیلومتر/ساعت)",
                                    descriptionFa = "باد شدید در $dayLabel؛ بررسی استحکام دریچه‌های اینلت، پرده‌های برزنتی و مهار پنجره‌های سالن لازم است.",
                                    dayLabelFa = dayLabel,
                                    isSevere = wind >= 50.0
                                )
                            )
                        }
                    }
                }

                if (alertsList.isEmpty()) {
                    alertsList.add(
                        WeatherEventAlert(
                            category = WeatherAlertCategory.FAVORABLE,
                            titleFa = "شرایط جوی مساعد و پایدار",
                            descriptionFa = "در روزهای آینده تغییرات اقلیمی شدیدی پیش‌بینی نشده است. تهویه سالن را متناسب با سن پرنده تنظیم فرمایید.",
                            dayLabelFa = "روزهای آتی",
                            isSevere = false
                        )
                    )
                }

                return@withContext OpenMeteoPayload(
                    currentTemp = currentTemp,
                    currentHumidity = currentHumidity,
                    currentWeatherCode = currentWeatherCode,
                    currentWindSpeed = currentWindSpeed,
                    forecastDays = forecastList,
                    alerts = alertsList,
                    daylightDurationSeconds = todayDaylightSec,
                    sunriseIso = todaySunriseIso,
                    sunsetIso = todaySunsetIso
                )
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * روش پشتیبانی برای دریافت آنلاین تک‌دما
     */
    suspend fun fetchOnlineTemperature(latitude: Double, longitude: Double): Double? {
        val payload = fetchOnlineWeatherFull(latitude, longitude)
        return payload?.currentTemp
    }

    /**
     * دریافت موقعیت مکانی دستگاه
     */
    fun detectDeviceLocation(context: Context): Location? {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePerm != PackageManager.PERMISSION_GRANTED && coarsePerm != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Ignore security exceptions
            } catch (e: Exception) {
                // Provider might not be available
            }
        }

        return bestLocation
    }

    /**
     * پیش‌بینی تخمینی فصلی آفلاین بر اساس اقلیم شهر
     */
    private fun generateOfflineForecast(city: IranCity?, baseTemp: Double, season: FarmSeason): Pair<List<WeatherForecastDay>, List<WeatherEventAlert>> {
        val days = listOf("امروز", "فردا", "پس‌فردا", "۳ روز آینده")
        val forecast = days.mapIndexed { idx, label ->
            val offset = if (idx % 2 == 0) 0.5 else -0.5
            WeatherForecastDay(
                dateIso = "",
                dayNameFa = label,
                weatherCode = if (season == FarmSeason.AUTUMN || season == FarmSeason.WINTER) 2 else 0,
                conditionType = if (season == FarmSeason.AUTUMN || season == FarmSeason.WINTER) WeatherConditionType.PARTLY_CLOUDY else WeatherConditionType.CLEAR,
                tempMax = baseTemp + 4.0 + offset,
                tempMin = baseTemp - 4.0 + offset,
                precipitationSumMm = if (season == FarmSeason.AUTUMN || season == FarmSeason.WINTER) 1.5 else 0.0,
                precipitationProbPercent = if (season == FarmSeason.AUTUMN || season == FarmSeason.WINTER) 25 else 5,
                windSpeedMaxKmH = 14.0
            )
        }

        val alerts = mutableListOf<WeatherEventAlert>()
        when (season) {
            FarmSeason.AUTUMN -> {
                alerts.add(
                    WeatherEventAlert(
                        category = WeatherAlertCategory.RAIN,
                        titleFa = "احتمال رطوبت و بارندگی پاییزی",
                        descriptionFa = "با آغاز پاییز و کاهش دمای شبانه، مراقب رطوبت بستر و کوران ورودی‌های هوا باشید (در انتظار اتصال اینترنت برای دریافت زنده).",
                        dayLabelFa = "فصل جاری",
                        isSevere = false
                    )
                )
            }
            FarmSeason.WINTER -> {
                alerts.add(
                    WeatherEventAlert(
                        category = WeatherAlertCategory.COLD_FROST,
                        titleFa = "سرمای زمستانی و نیاز به گرمایش",
                        descriptionFa = "دمای میانگین زمستان برای ${city?.name ?: "منطقه"}؛ هیترها و تهویه حداقلی را بررسی نمایید.",
                        dayLabelFa = "فصل جاری",
                        isSevere = false
                    )
                )
            }
            FarmSeason.SUMMER -> {
                alerts.add(
                    WeatherEventAlert(
                        category = WeatherAlertCategory.HEAT_WAVE,
                        titleFa = "گرمای تابستان و استرس حرارتی",
                        descriptionFa = "سیستم‌های خنک‌کننده پد و مه‌پاش را فعال و آماده نگه دارید.",
                        dayLabelFa = "فصل جاری",
                        isSevere = false
                    )
                )
            }
            FarmSeason.SPRING -> {
                alerts.add(
                    WeatherEventAlert(
                        category = WeatherAlertCategory.FAVORABLE,
                        titleFa = "هوای معتدل بهاری",
                        descriptionFa = "شرایط فصلی پایدار؛ تنظیم تهویه متناسب با دمای استاندارد سن گله.",
                        dayLabelFa = "فصل جاری",
                        isSevere = false
                    )
                )
            }
        }

        return Pair(forecast, alerts)
    }

    /**
     * استخراج داده‌های آب و هوایی از آخرین کش معتبر یا میانگین فصلی اقلیمی در صورت عدم اتصال اینترنت
     */
    fun getCachedOrOfflineWeather(settings: AppSettings): WeatherResult? {
        val cityName = settings.farmCity.trim()
        if (cityName.isBlank() && settings.cachedTempCelsius == null) {
            return null
        }

        val city = IranCitiesCatalog.findCityByName(cityName)
        val now = System.currentTimeMillis()
        val currentSeason = JalaliDateHelper.getCurrentSeason(now)
        val isCacheAvailable = settings.cachedTempCelsius != null
        val effectiveTemp = settings.cachedTempCelsius
            ?: (city?.let { IranCitiesCatalog.getSeasonalAvgTemp(it, currentSeason) } ?: 22.0)

        val (offlineForecast, offlineAlerts) = generateOfflineForecast(city, effectiveTemp, currentSeason)
        val latForSolar = settings.farmLatitude ?: (city?.lat ?: 35.6892)
        val daylightInfo = com.example.poultryapp.engine.SolarDaylightCalculator.calculateDaylight(
            latitudeDeg = latForSolar,
            cityName = if (cityName.isNotBlank()) cityName else (city?.name ?: "ایستگاه مزرعه")
        )

        return WeatherResult(
            temperatureCelsius = effectiveTemp,
            isOnline = false,
            cityName = if (cityName.isNotBlank()) cityName else (city?.name ?: "ایستگاه مزرعه"),
            province = settings.farmProvince.ifBlank { city?.province ?: "" },
            sourceDescription = if (isCacheAvailable) "آخرین دمای بروزرسانی‌شده (آفلاین)" else "آفلاین — میانگین فصل ${currentSeason.titleFa}",
            timestampMillis = if (isCacheAvailable && settings.lastWeatherFetchMillis > 0) settings.lastWeatherFetchMillis else now,
            humidityPercent = settings.cachedHumidityPercent,
            weatherCode = settings.cachedWeatherCode,
            conditionType = settings.cachedWeatherCode?.let { mapWmoCodeToCondition(it) }
                ?: (if (currentSeason == FarmSeason.AUTUMN || currentSeason == FarmSeason.WINTER) WeatherConditionType.PARTLY_CLOUDY else WeatherConditionType.CLEAR),
            windSpeedKmH = settings.cachedWindSpeedKmH,
            forecastDays = offlineForecast,
            upcomingAlerts = offlineAlerts,
            daylightDurationSeconds = daylightInfo.daylightHours * 3600.0,
            daylightDurationFa = daylightInfo.formattedDurationFa,
            sunriseFa = daylightInfo.sunriseApproxFa,
            sunsetFa = daylightInfo.sunsetApproxFa,
            supplementalLightingFa = daylightInfo.supplementalLightHoursForLayersFa
        )
    }

    /**
     * استخراج دمای موثر و رویدادهای جوی فارم
     */
    suspend fun resolveEffectiveTemperature(
        settings: AppSettings,
        forceOnlineRefresh: Boolean = false
    ): WeatherResult? {
        val cityName = settings.farmCity.trim()
        if (cityName.isBlank()) {
            return null
        }

        val city = IranCitiesCatalog.findCityByName(cityName)
        val lat = settings.farmLatitude ?: city?.lat
        val lon = settings.farmLongitude ?: city?.lon

        val now = System.currentTimeMillis()
        val isCacheFresh = settings.cachedTempCelsius != null &&
                (now - settings.lastWeatherFetchMillis) < WEATHER_CACHE_VALIDITY_MS

        // اگر کمتر از ۵ دقیقه گذشته و کاربر اجبار نکرده، از کش استفاده می‌کنیم
        if (isCacheFresh && !forceOnlineRefresh) {
            return getCachedOrOfflineWeather(settings)?.copy(
                isOnline = true,
                sourceDescription = "آنلاین بروزرسانی‌شده (هر ۵ دقیقه)"
            )
        }

        // تلاش برای استعلام کامل آنلاین از Open-Meteo
        if (settings.isOnlineWeatherEnabled && lat != null && lon != null) {
            val payload = fetchOnlineWeatherFull(lat, lon)
            if (payload != null) {
                val cond = mapWmoCodeToCondition(payload.currentWeatherCode ?: 0)
                val daylightInfo = if (payload.daylightDurationSeconds != null && payload.daylightDurationSeconds > 0) {
                    val formatted = com.example.poultryapp.engine.SolarDaylightCalculator.formatSecondsToHoursMinutesFa(payload.daylightDurationSeconds)
                    val hours = payload.daylightDurationSeconds / 3600.0
                    val supp = (16.0 - hours).coerceAtLeast(0.0)
                    val suppFa = if (supp > 0.1) {
                        val sH = supp.toInt()
                        val sM = ((supp - sH) * 60).toInt()
                        "${toPersianDigits(sH)} ساعت و ${toPersianDigits(sM)} دقیقه"
                    } else "نور طبیعی کافی است"

                    val riseFa = payload.sunriseIso?.substringAfter("T")?.take(5)?.let { toPersianDigits(it) } ?: "--:--"
                    val setFa = payload.sunsetIso?.substringAfter("T")?.take(5)?.let { toPersianDigits(it) } ?: "--:--"
                    com.example.poultryapp.engine.DaylightCalculationResult(
                        daylightHours = hours,
                        formattedDurationFa = formatted,
                        sunriseApproxFa = riseFa,
                        sunsetApproxFa = setFa,
                        supplementalLightHoursForLayersFa = suppFa,
                        cityName = cityName,
                        latitude = lat
                    )
                } else {
                    com.example.poultryapp.engine.SolarDaylightCalculator.calculateDaylight(
                        latitudeDeg = lat,
                        cityName = cityName
                    )
                }

                return WeatherResult(
                    temperatureCelsius = payload.currentTemp,
                    isOnline = true,
                    cityName = cityName,
                    province = settings.farmProvince.ifBlank { city?.province ?: "" },
                    sourceDescription = "آنلاین زنده (ایستگاه اقلیمی)",
                    timestampMillis = now,
                    humidityPercent = payload.currentHumidity,
                    weatherCode = payload.currentWeatherCode,
                    conditionType = cond,
                    windSpeedKmH = payload.currentWindSpeed,
                    forecastDays = payload.forecastDays,
                    upcomingAlerts = payload.alerts,
                    daylightDurationSeconds = daylightInfo.daylightHours * 3600.0,
                    daylightDurationFa = daylightInfo.formattedDurationFa,
                    sunriseFa = daylightInfo.sunriseApproxFa,
                    sunsetFa = daylightInfo.sunsetApproxFa,
                    supplementalLightingFa = daylightInfo.supplementalLightHoursForLayersFa
                )
            }
        }

        // در صورت عدم دسترسی به اینترنت یا خطا در استعلام آنلاین:
        // آخرین دمای بروزشده ذخیره‌شده حفظ می‌شود تا دما ریست نشود
        return getCachedOrOfflineWeather(settings)
    }
}

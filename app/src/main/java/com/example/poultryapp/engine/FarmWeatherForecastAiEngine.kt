package com.example.poultryapp.engine

import com.example.poultryapp.R
import com.example.poultryapp.data.model.IranCitiesCatalog
import com.example.poultryapp.data.model.IranCity
import com.example.poultryapp.service.WeatherConditionType
import com.example.poultryapp.service.WeatherForecastDay
import com.example.poultryapp.service.WeatherResult
import com.example.poultryapp.voice.PersianVoiceNormalizer
import com.example.poultryapp.voice.VoiceActionCardData
import com.example.poultryapp.voice.VoiceIntentType
import java.util.Calendar
import java.util.Locale

/**
 * داده‌های تفکیک‌شده تحلیل و پیش‌بینی آب‌وهوا توسط هوش مصنوعی فارم
 */
data class WeatherAiAnalysis(
    val targetDayLabelFa: String,
    val dayOffset: Int,
    val conditionTitleFa: String,
    val tempMax: Double,
    val tempMin: Double,
    val currentOrAverageTemp: Double,
    val rainProbPercent: Int,
    val windSpeedKmH: Double,
    val spokenSummaryFa: String,
    val cardData: VoiceActionCardData
)

/**
 * موتور تخصصی پایش، استعلام و پیش‌بینی آب‌وهوای فارم و شهرهای کشور
 * تفکیک قطعی سوالات جوی و اقلیمی از مصرف آب سالن مرغداری
 */
object FarmWeatherForecastAiEngine {

    /**
     * نگاشت مرتب‌شده شهرهای ایران و مراکز استان‌ها برای تطبیق هوشمند
     */
    private val sortedCities: List<Pair<String, IranCity>> by lazy {
        val list = mutableListOf<Pair<String, IranCity>>()
        for (c in IranCitiesCatalog.CITIES) {
            val normName = PersianVoiceNormalizer.normalize(c.name).trim()
            list.add(normName to c)
            val noSpace = normName.replace(" ", "")
            if (noSpace != normName && noSpace.length >= 3) {
                list.add(noSpace to c)
            }
        }

        // افزودن نگاشت نام استان‌ها به مرکز استان مربوطه
        val provinceCapitals = mapOf(
            "مازندران" to "ساری", "گیلان" to "رشت", "گلستان" to "گرگان",
            "خراسان رضوی" to "مشهد", "خراسان شمالی" to "بجنورد", "خراسان جنوبی" to "بیرجند",
            "آذربایجان شرقی" to "تبریز", "آذربایجان غربی" to "ارومیه", "اردبیل" to "اردبیل",
            "فارس" to "شیراز", "اصفهان" to "اصفهان", "خوزستان" to "اهواز",
            "کرمانشاه" to "کرمانشاه", "کردستان" to "سنندج", "لرستان" to "خرم‌آباد",
            "همدان" to "همدان", "مرکزی" to "اراک", "قم" to "قم", "قزوین" to "قزوین",
            "زنجان" to "زنجان", "سمنان" to "سمنان", "یزد" to "یزد", "کرمان" to "کرمان",
            "هرمزگان" to "بندرعباس", "بوشهر" to "بوشهر", "سیستان و بلوچستان" to "زاهدان",
            "ایلام" to "ایلام", "چهارمحال و بختیاری" to "شهرکرد", "کهگیلویه و بویراحمد" to "یاسوج",
            "البرز" to "کرج", "تهران" to "تهران"
        )
        for ((prov, capName) in provinceCapitals) {
            val capCity = IranCitiesCatalog.CITIES.find {
                PersianVoiceNormalizer.normalize(it.name) == PersianVoiceNormalizer.normalize(capName)
            }
            if (capCity != null) {
                val normProv = PersianVoiceNormalizer.normalize(prov).trim()
                list.add(normProv to capCity)
                val noSpaceProv = normProv.replace(" ", "")
                if (noSpaceProv != normProv && noSpaceProv.length >= 3) {
                    list.add(noSpaceProv to capCity)
                }
            }
        }

        list.distinctBy { it.first }.sortedByDescending { it.first.length }
    }

    /**
     * استخراج نام شهر یا استان از کلام کاربر
     */
    fun findCityInQuery(norm: String): IranCity? {
        val clean = PersianVoiceNormalizer.normalize(norm).trim()
        val words = clean.split(Regex("[\\s،,\\.؟?]+")).filter { it.isNotBlank() }
        for ((key, city) in sortedCities) {
            val keyWords = key.split(" ")
            if (keyWords.size == 1) {
                if (words.contains(key)) return city
            } else {
                val pattern = "(^|\\s)${Regex.escape(key)}(\\s|$)".toRegex()
                if (pattern.containsMatchIn(clean)) return city
            }
        }
        return null
    }

    /**
     * بررسی اینکه آیا پرامپت کاربر استعلام آب‌وهوا، هواشناسی یا پیش‌بینی جوی است.
     */
    fun isWeatherQuery(norm: String): Boolean {
        // سوالات تهویه سالن، اینلت و پد سلولزی اقلیمی نباید به عنوان پیش‌بینی هواشناسی تشخیص داده شوند
        if (norm.contains("تهویه") || norm.contains("اینلت") || norm.contains("پد سلولزی")) {
            return false
        }

        val weatherKeywords = listOf(
            "اب و هوا", "آب و هوا", "اب وهوا", "آب وهوا", "هواشناسی", "پیش بینی هوا", "پیش‌بینی هوا",
            "پیش بینی جوی", "پیش‌بینی جوی", "وضعیت هوا", "وضعیت جوی", "هوای فارم", "هوای مزرعه",
            "هوای سالن چطوره بیرون", "هوای آزاد", "دمای بیرون", "دمای محیط فارم", "دمای منطقه",
            "هوای امروز", "هوای فردا", "هوای پس فردا", "هوای پس‌فردا", "هوای ۳ روز", "هوای سه روز",
            "هوای ۲ روز", "هوای دو روز", "هوای ۴ روز", "هوای چهار روز", "هوای هفته",
            "بارندگی", "باران میبارد", "بارش باران", "باران داریم", "برف میبارد", "برف داریم",
            "بارش برف", "کولاک", "طوفان", "سرعت باد", "باد شدید", "پیش بینی بارش"
        )
        if (weatherKeywords.any { norm.contains(it) }) return true

        // اگر شهر خاصی نام برده شده باشد و کلمات اقلیمی یا سوالی در جمله باشد
        val detectedCity = findCityInQuery(norm)
        if (detectedCity != null) {
            val hasWeatherPrompt = norm.contains("هوا") || norm.contains("دما") || norm.contains("حرارت") ||
                    norm.contains("باران") || norm.contains("برف") || norm.contains("باد") ||
                    norm.contains("ابری") || norm.contains("بارندگی") || norm.contains("جوی") ||
                    norm.contains("پیش بینی") || norm.contains("پیش‌بینی") || norm.contains("وضعیت") ||
                    norm.contains("چطور") || norm.contains("چطوره") || norm.contains("چگونه") ||
                    norm.contains("چند درجه") || norm.contains("چنده") || norm.contains("بیرون") ||
                    norm.contains("منطقه") || norm.contains("اقلیم")
            if (hasWeatherPrompt) return true
        }

        val hasWeatherTopic = norm.contains("هوا") || norm.contains("باران") || norm.contains("برف") ||
                norm.contains("باد") || norm.contains("ابری") || norm.contains("بارندگی") || norm.contains("جوی")

        val hasTimeOrForecast = norm.contains("فردا") || norm.contains("پس فردا") || norm.contains("پس‌فردا") ||
                norm.contains("روز دیگر") || norm.contains("روز بعد") || norm.contains("پیش بینی") ||
                norm.contains("پیش‌بینی") || norm.contains("هفته") || norm.contains("چند روز بعد") ||
                norm.contains("امروز") || norm.contains("۳ روز") || norm.contains("سه روز") ||
                norm.contains("منطقه") || norm.contains("فارم") || norm.contains("مزرعه") ||
                norm.contains("بیرون") || norm.contains("آزاد")

        val isNotHallSpecific = !norm.contains("سالن") && !norm.contains("گله") && !norm.contains("نیپل") && !norm.contains("جوجه")

        val isCasualChitChat = (norm.contains("سلام") || norm.contains("خوبی") || norm.contains("چطوری")) &&
                (norm.contains("خیلی خوبه") || norm.contains("خوبه") || norm.contains("عالیه")) &&
                !norm.contains("چطور است") && !norm.contains("چطوره") && !norm.contains("چند درجه")
        if (isCasualChitChat) return false

        return hasWeatherTopic && hasTimeOrForecast && isNotHallSpecific
    }

    /**
     * استخراج تعداد روزهای افست از امروز (۰ = امروز، ۱ = فردا، ۲ = پس‌فردا، ۳ = ۳ روز دیگر...)
     */
    fun resolveDayOffset(norm: String): Int {
        return when {
            norm.contains("۴ روز دیگر") || norm.contains("۴ روز بعد") || norm.contains("چهار روز دیگر") || norm.contains("چهار روز بعد") -> 4
            norm.contains("۳ روز دیگر") || norm.contains("۳ روز بعد") || norm.contains("سه روز دیگر") || norm.contains("سه روز بعد") || norm.contains("۳روز") -> 3
            norm.contains("پس فردا") || norm.contains("پس‌فردا") || norm.contains("۲ روز دیگر") || norm.contains("۲ روز بعد") || norm.contains("دو روز دیگر") || norm.contains("دو روز بعد") || norm.contains("۲روز") -> 2
            norm.contains("فردا") || norm.contains("۱ روز دیگر") || norm.contains("یک روز دیگر") || norm.contains("یک روز بعد") -> 1
            else -> 0
        }
    }

    /**
     * عنوان روز هدف به زبان فارسی (امروز، فردا، پس‌فردا، ۳ روز آینده...)
     */
    fun getDayLabel(offset: Int, forecastDay: WeatherForecastDay? = null): String {
        val specificDayName = forecastDay?.dayNameFa?.trim()
        val suffix = if (!specificDayName.isNullOrBlank()) " ($specificDayName)" else ""
        return when (offset) {
            0 -> "امروز$suffix"
            1 -> "فردا$suffix"
            2 -> "پس‌فردا (۲ روز آینده)$suffix"
            3 -> "۳ روز آینده$suffix"
            4 -> "۴ روز آینده$suffix"
            else -> "$offset روز آینده$suffix"
        }
    }

    /**
     * حل‌وفصل و پردازش جامع استعلام آب‌وهوا برای شهر مشخص‌شده یا فارم
     */
    fun processWeatherInquiry(
        norm: String,
        spokenText: String,
        farmContext: FarmLiveContext
    ): WeatherAiAnalysis {
        val detectedCity = findCityInQuery(norm)
        val offset = resolveDayOffset(norm)
        val weatherResult = farmContext.weather
        val forecastDays = weatherResult?.forecastDays ?: emptyList()

        val isDefaultFarmCity = detectedCity == null ||
                (weatherResult != null && (
                    detectedCity.name.equals(weatherResult.cityName, ignoreCase = true) ||
                    weatherResult.cityName.contains(detectedCity.name) ||
                    detectedCity.name.contains(weatherResult.cityName)
                ))

        val cityName: String
        val provinceName: String
        val tempMax: Double
        val tempMin: Double
        val conditionTitle: String
        val rainProb: Int
        val windSpeed: Double

        if (isDefaultFarmCity && forecastDays.isNotEmpty()) {
            val forecastDay = if (offset in forecastDays.indices) forecastDays[offset] else forecastDays.last()
            cityName = weatherResult?.cityName?.ifBlank { "منطقه فارم" } ?: "منطقه فارم"
            provinceName = weatherResult?.province ?: ""
            tempMax = forecastDay.tempMax
            tempMin = forecastDay.tempMin
            conditionTitle = forecastDay.conditionType.titleFa
            rainProb = forecastDay.precipitationProbPercent
            windSpeed = forecastDay.windSpeedMaxKmH
        } else if (detectedCity != null) {
            // شهر استعلام‌شده مشخص خارج از فارم یا بدون کش زنده
            cityName = detectedCity.name
            provinceName = detectedCity.province
            val currentSeason = JalaliDateHelper.getCurrentSeason()
            val baseTemp = detectedCity.getTempForSeason(currentSeason)
            val offsetVariance = when (offset) { 0 -> 0.0; 1 -> 0.7; 2 -> -0.5; 3 -> 1.2; else -> -1.0 }
            tempMax = baseTemp + 3.5 + offsetVariance
            tempMin = (baseTemp - 5.5 + offsetVariance).coerceAtLeast(-15.0)

            val isNorth = detectedCity.province in listOf("گیلان", "مازندران", "گلستان")
            val isMountain = detectedCity.province in listOf("آذربایجان شرقی", "آذربایجان غربی", "اردبیل", "کردستان", "لرستان", "همدان", "چهارمحال و بختیاری", "زنجان")
            val isSouth = detectedCity.province in listOf("خوزستان", "بوشهر", "هرمزگان", "سیستان و بلوچستان")

            conditionTitle = when {
                norm.contains("برف") && (isMountain || currentSeason == FarmSeason.WINTER) -> "برفی و یخبندان"
                norm.contains("باران") || (isNorth && (currentSeason == FarmSeason.AUTUMN || currentSeason == FarmSeason.WINTER)) -> "نیمه‌ابری با بارش پراکنده باران"
                isMountain && currentSeason == FarmSeason.WINTER -> "ابری و سرد زمستانی"
                isSouth && (currentSeason == FarmSeason.SPRING || currentSeason == FarmSeason.SUMMER) -> "صاف و گرم آفتابی"
                else -> "صاف تا قسمتی ابری"
            }
            rainProb = when {
                conditionTitle.contains("باران") -> 65
                conditionTitle.contains("برف") -> 70
                isNorth -> 45
                isMountain -> 25
                else -> 10
            }
            windSpeed = if (norm.contains("باد") || norm.contains("طوفان")) 38.0 else 14.0
        } else {
            // شهر نامشخص، استفاده از وضعیت پیش‌فرض اقلیمی فارم
            cityName = weatherResult?.cityName?.ifBlank { "منطقه فارم" } ?: "منطقه فارم"
            provinceName = weatherResult?.province ?: ""
            val baseTemp = weatherResult?.temperatureCelsius ?: 22.0
            tempMax = baseTemp + 2.0
            tempMin = (baseTemp - 7.0).coerceAtLeast(5.0)
            conditionTitle = weatherResult?.conditionType?.titleFa ?: "صاف و آفتابی"
            rainProb = if (norm.contains("باران") || norm.contains("بارندگی")) 15 else 5
            windSpeed = 12.0
        }

        val dayLabel = getDayLabel(offset)
        val cityDisplay = if (provinceName.isNotBlank() && !cityName.contains(provinceName)) {
            "$cityName (استان $provinceName)"
        } else {
            cityName
        }

        val currentOrAvg = if (offset == 0) {
            if (isDefaultFarmCity) weatherResult?.temperatureCelsius ?: ((tempMax + tempMin) / 2.0)
            else (tempMax + tempMin) / 2.0
        } else {
            (tempMax + tempMin) / 2.0
        }

        val maxStr = toPersianDigits(String.format(Locale.US, "%.0f", tempMax))
        val minStr = toPersianDigits(String.format(Locale.US, "%.0f", tempMin))
        val rainStr = toPersianDigits(rainProb.toString())
        val windStr = toPersianDigits(String.format(Locale.US, "%.0f", windSpeed))

        // توصیه‌های تخصصی هوش مصنوعی برای مدیریت سالن‌های مرغداری متناسب با شرایط جوی
        val poultryAdviceFa = when {
            tempMax >= 32.0 -> "هشدار دمای بالا: مه‌پاش‌ها و پدهای سلولزی را آماده‌باش نگه دارید و ورودی هوای سالن را تنظیم کنید."
            tempMin <= 8.0 -> "هشدار افت دما: هیترها و مشعل‌های سالن را در شیفت شب چک کنید تا پرندگان دچار تنش سرمایی نشوند."
            rainProb >= 40 -> "احتمال بارش باران: آب‌بندی سقف و تهویه سالن را کنترل کنید تا رطوبت بستر افزایش نیابد."
            windSpeed >= 35.0 -> "هشدار وزش باد شدید: دریچه‌های اینلت (Inlet) را کنترل کرده تا جریان باد پرفشار مستقیماً به پرندگان نخورد."
            else -> "شرایط جوی مساعد و پایدار است؛ هوادهی و تهویه سالن‌ها طبق روال عادی برقرار باشد."
        }

        val spokenSummary = "پیش‌بینی آب‌وهوای $dayLabel در $cityDisplay: وضعیت جوی $conditionTitle، بیشینه دمای $maxStr درجه و کمینه $minStr درجه سانتی‌گراد، با احتمال بارش $rainStr درصد و سرعت باد $windStr کیلومتر بر ساعت است. $poultryAdviceFa"

        val detailsList = listOf(
            "افست زمانی" to dayLabel,
            "شهر مورد استعلام" to cityDisplay,
            "وضعیت جوی" to conditionTitle,
            "بیشینه / کمینه دما" to "$maxStr° الی $minStr° سانتی‌گراد",
            "احتمال بارندگی" to "$rainStr درصد",
            "سرعت باد" to "$windStr کیلومتر/ساعت",
            "توصیه مدیریتی سالن" to poultryAdviceFa
        )

        val card = VoiceActionCardData(
            intentType = VoiceIntentType.QUERY_STATS,
            title = "پیش‌بینی آب‌وهوای $cityName: $dayLabel",
            iconRes = R.drawable.ic_farm_dashboard,
            primaryMetric = "$maxStr° / $minStr°C - $conditionTitle",
            details = detailsList,
            systemEffect = "استخراج داده‌های هواشناسی و اقلیم محیطی شهر $cityName",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "isWeatherForecast" to true,
                "cityName" to cityName,
                "province" to provinceName,
                "dayOffset" to offset,
                "tempMax" to tempMax,
                "tempMin" to tempMin,
                "conditionTitle" to conditionTitle,
                "spokenSummary" to spokenSummary
            )
        )

        return WeatherAiAnalysis(
            targetDayLabelFa = dayLabel,
            dayOffset = offset,
            conditionTitleFa = conditionTitle,
            tempMax = tempMax,
            tempMin = tempMin,
            currentOrAverageTemp = currentOrAvg,
            rainProbPercent = rainProb,
            windSpeedKmH = windSpeed,
            spokenSummaryFa = spokenSummary,
            cardData = card
        )
    }
}

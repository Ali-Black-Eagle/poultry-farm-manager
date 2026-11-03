package com.example.poultryapp.engine

import com.example.poultryapp.R
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.voice.VoiceActionCardData
import com.example.poultryapp.voice.VoiceIntentType
import java.util.Locale

/**
 * موتور تخصصی محاسبات اقلیم، تهویه حداقل و تونلی، فشار استاتیک و آلودگی گازهای سالن مرغداری
 * (Poultry Ventilation, Climate & Environmental Physics Engine)
 *
 * قابلیت‌ها:
 * ۱. محاسبه نرخ تهویه حداقل (Minimum Ventilation) بر حسب توان ۰.۷۵ وزن زنده گله و چرخه تایمر ۵ دقیقه‌ای
 * ۲. محاسبه تهویه تونلی تابستانه، سرعت باد (Air Speed)، خنک‌سازی باد (Wind Chill) و تعداد فن‌های ۱۴۰ سانتی‌متری
 * ۳. محاسبه فشار منفی (Static Pressure) و تنظیم دهانه اینلت‌ها بر اساس عرض سالن
 * ۴. مدیریت پد کولینگ سلولزی و هشدارهای سایکومتریک رطوبت نسبی (>۷۵٪)
 * ۵. آستانه‌های مجاز و بحرانی گازهای آمونیاک (NH3)، دی‌اکسید کربن (CO2) و مونوکسید کربن (CO)
 */
object PoultryVentilationClimateEngine {

    fun isVentilationQuery(norm: String): Boolean {
        if (norm.contains("برق") || norm.contains("ژنراتور")) return false
        if ((norm.contains("له له") || norm.contains("له‌له")) && !norm.contains("eti") && !norm.contains("دمای موثر") && !norm.contains("تهویه")) return false
        return norm.contains("تهویه") || norm.contains("اینلت") || norm.contains("فن") ||
                norm.contains("هواکش") || norm.contains("فشار منفی") || norm.contains("پد سلولزی") ||
                norm.contains("پد خنک") || norm.contains("باد سالن") || norm.contains("سرعت باد") ||
                norm.contains("ویند چیل") || norm.contains("wind chill") || norm.contains("تهویه انتقالی") ||
                norm.contains("تهویه حداقل") || norm.contains("تهویه تونلی") || norm.contains("گاز امونیاک") ||
                norm.contains("گاز آمونیاک") || norm.contains("دی اکسید کربن") || norm.contains("مونوکسید") ||
                norm.contains("eti") || norm.contains("دمای موثر") || norm.contains("شاخص موثر") || norm.contains("شاخص eti") || norm.contains("دمای حسی") ||
                (norm.contains("هوا") && (norm.contains("میزان") || norm.contains("چقدر") || norm.contains("محاسبه")))
    }

    fun calculateVentilationParameters(
        spokenText: String,
        norm: String,
        targetFlock: Flock?
    ): VoiceActionCardData {
        val birdCount = targetFlock?.effectiveBirdCount ?: 10000
        val ageDays = targetFlock?.ageDays ?: 25
        val flockName = targetFlock?.name ?: "سالن استاندارد"

        val isEtiQuery = norm.contains("eti") || norm.contains("دمای موثر") || norm.contains("شاخص موثر") || norm.contains("شاخص eti") || norm.contains("دمای حسی")
        val isWindChillQuery = norm.contains("ویند چیل") || norm.contains("wind chill") || norm.contains("سرمایش باد")
        val isTransitionQuery = norm.contains("تهویه انتقالی") || (norm.contains("انتقالی") && norm.contains("تهویه"))
        val isTunnel = norm.contains("تونلی") || norm.contains("تابستان") || norm.contains("سرعت باد") || norm.contains("گرمازدگی")
        val isStaticPressure = norm.contains("فشار") || norm.contains("اینلت") || norm.contains("پاسکال")
        val isPadCooling = norm.contains("پد") || norm.contains("سلولز") || norm.contains("خنک کننده")
        val isGasQuery = norm.contains("گاز") || norm.contains("امونیاک") || norm.contains("آمونیاک") || norm.contains("co2")

        if (isEtiQuery) {
            val tempMatch = Regex("""(?:دمای|دما|حرارت)?\s*(\d+[\d\.,]*)\s*(?:درجه)""").find(norm)
            val T = tempMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 32.0

            val rhMatch = Regex("""(?:رطوبت|rh)?\s*(\d+[\d\.,]*)\s*(?:درصد|٪)""").find(norm)
            val RH = rhMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 60.0

            val vMatch = Regex("""(?:سرعت\s*باد|سرعت)?\s*(\d+[\d\.,]*)\s*(?:متر\s*بر\s*ثانیه|متر|m/s)""").find(norm)
            val v = vMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 2.0

            val windChillEffect = 1.8 * Math.pow(v, 0.6)
            val eti = T - 0.2 * (T - 14.0) * (1.0 - (RH / 100.0)) - windChillEffect

            val zoneTitle = when {
                eti < 18.0 -> "استرس سرمایی ❄️ (Cold Stress)"
                eti <= 24.0 -> "محدوده آسایش حرارتی مطلوب 🟢 (Comfort Zone)"
                eti <= 28.0 -> "استرس گرمایی ملایم 🟡 (Mild Heat Stress)"
                eti <= 32.0 -> "استرس گرمایی متوسط 🟠 (Moderate Heat Stress)"
                else -> "استرس گرمایی حاد و کشنده 🔴 (Lethal Heat Crisis)"
            }

            val details = mutableListOf(
                "شاخص دمای موثر (ETI)" to "${toPersianDigits(String.format(Locale.US, "%.1f", eti))} درجه سانتی‌گراد",
                "وضعیت آسایش حرارتی" to zoneTitle,
                "دمای محیطی دماسنج خشک (T)" to "${toPersianDigits(String.format(Locale.US, "%.1f", T))} درجه سانتی‌گراد",
                "رطوبت نسبی سالن (RH)" to "${toPersianDigits(String.format(Locale.US, "%.0f", RH))} درصد",
                "سرعت باد روی پرنده (v)" to "${toPersianDigits(String.format(Locale.US, "%.1f", v))} متر بر ثانیه (سرمایش باد: ${toPersianDigits(String.format(Locale.US, "%.1f", windChillEffect))}°C)",
                "فرمول استاندارد فیزیک طیور" to "ETI = T - 0.2*(T-14)*(1 - RH/100) - 1.8*v^0.6",
                "اقدام پیشنهادی اقلیم" to if (eti > 28.0) "افزایش سرعت هوای تونلی به بالای ۲.۲ متر بر ثانیه و فعال‌سازی پدها" else "حفظ شرایط فعلی تهویه و پایش مداوم رطوبت"
            )

            val spokenSummary = "دمای موثر ETI در دمای ${toPersianDigits(String.format(Locale.US, "%.0f", T))} درجه و رطوبت ${toPersianDigits(String.format(Locale.US, "%.0f", RH))} درصد با سرعت باد ${toPersianDigits(String.format(Locale.US, "%.1f", v))} متر بر ثانیه برابر با ${toPersianDigits(String.format(Locale.US, "%.1f", eti))} درجه سانتی‌گراد است که در «$zoneTitle» قرار دارد. فرمول علمی ETI = T - 0.2*(T-14)*(1 - RH/100) - 1.8*v^0.6 اثر خنک‌کنندگی ویند چیل را لحاظ می‌کند."

            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه شاخص دمای موثر (Effective Temperature Index - ETI) 🌪️🌡️",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "شاخص ETI: ${toPersianDigits(String.format(Locale.US, "%.1f", eti))}°C ($zoneTitle)",
                details = details,
                systemEffect = "محاسبه اثر ترکیبی دما، رطوبت نسبی و سرعت باد بر حس حرارتی پرنده",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "eti" to eti,
                    "zone" to zoneTitle,
                    "spokenSummary" to spokenSummary
                )
            )
        }

        if (isWindChillQuery) {
            val details = mutableListOf(
                "سرعت باد ۱.۰ متر بر ثانیه" to "کاهش دمای حسی پرنده حدود ۲ الی ۳ درجه سانتی‌گراد",
                "سرعت باد ۲.۰ متر بر ثانیه" to "کاهش دمای حسی پرنده حدود ۵ الی ۶ درجه سانتی‌گراد",
                "سرعت باد ۲.۵ الی ۲.۸ متر بر ثانیه" to "کاهش دمای حسی ۷ الی ۸ درجه (نقطه بهینه مرغ گوشتی بالغ)",
                "شرط پرریزی و سن گله" to "سرعت باد بالای ۱.۵ متر فقط برای سنین بالای ۲۵ تا ۲۸ روزگی (با پوشش پر کامل) مجاز است",
                "خطر در سنین پایین" to "اعمال باد تونلی روی جوجه با پرهای ناقص موجب استرس سرمایی، آسیت و افت ایمنی می‌شود"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه اثر سرمایش بادی (Wind Chill) در تهویه تونلی 🌪️🧊",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "افت حس دمایی به میزان ۵ الی ۸ درجه با سرعت ۲.۵ متر بر ثانیه",
                details = details,
                systemEffect = "محاسبه آیرودینامیک افت دمای احساسی بدن پرنده با وزش باد",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "سرعت باد در تهویه تونلی موجب اثر سرمایش باد یا ویند چیل می‌شود. باد با سرعت ۲.۵ متر بر ثانیه دمای احساسی بدن مرغ بالغ را تا ۷ درجه خنک‌تر می‌کند؛ اما این باد فقط برای سنین بالای ۲۵ روز با پوشش کامل پر مجاز است."
                )
            )
        }

        if (isTransitionQuery) {
            val details = mutableListOf(
                "هدف تهویه انتقالی" to "افزایش حجم هوادهی با فن‌های بزرگتر بدون ایجاد سرعت باد سرد روی پرندگان",
                "محرک ورود به فاز انتقالی" to "وقتی دمای سالن ۱ الی ۲ درجه بالاتر از دمای هدف می‌رود اما هنوز نیاز به تونلی و پد نیست",
                "مکانیزم ورودی و خروجی" to "بسته بودن پرده تونلی؛ ورود هوا صرفاً از اینلت‌های دیواره جانبی و تخلیه با ۱ تا ۳ فن ۱۴۰",
                "تنظیم پنجره‌های اینلت" to "بازشدگی حداکثری اینلت‌های جانبی برای ورود حجم هوای بالاتر بدون افت فشار منفی استاندارد"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "مدیریت و شرایط سوئیچ به تهویه انتقالی (Transition Ventilation) 🔄🌬️",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "جابجایی هوشمند بین تهویه حداقل اینلت‌ها و تهویه تونلی بدون نوسان دمایی",
                details = details,
                systemEffect = "حفظ تعادل دمایی سالن در فصول بهار و پاییز با تهویه انتقالی",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "تهویه انتقالی پلی میان تهویه حداقل و تونلی است. هنگامی که دمای سالن یک تا دو درجه بالا می‌رود، فن‌های بزرگ بدون باز کردن پرده تونلی روشن می‌شوند و هوا از اینلت‌ها وارد می‌شود تا بدون ایجاد کوران باد روی جوجه، گرمای اضافی خارج گردد."
                )
            )
        }

        if (isPadCooling) {
            val details = mutableListOf(
                "آستانه شروع به کار پدها" to "دمای سالن بالای ۲۸ الی ۲۹ درجه سانتی‌گراد",
                "خط قرمز و بحران رطوبت" to "در رطوبت نسبی بالاتر از ۷۵ الی ۸۰ درصد، پدها باید فوراً خاموش شوند",
                "علت خاموشی در رطوبت بالا" to "پرنده با لهیب زدن و تبخیر تنفسی خنک می‌شود؛ هوای شرجی مانع دفع حرارت شده و باعث خفگی سریع گله می‌گردد",
                "روش کارکرد پمپ" to "تایمری باشد (مثلاً ۱ دقیقه روشن / ۲ الی ۳ دقیقه خاموش) تا پدها مرطوب بمانند نه غرقاب",
                "سرعت باد روی سطح پد" to "۱.۵ الی ۱.۸ متر بر ثانیه برای تبخیر بهینه و جلوگیری از پاشش قطرات آب"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "پروتکل مدیریت پدهای سلولزی و خنک‌کننده سالن",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "هشدار سایکومتریک: خاموشی پد در رطوبت بالای ۷۵٪",
                details = details,
                systemEffect = "استاندارد جهانی خنک‌سازی تبخیری و جلوگیری از خفگی پرندگان در فصول گرم",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "پدهای سلولزی باید تنها در دمای بالای ۲۸ درجه وارد مدار شوند. مهم‌ترین اصل حیاتی: در رطوبت بالای ۷۵ تا ۸۰ درصد پمپ آب پدها باید کاملاً قطع شود، زیرا شرجی شدن هوا مانع لهیب زدن پرنده شده و تلفات سنگین خفگی می‌دهد."
                )
            )
        }

        if (isStaticPressure) {
            val details = mutableListOf(
                "عرض سالن ۱۲ متر" to "۲۰ الی ۲۵ پاسکال (فشار منفی مطلوب)",
                "عرض سالن ۱۴ متر" to "۲۵ الی ۳۰ پاسکال",
                "عرض سالن ۱۶ الی ۱۸ متر" to "۳۰ الی ۳۸ پاسکال",
                "ارتفاع پرتاب هوا" to "هوای ورودی از پنجره اینلت باید دقیقاً به مرکز سقف (تاج سالن) پرتاب شود تا با هوای گرم زیر سقف مخلوط گردد",
                "نشانه افت فشار (زیر ۱۵ پاسکال)" to "هوای سرد بدون اختلاط مستقیم روی سر پرندگان سقوط کرده و ایجاد آسیت، تاول سینه و سرماخوردگی می‌کند",
                "تنظیم دهانه اینلت" to "بازشو نباید کمتر از ۳ الی ۴ سانتی‌متر باشد تا جریان جت هوای پیوسته حفظ گردد"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "تنظیم فشار استاتیک و دهانه پنجره‌های اینلت (Inlet)",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "فشار استاندارد: ۲۵ تا ۳۵ پاسکال بر اساس عرض سالن",
                details = details,
                systemEffect = "محاسبه آیرودینامیک حرکت توده‌های هوا در سقف سالن مرغداری",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "فشار استاتیک مطلوب برای سالن‌های ۱۲ متری ۲۰ تا ۲۵ پاسکال و برای سالن‌های ۱۴ تا ۱۶ متری ۲۵ تا ۳۵ پاسکال است. این فشار باعث می‌شود هوای خنک ورودی به تاج سقف پرتاب شده و پس از گرم شدن به آرامی روی گله پایین بیاید."
                )
            )
        }

        if (isGasQuery) {
            val details = mutableListOf(
                "گاز آمونیاک (NH3)" to "حداکثر مجاز زیر ۱۵ ppm (در بالای ۲۰ ppm سوزش چشم انسان و آسیب مژک‌های نای رخ می‌دهد)",
                "دی‌اکسید کربن (CO2)" to "حداکثر مجاز زیر ۳۰۰۰ ppm (در بالای ۵۰۰۰ ppm باعث آسیت و افت ضریب تبدیل می‌شود)",
                "مونوکسید کربن (CO)" to "حداکثر مجاز زیر ۱۰ ppm (ناشی از احتراق ناقص هیترها، فوق‌العاده سمی و کشنده)",
                "رطوبت نسبی سالن" to "بازه استاندارد ۵۵ الی ۶۵ درصد",
                "اقدام اضطراری در بوی گاز" to "افزایش زمان روشن بودن تایمرهای تهویه حداقل به میزان ۲۰ الی ۳۰ درصد و بازرسی خطوط نیپل"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "استاندارد غلظت گازهای آلاینده سالن مرغداری",
                iconRes = R.drawable.ic_farm_health,
                primaryMetric = "آمونیاک زیر ۱۵ ppm | دی‌اکسید کربن زیر ۳۰۰۰ ppm",
                details = details,
                systemEffect = "شاخص‌های استاندارد پایش گازهای تنفسی و ایمنی زیستی گله",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "استاندارد حداکثر مجاز گاز آمونیاک زیر ۱۵ پی‌پی‌ام و دی‌اکسید کربن زیر ۳۰۰۰ پی‌پی‌ام است. گاز مونوکسید کربن ناشی از هیترها نباید از ۱۰ پی‌پی‌ام فراتر رود."
                )
            )
        }

        if (isTunnel) {
            // محاسبه تهویه تونلی تابستانه
            val avgWeightKg = when {
                ageDays <= 10 -> 0.3
                ageDays <= 20 -> 0.8
                ageDays <= 30 -> 1.5
                ageDays <= 40 -> 2.4
                else -> 2.8
            }
            val targetAirSpeed = "۲.۲ الی ۲.۸ متر بر ثانیه"
            val windChillEffect = "۴ الی ۶ درجه سانتی‌گراد کاهش حس دمایی (Wind Chill)"
            val fan140Capacity = 40000.0 // ظرفیت هر فن ۱۴۰ سانتیمتری به مترمکعب در ساعت
            val totalBiomassTons = (birdCount * avgWeightKg) / 1000.0
            val tunnelM3PerHour = birdCount * (if (avgWeightKg > 1.5) 8.0 else 5.0)
            val fansNeeded = Math.ceil(tunnelM3PerHour / fan140Capacity).toInt().coerceAtLeast(2)

            val details = mutableListOf(
                "سرعت باد هدف در ارتفاع پرنده" to targetAirSpeed,
                "اثر سرمایش باد (Wind Chill)" to windChillEffect,
                "کل دبی هوای تونلی مورد نیاز" to "${toPersianDigits(String.format(Locale.US, "%.0f", tunnelM3PerHour))} مترمکعب در ساعت",
                "تعداد هواکش ۱۴۰ مورد نیاز" to "${toPersianDigits(fansNeeded)} دستگاه (با ظرفیت ۴۰ هزار مترمکعب)",
                "زمان تعویض کامل هوای سالن" to "کمتر از ۵۰ الی ۶۰ ثانیه",
                "شرط ورود به تهویه تونلی" to "دمای سالن حداقل ۲ الی ۳ درجه بالاتر از دمای آسایش سن گله باشد"
            )

            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه تهویه تونلی تابستانه گله «$flockName»",
                iconRes = R.drawable.ic_farm_dashboard,
                primaryMetric = "سرعت باد ۲.۵ متر بر ثانیه (${toPersianDigits(fansNeeded)} فن ۱۴۰)",
                details = details,
                systemEffect = "محاسبات دینامیک سرمایش بادی (Wind Chill) برای مهار استرس گرمایی",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "ventilation" to true,
                    "spokenSummary" to "برای تهویه تونلی گله «$flockName»، سرعت باد هدف ۲.۲ تا ۲.۸ متر بر ثانیه است که سرمایش بادی ۴ تا ۶ درجه ایجاد می‌کند. کل دبی مورد نیاز حدود ${toPersianDigits(String.format(Locale.US, "%.0f", tunnelM3PerHour))} مترمکعب در ساعت معادل ${toPersianDigits(fansNeeded)} عدد فن ۱۴۰ سانتی‌متری است."
                )
            )
        }

        // تهویه حداقل زمستانه بر اساس سن و وزن
        val avgWeightKg = when {
            ageDays <= 7 -> 0.18
            ageDays <= 14 -> 0.45
            ageDays <= 21 -> 0.90
            ageDays <= 28 -> 1.50
            ageDays <= 35 -> 2.20
            else -> 2.70
        }
        // ضریب حداقل: توان ۰.۷۵ وزن زنده * ۰.۷ الی ۱ مترمکعب بر ساعت
        val metabolicWeight = Math.pow(avgWeightKg, 0.75)
        val minRatePerBird = (metabolicWeight * 0.8).coerceAtLeast(0.12)
        val totalMinM3PerHour = birdCount * minRatePerBird

        // تایمر ۵ دقیقه‌ای (۳۰۰ ثانیه) با فن ۱۴۰ (۴۰ هزار مترمکعب) یا ۱۰۰ (۲۰ هزار)
        val fanCapacityUsed = 20000.0 // فرض بر یک فن ۱۰۰ برای تهویه حداقل
        val fractionRunning = (totalMinM3PerHour / fanCapacityUsed).coerceIn(0.10, 0.95)
        val onSeconds = Math.round(fractionRunning * 300).toInt().coerceAtLeast(30)
        val offSeconds = 300 - onSeconds

        val details = mutableListOf(
            "نرخ تهویه حداقل هر پرنده" to "${toPersianDigits(String.format(Locale.US, "%.2f", minRatePerBird))} مترمکعب در ساعت",
            "کل دبی حداقل سالن (${toPersianDigits(birdCount)} قطعه)" to "${toPersianDigits(String.format(Locale.US, "%.0f", totalMinM3PerHour))} مترمکعب بر ساعت",
            "تنظیم تایمر سیکل ۵ دقیقه‌ای (۳۰۰ ثانیه)" to "${toPersianDigits(onSeconds)} ثانیه روشن / ${toPersianDigits(offSeconds)} ثانیه خاموش",
            "تجهیزات مورد استفاده" to "۱ دستگاه فن ۱۰۰ سانتی‌متری با فشار منفی ۲۰ تا ۲۵ پاسکال",
            "حداقل زمان روشن بودن مجاز" to "نباید کمتر از ۳۰ الی ۴۵ ثانیه باشد تا هوای سالن تخلیه شود",
            "هدف اصلی" to "تخلیه دی‌اکسید کربن و رطوبت بدون سرد کردن کف و بستر گله"
        )

        return VoiceActionCardData(
            intentType = VoiceIntentType.QUERY_STATS,
            title = "محاسبه نرخ تهویه حداقل (Minimum Ventilation) گله «$flockName»",
            iconRes = R.drawable.ic_farm_dashboard,
            primaryMetric = "تایمر تهویه: ${toPersianDigits(onSeconds)} ثانیه روشن / ${toPersianDigits(offSeconds)} ثانیه خاموش",
            details = details,
            systemEffect = "محاسبات متابولیک تبادل هوای حداقل بر پایه وزن فیزیولوژیک گله",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "ventilation" to true,
                "spokenSummary" to "برای سن ${toPersianDigits(ageDays)} روزگی گله «$flockName»، تهویه حداقل کل سالن ${toPersianDigits(String.format(Locale.US, "%.0f", totalMinM3PerHour))} مترمکعب در ساعت است. تنظیم بهینه تایمر در چرخه ۵ دقیقه‌ای: ${toPersianDigits(onSeconds)} ثانیه روشن و ${toPersianDigits(offSeconds)} ثانیه خاموش با فن تهویه است."
            )
        )
    }
}

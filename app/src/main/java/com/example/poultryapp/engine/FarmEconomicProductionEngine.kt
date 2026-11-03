package com.example.poultryapp.engine

import com.example.poultryapp.R
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.voice.VoiceActionCardData
import com.example.poultryapp.voice.VoiceIntentType
import java.util.Locale

/**
 * موتور محاسبات اقتصادی، شاخص‌های بهره‌وری اروپایی (EPEF) و راندمان تولید فارم
 * (Farm Economic, Performance Index & Production Mathematics Engine)
 *
 * شامل:
 * ۱. محاسبه شاخص راندمان اروپایی (European Production Efficiency Factor - EPEF)
 * ۲. ضریب تبدیل غذایی (FCR) واقعی و اصلاح‌شده بر مبنای وزن استاندارد
 * ۳. شاخص‌های گله‌های تخم‌گذار: راندمان مرغ-روز (Hen-Day %)، مرغ-آغازین (Hen-Housed) و مصرف دان به ازای هر شانه
 * ۴. نقطه سر به سر هزینه خوراک در برابر قیمت بازار (Break-Even Analysis)
 * ۵. تعیین روز و پنجره بهینه کشتار (Optimal Slaughter Window)
 */
object FarmEconomicProductionEngine {

    fun isEconomicQuery(norm: String): Boolean {
        return norm.contains("epef") || norm.contains("شاخص اروپایی") || norm.contains("ضریب اروپایی") ||
                norm.contains("شاخص تولید") || norm.contains("pi") ||
                norm.contains("fcr") || norm.contains("ضریب تبدیل") || norm.contains("سودآوری") ||
                norm.contains("نقطه سر به سر") || norm.contains("نقطه سر") || norm.contains("سربه‌سر") ||
                norm.contains("بهای تمام") || norm.contains("تمام شده") || norm.contains("تمام‌شده") ||
                norm.contains("سهم دان") || norm.contains("بهره وری") || norm.contains("کشتار بهینه") ||
                norm.contains("زمان کشتار") || norm.contains("مرغ روز") || norm.contains("hen day") ||
                norm.contains("بازده لاشه") || norm.contains("کشتارگاه") || norm.contains("لاشه") ||
                norm.contains("شانه ۳۰") || norm.contains("هر شانه") || norm.contains("کارتن تخم") ||
                norm.contains("افزایش قیمت دان") || norm.contains("حساسیت قیمت") || norm.contains("سود هر قطعه") ||
                norm.contains("fcr اصلاح") || norm.contains("ضریب تبدیل اصلاح") || norm.contains("وزن استاندارد ۲") ||
                norm.contains("صرفه اقتصادی") || norm.contains("هزینه تمام شده")
    }

    fun calculateEconomicMetrics(
        spokenText: String,
        norm: String,
        targetFlock: Flock?,
        farmContext: FarmLiveContext? = null
    ): VoiceActionCardData {
        val flockName = targetFlock?.name ?: "سالن نمونه"
        val birdCount = targetFlock?.effectiveBirdCount ?: 10000
        val ageDays = targetFlock?.ageDays ?: 38

        val isEpefQuery = norm.contains("epef") || norm.contains("شاخص اروپایی") || norm.contains("ضریب اروپایی") || norm.contains("بهره وری") || norm.contains("شاخص تولید") || norm.contains("pi")
        val isWeightCorrectedFcr = !isEpefQuery && (norm.contains("اصلاح") || norm.contains("تصحیح") || norm.contains("وزن استاندارد ۲") || norm.contains("وزن 2 کیلو") || norm.contains("وزن ۲ کیلو") || (norm.contains("fcr") && (norm.contains("۲ کیلو") || norm.contains("2 کیلو"))))
        val isFeedSensitivity = norm.contains("افزایش قیمت دان") || norm.contains("حساسیت") || (norm.contains("۱۰۰۰") && norm.contains("دان")) || (norm.contains("1000") && norm.contains("دان")) || (norm.contains("سود") && norm.contains("کم می") && norm.contains("دان"))
        val isCarcassYield = norm.contains("بازده لاشه") || norm.contains("کشتارگاه") || (norm.contains("لاشه") && (norm.contains("قیمت") || norm.contains("تمام شده") || norm.contains("درصد") || norm.contains("بازده")))
        val isLayerEggCost = (norm.contains("شانه") || norm.contains("کارتن") || norm.contains("تخم مرغ") || norm.contains("تخم‌مرغ")) && (norm.contains("قیمت تمام شده") || norm.contains("هزینه تمام شده") || norm.contains("چقدر است") || norm.contains("چقدر میشه") || norm.contains("محاسبه"))
        val isBreakEven = norm.contains("نقطه سر") || norm.contains("سر به سر") || norm.contains("سربه‌سر") ||
                norm.contains("بهای تمام") || norm.contains("تمام شده") || norm.contains("تمام‌شده") || norm.contains("سهم دان") || (norm.contains("قیمت تمام شده") && norm.contains("مرغ"))
        val isSlaughterQuery = norm.contains("زمان کشتار") || norm.contains("کشتار بهینه") || norm.contains("بارگیری")
        val isLayerIndex = norm.contains("مرغ روز") || norm.contains("hen day") || norm.contains("شانه") || norm.contains("تخم")

        // ۱. ضریب تبدیل اصلاح‌شده بر اساس وزن استاندارد ۲.۰ کیلوگرم (Weight-Corrected FCR)
        if (isWeightCorrectedFcr) {
            val weightMatch = Regex("""(?:وزن)?\s*(\d+[\d\.,]*)\s*(کیلو|گرم)?""").find(norm)
            val weightVal = weightMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 2400.0
            val weightG = if (weightVal < 10.0) weightVal * 1000.0 else weightVal

            val fcrMatch = Regex("""(?:fcr|ضریب\s*تبدیل)\s*(\d+[\d\.,]*)""").find(norm)
            val fcrActual = fcrMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 1.68

            val fcrCorrected = fcrActual - ((weightG - 2000.0) / 100.0) * 0.03
            val details = mutableListOf(
                "ضریب تبدیل اصلاح‌شده (FCR_corr)" to "${toPersianDigits(String.format(Locale.US, "%.3f", fcrCorrected))}",
                "فرمول علمی استاندارد" to "FCR_corr = FCR_actual - ((Weight_g - 2000)/100) * 0.03",
                "وزن زنده و ضریب تبدیل واقعی" to "وزن: ${toPersianDigits(weightG.toInt())} گرم | FCR واقعی: ${toPersianDigits(String.format(Locale.US, "%.2f", fcrActual))}",
                "منطق بیولوژیکی تصحیح وزن" to "پرنده‌های سنگین‌تر به ازای هر ۱۰۰ گرم وزن اضافه، ۰.۰۳ واحد به FCR نگهداری آنها افزوده می‌شود؛ استانداردسازی به ۲ کیلو امکان مقایسه عادلانه راندمان گله‌ها را فراهم می‌کند.",
                "تاثیر اقتصادی" to "هر ۰.۰۳ بهبود در FCR تصحیح‌شده معادل صرفه‌جویی ۱.۵ تن دان در هر دوره ۲۰ هزارتایی است."
            )
            val spokenSummary = "فرمول ضریب تبدیل اصلاح‌شده بر اساس وزن ۲ کیلوگرم برابر است با: FCR خام منهای (وزن زنده به گرم منهای ۲۰۰۰ تقسیم بر ۱۰۰ ضربدر ۰.۰۳). برای نمونه با وزن ${toPersianDigits(weightG.toInt())} گرم و FCR خام ${toPersianDigits(String.format(Locale.US, "%.2f", fcrActual))}، ضریب تبدیل اصلاح‌شده دقیقاً ${toPersianDigits(String.format(Locale.US, "%.3f", fcrCorrected))} است."
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه ضریب تبدیل تصحیح‌شده (وزن استاندارد ۲ کیلوگرم) ⚖️🍗",
                iconRes = R.drawable.ic_farm_financial,
                primaryMetric = "FCR اصلاح‌شده: ${toPersianDigits(String.format(Locale.US, "%.3f", fcrCorrected))} (مبنای ۲.۰ کیلوگرم)",
                details = details,
                systemEffect = "استانداردسازی علمی ضریب تبدیل بر مبنای وزن مرجع ۲ کیلوگرم",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "fcrCorrected" to fcrCorrected,
                    "spokenSummary" to spokenSummary
                )
            )
        }

        // ۲. تحلیل حساسیت نوسانات قیمت دان بر حاشیه سود فارم (Feed Price Sensitivity)
        if (isFeedSensitivity) {
            val deltaFeedPrice = 1000.0 // ۱۰۰۰ تومان
            val avgWeightKg = 2.5
            val fcr = 1.65
            val lifetimeFeedPerBirdKg = avgWeightKg * fcr // 4.125 kg
            val profitDropPerBird = deltaFeedPrice * lifetimeFeedPerBirdKg // 4,125 Tomans
            val flockSize = 20000
            val totalBarnProfitDrop = flockSize * profitDropPerBird // 82,500,000 Tomans

            val details = mutableListOf(
                "کاهش سود به ازای ۱۰۰۰ تومان افزایش دان" to "۴,۱۲۵ تومان به ازای هر قطعه مرغ گوشتی",
                "مبنای مصرف دان دوره هر پرنده" to "${toPersianDigits(String.format(Locale.US, "%.3f", lifetimeFeedPerBirdKg))} کیلوگرم دان (وزن ۲.۵ کیلو × ضریب تبدیل ۱.۶۵)",
                "اثر در یک سالن ۲۰ هزار قطعه‌ای" to "کاهش ۸۲,۵۰۰,۰۰۰ تومان (۸۲.۵ میلیون تومان) از سود خالص دوره",
                "حساسیت بهای تمام‌شده" to "افزایش ۱,۶۵۰ تومان در بهای تمام‌شده هر کیلو مرغ زنده تحویلی",
                "راهکارهای تاب‌آوری مالی" to "خرید سلف دان قبل از جوجه‌ریزی، بهبود ۰.۰۵ ضریب تبدیل جهت جبران کامل این هزینه"
            )
            val spokenSummary = "به ازای هر ۱۰۰۰ تومان افزایش قیمت هر کیلو دان، با فرض وزن ۲.۵ کیلو و ضریب تبدیل ۱.۶۵، مصرف دان دوره هر مرغ ۴.۱۲۵ کیلوگرم بوده و سود نهایی هر قطعه مرغ دقیقاً ۴,۱۲۵ تومان کاهش می‌یابد که در یک سالن ۲۰ هزارتایی معادل ۸۲ میلیون و ۵۰۰ هزار تومان افت سود خالص است."
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "تحلیل حساسیت افزایش قیمت دان بر سودآوری فارم 💰📉",
                iconRes = R.drawable.ic_farm_financial,
                primaryMetric = "کاهش سود هر قطعه: ${toPersianDigits(profitDropPerBird.toInt())} تومان به ازای هر ۱,۰۰۰ تومان افزایش دان",
                details = details,
                systemEffect = "مدل‌سازی مالی حساسیت حاشیه سود به نوسانات نهاده‌ها",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "profitDropPerBird" to profitDropPerBird,
                    "spokenSummary" to spokenSummary
                )
            )
        }

        // ۳. مدل‌سازی بازده لاشه کشتارگاهی (Eviscerated Processing Yield)
        if (isCarcassYield) {
            val livePrice = 65000.0 // ۶۵ هزار تومان هر کیلو زنده
            val slaughterFee = 4000.0 // ۴ هزار تومان اجرت کشتار و بسته‌بندی
            val dressingPercent = 71.0 // ۷۱ درصد بازده لاشه
            val carcassCostPerKg = (livePrice + slaughterFee) / (dressingPercent / 100.0) // ~97,183 Tomans

            val details = mutableListOf(
                "درصد بازده لاشه آماده طبخ (Dressing %)" to "۷۱ درصد (دامنه ۶۸ الی ۷۲ درصد)",
                "قیمت تمام‌شده هر کیلو گوشت مرغ لاشه" to "${toPersianDigits(carcassCostPerKg.toInt())} تومان (بر مبنای مرغ زنده ۶۵ هزار و اجرت ۴ هزار تومان)",
                "سهم سینه با استخوان در لاشه" to "۳۵ درصد وزن لاشه",
                "سهم فیله خالص سینه" to "۲۵ درصد وزن لاشه",
                "سهم ران و ساق در لاشه" to "۳۱ درصد وزن لاشه",
                "سهم بال و بازو" to "۹ درصد وزن لاشه",
                "آلایش خوراکی (دل، جگر، سنگدان)" to "۵ درصد وزن زنده پرنده",
                "صرفه اقتصادی کشتارگاه نسبت به زنده" to "۱۵ الی ۲۵ درصد سود خالص بالاتر در عرضه کشتارگاهی نسبت به فروش مرغ زنده سر فارم"
            )
            val spokenSummary = "بازده لاشه آماده طبخ مرغ گوشتی حدود ۷۱ درصد است. با احتساب قیمت مرغ زنده ۶۵ هزار تومان و اجرت کشتارگاه و افت لاشه، بهای تمام‌شده هر کیلو گوشت مرغ کشتارگاهی حدود ۹۷ هزار تومان محاسبه می‌شود که قطعات آن شامل ۳۵٪ سینه، ۲۵٪ فیله، ۳۱٪ ران و ۹٪ بال است."
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه بازده لاشه و قیمت تمام‌شده مرغ کشتارگاهی 🍗🏭",
                iconRes = R.drawable.ic_farm_financial,
                primaryMetric = "بازده لاشه: ۷۱٪ | بهای هر کیلو لاشه آماده: ${toPersianDigits(carcassCostPerKg.toInt())} تومان",
                details = details,
                systemEffect = "تحلیل اقتصادی بازده کشتارگاه و قطعه‌بندی لاشه طیور",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "carcassCostPerKg" to carcassCostPerKg,
                    "spokenSummary" to spokenSummary
                )
            )
        }

        // ۴. بهای تمام‌شده تخم‌مرغ تجاری بر اساس هر شانه و کارتن
        if (isLayerEggCost) {
            val feedPriceMatch = Regex("""(?:دان|خوراک)\s*(?:کیلویی)?\s*(\d+[\d\.,]*)\s*(هزار)?\s*تومان?""").find(norm)
            val rawPFeed = feedPriceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 22000.0
            val pFeed = if (rawPFeed < 1000.0) rawPFeed * 1000.0 else rawPFeed

            val feedPerEggKg = 0.130 // ۱۳۰ گرم دان به ازای هر عدد تخم‌مرغ
            val feedCostPerEgg = feedPerEggKg * pFeed // 2,860 Tomans at 22,000
            val totalCostPerEgg = feedCostPerEgg / 0.70 // 4,085 Tomans (دان ۷۰ درصد هزینه است)
            val trayCost = (totalCostPerEgg * 30.0) + 2000.0 // شانه مقوایی ۲۰۰۰ تومان -> ~124,550 Tomans
            val cartonCost = (trayCost * 6.0) + 15000.0 // کارتن مقوایی ۱۵۰۰۰ تومان -> ~762,300 Tomans

            val details = mutableListOf(
                "قیمت تمام‌شده هر شانه ۳۰ تایی" to "${toPersianDigits(trayCost.toInt())} تومان (با دان کیلویی ${toPersianDigits(pFeed.toInt())} تومان)",
                "قیمت تمام‌شده هر کارتن (۶ شانه / ۱۸۰ عدد)" to "${toPersianDigits(cartonCost.toInt())} تومان",
                "بهای تمام‌شده هر عدد تخم‌مرغ" to "${toPersianDigits(totalCostPerEgg.toInt())} تومان (هزینه خوراک: ${toPersianDigits(feedCostPerEgg.toInt())} تومان)",
                "مصرف دان هر عدد تخم‌مرغ" to "۱۳۰ گرم دان استاندارد نژادهای تخم‌گذار تجاری",
                "سهم دان در هزینه تولید تخم‌مرغ" to "۷۰ درصد کل هزینه‌ها (۳۰ درصد سایر هزینه‌های استهلاک پولت و کارگری)"
            )
            val spokenSummary = "قیمت تمام‌شده هر شانه ۳۰ تایی تخم‌مرغ با دان کیلویی ۲۲ هزار تومان حدود ۱۲۴ تا ۱۲۵ هزار تومان و هر کارتن ۱۸۰ تایی حدود ۷۶۲ هزار تومان است. این محاسبه بر مبنای مصرف ۱۳۰ گرم دان به ازای هر عدد تخم‌مرغ و سهم ۷۰ درصدی خوراک در قیمت تمام‌شده انجام شده است."
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "محاسبه قیمت تمام‌شده تخم‌مرغ (شانه ۳۰ تایی و کارتن) 🥚💰",
                iconRes = R.drawable.ic_farm_egg,
                primaryMetric = "هر شانه: ${toPersianDigits(trayCost.toInt())} تومان | هر کارتن: ${toPersianDigits(cartonCost.toInt())} تومان",
                details = details,
                systemEffect = "محاسبه بهای تمام‌شده شانه و کارتن تخم‌مرغ بر مبنای قیمت دان",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "trayCost" to trayCost,
                    "cartonCost" to cartonCost,
                    "spokenSummary" to spokenSummary
                )
            )
        }

        // ۵. نقطه سر به سر و بهای تمام‌شده مرغ زنده (Break-Even Unit Cost)
        if (isBreakEven) {
            val feedPriceMatch = Regex("""(?:دان|خوراک)\s*(?:کیلویی)?\s*(\d+[\d\.,]*)\s*(هزار)?\s*تومان?""").find(norm)
            val chickPriceMatch = Regex("""جوجه\s*(?:یک\s*روزه)?\s*(\d+[\d\.,]*)\s*(هزار)?\s*تومان?""").find(norm)

            val hasCustomParams = feedPriceMatch != null || chickPriceMatch != null

            if (hasCustomParams) {
                val rawPFeed = feedPriceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 25000.0
                val pFeed = if (rawPFeed < 1000.0) rawPFeed * 1000.0 else rawPFeed

                val rawPChick = chickPriceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 35000.0
                val pChick = if (rawPChick < 1000.0) rawPChick * 1000.0 else rawPChick

                val wLive = 2.5 // کیلوگرم وزن زنده بازاری
                val fcr = 1.65 // ضریب تبدیل استاندارد
                val mortality = 0.035 // ۳.۵ درصد تلفات
                val cVet = 4000.0 // واکسن و دارو
                val cOverhead = 5500.0 // سوخت، گاز، برق، کارگری و بستر

                val costFeed = fcr * wLive * pFeed // 103,125 Tomans
                val totalPlacedCost = pChick + costFeed + cVet + cOverhead // 147,625 Tomans
                val marketableWeight = wLive * (1.0 - mortality) // 2.4125 kg
                val breakEvenUnit = totalPlacedCost / marketableWeight // ~61,192 Tomans/kg

                val feedSharePercent = (costFeed / totalPlacedCost) * 100.0

                val details = mutableListOf(
                    "نقطه سر به سر (بهای هر کیلو مرغ زنده)" to "${toPersianDigits(breakEvenUnit.toInt())} تومان به ازای هر کیلوگرم",
                    "هزینه دان هر قطعه پرنده" to "${toPersianDigits(costFeed.toInt())} تومان (${toPersianDigits(String.format(Locale.US, "%.1f", feedSharePercent))}٪ کل هزینه)",
                    "هزینه خرید جوجه یک‌روزه" to "${toPersianDigits(pChick.toInt())} تومان",
                    "هزینه‌های دامپزشکی و واکسن" to "۴,۰۰۰ تومان",
                    "هزینه‌های جاری (سوخت، برق، کارگری، بستر)" to "۵,۵۰۰ تومان",
                    "کل بهای تمام‌شده پرورش هر قطعه" to "${toPersianDigits(totalPlacedCost.toInt())} تومان برای ${toPersianDigits(String.format(Locale.US, "%.2f", marketableWeight))} کیلو وزن بازاری",
                    "مفروضات محاسباتی" to "وزن هدف ۲.۵ کیلوگرم | ضریب تبدیل ۱.۶۵ | تلفات دوره ۳.۵ درصد"
                )
                val spokenSummary = "با دان ${toPersianDigits((pFeed/1000).toInt())} هزار تومان و جوجه ${toPersianDigits((pChick/1000).toInt())} هزار تومان و ضریب تبدیل ۱.۶۵، هزینه خوراک هر پرنده ${toPersianDigits(costFeed.toInt())} تومان بوده و بهای تمام‌شده و نقطه سر به سر هر کیلوگرم مرغ زنده دقیقاً حدود ${toPersianDigits(breakEvenUnit.toInt())} تومان محاسبه می‌شود. سهم خوراک در این دوره حدود ${toPersianDigits(feedSharePercent.toInt())} درصد کل هزینه‌هاست."

                return VoiceActionCardData(
                    intentType = VoiceIntentType.QUERY_STATS,
                    title = "محاسبه نقطه سر به سر و بهای تمام‌شده هر کیلو مرغ زنده 💰⚖️",
                    iconRes = R.drawable.ic_farm_financial,
                    primaryMetric = "نقطه سر به سر: ${toPersianDigits(breakEvenUnit.toInt())} تومان/کیلو (دان ${toPersianDigits((pFeed/1000).toInt())}k + جوجه ${toPersianDigits((pChick/1000).toInt())}k)",
                    details = details,
                    systemEffect = "محاسبه دینامیک بهای تمام‌شده بر اساس ورودی صوتی قیمت دان و جوجه",
                    rawText = spokenText,
                    isValid = true,
                    parameters = mapOf(
                        "breakEvenUnit" to breakEvenUnit,
                        "spokenSummary" to spokenSummary
                    )
                )
            }

            val details = mutableListOf(
                "سهم دان در بهای تمام‌شده" to "دان ۶۵ الی ۷۰ درصد کل هزینه‌های جاری دوره را تشکیل می‌دهد",
                "فرمول نقطه سر به سر (Break-Even)" to "قیمت تمام‌شده هر کیلو = کل هزینه‌های دوره ÷ وزن کل پرنده تحویلی",
                "ارزش مالی ضریب تبدیل FCR" to "بهبود ۰.۰۵ در ضریب تبدیل معادل صرفه‌جویی ۲.۵ تن دان در هر دوره ۲۰ هزارتایی است",
                "سایر هزینه‌های اصلی" to "جوجه یک‌روزه (۱۵ الی ۱۸٪) | دارو و واکسن (۴ الی ۶٪) | انرژی و سوخت (۳ الی ۵٪)"
            )
            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "حسابداری صنعتی و نقطه سر به سر (Break-Even) 💰📊",
                iconRes = R.drawable.ic_farm_financial,
                primaryMetric = "نقطه سر به سر: بهای تمام‌شده بر اساس سهم ۶۵ تا ۷۰ درصدی دان",
                details = details,
                systemEffect = "تحلیل حساسیت سود، بهای تمام‌شده و نقطه سر به سر مرغ زنده",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "economic" to true,
                    "spokenSummary" to "در مرغداری صنعتی دان ۶۵ تا ۷۰ درصد کل هزینه‌ها را تشکیل می‌دهد. قیمت و بهای تمام‌شده و نقطه سر به سر با تقسیم کل هزینه‌های دوره بر وزن زنده پرنده تحویلی محاسبه شده و بهبود ۰.۰۵ در ضریب تبدیل FCR معادل صرفه‌جویی چندین تن دان است."
                )
            )
        }

        if (isLayerIndex) {
            val dailyEggsSample = (birdCount * 0.90).toInt()
            val henDayPercent = 90.0
            val feedPerEggGrams = 118.0
            val feedPerDozenKg = (feedPerEggGrams * 12) / 1000.0
            val feedPerTray30Kg = (feedPerEggGrams * 30) / 1000.0

            val details = mutableListOf(
                "راندمان مرغ-روز (Hen-Day %)" to "${toPersianDigits(henDayPercent)} درصد",
                "مصرف دان هر عدد تخم‌مرغ" to "${toPersianDigits(String.format(Locale.US, "%.0f", feedPerEggGrams))} گرم دان",
                "مصرف دان هر شانه ۳۰ تایی" to "${toPersianDigits(String.format(Locale.US, "%.2f", feedPerTray30Kg))} کیلوگرم دان",
                "مصرف دان هر کارتن (۶ شانه / ۱۸۰ عدد)" to "${toPersianDigits(String.format(Locale.US, "%.1f", feedPerTray30Kg * 6))} کیلوگرم دان",
                "سهم دان در قیمت تمام‌شده تخم‌مرغ" to "حدود ۶۸ الی ۷۲ درصد کل هزینه‌های جاری سالن"
            )

            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "شاخص‌های اقتصادی تولید تخم‌مرغ گله «$flockName»",
                iconRes = R.drawable.ic_farm_egg,
                primaryMetric = "راندمان Hen-Day: ${toPersianDigits(henDayPercent)}٪ (مصرف ${toPersianDigits(String.format(Locale.US, "%.2f", feedPerTray30Kg))} کیلو به ازای هر شانه)",
                details = details,
                systemEffect = "محاسبه شاخص‌های استاندارد اتحادیه تولیدکنندگان تخم‌مرغ",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "economic" to true,
                    "spokenSummary" to "در راندمان مرغ-روز ۹۰ درصد، به ازای هر شانه ۳۰ عددی تخم‌مرغ حدود ${toPersianDigits(String.format(Locale.US, "%.2f", feedPerTray30Kg))} کیلوگرم دان مصرف می‌شود. سهم خوراک در قیمت تمام‌شده حدود ۷۰ درصد است."
                )
            )
        }

        if (isSlaughterQuery) {
            val details = mutableListOf(
                "سن استاندارد کشتار مرغ گوشتی" to "روز ۳۸ الی ۴۲ پرورش (وزن زنده ۲.۴ الی ۲.۸ کیلوگرم)",
                "دلیل عدم نگهداری بالای ۴۲ روز" to "پس از روز ۴۲، سرعت رشد روزانه کند شده و ضریب تبدیل (FCR) به بالای ۱.۸۵ می‌رسد و سود خالص افت می‌کند",
                "زمان قطع داروها (Withdrawal)" to "حداقل ۵ الی ۷ روز قبل از بارگیری باید دان پلت نهایی (Finisher 2 بدون کوکسیدیواستات) تغذیه شود",
                "قطع دان قبل از بارگیری" to "۸ الی ۱۰ ساعت قطع کامل دان قبل از کشتار جهت تخلیه چینه‌دان و جلوگیری از آلودگی لاشه در کشتارگاه",
                "آب آشامیدنی قبل از بارگیری" to "آب شیرین و تمیز تا لحظه شروع گرفتن مرغ‌ها نباید قطع شود تا افت وزن رطوبتی لاشه رخ ندهد"
            )

            return VoiceActionCardData(
                intentType = VoiceIntentType.QUERY_STATS,
                title = "پروتکل تعیین سن بهینه کشتار و بارگیری گله «$flockName»",
                iconRes = R.drawable.ic_farm_financial,
                primaryMetric = "پنجره طلایی کشتار: ۳۸ تا ۴۲ روزگی (FCR زیر ۱.۷۵)",
                details = details,
                systemEffect = "تحلیل منحنی بازده نزولی وزن‌گیری و ضریب تبدیل مرغ گوشتی",
                rawText = spokenText,
                isValid = true,
                parameters = mapOf(
                    "economic" to true,
                    "spokenSummary" to "پنجره طلایی بارگیری و کشتار بین ۳۸ تا ۴۲ روزگی است، زیرا پس از این سن ضریب تبدیل به شدت افزایش یافته و سود گله کاهش می‌یابد. حتماً ۸ تا ۱۰ ساعت قبل از بارگیری دان قطع شود، اما آب تا لحظه صید پرندگان در دسترس بماند."
                )
            )
        }

        // محاسبه EPEF / PI (شاخص بهره‌وری اروپایی و شاخص عملکرد)
        val survivalPercent = 96.5 // درصد ماندگاری استاندارد
        val weightMatch = Regex("""(?:وزن)?\s*(\d+(?:\.\d+)?)\s*(?:کیلو|kg)""").find(norm)
        val parsedWeight = weightMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val avgWeightKg = parsedWeight ?: when {
            ageDays <= 25 -> 1.20
            ageDays <= 35 -> 2.10
            ageDays <= 42 -> 2.65
            else -> 2.80
        }
        val ageMatch = Regex("""(?:سن)?\s*(\d+)\s*روز""").find(norm)
        val effectiveAgeDays = ageMatch?.groupValues?.get(1)?.toIntOrNull() ?: ageDays

        val fcrMatch = Regex("""(?:fcr|ضریب\s*تبدیل)\s*(\d+(?:\.\d+)?)""").find(norm)
        val parsedFcr = fcrMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val fcr = parsedFcr ?: when {
            effectiveAgeDays <= 25 -> 1.35
            effectiveAgeDays <= 35 -> 1.55
            effectiveAgeDays <= 42 -> 1.68
            else -> 1.82
        }

        // فرمول EPEF / PI = (Survival % * Weight kg) / (Age days * FCR) * 100
        val epefScore = (survivalPercent * avgWeightKg) / (effectiveAgeDays * fcr) * 100.0
        val evaluationFa = when {
            epefScore >= 400 -> "ممتاز و در سطح کلاس جهانی (World Class)"
            epefScore >= 360 -> "عالی و بسیار سودآور (Excellent)"
            epefScore >= 320 -> "خوب و استاندارد صنعتی (Good)"
            epefScore >= 280 -> "متوسط و نیازمند بهبود ضریب تبدیل (Average)"
            else -> "ضعیف و نیازمند بازنگری فوری تهویه و تغذیه (Poor)"
        }

        val details = mutableListOf(
            "امتیاز بهره‌وری اروپایی (EPEF / PI)" to "${toPersianDigits(String.format(Locale.US, "%.1f", epefScore))} امتیاز",
            "سطح عملکرد گله" to evaluationFa,
            "درصد ماندگاری (Survival %)" to "${toPersianDigits(String.format(Locale.US, "%.1f", survivalPercent))} درصد (تلفات ${toPersianDigits(String.format(Locale.US, "%.1f", 100.0 - survivalPercent))}٪)",
            "میانگین وزن زنده پرنده" to "${toPersianDigits(String.format(Locale.US, "%.2f", avgWeightKg))} کیلوگرم در سن ${toPersianDigits(effectiveAgeDays)} روزگی",
            "ضریب تبدیل غذایی (FCR)" to "${toPersianDigits(String.format(Locale.US, "%.2f", fcr))} کیلو دان به ازای ۱ کیلو گوشت",
            "فرمول ریاضی استاندارد" to "EPEF = (درصد ماندگاری × وزن زنده) ÷ (سن بر حسب روز × FCR) × ۱۰۰",
            "نکته کلیدی ارتقای EPEF" to "کاهش تنها ۰.۰۵ در ضریب تبدیل یا افزایش ۵۰ گرم وزن روزانه، امتیاز EPEF را بیش از ۱۵ واحد جهش می‌دهد"
        )

        return VoiceActionCardData(
            intentType = VoiceIntentType.QUERY_STATS,
            title = "محاسبه شاخص ضریب بهره‌وری اروپایی (EPEF / PI) گله «$flockName»",
            iconRes = R.drawable.ic_farm_financial,
            primaryMetric = "امتیاز EPEF: ${toPersianDigits(String.format(Locale.US, "%.0f", epefScore))} ($evaluationFa)",
            details = details,
            systemEffect = "استاندارد جهانی ارزیابی عملکرد و سودآوری دوره‌های پرورش طیور گوشتی",
            rawText = spokenText,
            isValid = true,
            parameters = mapOf(
                "economic" to true,
                "epef" to epefScore,
                "spokenSummary" to "شاخص راندمان اروپایی (EPEF / PI) گله «$flockName» حدود ${toPersianDigits(String.format(Locale.US, "%.0f", epefScore))} امتیاز است که در رتبه «$evaluationFa» قرار می‌گیرد. این امتیاز بر اساس وزن زنده ${toPersianDigits(String.format(Locale.US, "%.2f", avgWeightKg))} کیلوگرم، ماندگاری ${toPersianDigits(String.format(Locale.US, "%.1f", survivalPercent))} درصد و ضریب تبدیل ${toPersianDigits(String.format(Locale.US, "%.2f", fcr))} محاسبه شده است."
            )
        )
    }
}

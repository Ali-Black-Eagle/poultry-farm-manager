package com.example.poultryapp.engine

import com.example.poultryapp.data.local.BiosecurityCheckItem

object SeasonalBiosecurityEngine {

    data class SeasonalThemeInfo(
        val season: FarmSeason,
        val titleFa: String,
        val subtitleFa: String,
        val primaryRiskFa: String,
        val iconResName: String,
        val colorHex: Long
    )

    fun getCurrentSeasonTheme(): SeasonalThemeInfo {
        val currentSeason = JalaliDateHelper.getCurrentSeason()
        return when (currentSeason) {
            FarmSeason.AUTUMN -> SeasonalThemeInfo(
                season = FarmSeason.AUTUMN,
                titleFa = "بیوامنیتی فصل پاییز (مهر، آبان، آذر)",
                subtitleFa = "فصل مهاجرت پرندگان آبزی و ورود سویه‌های تنفسی",
                primaryRiskFa = "خطر بحرانی: آنفلوآنزای فوق حاد پرندگان و برونشیت عفونی",
                iconResName = "ic_farm_health",
                colorHex = 0xFFD97706
            )
            FarmSeason.WINTER -> SeasonalThemeInfo(
                season = FarmSeason.WINTER,
                titleFa = "بیوامنیتی فصل زمستان (دی، بهمن، اسفند)",
                subtitleFa = "بالانس گرمایش سالن، تهویه حداقلی و رطوبت بستر",
                primaryRiskFa = "خطر بحرانی: خفگی با گاز آمونیاک، کوکسیدیوز و افت دما",
                iconResName = "ic_farm_health",
                colorHex = 0xFF2563EB
            )
            FarmSeason.SPRING -> SeasonalThemeInfo(
                season = FarmSeason.SPRING,
                titleFa = "بیوامنیتی فصل بهار (فروردین، اردیبهشت، خرداد)",
                subtitleFa = "خروج انگل‌های خارجی، مایت قرمز و تغییر ساعات نوری",
                primaryRiskFa = "خطر بحرانی: مایت قرمز (کنه مرغی) و آلودگی خطوط آب",
                iconResName = "ic_farm_health",
                colorHex = 0xFF059669
            )
            FarmSeason.SUMMER -> SeasonalThemeInfo(
                season = FarmSeason.SUMMER,
                titleFa = "بیوامنیتی فصل تابستان (تیر، مرداد، شهریور)",
                subtitleFa = "مدیریت استرس گرمایی، سرویس پد خنک‌کننده و الکترولیت‌ها",
                primaryRiskFa = "خطر بحرانی: گرمازدگی شدید، قطعی برق/فن و کانیبالیسم",
                iconResName = "ic_farm_health",
                colorHex = 0xFFDC2626
            )
        }
    }
}

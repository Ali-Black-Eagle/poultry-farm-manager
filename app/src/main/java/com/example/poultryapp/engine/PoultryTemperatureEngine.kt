package com.example.poultryapp.engine

import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.BreedCatalog
import com.example.poultryapp.data.model.ProductionType

data class IdealTemperatureSpec(
    val minComfortCelsius: Double,
    val maxComfortCelsius: Double,
    val minTolerableCelsius: Double,
    val maxTolerableCelsius: Double,
    val stageFa: String,
    val adviceFa: String
) {
    // سازگاری با کدهای موجود
    val minCelsius: Double get() = minComfortCelsius
    val maxCelsius: Double get() = maxComfortCelsius
    val idealComfortCelsius: Double get() = (minComfortCelsius + maxComfortCelsius) / 2.0

    val formattedRange: String
        get() = "${minComfortCelsius.toInt()} تا ${maxComfortCelsius.toInt()} درجه (مجاز: ${minTolerableCelsius.toInt()} تا ${maxTolerableCelsius.toInt()}°C)"

    val shortRangeFa: String
        get() = "${minComfortCelsius.toInt()}-${maxComfortCelsius.toInt()}°C"
}

data class ClimateActionItem(
    val categoryFa: String,
    val titleFa: String,
    val descriptionFa: String
)

data class ThermalStressGuide(
    val isHeatStress: Boolean,
    val titleFa: String,
    val severityFa: String,
    val summaryFa: String,
    val actionItems: List<ClimateActionItem>
)

/**
 * موتور تخصصی و استانداردهای علمی دمای آسایش و پرورش طیور بر اساس گونه، نژاد و سن
 * منابع علمی معتبر:
 * - راهنمای مدیریت مرغ تخم‌گذار لوهمن و های‌لاین (Lohmann & Hy-Line Management Guide)
 * - راهنمای پرورش مرغ گوشتی راس ۳۰۸ و کاب ۵۰۰ (Ross 308 & Cobb 500 Broiler Guide)
 * - راهنمای تغذیه و پرورش طیور و بلدرچین شورای ملی تحقیقات آمریکا (NRC)
 * - استانداردهای بیوکلیماتیک سازمان خواربار و کشاورزی ملل متحد (FAO)
 *
 * نکته کلیدی بیولوژیک:
 * محدوده آسایش حرارتی (Thermoneutral Zone - TNZ) دمای ایده‌آل است؛ اما محدوده قابل‌تحمل و بی‌خطر پرنده بسیار گسترده‌تر است.
 * به عنوان مثال برای مرغ بالغ و نیمچه (از ۲ ماهگی به بعد)، دمای ۱۶ تا ۲۸ درجه سانتی‌گراد کاملاً ایمن و قابل‌تحمل است و پرنده هیچ استرسی متحمل نمی‌شود.
 */
object PoultryTemperatureEngine {

    fun getIdealTemperature(
        species: BirdSpecies,
        breedName: String = "",
        ageDays: Int
    ): IdealTemperatureSpec {
        val days = ageDays.coerceAtLeast(1)
        val prodType = BreedCatalog.getProductionType(species, breedName)

        return when (species) {
            BirdSpecies.CHICKEN -> {
                if (prodType == ProductionType.MEAT) {
                    // جوجه گوشتی پروار (Ross, Cobb, Arbor Acres)
                    when {
                        days <= 3 -> IdealTemperatureSpec(
                            minComfortCelsius = 32.0,
                            maxComfortCelsius = 34.0,
                            minTolerableCelsius = 31.0,
                            maxTolerableCelsius = 35.0,
                            stageFa = "استارتر اولیه (۱ تا ۳ روزگی)",
                            adviceFa = "دمای ۳۲ تا ۳۴ درجه در سطح پشت جوجه‌ها با مادر مصنوعی. توزیع یکنواخت جوجه‌ها نشان‌دهنده دمای عالی است."
                        )
                        days <= 7 -> IdealTemperatureSpec(
                            minComfortCelsius = 30.0,
                            maxComfortCelsius = 32.0,
                            minTolerableCelsius = 28.5,
                            maxTolerableCelsius = 33.5,
                            stageFa = "پایان هفته اول پروار",
                            adviceFa = "کاهش تدریجی به ۳۰ تا ۳۱ درجه و حفظ حداقل تهویه بدون ایجاد کوران مستقیم هوا."
                        )
                        days <= 14 -> IdealTemperatureSpec(
                            minComfortCelsius = 27.0,
                            maxComfortCelsius = 29.5,
                            minTolerableCelsius = 25.0,
                            maxTolerableCelsius = 31.0,
                            stageFa = "هفته دوم پروار",
                            adviceFa = "دمای مطلوب ۲۷ تا ۲۹ درجه. جلوگیری از نوسان دمایی بیش از ۳ درجه در شبانه‌روز."
                        )
                        days <= 21 -> IdealTemperatureSpec(
                            minComfortCelsius = 24.0,
                            maxComfortCelsius = 26.5,
                            minTolerableCelsius = 22.0,
                            maxTolerableCelsius = 28.5,
                            stageFa = "هفته سوم پروار",
                            adviceFa = "دمای ۲۴ تا ۲۶ درجه سانتی‌گراد همزمان با رشد پرهای پروازی و افزایش راندمان رشد."
                        )
                        days <= 28 -> IdealTemperatureSpec(
                            minComfortCelsius = 21.0,
                            maxComfortCelsius = 24.0,
                            minTolerableCelsius = 19.0,
                            maxTolerableCelsius = 27.5,
                            stageFa = "هفته چهارم پروار",
                            adviceFa = "دمای ۲۱ تا ۲۴ درجه. تا ۲۷ درجه کاملاً قابل‌تحمل است؛ افزایش جریان هوا برای خنک‌سازی توصیه می‌شود."
                        )
                        days <= 35 -> IdealTemperatureSpec(
                            minComfortCelsius = 20.0,
                            maxComfortCelsius = 22.5,
                            minTolerableCelsius = 18.0,
                            maxTolerableCelsius = 27.0,
                            stageFa = "هفته پنجم پروار",
                            adviceFa = "دمای مطلوب ۲۰ تا ۲۲ درجه (محدوده مجاز ۱۸ تا ۲۷ درجه)."
                        )
                        else -> IdealTemperatureSpec(
                            minComfortCelsius = 19.0,
                            maxComfortCelsius = 21.5,
                            minTolerableCelsius = 17.0,
                            maxTolerableCelsius = 26.5,
                            stageFa = "دوره پایانی پروار گوشتی",
                            adviceFa = "دمای ۱۹ تا ۲۲ درجه. به دلیل وزن بالا، دمای بالاتر از ۲۷ درجه به تهویه بادی سریع (Wind-Chill) نیاز دارد."
                        )
                    }
                } else {
                    // مرغ بومی، تخم‌گذار، دورگ و دو منظوره (Lohmann, Hy-Line, Black Star, Golpayegani, Native)
                    when {
                        days <= 3 -> IdealTemperatureSpec(
                            minComfortCelsius = 33.0,
                            maxComfortCelsius = 35.0,
                            minTolerableCelsius = 31.0,
                            maxTolerableCelsius = 36.0,
                            stageFa = "جوجه‌ریزی ۱ تا ۳ روزگی",
                            adviceFa = "دمای ۳۳ تا ۳۵ درجه زیر مادر مصنوعی و ۳۰ درجه در فضای اتاق پرورش."
                        )
                        days <= 7 -> IdealTemperatureSpec(
                            minComfortCelsius = 31.0,
                            maxComfortCelsius = 33.0,
                            minTolerableCelsius = 29.0,
                            maxTolerableCelsius = 34.0,
                            stageFa = "هفته اول پرورش",
                            adviceFa = "دمای مطلوب ۳۱ تا ۳۳ درجه سانتی‌گراد."
                        )
                        days <= 14 -> IdealTemperatureSpec(
                            minComfortCelsius = 28.0,
                            maxComfortCelsius = 31.0,
                            minTolerableCelsius = 26.0,
                            maxTolerableCelsius = 32.5,
                            stageFa = "هفته دوم پرورش",
                            adviceFa = "دمای مطلوب ۲۸ تا ۳۱ درجه سانتی‌گراد."
                        )
                        days <= 21 -> IdealTemperatureSpec(
                            minComfortCelsius = 25.0,
                            maxComfortCelsius = 28.0,
                            minTolerableCelsius = 23.0,
                            maxTolerableCelsius = 29.5,
                            stageFa = "هفته سوم پرورش",
                            adviceFa = "دمای مطلوب ۲۵ تا ۲۸ درجه سانتی‌گراد."
                        )
                        days <= 28 -> IdealTemperatureSpec(
                            minComfortCelsius = 22.0,
                            maxComfortCelsius = 25.0,
                            minTolerableCelsius = 19.0,
                            maxTolerableCelsius = 28.5,
                            stageFa = "هفته چهارم (۱ ماهگی)",
                            adviceFa = "دمای مطلوب ۲۲ تا ۲۵ درجه (تا ۲۸.۵ درجه بدون استرس و ایمن)."
                        )
                        days <= 60 -> IdealTemperatureSpec(
                            minComfortCelsius = 20.0,
                            maxComfortCelsius = 24.0,
                            minTolerableCelsius = 17.0,
                            maxTolerableCelsius = 28.5,
                            stageFa = "رشد پولت (۲ ماهگی)",
                            adviceFa = "دمای مطلوب ۲۰ تا ۲۴ درجه (محدوده مجاز ۱۷ تا ۲۸.۵ درجه سانتی‌گراد)."
                        )
                        days <= 120 -> IdealTemperatureSpec(
                            minComfortCelsius = 19.0,
                            maxComfortCelsius = 24.0,
                            minTolerableCelsius = 16.0,
                            maxTolerableCelsius = 28.0,
                            stageFa = "نیمچه در آستانه بلوغ (۳ تا ۴ ماهگی)",
                            adviceFa = "دمای مطلوب ۱۹ تا ۲۴ درجه. تا ۲۸ درجه سانتی‌گراد پرنده کاملاً راحت و بدون استرس است."
                        )
                        else -> IdealTemperatureSpec(
                            minComfortCelsius = 18.0,
                            maxComfortCelsius = 24.0,
                            minTolerableCelsius = 15.0,
                            maxTolerableCelsius = 28.0,
                            stageFa = "دوره بلوغ و تخم‌گذاری",
                            adviceFa = "محدوده آسایش ۱۸ تا ۲۴ درجه. تا ۲۸ درجه کاملاً ایمن و نرمال است؛ افت تولید تنها در دمای بالای ۲۹° یا زیر ۱۴° رخ می‌دهد."
                        )
                    }
                }
            }

            BirdSpecies.TURKEY -> {
                // بوقلمون
                when {
                    days <= 7 -> IdealTemperatureSpec(
                        minComfortCelsius = 35.0,
                        maxComfortCelsius = 37.0,
                        minTolerableCelsius = 33.0,
                        maxTolerableCelsius = 38.0,
                        stageFa = "جوجه بوقلمون (هفته اول)",
                        adviceFa = "دمای ۳۵ تا ۳۷ درجه زیر منبع حرارتی؛ جوجه بوقلمون در هفته اول بسیار نسبت به سرما حساس است."
                    )
                    days <= 14 -> IdealTemperatureSpec(
                        minComfortCelsius = 32.0,
                        maxComfortCelsius = 34.0,
                        minTolerableCelsius = 30.0,
                        maxTolerableCelsius = 35.5,
                        stageFa = "هفته دوم بوقلمون",
                        adviceFa = "دمای ۳۲ تا ۳۴ درجه سانتی‌گراد."
                    )
                    days <= 21 -> IdealTemperatureSpec(
                        minComfortCelsius = 29.0,
                        maxComfortCelsius = 31.0,
                        minTolerableCelsius = 27.0,
                        maxTolerableCelsius = 32.5,
                        stageFa = "هفته سوم بوقلمون",
                        adviceFa = "دمای ۲۹ تا ۳۱ درجه سانتی‌گراد."
                    )
                    days <= 28 -> IdealTemperatureSpec(
                        minComfortCelsius = 25.0,
                        maxComfortCelsius = 28.0,
                        minTolerableCelsius = 23.0,
                        maxTolerableCelsius = 29.5,
                        stageFa = "هفته چهارم بوقلمون",
                        adviceFa = "دمای ۲۵ تا ۲۸ درجه سانتی‌گراد."
                    )
                    days <= 42 -> IdealTemperatureSpec(
                        minComfortCelsius = 21.0,
                        maxComfortCelsius = 25.0,
                        minTolerableCelsius = 18.0,
                        maxTolerableCelsius = 28.0,
                        stageFa = "رشد پیشرفته (هفته ۵ و ۶)",
                        adviceFa = "دمای ۲۱ تا ۲۵ درجه سانتی‌گراد (تا ۲۸ درجه ایمن)."
                    )
                    else -> IdealTemperatureSpec(
                        minComfortCelsius = 16.0,
                        maxComfortCelsius = 22.0,
                        minTolerableCelsius = 13.0,
                        maxTolerableCelsius = 27.0,
                        stageFa = "پرواربندی و بلوغ بوقلمون",
                        adviceFa = "دمای ۱۶ تا ۲۲ درجه مطلوب است. بوقلمون بالغ سرمای ملایم را به خوبی تحمل می‌کند اما در گرمای بالای ۲۸ درجه نیاز به هوادهی دارد."
                    )
                }
            }

            BirdSpecies.QUAIL -> {
                // بلدرچین (مقاومت عالی به گرما)
                when {
                    days <= 3 -> IdealTemperatureSpec(
                        minComfortCelsius = 35.0,
                        maxComfortCelsius = 37.0,
                        minTolerableCelsius = 33.5,
                        maxTolerableCelsius = 38.0,
                        stageFa = "جوجه بلدرچین ۱ تا ۳ روزه",
                        adviceFa = "دمای ۳۵ تا ۳۷ درجه زیر لامپ مادر مصنوعی با هوای کاملاً ساکن."
                    )
                    days <= 7 -> IdealTemperatureSpec(
                        minComfortCelsius = 33.0,
                        maxComfortCelsius = 35.0,
                        minTolerableCelsius = 31.0,
                        maxTolerableCelsius = 36.5,
                        stageFa = "هفته اول بلدرچین",
                        adviceFa = "دمای ۳۳ تا ۳۵ درجه سانتی‌گراد."
                    )
                    days <= 14 -> IdealTemperatureSpec(
                        minComfortCelsius = 29.0,
                        maxComfortCelsius = 31.5,
                        minTolerableCelsius = 27.0,
                        maxTolerableCelsius = 33.5,
                        stageFa = "هفته دوم بلدرچین",
                        adviceFa = "دمای ۲۹ تا ۳۱.۵ درجه سانتی‌گراد."
                    )
                    days <= 21 -> IdealTemperatureSpec(
                        minComfortCelsius = 25.0,
                        maxComfortCelsius = 27.5,
                        minTolerableCelsius = 22.0,
                        maxTolerableCelsius = 29.5,
                        stageFa = "هفته سوم بلدرچین",
                        adviceFa = "دمای ۲۵ تا ۲۷.۵ درجه سانتی‌گراد."
                    )
                    days <= 35 -> IdealTemperatureSpec(
                        minComfortCelsius = 22.0,
                        maxComfortCelsius = 25.0,
                        minTolerableCelsius = 19.0,
                        maxTolerableCelsius = 29.0,
                        stageFa = "رشد و آماده‌سازی تخم‌گذاری",
                        adviceFa = "دمای ۲۲ تا ۲۵ درجه سانتی‌گراد (تا ۲۹ درجه ایمن و بدون استرس)."
                    )
                    else -> IdealTemperatureSpec(
                        minComfortCelsius = 20.0,
                        maxComfortCelsius = 25.0,
                        minTolerableCelsius = 17.0,
                        maxTolerableCelsius = 29.5,
                        stageFa = "دوران تخم‌گذاری بلدرچین",
                        adviceFa = "دمای ۲۰ تا ۲۵ درجه برای بالاترین راندمان تخم‌گذاری. بلدرچین گرما را تا ۲۹.۵ درجه به خوبی تحمل می‌کند."
                    )
                }
            }

            BirdSpecies.DUCK -> {
                // اردک (بسیار مقاوم به سرما و رطوبت)
                when {
                    days <= 3 -> IdealTemperatureSpec(
                        minComfortCelsius = 30.0,
                        maxComfortCelsius = 32.0,
                        minTolerableCelsius = 28.0,
                        maxTolerableCelsius = 34.0,
                        stageFa = "جوجه اردک ۱ تا ۳ روزه",
                        adviceFa = "دمای ۳۰ تا ۳۲ درجه زیر مادر مصنوعی با بستر خشک."
                    )
                    days <= 7 -> IdealTemperatureSpec(
                        minComfortCelsius = 27.0,
                        maxComfortCelsius = 29.5,
                        minTolerableCelsius = 24.0,
                        maxTolerableCelsius = 32.0,
                        stageFa = "هفته اول اردک",
                        adviceFa = "دمای ۲۷ تا ۲۹.۵ درجه سانتی‌گراد."
                    )
                    days <= 14 -> IdealTemperatureSpec(
                        minComfortCelsius = 23.0,
                        maxComfortCelsius = 26.0,
                        minTolerableCelsius = 20.0,
                        maxTolerableCelsius = 28.5,
                        stageFa = "هفته دوم اردک",
                        adviceFa = "دمای ۲۳ تا ۲۶ درجه سانتی‌گراد."
                    )
                    days <= 21 -> IdealTemperatureSpec(
                        minComfortCelsius = 19.0,
                        maxComfortCelsius = 23.0,
                        minTolerableCelsius = 16.0,
                        maxTolerableCelsius = 28.0,
                        stageFa = "هفته سوم اردک",
                        adviceFa = "دمای ۱۹ تا ۲۳ درجه سانتی‌گراد."
                    )
                    else -> IdealTemperatureSpec(
                        minComfortCelsius = 15.0,
                        maxComfortCelsius = 22.0,
                        minTolerableCelsius = 10.0,
                        maxTolerableCelsius = 28.5,
                        stageFa = "بلوغ و تخم‌گذاری اردک",
                        adviceFa = "دمای ۱۵ تا ۲۲ درجه سانتی‌گراد. اردک در برابر سرما بسیار مقاوم است و تا ۲۸.۵ درجه را به راحتی تاب می‌آورد."
                    )
                }
            }

            BirdSpecies.GOOSE -> {
                // غاز (مقاوم‌ترین پرنده اهلی به سرما و شرایط آزاد)
                when {
                    days <= 3 -> IdealTemperatureSpec(
                        minComfortCelsius = 30.0,
                        maxComfortCelsius = 32.0,
                        minTolerableCelsius = 28.0,
                        maxTolerableCelsius = 33.5,
                        stageFa = "جوجه غاز ۱ تا ۳ روزه",
                        adviceFa = "دمای ۳۰ تا ۳۲ درجه سانتی‌گراد."
                    )
                    days <= 7 -> IdealTemperatureSpec(
                        minComfortCelsius = 26.0,
                        maxComfortCelsius = 29.0,
                        minTolerableCelsius = 23.0,
                        maxTolerableCelsius = 31.0,
                        stageFa = "هفته اول غاز",
                        adviceFa = "دمای ۲۶ تا ۲۹ درجه سانتی‌گراد."
                    )
                    days <= 14 -> IdealTemperatureSpec(
                        minComfortCelsius = 22.0,
                        maxComfortCelsius = 25.0,
                        minTolerableCelsius = 18.0,
                        maxTolerableCelsius = 28.0,
                        stageFa = "هفته دوم غاز",
                        adviceFa = "دمای ۲۲ تا ۲۵ درجه سانتی‌گراد."
                    )
                    days <= 21 -> IdealTemperatureSpec(
                        minComfortCelsius = 18.0,
                        maxComfortCelsius = 22.0,
                        minTolerableCelsius = 14.0,
                        maxTolerableCelsius = 27.5,
                        stageFa = "هفته سوم غاز",
                        adviceFa = "دمای ۱۸ تا ۲۲ درجه سانتی‌گراد."
                    )
                    else -> IdealTemperatureSpec(
                        minComfortCelsius = 12.0,
                        maxComfortCelsius = 20.0,
                        minTolerableCelsius = 6.0,
                        maxTolerableCelsius = 28.0,
                        stageFa = "بلوغ و چرای غاز در مرتع",
                        adviceFa = "دمای ۱۲ تا ۲۰ درجه سانتی‌گراد. غاز بالغ فوق‌العاده مقاوم به سرما است و تا دمای ۲۸ درجه بدون مشکل فعالیت می‌کند."
                    )
                }
            }

            BirdSpecies.GUINEA_FOWL -> {
                // مرغ شاخدار (سازگار با آب‌وهوای گرمسیری و نیمه‌خشک)
                when {
                    days <= 7 -> IdealTemperatureSpec(
                        minComfortCelsius = 35.0,
                        maxComfortCelsius = 37.0,
                        minTolerableCelsius = 33.0,
                        maxTolerableCelsius = 38.0,
                        stageFa = "جوجه مرغ شاخدار (هفته اول)",
                        adviceFa = "دمای ۳۵ تا ۳۷ درجه سانتی‌گراد."
                    )
                    days <= 14 -> IdealTemperatureSpec(
                        minComfortCelsius = 31.0,
                        maxComfortCelsius = 34.0,
                        minTolerableCelsius = 29.0,
                        maxTolerableCelsius = 35.5,
                        stageFa = "هفته دوم",
                        adviceFa = "دمای ۳۱ تا ۳۴ درجه سانتی‌گراد."
                    )
                    days <= 21 -> IdealTemperatureSpec(
                        minComfortCelsius = 27.0,
                        maxComfortCelsius = 30.0,
                        minTolerableCelsius = 25.0,
                        maxTolerableCelsius = 32.0,
                        stageFa = "هفته سوم",
                        adviceFa = "دمای ۲۷ تا ۳۰ درجه سانتی‌گراد."
                    )
                    days <= 35 -> IdealTemperatureSpec(
                        minComfortCelsius = 23.0,
                        maxComfortCelsius = 26.0,
                        minTolerableCelsius = 20.0,
                        maxTolerableCelsius = 29.0,
                        stageFa = "رشد ۱ ماهگی",
                        adviceFa = "دمای ۲۳ تا ۲۶ درجه سانتی‌گراد."
                    )
                    else -> IdealTemperatureSpec(
                        minComfortCelsius = 18.0,
                        maxComfortCelsius = 24.0,
                        minTolerableCelsius = 15.0,
                        maxTolerableCelsius = 29.5,
                        stageFa = "بلوغ و تخم‌گذاری",
                        adviceFa = "دمای ۱۸ تا ۲۴ درجه سانتی‌گراد. مرغ شاخدار اصالتاً آفریقایی بوده و مقاومت بسیار بالایی در برابر گرما دارد."
                    )
                }
            }
        }
    }

    /**
     * راهکارهای عملیاتی و دامپزشکی برای کنترل و جبران استرس دمایی (گرما یا سرما)
     * ارزیابی بر اساس محدوده مجاز فیزیولوژیک (minTolerable و maxTolerable) انجام می‌شود
     * تا از آلارم‌های کاذب و بی‌مورد جلوگیری شود.
     */
    fun getThermalStressGuide(
        species: BirdSpecies,
        currentTemp: Double,
        spec: IdealTemperatureSpec
    ): ThermalStressGuide? {
        // اگر دما در محدوده مجاز و قابل‌تحمل پرنده باشد، هیچ استرسی وجود ندارد
        if (currentTemp >= spec.minTolerableCelsius && currentTemp <= spec.maxTolerableCelsius) {
            return null
        }

        val isHeat = currentTemp > spec.maxTolerableCelsius
        val diff = if (isHeat) {
            currentTemp - spec.maxTolerableCelsius
        } else {
            spec.minTolerableCelsius - currentTemp
        }

        val severity = when {
            diff <= 2.5 -> "خفیف"
            diff <= 5.0 -> "متوسط"
            else -> "شدید و بحرانی"
        }

        val diffFormatted = String.format(java.util.Locale.US, "%.1f", diff)

        return if (isHeat) {
            ThermalStressGuide(
                isHeatStress = true,
                titleFa = "راهنمای فوری مقابله با استرس گرمایی ($severity)",
                severityFa = severity,
                summaryFa = "دمای سالن $diffFormatted درجه بالاتر از آستانه مجاز تحمل پرنده در این سن است. در این دما افزایش نوشیدن آب، له‌له زدن و افت اشتها دیده می‌شود.",
                actionItems = listOf(
                    ClimateActionItem(
                        categoryFa = "آب و الکترولیت",
                        titleFa = "تأمین آب خنک و الکترولیت ضد استرس",
                        descriptionFa = "آب تمیز و خنک (۱۵ تا ۱۸ درجه سانتی‌گراد) به صورت نامحدود در اختیار گله بگذارید. افزودن ویتامین C و الکترولیت در آب از آلکالوز تنفسی و افت ایمنی پرنده ممانعت می‌کند."
                    ),
                    ClimateActionItem(
                        categoryFa = "تهویه و جریان هوا",
                        titleFa = "افزایش سرعت جریان هوا و سیستم خنک‌کننده",
                        descriptionFa = "هواکش‌ها را در دور بالا فعال کنید تا سرعت باد روی پشت پرنده‌ها حس خنکی (Wind Chill) ایجاد کند. در صورت نیاز پدهای سلولزی یا مه‌پاش را در ساعات گرم روز فعال کنید."
                    ),
                    ClimateActionItem(
                        categoryFa = "مدیریت خوراک",
                        titleFa = "قطع یا کاهش دان در ساعات اوج گرما",
                        descriptionFa = "در ساعات ظهر و اوج تابش (ساعت ۱۱ تا ۱۶) از دان دادن سنگین خودداری کنید؛ هضم دان حرارت متابولیک بدن را افزایش می‌دهد. دان اصلی را صبح زود یا غروب توزیع نمایید."
                    ),
                    ClimateActionItem(
                        categoryFa = "بستر و محیط سالن",
                        titleFa = "کاهش تراکم و هوادهی بستر",
                        descriptionFa = "از ازدحام پرندگان در گوشه‌ها جلوگیری کرده و رفت‌وآمد و استرس سالن را به حداقل برسانید. در ساعات گرم شدت نور را کاهش دهید تا پرندگان آرام باشند."
                    ),
                    ClimateActionItem(
                        categoryFa = "سایه‌بان و محیط",
                        titleFa = "فراهم کردن سایه و جلوگیری از تابش مستقیم آفتاب",
                        descriptionFa = "ایجاد سایه‌بان کافی در بهاربند یا گردشگاه پرندگان، نصب توری شید (Shade Net) روی پنجره‌های آفتاب‌گیر، و جلوگیری از تابش مستقیم خورشید بر پرندگان، تانکرهای آب و خطوط آبخوری."
                    )
                )
            )
        } else {
            ThermalStressGuide(
                isHeatStress = false,
                titleFa = "راهنمای فوری جبران سرما و حفظ دمای گله ($severity)",
                severityFa = severity,
                summaryFa = "دمای سالن $diffFormatted درجه سردتر از حداقل دمای مجاز این سن است. سرمای زیاد باعث کپه شدن روی هم، اتلاف انرژی و تضعیف سیستم تنفسی می‌شود.",
                actionItems = listOf(
                    ClimateActionItem(
                        categoryFa = "منابع گرمایشی",
                        titleFa = "افزایش دمای ترموستات یا هیترها",
                        descriptionFa = "دمای هیتر یا مادر مصنوعی را متناسب با سن تنظیم کنید. سنسور سنجش دما باید دقیقاً در ارتفاع قد پرندگان قرار داشته باشد."
                    ),
                    ClimateActionItem(
                        categoryFa = "درزگیری و بادشکن",
                        titleFa = "جلوگیری از وزش کوران مستقیم باد سرد",
                        descriptionFa = "ورودی‌های هوای هرز و درزهای پنجره‌ها را مهار کنید تا باد سرد مستقیم روی پرندگان نوزد. تهویه را روی حالت حداقل (تایمری) حفظ کنید."
                    ),
                    ClimateActionItem(
                        categoryFa = "بستر و کف سالن",
                        titleFa = "افزایش ضخامت پوشال بستر",
                        descriptionFa = "ضخامت پوشال خشک را به ۷ تا ۱۰ سانتی‌متر برسانید تا سرمای کف بتنی به پاهای پرنده انتقال نیابد. بخش‌های مرطوب بستر را سریعاً جایگزین کنید."
                    ),
                    ClimateActionItem(
                        categoryFa = "آب و خوراک پرانرژی",
                        titleFa = "تأمین آب ولرم و جیره پرانرژی",
                        descriptionFa = "دمای آب خوری‌ها نباید خیلی سرد (زیر ۱۴ درجه) باشد. مصرف دان کافی و حاوی کربوهیدرات کالری لازم برای گرم نگه داشتن بدن را تأمین می‌کند."
                    )
                )
            )
        }
    }
}

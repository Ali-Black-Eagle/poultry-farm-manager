package com.example.poultryapp.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Subtabs within the Breeding Guide screen
 */
enum class GuideSection(val titleFa: String, val icon: ImageVector) {
    GROWTH_DASHBOARD("داشبورد رشد و گله", Icons.Default.Assessment),
    STEP_BY_STEP("نقشه راه پرورش", Icons.Default.FormatListNumbered),
    CALCULATORS("جعبه‌ابزار محاسباتی", Icons.Default.Calculate),
    TRIAGE_CHECKLISTS("تریاژ و چک‌لیست‌ها", Icons.Default.Checklist)
}

/**
 * Stage of development
 */
enum class GuidePhase(val titleFa: String, val badge: String) {
    PHASE_0("مرحله صفر: ارزیابی و خرید", "قبل از خرید"),
    PHASE_1("مرحله اول: آماده‌سازی سالن و تجهیزات", "قبل از ورود"),
    PHASE_2("مرحله دوم: ۲۴ ساعت اول ورود", "روز اول"),
    PHASE_3("مرحله سوم: مدیریت هفته‌های رشد", "دوره رشد"),
    PHASE_4("مرحله چهارم: علوم تغذیه، جیره و آب", "مدیریت خوراک"),
    PHASE_5("مرحله پنجم: امنیت زیستی و واکسیناسیون", "بهداشت و واکسن"),
    PHASE_6("مرحله ششم: مدیریت تخم‌گذاری و تولید", "دوره تولید")
}

/**
 * Step detail item
 */
data class GuideStepDetail(
    val id: String,
    val stepNumber: Int,
    val phase: GuidePhase,
    val title: String,
    val summary: String,
    val detailedPoints: List<String>,
    val warningNote: String? = null,
    val proTip: String? = null
)

/**
 * Row in the weekly growth matrix
 */
data class GrowthScheduleRow(
    val ageDisplay: String,
    val phaseName: String,
    val tempCelsius: String,
    val humidityPercent: String,
    val lightProgram: String,
    val spaceDensity: String,
    val feedType: String,
    val crudeProtein: String,
    val waterIntakeDaily: String,
    val targetWeight: String,
    val keyAction: String,
    val commonRisk: String
)

/**
 * Incubation parameter set
 */
data class IncubationGuideData(
    val species: BirdSpecies,
    val totalIncubationDays: Int,
    val setterDays: String,
    val setterTempCelsius: String,
    val setterHumidityPercent: String,
    val hatcherDays: String,
    val hatcherTempCelsius: String,
    val hatcherHumidityPercent: String,
    val turningProtocol: String,
    val candlingDays: String,
    val coolingAndMisting: String,
    val hatchHelpWarning: String,
    val criticalTips: List<String>
)

/**
 * Emergency triage scenario
 */
data class EmergencyScenario(
    val id: String,
    val title: String,
    val clinicalSigns: List<String>,
    val immediateActions: List<String>,
    val prohibitedMistakes: List<String>,
    val vetNotificationTrigger: String,
    val isCritical: Boolean = true
)

/**
 * Checkable list item
 */
data class InteractiveChecklistItem(
    val id: String,
    val categoryTitle: String,
    val text: String,
    val explanation: String,
    val isCrucial: Boolean = false
)

/**
 * Milestone in incubation cycle
 */
data class IncubationMilestone(
    val title: String,
    val jalaliDateStr: String,
    val daysFromStart: Int,
    val daysRemaining: Int,
    val description: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

/**
 * Hall equipment requirements
 */
data class EquipmentEstimate(
    val feedersCount: Int,
    val drinkersCount: Int,
    val floorAreaM2: Double,
    val nestsCount: Int?,
    val broodingAreaM2: Double
)

/**
 * Ventilation requirements
 */
data class VentilationEstimate(
    val minWinterM3h: Double,
    val maxSummerM3h: Double,
    val totalFlockWeightKg: Double
)

object ComprehensiveBreedingGuideCatalog {

    fun getGrowthSchedule(species: BirdSpecies): List<GrowthScheduleRow> {
        return when (species) {
            BirdSpecies.CHICKEN -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱ (روز ۱-۷)",
                    phaseName = "برودینگ اولیه",
                    tempCelsius = "۳۳ تا ۳۵ درجه",
                    humidityPercent = "۶۰ تا ۷۰٪",
                    lightProgram = "۲۳ ساعت نور / ۳۰-۴۰ لوکس",
                    spaceDensity = "۳۵ تا ۴۰ قطعه/m² (در گارد)",
                    feedType = "سوپر استارتر کرامبل",
                    crudeProtein = "۲۱ تا ۲۲٪",
                    waterIntakeDaily = "۲۵ تا ۳۵ میلی‌لیتر",
                    targetWeight = "۱۲۰ تا ۱۵۰ گرم",
                    keyAction = "آب‌شکر ۵٪ در ۲ ساعت اول؛ تست پر بودن چینه‌دان در ساعت ۸ و ۲۴.",
                    commonRisk = "خفگی ناشی از سرما، دهیدراسیون و عفونت کیسه زرده."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۲ (روز ۸-۱۴)",
                    phaseName = "برودینگ ثانویه",
                    tempCelsius = "۳۰ تا ۳۲ درجه",
                    humidityPercent = "۵۵ تا ۶۵٪",
                    lightProgram = "۲۰ ساعت نور / ۲۰ لوکس",
                    spaceDensity = "۲۵ تا ۳۰ قطعه/m²",
                    feedType = "استارتر کرامبل",
                    crudeProtein = "۲۰ تا ۲۱٪",
                    waterIntakeDaily = "۴۵ تا ۵۵ میلی‌لیتر",
                    targetWeight = "۲۴۰ تا ۲۸۰ گرم",
                    keyAction = "کاهش روزانه نیم درجه دما؛ واکسن قطره چشمی نیوکاسل و برونشیت.",
                    commonRisk = "تجمع گرد و غبار، تاول مفصلی ناشی از رطوبت بستر."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۳ (روز ۱۵-۲۱)",
                    phaseName = "رشد اولیه",
                    tempCelsius = "۲۷ تا ۲۹ درجه",
                    humidityPercent = "۵۵ تا ۶۰٪",
                    lightProgram = "۱۸ ساعت نور / ۱۵ لوکس",
                    spaceDensity = "۱۸ تا ۲۰ قطعه/m²",
                    feedType = "استارتر / رشد ۱",
                    crudeProtein = "۱۹ تا ۲۰٪",
                    waterIntakeDaily = "۶۵ تا ۸۰ میلی‌لیتر",
                    targetWeight = "۳۸۰ تا ۴۳۰ گرم",
                    keyAction = "حذف گاردهای مقوایی، گسترش دانخوری‌های ناودانی یا آویز.",
                    commonRisk = "شروع کوکسیدیوز بالینی در صورت خیس بودن پای آبخوری‌ها."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۴ (روز ۲۲-۲۸)",
                    phaseName = "پردرآوری کامل",
                    tempCelsius = "۲۴ تا ۲۶ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۶ ساعت نور / ۱۰ لوکس",
                    spaceDensity = "۱۲ تا ۱۵ قطعه/m²",
                    feedType = "پلت رشد ۱",
                    crudeProtein = "۱۸ تا ۱۹٪",
                    waterIntakeDaily = "۹۰ تا ۱۱۰ میلی‌لیتر",
                    targetWeight = "۵۳۰ تا ۶۰۰ گرم",
                    keyAction = "توزین تصادفی ۵٪ گله جهت سنجش یکنواختی (Uniformity).",
                    commonRisk = "استرس تغییر جیره؛ عدم یکنواختی رشد ناشی از کمبود فضای دانخوری."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۵ تا ۸",
                    phaseName = "اسکلتی و بلوغ اولیه",
                    tempCelsius = "۲۱ تا ۲۳ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۲ ساعت نور / ۵-۱۰ لوکس",
                    spaceDensity = "۱۰ تا ۱۲ قطعه/m²",
                    feedType = "پلت رشد ۲",
                    crudeProtein = "۱۶ تا ۱۷٪",
                    waterIntakeDaily = "۱۲۰ تا ۱۵۰ میلی‌لیتر",
                    targetWeight = "۷۰۰ تا ۱۱۰۰ گرم",
                    keyAction = "تهویه حداقلی در زمستان؛ تزریق واکسن دوگانه روغنی در هفته هفتم.",
                    commonRisk = "افزایش گاز آمونیاک به بالای ۲۰ppm و سوزش مخاط چشم."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۹ تا ۱۶",
                    phaseName = "پولت تخم‌گذار",
                    tempCelsius = "۱۸ تا ۲۱ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۸ تا ۱۰ ساعت (تاریکی جهت ممانعت از بلوغ زودرس)",
                    spaceDensity = "۸ تا ۱۰ قطعه/m²",
                    feedType = "پولت / نگهدارنده",
                    crudeProtein = "۱۵ تا ۱۶٪",
                    waterIntakeDaily = "۱۵۰ تا ۱۸۰ میلی‌لیتر",
                    targetWeight = "۱۲۰۰ تا ۱۶۰۰ گرم",
                    keyAction = "کنترل دقیق وزن برای جلوگیری از چاقی پولت؛ نصب نشیمنگاه (Perch).",
                    commonRisk = "پرکنی و کانیبالیسم در اثر نور شدید یا کمبود نمک جیره."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱۷ به بعد",
                    phaseName = "پیش‌تولید و تخم‌گذاری",
                    tempCelsius = "۱۸ تا ۲۲ درجه",
                    humidityPercent = "۵۰ تا ۶۵٪",
                    lightProgram = "۱۶ ساعت نور ثابت / ۱۵-۲۰ لوکس",
                    spaceDensity = "۵ تا ۷ قطعه/m²",
                    feedType = "جیره لایه ۱ (کلسیم ۳.۸٪)",
                    crudeProtein = "۱۶.۵ تا ۱۷.۵٪",
                    waterIntakeDaily = "۲۰۰ تا ۲۵۰ میلی‌لیتر",
                    targetWeight = "۱۷۰۰ تا ۲۰۰۰ گرم",
                    keyAction = "نصب لانه‌های تخم‌گذاری؛ تحریک نوری پلکانی؛ جمع‌آوری ۴ بار در روز.",
                    commonRisk = "پرولاپس (بیرون‌زدگی مقعد) در صورت بلوغ زودرس یا تخم دوصفار بزرگ."
                )
            )

            BirdSpecies.TURKEY -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱ (روز ۱-۷)",
                    phaseName = "برودینگ حساس بوقلمون",
                    tempCelsius = "۳۶ تا ۳۸ درجه (زیر لامپ)",
                    humidityPercent = "۶۵ تا ۷۰٪",
                    lightProgram = "۲۴ ساعت نور پرتوان / ۶۰-۸۰ لوکس",
                    spaceDensity = "۲۵ قطعه/m² (حلقه برودر)",
                    feedType = "سوپر استارتر بوقلمون",
                    crudeProtein = "۲۸٪ (بسیار حیاتی)",
                    waterIntakeDaily = "۳۰ تا ۴۵ میلی‌لیتر",
                    targetWeight = "۱۵۰ تا ۱۷۰ گرم",
                    keyAction = "چیدن نوک با نور لیزری در هچری؛ آموزش نوک زدن به آبخوری کاسه‌ای.",
                    commonRisk = "گرسنگی مفرط (Starve-out) به علت بینایی ضعیف جوجه بوقلمون."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۲ (روز ۸-۱۴)",
                    phaseName = "برودینگ میانی",
                    tempCelsius = "۳۳ تا ۳۵ درجه",
                    humidityPercent = "۶۰ تا ۶۵٪",
                    lightProgram = "۲۲ ساعت نور / ۳۰ لوکس",
                    spaceDensity = "۱۵ قطعه/m²",
                    feedType = "استارتر ۱ بوقلمون",
                    crudeProtein = "۲۶ تا ۲۷٪",
                    waterIntakeDaily = "۶۰ تا ۸۰ میلی‌لیتر",
                    targetWeight = "۳۴۰ تا ۳۹۰ گرم",
                    keyAction = "گسترش شعاع حلقه برودر؛ تأمین تهویه بدون وزش باد مستقیم.",
                    commonRisk = "عفونت‌های تنفسی و آسپرژیلوزیس ناشی از بستر آلوده."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۳ تا ۴",
                    phaseName = "رشد فزاینده",
                    tempCelsius = "۲۷ تا ۳۰ درجه",
                    humidityPercent = "۵۵ تا ۶۰٪",
                    lightProgram = "۱۶ ساعت نور / ۱۵ لوکس",
                    spaceDensity = "۸ تا ۱۰ قطعه/m²",
                    feedType = "استارتر ۲ بوقلمون",
                    crudeProtein = "۲۴ تا ۲۵٪",
                    waterIntakeDaily = "۱۰۰ تا ۱۴۰ میلی‌لیتر",
                    targetWeight = "۱.۰ تا ۱.۳ کیلوگرم",
                    keyAction = "حذف کامل حلقه‌ها؛ واکسیناسیون نیوکاسل بر اساس نظر دامپزشک.",
                    commonRisk = "کوکسیدیوز بوقلمون و تاول سینه ناشی از پوشال خیس."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۵ تا ۸",
                    phaseName = "رشد اسکلتی و عضلانی",
                    tempCelsius = "۲۰ تا ۲۲ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۴ ساعت نور / ۱۰ لوکس",
                    spaceDensity = "۴ تا ۵ قطعه/m²",
                    feedType = "رشد بوقلمون",
                    crudeProtein = "۲۱ تا ۲۲٪",
                    waterIntakeDaily = "۲۵۰ تا ۳۵۰ میلی‌لیتر",
                    targetWeight = "۳.۲ تا ۴.۰ کیلوگرم",
                    keyAction = "مدیریت رطوبت بستر؛ تأمین تهویه قوی برای تخلیه آمونیاک سنگین.",
                    commonRisk = "بیماری سرسیاه (هیستومونیازیس) در صورت تماس با مرغ یا خاک آلوده."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۹ به بعد",
                    phaseName = "پایانی و پرواربندی",
                    tempCelsius = "۱۶ تا ۱۹ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۲ ساعت نور / ۵ لوکس",
                    spaceDensity = "۲ تا ۲.۵ قطعه/m²",
                    feedType = "فینیشر ۱ و ۲",
                    crudeProtein = "۱۵ تا ۱۸٪",
                    waterIntakeDaily = "۴۵۰ تا ۷۰۰ میلی‌لیتر",
                    targetWeight = "۸ تا ۱۵ کیلوگرم (بسته به جنسیت)",
                    keyAction = "پایش سلامت مفاصل و تاندون پا؛ رعایت دوره پرهیز دارویی قبل از کشتار.",
                    commonRisk = "دررفتگی مفاصل و شکستگی پا در سویه‌های سنگین‌وزن گوشتی."
                )
            )

            BirdSpecies.QUAIL -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "روز ۱ تا ۳",
                    phaseName = "نوزادی بلدرچین",
                    tempCelsius = "۳۵ تا ۳۷ درجه",
                    humidityPercent = "۶۰ تا ۷۰٪",
                    lightProgram = "۲۴ ساعت نور / ۴۰ لوکس",
                    spaceDensity = "۷۰ تا ۸۰ قطعه/m²",
                    feedType = "استارتر پودری ریز",
                    crudeProtein = "۲۵ تا ۲۷٪",
                    waterIntakeDaily = "۳ تا ۵ میلی‌لیتر",
                    targetWeight = "۷ تا ۱۵ گرم",
                    keyAction = "انداختن تیله شیشه‌ای در آبخوری تا بلدرچین خیس نشود و غرق نگردد.",
                    commonRisk = "غرق شدن در آبخوری، خفگی در گوشه‌های چهارگوش کارتن."
                ),
                GrowthScheduleRow(
                    ageDisplay = "روز ۴ تا ۱۴ (هفته ۱ و ۲)",
                    phaseName = "رشد انفجاری",
                    tempCelsius = "۳۰ تا ۳۳ درجه",
                    humidityPercent = "۵۵ تا ۶۵٪",
                    lightProgram = "۲۲ ساعت نور / ۲۰ لوکس",
                    spaceDensity = "۵۰ تا ۶۰ قطعه/m²",
                    feedType = "استارتر کرامبل ریز",
                    crudeProtein = "۲۴ تا ۲۵٪",
                    waterIntakeDaily = "۸ تا ۱۲ میلی‌لیتر",
                    targetWeight = "۳۵ تا ۷۵ گرم",
                    keyAction = "تعویض روزانه کاغذ بستر؛ کاهش نیم درجه حرارت در هر روز.",
                    commonRisk = "کمبود ویتامین B و بروز عارضه انگشتان پیچ‌خورده (Curled-toe)."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۳ تا ۴",
                    phaseName = "رشد و پروبال",
                    tempCelsius = "۲۳ تا ۲۶ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۶ ساعت نور / ۱۰ لوکس",
                    spaceDensity = "۳۵ تا ۴۰ قطعه/m²",
                    feedType = "رشد بلدرچین",
                    crudeProtein = "۲۰ تا ۲۲٪",
                    waterIntakeDaily = "۱۵ تا ۲۲ میلی‌لیتر",
                    targetWeight = "۱۲۰ تا ۱۶۰ گرم",
                    keyAction = "انتقال به قفس‌های شیب‌دار طبقاتی مکانیزه در صورت پرورش قفسی.",
                    commonRisk = "پرواز ناگهانی و برخورد سر با سقف قفس (نیاز به سقف فوم/توری)."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۵ به بعد",
                    phaseName = "بلوغ جنسی و تخم‌گذاری",
                    tempCelsius = "۲۰ تا ۲۲ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۶ تا ۱۷ ساعت ثابت / ۱۵ لوکس",
                    spaceDensity = "۲۵ قطعه در بستر یا ۱۶۰cm² در قفس",
                    feedType = "تخم‌گذار بلدرچین (کلسیم ۳٪)",
                    crudeProtein = "۱۹ تا ۲۰٪",
                    waterIntakeDaily = "۲۵ تا ۳۵ میلی‌لیتر",
                    targetWeight = "۲۰۰ تا ۲۴۰ گرم",
                    keyAction = "نسبت ۱ نر به ۳ ماده برای تخم نطفه‌دار؛ جمع‌آوری ۲ بار در روز تخم‌ها.",
                    commonRisk = "کاهش شدید تخم‌گذاری در اثر حتی نیم‌ساعت قطع نور یا سر و صدای ناگهانی."
                )
            )

            BirdSpecies.DUCK -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱",
                    phaseName = "برودینگ خشک جوجه‌اردک",
                    tempCelsius = "۳۰ تا ۳۲ درجه",
                    humidityPercent = "۶۰ تا ۶۵٪",
                    lightProgram = "۲۴ ساعت نور / ۳۰ لوکس",
                    spaceDensity = "۲۵ تا ۳۰ قطعه/m²",
                    feedType = "استارتر کرامبل مرطوب‌نشده",
                    crudeProtein = "۲۱ تا ۲۲٪",
                    waterIntakeDaily = "۴۰ تا ۶۰ میلی‌لیتر",
                    targetWeight = "۳۵۰ تا ۴۵۰ گرم",
                    keyAction = "شنا مطلقا ممنوع! عمق آبخوری فقط در حد شستشوی منقار و چشم باشد.",
                    commonRisk = "خیس شدن کرک‌ها، لرزیدن و تلفات ناشی از هیپوترمی (افت دمای بدن)."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۲ تا ۳",
                    phaseName = "رشد و ضدآب شدن پرها",
                    tempCelsius = "۲۴ تا ۲۶ درجه",
                    humidityPercent = "۶۰ تا ۷۰٪",
                    lightProgram = "۱۸ ساعت نور / ۱۵ لوکس",
                    spaceDensity = "۱۵ تا ۱۸ قطعه/m²",
                    feedType = "استارتر پلت ریز",
                    crudeProtein = "۱۹ تا ۲۰٪",
                    waterIntakeDaily = "۱۰۰ تا ۱۵۰ میلی‌لیتر",
                    targetWeight = "۸۵۰ تا ۱۵۰۰ گرم",
                    keyAction = "افزایش ضخامت بستر؛ هدایت زهکش آب به بیرون سالن.",
                    commonRisk = "بستر باتلاقی و سوختگی آمونیاکی کف پای اردک."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۴ به بعد",
                    phaseName = "پرواربندی یا تخم‌گذاری",
                    tempCelsius = "۱۶ تا ۲۰ درجه (بسیار مقاوم)",
                    humidityPercent = "۶۰ تا ۷۰٪",
                    lightProgram = "طبیعی / ۱۴ ساعت",
                    spaceDensity = "۴ تا ۵ قطعه/m²",
                    feedType = "پلت رشد و نگهداری",
                    crudeProtein = "۱۵ تا ۱۷٪",
                    waterIntakeDaily = "۲۵۰ تا ۴۰۰ میلی‌لیتر",
                    targetWeight = "۲.۵ تا ۳.۲ کیلوگرم",
                    keyAction = "فراهم ساختن حوضچه شنا با آب تمیز روان و امکان خروج آسان.",
                    commonRisk = "مسمومیت با سم بوتولینوم ناشی از لجن و آب راکد گندیده حوضچه."
                )
            )

            BirdSpecies.GOOSE -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱ تا ۲",
                    phaseName = "برودینگ اولیه غاز",
                    tempCelsius = "۳۱ تا ۳۳ درجه",
                    humidityPercent = "۶۰ تا ۶۵٪",
                    lightProgram = "۲۴ ساعت هفته اول، سپس ۱۸ ساعت",
                    spaceDensity = "۸ تا ۱۰ قطعه/m²",
                    feedType = "استارتر کرامبل غاز",
                    crudeProtein = "۲۰ تا ۲۱٪",
                    waterIntakeDaily = "۵۰ تا ۸۰ میلی‌لیتر",
                    targetWeight = "۴۰۰ تا ۸۰۰ گرم",
                    keyAction = "حفاظت شدید در برابر جانوران موذی؛ دسترسی به علف خردشده نرم از روز ۱۰.",
                    commonRisk = "حساسیت به سموم دارویی و خفگی در گوشه‌های سرد."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۳ تا ۵",
                    phaseName = "عادت‌پذیری به چراگاه",
                    tempCelsius = "۲۲ تا ۲۵ درجه",
                    humidityPercent = "طبیعی",
                    lightProgram = "طبیعی روز",
                    spaceDensity = "۴ قطعه/m² سرپوشیده + گردشگاه",
                    feedType = "پلت رشد + علوفه شبدر/یونجه",
                    crudeProtein = "۱۶ تا ۱۸٪",
                    waterIntakeDaily = "۱۵۰ تا ۲۵۰ میلی‌لیتر",
                    targetWeight = "۱.۵ تا ۲.۵ کیلوگرم",
                    keyAction = "شنا در حوضچه کم‌عمق؛ رهاسازی در چراگاه در ساعات معتدل روز.",
                    commonRisk = "حمله کلاغ، سگ یا پرندگان شکاری به جوجه‌غازها در محوطه باز."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۶ به بعد",
                    phaseName = "چراگاه آزاد و بلوغ",
                    tempCelsius = "محیط آزاد (مقاوم‌ترین پرنده)",
                    humidityPercent = "طبیعی",
                    lightProgram = "نور طبیعی روز",
                    spaceDensity = "۱ قطعه در سالن + ۵m² مرتع",
                    feedType = "چرای آزاد + مکمل دانه کامل",
                    crudeProtein = "۱۳ تا ۱۵٪",
                    waterIntakeDaily = "۳۵۰ تا ۵۰۰ میلی‌لیتر",
                    targetWeight = "۴.۰ تا ۶.۰ کیلوگرم",
                    keyAction = "فنس‌کشی محکم مرتع؛ تأمین سنگ‌ریزه (Grit) جهت هضم فیبر علوفه در سنگدان.",
                    commonRisk = "بلعیدن تکه‌های سیم، طناب یا پلاستیک رها شده در باغ و مرتع."
                )
            )

            BirdSpecies.GUINEA_FOWL -> listOf(
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱ (روز ۱-۷)",
                    phaseName = "برودینگ حساس مرغ شاخدار",
                    tempCelsius = "۳۵ تا ۳۷ درجه (فوق‌العاده حساس به سرما)",
                    humidityPercent = "۶۵ تا ۷۰٪",
                    lightProgram = "۲۴ ساعت نور / ۳۰-۴۰ لوکس",
                    spaceDensity = "۳۰ تا ۳۵ قطعه/m²",
                    feedType = "سوپر استارتر پرپروتئین",
                    crudeProtein = "۲۵ تا ۲۶٪ (بسیار حیاتی)",
                    waterIntakeDaily = "۱۵ تا ۲۵ میلی‌لیتر",
                    targetWeight = "۷۰ تا ۹۰ گرم",
                    keyAction = "آب‌شکر ۵٪ در ۲ ساعت اول؛ ممانعت اکید از خیس شدن پرزها در آبخوری.",
                    commonRisk = "تلفات صاعقه‌ای در صورت افت دما به زیر ۳۰ درجه یا خیس شدن کرک‌ها."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۲ تا ۴",
                    phaseName = "پردرآوری و رشد اولیه",
                    tempCelsius = "۲۸ تا ۳۱ درجه",
                    humidityPercent = "۵۵ تا ۶۵٪",
                    lightProgram = "۲۰ ساعت نور / ۲۰ لوکس",
                    spaceDensity = "۱۸ تا ۲۲ قطعه/m²",
                    feedType = "استارتر کرامبل شاخدار",
                    crudeProtein = "۲۳ تا ۲۴٪",
                    waterIntakeDaily = "۳۵ تا ۶۰ میلی‌لیتر",
                    targetWeight = "۲۰۰ تا ۳۵۰ گرم",
                    keyAction = "کاهش روزانه نیم درجه دما؛ واکسن نیوکاسل چشمی؛ پرهیز از سر و صدای ناگهانی.",
                    commonRisk = "رمیدن جمعی (Panic) و کپه شدن در گوشه‌های سالن."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۵ تا ۸",
                    phaseName = "عادت‌پذیری به گردشگاه و باغ",
                    tempCelsius = "۲۲ تا ۲۵ درجه",
                    humidityPercent = "طبیعی",
                    lightProgram = "۱۴ ساعت نور طبیعی",
                    spaceDensity = "۱۰ تا ۱۲ قطعه/m² سالن + گردشگاه باز",
                    feedType = "پلت رشد شاخدار",
                    crudeProtein = "۲۰ تا ۲۱٪",
                    waterIntakeDaily = "۸۰ تا ۱۲۰ میلی‌لیتر",
                    targetWeight = "۵۵۰ تا ۸۵۰ گرم",
                    keyAction = "نصب نشیمنگاه‌های مرتفع ۱ تا ۱.۵ متری؛ رهاسازی در باغات جهت شکار آفات.",
                    commonRisk = "پرواز به بالای درختان یا خروج از فنس (نیاز به فنس ۱.۸ متری)."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۹ تا ۱۴",
                    phaseName = "پایانی و پروار گوشتی",
                    tempCelsius = "۱۸ تا ۲۲ درجه (مقاوم)",
                    humidityPercent = "طبیعی",
                    lightProgram = "نور طبیعی روز",
                    spaceDensity = "۶ تا ۸ قطعه/m²",
                    feedType = "فینیشر پرواری",
                    crudeProtein = "۱۷ تا ۱۸٪",
                    waterIntakeDaily = "۱۵۰ تا ۲۰۰ میلی‌لیتر",
                    targetWeight = "۱.۲ تا ۱.۴ کیلوگرم (سن کشتار ایده‌آل)",
                    keyAction = "توزین گله جهت ذبح در هفته ۱۲ تا ۱۴؛ گوشت ترد ارگانیک با طعم قرقاول.",
                    commonRisk = "سفت شدن بافت گوشت در صورت تاخیر در کشتار به بالای ۱۶ هفتگی."
                ),
                GrowthScheduleRow(
                    ageDisplay = "هفته ۱۵ به بعد",
                    phaseName = "گله مولد و تخم‌گذاری فصلی",
                    tempCelsius = "۱۸ تا ۲۲ درجه",
                    humidityPercent = "۵۰ تا ۶۰٪",
                    lightProgram = "۱۶ ساعت نور ثابت / ۱۵ لوکس",
                    spaceDensity = "۴ تا ۵ قطعه/m²",
                    feedType = "تخم‌گذار شاخدار (کلسیم ۳.۵٪)",
                    crudeProtein = "۱۸ تا ۱۹٪",
                    waterIntakeDaily = "۲۰۰ تا ۲۵۰ میلی‌لیتر",
                    targetWeight = "۱.۵ تا ۱.۸ کیلوگرم",
                    keyAction = "نصب جعبه‌های تخم‌گذاری در کنج‌های تاریک؛ نسبت ۱ نر به ۴ ماده.",
                    commonRisk = "تخم‌گذاری مخفیانه در علفزارها و کاهش آمار جمع‌آوری تخم نطفه‌دار."
                )
            )
        }
    }

    fun getIncubationData(species: BirdSpecies): IncubationGuideData {
        return when (species) {
            BirdSpecies.CHICKEN -> IncubationGuideData(
                species = BirdSpecies.CHICKEN,
                totalIncubationDays = 21,
                setterDays = "روز ۱ تا ۱۸",
                setterTempCelsius = "۳۷.۵ تا ۳۷.۷ درجه (۹۹.۵ تا ۹۹.۸°F)",
                setterHumidityPercent = "۵۵ تا ۶۰ درصد",
                hatcherDays = "روز ۱۹ تا ۲۱",
                hatcherTempCelsius = "۳۶.۸ تا ۳۷.۰ درجه (۹۸.۲ تا ۹۸.۶°F)",
                hatcherHumidityPercent = "۷۰ تا ۷۵ درصد",
                turningProtocol = "چرخش ۴۵ درجه در هر ۱ تا ۲ ساعت (توقف در پایان روز ۱۸)",
                candlingDays = "روز ۷ (مشاهده رگ‌بندی عنکبوتی) و روز ۱۴ (جنین تیره با کیسه هوای شفاف)",
                coolingAndMisting = "برای تخم مرغ نیازی به آب‌پاشی و خنک‌سازی روزانه نیست.",
                hatchHelpWarning = "هرگز به زور پوسته تخم را باز نکنید؛ اگر رگ خونی فعال باشد، پرنده بر اثر خونریزی درجا تلف می‌شود.",
                criticalTips = listOf(
                    "ذخیره‌سازی تخم پیش از جوجه‌کشی بیش از ۷ روز به شدت هچ را کاهش می‌دهد.",
                    "تخم‌های کثیف را با پارچه خشک تمیز کنید، هرگز با آب نشویید.",
                    "در ۳ روز آخر (هچر) درب دستگاه را به هیچ وجه باز نکنید تا رطوبت تخلیه نشود."
                )
            )

            BirdSpecies.TURKEY -> IncubationGuideData(
                species = BirdSpecies.TURKEY,
                totalIncubationDays = 28,
                setterDays = "روز ۱ تا ۲۵",
                setterTempCelsius = "۳۷.۳ تا ۳۷.۵ درجه",
                setterHumidityPercent = "۵۵ تا ۵۸ درصد",
                hatcherDays = "روز ۲۶ تا ۲۸",
                hatcherTempCelsius = "۳۶.۵ تا ۳۶.۸ درجه",
                hatcherHumidityPercent = "۷۵ تا ۸۰ درصد",
                turningProtocol = "چرخش خودکار هر ۱ ساعت؛ توقف در روز ۲۵",
                candlingDays = "روز ۸ و روز ۲۱",
                coolingAndMisting = "از روز ۱۰ تا ۲۴ خنک‌سازی سبک ۵ دقیقه‌ای در روز به گردش هوا کمک می‌کند.",
                hatchHelpWarning = "جوجه بوقلمون در خروج از تخم تنبل‌تر از مرغ است، اما کمک زودهنگام خطر عفونت بند ناف را ۵ برابر می‌کند.",
                criticalTips = listOf(
                    "پوسته تخم بوقلمون ضخیم‌تر است؛ رطوبت هچر باید حتماً بالای ۷۵٪ تثبیت شود.",
                    "تهویه در هفته چهارم بسیار حیاتی است زیرا جنین اکسیژن فراوان مصرف می‌کند."
                )
            )

            BirdSpecies.QUAIL -> IncubationGuideData(
                species = BirdSpecies.QUAIL,
                totalIncubationDays = 17,
                setterDays = "روز ۱ تا ۱۴",
                setterTempCelsius = "۳۷.۶ تا ۳۷.۸ درجه",
                setterHumidityPercent = "۵۸ تا ۶۲ درصد",
                hatcherDays = "روز ۱۵ تا ۱۷",
                hatcherTempCelsius = "۳۷.۲ تا ۳۷.۴ درجه",
                hatcherHumidityPercent = "۷۵ تا ۸۰ درصد",
                turningProtocol = "چرخش متوالی حداقل ۱۲ تا ۲۴ بار در شبانه‌روز؛ توقف در روز ۱۴",
                candlingDays = "روز ۶ و روز ۱۱ (پوسته طرح‌دار بلدرچین نیاز به کندلر پرنور دارد)",
                coolingAndMisting = "نیازی به خنک‌سازی ندارد؛ نوسان رطوبت کشنده است.",
                hatchHelpWarning = "جوجه بلدرچین به قدری ظریف است که هرگونه مداخله مکانیکی باعث پارگی کیسه صفرا یا بند ناف می‌شود.",
                criticalTips = listOf(
                    "کف سبد هچر نباید لغزنده باشد؛ توری بسیار ریز پهن کنید تا فلجی پا (Splay Leg) رخ ندهد.",
                    "هچ بلدرچین بسیار برق‌آسا و ظرف چند ساعت اتفاق می‌افتد."
                )
            )

            BirdSpecies.DUCK -> IncubationGuideData(
                species = BirdSpecies.DUCK,
                totalIncubationDays = 28,
                setterDays = "روز ۱ تا ۲۵ (اردک اسرائیلی ۳۵ روز)",
                setterTempCelsius = "۳۷.۳ تا ۳۷.۵ درجه",
                setterHumidityPercent = "۶۵ تا ۷۰ درصد",
                hatcherDays = "روز ۲۶ تا ۲۸",
                hatcherTempCelsius = "۳۶.۸ درجه",
                hatcherHumidityPercent = "۸۰ تا ۸۵ درصد (بسیار بالا)",
                turningProtocol = "چرخش افقی کامل تا روز ۲۵",
                candlingDays = "روز ۷ و روز ۲۰",
                coolingAndMisting = "حیاتی: از روز ۱۰ به بعد روزی یک‌بار تخم‌ها را ۱۰ دقیقه خنک کرده و با آب ولرم اسپری غبارپاش کنید.",
                hatchHelpWarning = "پوسته تخم اردک بسیار مومی و چرب است و بدون رطوبت کافی تبدیل به سنگ می‌شود.",
                criticalTips = listOf(
                    "اسپری آب ولرم منافذ چربی پوسته را شسته و تنفس جنین را تسهیل می‌کند.",
                    "رطوبت هچر اگر کمتر از ۸۰٪ باشد، جنین در غشای زیر پوسته گیر کرده و خشک می‌شود."
                )
            )

            BirdSpecies.GOOSE -> IncubationGuideData(
                species = BirdSpecies.GOOSE,
                totalIncubationDays = 31,
                setterDays = "روز ۱ تا ۲۷",
                setterTempCelsius = "۳۷.۰ تا ۳۷.۲ درجه",
                setterHumidityPercent = "۶۵ تا ۷۰ درصد",
                hatcherDays = "روز ۲۸ تا ۳۱",
                hatcherTempCelsius = "۳۶.۴ تا ۳۶.۶ درجه",
                hatcherHumidityPercent = "۸۰ تا ۸۵ درصد",
                turningProtocol = "چرخش ۱۸۰ درجه دستی یا زاویه تند مکانیکی",
                candlingDays = "روز ۸ و روز ۲۲",
                coolingAndMisting = "از روز ۸ به بعد، خنک‌سازی روزانه تا ۳۲ درجه و آب‌پاشی با آب ۳۸ درجه کاملاً الزامی است.",
                hatchHelpWarning = "دوره نوک‌زدن تا خروج کامل جوجه غاز ممکن است ۲۴ تا ۳۶ ساعت طول بکشد؛ شتاب‌زده عمل نکنید.",
                criticalTips = listOf(
                    "تخم غاز به دلیل جثه درشت، حرارت درونی بالایی در هفته سوم تولید می‌کند؛ خنک‌سازی روزانه مرگ جنین در اثر داغی را مهار می‌کند.",
                    "ضدعفونی کردن دقیق قبل از ورود به انکوباتور با گاز یا محلول ملایم ضروری است."
                )
            )

            BirdSpecies.GUINEA_FOWL -> IncubationGuideData(
                species = BirdSpecies.GUINEA_FOWL,
                totalIncubationDays = 28,
                setterDays = "روز ۱ تا ۲۴",
                setterTempCelsius = "۳۷.۴ تا ۳۷.۶ درجه",
                setterHumidityPercent = "۵۵ تا ۵۸ درصد",
                hatcherDays = "روز ۲۵ تا ۲۸",
                hatcherTempCelsius = "۳۶.۸ تا ۳۷.۰ درجه",
                hatcherHumidityPercent = "۷۸ تا ۸۲ درصد (حیاتی جهت شکستن پوسته ضخیم)",
                turningProtocol = "چرخش دقیق تا پایان روز ۲۴؛ انتقال به هچر در روز ۲۵",
                candlingDays = "روز ۸ و روز ۲۲ (پوسته ضخیم و متراکم نیاز به نور کندلر قوی دارد)",
                coolingAndMisting = "از روز ۱۲ به بعد خنک‌سازی سبک روزانه ۵ دقیقه‌ای به نرمی پوسته کمک می‌کند.",
                hatchHelpWarning = "پوسته مرغ شاخدار ۳ برابر ضخیم‌تر از مرغ معمولی است؛ رطوبت هچر باید حتماً بالای ۷۸٪ ثابت بماند.",
                criticalTips = listOf(
                    "پوسته فوق‌العاده متراکم مرغ شاخدار تبخیر آب را کند می‌کند؛ رطوبت ۵۵٪ ستر برای ایجاد کیسه هوایی استاندارد الزامی است.",
                    "کف سبد هچر را با توری نرم بپوشانید تا جوجه‌های فعال دچار فلجی پا نشوند."
                )
            )
        }
    }

    fun getEmergencyScenarios(): List<EmergencyScenario> {
        return listOf(
            EmergencyScenario(
                id = "emg_sudden_mortality",
                title = "تلفات ناگهانی و پرنده‌های افتاده (Acute Mortality)",
                clinicalSigns = listOf(
                    "مشاهده چندین پرنده بی‌جان بدون علائم بالینی طولانی قبلی",
                    "کبودی تاج و ریش یا تیرگی مفاصل پا",
                    "خروج ترشحات کف‌آلود یا خونابه‌ای از منقار و کلواک"
                ),
                immediateActions = listOf(
                    "قرنطینه ۱۰۰ درصدی سالن و منع ورود و خروج هرگونه پرنده یا کود.",
                    "جمع‌آوری فوری لاشه‌ها در کیسه پلاستیکی ضخیم دربسته جهت بررسی آزمایشگاهی.",
                    "چک کردن فوری هواکش‌ها و هیتر برای رد فرضیه گازگرفتگی با مونوکسیدکربن یا خفگی آمونیاکی.",
                    "قطع کردن خطوط خوراک مشکوک و جایگزینی با آب تمیز و الکترولیت."
                ),
                prohibitedMistakes = listOf(
                    "مصرف خودسرانه آنتی‌بیوتیک یا ویتامین در آب (کالبدگشایی را مخدوش می‌کند).",
                    "رها کردن لاشه در گوشه سالن یا بیرون انداختن جلوی سگ و گربه."
                ),
                vetNotificationTrigger = "اگر تلفات در یک روز بیش از ۰.۵ درصد کل گله باشد، بلافاصله باید به دامپزشک طیور اطلاع داده شود."
            ),
            EmergencyScenario(
                id = "emg_feed_water_drop",
                title = "افت ناگهانی مصرف دان یا آب",
                clinicalSigns = listOf(
                    "راکد ماندن آبخوری‌ها یا خالی نشدن دانخوری‌ها به مدت ۲۴ ساعت",
                    "چینه‌دان‌های خالی در بازدید شبانه",
                    "بی‌حالی عمومی و توقف آوازخوانی یا تحرک گله"
                ),
                immediateActions = listOf(
                    "تست طعم و دمای آب (آب داغ ناشی از آفتاب روی لوله یا آب یخ‌زده هرگز مصرف نمی‌شود).",
                    "بررسی فشار نیپل‌ها و هواگیری انتهای لوله‌های آبخوری.",
                    "بررسی بوی دان؛ در صورت بوی ترشیدگی، نم‌کشیدگی یا تغییر رنگ، دان فورا تعویض شود."
                ),
                prohibitedMistakes = listOf(
                    "اجبار پرنده به خوردن دان کپک‌زده با اضافه کردن شکر یا اسانس.",
                    "قطع تهویه به بهانه گرم نگه‌داشتن سالن."
                ),
                vetNotificationTrigger = "افت بیش از ۲۰ درصدی مصرف آب معمولاً پیش‌درآمد بیماری‌های ویروسی حاد (نیوکاسل یا آنفلوانزا) ظرف ۴۸ ساعت آینده است."
            ),
            EmergencyScenario(
                id = "emg_heat_stress",
                title = "گرمازدگی حاد و له‌له زدن (Heat Stress)",
                clinicalSigns = listOf(
                    "باز کردن بال‌ها و دور کردن از بدن",
                    "نفس‌نفس زدن با منقار کاملاً باز (Panting) و تنفس تند",
                    "پهن شدن روی بستر خنک و اسهال آبکی ناشی از عطش مفرط"
                ),
                immediateActions = listOf(
                    "افزایش سرعت جریان باد بر روی سر پرندگان به ۲ تا ۲.۵ متر بر ثانیه.",
                    "خاموش کردن چراغ‌ها و توقف کامل خوراک‌دهی در ساعات گرم روز (هضم دان تولید حرارت داخلی می‌کند).",
                    "افزودن ویتامین C و بی‌کربنات سدیم (جوش شیرین) یا پودر الکترولیت به آب خنک آشامیدنی.",
                    "مه‌پاشی هوایی یا اسپری آب سرد روی سقف سالن."
                ),
                prohibitedMistakes = listOf(
                    "ریختن آب سرد یخ‌زده به صورت مستقیم روی پرندگان گرمازده (باعث شوک حرارتی و سکته قلبی آنی می‌شود).",
                    "تغذیه گله در اوج گرمای ظهر."
                ),
                vetNotificationTrigger = "اگر دمای سالن از ۳۴ درجه فراتر رفته و تلفات خفگی آغاز شود."
            ),
            EmergencyScenario(
                id = "emg_power_outage",
                title = "قطعی برق سالن و خطر خفگی گروهی",
                clinicalSigns = listOf(
                    "خاموش شدن فن‌های تهویه و تجمع حرارت و آمونیاک در سقف",
                    "جیغ کشیدن و انباشته شدن پرندگان روی هم در گوشه‌ها (Suffocation)"
                ),
                immediateActions = listOf(
                    "باز کردن فوری تمام درب‌های ورودی و خروجی اضطراری و پنجره‌ها جهت برقراری تهویه طبیعی.",
                    "استفاده از دیزل‌ژنراتور یا اینورتر برق اضطراری برای راه‌اندازی حداقل ۲ هواکش.",
                    "روشن کردن روشنایی اضطراری باتری‌دار در نقاط مختلف برای جلوگیری از هجوم به نقاط تاریک."
                ),
                prohibitedMistakes = listOf(
                    "ترک سالن و رها کردن جوجه‌ها در تاریکی مطلق (تلفات خفگی ظرف کمتر از ۳۰ دقیقه رخ می‌دهد)."
                ),
                vetNotificationTrigger = "پس از وصل مجدد برق، وضعیت گله از نظر آسیب ریوی پایش شود."
            )
        )
    }

    fun getInteractiveChecklists(): List<InteractiveChecklistItem> {
        return listOf(
            InteractiveChecklistItem(
                id = "chk_b1",
                categoryTitle = "۱. پیش از خرید و ورود",
                text = "ضدعفونی کامل سالن، شعله‌افکنی کف و سمپاشی درزها",
                explanation = "استفاده از فرمالین، گلوتارآلدئید یا پرسیدین حداقل ۴۸ ساعت قبل از ورود.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_b2",
                categoryTitle = "۱. پیش از خرید و ورود",
                text = "پهن کردن پوشال چوب خشک به ضخامت حداقل ۷ سانتی‌متر",
                explanation = "پوشال نباید خاک‌اره نرم داشته باشد تا در گلوی جوجه گیر نکند.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_b3",
                categoryTitle = "۱. پیش از خرید و ورود",
                text = "روشن کردن سیستم گرمایشی ۲۴ ساعت قبل از تخلیه جوجه",
                explanation = "کف سالن و پوشال‌ها باید به دمای ۳۲ تا ۳۴ درجه سانتی‌گراد رسیده باشند.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_d1",
                categoryTitle = "۲. بازرسی صبحگاهی (روزانه)",
                text = "بررسی سلامت جریان آب در خطوط آبخوری و نیپل‌ها",
                explanation = "از عدم گرفتگی ساچمه‌ها و هوادهی انتهای لوله‌ها اطمینان یابید.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_d2",
                categoryTitle = "۲. بازرسی صبحگاهی (روزانه)",
                text = "ثبت حداقل و حداکثر دمای شب گذشته از روی دماسنج ماکزیمم/مینیمم",
                explanation = "نوسان دمای شبانه نباید از ۲ تا ۳ درجه سانتی‌گراد تجاوز کند.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_d3",
                categoryTitle = "۲. بازرسی صبحگاهی (روزانه)",
                text = "بررسی رنگ و قوام مدفوع روی بستر",
                explanation = "وجود لکه‌های خونی، کف‌آلود زرد یا سبز صابونی باید گزارش شود.",
                isCrucial = false
            ),
            InteractiveChecklistItem(
                id = "chk_n1",
                categoryTitle = "۳. بازرسی ظهر و عصر (روزانه)",
                text = "تست فشردگی رطوبت بستر زیر آبخوری‌ها",
                explanation = "مشت کردن پوشال؛ نباید خیس و گلوله‌ای بماند.",
                isCrucial = false
            ),
            InteractiveChecklistItem(
                id = "chk_n2",
                categoryTitle = "۳. بازرسی ظهر و عصر (روزانه)",
                text = "جمع‌آوری تخم‌مرغ‌ها و بررسی سلامت پوسته",
                explanation = "تخم‌مرغ‌ها نباید در لانه انباشته شوند تا خطر شکستگی به صفر برسد.",
                isCrucial = true
            ),
            InteractiveChecklistItem(
                id = "chk_e1",
                categoryTitle = "۴. بازرسی شبانه و خاموشی",
                text = "بررسی چینه‌دان ۱۰ پرنده به صورت تصادفی",
                explanation = "چینه‌دان باید پر، نرم و یکنواخت باشد.",
                isCrucial = false
            ),
            InteractiveChecklistItem(
                id = "chk_e2",
                categoryTitle = "۴. بازرسی شبانه و خاموشی",
                text = "گوش دادن به صدای تنفس گله در تاریکی مطلق",
                explanation = "هرگونه صدای خس‌خس (Râle)، عطسه یا خرخر نشانه برونشیت یا مایکوپلاسما است.",
                isCrucial = true
            )
        )
    }

    fun getStepByStepGuide(species: BirdSpecies): List<GuideStepDetail> {
        return listOf(
            GuideStepDetail(
                id = "step_p0",
                stepNumber = 0,
                phase = GuidePhase.PHASE_0,
                title = "تصمیم‌گیری استراتژیک، برآورد هزینه و خرید جوجه سالم",
                summary = "موفقیت پرورش پیش از خرید جوجه تعیین می‌شود؛ انتخاب درست نژاد و منبع خرید نیمی از راه است.",
                detailedPoints = listOf(
                    "برآورد متراژ سالن: هرگز بیش از ظرفیت استاندارد جوجه نخرید (تراکم بالا مساوی است با بیماری و تلفات).",
                    "خرید فقط از هچری‌ها و گله‌های مادری با گواهی سلامت دامپزشکی و فاقد آلودگی سالمونلا و مایکوپلاسما.",
                    "تست بالینی در لحظه خرید: بند ناف باید کاملاً بسته و خشک باشد؛ مخرج تمیز و شکم نرم و غیرداغ باشد.",
                    "آماده‌سازی محل قرنطینه مجزا در صورتی که در فارم پرندگان دیگری نگهداری می‌شوند (حداقل فاصله ۳۰ متر به مدت ۲۱ روز)."
                ),
                warningNote = "خرید جوجه‌های ارزان‌قیمت از بازارهای هفتگی و دست‌فروشان مساوی با وارد کردن ویروس به سالن و نابودی سرمایه است.",
                proTip = "برای شروع کار اگر بی‌تجربه هستید، با تعداد کم (۵۰ تا ۱۰۰ قطعه مرغ یا ۲۰۰ قطعه بلدرچین) شروع کنید تا قلق‌های رفتاری دستتان بیاید."
            ),
            GuideStepDetail(
                id = "step_p1",
                stepNumber = 1,
                phase = GuidePhase.PHASE_1,
                title = "آماده‌سازی سالن، بستر، برودر و تجهیزات",
                summary = "فضای زندگی جوجه باید استریل، گرم، فاقد جریان باد تند و مجهز به تجهیزات در دسترس باشد.",
                detailedPoints = listOf(
                    "پاکسازی مکانیکی و شستشو با کف‌شوی پرفشار، سپس شعله‌افکنی کف سیمانی و ضدعفونی گازدهی یا مه‌پاشی.",
                    "پهن کردن پوشال خشک چوب صنوبر به ضخامت ۵ تا ۸ سانتی‌متر (هرگز خاک‌اره ریز نریزید چون بلعیده می‌شود).",
                    "نصب حلقه گارد مقوایی با ارتفاع ۴۵ سانتی‌متر به قطر ۳ تا ۴ متر برای محصور کردن جوجه‌ها در نزدیکی منبع حرارت.",
                    "گرم کردن اولیه سالن از ۲۴ ساعت قبل تا بتن کف کاملاً گرم شود و دمای بستر به ۳۲ تا ۳۴ درجه سانتی‌گراد برسد."
                ),
                warningNote = "گذاشتن جوجه روی کف سرد حتی اگر دمای هوای سالن بالا باشد، جذب زرده را متوقف کرده و تلفات ۴۰ درصدی می‌دهد.",
                proTip = "گوشه‌های سالن را با کارتن گرد (منحنی) کنید تا در صورت ترسیدن جوجه‌ها روی هم تلنبار نشوند و خفه نشوند."
            ),
            GuideStepDetail(
                id = "step_p2",
                stepNumber = 2,
                phase = GuidePhase.PHASE_2,
                title = "۲۴ ساعت طلایی ورود و رفتارشناسی حرارتی",
                summary = "حساس‌ترین شبانه‌روز عمر پرنده؛ اقدامات این ساعات یکنواختی و بقای کل دوره را رقم می‌زند.",
                detailedPoints = listOf(
                    "در بدو ورود تا ۲ ساعت اول فقط آب ولرم تمیز (۲۰-۲۴ درجه) همراه با ۵٪ شکر و الکترولیت داده شود.",
                    "پس از ۲ ساعت، دان استارتر کرامبل روی کاغذهای سفره‌ای تمیز کرافت در سطح ۵۰٪ حلقه پاشیده شود.",
                    "رفتارشناسی دمایی: اگر جوجه‌ها زیر لامپ تلنبار شدند محیط سرد است؛ اگر به لبه‌ها چسبیدند و له له زدند محیط داغ است؛ اگر یکنواخت پخش شدند و جیک‌جیک ملایم دارند دما ایده‌آل است.",
                    "تست پر بودن چینه‌دان: ۸ ساعت بعد ۸۰٪ و ۲۴ ساعت بعد ۹۵٪ جوجه‌ها باید چینه‌دان نرم و پر داشته باشند."
                ),
                warningNote = "روشن کردن تهویه قوی یا ایجاد کوران در روز اول جوجه‌ها را دچار سرماخوردگی و ترشحات چشمی مهلک می‌کند.",
                proTip = "نوک چند جوجه را به آرامی در آب بزنید تا صدای قطره آب بقیه گله را سریعاً به سمت آبخوری جذب کند."
            ),
            GuideStepDetail(
                id = "step_p3",
                stepNumber = 3,
                phase = GuidePhase.PHASE_3,
                title = "مدیریت هفتگی رشد، فضا و تغییر جیره",
                summary = "تنظیم پلکانی دما، رطوبت و گسترش سالن متناسب با رشد سریع عضلانی و اسکلتی پرنده.",
                detailedPoints = listOf(
                    "کاهش دمای سالن به میزان ۲.۵ الی ۳ درجه در هر هفته تا رسیدن به دمای آسایش محیطی (۲۱ درجه در هفته چهارم).",
                    "باز کردن تدریجی گاردها از روز دهم و آزاد کردن فضای کامل سالن.",
                    "تغییر دان استارتر به رشد حتماً طی ۴ روز به صورت تدریجی انجام شود (روز اول ۷۵٪ دان قبلی + ۲۵٪ دان جدید و...).",
                    "نصب دماسنج و رطوبت‌سنج در ارتفاع قد پرنده، نه در ارتفاع سر پرورشدهنده!"
                ),
                warningNote = "تغییر ناگهانی فرمول دان باعث اسهال اسمزی شدید و افت رشد ۲ هفته‌ای گله می‌شود.",
                proTip = "هر هفته ۵ درصد گله را وزن‌کشی کنید؛ اگر اختلاف وزن بالاست، تعداد دانخوری‌ها را افزایش دهید تا پرندگان ضعیف‌تر هم فرصت غذا خوردن داشته باشند."
            ),
            GuideStepDetail(
                id = "step_p4",
                stepNumber = 4,
                phase = GuidePhase.PHASE_4,
                title = "اصول جیره‌نویسی، مصرف آب و خوراک‌های ممنوعه",
                summary = "خوراک بیش از ۷۰ درصد هزینه فارم است؛ ترکیب متوازن ویتامین، اسید آمینه و مواد معدنی ضامن سلامت است.",
                detailedPoints = listOf(
                    "میزان مصرف آب همواره حدود ۱.۸ تا ۲.۲ برابر وزن دان مصرفی روزانه پرنده است.",
                    "در دوره تخم‌گذاری، نیاز کلسیم به ۳.۸ تا ۴.۲ درصد افزایش می‌یابد که با صدف معدنی یا کربنات کلسیم تأمین می‌شود.",
                    "شستشوی روزانه کاسه آبخوری‌ها و رسوب‌زدایی دوره‌ای لوله‌ها با اسید استیک رقیق یا پرکلرین.",
                    "ذخیره دان در انبار خشک، دارای پالت چوبی با فاصله ۲۰ سانتی‌متر از دیوار و کف برای ممانعت از رطوبت."
                ),
                warningNote = "نان خشک کپک‌زده عامل تولید سم مهلک آفلاتوکسین و تخریب کبد و مرگ خاموش است؛ استفاده از آن اکیداً ممنوع است.",
                proTip = "بعدازظهرها صدف درشت در اختیار مرغ تخم‌گذار بگذارید تا هنگام تشکیل پوسته در شب، کلسیم آزاد در روده موجود باشد."
            ),
            GuideStepDetail(
                id = "step_p5",
                stepNumber = 5,
                phase = GuidePhase.PHASE_5,
                title = "امنیت زیستی، بهداشت فارم و برنامه واکسیناسیون",
                summary = "پیشگیری همیشه ۱۰ برابر ارزان‌تر از درمان است؛ قرنطینه و رعایت اصول بهداشتی تنها راه ایمن ماندن است.",
                detailedPoints = listOf(
                    "حوضچه ضدعفونی چکمه در ورودی سالن و تعویض کفش و لباس اختصاصی برای هر سالن.",
                    "واکسیناسیون علیه نیوکاسل، برونشیت و گامبورو بر اساس تقویم دامپزشکی منطقه.",
                    "آب واکسیناسیون باید بدون کلر باشد (استفاده از شیرخشک بدون چربی به مقدار ۲ گرم در لیتر برای خنثی‌سازی کلر).",
                    "جداسازی سریع پرنده کزکرده در اتاق قرنطینه دور از سایرین."
                ),
                warningNote = "هرگز خودسرانه آنتی‌بیوتیک انسانی در آب گله نریزید؛ این کار باعث مسمومیت کلیه، فلجی و مقاومت دارویی پایدار می‌شود.",
                proTip = "واکسن زنده را در ساعات خنک صبح مصرف کنید و پیش از آن ۲ ساعت آبخوری‌ها را خالی بگذارید تا تشنه شوند و ظرف ۲ ساعت همه بنوشند."
            ),
            GuideStepDetail(
                id = "step_p6",
                stepNumber = 6,
                phase = GuidePhase.PHASE_6,
                title = "تخم‌گذاری، نوردهی، آشیانه‌سازی و جمع‌آوری",
                summary = "حفظ راندمان بالای تولید با مدیریت یکنواخت نور، آرامش محیطی و آشیانه‌های تمیز و تاریک.",
                detailedPoints = listOf(
                    "تحریک نوری از سن ۱۸ هفتگی با افزایش تدریجی روشنایی به ۱۶ ساعت در روز تثبیت شود.",
                    "تأمین یک لانه تخم‌گذاری مجزا به ازای هر ۴ الی ۵ قطعه مرغ در بخش‌های دنج و کم‌نور سالن.",
                    "جمع‌آوری تخم‌مرغ حداقل ۳ الی ۴ بار در طول روز برای جلوگیری از شکستگی و آلودگی مدفوعی.",
                    "نگهداری تخم‌مرغ در دمای ۱۴ تا ۱۶ درجه و رطوبت ۷۰٪ (هرگز تخم خوراکی یا نطفه‌دار را نشویید)."
                ),
                warningNote = "حتی نیم‌ساعت قطع نور یا استرس صوتی شدید می‌تواند گله تخم‌گذار را به لک (پرریزی زودهنگام) برده و تولید را صفر کند.",
                proTip = "اگر تخم‌مرغ‌ها نوک‌زده می‌شوند، سریعاً مقداری سنگ صدف، کلسیم و نمک جیره را چک کنید و تخم مصنوعی سرامیکی در لانه قرار دهید."
            )
        )
    }

    /**
     * Retrieves the specific growth row corresponding to the bird's exact age in days.
     */
    fun getScheduleRowForAge(species: BirdSpecies, ageDays: Int): GrowthScheduleRow {
        val schedule = getGrowthSchedule(species)
        if (schedule.isEmpty()) return getGrowthSchedule(BirdSpecies.CHICKEN).first()

        return when (species) {
            BirdSpecies.CHICKEN -> when {
                ageDays <= 7 -> schedule.getOrElse(0) { schedule.first() }
                ageDays <= 14 -> schedule.getOrElse(1) { schedule.first() }
                ageDays <= 21 -> schedule.getOrElse(2) { schedule.first() }
                ageDays <= 28 -> schedule.getOrElse(3) { schedule.first() }
                ageDays <= 56 -> schedule.getOrElse(4) { schedule.first() }
                ageDays <= 112 -> schedule.getOrElse(5) { schedule.last() }
                else -> schedule.getOrElse(6) { schedule.last() }
            }
            BirdSpecies.TURKEY -> when {
                ageDays <= 7 -> schedule.getOrElse(0) { schedule.first() }
                ageDays <= 14 -> schedule.getOrElse(1) { schedule.first() }
                ageDays <= 28 -> schedule.getOrElse(2) { schedule.first() }
                ageDays <= 56 -> schedule.getOrElse(3) { schedule.first() }
                else -> schedule.getOrElse(4) { schedule.last() }
            }
            BirdSpecies.QUAIL -> when {
                ageDays <= 3 -> schedule.getOrElse(0) { schedule.first() }
                ageDays <= 14 -> schedule.getOrElse(1) { schedule.first() }
                ageDays <= 28 -> schedule.getOrElse(2) { schedule.first() }
                else -> schedule.getOrElse(3) { schedule.last() }
            }
            BirdSpecies.DUCK -> when {
                ageDays <= 7 -> schedule.getOrElse(0) { schedule.first() }
                ageDays <= 21 -> schedule.getOrElse(1) { schedule.first() }
                else -> schedule.getOrElse(2) { schedule.last() }
            }
            BirdSpecies.GOOSE -> when {
                ageDays <= 14 -> schedule.getOrElse(0) { schedule.first() }
                ageDays <= 35 -> schedule.getOrElse(1) { schedule.first() }
                else -> schedule.getOrElse(2) { schedule.last() }
            }
            else -> schedule.first()
        }
    }

    /**
     * Calculates ventilation requirements based on bird count and average weight.
     */
    fun calculateVentilation(birdCount: Int, averageWeightKg: Double): VentilationEstimate {
        val count = birdCount.coerceAtLeast(1)
        val weight = averageWeightKg.coerceAtLeast(0.05)
        val totalWeight = count * weight
        return VentilationEstimate(
            minWinterM3h = totalWeight * 0.8,
            maxSummerM3h = totalWeight * 5.0,
            totalFlockWeightKg = totalWeight
        )
    }

    /**
     * Estimates required feeders, drinkers, hall space, and nesting boxes for a flock.
     */
    fun calculateEquipment(species: BirdSpecies, birdCount: Int): EquipmentEstimate {
        val count = birdCount.coerceAtLeast(1)
        val birdsPerFeeder = when (species) {
            BirdSpecies.CHICKEN -> 25
            BirdSpecies.TURKEY -> 15
            BirdSpecies.QUAIL -> 50
            BirdSpecies.DUCK -> 20
            BirdSpecies.GOOSE -> 15
            BirdSpecies.GUINEA_FOWL -> 25
        }
        val birdsPerDrinker = when (species) {
            BirdSpecies.CHICKEN -> 12
            BirdSpecies.TURKEY -> 8
            BirdSpecies.QUAIL -> 20
            BirdSpecies.DUCK -> 10
            BirdSpecies.GOOSE -> 8
            BirdSpecies.GUINEA_FOWL -> 12
        }
        val birdsPerM2 = when (species) {
            BirdSpecies.CHICKEN -> 10.0
            BirdSpecies.TURKEY -> 3.5
            BirdSpecies.QUAIL -> 30.0
            BirdSpecies.DUCK -> 4.5
            BirdSpecies.GOOSE -> 2.5
            BirdSpecies.GUINEA_FOWL -> 8.0
        }
        val feeders = ((count + birdsPerFeeder - 1) / birdsPerFeeder).coerceAtLeast(1)
        val drinkers = ((count + birdsPerDrinker - 1) / birdsPerDrinker).coerceAtLeast(1)
        val floorArea = (count / birdsPerM2)
        val nests = if (species == BirdSpecies.CHICKEN || species == BirdSpecies.QUAIL || species == BirdSpecies.DUCK) {
            ((count + 4) / 5).coerceAtLeast(1)
        } else null
        val broodingArea = floorArea * 0.35

        return EquipmentEstimate(
            feedersCount = feeders,
            drinkersCount = drinkers,
            floorAreaM2 = floorArea,
            nestsCount = nests,
            broodingAreaM2 = broodingArea
        )
    }

    /**
     * Calculates key incubation timeline milestones from start epoch timestamp.
     */
    fun calculateIncubationMilestones(species: BirdSpecies, startEpochMillis: Long): List<IncubationMilestone> {
        val data = getIncubationData(species)
        val totalDays = data.totalIncubationDays
        val currentMillis = System.currentTimeMillis()

        val candling1Day = when (species) {
            BirdSpecies.QUAIL -> 5
            BirdSpecies.CHICKEN -> 7
            BirdSpecies.DUCK -> 8
            BirdSpecies.TURKEY -> 8
            BirdSpecies.GOOSE -> 9
            else -> 7
        }
        val candling2Day = when (species) {
            BirdSpecies.QUAIL -> 11
            BirdSpecies.CHICKEN -> 14
            BirdSpecies.DUCK -> 21
            BirdSpecies.TURKEY -> 21
            BirdSpecies.GOOSE -> 22
            else -> 14
        }
        val hatcherStartDay = (totalDays - 3).coerceAtLeast(1)

        val milestones = listOf(
            Triple(0, "بارگذاری تخم در ستر", "تنظیم دما و رطوبت اولیه و فعال‌سازی چرخش خودکار."),
            Triple(candling1Day, "کندلینگ اول (نوربینی)", "تشخیص تشکیل شبکه عنکبوتی رگ‌ها و حذف تخم‌های بی‌نطفه."),
            Triple(candling2Day, "کندلینگ دوم", "پایش جنین و خط هوایی؛ خروج تخم‌های با تلفات جنینی."),
            Triple(hatcherStartDay, "انتقال به هچر و توقف چرخش", "انتقال به سبد هچ، خاموش کردن چرخش و افزایش رطوبت به بالای ۷۰٪."),
            Triple(totalDays, "هچ و خروج جوجه‌ها", "تکمیل خروج جوجه‌ها، خشک شدن کرک‌ها و انتقال به برودر.")
        )

        return milestones.map { (dayOffset, title, desc) ->
            val targetEpoch = startEpochMillis + (dayOffset * 86400000L)
            val targetJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate(targetEpoch).toString()
            val daysRemaining = ((targetEpoch - currentMillis) / 86400000L).toInt()
            val isCompleted = currentMillis > (targetEpoch + 43200000L)
            val isCurrent = !isCompleted && daysRemaining in 0..1

            IncubationMilestone(
                title = title,
                jalaliDateStr = targetJalali,
                daysFromStart = dayOffset,
                daysRemaining = daysRemaining,
                description = desc,
                isCompleted = isCompleted,
                isCurrent = isCurrent
            )
        }
    }
}

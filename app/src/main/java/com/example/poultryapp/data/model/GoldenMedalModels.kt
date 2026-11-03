package com.example.poultryapp.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.poultryapp.R

/**
 * دسته‌بندی مفهومی مدال‌های افتخار آکادمی پرورش طلایی
 */
enum class MedalCategory {
    SIDE_QUEST,     // مهار بحران‌ها و مأموریت‌های فرعی (مقدماتی)
    NOVICE_CYCLE,   // اتمام دوره مقدماتی پرورش (مقدماتی)
    MASTERY_3STAR,  // دوره تکمیلی و استادی ۳ ستاره کامل (مقدماتی)
    PRO_PATHOLOGY,  // تخصص پاتولوژی و امنیت زیستی بالینی (حرفه‌ای - ماژول ۱ و ۲)
    PRO_CLIMATE,    // مهندسی اقلیم، تهویه و فارم صنعتی (حرفه‌ای - ماژول ۳ و ۴)
    PRO_GRANDMASTER // دانشنامه عالی و بورد استادی کل (حرفه‌ای - ماژول ۵ و کل بورد)
}

/**
 * مدل داده‌ای مدال افتخار با مشخصات بصری و شرایط بازگشایی
 */
data class GoldenMedal(
    val id: String,
    val birdType: GoldenBirdType,
    val seasonTier: GameSeasonTier,
    val category: MedalCategory,
    val titleFa: String,
    val subtitleFa: String,
    val conceptDescriptionFa: String,
    val iconBadge: String,
    val tierTitleFa: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null,
    val progressCurrent: Int,
    val progressMax: Int,
    val requirementDescriptionFa: String,
    @get:DrawableRes val imageRes: Int = R.drawable.medal_novice_graduate
) {
    val progressPercentage: Int
        get() = if (progressMax > 0) ((progressCurrent.toFloat() / progressMax.toFloat()) * 100f).toInt().coerceIn(0, 100) else 0
}

/**
 * سازنده اطلاعات مدال‌های هر گونه پرنده
 */
object GoldenMedalCatalog {

    fun generateMedals(
        birdType: GoldenBirdType,
        seasonTier: GameSeasonTier,
        completedStagesCount: Int,
        totalStagesCount: Int,
        threeStarStagesCount: Int,
        completedSideQuestsCount: Int,
        totalSideQuestsCount: Int,
        completedProStagesCount: Int,
        threeStarProStagesCount: Int,
        unlockedDates: Map<String, String> = emptyMap()
    ): List<GoldenMedal> {
        val medals = mutableListOf<GoldenMedal>()

        if (seasonTier == GameSeasonTier.NOVICE) {
            // ۱. مدال مهار بحران‌های فرعی (سپر زمردی سلامت گله)
            val sqId = "medal_sq_${birdType.id}"
            val sqMax = if (totalSideQuestsCount > 0) totalSideQuestsCount else 1
            val sqCur = completedSideQuestsCount.coerceAtMost(sqMax)
            val sqUnlocked = totalSideQuestsCount > 0 && completedSideQuestsCount >= totalSideQuestsCount

            medals.add(
                GoldenMedal(
                    id = sqId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.NOVICE,
                    category = MedalCategory.SIDE_QUEST,
                    titleFa = "مدال مدافع سلامت و مهار بحران‌ها",
                    subtitleFa = "مهار کامل تمام سایدکوئست‌ها و سناریوهای شیوع بیماری",
                    conceptDescriptionFa = "این مدال به پرورش‌دهنده‌ای اعطا می‌شود که تمامی چالش‌های اورژانسی، بیماری‌های حاد و حوادث سالن ${birdType.titleFa} را با موفقیت مهار کرده و از تلفات گله جلوگیری نموده است.",
                    iconBadge = "🛡️",
                    tierTitleFa = "مدال نقره زمردین دامپزشکی",
                    primaryColor = Color(0xFF10B981),
                    secondaryColor = Color(0xFF064E3B),
                    isUnlocked = sqUnlocked,
                    unlockedDate = unlockedDates[sqId],
                    progressCurrent = sqCur,
                    progressMax = sqMax,
                    requirementDescriptionFa = "مهار تمام ${totalSideQuestsCount} مأموریت فرعی و بحرانی ${birdType.titleFa}",
                    imageRes = R.drawable.medal_side_quest_defense
                )
            )

            // ۲. مدال اتمام دوره مقدماتی پرورش (نشان زرین فارم)
            val novId = "medal_novice_${birdType.id}"
            val novMax = if (totalStagesCount > 0) totalStagesCount else 1
            val novCur = completedStagesCount.coerceAtMost(novMax)
            val novUnlocked = completedStagesCount >= novMax

            medals.add(
                GoldenMedal(
                    id = novId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.NOVICE,
                    category = MedalCategory.NOVICE_CYCLE,
                    titleFa = "مدال فارغ‌التحصیلی پرورش ${birdType.titleFa}",
                    subtitleFa = "تکمیل تمام مراحل از جوجه‌ریزی تا پایان دوره",
                    conceptDescriptionFa = "نشان رسمی احراز مهارت در پرورش استاندارد، مدیریت گله در سنین مختلف، دان‌خوری، نوردهی و اصول پایه تولید موفق ${birdType.titleFa}.",
                    iconBadge = "🐣",
                    tierTitleFa = "مدال زرین دوره مقدماتی",
                    primaryColor = Color(0xFFF59E0B),
                    secondaryColor = Color(0xFF1E3A8A),
                    isUnlocked = novUnlocked,
                    unlockedDate = unlockedDates[novId],
                    progressCurrent = novCur,
                    progressMax = novMax,
                    requirementDescriptionFa = "اتمام موفقیت‌آمیز تمام ${totalStagesCount} مرحله دوره مقدماتی",
                    imageRes = R.drawable.medal_novice_graduate
                )
            )

            // ۳. مدال مراحل تکمیلی و استادی ۳ ستاره (تاج طلایی امپریال)
            val masterId = "medal_mastery_${birdType.id}"
            val masterMax = if (totalStagesCount > 0) totalStagesCount else 1
            val masterCur = threeStarStagesCount.coerceAtMost(masterMax)
            val masterUnlocked = threeStarStagesCount >= masterMax

            medals.add(
                GoldenMedal(
                    id = masterId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.NOVICE,
                    category = MedalCategory.MASTERY_3STAR,
                    titleFa = "نشان زرین استادی ۳ ستاره و دوره تکمیلی",
                    subtitleFa = "تسلط صددرصدی و بیست از بیست بر تمام آزمون‌های پرورشی",
                    conceptDescriptionFa = "بالاترین نشان دوره مقدماتی برای کارآفرینانی که بدون خطا و با کسب ۳ ستاره کامل در تمام مراحل، شایستگی علمی و اجرایی خود را در سطح ممتاز به اثبات رسانده‌اند.",
                    iconBadge = "👑",
                    tierTitleFa = "مدال طلای ۲۴ عیار امپریال",
                    primaryColor = Color(0xFFFBBF24),
                    secondaryColor = Color(0xFF831843),
                    isUnlocked = masterUnlocked,
                    unlockedDate = unlockedDates[masterId],
                    progressCurrent = masterCur,
                    progressMax = masterMax,
                    requirementDescriptionFa = "کسب ۳ ستاره کامل در تمام ${totalStagesCount} مرحله دوره",
                    imageRes = R.drawable.medal_mastery_crown
                )
            )
        } else {
            // بخش حرفه‌ای (Professional Season - ۲۵ مرحله بورد)
            // ۱. مدال تخصص پاتولوژی و امنیت زیستی بالینی
            val pathId = "medal_pro_pathology_${birdType.id}"
            val pathCur = minOf(completedProStagesCount, 10)
            val pathUnlocked = completedProStagesCount >= 10

            medals.add(
                GoldenMedal(
                    id = pathId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.PROFESSIONAL,
                    category = MedalCategory.PRO_PATHOLOGY,
                    titleFa = "مدال تخصصی پاتولوژی و امنیت زیستی",
                    subtitleFa = "تسلط بر کالبدگشایی، فارماکولوژی و پایش آزمایشگاهی گله",
                    conceptDescriptionFa = "احراز شایستگی در تفریق بیماری‌های عفونی حاد، کالبدگشایی تشخیصی و طراحی برنامه‌های پیشرفته واکسیناسیون مگافارم ${birdType.titleFa}.",
                    iconBadge = "🔬",
                    tierTitleFa = "مدال پلاتینیوم پاتولوژی بالینی",
                    primaryColor = Color(0xFF06B6D4),
                    secondaryColor = Color(0xFF0F172A),
                    isUnlocked = pathUnlocked,
                    unlockedDate = unlockedDates[pathId],
                    progressCurrent = pathCur,
                    progressMax = 10,
                    requirementDescriptionFa = "اتمام ۱۰ مرحله نخست بورد تخصصی (پاتولوژی و بیوسکیوریتی)",
                    imageRes = R.drawable.medal_pro_pathology
                )
            )

            // ۲. مدال مهندسی اقلیم، تهویه و مگافارم
            val climId = "medal_pro_climate_${birdType.id}"
            val climCur = minOf(maxOf(completedProStagesCount - 10, 0), 10)
            val climUnlocked = completedProStagesCount >= 20

            medals.add(
                GoldenMedal(
                    id = climId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.PROFESSIONAL,
                    category = MedalCategory.PRO_CLIMATE,
                    titleFa = "مدال مهندسی اقلیم و سالن‌های هوشمند",
                    subtitleFa = "مدیریت پیشرفته تهویه تونلی، حداقل تهویه و فرمولاسیون جیره",
                    conceptDescriptionFa = "تسلط بر کنترل میکرواقلیم، محاسبات دبی هوا، سنسورهای آمونیاک و رطوبت و بهینه‌سازی ضریب تبدیل دان در مقیاس صنعتی.",
                    iconBadge = "⚙️",
                    tierTitleFa = "مدال تیتانیوم مهندسی مگافارم",
                    primaryColor = Color(0xFFF97316),
                    secondaryColor = Color(0xFF4C1D95),
                    isUnlocked = climUnlocked,
                    unlockedDate = unlockedDates[climId],
                    progressCurrent = climCur,
                    progressMax = 10,
                    requirementDescriptionFa = "اتمام مراحل ۱۱ تا ۲۰ بورد تخصصی (تهویه، تاسیسات و تغذیه)",
                    imageRes = R.drawable.medal_pro_climate
                )
            )

            // ۳. دانشنامه عالی و نشان بورد استادی کل
            val grandId = "medal_pro_grandmaster_${birdType.id}"
            val grandCur = threeStarProStagesCount.coerceAtMost(25)
            val grandUnlocked = threeStarProStagesCount >= 25

            medals.add(
                GoldenMedal(
                    id = grandId,
                    birdType = birdType,
                    seasonTier = GameSeasonTier.PROFESSIONAL,
                    category = MedalCategory.PRO_GRANDMASTER,
                    titleFa = "دانشنامه عالی و بورد استادی کل",
                    subtitleFa = "کسب ۳ ستاره در تمامی ۲۵ مرحله فوق‌تخصصی پرنده",
                    conceptDescriptionFa = "بالاترین عنوان آکادمیک صنعت پرورش طیور، ویژه مشاوران ارشد و مدیران تولید مگافارم‌های پرورشی کشور با احراز نمره کامل در تمام آزمون‌های فوق‌تخصصی.",
                    iconBadge = "💎",
                    tierTitleFa = "مدال الماس امپریال بورد عالی",
                    primaryColor = Color(0xFFA855F7),
                    secondaryColor = Color(0xFF854D0E),
                    isUnlocked = grandUnlocked,
                    unlockedDate = unlockedDates[grandId],
                    progressCurrent = grandCur,
                    progressMax = 25,
                    requirementDescriptionFa = "۳ ستاره کردن تمام ۲۵ مرحله بورد فوق‌تخصصی ${birdType.titleFa}",
                    imageRes = R.drawable.medal_pro_grandmaster
                )
            )
        }

        return medals
    }
}

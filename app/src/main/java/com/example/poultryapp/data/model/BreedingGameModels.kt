package com.example.poultryapp.data.model

import androidx.compose.ui.graphics.Color
import com.example.poultryapp.R

/**
 * مراحل تکاملی سن جوجه تا مرغ تخم‌گذار بالغ
 */
enum class ChickenGrowthStage(
    val titleFa: String,
    val ageDaysRange: String,
    val iconEmoji: String,
    val avatarTint: Long = 0xFFFFD54F
) {
    CHICK_DAY_1("جوجه ۱ روزه", "روز ۱", "🐣", 0xFFFFE082),
    CHICK_WEEK_2("جوجه هفته دوم", "روز ۸ تا ۱۴", "🐥", 0xFFFFD54F),
    CHICK_WEEK_3("جوجه هفته سوم", "روز ۱۵ تا ۲۱", "🐤", 0xFFFFCA28),
    GROWER_WEEK_4("نیمچه هفته چهارم", "روز ۲۲ تا ۲۸", "🐔", 0xFFFFB300),
    GROWER_WEEK_6("نیمچه هفته ششم", "روز ۳۶ تا ۴۲", "🐔", 0xFFFFA000),
    GROWER_WEEK_8("نیمچه هفته هشتم", "روز ۵۰ تا ۵۶", "🐔", 0xFFFF8F00),
    PULLET_WEEK_12("پولِت هفته دوازدهم", "روز ۷۸ تا ۸۴", "🐓", 0xFFE65100),
    PULLET_WEEK_16("پولِت آماده بلوغ", "روز ۱۰۵ تا ۱۱۲", "🐓", 0xFFBF360C),
    ADULT_HEN("مرغ تخم‌گذار بالغ", "هفته ۲۰ به بعد", "🏆", 0xFFFF6F00)
}

/**
 * انواع چالش‌های تعاملی درون بازی
 */
sealed class StageChallenge {
    abstract val questionTitleFa: String
    abstract val explanationFa: String

    /**
     * چالش سناریوی واقعی رفتارشناسی و بحران سالن
     */
    data class Scenario(
        override val questionTitleFa: String,
        val situationDescriptionFa: String,
        val optionsFa: List<String>,
        val correctOptionIndex: Int,
        override val explanationFa: String,
        val visualHintEmoji: String = "⚠️"
    ) : StageChallenge()

    /**
     * چالش تنظیم پارامترها و ابزارهای سالن با اسلایدر (دما، رطوبت، نور، جیره)
     */
    data class SliderControl(
        override val questionTitleFa: String,
        val parameterNameFa: String,
        val unitFa: String,
        val minValue: Float,
        val maxValue: Float,
        val optimalMin: Float,
        val optimalMax: Float,
        val initialValue: Float,
        override val explanationFa: String
    ) : StageChallenge()

    /**
     * کوییز سرعتی علمی و کاربردی
     */
    data class QuickQuiz(
        override val questionTitleFa: String,
        val questionFa: String,
        val optionsFa: List<String>,
        val correctOptionIndex: Int,
        override val explanationFa: String
    ) : StageChallenge()
}

/**
 * کارت طلایی دانش که با پیروزی در مرحله آزاد می‌شود
 */
data class KnowledgeCard(
    val id: String,
    val stageId: Int,
    val titleFa: String,
    val subtitleFa: String,
    val categoryFa: String = "دانش تخصصی",
    val keyRulesFa: List<String> = emptyList(),
    val proTipFa: String = "",
    val proTipsFa: List<String> = emptyList(),
    val practicalFormulaFa: String? = null,
    val keyTakewayFa: String? = null
) {
    val effectiveRules: List<String>
        get() = if (keyRulesFa.isNotEmpty()) keyRulesFa
                else if (!keyTakewayFa.isNullOrBlank()) listOf(keyTakewayFa)
                else emptyList()

    val effectiveProTip: String
        get() = if (proTipFa.isNotBlank()) proTipFa
                else if (proTipsFa.isNotEmpty()) proTipsFa.joinToString("\n• ", prefix = if (proTipsFa.size > 1) "• " else "")
                else ""
}

/**
 * اطلاعات کامل یک مرحله بازی در نقشه
 */
data class GameStage(
    val id: Int,
    val stageNumber: Int,
    val titleFa: String,
    val ageLabelFa: String,
    val daysRangeFa: String,
    val growthStage: ChickenGrowthStage,
    val missionBriefFa: String,
    val challenges: List<StageChallenge>,
    val goldenCard: KnowledgeCard,
    val isFinalMilestone: Boolean = false
)

/**
 * وضعیت پیشرفت کاربر در یک مرحله
 */
data class StageProgress(
    val stageId: Int,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val stars: Int = 0, // 0 to 3
    val highScore: Int = 0,
    val isCardUnlocked: Boolean = false
)

/**
 * نمایه کلی پیشرفت مزرعه‌دار
 */
data class FarmerGameProfile(
    val totalStars: Int = 0,
    val maxPossibleStars: Int = 45,
    val currentActiveStageId: Int = 1,
    val completedStagesCount: Int = 0,
    val farmerRankTitleFa: String = "مزرعه‌دار نوپا",
    val unlockedCardsCount: Int = 0
)

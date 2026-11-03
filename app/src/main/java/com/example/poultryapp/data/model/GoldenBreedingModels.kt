package com.example.poultryapp.data.model

import com.example.poultryapp.R

/**
 * گونه‌های پرندگان در آکادمی پرورش طلایی
 */
enum class GoldenBirdType(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val cycleDescriptionFa: String,
    val imageRes: Int,
    val avatarEmoji: String,
    val totalStages: Int
) {
    LAYER_HEN(
        id = "layer_hen",
        titleFa = "مرغ بومی و تخم‌گذار",
        subtitleFa = "پرورش مرغ تخم‌گذار محلی و دورگ",
        cycleDescriptionFa = "چرخه کامل تا اوج تخم‌گذاری و مدیریت گله",
        imageRes = R.drawable.bird_layer_chicken,
        avatarEmoji = "🐓",
        totalStages = 50
    ),
    BROILER(
        id = "broiler",
        titleFa = "مرغ گوشتی پروار",
        subtitleFa = "پرواربندی فشرده ۴۵ روزه راس و کاب",
        cycleDescriptionFa = "دوره فشرده ۶ هفته تا وزن بازاریابی",
        imageRes = R.drawable.bird_broiler_chicken,
        avatarEmoji = "🐔",
        totalStages = 45
    ),
    QUAIL(
        id = "quail",
        titleFa = "بلدرچین مینیاتوری",
        subtitleFa = "پرورش در فضای کم، قفس و تخم‌گذاری سریع",
        cycleDescriptionFa = "هچ سریع و شروع تخم‌گذاری در ۴۰ روزگی",
        imageRes = R.drawable.bird_quail,
        avatarEmoji = "🥚",
        totalStages = 42
    ),
    TURKEY(
        id = "turkey",
        titleFa = "بوقلمون پرواری",
        subtitleFa = "پرورش پرواری سنگین‌وزن در سالن و مرتع",
        cycleDescriptionFa = "دوره ۱۸ هفته تا رسیدن به وزن ایده‌آل",
        imageRes = R.drawable.bird_turkey,
        avatarEmoji = "🦃",
        totalStages = 44
    ),
    WATERFOWL(
        id = "waterfowl",
        titleFa = "اردک و غاز آبزی",
        subtitleFa = "پرورش مقاوم در استخر، باغ و چراگری",
        cycleDescriptionFa = "مقاوم‌ترین پرندگان در برابر شرایط محیطی",
        imageRes = R.drawable.bird_duck,
        avatarEmoji = "🦆",
        totalStages = 42
    ),
    COCKATIEL(
        id = "cockatiel",
        titleFa = "عروس هلندی (طوطی‌سانان)",
        subtitleFa = "تکثیر، تغذیه سرلاکی و نگهداری خانگی",
        cycleDescriptionFa = "پرطرفدارترین طوطی زینتی، از جفت‌گیری تا دان‌خوری جوجه",
        imageRes = R.drawable.bird_cockatiel,
        avatarEmoji = "🦜",
        totalStages = 42
    ),
    PARTRIDGE(
        id = "partridge",
        titleFa = "کبک چوکار کوهستانی",
        subtitleFa = "پرورش گوشتی و تخم‌گذار پربازده",
        cycleDescriptionFa = "سازگار با اقلیم کوهستانی، جیره پرپروتئین و تخم‌گذاری فصلی",
        imageRes = R.drawable.bird_partridge,
        avatarEmoji = "⛰️",
        totalStages = 42
    ),
    OSTRICH(
        id = "ostrich",
        titleFa = "شترمرغ آفریقایی",
        subtitleFa = "پرورش تجاری غول‌پیکر در مزرعه و مرتع",
        cycleDescriptionFa = "دوره ۱۰ ماهه پروار، گوشت قرمز سالم و چرم گرانبها",
        imageRes = R.drawable.bird_ostrich,
        avatarEmoji = "🏜️",
        totalStages = 42
    ),
    GUINEA_FOWL(
        id = "guinea_fowl",
        titleFa = "مرغ شاخدار (گینی)",
        subtitleFa = "پرورش در باغات، شکارچی آفات و تخم نفوذناپذیر",
        cycleDescriptionFa = "پرنده نیمه‌وحشی و مقاوم، گوشت پرورشی لوکس و کنترل طبیعی کنه",
        imageRes = R.drawable.bird_guineafowl,
        avatarEmoji = "🪶",
        totalStages = 42
    ),
    CANARY(
        id = "canary",
        titleFa = "قناری آوازخوان و فرم",
        subtitleFa = "پرورش خانگی، نغمه‌سرایی، جفت‌گیری و رنگ‌دانه‌ها",
        cycleDescriptionFa = "فتوپریود و آمادگی، تغذیه سرلاکی، لک‌تولی و نغمه‌سرایی طلایی",
        imageRes = R.drawable.bird_canary,
        avatarEmoji = "🐤",
        totalStages = 42
    ),
    PIGEON(
        id = "pigeon",
        titleFa = "کبوتر پروازی و اصیل",
        subtitleFa = "مدیریت کبوترخانه، پروازهای مسافتی و جلد کردن",
        cycleDescriptionFa = "تولید شیر چینه دان، جهت‌یابی مغناطیسی و بازگشت به لوفت",
        imageRes = R.drawable.bird_pigeon,
        avatarEmoji = "🕊️",
        totalStages = 42
    ),
    MUSCOVY_DUCK(
        id = "muscovy_duck",
        titleFa = "اردک مسکویی (اسرائیلی)",
        subtitleFa = "پرورش اردک بیصدا، کم‌چرب و چراگر گوشتی",
        cycleDescriptionFa = "جوجه‌کشی ۳۵ روزه، پروار سنگین و گوشت بدون چربی",
        imageRes = R.drawable.bird_muscovy_duck,
        avatarEmoji = "🦆",
        totalStages = 42
    )
}

/**
 * انواع چالش‌های تعاملی درون بازی (حداقل ۳ چالش در هر مرحله)
 */
sealed class GoldenChallenge {
    abstract val questionTitleFa: String
    abstract val explanationFa: String

    /**
     * ۱. سناریوی بحران و رفتارشناسی سالن
     */
    data class Scenario(
        override val questionTitleFa: String,
        val situationDescriptionFa: String,
        val optionsFa: List<String>,
        val correctOptionIndex: Int,
        override val explanationFa: String,
        val hintEmoji: String = "⚠️"
    ) : GoldenChallenge()

    /**
     * ۲. کنترل دقیق پارامترهای سالن با اسلایدر (پذیرش مقادیر مرزی)
     */
    data class SliderControl(
        override val questionTitleFa: String,
        val parameterNameFa: String,
        val unitFa: String,
        val minValue: Float,
        val maxValue: Float,
        val optimalMin: Float, // عدد مرزی پایین (مثلا ۳۲)
        val optimalMax: Float, // عدد مرزی بالا (مثلا ۳۵)
        val initialValue: Float,
        override val explanationFa: String
    ) : GoldenChallenge()

    /**
     * ۳. کوئیز علمی تخصصی جیره، واکسن، دارو و بهداشت
     */
    data class QuickQuiz(
        override val questionTitleFa: String,
        val situationDescriptionFa: String = "",
        val symptomDescriptionFa: String = "",
        val questionFa: String = situationDescriptionFa.ifEmpty { symptomDescriptionFa },
        val optionsFa: List<String>,
        val correctOptionIndex: Int,
        override val explanationFa: String
    ) : GoldenChallenge()

    /**
     * ۴. بررسی تصویری و عیب‌یابی بالینی سالن (مدفوع، بستر، پرها)
     */
    data class Diagnostics(
        override val questionTitleFa: String,
        val symptomDescriptionFa: String,
        val clinicalSignEmoji: String,
        val optionsFa: List<String>,
        val correctOptionIndex: Int,
        override val explanationFa: String
    ) : GoldenChallenge()
}

/**
 * اطلاعات کامل یک مرحله بازی برای یک پرنده
 */
data class GoldenStage(
    val id: Int,
    val birdType: GoldenBirdType,
    val stageNumber: Int,
    val titleFa: String,
    val ageLabelFa: String,
    val daysRangeFa: String,
    val avatarEmoji: String,
    val missionBriefFa: String,
    val challenges: List<GoldenChallenge>, // حداقل ۳ چالش
    val goldenCard: KnowledgeCard = KnowledgeCard(
        id = "card_${birdType.id}_$id",
        stageId = id,
        titleFa = titleFa,
        subtitleFa = ageLabelFa,
        keyTakewayFa = missionBriefFa,
        practicalFormulaFa = daysRangeFa,
        proTipFa = "نکته کلیدی بورد استادی فارم"
    ),
    val isFinalMilestone: Boolean = false
)

/**
 * دسته‌بندی مأموریت‌های جانبی و اضطراری
 */
enum class SideQuestCategory(
    val titleFa: String,
    val badgeEmoji: String,
    val colorHex: Long
) {
    DISEASE_OUTBREAK("بحران شیوع و همه‌گیری بیماری", "🦠", 0xFFDC2626),
    ENVIRONMENTAL_HAZARD("اورژانس محیطی و تأسیسات سالن", "🚨", 0xFFD97706),
    NUTRITION_METABOLIC("اختلالات تغذیه و جیره بحرانی", "🌾", 0xFF2563EB),
    BEHAVIORAL_CANBALISM("بحران رفتاری و کانیبالیسم", "⚔️", 0xFF7C3AED),
    INCUBATION_HATCHERY("کالیبراسیون انکوباتور و هچری", "🐣", 0xFF059669)
}

/**
 * مأموریت جانبی (ساید کوئست) برای هر پرنده
 */
data class GoldenSideQuest(
    val id: String,
    val birdType: GoldenBirdType,
    val titleFa: String,
    val subtitleFa: String,
    val targetAgeFa: String,
    val category: SideQuestCategory,
    val unlockStageRequired: Int,
    val emergencyScenarioFa: String,
    val challenges: List<GoldenChallenge>,
    val rewardBadgeTitleFa: String,
    val rewardBadgeIcon: String,
    val rewardCoins: Int = 100
)

/**
 * جوایز و تجهیزات طلایی باز شده مزرعه در مراحل عطف
 */
data class GoldenFarmReward(
    val id: String,
    val birdType: GoldenBirdType,
    val unlockStageNumber: Int,
    val titleFa: String,
    val categoryFa: String,
    val descriptionFa: String,
    val iconEmoji: String,
    val goldCoinsReward: Int,
    val technicalImpactFa: String,
    val imageRes: Int? = null
)

/**
 * نگاشت هوشمند تصویر دوبعدی متناسب با نوع جایزه
 */
fun GoldenFarmReward.resolveImageRes(): Int {
    if (imageRes != null && imageRes != 0) return imageRes
    return when {
        id.contains("syringe") || id.contains("handfeed") -> R.drawable.reward_handfeed_syringe
        id.contains("brooder") || id.contains("infrared") || id.contains("lamp") -> R.drawable.reward_infrared_brooder
        id.contains("scale") || id.contains("weigh") -> R.drawable.reward_precision_scale
        id.contains("feeder") || id.contains("dish") || id.contains("feed") -> R.drawable.reward_rotary_feeder
        id.contains("drinker") || id.contains("nipple") || id.contains("water") -> R.drawable.reward_nipple_drinker
        id.contains("climate") || id.contains("sensor") || id.contains("inlet") || id.contains("fogger") -> R.drawable.reward_smart_climate
        id.contains("incubator") || id.contains("hatch") -> R.drawable.reward_master_incubator
        id.contains("trophy") || unlockStageNumber >= 12 -> R.drawable.game_trophy_master
        else -> R.drawable.game_medal_expert
    }
}

/**
 * نمایه پیشرفت پرنده منتخب با پشتیبانی از سکه‌ها و جوایز
 */
data class BirdProgressSummary(
    val birdType: GoldenBirdType,
    val totalStars: Int = 0,
    val maxPossibleStars: Int,
    val sideQuestStars: Int = 0,
    val maxPossibleSideQuestStars: Int = 0,
    val currentActiveStageNumber: Int = 1,
    val completedStagesCount: Int = 0,
    val threeStarStagesCount: Int = 0,
    val unlockedCardsCount: Int = 0,
    val completedSideQuestsCount: Int = 0,
    val unlockedRewardsCount: Int = 0,
    val farmCoins: Int = 0,
    val masteryTitleFa: String = "نوپا در پرورش"
)

/**
 * فصل‌ها و سطوح دشواری بازی
 */
enum class GameSeasonTier(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val badgeEmoji: String
) {
    NOVICE(
        id = "novice",
        titleFa = "فصل ۱: دوره مقدماتی و استاندارد",
        subtitleFa = "آشنایی با اصول پایه‌ای پرورش و مدیریت فارم",
        badgeEmoji = "🌱"
    ),
    PROFESSIONAL(
        id = "professional",
        titleFa = "فصل ۲: بورد فوق‌تخصصی و استادی",
        subtitleFa = "چالش‌های سنگین دامپزشکی، کالبدگشایی و مدیریت بحران",
        badgeEmoji = "👑"
    )
}

/**
 * لوح زرین افتخار و دانشنامه رسمی بورد تخصصی (تالار افتخارات)
 */
data class GoldenPlaque(
    val id: String,
    val birdType: GoldenBirdType?,
    val titleFa: String,
    val specialtyFa: String,
    val descriptionFa: String,
    val criteriaFa: String,
    val imageRes: Int,
    val badgeEmoji: String = "📜",
    val isUnlocked: Boolean = false,
    val unlockedDateFa: String? = null
)



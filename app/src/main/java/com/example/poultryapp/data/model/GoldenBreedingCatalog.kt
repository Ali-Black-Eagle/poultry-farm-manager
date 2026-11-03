package com.example.poultryapp.data.model

import com.example.poultryapp.R
import com.example.poultryapp.data.model.novice_supp_stages.*

/**
 * کاتالوگ جامع دوره‌های آموزشی و مراحل پرورش طلایی برای تمام پرندگان
 * پشتیبانی کامل از فصل ۱ (مقدماتی)، فصل ۲ (بورد فوق‌تخصصی) و تالار افتخارات
 */
object GoldenBreedingCatalog {

    fun getStagesForBird(
        birdType: GoldenBirdType,
        seasonTier: GameSeasonTier = GameSeasonTier.NOVICE
    ): List<GoldenStage> {
        return if (seasonTier == GameSeasonTier.PROFESSIONAL) {
            getProStagesForBird(birdType)
        } else {
            when (birdType) {
                GoldenBirdType.LAYER_HEN -> LayerHenStagesData.stages + LayerHenSuppStages.stages
                GoldenBirdType.BROILER -> BroilerStagesData.stages + BroilerSuppStages.stages
                GoldenBirdType.QUAIL -> QuailStagesData.stages + QuailSuppStages.stages
                GoldenBirdType.TURKEY -> TurkeyStagesData.stages + TurkeySuppStages.stages
                GoldenBirdType.WATERFOWL -> WaterfowlStagesData.stages + WaterfowlSuppStages.stages
                GoldenBirdType.COCKATIEL -> CockatielStagesData.stages + CockatielSuppStages.stages
                GoldenBirdType.PARTRIDGE -> PartridgeStagesData.stages + PartridgeSuppStages.stages
                GoldenBirdType.OSTRICH -> OstrichStagesData.stages + OstrichSuppStages.stages
                GoldenBirdType.GUINEA_FOWL -> GuineaFowlStagesData.stages + GuineaFowlSuppStages.stages
                GoldenBirdType.CANARY -> CanaryStagesData.stages + CanarySuppStages.stages
                GoldenBirdType.PIGEON -> PigeonStagesData.stages + PigeonSuppStages.stages
                GoldenBirdType.MUSCOVY_DUCK -> MuscovyDuckStagesData.stages + MuscovyDuckSuppStages.stages
            }
        }
    }

    fun getProStagesForBird(birdType: GoldenBirdType): List<GoldenStage> {
        return ProfessionalStagesData.getProStagesForBird(birdType)
    }

    fun getStageById(
        birdType: GoldenBirdType,
        stageId: Int,
        seasonTier: GameSeasonTier = GameSeasonTier.NOVICE
    ): GoldenStage? {
        return getStagesForBird(birdType, seasonTier).find { it.id == stageId }
    }

    fun getSideQuestsForBird(birdType: GoldenBirdType): List<GoldenSideQuest> {
        return SideQuestsData.getForBird(birdType)
    }

    fun getRewardsForBird(birdType: GoldenBirdType): List<GoldenFarmReward> {
        return RewardsCatalogData.getForBird(birdType)
    }

    /**
     * کاتالوگ ۱۰ لوح زرین تالار افتخارات (۹ لوح اختصاصی گونه‌ها + دانشنامه استاد اعظم)
     */
    val allPlaques: List<GoldenPlaque> = listOf(
        GoldenPlaque(
            id = "plaque_layer_hen",
            birdType = GoldenBirdType.LAYER_HEN,
            titleFa = "لوح زرین استادی مرغ بومی و تخم‌گذار",
            specialtyFa = "بورد عالی فیزیولوژی تخم‌گذاری و پاتولوژی پولت",
            descriptionFa = "اعطا شده به پاس تسلط کامل بر موازنه الکترولیتی dEB، سرولوژی تیتراسیون واکسیناسیون و مهار اپیدمی‌های قفس صنعتی.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای مرغ تخم‌گذار",
            imageRes = R.drawable.bird_layer_chicken,
            badgeEmoji = "🐓"
        ),
        GoldenPlaque(
            id = "plaque_broiler",
            birdType = GoldenBirdType.BROILER,
            titleFa = "لوح زرین مهندسی مرغ گوشتی صنعتی",
            specialtyFa = "بورد عالی تهویه تونلی، ضریب تبدیل FCR و سنسوریک مگافارم",
            descriptionFa = "اعطا شده به پاس مدیریت پیشرفته فشار استاتیک پاسکال، پیشگیری از آسیت ریوی و بهینه‌سازی فرمولاسیون فینیشر.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای مرغ گوشتی",
            imageRes = R.drawable.bird_broiler_chicken,
            badgeEmoji = "🐔"
        ),
        GoldenPlaque(
            id = "plaque_quail",
            birdType = GoldenBirdType.QUAIL,
            titleFa = "لوح زرین پرورش فشرده بلدرچین ژاپنی",
            specialtyFa = "بورد عالی انکوباسیون ۱۷ روزه، آنتریت اولسراتیو و سیستم باتری",
            descriptionFa = "اعطا شده به پاس تسلط بر ناهنجاری‌های هچ بلدرچین، پاتوژنز کلستریدیوم کولینوم و راندمان اوج تخم‌گذاری مینیاتوری.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای بلدرچین",
            imageRes = R.drawable.bird_quail,
            badgeEmoji = "🥚"
        ),
        GoldenPlaque(
            id = "plaque_turkey",
            birdType = GoldenBirdType.TURKEY,
            titleFa = "لوح زرین سرپرستی بوقلمون سنگین‌وزن",
            specialtyFa = "بورد عالی تلقیح مصنوعی، رینوتراکئیت ART و پروزیس اسکلتی",
            descriptionFa = "اعطا شده به پاس رهبری گله‌های گوشتی سنگین، مهار هیستومونیازیس و اجرای برنامه نوردهی تحریک تخم‌گذاری.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای بوقلمون",
            imageRes = R.drawable.bird_turkey,
            badgeEmoji = "🦃"
        ),
        GoldenPlaque(
            id = "plaque_waterfowl",
            birdType = GoldenBirdType.WATERFOWL,
            titleFa = "لوح زرین هیدروبیولوژی اردک و غاز آبزی",
            specialtyFa = "بورد عالی طاعون اردک DVE، بهداشت استخر و غدد چربی پرورشی",
            descriptionFa = "اعطا شده به پاس کنترل بهداشتی آب‌های راکد، مهار هپاتیت ویروسی جوجه اردک و تغذیه ارگانیک چراگاهی.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای پرندگان آبزی",
            imageRes = R.drawable.bird_duck,
            badgeEmoji = "🦆"
        ),
        GoldenPlaque(
            id = "plaque_cockatiel",
            birdType = GoldenBirdType.COCKATIEL,
            titleFa = "لوح زرین تکثیر تخصصی عروس هلندی",
            specialtyFa = "بورد عالی نئوناتولوژی سرلاکی، ژنتیک رنگ و کلامیدیوز پرندگان",
            descriptionFa = "اعطا شده به پاس مراقبت‌های انکوباتور زینتی، مهار چینه‌دان ترش و پیشگیری از تب طوطی زئونوتیک.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای طوطی‌سانان",
            imageRes = R.drawable.bird_cockatiel,
            badgeEmoji = "🦜"
        ),
        GoldenPlaque(
            id = "plaque_partridge",
            birdType = GoldenBirdType.PARTRIDGE,
            titleFa = "لوح زرین اکولوژی و پرورش کبک کوهستانی",
            specialtyFa = "بورد عالی انگل نای گیپ‌ورم، کانیبالیسم نوری و پروتئین کرامبل",
            descriptionFa = "اعطا شده به پاس مهار سینگاموس تراکئا، کنترل اکوسیستم ارتفاعات و تثبیت ضریب تبدیل گوشت شکاری کبک.",
            criteriaFa = "اتمام موفق تمام مراحل بورد فوق‌حرفه‌ای کبک چوکار",
            imageRes = R.drawable.bird_partridge,
            badgeEmoji = "⛰️"
        ),
        GoldenPlaque(
            id = "plaque_ostrich",
            birdType = GoldenBirdType.OSTRICH,
            titleFa = "لوح زرین پرورش کلان شترمرغ آفریقایی",
            specialtyFa = "بورد عالی جراحی سنگدان، انکوباسیون ۴۲ روزه و فرآوری چرم الماس",
            descriptionFa = "اعطا شده به پاس مدیریت انسداد پیش‌معده، رطوبت پایین هچری کویری و صید ایمن راتیتی‌ها بدون میوپاتی.",
            criteriaFa = "اتمام موفق تمام ۲۵ مرحله بورد فوق‌حرفه‌ای شترمرغ",
            imageRes = R.drawable.bird_ostrich,
            badgeEmoji = "🏜️"
        ),
        GoldenPlaque(
            id = "plaque_guinea_fowl",
            birdType = GoldenBirdType.GUINEA_FOWL,
            titleFa = "لوح زرین زیست‌شناسی مرغ شاخدار گینی",
            specialtyFa = "بورد عالی شکار بیولوژیک آفات، نئوناتولوژی کیت‌ها و پوسته فوق‌سخت",
            descriptionFa = "اعطا شده به پاس کنترل کنه‌های باغات، تغذیه پروتئین بالای کیت‌ها و مدیریت آشیانه‌های مخفی در مرتع.",
            criteriaFa = "اتمام موفق تمام ۲۵ مرحله بورد فوق‌حرفه‌ای مرغ شاخدار",
            imageRes = R.drawable.bird_guineafowl,
            badgeEmoji = "🪶"
        ),
        GoldenPlaque(
            id = "plaque_canary",
            birdType = GoldenBirdType.CANARY,
            titleFa = "لوح زرین استادی تکثیر و آواز قناری",
            specialtyFa = "بورد عالی ژنتیک لیپوکروم، آناتومی سیروکس و تغذیه الوان",
            descriptionFa = "اعطا شده به پاس تسلط بر نغمه‌سرایی فاخر، فتوپریود تحریکی گنادها، پیشگیری از کنه تراشه و پرورش حرفه‌ای نژادهای فرم و ملودی.",
            criteriaFa = "اتمام موفق تمام ۲۵ مرحله بورد فوق‌حرفه‌ای قناری",
            imageRes = R.drawable.bird_canary,
            badgeEmoji = "🐤"
        ),
        GoldenPlaque(
            id = "plaque_pigeon",
            birdType = GoldenBirdType.PIGEON,
            titleFa = "لوح زرین ناوبری ژئومغناطیسی و لوفت کبوتر",
            specialtyFa = "بورد عالی مگنتورسپشن کریپتوکروم، فیزیولوژی پرواز مسافتی و کروپ‌میلک",
            descriptionFa = "اعطا شده به پاس تسلط بر تریکومونیازیس کبوترخانه‌ها، بیومکانیک پرواز بادهای مخالف، مدیریت آبله جلدی و آموزش بازگشت هوایی به آشیانه.",
            criteriaFa = "اتمام موفق تمام ۲۵ مرحله بورد فوق‌حرفه‌ای کبوتر",
            imageRes = R.drawable.bird_pigeon,
            badgeEmoji = "🕊️"
        ),
        GoldenPlaque(
            id = "plaque_muscovy_duck",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            titleFa = "لوح زرین پرورش تخصصی و استادی اردک مسکویی",
            specialtyFa = "بورد عالی انکوباسیون ۳۵ روزه، پاتولوژی درزی، مهار پرواز و تولید گوشت لخم",
            descriptionFa = "اعطا شده به پاس تسلط کامل بر بیوفیزیک پوسته مومی تخم مسکویی، مهار پارووویروس درزی، پرورش ارگانیک در باغات، مهار آیرودینامیک پرواز و فرآوری گوشت قرمز کم‌چرب.",
            criteriaFa = "اتمام موفق تمام ۲۵ مرحله بورد فوق‌حرفه‌ای اردک مسکویی",
            imageRes = R.drawable.bird_muscovy_duck,
            badgeEmoji = "🦆"
        ),
        GoldenPlaque(
            id = "plaque_grand_master",
            birdType = null,
            titleFa = "دانشنامه زرین استاد اعظم علوم پرندگان",
            specialtyFa = "عالی‌ترین درجه بورد دامپزشکی و پرورش تمامی ۱۲ گونه پرندگان آکادمی",
            descriptionFa = "بالاترین نشان افتخار آکادمی پرورش طلایی؛ اعطا شده به پرورش‌دهنده‌ای که تمام مراحل فوق‌تخصصی را با افتخار به پایان رسانده است.",
            criteriaFa = "کسب تمامی ۱۲ لوح زرین استادی پرندگان آکادمی",
            imageRes = R.drawable.game_trophy_master,
            badgeEmoji = "👑"
        )
    )
}

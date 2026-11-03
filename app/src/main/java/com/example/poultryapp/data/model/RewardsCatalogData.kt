package com.example.poultryapp.data.model

import com.example.poultryapp.R

/**
 * کاتالوگ جوایز و تجهیزات طلایی فارم برای هر ۸ پرنده در نقاط عطف مراحل
 */
object RewardsCatalogData {

    private val allRewards: List<GoldenFarmReward> = listOf(
        // ۱. جوایز مرغ بومی و تخم‌گذار
        GoldenFarmReward(
            id = "rew_layer_3",
            birdType = GoldenBirdType.LAYER_HEN,
            unlockStageNumber = 3,
            titleFa = "دماسنج و رطوبت‌سنج دیجیتال کالیبره سوئیسی",
            categoryFa = "تجهیزات پایش محیطی",
            descriptionFa = "سنسور با دقت ۰.۱ درجه برای سنجش پیوسته دمای بستر و آسایش حرارتی جوجه‌ها.",
            iconEmoji = "🌡️",
            goldCoinsReward = 60,
            technicalImpactFa = "کاهش ۵۰٪ خطای تنظیم دمای مادر مصنوعی و پیشگیری از سرمازدگی اولیه"
        ),
        GoldenFarmReward(
            id = "rew_layer_6",
            birdType = GoldenBirdType.LAYER_HEN,
            unlockStageNumber = 6,
            titleFa = "مه‌پاش فوگر نازل استیل کم‌فشار سالن",
            categoryFa = "تجهیزات اقلیمی",
            descriptionFa = "سیستم مه‌ساز هوشمند جهت ایجاد رطوبت ۶۵٪ در هفته‌های ابتدایی و خنک‌سازی تابستان.",
            iconEmoji = "💧",
            goldCoinsReward = 120,
            technicalImpactFa = "جلوگیری از گرد و خاک بستر و حفظ سلامت کیسه‌های هوایی ریه گله"
        ),
        GoldenFarmReward(
            id = "rew_layer_10",
            birdType = GoldenBirdType.LAYER_HEN,
            unlockStageNumber = 10,
            titleFa = "دانخوری اتوماتیک بشقابی ضد ریزش و هدررفت",
            categoryFa = "تجهیزات تغذیه هوشمند",
            descriptionFa = "بشقاب‌های با طراحی آرگونومیک برای تنظیم دقیق ارتفاع و جلوگیری از پرت دان.",
            iconEmoji = "🥣",
            goldCoinsReward = 200,
            technicalImpactFa = "صرفه‌جویی ۱۰ درصدی در مصرف دان و یکنواختی کامل وزن گله"
        ),
        GoldenFarmReward(
            id = "rew_layer_15",
            birdType = GoldenBirdType.LAYER_HEN,
            unlockStageNumber = 15,
            titleFa = "لوکس‌متر طیوری و کنترلر نور اتوماتیک دیمردار",
            categoryFa = "تجهیزات نوردهی تخصصی",
            descriptionFa = "تنظیم طیف نوری گرم (۲۷۰۰ کلوین) و شدت دقیق نور برای تحریک تخم‌گذاری بدون استرس کانیبالیسم.",
            iconEmoji = "💡",
            goldCoinsReward = 320,
            technicalImpactFa = "افزایش ۱۲ درصدی نرخ تولید تخم‌مرغ و کنترل تهاجم و پرکنی"
        ),
        GoldenFarmReward(
            id = "rew_layer_20",
            birdType = GoldenBirdType.LAYER_HEN,
            unlockStageNumber = 20,
            titleFa = "تندیس استادی و کاپ طلایی قهرمانی پرورش مرغ تخم‌گذار",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "نشان عالی سرپرست ارشد فارم و اعطای گواهی‌نامه رسمی استادی پرورش مرغ تخم‌گذار.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "باز شدن تمامی ترفندهای سری تولید صنعتی و گواهی‌نامه مهارت"
        ),

        // ۲. جوایز مرغ گوشتی پروار
        GoldenFarmReward(
            id = "rew_broiler_3",
            birdType = GoldenBirdType.BROILER,
            unlockStageNumber = 3,
            titleFa = "ترازوی دیجیتال آویز گله با دقت گرمی",
            categoryFa = "پایش رشد و وزن",
            descriptionFa = "ترازوی کالیبره با سبد اختصاصی توزین سریع جوجه‌ها برای رسم دقیق منحنی رشد استاندارد راس ۳۰۸.",
            iconEmoji = "⚖️",
            goldCoinsReward = 60,
            technicalImpactFa = "محاسبه دقیق یکنواختی (Uniformity) و ضریب تبدیل هفتگی"
        ),
        GoldenFarmReward(
            id = "rew_broiler_6",
            birdType = GoldenBirdType.BROILER,
            unlockStageNumber = 6,
            titleFa = "سنسور فشار استاتیک و کنترلر اینلت‌های تهویه",
            categoryFa = "تجهیزات تهویه تونلی",
            descriptionFa = "کنترل میلیمتر به میلیمتر بازشوهای هوای ورودی برای پرتاب هوای تازه به تاج سالن بدون کوران روی جوجه.",
            iconEmoji = "💨",
            goldCoinsReward = 140,
            technicalImpactFa = "جلوگیری از افت اکسیژن و پیشگیری قاطع از سندرم آسیت"
        ),
        GoldenFarmReward(
            id = "rew_broiler_10",
            birdType = GoldenBirdType.BROILER,
            unlockStageNumber = 10,
            titleFa = "سیستم پایش آبخوری نیپل ضد چکه با مدیگاتور تزریق دارو",
            categoryFa = "آبرسانی و بهداشت",
            descriptionFa = "لوله نیپل استیل با فشار شکن دقیق و دوزاتر تزریق واکسن و اسیدی‌فایر به آب مصرفی.",
            iconEmoji = "🚰",
            goldCoinsReward = 220,
            technicalImpactFa = "حفظ خشکی کامل بستر و ریشه‌کنی تاول سینه و پد پای پرنده"
        ),
        GoldenFarmReward(
            id = "rew_broiler_15",
            birdType = GoldenBirdType.BROILER,
            unlockStageNumber = 15,
            titleFa = "کاپ زرین استاد پرواربندی و مدیریت فارم گوشتی",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "نشان قهرمانی پرورش ۴۵ روزه با رکورد ضریب تبدیل کمتر از ۱.۵۵ و تلفات زیر ۲.۵٪.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "کسب بالاترین نشان اقتصادی و گواهی‌نامه طلایی پرورش گوشتی"
        ),

        // ۳. جوایز بلدرچین مینیاتوری
        GoldenFarmReward(
            id = "rew_quail_3",
            birdType = GoldenBirdType.QUAIL,
            unlockStageNumber = 3,
            titleFa = "مادر مصنوعی تابشی مادون قرمز کم‌مصرف بلدرچین",
            categoryFa = "تجهیزات پرورش مینیاتوری",
            descriptionFa = "تابش گرمای ملایم بدون ایجاد نور زننده برای آرامش جوجه بلدرچین‌های ۲ روزه.",
            iconEmoji = "🔥",
            goldCoinsReward = 60,
            technicalImpactFa = "کاهش تلفات هفته اول به زیر ۱٪"
        ),
        GoldenFarmReward(
            id = "rew_quail_6",
            birdType = GoldenBirdType.QUAIL,
            unlockStageNumber = 6,
            titleFa = "قفس گالوانیزه ضد زنگ طبقاتی با شیب شیار جمع‌آوری تخم",
            categoryFa = "تجهیزات جایگاه",
            descriptionFa = "قفس صنعتی مجهز به توری کف با شیب استاندارد ۷ درجه برای هدایت خودکار تخم بدون ترک خوردگی.",
            iconEmoji = "🏢",
            goldCoinsReward = 150,
            technicalImpactFa = "بهداشت ۱۰۰ درصدی تخم‌ها و عدم تماس با فضله"
        ),
        GoldenFarmReward(
            id = "rew_quail_12",
            birdType = GoldenBirdType.QUAIL,
            unlockStageNumber = 12,
            titleFa = "مدال طلایی و تندیس استادی پرورش سریع بلدرچین",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "دستیابی به اوج تخم‌گذاری در روز ۴۲ با نرخ تولید بالای ۸۵٪ در قفس صنعتی.",
            iconEmoji = "🏆",
            goldCoinsReward = 450,
            technicalImpactFa = "اعطای گواهی‌نامه رسمی تخصص بلدرچین و بازارپسندی محصول"
        ),

        // ۴. جوایز بوقلمون پرواری
        GoldenFarmReward(
            id = "rew_turkey_3",
            birdType = GoldenBirdType.TURKEY,
            unlockStageNumber = 3,
            titleFa = "حلقه برودر ضد استرس گوشه‌نشینی جوجه بوقلمون",
            categoryFa = "تجهیزات روزهای اول",
            descriptionFa = "حلقه مقوایی مدور با آبخوری‌های تیله‌دار رنگی برای آموزش سریع نوشیدن به پولت‌های تازه متولد.",
            iconEmoji = "🎯",
            goldCoinsReward = 70,
            technicalImpactFa = "ریشه‌کنی تلفات ناشی از تشنگی و نابلدی جوجه بوقلمون"
        ),
        GoldenFarmReward(
            id = "rew_turkey_7",
            birdType = GoldenBirdType.TURKEY,
            unlockStageNumber = 7,
            titleFa = "سیستم تهویه پرفشار ضد گرد و خاک و گاز آمونیاک",
            categoryFa = "بهداشت تنفس بوقلمون",
            descriptionFa = "فن‌های قدرتمند جهت خروج گازهای سنگین تولید شده از فضله پرنده سنگین‌وزن.",
            iconEmoji = "🌪️",
            goldCoinsReward = 160,
            technicalImpactFa = "حفظ شفافیت ریه و جلوگیری از تاول مفصل پاشنه"
        ),
        GoldenFarmReward(
            id = "rew_turkey_14",
            birdType = GoldenBirdType.TURKEY,
            unlockStageNumber = 14,
            titleFa = "کاپ طلایی و تندیس استاد پرورش بوقلمون فوق سنگین",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "پرورش بوقلمون‌های برنز و بیوتی با میانگین وزنی ۱۸ تا ۲۰ کیلوگرم در ۱۸ هفته.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "گواهی‌نامه کارشناس ارشد پرورش پرندگان سنگین‌وزن"
        ),

        // ۵. جوایز اردک و غاز
        GoldenFarmReward(
            id = "rew_waterfowl_3",
            birdType = GoldenBirdType.WATERFOWL,
            unlockStageNumber = 3,
            titleFa = "آبخوری حفاظ‌دار شیاردار ضد خیس‌کردن بستر",
            categoryFa = "بهداشت پرندگان آبزی",
            descriptionFa = "طراحی اختصاصی که اردک فقط سر را در آب فرو می‌برد و نمی‌تواند وارد آبخوری شود.",
            iconEmoji = "🦆",
            goldCoinsReward = 60,
            technicalImpactFa = "خشک ماندن ۷۰ درصدی بستر پوشال و جلوگیری از کوکسیدیوز"
        ),
        GoldenFarmReward(
            id = "rew_waterfowl_6",
            birdType = GoldenBirdType.WATERFOWL,
            unlockStageNumber = 6,
            titleFa = "حوضچه ضدعفونی و شناور زلال‌کننده جریان آب",
            categoryFa = "تجهیزات آبگیر آزاد",
            descriptionFa = "استخر کم‌عمق با پمپ تخلیه لجن جهت ضدعفونی مداوم آب و نظافت پرهای پرواز.",
            iconEmoji = "🏊",
            goldCoinsReward = 150,
            technicalImpactFa = "تحریک غده چربی پرها و آب‌بندی طبیعی بدن پرنده"
        ),
        GoldenFarmReward(
            id = "rew_waterfowl_12",
            birdType = GoldenBirdType.WATERFOWL,
            unlockStageNumber = 12,
            titleFa = "تندیس زرین استاد بوم‌شناسی و پرورش غاز و اردک",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "تولید همزمان گوشت ارگانیک در چراگاه و تخم‌مرغ غاز با بالاترین استانداردهای رفاه حیوانات.",
            iconEmoji = "🏆",
            goldCoinsReward = 450,
            technicalImpactFa = "اعطای دانشنامه پرورش پایدار و کم‌هزینه آبزیان",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۶. جوایز عروس هلندی (طوطی‌سانان)
        GoldenFarmReward(
            id = "rew_cockatiel_4",
            birdType = GoldenBirdType.COCKATIEL,
            unlockStageNumber = 4,
            titleFa = "کیت سرنگ سرلاک‌دهی سیلیکونی و دماسنج دیجیتال",
            categoryFa = "تجهیزات پرورش دستی جوجه",
            descriptionFa = "سرنگ ۱۰ سی‌سی با لوله سیلیکونی نرم و سنسور پایش دمای ۳۸.۵ درجه سرلاک.",
            iconEmoji = "🍼",
            goldCoinsReward = 120,
            technicalImpactFa = "ریشه‌کنی ۱۰۰ درصدی سوختگی چینه دان و استاز قارچی جوجه‌ها",
            imageRes = R.drawable.reward_handfeed_syringe
        ),
        GoldenFarmReward(
            id = "rew_cockatiel_8",
            birdType = GoldenBirdType.COCKATIEL,
            unlockStageNumber = 8,
            titleFa = "صندوقچه طلایی آشیانه و لانه چوبی استاندارد مکانیزه",
            categoryFa = "تجهیزات آشیانه و تکثیر",
            descriptionFa = "لانه چوبی ۳۰×۲۰×۲۰ با کف مقعر کاسه‌ای، پوشال چوب درشت و درب بازدید کشویی.",
            iconEmoji = "🪵",
            goldCoinsReward = 200,
            technicalImpactFa = "پیشگیری از شکستگی تخم‌ها و عارضه باز شدن پای جوجه‌ها",
            imageRes = R.drawable.game_chest_gold
        ),
        GoldenFarmReward(
            id = "rew_cockatiel_12",
            birdType = GoldenBirdType.COCKATIEL,
            unlockStageNumber = 12,
            titleFa = "کاپ زرین استادی تکثیر و پرورش طوطی‌سانان",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "نشان عالی پرورش‌دهنده طلایی عروس هلندی با ثبت رکورد هچ و جوجه‌پروری موفق.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "اعطای دانشنامه رسمی استادی در پرندگان زینتی و خانگی",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۷. جوایز کبک چوکار کوهستانی
        GoldenFarmReward(
            id = "rew_partridge_3",
            birdType = GoldenBirdType.PARTRIDGE,
            unlockStageNumber = 3,
            titleFa = "مادر مصنوعی تابشی مادون قرمز و توری کف محافظ پای کبک",
            categoryFa = "تجهیزات برودر و جایگاه",
            descriptionFa = "هیتر تابشی سرامیکی ۳۵ درجه به همراه توری مش‌ریز پلی‌اتیلن ضد لغزش.",
            iconEmoji = "🔥",
            goldCoinsReward = 100,
            technicalImpactFa = "مهار کامل فلجی پای جوجه کبک و حفظ حرارت کانون برودر",
            imageRes = R.drawable.reward_infrared_brooder
        ),
        GoldenFarmReward(
            id = "rew_partridge_6",
            birdType = GoldenBirdType.PARTRIDGE,
            unlockStageNumber = 6,
            titleFa = "نشان تخصصی عینک‌های محافظ و کیت مهار کانیبالیسم",
            categoryFa = "مدیریت رفتار و ضدپرکنی",
            descriptionFa = "عینک‌های پین‌دار سبک پلی‌اتیلن جهت کور کردن دید مستقیم حمله در سالن.",
            iconEmoji = "🕶️",
            goldCoinsReward = 220,
            technicalImpactFa = "کاهش تلفات ناشی از پرخاشگری گله از ۴۰٪ به صفر",
            imageRes = R.drawable.game_medal_expert
        ),
        GoldenFarmReward(
            id = "rew_partridge_12",
            birdType = GoldenBirdType.PARTRIDGE,
            unlockStageNumber = 12,
            titleFa = "کاپ طلایی قهرمانی پرورش صنعتی کبک چوکار",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "رسیدن به ضریب تبدیل اقتصادی زیر ۳.۸ و تولید لاشه لوکس ۵۰۰ گرمی.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "اعطای دیپلم افتخار مدیریت فارم صنعتی ماکیان خاص",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۸. جوایز شترمرغ آفریقایی
        GoldenFarmReward(
            id = "rew_ostrich_2",
            birdType = GoldenBirdType.OSTRICH,
            unlockStageNumber = 2,
            titleFa = "دستگاه انکوباتور تخصصی با رطوبت‌گیر ۲۵٪ تخم شترمرغ",
            categoryFa = "تجهیزات هچری مگافارم",
            descriptionFa = "سیستم هوشمند انکوباسیون با سنسورهای دیجیتال سوئیسی و پمپ رطوبت‌زدا.",
            iconEmoji = "🥚",
            goldCoinsReward = 150,
            technicalImpactFa = "تضمین افت وزن ۱۵ درصدی تخم و ثبت هچ بالای ۸۵٪",
            imageRes = R.drawable.reward_master_incubator
        ),
        GoldenFarmReward(
            id = "rew_ostrich_7",
            birdType = GoldenBirdType.OSTRICH,
            unlockStageNumber = 7,
            titleFa = "ترازوی دیجیتال دقیق آویز گله و آسیاب گریت سنگدان",
            categoryFa = "پایش رشد و هضم فیبر",
            descriptionFa = "ترازوی کالیبره وزن‌کشی پرنده و آخور توزیع سنگ‌ریزه‌های گرانیتی هاضمه.",
            iconEmoji = "⚖️",
            goldCoinsReward = 250,
            technicalImpactFa = "کنترل دقیق منحنی رشد و هضم بهینه یونجه در سکوم",
            imageRes = R.drawable.reward_precision_scale
        ),
        GoldenFarmReward(
            id = "rew_ostrich_12",
            birdType = GoldenBirdType.OSTRICH,
            unlockStageNumber = 12,
            titleFa = "تندیس زرین سرپرست ارشد مگافارم شترمرغ",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "دستیابی به وزن ۱۰۰ کیلوگرم در ۱۱ ماهگی و تولید چرم الماس و گوشت قرمز ارگانیک.",
            iconEmoji = "🏆",
            goldCoinsReward = 600,
            technicalImpactFa = "اعطای بالاترین نشان بازرگانی و مدیریت صنعت شترمرغ",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۹. جوایز مرغ شاخدار (گینی)
        GoldenFarmReward(
            id = "rew_guinea_4",
            birdType = GoldenBirdType.GUINEA_FOWL,
            unlockStageNumber = 4,
            titleFa = "نشیمنگاه مرتفع چوبی ضدشکارچی و سطل دان شرطی‌ساز",
            categoryFa = "تجهیزات خواب و امنیت شبانه",
            descriptionFa = "نردبان نشیمنگاه چوبی ۲ متری به همراه سطل دان آکوستیک جهت بازگرداندن قطعی گله به سالن در غروب.",
            iconEmoji = "🏡",
            goldCoinsReward = 120,
            technicalImpactFa = "ریشه‌کنی تلفات ناشی از حمله شغال و روباه در طول شب",
            imageRes = R.drawable.reward_rotary_feeder
        ),
        GoldenFarmReward(
            id = "rew_guinea_8",
            birdType = GoldenBirdType.GUINEA_FOWL,
            unlockStageNumber = 8,
            titleFa = "دانخوری مخروطی ضدپراکندگی با شبکه محافظتی استیل",
            categoryFa = "تجهیزات تغذیه ارگونومیک",
            descriptionFa = "دانخوری ناودانی با لبه‌های برگشته ضد بیرون‌ریزی دان و مخزن توزیع گریت سیلیسی سنگدان.",
            iconEmoji = "🌾",
            goldCoinsReward = 200,
            technicalImpactFa = "کاهش ۹۵ درصدی هدررفت دان و بهینه‌سازی ضریب تبدیل",
            imageRes = R.drawable.reward_nipple_drinker
        ),
        GoldenFarmReward(
            id = "rew_guinea_12",
            birdType = GoldenBirdType.GUINEA_FOWL,
            unlockStageNumber = 12,
            titleFa = "تندیس زرین استاد بوم‌شناسی و پرورش مرغ شاخدار",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "تسلط بر تکثیر، کنترل ارگانیک آفات باغات، تولید گوشت شکاری لوکس و سیستم آژیر هشدار مزرعه.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "اعطای مدال عالی تولید ارگانیک و مدیریت پرندگان نیمه‌وحشی",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۹. جوایز قناری
        GoldenFarmReward(
            id = "rew_canary_3",
            birdType = GoldenBirdType.CANARY,
            unlockStageNumber = 3,
            titleFa = "نی‌نشیمنگاه ارگونومیک با چوب طبیعی چنار و کاتر ناخن",
            categoryFa = "تجهیزات قفس و آسایش فیزیکی",
            descriptionFa = "چوب‌های نشیمن با قطرهای متغیر (۱۰ تا ۱۴ میلی‌متر) برای ورزش عضلات انگشتان و جلوگیری از پودودرماتیت و آرتروز قناری.",
            iconEmoji = "🌿",
            goldCoinsReward = 80,
            technicalImpactFa = "پیشگیری ۱۰۰ درصدی از میخچه پا و سایش طبیعی ناخن‌ها بدون جراحت"
        ),
        GoldenFarmReward(
            id = "rew_canary_6",
            birdType = GoldenBirdType.CANARY,
            unlockStageNumber = 6,
            titleFa = "کاسه لانه نمدی بیضی با الیاف نارگیل ضدکنه",
            categoryFa = "تجهیزات تکثیر و آشیانه‌سازی",
            descriptionFa = "کاسه لانه بهداشتی با تهویه زیرین و پد نمدی ضدآلرژی به همراه دیسپنسر نخ پنبه‌ای استریل.",
            iconEmoji = "🪹",
            goldCoinsReward = 150,
            technicalImpactFa = "بهبود گرمایش تخم‌ها و کاهش ۴۰ درصدی مرگ‌ومیر جنینی ناشی از چسبندگی پرها"
        ),
        GoldenFarmReward(
            id = "rew_canary_8",
            birdType = GoldenBirdType.CANARY,
            unlockStageNumber = 8,
            titleFa = "تایمر دیجیتال طلوع و غروب ملایم (Dimmer Photoperiod)",
            categoryFa = "کنترل فتوپریود و غدد درون‌ریز",
            descriptionFa = "شبیه‌ساز هوشمند نور خورشید با کاهش و افزایش نیم‌ساعته نور جهت جلوگیری از سکته نوری و تنظیم تولک.",
            iconEmoji = "🌅",
            goldCoinsReward = 240,
            technicalImpactFa = "تنظیم دقیق سیکل هورمونی تستوسترون و شروع نغمه‌سرایی و آمادگی جفت‌گیری"
        ),
        GoldenFarmReward(
            id = "rew_canary_12",
            birdType = GoldenBirdType.CANARY,
            unlockStageNumber = 12,
            titleFa = "تندیس بلورین استاد آواز و تکثیر نژادهای قناری",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "تسلط کامل بر ژنتیک رنگ، تغذیه سرلاکی، پرورش نژادهای رسمی و درمان تخصصی بیماری‌های تنفسی.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "کسب نشان استادی پرورش قناری و باز شدن دیپلم افتخار رسمی",
            imageRes = R.drawable.game_trophy_master
        ),

        // ۱۰. جوایز کبوتر
        GoldenFarmReward(
            id = "rew_pigeon_3",
            birdType = GoldenBirdType.PIGEON,
            unlockStageNumber = 3,
            titleFa = "دانخوری ناودانی دوطرفه با میله‌های غلطان ضدفرود",
            categoryFa = "بهداشت تغذیه لوفت",
            descriptionFa = "دانخوری لوفت کبوتر با حفاظ میله‌ای چرخان که مانع از فضله‌گذاری و فرود کبوتران روی دانه‌ها می‌شود.",
            iconEmoji = "🌾",
            goldCoinsReward = 80,
            technicalImpactFa = "حذف کامل آلودگی مدفوعی دان و پیشگیری ریشه‌ای از سالمونلا و کوکسیدیوز"
        ),
        GoldenFarmReward(
            id = "rew_pigeon_6",
            birdType = GoldenBirdType.PIGEON,
            unlockStageNumber = 6,
            titleFa = "آبخوری گنبدی مخروطی ضدآلودگی با سیستم ضدانجماد",
            categoryFa = "تجهیزات آبرسانی بهداشتی",
            descriptionFa = "طراحی کلاهکی نوک‌تیز که پرنده نمی‌تواند روی آن بنشیند و آب همیشه شفاف و عاری از گرد پر باقی می‌ماند.",
            iconEmoji = "🚰",
            goldCoinsReward = 150,
            technicalImpactFa = "کاهش ۹۰ درصدی انتقال تریکومونیازیس از طریق آب مصرفی گله"
        ),
        GoldenFarmReward(
            id = "rew_pigeon_8",
            birdType = GoldenBirdType.PIGEON,
            unlockStageNumber = 8,
            titleFa = "ساعت الکترونیکی ثبت زمان و تله ورود تک‌جهته (Sputnik Trap)",
            categoryFa = "تجهیزات تخصصی پرواز و رکوردگیری",
            descriptionFa = "سیستم ورودی اسپوتنیک با دریچه‌های آلومینیومی بدون بازگشت و آنتن اسکن چیپ پای کبوتر مسافتی.",
            iconEmoji = "⏱️",
            goldCoinsReward = 260,
            technicalImpactFa = "ثبت صدم ثانیه رکورد ورود به لوفت و ممانعت از خروج پرندگان خسته"
        ),
        GoldenFarmReward(
            id = "rew_pigeon_12",
            birdType = GoldenBirdType.PIGEON,
            unlockStageNumber = 12,
            titleFa = "تندیس زرین شاهین تیزپرواز و مربی برتر کبوتران مسافتی",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "تسلط بر ناوبری ژئومغناطیسی، آماده‌سازی پروازهای مسافت بلند ۵۰۰ کیلومتر، فیزیولوژی عضله سینه و مدیریت لوفت.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "اعطای کاپ عالی قهرمانی لوفت و بازگشایی لوح زرین مسابقات",
            imageRes = R.drawable.game_trophy_master
        ),
        // ۱۲. جوایز اردک مسکویی (Muscovy Duck)
        GoldenFarmReward(
            id = "rew_muscovy_3",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            unlockStageNumber = 3,
            titleFa = "سیستم مه‌پاش ولرم و انکوباتور دیجیتال ۳۵ روزه",
            categoryFa = "تجهیزات هچری و انکوباسیون",
            descriptionFa = "سنسور سنجش رطوبت و مه‌پاش اولتراسونیک با آب ولرم برای شبیه‌سازی دقیق رطوبت طبیعی پرهای مادر.",
            iconEmoji = "🥚",
            goldCoinsReward = 80,
            technicalImpactFa = "افزایش ۱۵ درصدی راندمان هچ تخم‌های مومی مسکویی و نرم شدن غشا"
        ),
        GoldenFarmReward(
            id = "rew_muscovy_6",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            unlockStageNumber = 6,
            titleFa = "نشیمنگاه‌های هوایی چوبی ضدلغزش با پایه‌های ضدشپشک",
            categoryFa = "تجهیزات رفاه و غریزه پرنده",
            descriptionFa = "سازه‌های چوبی صیقلی چندسطحی در ارتفاع استاندارد برای ارضای غریزه نشستن روی درخت.",
            iconEmoji = "🪵",
            goldCoinsReward = 140,
            technicalImpactFa = "حذف کامل کانیبالیسم شبانه و آرامش عضلات سینه نرهای سنگین"
        ),
        GoldenFarmReward(
            id = "rew_muscovy_8",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            unlockStageNumber = 8,
            titleFa = "سکوی توری گالوانیزه زهکشی زیر آبخوری و پمپ لجن‌کش",
            categoryFa = "مدیریت بهداشت بستر و آب",
            descriptionFa = "شبکه توری متال با کانال تخلیه فوری آب‌های شستشوی سر و منقار به مخزن سپتیک.",
            iconEmoji = "🚰",
            goldCoinsReward = 220,
            technicalImpactFa = "خشکی ۱۰۰ درصدی پوشال سالن و حذف بوی تند آمونیاک و تاول سینه"
        ),
        GoldenFarmReward(
            id = "rew_muscovy_10",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            unlockStageNumber = 10,
            titleFa = "دانخوری‌های مکانیزه ضد شلختگی با محافظ اسپینر استیل",
            categoryFa = "تجهیزات تغذیه هوشمند",
            descriptionFa = "طراحی آرگونومیک ناودانی متناسب با پوزه پهن اردک مسکویی با میله گردان ضد ورود فضولات.",
            iconEmoji = "🥣",
            goldCoinsReward = 320,
            technicalImpactFa = "صرفه‌جویی ۱۸ درصدی در مصرف خوراک و جلوگیری از ترشیدگی دان"
        ),
        GoldenFarmReward(
            id = "rew_muscovy_12",
            birdType = GoldenBirdType.MUSCOVY_DUCK,
            unlockStageNumber = 12,
            titleFa = "تندیس زرین استادی پرورش اردک مسکویی و خوتکای طلایی",
            categoryFa = "افتخار نهایی آکادمی",
            descriptionFa = "تسلط کامل بر انکوباسیون ۳۵ روزه، پرورش ارگانیک در باغات، مهار بیماری درزی و تولید گوشت لخم بدون چربی.",
            iconEmoji = "🏆",
            goldCoinsReward = 500,
            technicalImpactFa = "اعطای لوح زرین استادی مسکویی و ثبت نام در تالار افتخارات آکادمی",
            imageRes = R.drawable.game_trophy_master
        )
    )

    fun getForBird(birdType: GoldenBirdType): List<GoldenFarmReward> {
        return allRewards.filter { it.birdType == birdType }
    }

    fun getAll(): List<GoldenFarmReward> = allRewards
}

package com.example.poultryapp.data.model

object MedicationCatalog {

    val allMedications: List<PoultryMedication> = listOf(
        // ==========================================
        // ۱. آنتی‌بیوتیک‌ها (رویان دارو)
        // ==========================================
        PoultryMedication(
            id = "mega_enro_20",
            tradeNameFa = "مگا انرو® رویان (انروفلوکساسین ۲۰٪)",
            genericNameEn = "Mega Enro® 20% Oral Solution",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.QUAIL, BirdSpecies.DUCK),
            formAndPackaging = "محلول خوراکی، بطری‌های پلی‌اتیلنی ۱ لیتری و ۲۵۰ میلی‌لیتری",
            activeIngredientsFa = "هر میلی‌لیتر حاوی ۲۰۰ میلی‌گرم انروفلوکساسین خالص می‌باشد.",
            indicationsFa = "درمان تخصصی و قطعی عفونت‌های حاد باکتریایی دستگاه تنفس و گوارش ناشی از باکتری‌های گرم منفی و گرم مثبت شامل CRD کمپلکس، کلی‌باسیلوز (E.coli)، وبای مرغان (پاستورلوزیس)، سالمونلوزیس، سینوزیت عفونی بوقلمون و کوریزای عفونی.",
            dosageFa = "۰.۵ تا ۱ میلی‌لیتر در هر ۲ لیتر آب آشامیدنی (معادل ۱۰ میلی‌گرم ماده مؤثره به ازای هر کیلوگرم وزن زنده پرنده در روز). برای سالمونلوز دوز ۱ میلی‌لیتر در لیتر توصیه می‌شود.",
            durationDaysFa = "۳ تا ۵ روز متوالی (در سالمونلوز ۵ الی ۷ روز پیوسته).",
            withdrawalPeriodFa = "گوشت مرغ: ۵ روز قبل از کشتار؛ گوشت بوقلمون: ۸ روز. در گله‌های مرغ تخمگذار تجاری مصرف‌کننده تخم‌مرغ خوراکی انسان منع مصرف دارد.",
            contraindicationsFa = "از مصرف همزمان با داروهای حاوی یون‌های آلومینیوم، کلسیم، منیزیم، آهن و تتراسایکلین‌ها به علت تشکیل کمپلکس نامحلول و افت شدید جذب پرهیز شود.",
            storageAndPrecautionsFa = "در دمای ۱۵ تا ۲۵ درجه سانتی‌گراد، دور از تابش مستقیم آفتاب و یخ‌زدگی نگهداری شود. آب حاوی دارو روزانه تازه تهیه گردد."
        ),
        PoultryMedication(
            id = "enrofloxacin_10_rooyan",
            tradeNameFa = "انروفلوکساسین ۱۰٪ رویان",
            genericNameEn = "Enrofloxacin 10% Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.QUAIL),
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "هر میلی‌لیتر حاوی ۱۰۰ میلی‌گرم انروفلوکساسین است.",
            indicationsFa = "کنترل و درمان بیماری‌های عفونی باکتریایی تنفسی، عفونت‌های کیسه‌های هوایی، کلی‌سپتی‌سمی، کوریزای عفونی و سینوزیت.",
            dosageFa = "۱ میلی‌لیتر در هر لیتر آب آشامیدنی به مدت ۳ تا ۵ روز متوالی.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت: ۹ روز قبل از کشتار.",
            contraindicationsFa = "همراه با مکمل‌های غلیظ کلسیمی و آنتی‌اسیدها مصرف نشود.",
            storageAndPrecautionsFa = "در جای خشک، خنک و دور از نور نگهداری شود."
        ),
        PoultryMedication(
            id = "aprasol_50",
            tradeNameFa = "آپراسول رویان® (آپرامایسین ۵۰٪)",
            genericNameEn = "Aprasol® 50% Water Soluble Powder",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN),
            formAndPackaging = "پودر محلول در آب، ساشه ۱۰۰ و ۵۰۰ گرمی",
            activeIngredientsFa = "هر گرم حاوی ۵۰۰ میلی‌گرم آپرامایسین سولفات (آمینوگلیکوزید وسیع‌الطیف با اثر باکتریوسیدال قوی) می‌باشد.",
            indicationsFa = "درمان سریع و مؤثر کلی‌باسیلوزیس (عفونت‌های گوارشی و سپتی‌سمی ناشی از اشریشیا کلی)، سالمونلوزیس و انتریت‌های باکتریایی در جوجه‌های گوشتی و پولت‌ها.",
            dosageFa = "۰.۵ تا ۱ گرم در هر ۲ تا ۴ لیتر آب آشامیدنی (معادل ۲۰ تا ۴۰ میلی‌گرم بر کیلوگرم وزن زنده در روز).",
            durationDaysFa = "۵ تا ۷ روز متوالی.",
            withdrawalPeriodFa = "گوشت: ۷ روز قبل از کشتار.",
            contraindicationsFa = "با سایر آمینوگلیکوزیدها (استرپتومایسین یا نئومایسین) همزمان تجویز نشود.",
            storageAndPrecautionsFa = "در دمای زیر ۲۵ درجه، در ظرف دربسته و دور از رطوبت نگهداری گردد."
        ),
        PoultryMedication(
            id = "chlortetrasin_50",
            tradeNameFa = "کلرتتراسین رویان® (کلرتتراسایکلین ۵۰٪)",
            genericNameEn = "Chlortetrasin® 50% Soluble Powder / Premix",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "پودر حل‌شونده و پرمیکس مخلوط در دان، بسته‌های ۵۰۰ گرمی و ۱ کیلوگرمی",
            activeIngredientsFa = "هر گرم پودر حاوی ۵۰۰ میلی‌گرم کلرتتراسایکلین هیدروکلراید می‌باشد.",
            indicationsFa = "مهار باکتری‌های گرم مثبت و منفی، درمان سینوزیت عفونی، عفونت‌های کیسه‌های هوایی، اسهال‌های باکتریایی و کاهش تلفات اولیه جوجه‌ها.",
            dosageFa = "۲۰۰ تا ۴۰۰ میلی‌گرم پودر به ازای هر لیتر آب آشامیدنی یا ۳۰۰ تا ۵۰۰ گرم در هر تن دان مصرفی گله.",
            durationDaysFa = "۵ تا ۷ روز متوالی.",
            withdrawalPeriodFa = "گوشت مرغ: ۲ روز؛ تخم‌مرغ: ۱ روز.",
            contraindicationsFa = "در دان‌های با سطح کلسیم بسیار بالا به علت شلاتاسیون جذب دارو کاهش می‌یابد.",
            storageAndPrecautionsFa = "در جای خشک، تاریک و خنک نگهداری شود."
        ),
        PoultryMedication(
            id = "avipen_v",
            tradeNameFa = "اِیوپین وی رویان® (فنوکسی متیل پنی‌سیلین ۲۹.۳٪)",
            genericNameEn = "Avipen V® 29.3% Soluble Powder",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "پودر محلول در آب، قوطی ۵۰۰ گرمی",
            activeIngredientsFa = "هر گرم پودر حاوی ۲۹۳ میلی‌گرم فنوکسی متیل پنی‌سیلین پتاسیم (پنی‌سیلین V مقاوم به اسید معده) است.",
            indicationsFa = "داروی خط اول و تخصصی جهت پیشگیری و درمان آنتریت نکروتیک (Necrotic Enteritis) ناشی از کلستریدیوم پرفرنژنس در جوجه‌های گوشتی و گله‌های مادر.",
            dosageFa = "۶۵ تا ۱۰۰ میلی‌گرم در هر لیتر آب آشامیدنی (معادل ۱۰ تا ۲۰ میلی‌گرم پنی‌سیلین خالص به ازای هر کیلو وزن پرنده).",
            durationDaysFa = "۳ تا ۵ روز پیوسته.",
            withdrawalPeriodFa = "گوشت: ۲ روز قبل از ذبح.",
            contraindicationsFa = "در پرندگان دارای حساسیت به بتالاکتام‌ها منع مصرف دارد.",
            storageAndPrecautionsFa = "پس از باز کردن در قوطی، از تماس با رطوبت محافظت شود."
        ),
        PoultryMedication(
            id = "tiamulin_45",
            tradeNameFa = "تیامولین ۴۵٪ رویان",
            genericNameEn = "Tiamulin Hydrogen Fumarate 45% (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "پودر محلول در آب، ساشه ۱۰۰ و ۵۰۰ گرمی",
            activeIngredientsFa = "هر گرم حاوی ۴۵۰ میلی‌گرم تیامولین هیدروژن فومارات است.",
            indicationsFa = "درمان و پیشگیری بسیار کارآمد از بیماری مزمن تنفسی (CRD)، سینوویت عفونی، و سینوزیت ناشی از مایکوپلاسما گالی‌سپتیکوم و مایکوپلاسما سینوویه.",
            dosageFa = "۳۰ تا ۶۰ میلی‌گرم در هر لیتر آب آشامیدنی (معادل ۲۰ تا ۲۵ میلی‌گرم ماده مؤثره برای هر کیلو وزن در روز).",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت مرغ: ۳ روز؛ گوشت بوقلمون: ۵ روز قبل از کشتار.",
            contraindicationsFa = "هشدار خطرناک: مصرف همزمان تیامولین با داروهای ضدکوکسیدیوز یونوفوره (موننسین، ناراسین، سالینومایسین، مادورامایسین و لازالوسید) اکیداً ممنوع است و باعث فلجی شدید و تلفات گسترده می‌شود. فاصله زمانی حداقل ۷ روز قبل و بعد از مصرف یونوفورها رعایت شود.",
            storageAndPrecautionsFa = "در دمای زیر ۲۵ درجه، در جای خشک و دور از نور نگهداری شود."
        ),
        PoultryMedication(
            id = "doxycycline_50_rooyan",
            tradeNameFa = "داکسی‌سایکلین ۵۰٪ رویان",
            genericNameEn = "Doxycycline Hyclate 50% (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "پودر حل شونده در آب، ساشه و قوطی ۵۰۰ گرمی",
            activeIngredientsFa = "هر گرم حاوی ۵۰۰ میلی‌گرم داکسی‌سایکلین هایکلیت با حلالیت بسیار بالا است.",
            indicationsFa = "درمان عفونت‌های حاد و مزمن دستگاه تنفسی (CRD کمپلکس)، کوریزای عفونی، ورم روده نکروتیک و عفونت‌های مایکوپلاسمایی همراه با باکتری‌های ثانویه.",
            dosageFa = "۱۰۰ تا ۲۰۰ میلی‌گرم در هر لیتر آب آشامیدنی (معادل ۱۰ الی ۲۰ میلی‌گرم ماده خالص برای هر کیلوگرم وزن زنده در روز).",
            durationDaysFa = "۳ تا ۵ روز پیوسته.",
            withdrawalPeriodFa = "گوشت: ۵ روز قبل از کشتار. در طیور تخمگذار با مصرف انسانی تخم‌مرغ منع مصرف دارد.",
            contraindicationsFa = "با یون‌های فلزی دو و سه ظرفیتی به طور همزمان مصرف نگردد.",
            storageAndPrecautionsFa = "در جای خشک و خنک و دور از تابش آفتاب نگهداری شود."
        ),
        PoultryMedication(
            id = "sultrimix_rooyan",
            tradeNameFa = "سولتریمیکس رویان (سولفادیازین + تری‌متوپریم)",
            genericNameEn = "Sultrimix® Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "حاوی ۲۰۰ میلی‌گرم سولفادیازین و ۴۰ میلی‌گرم تری‌متوپریم در هر میلی‌لیتر (ترکیب سینرژیک مسدودکننده اسید فولیک باکتری).",
            indicationsFa = "درمان باکتری‌های گرم مثبت و منفی مقاوم، کلی‌باسیلوزیس، سالمونلوز، پاستورلوز (وبا)، کوریزای عفونی و کوکسیدیوزهای باکتریایی ثانویه.",
            dosageFa = "۱ میلی‌لیتر در هر ۲ لیتر آب آشامیدنی (معادل ۳۰ میلی‌گرم ماده ترکیبی برای هر کیلوگرم وزن زنده پرنده).",
            durationDaysFa = "۴ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت: ۵ روز قبل از ذبح. در مرغان تخمگذار تجاری مصرف نشود.",
            contraindicationsFa = "در صورت وجود نارسایی شدید کبدی و کلیوی منع مصرف دارد.",
            storageAndPrecautionsFa = "در دمای ۱۵ الی ۲۵ درجه و دور از یخ‌زدگی نگهداری شود."
        ),
        PoultryMedication(
            id = "coli_stop_10",
            tradeNameFa = "کلی‌استاپ ۱۰ رویان (کولیستین سولفات)",
            genericNameEn = "Coli-Stop 10® Oral Solution",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.QUAIL),
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "هر میلی‌لیتر حاوی ۲٬۰۰۰٬۰۰۰ واحد بین‌المللی کولیستین سولفات است.",
            indicationsFa = "درمان اختصاصی عفونت‌های گوارشی حاد و اسهال‌های باکتریایی ناشی از اشریشیا کلی و سالمونلا در مجرای روده بدون جذب سیستمیک و بدون برجا گذاشتن باقیمانده در بافت‌ها.",
            dosageFa = "۱ میلی‌لیتر در هر ۲ تا ۴ لیتر آب آشامیدنی به مدت ۳ تا ۵ روز متوالی.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت: ۱ روز؛ تخم‌مرغ: ۰ روز (بدون دوره پرهیز).",
            contraindicationsFa = "تداخل دارویی شاخصی ندارد.",
            storageAndPrecautionsFa = "در دمای زیر ۲۵ درجه سانتی‌گراد نگهداری شود."
        ),
        PoultryMedication(
            id = "tylosin_rooyan",
            tradeNameFa = "تایلوزین تارتارات رویان",
            genericNameEn = "Tylosin Tartrate Soluble Powder (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTIBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL),
            formAndPackaging = "پودر محلول در آب، قوطی ۱۰۰ گرمی",
            activeIngredientsFa = "هر گرم پودر حاوی ۱۰۰۰ میلی‌گرم تایلوزین تارتارات خالص است.",
            indicationsFa = "پیشگیری و درمان تخصصی بیماری مزمن تنفسی (CRD) ناشی از مایکوپلاسما گالی‌سپتیکوم و مایکوپلاسما سینوویه در مرغ و بوقلمون.",
            dosageFa = "۰.۵ تا ۱ گرم در هر لیتر آب آشامیدنی در ۳ روز اول ورود جوجه، و تکرار در سن ۴ هفتگی به مدت ۱ تا ۲ روز.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت مرغ: ۱ روز؛ گوشت بوقلمون: ۵ روز. تخم‌مرغ: بدون پرهیز.",
            contraindicationsFa = "با لینکومایسین و اریترومایسین مصرف نشود.",
            storageAndPrecautionsFa = "در جای خشک، خنک و دور از رطوبت نگهداری گردد."
        ),

        // ==========================================
        // ۲. ضدکوکسیدیوز و ضدانگل‌ها (رویان دارو)
        // ==========================================
        PoultryMedication(
            id = "freecox_rooyan",
            tradeNameFa = "فری‌کوکس® رویان (تولترازوریل ۲.۵٪)",
            genericNameEn = "Freecox® 2.5% Oral Solution (Toltrazuril)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTICODICIDIAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "محلول خوراکی، بطری ۱ لیتری",
            activeIngredientsFa = "هر میلی‌لیتر حاوی ۲۵ میلی‌گرم تولترازوریل خالص (مشتق تری‌آزینتریون) است.",
            indicationsFa = "داروی مرجع و خط اول درمان قطعی کوکسیدیوز بالینی در طیور. مؤثر بر تمامی مراحل تکاملی داخل سلولی انگل‌های ایمریا شامل ایمریا آسروولینا، برونتی، ماکسیما، میتیس، نکاتریکس و تنلا (عامل اسهال خونی سکومی).",
            dosageFa = "۷ میلی‌گرم به ازای هر کیلو وزن زنده در روز (معادل ۱ میلی‌لیتر در هر لیتر آب آشامیدنی به مدت ۲۴ ساعت پیوسته در ۲ روز متوالی، یا ۳ میلی‌لیتر در هر لیتر آب به مدت ۸ ساعت در روز در ۲ روز متوالی).",
            durationDaysFa = "۲ روز متوالی.",
            withdrawalPeriodFa = "گوشت مرغ: ۱۲ تا ۱۴ روز؛ گوشت بوقلمون: ۱۶ روز قبل از کشتار. در مرغان تخمگذار تجاری منع مصرف دارد.",
            contraindicationsFa = "در آب‌های به شدت اسیدی رقیق نگردد.",
            storageAndPrecautionsFa = "در دمای ۱۵ تا ۲۵ درجه نگهداری شود. از تماس مستقیم با پوست و چشم اجتناب گردد."
        ),
        PoultryMedication(
            id = "coccizine_rooyan",
            tradeNameFa = "کوکسی‌زاین® رویان (سولفاکلوزاین ۳۰٪)",
            genericNameEn = "Coccizine® 30% Soluble Powder",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTICODICIDIAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY),
            formAndPackaging = "پودر حل‌شونده در آب، ساشه ۱۰۰ و ۲۵۰ گرمی",
            activeIngredientsFa = "هر گرم حاوی ۳۰۰ میلی‌گرم سولفاکلوزاین سدیم مونو هیدرات است.",
            indicationsFa = "درمان اورژانسی اسهال خونی حاد ناشی از کوکسیدیوز سکومی (ایمریا تنلا و نکاتریکس) و درمان همزمان پاستورلوز (وبای مرغان) و تیفوئید.",
            dosageFa = "۱.۵ تا ۲ گرم در هر لیتر آب آشامیدنی به مدت ۳ روز متوالی (روش ۳-۲-۳ در صورت نیاز به تکرار).",
            durationDaysFa = "۳ روز پیوسته.",
            withdrawalPeriodFa = "گوشت مرغ: ۷ روز؛ گوشت بوقلمون: ۱۴ روز.",
            contraindicationsFa = "در پرندگان با کمبود شدید آب یا نارسایی حاد کلیه مصرف نشود.",
            storageAndPrecautionsFa = "در محل خشک و دور از نور مستقیم نگهداری شود."
        ),
        PoultryMedication(
            id = "ampromax_rooyan",
            tradeNameFa = "آمپرومکس رویان (آمپرولیوم ۲۰٪)",
            genericNameEn = "Ampromax® 20% Soluble Powder (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTICODICIDIAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.QUAIL),
            formAndPackaging = "پودر محلول در آب، بسته‌های ۱۰۰ و ۱۰۰۰ گرمی",
            activeIngredientsFa = "هر گرم پودر حاوی ۲۰۰ میلی‌گرم آمپرولیوم هیدروکلراید است.",
            indicationsFa = "پیشگیری و درمان کوکسیدیوز روده‌ای و سکومی در جوجه‌های گوشتی، پولت‌های جایگزین تخمگذار و بوقلمون.",
            dosageFa = "درمان: ۱.۲ تا ۲.۵ گرم در هر لیتر آب آشامیدنی به مدت ۵ تا ۷ روز. پیشگیری: ۰.۶ گرم در لیتر به مدت ۱ الی ۲ هفته متوالی.",
            durationDaysFa = "۵ تا ۷ روز برای درمان کامل.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز). مجاز در گله‌های گوشتی و تخمگذار بدون توقف تخم‌گذاری.",
            contraindicationsFa = "مصرف همزمان با دوز بالای ویتامین B1 (تیامین) اثر ضدکوکسیدیوزی آن را خنثی می‌نماید.",
            storageAndPrecautionsFa = "در ظرف کاملاً دربسته و در محل خشک نگهداری شود."
        ),
        PoultryMedication(
            id = "levamisole_rooyan",
            tradeNameFa = "لوامیزول رویان (ضدانگل لوله‌ای و ایمونواستیمولانت)",
            genericNameEn = "Levamisole HCl Powder (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ANTICODICIDIAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.GOOSE),
            formAndPackaging = "ساشه ۵ گرمی و ۱۰۰ گرمی",
            activeIngredientsFa = "هر گرم حاوی لوامیزول هیدروکلراید خالص است.",
            indicationsFa = "فلج کردن و دفع انواع کرم‌های گرد لوله‌ای دستگاه گوارش (آسکاریدیا، هتراکیس گالیناروم و کاپیلاریا) و تقویت ایمنی اختصاصی گله قبل از واکسیناسیون‌ها.",
            dosageFa = "۲۵ تا ۳۰ میلی‌گرم ماده خالص برای هر کیلوگرم وزن زنده در یک نوبت مصرف در آب آشامیدنی (تقریباً ۱ گرم برای ۴۰ تا ۵۰ کیلوگرم وزن زنده).",
            durationDaysFa = "تک‌نوبت در یک روز (با تکرار پس از ۱۴ روز در صورت آلودگی شدید بستر).",
            withdrawalPeriodFa = "گوشت: ۳ روز قبل از کشتار.",
            contraindicationsFa = "با ترکیبات ارگانوفسفره یا پیرانتل همزمان استفاده نشود.",
            storageAndPrecautionsFa = "دور از دسترس اطفال و در جای خشک نگهداری شود."
        ),

        // ==========================================
        // ۳. ویتامین‌ها و مکمل‌های تقویتی و کلیوی (رویان دارو)
        // ==========================================
        PoultryMedication(
            id = "vita_renyl_rooyan",
            tradeNameFa = "ویتارنیل® رویان (شستشو و تقویت کلیه)",
            genericNameEn = "Vita-Renyl® Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.VITAMIN_MINERAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی، بطری ۱ لیتری",
            activeIngredientsFa = "فرموله اختصاصی حاوی هگزامین، سوربیتول، عصاره‌های گیاهی مدر (Diuretic)، استات پتاسیم و ویتامین‌های گروه B.",
            indicationsFa = "پاکسازی و دفع رسوبات اورات از توبول‌های کلیه، پیشگیری و درمان نقرس احشایی (Visceral Gout)، کاهش عوارض برونشیت کلیوی و گامبورو، بهبود وضعیت گله پس از دوره‌های سنگین درمانی با سولفانامیدها و آنتی‌بیوتیک‌ها.",
            dosageFa = "۱ تا ۲ میلی‌لیتر در هر لیتر آب آشامیدنی به مدت ۳ تا ۵ روز متوالی.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "ندارد.",
            storageAndPrecautionsFa = "در دمای ۱۵ تا ۲۵ درجه، دور از نور مستقیم نگهداری شود."
        ),
        PoultryMedication(
            id = "energy_plus_rooyan",
            tradeNameFa = "انرژی پلاس® رویان (ضد استرس گرمایی و شوک)",
            genericNameEn = "Energy Plus® Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.ELECTROLYTE_ANTISTRESS,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی غلیظ، گالن ۱ و ۵ لیتری",
            activeIngredientsFa = "ترکیب هم‌افزای گلوکز مایع، بتائین خالص (تنظیم‌کننده اسمزی)، اسید فسفریک، یون‌های سدیم، پتاسیم و منیزیم.",
            indicationsFa = "مهار قطعی استرس گرمایی در تابستان، پیشگیری از آلکالوز تنفسی و تلفات ناشی از گرمازدگی، بازگرداندن انرژی جوجه پس از جوجه‌ریزی، کاهش افت وزن در ساعات گرم روز و ریکاوری بعد از بیماری‌ها.",
            dosageFa = "۱ تا ۲ لیتر در ۱۰۰۰ لیتر آب آشامیدنی در ساعات اوج گرما (ساعت ۱۰ صبح تا ۵ بعدازظهر).",
            durationDaysFa = "در طول روزهای گرم یا ۲۴ ساعت اول ورود جوجه.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "ندارد.",
            storageAndPrecautionsFa = "در جای خنک و دور از تابش مستقیم آفتاب نگهداری شود."
        ),
        PoultryMedication(
            id = "amino_vitatrace_rooyan",
            tradeNameFa = "آمینو ویتاتریس® رویان (مولتی‌ویتامین + اسیدآمینه + مینرال)",
            genericNameEn = "Amino-Vitatrace® Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.VITAMIN_MINERAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "حاوی تمام ویتامین‌های محلول در آب و چربی، اسیدهای آمینه ضروری (متیونین، لیزین، ترئونین، تریپتوفان) و عناصر ریزمغذی روی، منگنز، مس، آهن و سلنیوم.",
            indicationsFa = "بهبود چشمگیر وزن‌گیری، کاهش ضریب تبدیل دان (FCR)، تسریع پردرآوری، جلوگیری از کانیبالیسم و پرکنی، تقویت سیستم اسکلتی و ایمنی گله.",
            dosageFa = "۱ میلی‌لیتر در هر ۲ تا ۴ لیتر آب آشامیدنی.",
            durationDaysFa = "۳ تا ۵ روز متوالی در هر ماه یا در دوره‌های رشد سریع.",
            withdrawalPeriodFa = "بدون دوره پرهیز (۰ روز).",
            contraindicationsFa = "ندارد.",
            storageAndPrecautionsFa = "در دمای ۱۵ تا ۲۵ درجه و دور از تابش آفتاب نگهداری شود."
        ),
        PoultryMedication(
            id = "ad3eck_rooyan",
            tradeNameFa = "ویتامین AD3ECK رویان",
            genericNameEn = "AD3ECK Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.VITAMIN_MINERAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی امولسیون ۱ لیتری",
            activeIngredientsFa = "حاوی غلظت بالای ویتامین‌های A, D3, E, C و K3.",
            indicationsFa = "کنترل خونریزی‌های ناشی از کوکسیدیوز و نوک‌چینی (ویتامین K)، افزایش استحکام پوسته تخم‌مرغ و مهار تخم‌مرغ‌های لمبه (A, D3)، تقویت ایمنی در برابر استرس‌های فصلی (E, C).",
            dosageFa = "۱ میلی‌لیتر در هر ۲ لیتر آب آشامیدنی.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "از مصرف بیش از حد و متوالی به علت سمیت ویتامین A و D اجتناب گردد.",
            storageAndPrecautionsFa = "در جای خنک و دور از یخ‌زدگی نگهداری شود."
        ),
        PoultryMedication(
            id = "vit_e_se_rooyan",
            tradeNameFa = "ویتامین E + سلنیوم رویان",
            genericNameEn = "Vitamin E 10% + Selenium Oral Solution (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.VITAMIN_MINERAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "هر میلی‌لیتر حاوی ۱۰۰ میلی‌گرم ویتامین E و ۰.۵ میلی‌گرم سدیم سلنیت خالص می‌باشد.",
            indicationsFa = "پیشگیری و درمان دیستروفی عضلانی (بیماری عضله سفید)، عارضه آنسفالومالاسی (بیماری جوجه دیوانه)، بیماری تراوش مایع زیر پوستی و تقویت فوق‌العاده باروری و نطفه‌داری خروس‌ها در گله‌های مولد.",
            dosageFa = "۱ میلی‌لیتر در هر ۲ لیتر آب آشامیدنی.",
            durationDaysFa = "۳ تا ۵ روز پیوسته.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "از مصرف همزمان با سایر مکمل‌های پرسلنیوم خودداری شود.",
            storageAndPrecautionsFa = "در محیط خنک و تاریک نگهداری شود."
        ),
        PoultryMedication(
            id = "multivit_elec_rooyan",
            tradeNameFa = "مولتی‌ویتامین و الکترولیت رویان",
            genericNameEn = "Multivitamin + Electrolytes Powder (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.VITAMIN_MINERAL,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "پودر حل شونده در آب، ساشه و قوطی‌های ۵۰۰ و ۱۰۰۰ گرمی",
            activeIngredientsFa = "حاوی تمام ویتامین‌های ضروری خانواده B، ویتامین A، D3، E و املاح سدیم، پتاسیم و کلراید.",
            indicationsFa = "جبران سریع کمبودهای ویتامینی، رفع بی‌حالی و بی‌اشتهایی گله، پس از درمان‌های آنتی‌بیوتیکی و جهت ریکاوری بعد از واکسیناسیون‌ها.",
            dosageFa = "۱ گرم در هر ۲ تا ۴ لیتر آب آشامیدنی.",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "ندارد.",
            storageAndPrecautionsFa = "در ظرف دربسته و دور از نفوذ رطوبت نگهداری گردد."
        ),
        PoultryMedication(
            id = "mucolyse_rooyan",
            tradeNameFa = "موکولیز رویان / برونکوسپت (خلط‌آور و بازکننده تنفس)",
            genericNameEn = "Mucolyse® Oral Solution / Bronchosept",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.RESPIRATORY_EXPECTORANT,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "محلول خوراکی ۱ لیتری",
            activeIngredientsFa = "حاوی بروم‌هگزین هیدروکلراید، منتول و اسانس طبیعی اکالیپتوس و آویشن باغی.",
            indicationsFa = "رقیق‌کننده و حل‌کننده موکوس و ترشحات غلیظ تراشه و برونش‌ها، تسکین تنفس در برونشیت عفونی، نیوکاسل، کوریزا و CRD، پاکسازی ریه در سالن‌های با گرد و غبار یا گاز آمونیاک بالا.",
            dosageFa = "۱ میلی‌لیتر در هر لیتر آب آشامیدنی (یا به صورت اسپری مه‌پاشی در هوای سالن با دوز ۲۰۰ میلی‌لیتر در ۱۰ لیتر آب).",
            durationDaysFa = "۳ تا ۵ روز متوالی.",
            withdrawalPeriodFa = "گوشت: ۲ روز قبل از کشتار.",
            contraindicationsFa = "در صورت بروز ادم ریوی شدید با دامپزشک مشورت شود.",
            storageAndPrecautionsFa = "در ظرف تیره در دمای زیر ۲۵ درجه سانتی‌گراد نگهداری شود."
        ),

        // ==========================================
        // ۴. پروبیوتیک‌ها و ضدعفونی‌کننده‌ها (رویان دارو)
        // ==========================================
        PoultryMedication(
            id = "dynapro_777_rooyan",
            tradeNameFa = "دیناپرو ۷۷۷ رویان (پروبیوتیک جوشان)",
            genericNameEn = "DynaPro 777® Effervescent Probiotic Tablets",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.PROBIOTIC_PREBIOTIC,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "قوطی‌های حاوی قرص‌های جوشان حل‌شونده فوری",
            activeIngredientsFa = "ترکیب پروبیوتیک‌های باکتریایی و مخمری فعال همراه با ویتامین‌ها و محرک‌های ایمنی سلولی.",
            indicationsFa = "استقرار سریع باکتری‌های همزیست مفید در دستگاه گوارش، رقابت بیولوژیک با سالمونلا و باکتری‌های بیماری‌زا، بهبود ضریب تبدیل خوراک و کاهش رطوبت بستر.",
            dosageFa = "یک قرص جوشان در هر ۵۰ تا ۱۰۰ لیتر آب آشامیدنی بدون کلر.",
            durationDaysFa = "۳ روز متوالی پس از ورود جوجه و بعد از هر دوره مصرف آنتی‌بیوتیک.",
            withdrawalPeriodFa = "طبیعی و فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "با آب حاوی کلر یا همزمان با آنتی‌بیوتیک‌ها مصرف نشود.",
            storageAndPrecautionsFa = "در جای خشک، خنک و دور از تابش آفتاب نگهداری شود."
        ),
        PoultryMedication(
            id = "benzalkonium_rooyan",
            tradeNameFa = "بنزالکونیوم کلراید ۲۰٪ رویان",
            genericNameEn = "Benzalkonium Chloride 20% Disinfectant (Rooyan)",
            manufacturer = "داروسازی رویان دارو",
            category = MedicationCategory.DISINFECTANT_SANITIZER,
            administrationRoute = AdministrationRoute.ENVIRONMENTAL,
            targetSpecies = BirdSpecies.entries,
            formAndPackaging = "گالن‌های ۱ و ۵ لیتری مایع غلیظ ضدعفونی",
            activeIngredientsFa = "هر ۱۰۰ میلی‌لیتر حاوی ۲۰ گرم بنزالکونیوم کلراید (ترکیب آمونیوم چهارتایی خالص) است.",
            indicationsFa = "ضدعفونی کلیه سطوح سالن‌های پرورش، قفس‌ها، تانکرها و لوله‌های نیپل آبخوری، دانخوری‌ها، حوضچه‌های ضدعفونی ورودی فارم و وسایل نقلیه حمل دان.",
            dosageFa = "شستشوی سطوح: ۱ لیتر در ۲۰۰ لیتر آب؛ ضدعفونی مدار آبخوری هنگام خالی بودن سالن: ۱ لیتر در ۱۰۰۰ لیتر آب.",
            durationDaysFa = "قبل از هر جوجه‌ریزی و در پایان هر دوره پرورشی.",
            withdrawalPeriodFa = "مصرف خوراکی مستقیم ندارد.",
            contraindicationsFa = "با صابون و شوینده‌های آنیونی ترکیب نشود (بی‌اثر می‌شود).",
            storageAndPrecautionsFa = "هنگام کار از دستکش، ماسک و عینک ایمنی استفاده گردد."
        ),

        // ==========================================
        // ۵. واکسن‌های بیولوژیک طیور
        // ==========================================
        PoultryMedication(
            id = "vaccine_nd_b1_rooyan",
            tradeNameFa = "واکسن زنده نیوکاسل B1 / کلون ۳۰",
            genericNameEn = "Newcastle Disease Live Vaccine (B1 Strain / Clone 30)",
            manufacturer = "مؤسسه رازی / تحت نظارت دامپزشکی",
            category = MedicationCategory.VACCINE,
            administrationRoute = AdministrationRoute.EYE_DROP,
            targetSpecies = listOf(BirdSpecies.CHICKEN, BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL),
            formAndPackaging = "ویال لیوفیلیزه ۱۰۰۰ و ۲۵۰۰ دوزی",
            activeIngredientsFa = "حاوی سویه تخفیف حدت‌یافته ویروس نیوکاسل (B1 یا Clone 30).",
            indicationsFa = "ایمن‌سازی فعال اولیه جوجه‌ها علیه سویه‌های فیلدی خطرناک نیوکاسل تنفسی و عصبی.",
            dosageFa = "یک قطره در یک چشم یا محلول در آب آشامیدنی بدون کلر همراه با شیرخشک، یک دوز کامل برای هر پرنده.",
            durationDaysFa = "سن ۷ تا ۱۰ روزگی با تکرار در سن ۲۱ تا ۲۴ روزگی.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "فقط در گله‌های کاملاً سالم تجویز شود.",
            storageAndPrecautionsFa = "رعایت دقیق زنجیره سرد ۲ تا ۸ درجه سانتی‌گراد تا لحظه مصرف."
        ),
        PoultryMedication(
            id = "vaccine_gumboro_rooyan",
            tradeNameFa = "واکسن گامبورو (بورس عفونی D78)",
            genericNameEn = "Infectious Bursal Disease Vaccine (Gumboro D78)",
            manufacturer = "مؤسسه رازی / داروسازی دامی",
            category = MedicationCategory.VACCINE,
            administrationRoute = AdministrationRoute.DRINKING_WATER,
            targetSpecies = listOf(BirdSpecies.CHICKEN),
            formAndPackaging = "ویال ۱۰۰۰ و ۲۵۰۰ دوزی لیوفیلیزه",
            activeIngredientsFa = "ویروس زنده تخفیف حدت‌یافته گامبورو سویه اینترمدیت.",
            indicationsFa = "ایمن‌سازی فعال علیه بیماری تضعیف‌کننده ایمنی بورس عفونی (گامبورو) در جوجه‌های گوشتی و تخمگذار.",
            dosageFa = "یک دوز برای هر قطعه جوجه از طریق آب آشامیدنی بدون کلر همراه با شیر خشک بدون چربی.",
            durationDaysFa = "معمولاً سن ۱۴ الی ۱۶ روزگی و تکرار در سن ۲۲ روزگی.",
            withdrawalPeriodFa = "فاقد دوره پرهیز (۰ روز).",
            contraindicationsFa = "در گله‌های با استرس حاد یا بیماری زمینه‌ای مصرف نگردد.",
            storageAndPrecautionsFa = "حفظ دقیق زنجیره سرد ۲ تا ۸ درجه سانتی‌گراد."
        )
    )

    fun getMedicationsByCategory(category: MedicationCategory?): List<PoultryMedication> {
        return if (category == null) allMedications else allMedications.filter { it.category == category }
    }

    fun searchMedications(query: String, category: MedicationCategory? = null): List<PoultryMedication> {
        val base = getMedicationsByCategory(category)
        if (query.isBlank()) return base
        val q = query.trim().lowercase()
        return base.filter {
            it.tradeNameFa.lowercase().contains(q) ||
                    it.genericNameEn.lowercase().contains(q) ||
                    it.activeIngredientsFa.lowercase().contains(q) ||
                    it.indicationsFa.lowercase().contains(q) ||
                    it.dosageFa.lowercase().contains(q) ||
                    it.manufacturer.lowercase().contains(q)
        }
    }

    val allDrugs: List<PoultryMedication>
        get() = allMedications.filter { it.category.isDrug }

    val allSupplements: List<PoultryMedication>
        get() = allMedications.filter { it.category.isSupplement }

    val drugCategories: List<MedicationCategory>
        get() = MedicationCategory.entries.filter { it.isDrug }

    val supplementCategories: List<MedicationCategory>
        get() = MedicationCategory.entries.filter { it.isSupplement }

    fun searchDrugs(query: String, category: MedicationCategory? = null): List<PoultryMedication> {
        val base = if (category != null && category.isDrug) {
            allDrugs.filter { it.category == category }
        } else {
            allDrugs
        }
        if (query.isBlank()) return base
        val q = query.trim().lowercase()
        return base.filter {
            it.tradeNameFa.lowercase().contains(q) ||
                    it.genericNameEn.lowercase().contains(q) ||
                    it.activeIngredientsFa.lowercase().contains(q) ||
                    it.indicationsFa.lowercase().contains(q) ||
                    it.dosageFa.lowercase().contains(q)
        }
    }

    fun searchSupplements(query: String, category: MedicationCategory? = null): List<PoultryMedication> {
        val base = if (category != null && category.isSupplement) {
            allSupplements.filter { it.category == category }
        } else {
            allSupplements
        }
        if (query.isBlank()) return base
        val q = query.trim().lowercase()
        return base.filter {
            it.tradeNameFa.lowercase().contains(q) ||
                    it.genericNameEn.lowercase().contains(q) ||
                    it.activeIngredientsFa.lowercase().contains(q) ||
                    it.indicationsFa.lowercase().contains(q) ||
                    it.dosageFa.lowercase().contains(q)
        }
    }

    /**
     * Finds and matches specific medications from a disease's commonDrugsFa text string.
     */
    fun findMatchingMedications(diseaseDrugNamesText: String): List<PoultryMedication> {
        if (diseaseDrugNamesText.isBlank()) return emptyList()
        val text = diseaseDrugNamesText.lowercase()
        return allMedications.filter { med ->
            val trade = med.tradeNameFa.lowercase()
            val gen = med.genericNameEn.lowercase()
            val active = med.activeIngredientsFa.lowercase()

            when {
                text.contains("انروفلوکساسین") && (trade.contains("انرو") || gen.contains("enro")) -> true
                text.contains("تایلوزین") && (trade.contains("تایلوزین") || gen.contains("tylosin")) -> true
                text.contains("داکسی") && (trade.contains("داکسی") || gen.contains("doxy")) -> true
                text.contains("اکسی تتراسایکلین") && (trade.contains("تترا") || gen.contains("tetra")) -> true
                text.contains("کلرتتراسایکلین") && (trade.contains("کلرتترا") || gen.contains("chlortetra")) -> true
                text.contains("کولیستین") && (trade.contains("کلی") || gen.contains("coli") || gen.contains("colistin")) -> true
                text.contains("تولترازوریل") && (trade.contains("فری‌کوکس") || trade.contains("تولترازوریل") || gen.contains("toltrazuril")) -> true
                text.contains("بایکوکس") && (trade.contains("فری‌کوکس") || gen.contains("toltrazuril")) -> true
                text.contains("آمپرولیوم") && (trade.contains("آمپرومکس") || trade.contains("آمپرولیوم") || gen.contains("ampro")) -> true
                text.contains("سولفاکلوزاین") && (trade.contains("کوکسی‌زاین") || gen.contains("coccizine")) -> true
                text.contains("لوامیزول") && (trade.contains("لوامیزول") || gen.contains("levamisole")) -> true
                text.contains("بروم‌هگزین") && (trade.contains("موکولیز") || gen.contains("mucolyse")) -> true
                text.contains("الکترولیت") && (trade.contains("الکترولیت") || trade.contains("انرژی پلاس")) -> true
                text.contains("مولتی ویتامین") && (trade.contains("مولتی‌ویتامین") || trade.contains("آمینو ویتاتریس")) -> true
                text.contains("ویتامین c") && (trade.contains("ad3eck") || trade.contains("انرژی پلاس")) -> true
                text.contains("پروبیوتیک") && (trade.contains("دیناپرو") || gen.contains("probiotic")) -> true
                text.contains("ضدعفونی") && (trade.contains("بنزالکونیوم") || gen.contains("disinfectant")) -> true
                text.contains("پنی‌سیلین") && (trade.contains("ایوپین") || gen.contains("penicillin")) -> true
                text.contains("تیامولین") && (trade.contains("تیامولین") || gen.contains("tiamulin")) -> true
                text.contains("آپرامایسین") && (trade.contains("آپراسول") || gen.contains("aprasol")) -> true
                else -> false
            }
        }.distinctBy { it.id }
    }
}

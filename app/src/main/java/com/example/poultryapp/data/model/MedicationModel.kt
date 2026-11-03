package com.example.poultryapp.data.model

enum class MedicationCategory(val titleFa: String, val badgeColorHex: Long) {
    ANTIBIOTIC("آنتی‌بیوتیک", 0xFFE57373),
    ANTICODICIDIAL("ضدکوکسیدیوز و ضدانگل", 0xFFBA68C8),
    VITAMIN_MINERAL("ویتامین و مواد معدنی", 0xFFFFB74D),
    ELECTROLYTE_ANTISTRESS("الکترولیت و ضد استرس", 0xFF4DD0E1),
    PROBIOTIC_PREBIOTIC("پروبیوتیک و گوارشی", 0xFF81C784),
    RESPIRATORY_EXPECTORANT("تنفسی و موکولیتیک", 0xFF26A69A),
    VACCINE("واکسن و بیولوژیک", 0xFF64B5F6),
    DISINFECTANT_SANITIZER("ضدعفونی‌کننده", 0xFFA1887F);

    val isSupplement: Boolean
        get() = this == VITAMIN_MINERAL ||
                this == ELECTROLYTE_ANTISTRESS ||
                this == PROBIOTIC_PREBIOTIC

    val isDrug: Boolean
        get() = !isSupplement
}

enum class AdministrationRoute(val titleFa: String) {
    DRINKING_WATER("محلول در آب آشامیدنی"),
    FEED_MIX("مخلوط در دان"),
    SPRAY_AEROSOL("اسپری و مه‌پاشی"),
    INJECTION("تزریقی (عضلانی/زیرپوستی)"),
    EYE_DROP("قطره چشمی"),
    ENVIRONMENTAL("محیطی و ضدعفونی")
}

data class PoultryMedication(
    val id: String,
    val tradeNameFa: String,
    val genericNameEn: String,
    val manufacturer: String = "شرکت داروسازی رویان دارو",
    val category: MedicationCategory,
    val administrationRoute: AdministrationRoute,
    val targetSpecies: List<BirdSpecies>,
    val formAndPackaging: String,
    val activeIngredientsFa: String,
    val indicationsFa: String,
    val dosageFa: String,
    val durationDaysFa: String,
    val withdrawalPeriodFa: String,
    val contraindicationsFa: String,
    val storageAndPrecautionsFa: String
)

package com.example.poultryapp.data.model

enum class PathogenType(val titleFa: String) {
    VIRAL("ویروسی"),
    BACTERIAL("باکتریایی"),
    PARASITIC("انگلی"),
    FUNGAL("قارچی"),
    NUTRITIONAL_METABOLIC("تغذیه‌ای و متابولیک")
}

enum class GrowthStage(val titleFa: String, val minWeeks: Int, val maxWeeks: Int) {
    ALL_STAGES("تمام سنین و دوره‌ها", 0, 150),
    EARLY_CHICK("جوجه‌های نوزاد (۱ تا ۷ روز)", 0, 1),
    BROODING_GROWTH("سنین رشد و پروراری (۱ تا ۶ هفته)", 1, 6),
    PULLET_REARING("پولت و نیمچگی (۷ تا ۱۸ هفته)", 7, 18),
    LAYING_ADULT("بالغ و تخم‌گذار (بالای ۱۸ هفته)", 18, 150)
}

enum class SymptomCategory(val titleFa: String) {
    RESPIRATORY("تنفسی"),
    DIGESTIVE("گوارشی"),
    NERVOUS("عصبی"),
    SKIN_APPEARANCE("پوستی و ظاهری"),
    REPRODUCTIVE_EGG("تولیدمثلی و تخم"),
    GENERAL_BEHAVIORAL("عمومی و رفتاری")
}

data class ClinicalSymptom(
    val id: String,
    val titleFa: String,
    val category: SymptomCategory,
    val voiceAliases: List<String> = emptyList()
)

data class DiseaseInfo(
    val id: String,
    val nameFa: String,
    val scientificName: String,
    val pathogenType: PathogenType,
    val susceptibleSpecies: List<BirdSpecies>,
    val primarySymptomIds: List<String>,      // Weight 3.0
    val secondarySymptomIds: List<String>,    // Weight 1.5
    val pathognomonicSymptomIds: List<String> = emptyList(), // Weight 5.0 (Definitive indicator)
    val fullSymptomsDescriptionFa: String,
    val definitiveDiagnosisFa: String,
    val treatmentStepsFa: String,
    val commonDrugsFa: String,
    val dosagesFa: String,
    val preventionAndVaccineFa: String,
    val contagiousnessFa: String,
    val zoonoticRiskFa: String,
    val quarantineAdviceFa: String,
    val isUrgentEmergency: Boolean = false,
    val minAgeWeeks: Int = 0,
    val maxAgeWeeks: Int = 150,
    val peakAgeWeeksStart: Int = 0,
    val peakAgeWeeksEnd: Int = 150,
    val susceptibleStages: List<GrowthStage> = listOf(GrowthStage.ALL_STAGES),
    val nutritionalDeficiencyNameFa: String? = null,
    val veterinaryClinicalTipFa: String? = null
) {
    val isNutritionalDeficiency: Boolean
        get() = (pathogenType == PathogenType.NUTRITIONAL_METABOLIC)
}

data class DiagnosticResult(
    val disease: DiseaseInfo,
    val probabilityPercent: Int, // 0 to 100
    val matchedSymptoms: List<String>,
    val ageCompatibilityNoteFa: String = "",
    val differentialKeySignFa: String = "",
    val isNutritional: Boolean = (disease.pathogenType == PathogenType.NUTRITIONAL_METABOLIC)
)

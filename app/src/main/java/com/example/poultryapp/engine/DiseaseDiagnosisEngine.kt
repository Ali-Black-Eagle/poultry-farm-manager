package com.example.poultryapp.engine

import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.DiagnosticResult
import com.example.poultryapp.data.model.DiseaseCatalog
import com.example.poultryapp.data.model.DiseaseInfo
import com.example.poultryapp.data.model.GrowthStage
import com.example.poultryapp.data.model.PathogenType

data class DiseaseAgeProfile(
    val minWeeks: Int = 0,
    val maxWeeks: Int = 150,
    val peakStartWeeks: Int = 0,
    val peakEndWeeks: Int = 150,
    val isStrictAgeLimited: Boolean = false,
    val differentialKeySign: String = "",
    val deficiencyRemedy: String? = null
)

data class VeterinaryConsultation(
    val topResult: DiagnosticResult?,
    val differentialCandidates: List<DiagnosticResult>,
    val isNutritionalDeficiency: Boolean,
    val evaluatedAgeWeeks: Int?,
    val clinicalReasoningFa: String,
    val immediateEmergencyActionFa: String,
    val firstLineRxFa: String,
    val followUpQuestionsFa: List<String>
)

object DiseaseDiagnosisEngine {

    private val AGE_PROFILES = mapOf(
        // سنین رشد و جوجه‌های گوشتی ۲ تا ۶ هفته
        "coccidiosis" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 20, peakStartWeeks = 3, peakEndWeeks = 6, differentialKeySign = "اسهال خونی یا قهوه‌ای موکوسی و ضعف ناگهانی"),
        "coccidiosis_cecal_acute" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 16, peakStartWeeks = 3, peakEndWeeks = 6, isStrictAgeLimited = false, differentialKeySign = "خونریزی حاد سکوم، اسهال خونی تازه و مرگ سریع"),
        "necrotic_enteritis" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 10, peakStartWeeks = 2, peakEndWeeks = 5, differentialKeySign = "اسهال تیره کف‌آلود لاشه با غشای حوله‌ای ژژونوم"),
        "gumboro" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 12, peakStartWeeks = 3, peakEndWeeks = 6, differentialKeySign = "تلفات نوک‌تیز ناگهانی، اسهال سفید آبکی و بورس متورم"),
        "marek" to DiseaseAgeProfile(minWeeks = 3, maxWeeks = 30, peakStartWeeks = 6, peakEndWeeks = 16, differentialKeySign = "فلجی نامتقارن یک پا به جلو و یک پا به عقب و عنبیه خاکستری"),
        "reovirus_arthritis" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 16, peakStartWeeks = 4, peakEndWeeks = 10, differentialKeySign = "تورم تاندون‌های فلکسور و پارگی تاندون گاستروکنمیوس"),

        // جوجه‌های نوزاد و هفته اول هچ
        "omphalitis_yolk_sac" to DiseaseAgeProfile(minWeeks = 0, maxWeeks = 2, peakStartWeeks = 0, peakEndWeeks = 1, isStrictAgeLimited = true, differentialKeySign = "ناف باز و سیاه، کیسه زرده جذب‌نشده و بوی بد هچری"),
        "fowl_typhoid_pullorum" to DiseaseAgeProfile(minWeeks = 0, maxWeeks = 4, peakStartWeeks = 0, peakEndWeeks = 2, differentialKeySign = "سفیدک و گچ دور مقعد و تلفات در سنین زیر ۲ هفته"),
        "acute_chick_dehydration" to DiseaseAgeProfile(minWeeks = 0, maxWeeks = 2, peakStartWeeks = 0, peakEndWeeks = 1, isStrictAgeLimited = true, differentialKeySign = "پاستی‌ونت، چسبیدن فضله گچی و چروکیدگی ساق پا"),
        "chilling_hypothermia" to DiseaseAgeProfile(minWeeks = 0, maxWeeks = 3, peakStartWeeks = 0, peakEndWeeks = 1, differentialKeySign = "کپه شدن دور مادر مصنوعی و کزکردگی ناشی از افت دما"),

        // کمبودهای تغذیه‌ای و اسکلتی سنین رشد
        "perosis_manganese" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 8, peakStartWeeks = 3, peakEndWeeks = 5, differentialKeySign = "دررفتگی تاندون آشیل و چرخش مفصل خرگوشی به خارج", deficiencyRemedy = "پرمیکس منگنز (سولفات منگنز) + کولین کلراید"),
        "rickets_d3_calcium" to DiseaseAgeProfile(minWeeks = 1, maxWeeks = 10, peakStartWeeks = 2, peakEndWeeks = 5, differentialKeySign = "نرمی استخوان ساق، منقار لاستیکی انعطاف‌پذیر و دنده‌های تسبیحی", deficiencyRemedy = "پرمیکس فسفات دی‌کلسیک + ویتامین D3"),
        "niacin_b3_deficiency" to DiseaseAgeProfile(minWeeks = 1, maxWeeks = 8, peakStartWeeks = 2, peakEndWeeks = 5, differentialKeySign = "پاهای به شدت کمانی و پرانتزی در جوجه‌های سنگین", deficiencyRemedy = "مکمل ویتامین B3 (نیاسین‌آمید)"),
        "riboflavin_b2_deficiency" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 8, peakStartWeeks = 3, peakEndWeeks = 5, differentialKeySign = "فلجی و جمع‌شدگی انگشتان پا به داخل (پنجه خمیده)", deficiencyRemedy = "پودر ویتامین B2 (ریبوفلاوین)"),
        "crazy_chick_vit_e" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 8, peakStartWeeks = 2, peakEndWeeks = 5, differentialKeySign = "آنسفالومالاسی، عدم تعادل و تلوتلو خوردن و چرخش سر", deficiencyRemedy = "پرمیکس ویتامین E + سلنیوم"),
        "thiamine_b1_deficiency" to DiseaseAgeProfile(minWeeks = 1, maxWeeks = 6, peakStartWeeks = 1, peakEndWeeks = 4, differentialKeySign = "ستاره‌نگری و انقباض عضلات گردن رو به آسمان", deficiencyRemedy = "محلول ویتامین B1 (تیامین)"),
        "ascites_water_belly" to DiseaseAgeProfile(minWeeks = 3, maxWeeks = 8, peakStartWeeks = 4, peakEndWeeks = 7, differentialKeySign = "اتساع شکم پر از مایع زردرنگ در جوجه‌های پررشد سردخانه"),
        "sudden_death_syndrome_sds" to DiseaseAgeProfile(minWeeks = 2, maxWeeks = 8, peakStartWeeks = 3, peakEndWeeks = 6, differentialKeySign = "سکته ناگهانی و افتادن به پشت در جوجه‌های بسیار درشت"),

        // عوارض مختص سن بلوغ و تخم‌گذاری (بالای ۱۸ هفته)
        "egg_drop_syndrome" to DiseaseAgeProfile(minWeeks = 18, maxWeeks = 150, peakStartWeeks = 24, peakEndWeeks = 60, isStrictAgeLimited = true, differentialKeySign = "افت ناگهانی تخم‌مرغ بدون علائم عمومی حاد و تخم‌های بدون پوسته"),
        "vent_prolapse_laying" to DiseaseAgeProfile(minWeeks = 18, maxWeeks = 150, peakStartWeeks = 22, peakEndWeeks = 50, isStrictAgeLimited = true, differentialKeySign = "بیرون‌زدگی کلواک و مجرای تخم‌بر ناشی از تخم درشت یا چاقی"),
        "egg_binding" to DiseaseAgeProfile(minWeeks = 18, maxWeeks = 150, peakStartWeeks = 20, peakEndWeeks = 60, isStrictAgeLimited = true, differentialKeySign = "زور زدن مداوم و راه رفتن پنگوئنی ناشی از گیر کردن تخم"),
        "fatty_liver_syndrome" to DiseaseAgeProfile(minWeeks = 20, maxWeeks = 150, peakStartWeeks = 30, peakEndWeeks = 75, isStrictAgeLimited = true, differentialKeySign = "مرگ ناگهانی مرغ تخمگذار چاق همراه با هماتوم و پارگی کبد"),

        // اردک و غاز نوزاد
        "duck_viral_hepatitis" to DiseaseAgeProfile(minWeeks = 0, maxWeeks = 4, peakStartWeeks = 0, peakEndWeeks = 2, isStrictAgeLimited = true, differentialKeySign = "مرگ سریع چند ساعته جوجه اردک زیر ۳ هفته با کشیدگی سر به عقب")
    )

    fun getAgeProfile(diseaseId: String): DiseaseAgeProfile {
        return AGE_PROFILES[diseaseId] ?: DiseaseAgeProfile()
    }

    /**
     * Evaluates symptoms with clinical age-awareness and nutritional deficiency discrimination.
     */
    fun diagnose(
        species: BirdSpecies = BirdSpecies.CHICKEN,
        selectedSymptomIds: Set<String>,
        flockAgeWeeks: Int? = null,
        flockAgeDays: Int? = null,
        growthStage: GrowthStage? = null
    ): List<DiagnosticResult> {
        if (selectedSymptomIds.isEmpty()) return emptyList()

        val effectiveAgeWeeks: Int? = when {
            flockAgeWeeks != null -> flockAgeWeeks
            flockAgeDays != null -> (flockAgeDays / 7).coerceAtLeast(0)
            growthStage != null && growthStage != GrowthStage.ALL_STAGES -> (growthStage.minWeeks + growthStage.maxWeeks) / 2
            else -> null
        }

        val candidateDiseases = DiseaseCatalog.getDiseasesForSpecies(species)
        val results = mutableListOf<DiagnosticResult>()

        for (disease in candidateDiseases) {
            val ageProfile = getAgeProfile(disease.id)

            // ارزیابی تطابق زیستی سن پرنده
            if (effectiveAgeWeeks != null && ageProfile.isStrictAgeLimited) {
                // اگر بیماری اکیداً وابسته به سن باشد (مانند تخم‌گذاری یا نوزادی) و سن خارج از بازه باشد
                if (effectiveAgeWeeks < ageProfile.minWeeks || effectiveAgeWeeks > ageProfile.maxWeeks) {
                    continue // به طور کامل حذف می‌شود زیرا وقوع آن از نظر زیستی غیرممکن است
                }
            }

            var earnedScore = 0.0
            var maxPossibleScoreForDisease = 0.0

            for (symId in disease.pathognomonicSymptomIds) {
                maxPossibleScoreForDisease += 5.0
                if (selectedSymptomIds.contains(symId)) {
                    earnedScore += 5.0
                }
            }

            for (symId in disease.primarySymptomIds) {
                maxPossibleScoreForDisease += 3.0
                if (selectedSymptomIds.contains(symId)) {
                    earnedScore += 3.0
                }
            }

            for (symId in disease.secondarySymptomIds) {
                maxPossibleScoreForDisease += 1.5
                if (selectedSymptomIds.contains(symId)) {
                    earnedScore += 1.5
                }
            }

            if (earnedScore > 0.0 && maxPossibleScoreForDisease > 0.0) {
                var rawMatchRatio = earnedScore / maxPossibleScoreForDisease

                val totalSelected = selectedSymptomIds.size
                val matchedCount = disease.pathognomonicSymptomIds.count { selectedSymptomIds.contains(it) } +
                        disease.primarySymptomIds.count { selectedSymptomIds.contains(it) } +
                        disease.secondarySymptomIds.count { selectedSymptomIds.contains(it) }

                val precisionRatio = matchedCount.toDouble() / totalSelected.coerceAtLeast(1)
                var blendedProbability = (rawMatchRatio * 0.65 + precisionRatio * 0.35) * 100.0

                var ageNote = ""
                if (effectiveAgeWeeks != null) {
                    if (effectiveAgeWeeks in ageProfile.peakStartWeeks..ageProfile.peakEndWeeks) {
                        // سن دقیقاً در پنجره اوج شیوع قرار دارد -> تقویت احتمال
                        blendedProbability *= 1.25
                        ageNote = "منطبق بر سن گله (هفته $effectiveAgeWeeks - اوج شیوع)"
                    } else if (effectiveAgeWeeks < ageProfile.minWeeks || effectiveAgeWeeks > ageProfile.maxWeeks) {
                        // سن خارج از محدوده معمول است -> جریمه ملایم
                        blendedProbability *= 0.60
                        ageNote = "نامعمول برای سن گله (هفته $effectiveAgeWeeks)"
                    } else {
                        ageNote = "در محدوده سنی محتمل (هفته $effectiveAgeWeeks)"
                    }
                } else if (growthStage != null && growthStage != GrowthStage.ALL_STAGES) {
                    ageNote = "مرحله: ${growthStage.titleFa}"
                }

                val finalPercent = blendedProbability.toInt().coerceIn(5, 98)

                val matchedSymptomNames = DiseaseCatalog.allSymptoms
                    .filter { selectedSymptomIds.contains(it.id) &&
                            (disease.pathognomonicSymptomIds.contains(it.id) ||
                             disease.primarySymptomIds.contains(it.id) ||
                             disease.secondarySymptomIds.contains(it.id)) }
                    .map { it.titleFa }

                val keySign = ageProfile.differentialKeySign.ifBlank {
                    disease.pathognomonicSymptomIds.firstOrNull()?.let { pId ->
                        DiseaseCatalog.allSymptoms.find { it.id == pId }?.titleFa
                    } ?: disease.primarySymptomIds.firstOrNull()?.let { pId ->
                        DiseaseCatalog.allSymptoms.find { it.id == pId }?.titleFa
                    } ?: disease.definitiveDiagnosisFa.take(60)
                }

                results.add(
                    DiagnosticResult(
                        disease = disease,
                        probabilityPercent = finalPercent,
                        matchedSymptoms = matchedSymptomNames,
                        ageCompatibilityNoteFa = ageNote,
                        differentialKeySignFa = keySign,
                        isNutritional = (disease.pathogenType == PathogenType.NUTRITIONAL_METABOLIC)
                    )
                )
            }
        }

        return results.sortedByDescending { it.probabilityPercent }
    }

    /**
     * Conducts full Avian Veterinary Differential Diagnosis consultation.
     */
    fun consultVeterinarian(
        species: BirdSpecies = BirdSpecies.CHICKEN,
        selectedSymptomIds: Set<String>,
        flockAgeWeeks: Int? = null,
        flockAgeDays: Int? = null,
        growthStage: GrowthStage? = null
    ): VeterinaryConsultation {
        val rankedResults = diagnose(
            species = species,
            selectedSymptomIds = selectedSymptomIds,
            flockAgeWeeks = flockAgeWeeks,
            flockAgeDays = flockAgeDays,
            growthStage = growthStage
        )

        val top = rankedResults.firstOrNull()
        val differentials = rankedResults.drop(1).take(3)
        val isNutritional = top?.isNutritional ?: false

        val effectiveAgeWeeks: Int? = when {
            flockAgeWeeks != null -> flockAgeWeeks
            flockAgeDays != null -> (flockAgeDays / 7).coerceAtLeast(0)
            growthStage != null && growthStage != GrowthStage.ALL_STAGES -> (growthStage.minWeeks + growthStage.maxWeeks) / 2
            else -> null
        }

        val ageStr = if (effectiveAgeWeeks != null) "هفته $effectiveAgeWeeks" else "سن عمومی"
        val topDiseaseName = top?.disease?.nameFa ?: "عارضه بالینی نامشخص"

        val clinicalReasoning = if (top != null) {
            val nature = if (isNutritional) "کمبود مواد مغذی و متابولیک" else "عفونت بالینی ${top.disease.pathogenType.titleFa}"
            val ageFactor = if (top.ageCompatibilityNoteFa.isNotBlank()) " و ${top.ageCompatibilityNoteFa}" else ""
            "با بررسی نشانه‌های شنیده‌شده ($ageStr)$ageFactor، محتمل‌ترین تشخیص به عنوان $nature، عارضه «$topDiseaseName» با ${top.probabilityPercent}٪ تطابق بالینی است."
        } else {
            "نشانه‌های بیان‌شده با الگوی قطعی هیچ یک از بیماری‌های شاخص مطابقت کامل ندارد و نیاز به پایش بیشتر دارد."
        }

        val immediateAction = if (top != null) {
            if (isNutritional) {
                val remedy = getAgeProfile(top.disease.id).deficiencyRemedy ?: "افزودن مکمل و پرمیکس ویتامینه به آب و دان"
                "اقدام تغذیه‌ای فوری: ۱) $remedy. ۲) بازبینی فرمول جیره و نسبت کلسیم به فسفر. ۳) جداسازی پرندگان آسیب‌دیده زیر نور ملایم."
            } else {
                "اقدامات اورژانسی گله: ۱) قرنطینه سالن و کاهش تراکم. ۲) بهینه‌سازی تهویه و افزایش دمای سالن به میزان ۲ درجه. ۳) ضدعفونی خطوط آبخوری."
            }
        } else {
            "توصیه: بهبود شرایط بستر و آبخوری‌ها و در صورت افزایش علائم، تماس با دامپزشک فارم."
        }

        val rxText = if (top != null) {
            val d = top.disease
            "${d.commonDrugsFa} (دوز پیشنهادی: ${d.dosagesFa})"
        } else {
            "فاقد نسخه اولیه تا ثبت علائم تفکیکی"
        }

        val followUps = mutableListOf<String>()
        if (top != null && differentials.isNotEmpty()) {
            val diffNames = differentials.map { it.disease.nameFa }.joinToString(" یا ")
            followUps.add("جهت تفکیک از $diffNames، به علامت ممیزه «${top.differentialKeySignFa}» دقت فرمایید.")
        }
        followUps.add("آیا در مدفوع پرنده تغییر رنگ یا خونریزی مشاهده می‌کنید؟")

        return VeterinaryConsultation(
            topResult = top,
            differentialCandidates = differentials,
            isNutritionalDeficiency = isNutritional,
            evaluatedAgeWeeks = effectiveAgeWeeks,
            clinicalReasoningFa = clinicalReasoning,
            immediateEmergencyActionFa = immediateAction,
            firstLineRxFa = rxText,
            followUpQuestionsFa = followUps
        )
    }
}

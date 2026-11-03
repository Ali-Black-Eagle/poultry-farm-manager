package com.example.poultryapp.data.model

import com.example.poultryapp.data.model.pro_stages.*

/**
 * کاتالوگ جامع دوره فوق‌تخصصی و بورد استادی فارم (فصل ۲)
 * شامل ۲۲۵ مرحله فوق‌حرفه‌ای (۲۵ مرحله برای هر یک از ۹ گونه پرنده)
 * تفکیک شده بر اساس ۵ محور تخصصی دامپزشکی، تهویه، بیوشیمی تغذیه، امنیت زیستی و مگافارم
 */
object ProfessionalStagesData {

    val layerHenProStages: List<GoldenStage> = LayerHenProStages.stages
    val broilerProStages: List<GoldenStage> = BroilerProStages.stages
    val quailProStages: List<GoldenStage> = QuailProStages.stages
    val turkeyProStages: List<GoldenStage> = TurkeyProStages.stages
    val waterfowlProStages: List<GoldenStage> = WaterfowlProStages.stages
    val cockatielProStages: List<GoldenStage> = CockatielProStages.stages
    val partridgeProStages: List<GoldenStage> = PartridgeProStages.stages
    val ostrichProStages: List<GoldenStage> = OstrichProStages.stages
    val guineaFowlProStages: List<GoldenStage> = GuineaFowlProStages.stages
    val canaryProStages: List<GoldenStage> = CanaryProStages.stages
    val pigeonProStages: List<GoldenStage> = PigeonProStages.stages
    val muscovyDuckProStages: List<GoldenStage> = MuscovyDuckProStages.stages

    fun getProStagesForBird(birdType: GoldenBirdType): List<GoldenStage> {
        return when (birdType) {
            GoldenBirdType.LAYER_HEN -> layerHenProStages
            GoldenBirdType.BROILER -> broilerProStages
            GoldenBirdType.QUAIL -> quailProStages
            GoldenBirdType.TURKEY -> turkeyProStages
            GoldenBirdType.WATERFOWL -> waterfowlProStages
            GoldenBirdType.COCKATIEL -> cockatielProStages
            GoldenBirdType.PARTRIDGE -> partridgeProStages
            GoldenBirdType.OSTRICH -> ostrichProStages
            GoldenBirdType.GUINEA_FOWL -> guineaFowlProStages
            GoldenBirdType.CANARY -> canaryProStages
            GoldenBirdType.PIGEON -> pigeonProStages
            GoldenBirdType.MUSCOVY_DUCK -> muscovyDuckProStages
        }
    }
}

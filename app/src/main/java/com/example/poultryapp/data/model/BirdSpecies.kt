package com.example.poultryapp.data.model

enum class BirdSpecies(val titleFa: String, val latinName: String) {
    CHICKEN("مرغ", "Chicken"),
    GUINEA_FOWL("مرغ شاخدار", "Guinea Fowl"),
    DUCK("اردک", "Duck"),
    TURKEY("بوقلمون", "Turkey"),
    GOOSE("غاز", "Goose"),
    QUAIL("بلدرچین", "Quail");

    companion object {
        fun fromString(value: String): BirdSpecies {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.titleFa == value } ?: CHICKEN
        }
    }
}

enum class ProductionType(val titleFa: String) {
    MEAT("گوشتی"),
    LAYER("تخمگذار"),
    DUAL("دومنظوره"),
    NATIVE("بومی"),
    ORNAMENTAL("زینتی")
}

data class BreedOption(
    val nameFa: String,
    val species: BirdSpecies,
    val productionType: ProductionType
)

object BreedCatalog {

    val chickenBreeds = listOf(
        BreedOption("آمروک", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("تخمگذار لگهورن", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("هایلاین", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("ال‌اس‌ال", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("بلک استار", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("لوهمن قهوه‌ای", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("ایزابرون", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("آراکانا", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("لهستانی", BirdSpecies.CHICKEN, ProductionType.LAYER),
        BreedOption("گوشتی راس", BirdSpecies.CHICKEN, ProductionType.MEAT),
        BreedOption("کاب", BirdSpecies.CHICKEN, ProductionType.MEAT),
        BreedOption("آربر ایکرز", BirdSpecies.CHICKEN, ProductionType.MEAT),
        BreedOption("برهما", BirdSpecies.CHICKEN, ProductionType.ORNAMENTAL),
        BreedOption("استرالوپ", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("پلیموت راک", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("رود آیلند رد", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("سوسکس", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("کوشین", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("گلپایگانی", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("مرندی", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("لاری (اصیل)", BirdSpecies.CHICKEN, ProductionType.NATIVE),
        BreedOption("مرغ شاخدار (گینه)", BirdSpecies.CHICKEN, ProductionType.DUAL),
        BreedOption("بومی/محلی", BirdSpecies.CHICKEN, ProductionType.NATIVE)
    )

    val guineaFowlBreeds = listOf(
        BreedOption("مرواریدی (خاکستری)", BirdSpecies.GUINEA_FOWL, ProductionType.DUAL),
        BreedOption("سفید", BirdSpecies.GUINEA_FOWL, ProductionType.DUAL),
        BreedOption("اسطوخودوسی (یاسی)", BirdSpecies.GUINEA_FOWL, ProductionType.DUAL),
        BreedOption("ابلق (پایبالد)", BirdSpecies.GUINEA_FOWL, ProductionType.DUAL),
        BreedOption("گوشتی تجاری", BirdSpecies.GUINEA_FOWL, ProductionType.MEAT),
        BreedOption("بومی", BirdSpecies.GUINEA_FOWL, ProductionType.NATIVE)
    )

    val duckBreeds = listOf(
        BreedOption("پکینی", BirdSpecies.DUCK, ProductionType.MEAT),
        BreedOption("خاکی کمپبل", BirdSpecies.DUCK, ProductionType.LAYER),
        BreedOption("مسکووی", BirdSpecies.DUCK, ProductionType.MEAT),
        BreedOption("روئن", BirdSpecies.DUCK, ProductionType.DUAL),
        BreedOption("رانر هندی", BirdSpecies.DUCK, ProductionType.LAYER),
        BreedOption("ملارد (کله‌سبز)", BirdSpecies.DUCK, ProductionType.NATIVE),
        BreedOption("بومی", BirdSpecies.DUCK, ProductionType.NATIVE)
    )

    val turkeyBreeds = listOf(
        BreedOption("بیوتی ۶ (BUT 6)", BirdSpecies.TURKEY, ProductionType.MEAT),
        BreedOption("نیکولاس", BirdSpecies.TURKEY, ProductionType.MEAT),
        BreedOption("برنز آمریکایی", BirdSpecies.TURKEY, ProductionType.MEAT),
        BreedOption("سفید هلندی (برود برستد وایت)", BirdSpecies.TURKEY, ProductionType.MEAT),
        BreedOption("بومی", BirdSpecies.TURKEY, ProductionType.NATIVE)
    )

    val gooseBreeds = listOf(
        BreedOption("امدن", BirdSpecies.GOOSE, ProductionType.MEAT),
        BreedOption("تولوز", BirdSpecies.GOOSE, ProductionType.MEAT),
        BreedOption("چینی", BirdSpecies.GOOSE, ProductionType.DUAL),
        BreedOption("بومی", BirdSpecies.GOOSE, ProductionType.NATIVE)
    )

    val quailBreeds = listOf(
        BreedOption("ژاپنی (کوتورنیکس)", BirdSpecies.QUAIL, ProductionType.LAYER),
        BreedOption("تگزاس آمبر (سفید)", BirdSpecies.QUAIL, ProductionType.MEAT),
        BreedOption("باب‌وایت", BirdSpecies.QUAIL, ProductionType.MEAT),
        BreedOption("بومی", BirdSpecies.QUAIL, ProductionType.NATIVE)
    )

    fun getBreedsForSpecies(
        species: BirdSpecies,
        customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList()
    ): List<BreedOption> {
        val defaults = when (species) {
            BirdSpecies.CHICKEN -> chickenBreeds
            BirdSpecies.GUINEA_FOWL -> guineaFowlBreeds
            BirdSpecies.DUCK -> duckBreeds
            BirdSpecies.TURKEY -> turkeyBreeds
            BirdSpecies.GOOSE -> gooseBreeds
            BirdSpecies.QUAIL -> quailBreeds
        }
        val customs = customBreeds.filter { it.species == species }.map {
            BreedOption(
                nameFa = it.name,
                species = it.species,
                productionType = it.productionType
            )
        }
        return customs + defaults
    }

    fun getProductionType(
        species: BirdSpecies,
        breedName: String,
        customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList()
    ): ProductionType {
        val customMatch = customBreeds.find { it.species == species && it.name.trim().equals(breedName.trim(), ignoreCase = true) }
        if (customMatch != null) return customMatch.productionType
        val all = getBreedsForSpecies(species, customBreeds)
        val match = all.find { it.nameFa.equals(breedName.trim(), ignoreCase = true) }
        return match?.productionType ?: when {
            breedName.contains("برهما") || breedName.contains("زینتی") -> ProductionType.ORNAMENTAL
            breedName.contains("گوشتی") || breedName.contains("راس") || breedName.contains("کاب") || breedName.contains("پکینی") || breedName.contains("برنز") || breedName.contains("بیوتی") || breedName.contains("نیکولاس") || breedName.contains("تگزاس") -> ProductionType.MEAT
            breedName.contains("تخمگذار") || breedName.contains("هایلاین") || breedName.contains("لگهورن") || breedName.contains("کمپبل") || breedName.contains("بلک استار") || breedName.contains("لوهمن") || breedName.contains("ایزا") || breedName.contains("آراکانا") || breedName.contains("لهستانی") -> ProductionType.LAYER
            breedName.contains("بومی") || breedName.contains("محلی") || breedName.contains("لاری") || breedName.contains("ملارد") -> ProductionType.NATIVE
            else -> ProductionType.DUAL
        }
    }
}

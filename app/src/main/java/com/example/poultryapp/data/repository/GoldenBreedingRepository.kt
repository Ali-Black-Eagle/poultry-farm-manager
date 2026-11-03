package com.example.poultryapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.poultryapp.data.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * مدیریت ذخیره‌سازی محلی پیشرفت بازی به تفکیک ۹ گونه پرنده
 * پشتیبانی کامل از فصل ۱، بورد فوق‌تخصصی فصل ۲، و تالار افتخارات (لوح‌های زرین)
 * بدون سیستم سکه و فروشگاه
 */
class GoldenBreedingRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "golden_breeding_academy_prefs",
        Context.MODE_PRIVATE
    )

    private fun maxStageKey(bird: GoldenBirdType) = "max_stage_${bird.id}"
    private fun stageStarsKey(bird: GoldenBirdType, stageId: Int) = "stars_${bird.id}_$stageId"
    private fun stageCompletedKey(bird: GoldenBirdType, stageId: Int) = "completed_${bird.id}_$stageId"
    private fun cardUnlockedKey(bird: GoldenBirdType, stageId: Int) = "card_${bird.id}_$stageId"

    /**
     * بالاترین شماره مرحله آزاد شده برای یک پرنده در فصل ۱
     */
    fun getMaxUnlockedStageNumber(birdType: GoldenBirdType): Int {
        return prefs.getInt(maxStageKey(birdType), 1).coerceAtLeast(1)
    }

    /**
     * وضعیت پیشرفت یک مرحله خاص در فصل ۱
     */
    fun getStageProgress(birdType: GoldenBirdType, stage: GoldenStage): StageProgress {
        val maxUnlocked = getMaxUnlockedStageNumber(birdType)
        val isUnlocked = stage.stageNumber <= maxUnlocked
        val isCompleted = prefs.getBoolean(stageCompletedKey(birdType, stage.id), false)
        val stars = prefs.getInt(stageStarsKey(birdType, stage.id), 0)
        val isCardUnlocked = prefs.getBoolean(cardUnlockedKey(birdType, stage.id), isCompleted)

        return StageProgress(
            stageId = stage.id,
            isUnlocked = isUnlocked,
            isCompleted = isCompleted,
            stars = stars,
            isCardUnlocked = isCardUnlocked
        )
    }

    /**
     * ثبت پایان موفق یک مرحله (بدون اعطای سکه)
     */
    fun completeStage(birdType: GoldenBirdType, stage: GoldenStage, earnedStars: Int): StageProgress {
        val clampedStars = earnedStars.coerceIn(1, 3)
        val currentStars = prefs.getInt(stageStarsKey(birdType, stage.id), 0)
        val bestStars = maxOf(currentStars, clampedStars)

        val editor = prefs.edit()
        editor.putInt(stageStarsKey(birdType, stage.id), bestStars)
        editor.putBoolean(stageCompletedKey(birdType, stage.id), true)
        editor.putBoolean(cardUnlockedKey(birdType, stage.id), true)

        val currentMax = getMaxUnlockedStageNumber(birdType)
        val stagesList = GoldenBreedingCatalog.getStagesForBird(birdType, GameSeasonTier.NOVICE)
        if (stage.stageNumber >= currentMax && stage.stageNumber < stagesList.size) {
            editor.putInt(maxStageKey(birdType), stage.stageNumber + 1)
        }
        editor.apply()

        checkAndAwardMedals(birdType)

        return getStageProgress(birdType, stage)
    }

    private fun sideQuestCompletedKey(bird: GoldenBirdType, questId: String) = "sidequest_${bird.id}_$questId"
    private fun sideQuestStarsKey(bird: GoldenBirdType, questId: String) = "sidequest_stars_${bird.id}_$questId"
    private fun rewardClaimedKey(rewardId: String) = "reward_claimed_$rewardId"

    /**
     * دریافت ستاره‌های کسب‌شده در یک ساید کوئست (۰ تا ۳ ستاره)
     */
    fun getSideQuestStars(bird: GoldenBirdType, questId: String): Int {
        val stars = prefs.getInt(sideQuestStarsKey(bird, questId), 0)
        if (stars > 0) return stars
        // سازگاری با سوابق قبلی که فقط به صورت بولی ذخیره شده بودند
        return if (prefs.getBoolean(sideQuestCompletedKey(bird, questId), false)) 3 else 0
    }

    /**
     * بررسی اتمام یک ساید کوئست
     */
    fun isSideQuestCompleted(bird: GoldenBirdType, questId: String): Boolean {
        return getSideQuestStars(bird, questId) > 0 || prefs.getBoolean(sideQuestCompletedKey(bird, questId), false)
    }

    /**
     * دریافت نقشه ستاره‌های تمام ساید کوئست‌های یک پرنده برای دسترسی O(1) سریع
     */
    fun getAllSideQuestStars(bird: GoldenBirdType): Map<String, Int> {
        val sideQuests = GoldenBreedingCatalog.getSideQuestsForBird(bird)
        return sideQuests.associate { it.id to getSideQuestStars(bird, it.id) }
    }

    /**
     * تکمیل موفق ساید کوئست همراه با ثبت ستاره‌های کسب‌شده (۱ تا ۳ ستاره)
     */
    fun completeSideQuest(bird: GoldenBirdType, quest: GoldenSideQuest, earnedStars: Int = 3) {
        val editor = prefs.edit()
        val currentStars = getSideQuestStars(bird, quest.id)
        val bestStars = maxOf(currentStars, earnedStars.coerceIn(1, 3))
        editor.putInt(sideQuestStarsKey(bird, quest.id), bestStars)
        editor.putBoolean(sideQuestCompletedKey(bird, quest.id), true)
        editor.apply()
        checkAndAwardMedals(bird)
    }

    /**
     * بررسی دریافت پاداش نقطه عطف
     */
    fun isRewardClaimed(rewardId: String): Boolean {
        return prefs.getBoolean(rewardClaimedKey(rewardId), false)
    }

    /**
     * دریافت پاداش و ثبت در گنجه
     */
    fun claimReward(reward: GoldenFarmReward) {
        val editor = prefs.edit()
        editor.putBoolean(rewardClaimedKey(reward.id), true)
        editor.apply()
    }

    /**
     * دریافت لیست جوایز به دست آمده برای یک پرنده
     */
    fun getUnlockedRewards(birdType: GoldenBirdType): List<GoldenFarmReward> {
        val maxUnlocked = getMaxUnlockedStageNumber(birdType)
        val allRewards = GoldenBreedingCatalog.getRewardsForBird(birdType)
        return allRewards.filter { it.unlockStageNumber <= maxUnlocked }
    }

    /**
     * بررسی اینکه آیا مرحله تکمیل شده جایزه عطف جدیدی آزاد می‌کند
     */
    fun checkNewMilestoneReward(birdType: GoldenBirdType, completedStageNumber: Int): GoldenFarmReward? {
        val allRewards = GoldenBreedingCatalog.getRewardsForBird(birdType)
        return allRewards.find { it.unlockStageNumber == completedStageNumber && !isRewardClaimed(it.id) }
    }

    /**
     * خلاصه پیشرفت پرنده در فصل ۱ (با تفکیک دقیق ستاره‌های مراحل اصلی و ستاره‌های سبز مراحل فرعی)
     */
    fun getBirdProgressSummary(birdType: GoldenBirdType): BirdProgressSummary {
        val stages = GoldenBreedingCatalog.getStagesForBird(birdType, GameSeasonTier.NOVICE)
        val sideQuests = GoldenBreedingCatalog.getSideQuestsForBird(birdType)
        val rewards = GoldenBreedingCatalog.getRewardsForBird(birdType)

        var mainStars = 0
        var completedCount = 0
        var threeStarCount = 0
        var unlockedCardsCount = 0

        for (stage in stages) {
            val progress = getStageProgress(birdType, stage)
            mainStars += progress.stars
            if (progress.isCompleted) completedCount++
            if (progress.isCompleted && progress.stars == 3) threeStarCount++
            if (progress.isCardUnlocked) unlockedCardsCount++
        }

        var sqEarnedStars = 0
        var completedSideQuests = 0
        for (sq in sideQuests) {
            val sqStars = getSideQuestStars(birdType, sq.id)
            sqEarnedStars += sqStars
            if (sqStars > 0 || isSideQuestCompleted(birdType, sq.id)) completedSideQuests++
        }

        val maxUnlocked = getMaxUnlockedStageNumber(birdType)
        val unlockedRewardsCount = rewards.count { it.unlockStageNumber <= maxUnlocked }

        val rankTitle = when {
            completedCount >= stages.size -> "🏆 استاد پرورش ${birdType.titleFa}"
            completedCount >= (stages.size * 3 / 4) -> "🌟 پرورش‌دهنده ممتاز و کارآزموده"
            completedCount >= (stages.size / 2) -> "🐥 سرپرست مجرب فارم"
            completedCount >= (stages.size / 4) -> "🌾 کارآموز فعال"
            else -> "🐣 نوپا در پرورش"
        }

        val combinedTotalStars = mainStars + sqEarnedStars
        val combinedMaxStars = (stages.size + sideQuests.size) * 3

        return BirdProgressSummary(
            birdType = birdType,
            totalStars = combinedTotalStars,
            maxPossibleStars = combinedMaxStars,
            sideQuestStars = sqEarnedStars,
            maxPossibleSideQuestStars = sideQuests.size * 3,
            currentActiveStageNumber = maxUnlocked,
            completedStagesCount = completedCount,
            threeStarStagesCount = threeStarCount,
            unlockedCardsCount = unlockedCardsCount,
            completedSideQuestsCount = completedSideQuests,
            unlockedRewardsCount = unlockedRewardsCount,
            farmCoins = 0,
            masteryTitleFa = rankTitle
        )
    }

    /**
     * تعداد مراحل ۳ ستاره شده در فصل مقدماتی
     */
    fun getNoviceThreeStarCount(bird: GoldenBirdType): Int {
        val stages = GoldenBreedingCatalog.getStagesForBird(bird, GameSeasonTier.NOVICE)
        return stages.count { stage ->
            val progress = getStageProgress(bird, stage)
            progress.isCompleted && progress.stars == 3
        }
    }

    /**
     * بررسی اینکه آیا تمام مراحل فصل مقدماتی ۳ ستاره شده‌اند
     */
    fun isAllNoviceStagesThreeStarred(bird: GoldenBirdType): Boolean {
        val stages = GoldenBreedingCatalog.getStagesForBird(bird, GameSeasonTier.NOVICE)
        if (stages.isEmpty()) return false
        return stages.all { stage ->
            val progress = getStageProgress(bird, stage)
            progress.isCompleted && progress.stars == 3
        }
    }

    /**
     * دریافت کارت‌های طلایی باز شده یک پرنده
     */
    fun getUnlockedCards(birdType: GoldenBirdType): List<KnowledgeCard> {
        val stages = GoldenBreedingCatalog.getStagesForBird(birdType)
        val list = mutableListOf<KnowledgeCard>()
        for (stage in stages) {
            val p = getStageProgress(birdType, stage)
            if (p.isCardUnlocked || p.isCompleted) {
                list.add(stage.goldenCard)
            }
        }
        return list
    }

    /**
     * تمام کارت‌های طلایی در کل آکادمی
     */
    fun getAllCards(): List<KnowledgeCard> {
        val all = mutableListOf<KnowledgeCard>()
        for (bird in GoldenBirdType.values()) {
            all.addAll(getUnlockedCards(bird))
        }
        return all
    }

    // -------------------------------------------------------------
    // ۱. مدیریت پیشرفت فصل فوق‌حرفه‌ای (Professional Season - بورد تخصصی)
    // -------------------------------------------------------------
    private fun proUnlockedKey(bird: GoldenBirdType) = "pro_season_unlocked_${bird.id}"
    private fun proMaxStageKey(bird: GoldenBirdType) = "pro_max_stage_${bird.id}"
    private fun proStageStarsKey(bird: GoldenBirdType, stageId: Int) = "pro_stars_${bird.id}_$stageId"
    private fun proStageCompletedKey(bird: GoldenBirdType, stageId: Int) = "pro_completed_${bird.id}_$stageId"

    /**
     * قفل فصل حرفه‌ای تنها بعد از ۳ ستاره کردن تمام مراحل فصل مقدماتی همان پرنده باز می‌شود
     */
    fun isProSeasonUnlocked(bird: GoldenBirdType): Boolean {
        return isAllNoviceStagesThreeStarred(bird) || prefs.getBoolean(proUnlockedKey(bird), false)
    }

    fun unlockProSeason(bird: GoldenBirdType): Boolean {
        prefs.edit().putBoolean(proUnlockedKey(bird), true).apply()
        return true
    }

    fun getProMaxUnlockedStageNumber(bird: GoldenBirdType): Int {
        return prefs.getInt(proMaxStageKey(bird), 1).coerceAtLeast(1)
    }

    fun getProStageProgress(bird: GoldenBirdType, stage: GoldenStage): StageProgress {
        val maxUnlocked = getProMaxUnlockedStageNumber(bird)
        val isUnlocked = stage.stageNumber <= maxUnlocked && isProSeasonUnlocked(bird)
        val isCompleted = prefs.getBoolean(proStageCompletedKey(bird, stage.id), false)
        val stars = prefs.getInt(proStageStarsKey(bird, stage.id), 0)

        return StageProgress(
            stageId = stage.id,
            isUnlocked = isUnlocked,
            isCompleted = isCompleted,
            stars = stars,
            isCardUnlocked = isCompleted
        )
    }

    fun completeProStage(bird: GoldenBirdType, stage: GoldenStage, earnedStars: Int): StageProgress {
        val clampedStars = earnedStars.coerceIn(1, 3)
        val currentStars = prefs.getInt(proStageStarsKey(bird, stage.id), 0)
        val bestStars = maxOf(currentStars, clampedStars)

        val editor = prefs.edit()
        editor.putInt(proStageStarsKey(bird, stage.id), bestStars)
        editor.putBoolean(proStageCompletedKey(bird, stage.id), true)

        val currentMax = getProMaxUnlockedStageNumber(bird)
        val proStagesList = GoldenBreedingCatalog.getProStagesForBird(bird)
        if (stage.stageNumber >= currentMax && stage.stageNumber < proStagesList.size) {
            editor.putInt(proMaxStageKey(bird), stage.stageNumber + 1)
        }
        editor.apply()

        // ارزیابی اتمام تمام مراحل برای اعطای لوح زرین افتخار و مدال‌ها
        checkAndAwardPlaques(bird)
        checkAndAwardMedals(bird)

        return getProStageProgress(bird, stage)
    }

    fun getProBirdProgressSummary(bird: GoldenBirdType): BirdProgressSummary {
        val stages = GoldenBreedingCatalog.getProStagesForBird(bird)
        var totalStars = 0
        var completedCount = 0

        for (stage in stages) {
            val progress = getProStageProgress(bird, stage)
            totalStars += progress.stars
            if (progress.isCompleted) completedCount++
        }

        val maxUnlocked = getProMaxUnlockedStageNumber(bird)

        val rankTitle = when {
            completedCount >= stages.size -> "🎖️ دارنده لوح زرین بورد استادی ${bird.titleFa}"
            completedCount >= (stages.size * 3 / 4) -> "🔬 پژوهشگر و مشاور ارشد فارم"
            completedCount >= (stages.size / 2) -> "🧪 متخصص بالینی فارم صنعتی"
            completedCount >= 1 -> "📑 داوطلب بورد تخصصی"
            else -> "👑 آماده ورود به بورد فوق‌حرفه‌ای"
        }

        return BirdProgressSummary(
            birdType = bird,
            totalStars = totalStars,
            maxPossibleStars = stages.size * 3,
            currentActiveStageNumber = maxUnlocked,
            completedStagesCount = completedCount,
            unlockedCardsCount = completedCount,
            completedSideQuestsCount = 0,
            unlockedRewardsCount = 0,
            farmCoins = 0,
            masteryTitleFa = rankTitle
        )
    }

    // -------------------------------------------------------------
    // ۲. تالار افتخارات و لوح‌های زرین بورد تخصصی (Golden Plaques Hall of Fame)
    // -------------------------------------------------------------
    private fun plaqueUnlockedKey(plaqueId: String) = "plaque_unlocked_$plaqueId"
    private fun plaqueDateKey(plaqueId: String) = "plaque_date_$plaqueId"

    fun isPlaqueUnlocked(plaqueId: String): Boolean {
        return prefs.getBoolean(plaqueUnlockedKey(plaqueId), false)
    }

    fun getPlaqueUnlockedDate(plaqueId: String): String? {
        return prefs.getString(plaqueDateKey(plaqueId), null)
    }

    fun unlockPlaque(plaqueId: String) {
        if (!isPlaqueUnlocked(plaqueId)) {
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            prefs.edit()
                .putBoolean(plaqueUnlockedKey(plaqueId), true)
                .putString(plaqueDateKey(plaqueId), dateStr)
                .apply()
        }
    }

    /**
     * ارزیابی اعطای خودکار لوح پرنده پس از اتمام ۲۵ مرحله
     */
    private fun checkAndAwardPlaques(bird: GoldenBirdType) {
        val proStages = GoldenBreedingCatalog.getProStagesForBird(bird)
        val allCompleted = proStages.all { stage ->
            prefs.getBoolean(proStageCompletedKey(bird, stage.id), false)
        }

        if (allCompleted) {
            val plaqueId = "plaque_${bird.id}"
            unlockPlaque(plaqueId)

            // بررسی دانشنامه استاد اعظم (اتمام تمام ۹ پرنده)
            val allBirdsCompleted = GoldenBirdType.values().all { b ->
                isPlaqueUnlocked("plaque_${b.id}")
            }
            if (allBirdsCompleted) {
                unlockPlaque("plaque_grand_master")
            }
        }
    }

    /**
     * دریافت لیست ۱۰ لوح افتخار به همراه وضعیت آزاد شدن و تاریخ
     */
    fun getAllPlaques(): List<GoldenPlaque> {
        return GoldenBreedingCatalog.allPlaques.map { plaque ->
            val unlocked = isPlaqueUnlocked(plaque.id)
            val date = getPlaqueUnlockedDate(plaque.id)
            plaque.copy(
                isUnlocked = unlocked,
                unlockedDateFa = date
            )
        }
    }

    /**
     * تعداد لوح‌های زرین آزاد شده
     */
    fun getUnlockedPlaquesCount(): Int {
        return GoldenBreedingCatalog.allPlaques.count { isPlaqueUnlocked(it.id) }
    }

    // -------------------------------------------------------------
    // ۳. مدیریت مدال‌های افتخار آکادمی (Medals System)
    // -------------------------------------------------------------
    private fun medalUnlockedKey(medalId: String) = "medal_unlocked_$medalId"
    private fun medalDateKey(medalId: String) = "medal_date_$medalId"

    fun isMedalUnlocked(medalId: String): Boolean {
        return prefs.getBoolean(medalUnlockedKey(medalId), false)
    }

    fun getMedalUnlockedDate(medalId: String): String? {
        return prefs.getString(medalDateKey(medalId), null)
    }

    fun unlockMedal(medalId: String) {
        if (!isMedalUnlocked(medalId)) {
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            prefs.edit()
                .putBoolean(medalUnlockedKey(medalId), true)
                .putString(medalDateKey(medalId), dateStr)
                .apply()
        }
    }

    fun getMedalsForBird(bird: GoldenBirdType, season: GameSeasonTier): List<GoldenMedal> {
        val summary = if (season == GameSeasonTier.NOVICE) getBirdProgressSummary(bird) else getProBirdProgressSummary(bird)
        val stages = if (season == GameSeasonTier.NOVICE) GoldenBreedingCatalog.getStagesForBird(bird, GameSeasonTier.NOVICE) else GoldenBreedingCatalog.getProStagesForBird(bird)
        val sideQuests = if (season == GameSeasonTier.NOVICE) GoldenBreedingCatalog.getSideQuestsForBird(bird) else emptyList()
        val novice3StarCount = getNoviceThreeStarCount(bird)

        val proStages = GoldenBreedingCatalog.getProStagesForBird(bird)
        val completedProCount = proStages.count { getProStageProgress(bird, it).isCompleted }
        val threeStarProCount = proStages.count {
            val p = getProStageProgress(bird, it)
            p.isCompleted && p.stars == 3
        }

        val datesMap = mutableMapOf<String, String>()
        for (category in MedalCategory.values()) {
            val mId = when (category) {
                MedalCategory.SIDE_QUEST -> "medal_sq_${bird.id}"
                MedalCategory.NOVICE_CYCLE -> "medal_novice_${bird.id}"
                MedalCategory.MASTERY_3STAR -> "medal_mastery_${bird.id}"
                MedalCategory.PRO_PATHOLOGY -> "medal_pro_pathology_${bird.id}"
                MedalCategory.PRO_CLIMATE -> "medal_pro_climate_${bird.id}"
                MedalCategory.PRO_GRANDMASTER -> "medal_pro_grandmaster_${bird.id}"
            }
            getMedalUnlockedDate(mId)?.let { datesMap[mId] = it }
        }

        val medals = GoldenMedalCatalog.generateMedals(
            birdType = bird,
            seasonTier = season,
            completedStagesCount = summary.completedStagesCount,
            totalStagesCount = stages.size,
            threeStarStagesCount = novice3StarCount,
            completedSideQuestsCount = summary.completedSideQuestsCount,
            totalSideQuestsCount = sideQuests.size,
            completedProStagesCount = completedProCount,
            threeStarProStagesCount = threeStarProCount,
            unlockedDates = datesMap
        )

        for (medal in medals) {
            if (medal.isUnlocked && !isMedalUnlocked(medal.id)) {
                unlockMedal(medal.id)
            }
        }

        return medals
    }

    fun getUnlockedMedalsCount(bird: GoldenBirdType): Int {
        val novice = getMedalsForBird(bird, GameSeasonTier.NOVICE)
        val pro = getMedalsForBird(bird, GameSeasonTier.PROFESSIONAL)
        return novice.count { it.isUnlocked } + pro.count { it.isUnlocked }
    }

    fun getUnlockedMedalsCountForSeason(bird: GoldenBirdType, season: GameSeasonTier): Int {
        return getMedalsForBird(bird, season).count { it.isUnlocked }
    }

    fun getTotalUnlockedMedalsCount(): Int {
        return GoldenBirdType.values().sumOf { getUnlockedMedalsCount(it) }
    }

    fun checkAndAwardMedals(bird: GoldenBirdType) {
        getMedalsForBird(bird, GameSeasonTier.NOVICE)
        getMedalsForBird(bird, GameSeasonTier.PROFESSIONAL)
    }

    /**
     * بوسترهای کمکی چالش‌ها (بدون نیاز به سکه و فروشگاه)
     */
    private fun boosterKey(boosterId: String) = "booster_count_$boosterId"

    fun getBoosterCount(boosterId: String): Int {
        return prefs.getInt(boosterKey(boosterId), 3)
    }

    fun useBooster(boosterId: String): Boolean {
        val current = getBoosterCount(boosterId)
        if (current > 0) {
            prefs.edit().putInt(boosterKey(boosterId), current - 1).apply()
            return true
        }
        return false
    }

    fun isFacilityOwned(facilityId: String): Boolean = true

    companion object {
        const val BOOSTER_HINT = "golden_hint"
        const val BOOSTER_STAR_SHIELD = "star_shield"
        const val BOOSTER_REROLL = "reroll_chance"
        const val BOOSTER_DIAGNOSTIC_KIT = "diagnostic_kit"

        const val FACILITY_IOT_SENSORS = "facility_iot_sensors"
        const val FACILITY_PATHOLOGY_LAB = "facility_pathology_lab"
        const val FACILITY_VET_ARCHIVE = "facility_vet_archive"
    }
}

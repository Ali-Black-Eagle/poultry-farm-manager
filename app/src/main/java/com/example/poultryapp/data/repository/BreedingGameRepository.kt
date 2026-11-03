package com.example.poultryapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.poultryapp.data.model.BreedingGameCatalog
import com.example.poultryapp.data.model.FarmerGameProfile
import com.example.poultryapp.data.model.KnowledgeCard
import com.example.poultryapp.data.model.StageProgress

/**
 * مدیریت ذخیره‌سازی محلی پیشرفت کاربر در بازی پرورش طیور
 */
class BreedingGameRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("poultry_breeding_game_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MAX_UNLOCKED_STAGE = "key_max_unlocked_stage"
        private const val KEY_STAGE_STARS_PREFIX = "key_stage_stars_"
        private const val KEY_STAGE_COMPLETED_PREFIX = "key_stage_completed_"
        private const val KEY_CARD_UNLOCKED_PREFIX = "key_card_unlocked_"
    }

    /**
     * دریافت بالاترین مرحله آنلاک شده (حداقل مرحله ۱ همیشه باز است)
     */
    fun getMaxUnlockedStage(): Int {
        return prefs.getInt(KEY_MAX_UNLOCKED_STAGE, 1).coerceAtLeast(1)
    }

    /**
     * دریافت وضعیت پیشرفت یک مرحله
     */
    fun getStageProgress(stageId: Int): StageProgress {
        val maxUnlocked = getMaxUnlockedStage()
        val isUnlocked = stageId <= maxUnlocked
        val isCompleted = prefs.getBoolean(KEY_STAGE_COMPLETED_PREFIX + stageId, false)
        val stars = prefs.getInt(KEY_STAGE_STARS_PREFIX + stageId, 0)
        val isCardUnlocked = prefs.getBoolean(KEY_CARD_UNLOCKED_PREFIX + stageId, isCompleted)

        return StageProgress(
            stageId = stageId,
            isUnlocked = isUnlocked,
            isCompleted = isCompleted,
            stars = stars,
            isCardUnlocked = isCardUnlocked
        )
    }

    /**
     * دریافت وضعیت تمام مراحل بازی
     */
    fun getAllStagesProgress(): Map<Int, StageProgress> {
        val map = mutableMapOf<Int, StageProgress>()
        for (stage in BreedingGameCatalog.stages) {
            map[stage.id] = getStageProgress(stage.id)
        }
        return map
    }

    /**
     * ثبت پایان موفق یک مرحله و باز شدن مرحله بعدی
     */
    fun completeStage(stageId: Int, earnedStars: Int): StageProgress {
        val clampedStars = earnedStars.coerceIn(1, 3)
        val currentStars = prefs.getInt(KEY_STAGE_STARS_PREFIX + stageId, 0)
        val bestStars = maxOf(currentStars, clampedStars)

        val editor = prefs.edit()
        editor.putInt(KEY_STAGE_STARS_PREFIX + stageId, bestStars)
        editor.putBoolean(KEY_STAGE_COMPLETED_PREFIX + stageId, true)
        editor.putBoolean(KEY_CARD_UNLOCKED_PREFIX + stageId, true)

        // باز کردن مرحله بعدی در صورت وجود
        val currentMaxUnlocked = getMaxUnlockedStage()
        if (stageId >= currentMaxUnlocked && stageId < BreedingGameCatalog.stages.size) {
            editor.putInt(KEY_MAX_UNLOCKED_STAGE, stageId + 1)
        }
        editor.apply()

        return getStageProgress(stageId)
    }

    /**
     * دریافت نمایه کلی کاربر و دستاوردها
     */
    fun getFarmerProfile(): FarmerGameProfile {
        val allProgress = getAllStagesProgress()
        var totalStars = 0
        var completedCount = 0
        var unlockedCardsCount = 0

        for ((_, p) in allProgress) {
            totalStars += p.stars
            if (p.isCompleted) completedCount++
            if (p.isCardUnlocked) unlockedCardsCount++
        }

        val rankTitle = when {
            completedCount >= 15 -> "🏆 استاد ارشد فارم و پرورش"
            completedCount >= 12 -> "🌟 متخصص ارشد تولید و تخم‌گذاری"
            completedCount >= 8 -> "🐓 مرغدار ماهر و باتجربه"
            completedCount >= 4 -> "🐥 پرورش‌دهنده کوشا"
            completedCount >= 1 -> "🐣 کارآموز فعال مزرعه"
            else -> "🌾 نوپای مشتاق یادگیری"
        }

        return FarmerGameProfile(
            totalStars = totalStars,
            maxPossibleStars = BreedingGameCatalog.stages.size * 3,
            currentActiveStageId = getMaxUnlockedStage(),
            completedStagesCount = completedCount,
            farmerRankTitleFa = rankTitle,
            unlockedCardsCount = unlockedCardsCount
        )
    }

    /**
     * دریافت لیست تمام کارت‌های طلایی آنلاک شده
     */
    fun getUnlockedKnowledgeCards(): List<KnowledgeCard> {
        val unlockedCards = mutableListOf<KnowledgeCard>()
        for (stage in BreedingGameCatalog.stages) {
            val progress = getStageProgress(stage.id)
            if (progress.isCardUnlocked || progress.isCompleted) {
                unlockedCards.add(stage.goldenCard)
            }
        }
        return unlockedCards
    }

    /**
     * ریست کردن بازی (برای تست یا شروع مجدد اختیاری کاربر)
     */
    fun resetGame() {
        prefs.edit().clear().apply()
    }
}

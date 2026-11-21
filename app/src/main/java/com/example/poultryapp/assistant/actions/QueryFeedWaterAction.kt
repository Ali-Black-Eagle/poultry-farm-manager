package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.FeedWaterRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.LiveConsumptionTracker
import com.example.poultryapp.engine.toPersianDigits

data class QueryFeedWaterParams(
    val flockId: Long?,
    val flockName: String?,
    val itemType: String?
)

class QueryFeedWaterAction(
    private val feedWaterRepo: FeedWaterRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<QueryFeedWaterParams> {

    override val intent: IntentId = IntentId.QUERY_FEED_WATER

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryFeedWaterParams {
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        val type = slots[SlotId.FEED_WATER_TYPE]?.typedValue?.toString()
        return QueryFeedWaterParams(flockId, flockName, type)
    }

    override suspend fun validate(params: QueryFeedWaterParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryFeedWaterParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام مصرف دان و آب",
            titleEn = "Feed & Water Status",
            fields = listOf("سالن" to (params.flockName ?: "همه سالن‌ها"))
        )
    }

    override suspend fun execute(params: QueryFeedWaterParams): ActionResult {
        val flocks = if (params.flockId != null) {
            val f = flockRepo.getFlockById(params.flockId)
            if (f != null) listOf(f) else flockRepo.getAllFlocks()
        } else {
            flockRepo.getAllFlocks()
        }

        if (flocks.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "هیچ سالن یا گله فعالی برای استعلام دان و آب ثبت نشده است.",
                messageEn = "No active flocks found to query feed and water."
            )
        }

        val sbFa = StringBuilder()
        sbFa.append("📊 وضعیت دان و آب گله‌ها:\n")

        for (flock in flocks) {
            val feedTodayGrams = feedWaterRepo.getTodayChargesTotal(flock.id, ChargeItemType.FEED)
            val waterTodayMl = feedWaterRepo.getTodayChargesTotal(flock.id, ChargeItemType.WATER)

            val feedFormatted = DailyNutritionEngine.formatFeed(feedTodayGrams)
            val waterFormatted = DailyNutritionEngine.formatWater(waterTodayMl)

            val dailyReq = DailyNutritionEngine.calculateFlockRequirement(flock = flock)
            val latestFeed = feedWaterRepo.getLatestCharge(flock.id, ChargeItemType.FEED)
            val latestWater = feedWaterRepo.getLatestCharge(flock.id, ChargeItemType.WATER)

            val feedStatus = LiveConsumptionTracker.calculateLiveStatus(
                itemType = ChargeItemType.FEED,
                chargedAmount = latestFeed?.chargedAmount ?: 0.0,
                chargeTimestampMillis = latestFeed?.chargeTimestampMillis ?: 0L,
                dailyRequirementTotal = dailyReq.totalFeedGrams
            )
            val waterStatus = LiveConsumptionTracker.calculateLiveStatus(
                itemType = ChargeItemType.WATER,
                chargedAmount = latestWater?.chargedAmount ?: 0.0,
                chargeTimestampMillis = latestWater?.chargeTimestampMillis ?: 0L,
                dailyRequirementTotal = dailyReq.totalWaterMl
            )

            sbFa.append("• ${flock.name} (${toPersianDigits(flock.effectiveBirdCount)} پرنده):\n")
            sbFa.append("  - شارژ دان امروز: $feedFormatted (مانده در منبع: ${feedStatus.remainingAmountFa})\n")
            sbFa.append("  - شارژ آب امروز: $waterFormatted (مانده در منبع: ${waterStatus.remainingAmountFa})\n")
        }

        return ActionResult(
            success = true,
            messageFa = sbFa.toString().trim(),
            messageEn = "Feed and water report generated for ${flocks.size} flocks."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

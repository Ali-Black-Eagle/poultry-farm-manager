package com.example.poultryapp.assistant.actions

import android.content.Context
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.data.repository.ScheduleRepository
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.receiver.AlarmScheduler

data class AddScheduleParams(
    val title: String,
    val scheduleType: String,
    val hour: Int,
    val minute: Int,
    val flockId: Long?,
    val flockName: String?
)

class AddScheduleAction(
    private val context: Context,
    private val scheduleRepo: ScheduleRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<AddScheduleParams> {

    override val intent: IntentId = IntentId.ADD_SCHEDULE

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): AddScheduleParams {
        val title = slots[SlotId.NOTE]?.typedValue?.toString()
            ?: slots[SlotId.MEDICATION_NAME]?.typedValue?.toString()
            ?: "یادآور فارم"
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        val type = if (title.contains("واکسن")) "واکسن"
        else if (title.contains("ویتامین")) "ویتامین"
        else if (title.contains("ضدعفونی")) "ضدعفونی"
        else "دارو"

        val timePair = (slots[SlotId.SCHEDULE_TIME]?.typedValue as? Pair<*, *>)?.let { (h, m) ->
            if (h is Int && m is Int) Pair(h, m) else null
        }
        val hour = timePair?.first ?: 9
        val minute = timePair?.second ?: 0

        return AddScheduleParams(
            title = title,
            scheduleType = type,
            hour = hour,
            minute = minute,
            flockId = flockId,
            flockName = flockName
        )
    }

    override suspend fun validate(params: AddScheduleParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: AddScheduleParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "تنظیم یادآور و تقویم فارم",
            titleEn = "Add Schedule Task",
            fields = listOf(
                "عنوان" to params.title,
                "نوع" to params.scheduleType,
                "ساعت" to "${toPersianDigits("%02d".format(params.hour))}:${toPersianDigits("%02d".format(params.minute))}",
                "سالن" to (params.flockName ?: "همه سالن‌ها")
            )
        )
    }

    override suspend fun execute(params: AddScheduleParams): ActionResult {
        val targetFlockId = params.flockId ?: flockRepo.getAllFlocks().firstOrNull()?.id
        val targetFlockName = params.flockName ?: flockRepo.getAllFlocks().firstOrNull()?.name

        val sched = CalendarSchedule(
            flockId = targetFlockId,
            flockName = targetFlockName,
            scheduleType = params.scheduleType,
            title = params.title,
            hour = params.hour,
            minute = params.minute,
            daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6), // Every day by default
            isEnabled = true
        )

        val schedId = scheduleRepo.insertSchedule(sched)
        val created = sched.copy(id = schedId)
        AlarmScheduler.scheduleWeeklyAlarm(context, created)

        val msgFa = "یادآور '${params.title}' برای ساعت ${toPersianDigits("%02d".format(params.hour))}:${toPersianDigits("%02d".format(params.minute))} در تقویم فارم ثبت و هشدار گوشی تنظیم شد."
        val msgEn = "Scheduled task '${params.title}' at ${params.hour}:${params.minute}."

        return ActionResult(
            success = true,
            messageFa = msgFa,
            messageEn = msgEn,
            createdRecordId = schedId,
            undoToken = "SCHEDULE:$schedId"
        )
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("SCHEDULE:")) return false
        val id = undoToken.removePrefix("SCHEDULE:").toLongOrNull() ?: return false
        AlarmScheduler.cancelScheduleAlarms(context, id)
        return scheduleRepo.deleteSchedule(id)
    }
}

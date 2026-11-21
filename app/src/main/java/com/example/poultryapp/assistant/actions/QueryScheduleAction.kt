package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.ScheduleRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import java.util.Calendar

class QueryScheduleAction(
    private val scheduleRepo: ScheduleRepository
) : AssistantAction<Unit> {

    override val intent: IntentId = IntentId.QUERY_SCHEDULE

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult = ValidationResult.Valid

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام برنامه‌ها و یادآورها",
            titleEn = "Query Schedule",
            fields = listOf("تقویم" to "برنامه‌های فارم")
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val schedules = scheduleRepo.getAllSchedules().filter { it.isEnabled }
        if (schedules.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "هیچ برنامه یا یادآور فعالی در تقویم فارم ثبت نشده است.",
                messageEn = "No active scheduled tasks found."
            )
        }

        val cal = Calendar.getInstance()
        val todayShamsiDayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        val todayJalali = JalaliDateHelper.getJalaliDate()

        val todayTasks = schedules.filter {
            if (it.scheduleMode == "MONTHLY") {
                it.dayOfMonth == todayJalali.day
            } else {
                it.daysOfWeek.contains(todayShamsiDayIndex)
            }
        }.sortedBy { it.hour * 60 + it.minute }

        val sb = StringBuilder()
        if (todayTasks.isNotEmpty()) {
            sb.append("📋 برنامه‌های امروز فارم (${todayJalali.dayName} ${toPersianDigits(todayJalali.day)} ${todayJalali.monthName} ${toPersianDigits(todayJalali.year)}):\n")
            for (s in todayTasks) {
                val flockStr = if (s.flockName != null) " [${s.flockName}]" else ""
                sb.append("• ساعت ${toPersianDigits(s.timeDisplayFa)}: ${s.title} (${s.scheduleType})$flockStr\n")
            }
            sb.append("\n📌 سایر برنامه‌های فعال هفته:\n")
            val otherTasks = schedules.filter { !todayTasks.contains(it) }
            for (s in otherTasks) {
                val flockStr = if (s.flockName != null) " [${s.flockName}]" else ""
                sb.append("• ${s.daysDisplayFa} ساعت ${toPersianDigits(s.timeDisplayFa)}: ${s.title}$flockStr\n")
            }
        } else {
            sb.append("📅 برای امروز برنامه‌ای ثبت نشده است. برنامه‌های فعال هفته (${toPersianDigits(schedules.size)} مورد):\n")
            for (s in schedules) {
                val flockStr = if (s.flockName != null) " [${s.flockName}]" else ""
                sb.append("• ${s.daysDisplayFa} ساعت ${toPersianDigits(s.timeDisplayFa)}: ${s.title} (${s.scheduleType})$flockStr\n")
            }
        }

        return ActionResult(
            success = true,
            messageFa = sb.toString().trim(),
            messageEn = "Found ${schedules.size} active schedules (${todayTasks.size} for today)."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

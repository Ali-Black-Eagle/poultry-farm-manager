package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.SettingsRepository
import com.example.poultryapp.engine.toPersianDigits

class QueryWeatherAction(
    private val settingsRepo: SettingsRepository
) : AssistantAction<Unit> {

    override val intent: IntentId = IntentId.QUERY_WEATHER

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult = ValidationResult.Valid

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام اقلیم و تهویه فارم",
            titleEn = "Query Weather",
            fields = listOf("فارم" to "وضعیت اقلیمی و تهویه")
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val s = settingsRepo.getSettings()
        val temp = s.manualTempCelsius ?: s.cachedTempCelsius
        val humidity = s.cachedHumidityPercent
        val city = if (s.farmCity.isNotBlank()) s.farmCity else if (s.farmProvince.isNotBlank()) s.farmProvince else "فارم"

        if (temp == null) {
            return ActionResult(
                success = true,
                messageFa = "اطلاعات دمایی فارم هنوز دریافت نشده است. می‌توانید در تنظیمات شهر خود را مشخص کرده یا دمای دستی سالن را وارد نمایید.",
                messageEn = "No weather information available."
            )
        }

        val sb = StringBuilder()
        sb.append("🌤️ وضعیت آب و هوای $city:\n")
        sb.append("• دمای محیط: ${toPersianDigits(temp.toInt())} درجه سانتی‌گراد\n")
        if (humidity != null) {
            sb.append("• رطوبت نسبی: ${toPersianDigits(humidity)} درصد\n")
        }

        // Ventilation recommendation
        sb.append("\n🌬️ توصیه تهویه و مدیریت سالن:\n")
        when {
            temp >= 32.0 -> {
                sb.append("⚠️ هشدار استرس گرمایی شدید! پدهای سلولزی و مه‌پاش را فعال کنید، هوادهی حداکثری و افزایش ویتامین C و الکترولیت در آب توصیه می‌شود.")
            }
            temp >= 28.0 -> {
                sb.append("دمای سالن بالا است. افزایش هوادهی، بررسی عملکرد فن‌ها و خنک نگه داشتن آب آشامیدنی ضروری است.")
            }
            temp in 18.0..24.0 -> {
                sb.append("✅ شرایط دمایی در محدوده آسایش پرندگان است. حفظ تهویه حداقلی و کنترل رطوبت بستر کافی است.")
            }
            else -> {
                sb.append("دمای سالن خنک است. سیستم‌های گرمایشی (هیترها) را بررسی کرده و از ورود کوران هوای سرد به داخل سالن جلوگیری فرمایید.")
            }
        }

        return ActionResult(
            success = true,
            messageFa = sb.toString().trim(),
            messageEn = "Current farm temperature is $temp C."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}


package com.example.poultryapp.assistant.core.dialogue

import java.time.LocalDate

sealed interface SafetyResult {
    object Passed : SafetyResult
    data class Warning(val warningMessage: String) : SafetyResult
    data class Rejected(val reasonMessage: String) : SafetyResult
}

object SafetyChecks {

    fun validateEggCount(
        count: Int,
        activeFlockSize: Int?,
        median14Days: Double? = null
    ): SafetyResult {
        if (count <= 0) {
            return SafetyResult.Rejected("تعداد تخم‌مرغ باید بزرگتر از صفر باشد.")
        }

        if (activeFlockSize != null && count > (activeFlockSize * 1.10)) {
            return SafetyResult.Warning("تعداد ثبت شده ($count) بیشتر از ظرفیت گله ($activeFlockSize قطعه) است. آیا تایید می‌کنید؟")
        }

        if (median14Days != null && median14Days > 0.0 && count > (median14Days * 3.0)) {
            return SafetyResult.Warning("این مقدار بسیار بیشتر از میانگین معمول (${median14Days.toInt()}) است. ذخیره شود؟")
        }

        return SafetyResult.Passed
    }

    fun validateMoneyAmount(
        amount: Double,
        categoryMedian: Double? = null,
        sanityMaxLimit: Double = 500_000_000.0 // 500 million Tomans
    ): SafetyResult {
        if (amount <= 0.0) {
            return SafetyResult.Rejected("مبلغ تراکنش باید بزرگتر از صفر باشد.")
        }

        if (amount > sanityMaxLimit) {
            return SafetyResult.Warning("مبلغ ثبت شده ($amount تومان) بالاتر از سقف معمول فارم است. آیا مطمئن هستید؟")
        }

        if (categoryMedian != null && categoryMedian > 0.0 && amount > (categoryMedian * 10.0)) {
            return SafetyResult.Warning("این مبلغ به طور چشمگیری بالاتر از میانگین این سرفصل است. آیا تایید می‌کنید؟")
        }

        return SafetyResult.Passed
    }

    fun validateDate(date: LocalDate, now: LocalDate = LocalDate.now()): SafetyResult {
        if (date.isAfter(now)) {
            return SafetyResult.Rejected("تاریخ نمی‌تواند در آینده باشد.")
        }
        if (date.isBefore(now.minusDays(60))) {
            return SafetyResult.Warning("تاریخ ثبت شده مربوط به بیش از ۶۰ روز قبل است. آیا تایید می‌کنید؟")
        }
        return SafetyResult.Passed
    }
}


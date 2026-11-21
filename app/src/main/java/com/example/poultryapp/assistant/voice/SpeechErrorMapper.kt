package com.example.poultryapp.assistant.voice

enum class AttemptStage {
    DEFAULT_HYBRID,
    GOOGLE_SERVICE,
    OFFLINE_RETRY
}

object SpeechErrorMapper {

    fun isLanguageUnsupported(errorCode: Int): Boolean {
        return errorCode == 12 || errorCode == 13 || errorCode == 14
    }

    fun isNetworkError(errorCode: Int): Boolean {
        return errorCode == 1 || errorCode == 2
    }

    fun isSpeechTimeoutOrNoMatch(errorCode: Int): Boolean {
        return errorCode == 6 || errorCode == 7
    }

    fun nextFallbackStage(
        errorCode: Int,
        currentStage: AttemptStage,
        hasGoogleService: Boolean,
        hasMoreGoogleCandidates: Boolean = false
    ): AttemptStage? {
        return when {
            isLanguageUnsupported(errorCode) &&
                currentStage == AttemptStage.DEFAULT_HYBRID &&
                hasGoogleService -> AttemptStage.GOOGLE_SERVICE

            isLanguageUnsupported(errorCode) &&
                currentStage == AttemptStage.GOOGLE_SERVICE &&
                hasMoreGoogleCandidates -> AttemptStage.GOOGLE_SERVICE

            isNetworkError(errorCode) &&
                (currentStage == AttemptStage.DEFAULT_HYBRID || currentStage == AttemptStage.GOOGLE_SERVICE) ->
                AttemptStage.OFFLINE_RETRY

            else -> null
        }
    }

    fun messageFor(errorCode: Int, customMessage: String? = null): String {
        return when (errorCode) {
            1 -> "مهلت اتصال به سرور تشخیص گفتار به پایان رسید."
            2 -> "خطای اتصال به اینترنت برای تشخیص گفتار."
            3 -> "خطای دریافت صدا از میکروفون."
            4 -> "خطای موقت سرویس تشخیص گفتار گوگل."
            5 -> "خطای داخلی تشخیص گفتار. لطفاً دوباره امتحان کنید."
            6 -> "صدایی دریافت نشد. لطفاً نزدیک‌تر به میکروفون صحبت کنید."
            7 -> "گفتار قابل تشخیص نبود. لطفاً واضح‌تر صحبت کنید."
            8 -> "سرویس تشخیص گفتار مشغول است. لحظه‌ای صبر کنید و دوباره امتحان کنید."
            9 -> "دسترسی میکروفون داده نشده است. لطفاً مجوز میکروفون را فعال کنید."
            10 -> "تعداد درخواست‌ها زیاد بود. لحظه‌ای صبر کنید و دوباره امتحان کنید."
            11 -> "ارتباط با سرویس تشخیص گفتار قطع شد. لطفاً دوباره امتحان کنید."
            12 -> "سرویس فعلی از زبان فارسی پشتیبانی نمی‌کند. می‌توانید از میکروفون سیستم استفاده کنید."
            13 -> "بستهٔ زبان فارسی روی سرویس فعلی در دسترس نیست. می‌توانید از میکروفون سیستم استفاده کنید."
            14 -> "امکان بررسی پشتیبانی زبان فارسی وجود ندارد."
            -1 -> customMessage?.takeIf { it.isNotBlank() } ?: "سرویس تشخیص گفتار روی این دستگاه فعال نیست."
            else -> "خطای تشخیص صوت (کد: $errorCode)"
        }
    }
}


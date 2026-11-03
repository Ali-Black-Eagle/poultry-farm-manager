package com.example.poultryapp.voice

/**
 * درگاه هوشمند ارتباط با API آنلاین هوش مصنوعی برای پاسخ‌دهی به سوالات دانشی و عمومی کاربر
 * (Online AI API Question-Answering Bridge)
 * این ماژول امکان ارسال سوالات علمی، پاتولوژی، جیره‌نویسی و تحلیلی کاربر به API ابری دلخواه
 * را بدون تداخل با هسته ایجنتیک آفلاین فراهم می‌سازد.
 */
interface AiQuestionAnswerBridge {
    /**
     * ارسال سوال کاربر به API هوش مصنوعی و دریافت پاسخ متنی
     *
     * @param question متن سوال نرمال‌شده یا خام کاربر
     * @param farmSummary خلاصه‌ای از آمار و زمینه زنده فارم در صورت نیاز جهت ارسال به مدل
     * @return پاسخ تشخیصی یا مشاوره‌ای دریافت شده از API
     */
    suspend fun answerQuestion(question: String, farmSummary: String? = null): String

    /**
     * نسخه همگام (Synchronous) جهت فراخوانی در بخش‌های غیرهمگام یا موتورهای پردازشی
     */
    fun answerQuestionSync(question: String, farmSummary: String? = null): String
}

/**
 * پیاده‌سازی پیش‌فرض درگاه هوش مصنوعی
 * این شیء آماده اتصال به کلاینت‌های HTTP، مدل‌های ابری یا SDK دلخواه کاربر است.
 * در صورتی که کاربر هنوز کلید API را وارد نکرده باشد، پیامی راهنما و استاندارد صادر می‌کند.
 */
object DefaultAiQuestionAnswerBridge : AiQuestionAnswerBridge {

    @Volatile
    private var customHandler: ((String, String?) -> String)? = null

    /**
     * تنظیم هندلر یا کلاینت دلخواه API برای دریافت مستقیم پاسخ از سرور/مدل آنلاین
     */
    fun setApiHandler(handler: (question: String, farmSummary: String?) -> String) {
        customHandler = handler
    }

    /**
     * پاک‌سازی هندلر سفارشی و بازگشت به حالت پیش‌فرض
     */
    fun resetToDefault() {
        customHandler = null
    }

    override suspend fun answerQuestion(question: String, farmSummary: String?): String {
        return answerQuestionSync(question, farmSummary)
    }

    override fun answerQuestionSync(question: String, farmSummary: String?): String {
        val handler = customHandler
        if (handler != null) {
            return try {
                handler(question, farmSummary)
            } catch (e: Exception) {
                "خطا در برقراری ارتباط با API هوش مصنوعی: ${e.localizedMessage ?: "پاسخی دریافت نشد"}. لطفاً اتصال اینترنت خود را بررسی نمایید."
            }
        }

        // پیام راهنمای پیش‌فرض در زمان آماده‌سازی زیرساخت جهت اتصال به API کاربر
        return "این پرسش در حوزه دانش و مشاوره تخصصی قرار دارد و برای پاسخ‌دهی به API آنلاین هوش مصنوعی ارجاع خواهد شد.\n" +
                "«سیستم آماده دریافت کلید و آدرس API شما برای پاسخ‌دهی هوشمند ابری است»."
    }
}


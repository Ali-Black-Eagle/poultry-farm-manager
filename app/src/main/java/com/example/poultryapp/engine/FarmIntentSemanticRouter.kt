package com.example.poultryapp.engine

import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.voice.PersianVoiceNormalizer

/**
 * دامنه‌های استنتاجی روتینگ معنایی هوش مصنوعی فارم (Hybrid Semantic Routing Taxonomy)
 * تفکیک دقیق فرامین عملیاتی آفلاین از سوالات ارجاع‌شونده به API آنلاین همراه با سپر ضد توکن‌سوزی
 */
enum class FarmSemanticIntent {
    OPERATIONAL_RECORD,          // فرامین عملیاتی ثبت، جهش در دیتابیس فارم و ویرایش داده‌ها (آفلاین)
    APP_NAVIGATION,              // ناوبری و جابجایی صوتی بین صفحات اپلیکیشن (آفلاین)
    SMALL_TALK_OR_GREETING,      // تعارفات، سلام، احوال‌پرسی و هویت (آفلاین بدون توکن‌سوزی)
    CANCEL_OR_DISMISS,           // لغو، انصراف یا خداحافظی (آفلاین)
    FLOCK_LIVE_STATS_QUERY,      // استعلام وضعیت آماری زنده سالن یا فارم از دیتابیس (آفلاین)
    KNOWLEDGE_API_QUERY          // سوالات علمی، پاتولوژی، جیره و عمومی ارجاع‌شونده به API آنلاین
}

/**
 * ارکستراتور و روتِر معنایی قصد کاربر (Farm Intent Semantic Router)
 * این ماژول مرز بین فرامین عملیاتی محلی، تعاملات روزمره (ضد توکن‌سوزی) و پرسش‌های آنلاین API را تعیین می‌کند.
 */
object FarmIntentSemanticRouter {

    fun isQuestionPattern(norm: String): Boolean {
        return norm.contains("چگونه") || norm.contains("چطور") || norm.contains("چطوری") ||
                norm.contains("کدام") || norm.contains("کدوم") ||
                (norm.contains("روش") && !norm.contains("فروش") && !norm.contains("سروش")) || norm.contains("طرز") || norm.contains("نحوه") || norm.contains("اصول") ||
                norm.contains("راهنمای") || norm.contains("آموزش") || norm.contains("چرا") || norm.contains("آیا") ||
                norm.contains("چند متر") || norm.contains("چند درجه") || norm.contains("چند روز") ||
                norm.contains("چقدر") || norm.contains("چند تا") || norm.contains("کی به") || norm.contains("کی تخم") ||
                norm.contains("ساخت") || norm.contains("درست کردن") || norm.contains("بسازیم") || norm.contains("بسازم") ||
                norm.contains("چه غذایی") || norm.contains("چه دانی") || norm.contains("چی بدم") || norm.contains("چی بخوره") || norm.contains("چه خوراکی") ||
                norm.contains("چه دارویی") || norm.contains("چه واکسنی") || norm.contains("چیکار کنم") || norm.contains("چه کار کنم") ||
                norm.contains("چه باید کرد") || norm.contains("درمان") ||
                ((norm.contains("علت") || norm.contains("دلیل")) && !norm.contains("به علت") && !norm.contains("به دلیل")) ||
                norm.contains("علائم") || norm.contains("نشانه") || norm.contains("راهکار") || norm.contains("چجوری") ||
                norm.contains("چیست") || norm.contains("کیست") || norm.contains("کجاست") || norm.contains("چه میزان") ||
                norm.contains("چه وقت") || norm.contains("تا کی") || norm.contains("چند درجه میخواد") ||
                norm.contains("چند درجه لازمه") || norm.contains("کی کشتار") || norm.contains("کی آماده میشه") ||
                norm.contains("کی تخم میاد") || norm.contains("کی تخم میذاره") || norm.contains("چرا تخم نمیکنه") ||
                norm.contains("چرا تخم نمیذارن") || norm.contains("چرا دان نمیخورن") || norm.contains("چرا آب نمیخورن") ||
                norm.contains("مشکل چیه") || norm.contains("علتش چیه") || norm.contains("دلیلش چیه") ||
                norm.contains("چی بهش بدم") || norm.contains("چی بهشون بدم") || norm.contains("چیکارش کنم")
    }

    /**
     * بررسی اینکه آیا کلام کاربر متوجه گله، سالن یا فارم شخصی خودش است
     */
    fun isPersonalOrFlockQuery(norm: String): Boolean {
        if (Regex("""(^|\s)من(\s|$)""").containsMatchIn(norm)) return true
        if (norm.contains("گله من") || norm.contains("سالن من") || norm.contains("فارم من") ||
            norm.contains("مرغ های من") || norm.contains("مرغهای من") || norm.contains("مرغام") || norm.contains("مرغ هام") ||
            norm.contains("گله های من") || norm.contains("گله هام") || norm.contains("سالن های من") || norm.contains("سالن هام") ||
            norm.contains("جوجه هام") || norm.contains("جوجه های من") || norm.contains("پرنده های من") || norm.contains("پرنده هام") ||
            norm.contains("حیوانات من") || norm.contains("فارم ما") || norm.contains("سالن ما") || norm.contains("گله ما")) {
            return true
        }
        if (Regex("""(سالن|گله)\s*(\d+|[۰-۹]+|یک|دو|سه|چهار|پنج)""").containsMatchIn(norm) ||
            norm.contains("سالن فعلی") || norm.contains("سالن اصلی") || norm.contains("گله فعلی")) {
            return true
        }
        if (norm.contains("سرد نیست") || norm.contains("گرم نیست") ||
            norm.contains("برق قطع") || norm.contains("ژنراتور") ||
            norm.contains("سنسور") || norm.contains("امروز چقدر") ||
            norm.contains("تلفات امروز") || norm.contains("تخم امروز")) {
            return true
        }
        return false
    }

    /**
     * الگوهای صریح یا اخباری ثبت عملیاتی فارم (جهش دیتابیس)
     */
    fun isOperationalActionPattern(norm: String): Boolean {
        if (isQuestionPattern(norm)) return false

        // افعال امری صریح
        val hasImperative = norm.contains("ثبت") || norm.contains("حذف") || norm.contains("اضافه کن") || norm.contains("وارد کن") ||
                norm.contains("بزن") || norm.contains("بنویس") || norm.contains("شارژ") || norm.contains("کم کن") ||
                norm.contains("انتقال") || norm.contains("منتقل") || norm.contains("جابجا") || norm.contains("زنگ بزن") ||
                norm.contains("تماس") || norm.contains("آلارم") || norm.contains("یادآور") || norm.contains("برو به") ||
                norm.contains("برو صفحه") || norm.contains("باز کن") || norm.contains("ایجاد گله") || norm.contains("تعریف گله") ||
                norm.contains("گله جدید") || (norm.contains("گله") && norm.contains("بساز")) ||
                norm.contains("تنظیم") || norm.contains("فعال کن") || norm.contains("فعال شود") || norm.contains("غیرفعال کن") || norm.contains("غیرفعال شود") || (norm.contains("پایش") && (norm.contains("فعال") || norm.contains("غیرفعال"))) ||
                ((norm.contains("درآمد") || norm.contains("فروش")) && (norm.contains("تومان") || norm.contains("مبلغ") || norm.contains("میلیون")))

        // افعال گذشته، اخباری، مجهول و گزارش‌های روزمره
        val hasPastOrDeclarative = norm.contains("دادیم") || norm.contains("دادم") || norm.contains("دادند") || norm.contains("داد") ||
                norm.contains("ریختیم") || norm.contains("ریختم") || norm.contains("ریختند") || norm.contains("ریخت") ||
                norm.contains("تلف شد") || norm.contains("تلف شدند") || norm.contains("تلف گردید") ||
                norm.contains("تلفات دادیم") || norm.contains("تلفات داشتیم") || norm.contains("تلفات خوردیم") ||
                norm.contains("مصرف شد") || norm.contains("مصرف کرد") || norm.contains("مصرف کردند") || norm.contains("مصرف کردیم") ||
                norm.contains("خریدیم") || norm.contains("خریدم") || norm.contains("خریدند") ||
                norm.contains("فروختیم") || norm.contains("فروختم") || norm.contains("فروختند") ||
                norm.contains("تخلیه شد") || norm.contains("تخلیه کردیم") || norm.contains("تخلیه شدیم") ||
                norm.contains("جمع کردیم") || norm.contains("جمع شد") || norm.contains("جمع آوری کردیم") ||
                norm.contains("مردن") || norm.contains("مردند") || norm.contains("مرد") ||
                norm.contains("تلف نشد") || norm.contains("تلف نشدن") || norm.contains("تلفات نداشتیم") ||
                norm.contains("تخم نداشتیم") || norm.contains("تخم مرغ نداشتیم") ||
                norm.contains("توزین کردیم") || norm.contains("وزن کشیدیم") || norm.contains("وزن کشی کردیم") ||
                norm.contains("واکسن زدیم") || norm.contains("دارو دادیم") || norm.contains("کولین") || norm.contains("کالینگ") || norm.contains("وازده")

        val hasActionContext = norm.contains("دان") || norm.contains("تخم") || norm.contains("تلفات") ||
                norm.contains("دما") || norm.contains("آب") || norm.contains("کیلو") || norm.contains("تن") ||
                norm.contains("شانه") || norm.contains("کارتن") || norm.contains("کیسه") || norm.contains("بسته") ||
                norm.contains("لیتر") || norm.contains("متر مکعب") || norm.contains("گرم") || norm.contains("راس") ||
                norm.contains("قطعه") || norm.contains("عدد") || norm.contains("سر") || norm.contains("سالن") ||
                norm.contains("گله") || norm.contains("وزن") || norm.contains("واکسن") || norm.contains("دارو")

        return (hasImperative || hasPastOrDeclarative) && hasActionContext
    }

    /**
     * بررسی فرامین ناوبری و هدایت صوتی به صفحات برنامه
     */
    fun isNavigationCommand(norm: String): Boolean {
        return norm.contains("برو به") || norm.contains("برو صفحه") || norm.contains("باز کن") ||
                norm.contains("نمایش بده") || norm.contains("صفحه رو بیار") || norm.contains("صفحه را باز کن")
    }

    /**
     * روتینگ هوشمند معنایی کلام کاربر
     */
    fun routeIntent(
        norm: String,
        spokenText: String,
        matchedFlock: Flock? = null
    ): FarmSemanticIntent {
        // ۱. بررسی لغو و انصراف یا خداحافظی
        if (norm.contains("خداحافظ") || norm.contains("فعلا") || norm.contains("شب خوش") ||
            norm.contains("بای") || (norm == "تمام" || norm == "خروج")) {
            return FarmSemanticIntent.CANCEL_OR_DISMISS
        }

        // ۲. تعارفات روزمره و سلام (سپر ضد توکن‌سوزی آفلاین)
        if (isGreetingOrSmallTalk(norm)) {
            return FarmSemanticIntent.SMALL_TALK_OR_GREETING
        }

        // ۳. فرامین ناوبری صوتی به صفحات برنامه
        if (isNavigationCommand(norm) && !isQuestionPattern(norm)) {
            return FarmSemanticIntent.APP_NAVIGATION
        }

        // ۴. فرامین عملیاتی ثبت یا ویرایش دیتابیس فارم (ایجنتیک آفلاین)
        if (isOperationalActionPattern(norm)) {
            return FarmSemanticIntent.OPERATIONAL_RECORD
        }

        // ۵. استعلام آمار زنده سالن‌ها و انبار فارم
        if (isFlockStatsQuery(norm)) {
            return FarmSemanticIntent.FLOCK_LIVE_STATS_QUERY
        }

        // ۶. سایر سوالات و تعاملات -> ارجاع مستقیم به API آنلاین هوش مصنوعی
        return FarmSemanticIntent.KNOWLEDGE_API_QUERY
    }

    private fun isGreetingOrSmallTalk(norm: String): Boolean {
        val trimmed = norm.trim()
        if (trimmed in listOf("سلام", "درود", "صبح بخیر", "عصر بخیر", "شب بخیر", "روز بخیر",
                "خسته نباشید", "خسته نباشی", "سلام علیکم", "سلام علیک", "یا علی",
                "ممنون", "متشکرم", "دستت درد نکنه", "دمت گرم", "عالی بود", "تشکر",
                "تو کی هستی", "شما کی هستی", "خودتو معرفی کن", "راهنما", "کمک", "چیکار میتونی بکنی")) {
            return true
        }
        if (norm.startsWith("سلام") || norm.startsWith("درود") || norm.startsWith("صبح بخیر") || norm.startsWith("خسته نباشید")) {
            // اگر در ادامه جمله فرمان عملیاتی یا سوال خاصی نباشد
            if (!isOperationalActionPattern(norm) && !isQuestionPattern(norm)) {
                return true
            }
        }
        return false
    }

    private fun isFlockStatsQuery(norm: String): Boolean {
        val hasStatsTerm = norm.contains("آمار") || norm.contains("امار") || norm.contains("وضعیت") ||
                norm.contains("گزارش") || norm.contains("موجودی") || norm.contains("سرشماری") ||
                norm.contains("تعداد کل") || norm.contains("چند پرنده") || norm.contains("چند مرغ") ||
                norm.contains("سن") || norm.contains("روزگی") || norm.contains("چند روزه")
        val hasTarget = norm.contains("سالن") || norm.contains("گله") || norm.contains("فارم") || norm.contains("انبار") || norm.contains("کل")
        return hasStatsTerm && hasTarget && !isOperationalActionPattern(norm)
    }
}

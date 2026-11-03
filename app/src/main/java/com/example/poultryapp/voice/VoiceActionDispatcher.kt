package com.example.poultryapp.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.ui.main.MainViewModel

data class VoiceDispatchResult(
    val success: Boolean,
    val message: String,
    val errorDetail: String? = null
)

object VoiceActionDispatcher {

    /**
     * اعتبارسنجی سطح دسترسی نقش کاربر و اعتبار اولیه کارت عملیات صوتی
     */
    fun checkRolePermission(
        card: VoiceActionCardData,
        userRole: UserRole = UserRole.MANAGER
    ): VoiceDispatchResult? {
        if (!card.isValid && !card.needsMoreInfo) {
            return VoiceDispatchResult(
                success = false,
                message = card.errorMessage ?: "دستور نامعتبر است و منجر به تغییری در داده‌ها نمی‌شود."
            )
        }

        if (userRole == UserRole.WORKER) {
            val target = (card.parameters["targetScreen"] as? String ?: card.parameters["targetTab"] as? String)?.uppercase() ?: ""
            val isForbiddenNav = target == "FINANCIAL" || target == "SETTINGS"
            val isForbidden = card.intentType == VoiceIntentType.RECORD_FINANCIAL ||
                    card.intentType == VoiceIntentType.CREATE_FLOCK ||
                    card.intentType == VoiceIntentType.TRANSFER_BIRDS ||
                    (card.intentType == VoiceIntentType.NAVIGATE_SCREEN && isForbiddenNav) ||
                    (card.intentType == VoiceIntentType.COMPOUND_ACTION && card.subActions.any { sub ->
                        val subTarget = (sub.parameters["targetScreen"] as? String ?: sub.parameters["targetTab"] as? String)?.uppercase() ?: ""
                        sub.intentType == VoiceIntentType.RECORD_FINANCIAL ||
                        sub.intentType == VoiceIntentType.CREATE_FLOCK ||
                        sub.intentType == VoiceIntentType.TRANSFER_BIRDS ||
                        (sub.intentType == VoiceIntentType.NAVIGATE_SCREEN && (subTarget == "FINANCIAL" || subTarget == "SETTINGS"))
                    })

            if (isForbidden) {
                return VoiceDispatchResult(
                    success = false,
                    message = "دسترسی غیر مجاز. این عملیات صرفاً در صلاحیت مدیر فارم است."
                )
            }
        }
        return null
    }

    /**
     * اجرای مستقیم عملیات صوتی تاییدشده بر روی ویومدل و محیط اندروید همراه با اعتبارسنجی دقیق خروجی.
     */
    fun dispatch(
        context: Context,
        viewModel: MainViewModel,
        card: VoiceActionCardData,
        userRole: UserRole = UserRole.MANAGER,
        onNavigate: (String) -> Unit
    ): VoiceDispatchResult {
        val permError = checkRolePermission(card, userRole)
        if (permError != null) return permError

        val params = card.parameters
        val flockId = card.flockId

        return try {
            when (card.intentType) {
                // ۰. ایجاد و افزودن گله جدید به فارم
                VoiceIntentType.CREATE_FLOCK -> {
                    val name = (params["flockName"] as? String) ?: (params["name"] as? String) ?: "گله جدید"
                    val species = (params["species"] as? BirdSpecies) ?: BirdSpecies.CHICKEN
                    val breed = (params["breed"] as? String) ?: "آمروک"
                    val birdCount = (params["birdCount"] as? Int) ?: 100
                    val ageDays = (params["ageDays"] as? Int) ?: 1
                    val notes = (params["notes"] as? String) ?: "ایجاد شده با دستیار هوشمند فارم"

                    viewModel.addFlock(
                        name = name,
                        species = species,
                        breed = breed,
                        ageDays = ageDays,
                        birdCount = birdCount,
                        notes = notes
                    )
                    onNavigate("FLOCKS")
                    VoiceDispatchResult(
                        success = true,
                        message = "گله «$name» شامل ${toPersianDigits(birdCount)} قطعه با موفقیت در فارم ایجاد شد."
                    )
                }

                // ۱. ثبت تراکنش‌های مالی (هزینه و درآمد)
                VoiceIntentType.RECORD_FINANCIAL -> {
                    val amount = (params["amountTomans"] as? Double) ?: 0.0
                    if (amount <= 0.0) {
                        return VoiceDispatchResult(false, "خطا: مبلغ تراکنش باید بزرگتر از صفر باشد.")
                    }

                    val type = (params["type"] as? TransactionType) ?: TransactionType.EXPENSE
                    val category = (params["category"] as? TransactionCategory) ?: (
                        if (type == TransactionType.INCOME) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
                    )
                    val title = (params["title"] as? String) ?: (if (type == TransactionType.INCOME) "درآمد دستیار هوشمند" else "هزینه دستیار هوشمند")
                    val todayJalali = JalaliDateHelper.getJalaliDate().toStandardString()

                    val transaction = FinancialTransaction(
                        title = title,
                        amountTomans = amount,
                        type = type,
                        category = category,
                        dateJalaliStr = todayJalali,
                        relatedFlockId = flockId,
                        relatedFlockName = card.flockName,
                        notes = "ثبت شده با دستیار هوشمند فارم"
                    )

                    viewModel.addFinancialTransaction(transaction)
                    val formatted = toPersianDigits(amount.toLong())
                    VoiceDispatchResult(
                        success = true,
                        message = "تراکنش مالی ${type.titleFa} به مبلغ $formatted تومان با موفقیت ثبت شد."
                    )
                }

                // ۲. ثبت تولید تخم‌مرغ
                VoiceIntentType.RECORD_EGG -> {
                    if (flockId == null) {
                        return VoiceDispatchResult(false, "خطا: سالن یا گله هدف در فارم مشخص نیست.")
                    }
                    val fertile = (params["fertileCount"] as? Int) ?: 0
                    val table = (params["tableEggCount"] as? Int) ?: 0
                    val trayCount = (params["trayCount"] as? Double) ?: 0.0

                    if (fertile <= 0 && table <= 0 && trayCount <= 0.0) {
                        return VoiceDispatchResult(false, "خطا: تعداد تخم‌مرغ معتبر نیست یا صفر است.")
                    }

                    viewModel.logDetailedEggProduction(
                        flockId = flockId,
                        fertileCount = fertile,
                        tableEggCount = table,
                        brokenCount = 0,
                        dirtyCount = 0,
                        doubleYolkCount = 0,
                        trayCount = trayCount,
                        trayGrossWeightGrams = null,
                        notes = "ثبت شده با دستیار هوشمند فارم"
                    )
                    VoiceDispatchResult(
                        success = true,
                        message = "تولید تخم‌مرغ با موفقیت ثبت شد و انبار شارژ گردید."
                    )
                }

                // ۳. ثبت تلفات و خروج پرندگان
                VoiceIntentType.RECORD_MORTALITY -> {
                    if (flockId == null) {
                        return VoiceDispatchResult(false, "خطا: گله هدف برای ثبت تلفات مشخص نشده است.")
                    }
                    val count = (params["count"] as? Int) ?: 1
                    if (count <= 0) {
                        return VoiceDispatchResult(false, "خطا: تعداد تلفات پرندگان باید بزرگتر از صفر باشد.")
                    }

                    val reason = (params["reason"] as? MortalityReason) ?: MortalityReason.DISEASE

                    viewModel.logFlockMortality(
                        flockId = flockId,
                        count = count,
                        reason = reason,
                        diagnosisHint = "ثبت شده با دستیار هوشمند فارم",
                        notes = card.rawText
                    )
                    VoiceDispatchResult(
                        success = true,
                        message = "$count قطعه تلفات با موفقیت ثبت و از آمار گله کسر شد."
                    )
                }

                // ۴. شارژ دان و خوراک سالن
                VoiceIntentType.RECORD_FEED -> {
                    if (flockId == null) {
                        return VoiceDispatchResult(false, "خطا: گله مقصد جهت شارژ دان مشخص نیست.")
                    }
                    val amount = ((params["amount"] as? Double) ?: (params["amountKg"] as? Double)) ?: 0.0
                    if (amount <= 0.0) {
                        return VoiceDispatchResult(false, "خطا: مقدار دان باید عددی مثبت باشد.")
                    }

                    viewModel.logCharge(
                        flockId = flockId,
                        itemType = ChargeItemType.FEED,
                        amount = amount,
                        notes = "شارژ صوتی دان"
                    )
                    VoiceDispatchResult(
                        success = true,
                        message = "${toPersianDigits(amount)} کیلوگرم دان با موفقیت ثبت و از انبار کسر شد."
                    )
                }

                // ۵. مصرف آب سالن
                VoiceIntentType.RECORD_WATER -> {
                    if (flockId == null) {
                        return VoiceDispatchResult(false, "خطا: گله مقصد جهت ثبت مصرف آب مشخص نیست.")
                    }
                    val amount = ((params["amount"] as? Double) ?: (params["amountLiters"] as? Double)) ?: 0.0
                    if (amount <= 0.0) {
                        return VoiceDispatchResult(false, "خطا: حجم آب مصرفی باید بزرگتر از صفر باشد.")
                    }

                    viewModel.logCharge(
                        flockId = flockId,
                        itemType = ChargeItemType.WATER,
                        amount = amount,
                        notes = "شارژ صوتی آب"
                    )
                    VoiceDispatchResult(
                        success = true,
                        message = "${toPersianDigits(amount)} لیتر آب مصرفی سالن با موفقیت ثبت شد."
                    )
                }

                // ۶. ثبت تجویز دارو و مکمل
                VoiceIntentType.RECORD_MEDICATION -> {
                    val targetFlockId = flockId ?: viewModel.selectedFlockId.value ?: 1L
                    val medName = (params["medName"] as? String) ?: "مکمل دارویی"

                    viewModel.setFlockMedicationWithdrawal(targetFlockId, 3, medName)
                    VoiceDispatchResult(
                        success = true,
                        message = "تجویز «$medName» و دوره پرهیز با موفقیت برای سالن ثبت شد."
                    )
                }

                // ۷. استعلام آمار و وضعیت
                VoiceIntentType.QUERY_STATS -> {
                    val topic = (params["topic"] as? String) ?: ""
                    when {
                        topic.contains("دان") -> onNavigate("FEED_WATER")
                        topic.contains("تخم") -> onNavigate("FLOCKS")
                        topic.contains("هوا") -> onNavigate("HOME")
                        else -> onNavigate("HOME")
                    }
                    VoiceDispatchResult(
                        success = true,
                        message = "گزارش و اطلاعات $topic نمایش داده شد."
                    )
                }

                // ۸. ناوبری و انتقال بین صفحات
                VoiceIntentType.NAVIGATE_SCREEN -> {
                    val target = ((params["targetTab"] as? String) ?: (params["targetScreen"] as? String)) ?: "HOME"
                    onNavigate(target)
                    VoiceDispatchResult(
                        success = true,
                        message = "انتقال به بخش درخواست‌شده با موفقیت انجام شد."
                    )
                }

                // ۹. تماس با مخاطبین تجاری
                VoiceIntentType.CALL_CONTACT -> {
                    val phone = ((params["phone"] as? String) ?: (params["phoneNumber"] as? String)) ?: ""
                    val contactName = (params["contactName"] as? String) ?: "مخاطب"
                    if (phone.isBlank()) {
                        return VoiceDispatchResult(false, "خطا: شماره تماسی برای $contactName یافت نشد.")
                    }

                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(callIntent)
                    VoiceDispatchResult(
                        success = true,
                        message = "شماره‌گیر تلفن با موفقیت باز شد."
                    )
                }

                // ۱۰. تشخیص بالینی و پرونده بیماری فارم
                VoiceIntentType.DIAGNOSE_DISEASE -> {
                    val diseaseId = params["diseaseId"] as? String
                    val diseaseName = (params["diseaseName"] as? String) ?: "بیماری"
                    if (diseaseId != null) {
                        onNavigate("DISEASE_DETAIL:$diseaseId")
                        VoiceDispatchResult(
                            success = true,
                            message = "پرونده بالینی «$diseaseName» با موفقیت باز شد."
                        )
                    } else {
                        onNavigate("HEALTH")
                        VoiceDispatchResult(
                            success = true,
                            message = "بخش غربالگری و کلینیک بیماری‌های فارم باز شد."
                        )
                    }
                }

                // ۱۱. مدیریت و شارژ اقلام انبار (Warehouse Management)
                VoiceIntentType.MANAGE_WAREHOUSE -> {
                    val itemId = (params["itemId"] as? Long) ?: (params["warehouseItemId"] as? Long)
                    val deltaStock = (params["deltaStock"] as? Double) ?: (params["amount"] as? Double) ?: 0.0
                    val itemName = (params["itemName"] as? String) ?: "کالای انبار"

                    if (itemId != null && deltaStock != 0.0) {
                        viewModel.adjustWarehouseStock(itemId, deltaStock)
                        VoiceDispatchResult(
                            success = true,
                            message = "موجودی «$itemName» با موفقیت به میزان ${toPersianDigits(Math.abs(deltaStock))} واحد تغییر یافت."
                        )
                    } else {
                        onNavigate("WAREHOUSE")
                        VoiceDispatchResult(
                            success = true,
                            message = "صفحه انبار مرکزی فارم با موفقیت باز شد."
                        )
                    }
                }

                // ۱۲. ثبت ترکیبی چند اقدام همزمان (Compound Multi-Action)
                VoiceIntentType.COMPOUND_ACTION -> {
                    val subActions = card.subActions
                    if (subActions.isEmpty()) {
                        return VoiceDispatchResult(false, "هیچ عملیات مشخصی در دستور ترکیبی یافت نشد.")
                    }

                    val messages = mutableListOf<String>()
                    var allSuccess = true
                    for (sub in subActions) {
                        val subResult = dispatch(
                            context = context,
                            viewModel = viewModel,
                            card = sub,
                            userRole = userRole,
                            onNavigate = onNavigate
                        )
                        if (!subResult.success) {
                            allSuccess = false
                        }
                        messages.add("• " + subResult.message)
                    }

                    VoiceDispatchResult(
                        success = allSuccess,
                        message = if (allSuccess) {
                            "تمام ${toPersianDigits(subActions.size)} عملیات با موفقیت ثبت شدند:\n" + messages.joinToString("\n")
                        } else {
                            "برخی از اقدامات با خطا مواجه شدند:\n" + messages.joinToString("\n")
                        }
                    )
                }

                // ۱۳. تنظیم آلارم و یادآور تقویم فارم
                VoiceIntentType.SET_SCHEDULE -> {
                    val title = (params["title"] as? String) ?: "یادآور فارم"
                    val scheduleType = (params["scheduleType"] as? String) ?: "سایر"
                    val hour = (params["hour"] as? Int) ?: 9
                    val minute = (params["minute"] as? Int) ?: 0
                    val daysOfWeek = (params["daysOfWeek"] as? List<*>)?.filterIsInstance<Int>() ?: emptyList()
                    val scheduleMode = (params["scheduleMode"] as? String) ?: "WEEKLY"
                    val sched = com.example.poultryapp.data.local.CalendarSchedule(
                        flockId = flockId,
                        flockName = card.flockName,
                        scheduleType = scheduleType,
                        title = title,
                        notes = "تنظیم شده با دستیار هوشمند فارم",
                        daysOfWeek = daysOfWeek,
                        hour = hour,
                        minute = minute,
                        isEnabled = true,
                        scheduleMode = scheduleMode
                    )
                    viewModel.addSchedule(sched)
                    val timeStr = "${toPersianDigits("%02d".format(hour))}:${toPersianDigits("%02d".format(minute))}"
                    VoiceDispatchResult(
                        success = true,
                        message = "یادآور «$title» برای ساعت $timeStr در تقویم فارم با موفقیت تنظیم شد."
                    )
                }

                // ۱۴. ثبت مشاهدات سالن و گزارش شیفت کارگر
                VoiceIntentType.RECORD_OBSERVATION -> {
                    val noteText = (params["note"] as? String) ?: card.rawText
                    val category = (params["category"] as? String) ?: "OBSERVATION"
                    val dateJalaliStr = (params["dateJalaliStr"] as? String)
                    val targetFlockId = flockId ?: viewModel.selectedFlockId.value ?: 1L
                    viewModel.recordFlockObservation(
                        flockId = targetFlockId,
                        note = noteText,
                        authorRole = userRole,
                        category = category,
                        dateJalaliStr = dateJalaliStr
                    )
                    VoiceDispatchResult(
                        success = true,
                        message = "مشاهده و گزارش شیفت سالن با موفقیت در سوابق فارم ثبت گردید."
                    )
                }

                // ۱۵. محاسبه صوتی شاخص‌های علمی پرورشی در لحظه
                VoiceIntentType.CALCULATE_METRIC -> {
                    VoiceDispatchResult(
                        success = true,
                        message = "${card.primaryMetric} - ${card.systemEffect}"
                    )
                }

                // ۱۶. انتقال پرندگان بین سالن‌ها و گله‌ها
                VoiceIntentType.TRANSFER_BIRDS -> {
                    val fromId = (params["fromFlockId"] as? Long) ?: (params["fromId"] as? Long) ?: 1L
                    val toId = (params["toFlockId"] as? Long) ?: (params["toId"] as? Long) ?: 2L
                    val count = (params["count"] as? Int) ?: (params["birdCount"] as? Int) ?: 1
                    val fromName = (params["fromFlockName"] as? String) ?: "سالن مبدا"
                    val toName = (params["toFlockName"] as? String) ?: "سالن مقصد"

                    viewModel.transferBirds(fromId, toId, count, "انتقال صوتی با دستیار هوشمند")
                    onNavigate("FLOCKS")
                    VoiceDispatchResult(
                        success = true,
                        message = "${toPersianDigits(count)} قطعه پرنده با موفقیت از «$fromName» به «$toName» انتقال یافت."
                    )
                }

                // ۱۷. تنظیم دمای اختصاصی سالن و گله
                VoiceIntentType.SET_FLOCK_TEMPERATURE -> {
                    val targetFlockId = flockId ?: viewModel.selectedFlockId.value ?: 1L
                    val temp = (params["tempCelsius"] as? Double)
                        ?: ((params["temp"] as? Number)?.toDouble())
                    if (temp == null || temp !in 5.0..48.0) {
                        return VoiceDispatchResult(false, "خطا: دمای درخواستی برای تنظیم معتبر نیست (باید بین ۵ تا ۴۸ درجه باشد).")
                    }
                    viewModel.setFlockCustomTemperature(targetFlockId, temp)
                    val formatted = toPersianDigits(String.format(java.util.Locale.US, "%.1f", temp))
                    VoiceDispatchResult(
                        success = true,
                        message = "دمای اختصاصی سالن «${card.flockName ?: "گله"}» با موفقیت روی $formatted درجه سانتی‌گراد تنظیم و ثبت شد."
                    )
                }

                // ۱۸. فعال‌سازی یا غیرفعال‌سازی پایش هوشمند دان و آب سالن
                VoiceIntentType.TOGGLE_TRACKING -> {
                    val targetFlockId = flockId ?: viewModel.selectedFlockId.value ?: 1L
                    val trackingType = (params["trackingType"] as? String) ?: "FEED"
                    val isEnabled = (params["isEnabled"] as? Boolean) ?: true
                    val fName = card.flockName ?: "گله"
                    val stateStr = if (isEnabled) "فعال" else "غیرفعال"

                    if (trackingType == "WATER") {
                        viewModel.toggleFlockWaterTracking(targetFlockId, isEnabled)
                        VoiceDispatchResult(
                            success = true,
                            message = "پایش هوشمند آب سالن «$fName» با موفقیت $stateStr شد."
                        )
                    } else {
                        viewModel.toggleFlockFeedTracking(targetFlockId, isEnabled)
                        VoiceDispatchResult(
                            success = true,
                            message = "پایش هوشمند دان سالن «$fName» با موفقیت $stateStr شد."
                        )
                    }
                }

                else -> {
                    VoiceDispatchResult(
                        success = false,
                        message = "این دستور با هیچ یک از عملیات‌های فارم همخوانی ندارد و تغییری ایجاد نخواهد شد."
                    )
                }
            }
        } catch (e: Exception) {
            VoiceDispatchResult(
                success = false,
                message = "خطا در حین اجرای دستور: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}

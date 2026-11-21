package com.example.poultryapp

import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.FlockSubGroup
import com.example.poultryapp.data.local.RationIngredient
import com.example.poultryapp.data.local.RationItemType
import com.example.poultryapp.data.local.RationPlan
import com.example.poultryapp.data.local.RationPricingMode
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.data.model.ComprehensiveFeedCatalog
import com.example.poultryapp.data.model.FeedCategory
import com.example.poultryapp.engine.SolarDaylightCalculator
import com.example.poultryapp.receiver.LiveFeedWaterNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NewFeaturesValidationTest {

    // ==========================================
    // ۱. تست محاسبات طول روز و ساعات روشنایی خورشید
    // ==========================================
    @Test
    fun testSolarDaylightCalculator_Tehran() {
        val tehranLat = 35.6892

        // انقلاب تابستانی (۲۱ ژوئن - تقریبا روز ۱۷۲ سال): طول روز باید بالای ۱۴ ساعت باشد
        val summerResult = SolarDaylightCalculator.calculateDaylight(
            latitudeDeg = tehranLat,
            dayOfYear = 172,
            cityName = "تهران"
        )
        assertTrue("طول روز در تابستان تهران باید بالای ۱۴ ساعت باشد: ${summerResult.daylightHours}", summerResult.daylightHours > 14.0)

        // انقلاب زمستانی (۲۱ دسامبر - روز ۳۵۵ سال): طول روز باید زیر ۱۰ ساعت باشد
        val winterResult = SolarDaylightCalculator.calculateDaylight(
            latitudeDeg = tehranLat,
            dayOfYear = 355,
            cityName = "تهران"
        )
        assertTrue("طول روز در زمستان تهران باید زیر ۱۰ ساعت باشد: ${winterResult.daylightHours}", winterResult.daylightHours < 10.0)

        // اعتدال بهاری (۲۱ مارس - روز ۸۰ سال): طول روز باید حدود ۱۲ ساعت باشد
        val equinoxResult = SolarDaylightCalculator.calculateDaylight(
            latitudeDeg = tehranLat,
            dayOfYear = 80,
            cityName = "تهران"
        )
        assertTrue("طول روز در اعتدال بهاری باید بین ۱۱.۵ تا ۱۲.۵ ساعت باشد: ${equinoxResult.daylightHours}", equinoxResult.daylightHours in 11.5..12.5)

        // تست فرمت‌بندی و توصیه‌های روشنایی
        assertTrue("باید شامل مدت زمان فارسی باشد", summerResult.formattedDurationFa.isNotBlank())
        assertTrue("طلوع آفتاب باید محاسبه شده باشد", summerResult.sunriseApproxFa.isNotBlank())
        assertTrue("غروب آفتاب باید محاسبه شده باشد", summerResult.sunsetApproxFa.isNotBlank())
        assertTrue("در زمستان باید نور مکمل نیاز باشد", winterResult.supplementalLightHoursForLayersFa.contains("ساعت"))

        // تست تبدیل ثانیه‌های Open-Meteo
        val formatted = SolarDaylightCalculator.formatSecondsToHoursMinutesFa(50400.0) // 14 hours
        assertTrue("تبدیل ثانیه به ساعت", formatted.contains("۱۴"))
    }

    // ==========================================
    // ۲. تست کاتالوگ جامع اقلام خوراک و دسته‌بندی‌ها
    // ==========================================
    @Test
    fun testComprehensiveFeedCatalog_Completeness() {
        val items = ComprehensiveFeedCatalog.ITEMS
        assertTrue("کاتالوگ باید حداقل ۵۰ قلم خوراک استاندارد داشته باشد", items.size >= 50)

        // بررسی اقلام اصلی و پایه‌ای مرغداری در ایران
        val corn = items.firstOrNull { it.name.contains("ذرت") }
        assertNotNull("ذرت باید در کاتالوگ موجود باشد", corn)
        assertTrue("انرژی متابولیسمی ذرت باید بالای ۳۰۰۰ باشد", corn!!.energyKcalKg > 3000.0)

        val soy = items.firstOrNull { it.name.contains("سویا") }
        assertNotNull("کنجاله سویا باید در کاتالوگ موجود باشد", soy)
        assertTrue("پروتئین سویا باید بالای ۴۰ درصد باشد", soy!!.crudeProtein >= 40.0)

        val dcp = items.firstOrNull { it.name.contains("DCP") || it.name.contains("دی‌کلسیم فسفات") }
        assertNotNull("دی کلسیم فسفات باید در کاتالوگ باشد", dcp)
        assertTrue("فسفر DCP باید بالای ۱۵ درصد باشد", dcp!!.availablePhosphorus > 15.0)

        // بررسی اینکه تمامی دسته‌ها در کاتالوگ استفاده شده‌اند
        val usedCategories = items.map { it.category }.toSet()
        assertTrue("حداقل ۷ دسته از اقلام باید پوشش داده شده باشند", usedCategories.size >= 7)

        // تست تبدیل دسته‌بندی‌ها از رشته
        assertEquals(FeedCategory.GRAINS, FeedCategory.fromString("GRAINS"))
        assertEquals(FeedCategory.GRAINS, FeedCategory.fromString("غلات و دانه‌ها"))
        assertEquals(FeedCategory.PLANT_PROTEIN, FeedCategory.fromString("کنجاله‌ها و پروتئین گیاهی"))
    }

    // ==========================================
    // ۳. تست محاسبات آنالیز تغذیه‌ای و ریزمغذی‌ها در جیره
    // ==========================================
    @Test
    fun testRationNutrientWeightedCalculations() {
        // ایجاد یک جیره استاندارد با ۶۰٪ ذرت و ۳۵٪ کنجاله سویا و ۵٪ مکمل/دی‌کلسیم فسفات
        val ingredients = listOf(
            RationIngredient(
                name = "ذرت دانه‌ای",
                percentageOrAmount = 60.0,
                pricePerKg = 13000.0,
                crudeProteinPercent = 8.5,
                category = "غلات",
                energyKcalKg = 3350.0,
                calciumPercent = 0.02,
                availablePhosphorusPercent = 0.09,
                methioninePercent = 0.18,
                lysinePercent = 0.26,
                sodiumPercent = 0.02,
                crudeFiberPercent = 2.2,
                fatPercent = 3.8
            ),
            RationIngredient(
                name = "کنجاله سویا ۴۴٪",
                percentageOrAmount = 35.0,
                pricePerKg = 22000.0,
                crudeProteinPercent = 44.0,
                category = "کنجاله",
                energyKcalKg = 2230.0,
                calciumPercent = 0.28,
                availablePhosphorusPercent = 0.20,
                methioninePercent = 0.62,
                lysinePercent = 2.80,
                sodiumPercent = 0.04,
                crudeFiberPercent = 6.0,
                fatPercent = 1.2
            ),
            RationIngredient(
                name = "دی‌کلسیم فسفات",
                percentageOrAmount = 5.0,
                pricePerKg = 35000.0,
                crudeProteinPercent = 0.0,
                category = "معدنی",
                energyKcalKg = 0.0,
                calciumPercent = 24.0,
                availablePhosphorusPercent = 18.5,
                methioninePercent = 0.0,
                lysinePercent = 0.0,
                sodiumPercent = 0.1,
                crudeFiberPercent = 0.0,
                fatPercent = 0.0
            )
        )

        val plan = RationPlan(
            id = 1L,
            name = "جیره آزمایشی جوجه گوشتی",
            itemType = RationItemType.FEED,
            targetBird = "BROILER",
            targetStage = "رشد",
            pricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS,
            batchBaseKg = 100.0,
            ingredients = ingredients
        )

        assertEquals(100.0, plan.totalPercentage, 0.001)

        // بررسی قیمت کل هر کیلو: (60*13000 + 35*22000 + 5*35000) / 100 = 780000 + 770000 + 175000 = 1725000 / 100 = 17250 تومان
        assertEquals(17250.0, plan.effectivePricePerKg, 0.01)

        // بررسی پروتئین خام: (60*8.5 + 35*44.0 + 0) / 100 = (510 + 1540) / 100 = 20.5%
        assertNotNull(plan.estimatedCrudeProteinPercent)
        assertEquals(20.5, plan.estimatedCrudeProteinPercent!!, 0.01)

        // بررسی انرژی: (60*3350 + 35*2230) / 100 = (201000 + 78050) / 100 = 2790.5 kcal/kg
        assertNotNull(plan.estimatedEnergyKcalKg)
        assertEquals(2790.5, plan.estimatedEnergyKcalKg!!, 0.01)

        // بررسی کلسیم: (60*0.02 + 35*0.28 + 5*24.0) / 100 = (1.2 + 9.8 + 120.0) / 100 = 1.31%
        assertNotNull(plan.estimatedCalciumPercent)
        assertEquals(1.31, plan.estimatedCalciumPercent!!, 0.01)

        // بررسی فسفر قابل جذب: (60*0.09 + 35*0.20 + 5*18.5) / 100 = (5.4 + 7.0 + 92.5) / 100 = 1.049%
        assertNotNull(plan.estimatedPhosphorusPercent)
        assertEquals(1.049, plan.estimatedPhosphorusPercent!!, 0.01)

        // بررسی متیونین: (60*0.18 + 35*0.62) / 100 = (10.8 + 21.7) / 100 = 0.325%
        assertNotNull(plan.estimatedMethioninePercent)
        assertEquals(0.325, plan.estimatedMethioninePercent!!, 0.01)

        // بررسی لیزین: (60*0.26 + 35*2.80) / 100 = (15.6 + 98.0) / 100 = 1.136%
        assertNotNull(plan.estimatedLysinePercent)
        assertEquals(1.136, plan.estimatedLysinePercent!!, 0.01)
    }

    // ==========================================
    // ۴. تست منطق کسر و بازگشت تلفات در گله
    // ==========================================
    @Test
    fun testFlockMortalityDeductionAndRestoreLogic() {
        val now = System.currentTimeMillis()
        val initialSubGroups = listOf(
            FlockSubGroup(species = BirdSpecies.CHICKEN, breed = "راس ۳۰۸", birdCount = 300, birthEpochMillis = now),
            FlockSubGroup(species = BirdSpecies.CHICKEN, breed = "کاب ۵۰۰", birdCount = 200, birthEpochMillis = now)
        )

        val flock = Flock(
            id = 10L,
            name = "سالن ۱ گوشتی",
            species = BirdSpecies.CHICKEN,
            breed = "میکس",
            birdCount = 500,
            birthEpochMillis = now,
            subGroups = initialSubGroups
        )

        assertEquals(500, flock.effectiveBirdCount)

        // شبیه‌سازی ثبت تلفات (کسر ۵ پرنده از گروه بزرگتر یا به تناسب)
        val mortalityCount = 5
        val mutableSubGroups = flock.subGroups.map { it.copy() }.toMutableList()
        var remainingToDeduct = mortalityCount

        val sortedIndices = mutableSubGroups.indices.sortedByDescending { mutableSubGroups[it].birdCount }
        for (idx in sortedIndices) {
            if (remainingToDeduct <= 0) break
            val group = mutableSubGroups[idx]
            val deduct = minOf(remainingToDeduct, group.birdCount)
            mutableSubGroups[idx] = group.copy(birdCount = group.birdCount - deduct)
            remainingToDeduct -= deduct
        }

        val updatedFlock = flock.copy(
            birdCount = maxOf(0, flock.birdCount - mortalityCount),
            subGroups = mutableSubGroups
        )

        // پرنده‌ها باید از ۵۰۰ به ۴۹۵ کاهش یافته باشند
        assertEquals(495, updatedFlock.birdCount)
        assertEquals(495, updatedFlock.effectiveBirdCount)
        assertEquals(295, updatedFlock.subGroups[0].birdCount) // کسر از گروه ۳۰۰ تایی
        assertEquals(200, updatedFlock.subGroups[1].birdCount)

        // شبیه‌سازی حذف لاگ تلفات و بازگشت پرنده‌ها (Restore)
        val restoredSubGroups = updatedFlock.subGroups.map { it.copy() }.toMutableList()
        val firstIdx = restoredSubGroups.indices.maxByOrNull { restoredSubGroups[it].birdCount } ?: 0
        restoredSubGroups[firstIdx] = restoredSubGroups[firstIdx].copy(
            birdCount = restoredSubGroups[firstIdx].birdCount + mortalityCount
        )

        val restoredFlock = updatedFlock.copy(
            birdCount = updatedFlock.birdCount + mortalityCount,
            subGroups = restoredSubGroups
        )

        assertEquals(500, restoredFlock.birdCount)
        assertEquals(500, restoredFlock.effectiveBirdCount)
        assertEquals(300, restoredFlock.subGroups[0].birdCount)
    }

    // ==========================================
    // ۵. تست احترام به خاموش بودن هشدار کمبود موجودی در انبار
    // ==========================================
    @Test
    fun testWarehouseLowStockAlertToggleRespect() {
        // کالای میان دان که هشدار کمبود موجودی آن خاموش شده است (minAlertThreshold = 0.0)
        val disabledFeedAlertItem = WarehouseItem(
            id = 1L,
            name = "میان دان",
            category = WarehouseCategory.FEED,
            currentStock = 5.0,
            unit = "کیلوگرم",
            minAlertThreshold = 0.0
        )

        // حتی با باقی‌ماندن کمتر از ۳ روز یا موجودی صفر، وقتی هشدار خاموش است نباید اعلان صادر شود
        assertFalse(
            LiveFeedWaterNotificationManager.shouldNotifyWarehouseLowStock(
                item = disabledFeedAlertItem,
                daysRemaining = 1.0,
                feedWaterAlertsEnabled = true
            )
        )
        assertFalse(
            LiveFeedWaterNotificationManager.shouldNotifyWarehouseLowStock(
                item = disabledFeedAlertItem.copy(currentStock = 0.0),
                daysRemaining = 0.0,
                feedWaterAlertsEnabled = true
            )
        )

        // وقتی هشدار روی همین کالا روشن شود (minAlertThreshold = 50.0)
        val enabledFeedAlertItem = disabledFeedAlertItem.copy(minAlertThreshold = 50.0)
        assertTrue(
            LiveFeedWaterNotificationManager.shouldNotifyWarehouseLowStock(
                item = enabledFeedAlertItem,
                daysRemaining = 2.0,
                feedWaterAlertsEnabled = true
            )
        )
        assertTrue(
            LiveFeedWaterNotificationManager.shouldNotifyWarehouseLowStock(
                item = enabledFeedAlertItem.copy(currentStock = 40.0),
                daysRemaining = 10.0,
                feedWaterAlertsEnabled = true
            )
        )

        // وقتی تنظیمات کلی هشدار دان و آب خاموش باشد، نباید اعلان صادر شود
        assertFalse(
            LiveFeedWaterNotificationManager.shouldNotifyWarehouseLowStock(
                item = enabledFeedAlertItem,
                daysRemaining = 1.0,
                feedWaterAlertsEnabled = false
            )
        )
    }
}

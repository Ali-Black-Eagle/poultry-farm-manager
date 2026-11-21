package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.RouterDecision
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.router.AssistantRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AllCapabilitiesRoutingTest {

    private lateinit var router: AssistantRouter

    @Before
    fun setUp() {
        router = AssistantRouter()
    }

    @Test
    fun testFeedAndWaterRouting() {
        val d1 = router.route("شارژ دان ۱۰۰ کیلو برای سالن")
        assertTrue("Expected Execute for ADD_FEED_WATER, got $d1", d1 is RouterDecision.Execute)
        val exec1 = d1 as RouterDecision.Execute
        assertEquals(IntentId.ADD_FEED_WATER, exec1.intent)
        assertEquals(100, exec1.slots[SlotId.COUNT]?.typedValue)
        assertEquals("FEED", exec1.slots[SlotId.FEED_WATER_TYPE]?.typedValue)

        val d2 = router.route("شارژ آب ۵۰۰ لیتر")
        assertTrue("Expected Execute for ADD_FEED_WATER, got $d2", d2 is RouterDecision.Execute)
        val exec2 = d2 as RouterDecision.Execute
        assertEquals(IntentId.ADD_FEED_WATER, exec2.intent)
        assertEquals(500, exec2.slots[SlotId.COUNT]?.typedValue)
        assertEquals("WATER", exec2.slots[SlotId.FEED_WATER_TYPE]?.typedValue)

        val q1 = router.route("امروز چقدر دان مصرف شده؟")
        assertTrue("Expected Execute for QUERY_FEED_WATER, got $q1", q1 is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_FEED_WATER, (q1 as RouterDecision.Execute).intent)
    }

    @Test
    fun testMortalityRouting() {
        val d1 = router.route("تلفات ۵ قطعه در سالن به علت گرمازدگی")
        assertTrue("Expected Execute for LOG_MORTALITY, got $d1", d1 is RouterDecision.Execute)
        val exec1 = d1 as RouterDecision.Execute
        assertEquals(IntentId.LOG_MORTALITY, exec1.intent)
        assertEquals(5, exec1.slots[SlotId.COUNT]?.typedValue)
        assertEquals("HEAT_COLD_STRESS", exec1.slots[SlotId.MORTALITY_REASON]?.typedValue)

        val q1 = router.route("تلفات این هفته چقدر بوده؟")
        assertTrue("Expected Execute for QUERY_MORTALITY, got $q1", q1 is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_MORTALITY, (q1 as RouterDecision.Execute).intent)
    }

    @Test
    fun testFlocksAndTransferRouting() {
        val q1 = router.route("چند تا مرغ داریم؟")
        assertTrue("Expected Execute for QUERY_FLOCKS, got $q1", q1 is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_FLOCKS, (q1 as RouterDecision.Execute).intent)

        val t1 = router.route("انتقال ۵۰ مرغ از سالن به سالن")
        assertTrue("Expected Execute for TRANSFER_BIRDS, got $t1", t1 is RouterDecision.Execute)
        val exec1 = t1 as RouterDecision.Execute
        assertEquals(IntentId.TRANSFER_BIRDS, exec1.intent)
        assertEquals(50, exec1.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testWarehouseRouting() {
        val q1 = router.route("موجودی انبار چقدره؟")
        assertTrue("Expected Execute for QUERY_WAREHOUSE, got $q1", q1 is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_WAREHOUSE, (q1 as RouterDecision.Execute).intent)

        val c1 = router.route("مصرف ۵۰ کیلو از انبار برای سالن")
        assertTrue("Expected Execute for CONSUME_WAREHOUSE, got $c1", c1 is RouterDecision.Execute)
        val exec1 = c1 as RouterDecision.Execute
        assertEquals(IntentId.CONSUME_WAREHOUSE, exec1.intent)
        assertEquals(50, exec1.slots[SlotId.COUNT]?.typedValue)
    }

    @Test
    fun testVeterinaryAndHealthRouting() {
        val diag = router.route("مرغ‌ها اسهال سفید و بی حالی دارند چه بیماریه؟")
        assertTrue("Expected Execute for DIAGNOSE_DISEASE, got $diag", diag is RouterDecision.Execute)
        assertEquals(IntentId.DIAGNOSE_DISEASE, (diag as RouterDecision.Execute).intent)

        val med = router.route("دوز داروی اکسی تتراسایکلین چقدره؟")
        assertTrue("Expected Execute for QUERY_MEDICATION, got $med", med is RouterDecision.Execute)
        val execMed = med as RouterDecision.Execute
        assertEquals(IntentId.QUERY_MEDICATION, execMed.intent)
        assertEquals("اکسی تتراسایکلین", execMed.slots[SlotId.MEDICATION_NAME]?.typedValue)
    }

    @Test
    fun testCalendarAndContactsAndWeatherRouting() {
        val sched = router.route("یادآور واکسن فردا ساعت ۹ صبح")
        assertTrue("Expected Execute for ADD_SCHEDULE, got $sched", sched is RouterDecision.Execute)
        assertEquals(IntentId.ADD_SCHEDULE, (sched as RouterDecision.Execute).intent)

        val qSched = router.route("برنامه‌های امروز چیه؟")
        assertTrue("Expected Execute for QUERY_SCHEDULE, got $qSched", qSched is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_SCHEDULE, (qSched as RouterDecision.Execute).intent)

        val contact = router.route("شماره تلفن دامپزشک چنده؟")
        assertTrue("Expected Execute for QUERY_CONTACTS, got $contact", contact is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_CONTACTS, (contact as RouterDecision.Execute).intent)

        val weather = router.route("دمای هوای فارم چنده؟")
        assertTrue("Expected Execute for QUERY_WEATHER, got $weather", weather is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_WEATHER, (weather as RouterDecision.Execute).intent)
    }

    @Test
    fun testScreenNavigationRouting() {
        val nav1 = router.route("برو به انبار")
        assertTrue("Expected Execute for OPEN_SCREEN, got $nav1", nav1 is RouterDecision.Execute)
        val exec1 = nav1 as RouterDecision.Execute
        assertEquals(IntentId.OPEN_SCREEN, exec1.intent)
        assertEquals("MANAGEMENT:WAREHOUSE", exec1.slots[SlotId.TARGET_SCREEN]?.typedValue)

        val nav2 = router.route("برو به تنظیمات")
        assertTrue("Expected Execute for OPEN_SCREEN, got $nav2", nav2 is RouterDecision.Execute)
        val exec2 = nav2 as RouterDecision.Execute
        assertEquals(IntentId.OPEN_SCREEN, exec2.intent)
        assertEquals("SETTINGS", exec2.slots[SlotId.TARGET_SCREEN]?.typedValue)

        val navSales = router.route("برو به بخش فروش و کشتار")
        assertTrue("Expected Execute for OPEN_SCREEN, got $navSales", navSales is RouterDecision.Execute)
        val execSales = navSales as RouterDecision.Execute
        assertEquals(IntentId.OPEN_SCREEN, execSales.intent)
        assertEquals("FLOCKS:SALES_SLAUGHTER", execSales.slots[SlotId.TARGET_SCREEN]?.typedValue)

        val navMort = router.route("برو به آمار تلفات")
        assertTrue("Expected Execute for OPEN_SCREEN, got $navMort", navMort is RouterDecision.Execute)
        val execMort = navMort as RouterDecision.Execute
        assertEquals(IntentId.OPEN_SCREEN, execMort.intent)
        assertEquals("FLOCKS:MORTALITY", execMort.slots[SlotId.TARGET_SCREEN]?.typedValue)
    }

    @Test
    fun testBirdSaleAndSlaughterRoutingAndDisambiguation() {
        val saleFa = router.route("۵۰ مرغ فروختم ۱۵ میلیون تومان")
        assertTrue("Expected Execute for SELL_BIRDS, got $saleFa", saleFa is RouterDecision.Execute)
        val execSale = saleFa as RouterDecision.Execute
        assertEquals(IntentId.SELL_BIRDS, execSale.intent)
        assertEquals(50, execSale.slots[SlotId.COUNT]?.typedValue)
        assertEquals(15000000.0, (execSale.slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as Number).toDouble(), 0.01)

        val slaughterFa = router.route("کشتار ۱۰۰ مرغ با وزن زنده ۲۴۰ کیلو و وزن لاشه ۱۷۵ کیلو")
        assertTrue("Expected Execute for SLAUGHTER_BIRDS, got $slaughterFa", slaughterFa is RouterDecision.Execute)
        val execSlaughter = slaughterFa as RouterDecision.Execute
        assertEquals(IntentId.SLAUGHTER_BIRDS, execSlaughter.intent)
        assertEquals(100, execSlaughter.slots[SlotId.COUNT]?.typedValue)
        assertEquals(240.0, (execSlaughter.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)
        assertEquals(175.0, (execSlaughter.slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as Number).toDouble(), 0.01)

        // Disambiguation: Egg sale MUST stay ADD_TRANSACTION
        val eggSale = router.route("فروش تخم مرغ ۵۰۰ هزار")
        assertTrue("Expected Execute for ADD_TRANSACTION, got $eggSale", eggSale is RouterDecision.Execute)
        assertEquals(IntentId.ADD_TRANSACTION, (eggSale as RouterDecision.Execute).intent)

        // Disambiguation: Profit query MUST stay QUERY_FINANCE
        val financeQuery = router.route("سود فروش مرغ چقدره؟")
        assertTrue("Expected Execute for QUERY_FINANCE, got $financeQuery", financeQuery is RouterDecision.Execute)
        assertEquals(IntentId.QUERY_FINANCE, (financeQuery as RouterDecision.Execute).intent)

        // General question about slaughter MUST route online
        val generalQ = router.route("چرا پرنده‌ها را کشتار می‌کنند؟")
        assertTrue("Expected AskOnline for general question, got $generalQ", generalQ is RouterDecision.AskOnline)
    }
}


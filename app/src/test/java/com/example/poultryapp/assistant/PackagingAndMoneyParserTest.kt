package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.entities.*
import org.junit.Assert.*
import org.junit.Test

class PackagingAndMoneyParserTest {

    @Test
    fun testPackagingUnitExtraction() {
        // 5 trays of eggs = 150 eggs
        val trays = PackagingUnitExtractor.extractUnits("۵ شانه تخم جمع شد", eggsPerTray = 30)
        assertEquals(1, trays.size)
        assertEquals(PoultryUnitType.EGG_TRAY, trays.first().unitType)
        assertEquals(150.0, trays.first().calculatedTotal, 0.001)

        // 2 cartons = 12 trays = 360 eggs
        val cartons = PackagingUnitExtractor.extractUnits("2 cartons of eggs", eggsPerTray = 30)
        assertEquals(1, cartons.size)
        assertEquals(PoultryUnitType.EGG_CARTON, cartons.first().unitType)
        assertEquals(360.0, cartons.first().calculatedTotal, 0.001)

        // 4 sacks of feed = 200 kg
        val sacks = PackagingUnitExtractor.extractUnits("۴ کیسه دان", eggsPerTray = 30)
        assertEquals(1, sacks.size)
        assertEquals(PoultryUnitType.FEED_SACK, sacks.first().unitType)
        assertEquals(200.0, sacks.first().calculatedTotal, 0.001)
    }

    @Test
    fun testMoneyExtraction() {
        val numbers = NumberParser.extractNumbers("هزینه دان ۱ میلیون و ۵۰۰ هزار تومان ثبت کن")
        val money = MoneyParser.extractMoney("هزینه دان ۱ میلیون و ۵۰۰ هزار تومان ثبت کن", numbers)
        assertFalse(money.isEmpty())
        assertEquals(1500000.0, money.first().amountInTomans, 0.001)
        assertEquals(Currency.TOMAN, money.first().currency)

        val rialsNum = NumberParser.extractNumbers("۵۰۰۰۰۰۰ ریال")
        val rialsMoney = MoneyParser.extractMoney("۵۰۰۰۰۰۰ ریال", rialsNum)
        assertFalse(rialsMoney.isEmpty())
        assertEquals(500000.0, rialsMoney.first().amountInTomans, 0.001) // 5,000,000 Rial = 500,000 Toman

        val dollarNum = NumberParser.extractNumbers("$500 for vaccine")
        val dollarMoney = MoneyParser.extractMoney("$500 for vaccine", dollarNum)
        assertFalse(dollarMoney.isEmpty())
        assertEquals(Currency.USD, dollarMoney.first().currency)
    }

    @Test
    fun testMoneyParserDoesNotConfuseBirdCountWithSaleMoney() {
        val faText = "۵۰ مرغ سالن ۱ فروختم ۱۵ میلیون تومان"
        val faNumbers = NumberParser.extractNumbers(faText)
        val faMoney = MoneyParser.extractMoney(faText, faNumbers)
        assertEquals(1, faMoney.size)
        assertEquals(15_000_000.0, faMoney.first().amountInTomans, 0.001)

        val enText = "sold 40 birds from flock 1 for 500"
        val enNumbers = NumberParser.extractNumbers(enText)
        val enMoney = MoneyParser.extractMoney(enText, enNumbers)
        assertEquals(1, enMoney.size)
        assertEquals(500.0, enMoney.first().amountInTomans, 0.001)
    }
}


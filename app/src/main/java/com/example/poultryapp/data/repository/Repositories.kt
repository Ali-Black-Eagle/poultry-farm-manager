package com.example.poultryapp.data.repository

import android.content.Context
import com.example.poultryapp.data.local.AppDatabaseHelper
import com.example.poultryapp.data.local.AppSettings
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.local.FeedWaterCharge
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.BirdTransfer
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.RationPlan
import com.example.poultryapp.data.backup.FullAppBackupManager
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.JalaliDateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FlockRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllFlocksFlow(): Flow<List<Flock>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllFlocks()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllFlocks(): List<Flock> = withContext(Dispatchers.IO) {
        dbHelper.getAllFlocks()
    }

    suspend fun getFlockById(id: Long): Flock? = withContext(Dispatchers.IO) {
        dbHelper.getFlockById(id)
    }

    suspend fun insertFlock(flock: Flock): Long = withContext(Dispatchers.IO) {
        dbHelper.insertFlock(flock)
    }

    suspend fun updateFlock(flock: Flock): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateFlock(flock)
    }

    suspend fun deleteFlock(flockId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteFlock(flockId)
    }

    suspend fun transferBirds(
        fromFlockId: Long,
        toFlockId: Long,
        birdCount: Int,
        dateJalaliStr: String,
        notes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.transferBirds(fromFlockId, toFlockId, birdCount, dateJalaliStr, notes)
    }

    suspend fun updateMedicationWithdrawal(
        flockId: Long,
        endMillis: Long?,
        medicationName: String?
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateFlockMedicationWithdrawal(flockId, endMillis, medicationName)
    }

    fun getAllTransfersFlow(): Flow<List<BirdTransfer>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllBirdTransfers()
        }.flowOn(Dispatchers.IO)
    }
}

class FeedWaterRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getChargesUpdateFlow(): Flow<Long> = dbHelper.dbUpdateSignal

    suspend fun logCharge(
        flockId: Long,
        itemType: ChargeItemType,
        amount: Double,
        notes: String? = null,
        dateJalaliStr: String? = null,
        timestampMillis: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val now = timestampMillis ?: System.currentTimeMillis()
        val jalaliDate = dateJalaliStr ?: JalaliDateHelper.getJalaliDate(now).toStandardString()
        val charge = FeedWaterCharge(
            flockId = flockId,
            itemType = itemType,
            chargedAmount = amount,
            chargeTimestampMillis = now,
            dateJalaliStr = jalaliDate,
            notes = notes
        )
        dbHelper.insertCharge(charge)
    }

    suspend fun getLatestCharge(flockId: Long, itemType: ChargeItemType): FeedWaterCharge? = withContext(Dispatchers.IO) {
        dbHelper.getLatestChargeForFlock(flockId, itemType)
    }

    suspend fun getTodayChargesTotal(flockId: Long, itemType: ChargeItemType): Double = withContext(Dispatchers.IO) {
        val todayStr = JalaliDateHelper.getJalaliDate().toStandardString()
        val charges = dbHelper.getChargesForFlockAndDate(flockId, todayStr)
        charges.filter { it.itemType == itemType }.sumOf { it.chargedAmount }
    }

    suspend fun getChargesHistory(flockId: Long): List<FeedWaterCharge> = withContext(Dispatchers.IO) {
        dbHelper.getAllChargesForFlock(flockId)
    }

    fun getAllChargesFlow(): Flow<List<FeedWaterCharge>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllCharges()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllCharges(): List<FeedWaterCharge> = withContext(Dispatchers.IO) {
        dbHelper.getAllCharges()
    }

    suspend fun deleteCharge(chargeId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteCharge(chargeId)
    }
}

class ScheduleRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getSchedulesFlow(): Flow<List<CalendarSchedule>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllSchedules()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllSchedules(): List<CalendarSchedule> = withContext(Dispatchers.IO) {
        dbHelper.getAllSchedules()
    }

    suspend fun insertSchedule(schedule: CalendarSchedule): Long = withContext(Dispatchers.IO) {
        dbHelper.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: CalendarSchedule): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(scheduleId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteSchedule(scheduleId)
    }

    suspend fun toggleSchedule(scheduleId: Long, isEnabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        dbHelper.toggleSchedule(scheduleId, isEnabled)
    }
}

class SettingsRepository(private val context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun getSettings(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            themeMode = dbHelper.getSetting("theme_mode", "SYSTEM"),
            alarmSound = dbHelper.getSetting("alarm_sound", "DEFAULT"),
            feedWaterAlertsEnabled = dbHelper.getSetting("alert_feed_water", "true").toBoolean(),
            calendarAlertsEnabled = dbHelper.getSetting("alert_calendar", "true").toBoolean(),
            morningChargeAlertEnabled = dbHelper.getSetting("alert_morning", "true").toBoolean(),
            morningChargeHour = dbHelper.getSetting("morning_hour", "7").toIntOrNull() ?: 7,
            morningChargeMinute = dbHelper.getSetting("morning_minute", "0").toIntOrNull() ?: 0,
            lowThresholdPercent = dbHelper.getSetting("low_threshold", "15.0").toDoubleOrNull() ?: 15.0,
            farmProvince = dbHelper.getSetting("farm_province", ""),
            farmCity = dbHelper.getSetting("farm_city", ""),
            farmLatitude = dbHelper.getSetting("farm_latitude", "").toDoubleOrNull(),
            farmLongitude = dbHelper.getSetting("farm_longitude", "").toDoubleOrNull(),
            cachedTempCelsius = dbHelper.getSetting("cached_temp_celsius", "").toDoubleOrNull(),
            cachedHumidityPercent = dbHelper.getSetting("cached_humidity", "").toIntOrNull(),
            cachedWindSpeedKmH = dbHelper.getSetting("cached_wind_speed", "").toDoubleOrNull(),
            cachedWeatherCode = dbHelper.getSetting("cached_weather_code", "").toIntOrNull(),
            manualTempCelsius = dbHelper.getSetting("manual_temp_celsius", "").toDoubleOrNull(),
            lastWeatherFetchMillis = dbHelper.getSetting("last_weather_fetch_millis", "0").toLongOrNull() ?: 0L,
            isOnlineWeatherEnabled = dbHelper.getSetting("is_online_weather_enabled", "true").toBoolean(),
            isWorkerModeEnabled = dbHelper.getSetting("is_worker_mode", "false").toBoolean(),
            managerPinHash = dbHelper.getSetting("manager_pin_hash", ""),
            managerPinSalt = dbHelper.getSetting("manager_pin_salt", ""),
            managerRecoveryKey = dbHelper.getSetting("manager_recovery_key", ""),
            isBiometricEnabled = dbHelper.getSetting("is_biometric_enabled", "true").toBoolean(),
            isAutoAddEggsToWarehouseEnabled = dbHelper.getSetting("is_auto_add_eggs_warehouse", "true").toBoolean()
        )
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dbHelper.saveSetting("theme_mode", settings.themeMode)
        dbHelper.saveSetting("alarm_sound", settings.alarmSound)
        dbHelper.saveSetting("alert_feed_water", settings.feedWaterAlertsEnabled.toString())
        dbHelper.saveSetting("alert_calendar", settings.calendarAlertsEnabled.toString())
        dbHelper.saveSetting("alert_morning", settings.morningChargeAlertEnabled.toString())
        dbHelper.saveSetting("morning_hour", settings.morningChargeHour.toString())
        dbHelper.saveSetting("morning_minute", settings.morningChargeMinute.toString())
        dbHelper.saveSetting("low_threshold", settings.lowThresholdPercent.toString())
        dbHelper.saveSetting("farm_province", settings.farmProvince)
        dbHelper.saveSetting("farm_city", settings.farmCity)
        dbHelper.saveSetting("farm_latitude", settings.farmLatitude?.toString() ?: "")
        dbHelper.saveSetting("farm_longitude", settings.farmLongitude?.toString() ?: "")
        dbHelper.saveSetting("cached_temp_celsius", settings.cachedTempCelsius?.toString() ?: "")
        dbHelper.saveSetting("cached_humidity", settings.cachedHumidityPercent?.toString() ?: "")
        dbHelper.saveSetting("cached_wind_speed", settings.cachedWindSpeedKmH?.toString() ?: "")
        dbHelper.saveSetting("cached_weather_code", settings.cachedWeatherCode?.toString() ?: "")
        dbHelper.saveSetting("manual_temp_celsius", settings.manualTempCelsius?.toString() ?: "")
        dbHelper.saveSetting("last_weather_fetch_millis", settings.lastWeatherFetchMillis.toString())
        dbHelper.saveSetting("is_online_weather_enabled", settings.isOnlineWeatherEnabled.toString())
        dbHelper.saveSetting("is_worker_mode", settings.isWorkerModeEnabled.toString())
        dbHelper.saveSetting("manager_pin_hash", settings.managerPinHash)
        dbHelper.saveSetting("manager_pin_salt", settings.managerPinSalt)
        dbHelper.saveSetting("manager_recovery_key", settings.managerRecoveryKey)
        dbHelper.saveSetting("is_biometric_enabled", settings.isBiometricEnabled.toString())
        dbHelper.saveSetting("is_auto_add_eggs_warehouse", settings.isAutoAddEggsToWarehouseEnabled.toString())
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        FullAppBackupManager.createFullBackupJson(context)
    }

    suspend fun importBackupJson(json: String): Boolean = withContext(Dispatchers.IO) {
        FullAppBackupManager.restoreFullBackup(context, json)
    }

    suspend fun mergeBackupJson(json: String): Boolean = withContext(Dispatchers.IO) {
        FullAppBackupManager.mergeFullBackup(context, json)
    }
}

class WarehouseRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getWarehouseItemsFlow(): Flow<List<com.example.poultryapp.data.local.WarehouseItem>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllWarehouseItems()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllWarehouseItems(): List<com.example.poultryapp.data.local.WarehouseItem> = withContext(Dispatchers.IO) {
        dbHelper.getAllWarehouseItems()
    }

    suspend fun getWarehouseItemById(id: Long): com.example.poultryapp.data.local.WarehouseItem? = withContext(Dispatchers.IO) {
        dbHelper.getWarehouseItemById(id)
    }

    suspend fun insertWarehouseItem(item: com.example.poultryapp.data.local.WarehouseItem): Long = withContext(Dispatchers.IO) {
        dbHelper.insertWarehouseItem(item)
    }

    suspend fun updateWarehouseItem(item: com.example.poultryapp.data.local.WarehouseItem): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateWarehouseItem(item)
    }

    suspend fun deleteWarehouseItem(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteWarehouseItem(id)
    }

    suspend fun autoAddEggs(flockId: Long, flockName: String, fertileCount: Int, tableCount: Int, dateJalaliStr: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.autoAddProducedEggsToWarehouse(flockId, flockName, fertileCount, tableCount, dateJalaliStr)
    }
}

class CustomBreedRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getCustomBreedsFlow(): Flow<List<com.example.poultryapp.data.local.CustomBreed>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllCustomBreeds()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllCustomBreeds(): List<com.example.poultryapp.data.local.CustomBreed> = withContext(Dispatchers.IO) {
        dbHelper.getAllCustomBreeds()
    }

    suspend fun getCustomBreedById(id: Long): com.example.poultryapp.data.local.CustomBreed? = withContext(Dispatchers.IO) {
        dbHelper.getCustomBreedById(id)
    }

    suspend fun insertCustomBreed(breed: com.example.poultryapp.data.local.CustomBreed): Long = withContext(Dispatchers.IO) {
        dbHelper.insertCustomBreed(breed)
    }

    suspend fun updateCustomBreed(breed: com.example.poultryapp.data.local.CustomBreed): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateCustomBreed(breed)
    }

    suspend fun deleteCustomBreed(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteCustomBreed(id)
    }
}

class EggProductionRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllEggProductionsFlow(): Flow<List<EggProduction>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllEggProductions()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllEggProductions(): List<EggProduction> = withContext(Dispatchers.IO) {
        dbHelper.getAllEggProductions()
    }

    suspend fun getEggProductionsForFlock(flockId: Long): List<EggProduction> = withContext(Dispatchers.IO) {
        dbHelper.getEggProductionsForFlock(flockId)
    }

    suspend fun insertEggProduction(production: EggProduction): Long = withContext(Dispatchers.IO) {
        dbHelper.insertEggProduction(production)
    }

    suspend fun deleteEggProduction(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteEggProduction(id)
    }
}

class FinancialRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllTransactionsFlow(): Flow<List<FinancialTransaction>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllFinancialTransactions()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllTransactions(): List<FinancialTransaction> = withContext(Dispatchers.IO) {
        dbHelper.getAllFinancialTransactions()
    }

    suspend fun insertTransaction(tx: FinancialTransaction): Long = withContext(Dispatchers.IO) {
        dbHelper.insertFinancialTransaction(tx)
    }

    suspend fun deleteTransaction(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteFinancialTransaction(id)
    }

    suspend fun consumeWarehouseStock(
        itemId: Long,
        amount: Double,
        flockIds: List<Long>,
        dateJalaliStr: String,
        notes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.consumeWarehouseItem(itemId, amount, flockIds, dateJalaliStr, notes)
    }

    suspend fun consumeWarehouseStock(
        itemId: Long,
        amount: Double,
        flockId: Long?,
        dateJalaliStr: String,
        notes: String? = null
    ): Boolean = consumeWarehouseStock(itemId, amount, flockId?.let { listOf(it) } ?: emptyList(), dateJalaliStr, notes)

    suspend fun sellEggStock(
        itemId: Long,
        count: Double,
        pricePerUnit: Double,
        dateJalaliStr: String,
        notes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.sellEggItem(itemId, count, pricePerUnit, dateJalaliStr, notes)
    }
}

class RationRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllRationPlansFlow(): Flow<List<RationPlan>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllRationPlans()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllRationPlans(): List<RationPlan> = withContext(Dispatchers.IO) {
        dbHelper.getAllRationPlans()
    }

    suspend fun getRationPlanById(id: Long): RationPlan? = withContext(Dispatchers.IO) {
        dbHelper.getRationPlanById(id)
    }

    suspend fun insertRationPlan(plan: RationPlan): Long = withContext(Dispatchers.IO) {
        dbHelper.insertRationPlan(plan)
    }

    suspend fun updateRationPlan(plan: RationPlan): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateRationPlan(plan)
    }

    suspend fun deleteRationPlan(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteRationPlan(id)
    }

    fun getAllCustomFeedIngredientsFlow(): Flow<List<com.example.poultryapp.data.model.PoultryFeedRawItem>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllCustomFeedIngredients()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun insertCustomFeedIngredient(item: com.example.poultryapp.data.model.PoultryFeedRawItem): Boolean = withContext(Dispatchers.IO) {
        dbHelper.insertCustomFeedIngredient(item)
    }

    suspend fun deleteCustomFeedIngredient(id: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteCustomFeedIngredient(id)
    }
}

class MortalityRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllMortalityLogsFlow(): Flow<List<com.example.poultryapp.data.local.FlockMortalityLog>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllMortalityLogs()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllMortalityLogs(): List<com.example.poultryapp.data.local.FlockMortalityLog> = withContext(Dispatchers.IO) {
        dbHelper.getAllMortalityLogs()
    }

    suspend fun insertMortalityLog(log: com.example.poultryapp.data.local.FlockMortalityLog): Long = withContext(Dispatchers.IO) {
        dbHelper.insertMortalityLog(log)
    }

    suspend fun deleteMortalityLog(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteMortalityLog(id)
    }
}

class FarmContactRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllContactsFlow(): Flow<List<com.example.poultryapp.data.local.FarmContact>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllContacts()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllContacts(): List<com.example.poultryapp.data.local.FarmContact> = withContext(Dispatchers.IO) {
        dbHelper.getAllContacts()
    }

    suspend fun insertContact(contact: com.example.poultryapp.data.local.FarmContact): Long = withContext(Dispatchers.IO) {
        dbHelper.insertContact(contact)
    }

    suspend fun updateContact(contact: com.example.poultryapp.data.local.FarmContact): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateContact(contact)
    }

    suspend fun deleteContact(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteContact(id)
    }
}

class BiosecurityRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getBiosecurityChecksFlow(season: com.example.poultryapp.engine.FarmSeason? = null): Flow<List<com.example.poultryapp.data.local.BiosecurityCheckItem>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getBiosecurityChecks(season)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getBiosecurityChecks(season: com.example.poultryapp.engine.FarmSeason? = null): List<com.example.poultryapp.data.local.BiosecurityCheckItem> = withContext(Dispatchers.IO) {
        dbHelper.getBiosecurityChecks(season)
    }

    suspend fun toggleBiosecurityCheck(id: String, isCompleted: Boolean, dateJalaliStr: String? = null): Boolean = withContext(Dispatchers.IO) {
        dbHelper.toggleBiosecurityCheck(id, isCompleted, dateJalaliStr)
    }
}

class BirdExitRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    fun getAllBirdSalesFlow(): Flow<List<com.example.poultryapp.data.local.BirdSaleLog>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllBirdSaleLogs()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllBirdSales(): List<com.example.poultryapp.data.local.BirdSaleLog> = withContext(Dispatchers.IO) {
        dbHelper.getAllBirdSaleLogs()
    }

    suspend fun getBirdSaleById(id: Long): com.example.poultryapp.data.local.BirdSaleLog? = withContext(Dispatchers.IO) {
        dbHelper.getBirdSaleLogById(id)
    }

    suspend fun insertBirdSaleLog(
        log: com.example.poultryapp.data.local.BirdSaleLog,
        createFinanceTransaction: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        dbHelper.insertBirdSaleLog(log, createFinanceTransaction)
    }

    suspend fun deleteBirdSaleLog(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteBirdSaleLog(id)
    }

    fun getAllBirdSlaughtersFlow(): Flow<List<com.example.poultryapp.data.local.BirdSlaughterLog>> {
        return dbHelper.dbUpdateSignal.map {
            dbHelper.getAllBirdSlaughterLogs()
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllBirdSlaughters(): List<com.example.poultryapp.data.local.BirdSlaughterLog> = withContext(Dispatchers.IO) {
        dbHelper.getAllBirdSlaughterLogs()
    }

    suspend fun getBirdSlaughterById(id: Long): com.example.poultryapp.data.local.BirdSlaughterLog? = withContext(Dispatchers.IO) {
        dbHelper.getBirdSlaughterLogById(id)
    }

    suspend fun insertBirdSlaughterLog(log: com.example.poultryapp.data.local.BirdSlaughterLog): Long = withContext(Dispatchers.IO) {
        dbHelper.insertBirdSlaughterLog(log)
    }

    suspend fun deleteBirdSlaughterLog(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteBirdSlaughterLog(id)
    }
}

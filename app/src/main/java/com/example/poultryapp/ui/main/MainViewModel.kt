package com.example.poultryapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poultryapp.data.local.AppSettings
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.local.FeedWaterCharge
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.repository.FeedWaterRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.data.repository.ScheduleRepository
import com.example.poultryapp.data.repository.SettingsRepository
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.receiver.AlarmScheduler
import com.example.poultryapp.receiver.LiveFeedWaterNotificationManager
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.data.repository.WarehouseRepository
import com.example.poultryapp.engine.toPersianDigits
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.ContactCategory
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.data.repository.MortalityRepository
import com.example.poultryapp.data.repository.FarmContactRepository
import com.example.poultryapp.security.AppCryptoManager

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val flockRepo = FlockRepository(application)
    private val feedWaterRepo = FeedWaterRepository(application)
    private val scheduleRepo = ScheduleRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val warehouseRepo = WarehouseRepository(application)
    private val customBreedRepo = com.example.poultryapp.data.repository.CustomBreedRepository(application)
    private val eggRepo = com.example.poultryapp.data.repository.EggProductionRepository(application)
    private val financialRepo = com.example.poultryapp.data.repository.FinancialRepository(application)
    private val rationRepo = com.example.poultryapp.data.repository.RationRepository(application)
    private val mortalityRepo = MortalityRepository(application)
    private val birdExitRepo = com.example.poultryapp.data.repository.BirdExitRepository(application)
    private val contactRepo = FarmContactRepository(application)
    private val biosecurityRepo = com.example.poultryapp.data.repository.BiosecurityRepository(application)

    private val _flocks = MutableStateFlow<List<Flock>>(emptyList())
    val flocks: StateFlow<List<Flock>> = _flocks.asStateFlow()

    private val _biosecurityChecks = MutableStateFlow<List<com.example.poultryapp.data.local.BiosecurityCheckItem>>(emptyList())
    val biosecurityChecks: StateFlow<List<com.example.poultryapp.data.local.BiosecurityCheckItem>> = _biosecurityChecks.asStateFlow()

    private val _rationPlans = MutableStateFlow<List<com.example.poultryapp.data.local.RationPlan>>(emptyList())
    val rationPlans: StateFlow<List<com.example.poultryapp.data.local.RationPlan>> = _rationPlans.asStateFlow()

    private val _customFeedIngredients = MutableStateFlow<List<com.example.poultryapp.data.model.PoultryFeedRawItem>>(emptyList())
    val customFeedIngredients: StateFlow<List<com.example.poultryapp.data.model.PoultryFeedRawItem>> = _customFeedIngredients.asStateFlow()

    private val _customBreeds = MutableStateFlow<List<com.example.poultryapp.data.local.CustomBreed>>(emptyList())
    val customBreeds: StateFlow<List<com.example.poultryapp.data.local.CustomBreed>> = _customBreeds.asStateFlow()

    private val _eggProductions = MutableStateFlow<List<com.example.poultryapp.data.local.EggProduction>>(emptyList())
    val eggProductions: StateFlow<List<com.example.poultryapp.data.local.EggProduction>> = _eggProductions.asStateFlow()

    private val _financialTransactions = MutableStateFlow<List<com.example.poultryapp.data.local.FinancialTransaction>>(emptyList())
    val financialTransactions: StateFlow<List<com.example.poultryapp.data.local.FinancialTransaction>> = _financialTransactions.asStateFlow()

    private val _birdTransfers = MutableStateFlow<List<com.example.poultryapp.data.local.BirdTransfer>>(emptyList())
    val birdTransfers: StateFlow<List<com.example.poultryapp.data.local.BirdTransfer>> = _birdTransfers.asStateFlow()

    private val _selectedFlockId = MutableStateFlow<Long?>(null)
    val selectedFlockId: StateFlow<Long?> = _selectedFlockId.asStateFlow()

    private val _latestFeedCharge = MutableStateFlow<FeedWaterCharge?>(null)
    val latestFeedCharge: StateFlow<FeedWaterCharge?> = _latestFeedCharge.asStateFlow()

    private val _latestWaterCharge = MutableStateFlow<FeedWaterCharge?>(null)
    val latestWaterCharge: StateFlow<FeedWaterCharge?> = _latestWaterCharge.asStateFlow()

    private val _todayFeedTotal = MutableStateFlow(0.0)
    val todayFeedTotal: StateFlow<Double> = _todayFeedTotal.asStateFlow()

    private val _todayWaterTotal = MutableStateFlow(0.0)
    val todayWaterTotal: StateFlow<Double> = _todayWaterTotal.asStateFlow()

    private val _chargeHistory = MutableStateFlow<List<FeedWaterCharge>>(emptyList())
    val chargeHistory: StateFlow<List<FeedWaterCharge>> = _chargeHistory.asStateFlow()

    private val _allFarmCharges = MutableStateFlow<List<FeedWaterCharge>>(emptyList())
    val allFarmCharges: StateFlow<List<FeedWaterCharge>> = _allFarmCharges.asStateFlow()

    private val _schedules = MutableStateFlow<List<CalendarSchedule>>(emptyList())
    val schedules: StateFlow<List<CalendarSchedule>> = _schedules.asStateFlow()

    private val _warehouseItems = MutableStateFlow<List<WarehouseItem>>(emptyList())
    val warehouseItems: StateFlow<List<WarehouseItem>> = _warehouseItems.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    private val _mortalityLogs = MutableStateFlow<List<FlockMortalityLog>>(emptyList())
    val mortalityLogs: StateFlow<List<FlockMortalityLog>> = _mortalityLogs.asStateFlow()

    private val _birdSales = MutableStateFlow<List<com.example.poultryapp.data.local.BirdSaleLog>>(emptyList())
    val birdSales: StateFlow<List<com.example.poultryapp.data.local.BirdSaleLog>> = _birdSales.asStateFlow()

    private val _birdSlaughters = MutableStateFlow<List<com.example.poultryapp.data.local.BirdSlaughterLog>>(emptyList())
    val birdSlaughters: StateFlow<List<com.example.poultryapp.data.local.BirdSlaughterLog>> = _birdSlaughters.asStateFlow()

    private val _farmContacts = MutableStateFlow<List<FarmContact>>(emptyList())
    val farmContacts: StateFlow<List<FarmContact>> = _farmContacts.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.MANAGER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _weatherState = MutableStateFlow<com.example.poultryapp.service.WeatherResult?>(null)
    val weatherState: StateFlow<com.example.poultryapp.service.WeatherResult?> = _weatherState.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private var weatherTickerJob: kotlinx.coroutines.Job? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    init {
        observeFlocks()
        observeSchedules()
        observeWarehouse()
        observeCustomBreeds()
        observeAllCharges()
        observeEggProductions()
        observeFinancialTransactions()
        observeBirdTransfers()
        observeRationPlans()
        observeCustomFeedIngredients()
        observeMortalityLogs()
        observeBirdExits()
        observeFarmContacts()
        observeBiosecurity()
        loadSettings()
        startWeatherPeriodicSync()
        registerNetworkCallback()
    }

    private fun startWeatherPeriodicSync() {
        weatherTickerJob?.cancel()
        weatherTickerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000L) // بروزرسانی خودکار هر ۵ دقیقه
                refreshWeather(forceOnline = true)
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val cm = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (cm != null) {
                val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(2000L) // تاخیر کوتاه برای برقراری کامل اتصال و DNS
                            refreshWeather(forceOnline = true)
                        }
                    }
                }
                val request = android.net.NetworkRequest.Builder()
                    .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
                networkCallback = callback
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        weatherTickerJob?.cancel()
        try {
            val cm = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeAllCharges() {
        viewModelScope.launch {
            feedWaterRepo.getAllChargesFlow().collect { list ->
                _allFarmCharges.value = list
            }
        }
    }

    private fun observeFlocks() {
        viewModelScope.launch {
            flockRepo.getAllFlocksFlow().collect { list ->
                _flocks.value = list
                if (_selectedFlockId.value == null && list.isNotEmpty()) {
                    _selectedFlockId.value = list.first().id
                }
                loadChargeDataForSelectedFlock()
            }
        }
    }

    private fun observeSchedules() {
        viewModelScope.launch {
            scheduleRepo.getSchedulesFlow().collect { list ->
                _schedules.value = list
            }
        }
    }

    private fun observeWarehouse() {
        viewModelScope.launch {
            warehouseRepo.getWarehouseItemsFlow().collect { list ->
                _warehouseItems.value = list
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val loaded = settingsRepo.getSettings()
            _settings.value = loaded
            _isSettingsLoaded.value = true
            if (loaded.isWorkerModeEnabled && loaded.managerPinHash.isNotBlank()) {
                _userRole.value = UserRole.WORKER
            } else {
                _userRole.value = UserRole.MANAGER
            }
            // بارگذاری فوری دمای آخرین بار ذخیره‌شده تا در هنگام باز شدن برنامه بدون اینترنت، آخرین دما بلافاصله نمایش داده شده و ریست نشود
            val cachedResult = com.example.poultryapp.service.WeatherAndLocationService.getCachedOrOfflineWeather(loaded)
            if (cachedResult != null) {
                _weatherState.value = cachedResult
            }
            refreshWeather(forceOnline = true)
        }
    }

    fun selectFlock(flockId: Long) {
        if (_selectedFlockId.value == flockId) return
        _selectedFlockId.value = flockId
        // Clear charges immediately to avoid race condition where previous flock's depleted status flashes
        _latestFeedCharge.value = null
        _latestWaterCharge.value = null
        loadChargeDataForSelectedFlock()
    }

    fun loadChargeDataForSelectedFlock() {
        val fid = _selectedFlockId.value ?: return
        viewModelScope.launch {
            val feed = feedWaterRepo.getLatestCharge(fid, ChargeItemType.FEED)
            val water = feedWaterRepo.getLatestCharge(fid, ChargeItemType.WATER)
            val todayFeed = feedWaterRepo.getTodayChargesTotal(fid, ChargeItemType.FEED)
            val todayWater = feedWaterRepo.getTodayChargesTotal(fid, ChargeItemType.WATER)
            val history = feedWaterRepo.getChargesHistory(fid)
            val allFarm = feedWaterRepo.getAllCharges()
            if (_selectedFlockId.value == fid) {
                _latestFeedCharge.value = feed
                _latestWaterCharge.value = water
                _todayFeedTotal.value = todayFeed
                _todayWaterTotal.value = todayWater
                _chargeHistory.value = history
                _allFarmCharges.value = allFarm
            }
        }
    }

    fun addFlock(
        name: String,
        species: BirdSpecies,
        breed: String,
        ageDays: Int,
        birdCount: Int,
        notes: String?,
        subGroups: List<com.example.poultryapp.data.local.FlockSubGroup> = emptyList(),
        isFeedEnabled: Boolean = true,
        isWaterEnabled: Boolean = true,
        customTempCelsius: Double? = null
    ) {
        viewModelScope.launch {
            val birthMillis = subGroups.firstOrNull()?.birthEpochMillis
                ?: (System.currentTimeMillis() - (ageDays.coerceAtLeast(1) * 24L * 60 * 60 * 1000))
            val flock = Flock(
                name = name,
                species = species,
                breed = breed,
                birthEpochMillis = birthMillis,
                birdCount = birdCount,
                notes = notes,
                subGroups = subGroups,
                isFeedEnabled = isFeedEnabled,
                isWaterEnabled = isWaterEnabled,
                customTempCelsius = customTempCelsius
            )
            val newId = flockRepo.insertFlock(flock)
            _selectedFlockId.value = newId
        }
    }

    fun editFlock(flock: Flock) {
        viewModelScope.launch {
            flockRepo.updateFlock(flock)
        }
    }

    fun setFlockCustomTemperature(flockId: Long, tempCelsius: Double?) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId } ?: return@launch
            flockRepo.updateFlock(flock.copy(customTempCelsius = tempCelsius))
            loadChargeDataForSelectedFlock()
        }
    }

    fun toggleFlockFeedTracking(flockId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId } ?: return@launch
            flockRepo.updateFlock(flock.copy(isFeedEnabled = isEnabled))
            if (!isEnabled) {
                AlarmScheduler.cancelChargeWarningAlarms(getApplication(), flockId, ChargeItemType.FEED)
            }
            loadChargeDataForSelectedFlock()
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun toggleFlockWaterTracking(flockId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId } ?: return@launch
            flockRepo.updateFlock(flock.copy(isWaterEnabled = isEnabled))
            if (!isEnabled) {
                AlarmScheduler.cancelChargeWarningAlarms(getApplication(), flockId, ChargeItemType.WATER)
            }
            loadChargeDataForSelectedFlock()
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun deleteFlock(flockId: Long) {
        viewModelScope.launch {
            flockRepo.deleteFlock(flockId)
            AlarmScheduler.cancelChargeWarningAlarms(getApplication(), flockId)
            LiveFeedWaterNotificationManager.cancelNotification(getApplication(), flockId)
            if (_selectedFlockId.value == flockId) {
                _selectedFlockId.value = _flocks.value.firstOrNull { it.id != flockId }?.id
            }
        }
    }

    // ==================== WAREHOUSE CRUD ====================

    fun addWarehouseItem(item: WarehouseItem) {
        viewModelScope.launch {
            val insertedId = warehouseRepo.insertWarehouseItem(item)
            LiveFeedWaterNotificationManager.checkAndNotifyWarehouseLowStock(getApplication())

            // Auto-create financial transaction for Feed Purchase (خرید دان) if stock & price > 0
            if (item.category == WarehouseCategory.FEED && item.currentStock > 0.0 && item.pricePerUnit > 0.0) {
                val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                val flockNames = _flocks.value.filter { it.id in item.assignedFlockIds }.map { it.name }
                val flockNamesStr = if (flockNames.isNotEmpty()) flockNames.joinToString("، ") else null
                financialRepo.insertTransaction(
                    FinancialTransaction(
                        title = "خرید دان (${item.name} - ${toPersianDigits(item.currentStock.toInt())} ${item.unit})" +
                                if (flockNamesStr != null) " - گله: $flockNamesStr" else "",
                        amountTomans = item.currentStock * item.pricePerUnit,
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.FEED_PURCHASE,
                        dateJalaliStr = todayJalali,
                        relatedFlockId = item.assignedFlockIds.firstOrNull(),
                        relatedFlockName = flockNamesStr,
                        relatedWarehouseItemId = insertedId,
                        quantity = item.currentStock,
                        unit = item.unit,
                        unitPriceTomans = item.pricePerUnit,
                        notes = item.notes
                    )
                )
            }
        }
    }

    fun editWarehouseItem(item: WarehouseItem) {
        viewModelScope.launch {
            val oldItem = _warehouseItems.value.find { it.id == item.id }
            warehouseRepo.updateWarehouseItem(item)
            LiveFeedWaterNotificationManager.checkAndNotifyWarehouseLowStock(getApplication())

            // If feed stock was increased during edit, record the purchase transaction for the additional amount
            if (oldItem != null && item.category == WarehouseCategory.FEED && item.currentStock > oldItem.currentStock && item.pricePerUnit > 0.0) {
                val addedAmount = item.currentStock - oldItem.currentStock
                val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                val flockNames = _flocks.value.filter { it.id in item.assignedFlockIds }.map { it.name }
                val flockNamesStr = if (flockNames.isNotEmpty()) flockNames.joinToString("، ") else null
                financialRepo.insertTransaction(
                    FinancialTransaction(
                        title = "خرید دان (${item.name} - ${toPersianDigits(addedAmount.toInt())} ${item.unit})" +
                                if (flockNamesStr != null) " - گله: $flockNamesStr" else "",
                        amountTomans = addedAmount * item.pricePerUnit,
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.FEED_PURCHASE,
                        dateJalaliStr = todayJalali,
                        relatedFlockId = item.assignedFlockIds.firstOrNull(),
                        relatedFlockName = flockNamesStr,
                        relatedWarehouseItemId = item.id,
                        quantity = addedAmount,
                        unit = item.unit,
                        unitPriceTomans = item.pricePerUnit,
                        notes = "افزایش موجودی در ویرایش کالا"
                    )
                )
            }
        }
    }

    fun deleteWarehouseItem(id: Long) {
        viewModelScope.launch {
            warehouseRepo.deleteWarehouseItem(id)
            LiveFeedWaterNotificationManager.cancelWarehouseNotification(getApplication(), id)
        }
    }

    fun adjustWarehouseStock(id: Long, delta: Double) {
        viewModelScope.launch {
            val item = _warehouseItems.value.find { it.id == id } ?: return@launch
            val newStock = (item.currentStock + delta).coerceAtLeast(0.0)
            warehouseRepo.updateWarehouseItem(item.copy(currentStock = newStock))
            LiveFeedWaterNotificationManager.checkAndNotifyWarehouseLowStock(getApplication())

            // If feed stock was increased (+1, +5, +25), record the purchase transaction
            if (delta > 0.0 && item.category == WarehouseCategory.FEED && item.pricePerUnit > 0.0) {
                val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                val flockNames = _flocks.value.filter { it.id in item.assignedFlockIds }.map { it.name }
                val flockNamesStr = if (flockNames.isNotEmpty()) flockNames.joinToString("، ") else null
                financialRepo.insertTransaction(
                    FinancialTransaction(
                        title = "خرید دان (${item.name} - ${toPersianDigits(delta.toInt())} ${item.unit})" +
                                if (flockNamesStr != null) " - گله: $flockNamesStr" else "",
                        amountTomans = delta * item.pricePerUnit,
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.FEED_PURCHASE,
                        dateJalaliStr = todayJalali,
                        relatedFlockId = item.assignedFlockIds.firstOrNull(),
                        relatedFlockName = flockNamesStr,
                        relatedWarehouseItemId = item.id,
                        quantity = delta,
                        unit = item.unit,
                        unitPriceTomans = item.pricePerUnit,
                        notes = "افزایش سریع موجودی انبار"
                    )
                )
            }

            // If item is marked for sale and stock was reduced (delta < 0), record sale profit/income in financial transactions
            if (delta < 0.0 && item.isForSale && item.pricePerUnit > 0.0) {
                val deductedAmount = (-delta).coerceAtMost(item.currentStock)
                if (deductedAmount > 0.0) {
                    val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                    val revenue = deductedAmount * item.pricePerUnit
                    financialRepo.insertTransaction(
                        FinancialTransaction(
                            title = "فروش ${item.name} (${toPersianDigits(deductedAmount.toInt())} ${item.unit})",
                            amountTomans = revenue,
                            type = TransactionType.INCOME,
                            category = if (item.category == WarehouseCategory.EGG) TransactionCategory.EGG_SALE else TransactionCategory.OTHER_INCOME,
                            dateJalaliStr = todayJalali,
                            relatedWarehouseItemId = item.id,
                            quantity = deductedAmount,
                            unit = item.unit,
                            unitPriceTomans = item.pricePerUnit,
                            notes = "کسر و فروش از انبار"
                        )
                    )
                }
            }
        }
    }

    fun depleteWarehouseStock(id: Long) {
        viewModelScope.launch {
            val item = _warehouseItems.value.find { it.id == id } ?: return@launch
            val stockToDeplete = item.currentStock
            if (stockToDeplete <= 0.0) return@launch

            warehouseRepo.updateWarehouseItem(item.copy(currentStock = 0.0))
            LiveFeedWaterNotificationManager.checkAndNotifyWarehouseLowStock(getApplication())

            // If item was marked for sale, record full revenue/profit in financial transactions
            if (item.isForSale && item.pricePerUnit > 0.0) {
                val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
                val revenue = stockToDeplete * item.pricePerUnit
                financialRepo.insertTransaction(
                    FinancialTransaction(
                        title = "فروش و اتمام کامل ${item.name} (${toPersianDigits(stockToDeplete.toInt())} ${item.unit})",
                        amountTomans = revenue,
                        type = TransactionType.INCOME,
                        category = if (item.category == WarehouseCategory.EGG) TransactionCategory.EGG_SALE else TransactionCategory.OTHER_INCOME,
                        dateJalaliStr = todayJalali,
                        relatedWarehouseItemId = item.id,
                        quantity = stockToDeplete,
                        unit = item.unit,
                        unitPriceTomans = item.pricePerUnit,
                        notes = "اتمام و فروش کامل موجودی انبار"
                    )
                )
            }
        }
    }

    fun logCharge(
        flockId: Long,
        itemType: ChargeItemType,
        amount: Double,
        notes: String? = null,
        dateJalaliStr: String? = null,
        timestampMillis: Long? = null
    ) {
        viewModelScope.launch {
            feedWaterRepo.logCharge(flockId, itemType, amount, notes, dateJalaliStr, timestampMillis)
            loadChargeDataForSelectedFlock()

            if (amount > 0.0) {
                // Schedule low and empty warnings for feed/water
                LiveFeedWaterNotificationManager.scheduleChargeLowAndEmptyWarnings(
                    context = getApplication(),
                    flockId = flockId,
                    itemType = itemType,
                    chargedAmount = amount
                )
            }

            // Update live notification in notification bar immediately
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun logEmptyCharge(flockId: Long, itemType: ChargeItemType) {
        logCharge(flockId, itemType, 0.0, "تخلیه و دور ریختن باقیمانده")
    }

    fun deleteCharge(chargeId: Long) {
        viewModelScope.launch {
            feedWaterRepo.deleteCharge(chargeId)
            loadChargeDataForSelectedFlock()
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun setManualTemperature(tempCelsius: Double?) {
        viewModelScope.launch {
            val updatedSettings = _settings.value.copy(manualTempCelsius = tempCelsius)
            _settings.value = updatedSettings
            settingsRepo.saveSettings(updatedSettings)
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun addSchedule(schedule: CalendarSchedule) {
        viewModelScope.launch {
            val id = scheduleRepo.insertSchedule(schedule)
            val created = schedule.copy(id = id)
            AlarmScheduler.scheduleWeeklyAlarm(getApplication(), created)
        }
    }

    fun editSchedule(schedule: CalendarSchedule) {
        viewModelScope.launch {
            scheduleRepo.updateSchedule(schedule)
            AlarmScheduler.scheduleWeeklyAlarm(getApplication(), schedule)
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), scheduleId.toInt())
            scheduleRepo.deleteSchedule(scheduleId)
        }
    }

    fun toggleSchedule(scheduleId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            scheduleRepo.toggleSchedule(scheduleId, isEnabled)
            val sched = _schedules.value.find { it.id == scheduleId }
            if (sched != null) {
                val updated = sched.copy(isEnabled = isEnabled)
                AlarmScheduler.scheduleWeeklyAlarm(getApplication(), updated)
            }
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepo.saveSettings(newSettings)
            _settings.value = newSettings

            // Handle morning alarm toggle
            if (newSettings.morningChargeAlertEnabled) {
                AlarmScheduler.scheduleMorningReminder(
                    getApplication(),
                    newSettings.morningChargeHour,
                    newSettings.morningChargeMinute
                )
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), 9999)
            }

            // Immediately sync or cancel feed/water/warehouse notifications & alarms
            LiveFeedWaterNotificationManager.updateAllLowNotifications(getApplication())
        }
    }

    fun refreshWeather(forceOnline: Boolean = false) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val result = com.example.poultryapp.service.WeatherAndLocationService.resolveEffectiveTemperature(
                _settings.value,
                forceOnline
            )
            if (result != null) {
                _weatherState.value = result
            }
            if (result != null && result.isOnline) {
                val updatedSettings = _settings.value.copy(
                    cachedTempCelsius = result.temperatureCelsius,
                    cachedHumidityPercent = result.humidityPercent,
                    cachedWindSpeedKmH = result.windSpeedKmH,
                    cachedWeatherCode = result.weatherCode,
                    lastWeatherFetchMillis = result.timestampMillis
                )
                settingsRepo.saveSettings(updatedSettings)
                _settings.value = updatedSettings
            }
            _isWeatherLoading.value = false
        }
    }

    fun setFarmLocation(province: String, city: String) {
        viewModelScope.launch {
            val iranCity = com.example.poultryapp.data.model.IranCitiesCatalog.findCityByName(city)
            val updatedSettings = _settings.value.copy(
                farmProvince = province,
                farmCity = city,
                farmLatitude = iranCity?.lat,
                farmLongitude = iranCity?.lon
            )
            settingsRepo.saveSettings(updatedSettings)
            _settings.value = updatedSettings
            refreshWeather(forceOnline = true)
        }
    }

    fun detectLocationGps(onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val loc = com.example.poultryapp.service.WeatherAndLocationService.detectDeviceLocation(getApplication())
            if (loc != null) {
                val nearest = com.example.poultryapp.data.model.IranCitiesCatalog.findNearestCity(loc.latitude, loc.longitude)
                val updatedSettings = _settings.value.copy(
                    farmProvince = nearest.province,
                    farmCity = nearest.name,
                    farmLatitude = loc.latitude,
                    farmLongitude = loc.longitude
                )
                settingsRepo.saveSettings(updatedSettings)
                _settings.value = updatedSettings
                refreshWeather(forceOnline = true)
                onResult(true, "موقعیت فارم شناسایی شد: استان ${nearest.province}، شهر ${nearest.name}")
            } else {
                _isWeatherLoading.value = false
                onResult(false, "امکان دریافت موقعیت از GPS وجود نداشت. لطفاً دسترسی موقعیت مکانی (Location) را بررسی نمایید یا شهر را دستی انتخاب کنید.")
            }
        }
    }

    suspend fun exportBackupJson(): String {
        return settingsRepo.exportBackupJson()
    }

    suspend fun importBackupJson(json: String): Boolean {
        val success = settingsRepo.importBackupJson(json)
        if (success) {
            loadChargeDataForSelectedFlock()
        }
        return success
    }

    // ==================== CUSTOM BREEDS CRUD ====================

    private fun observeCustomBreeds() {
        viewModelScope.launch {
            customBreedRepo.getCustomBreedsFlow().collect { list ->
                _customBreeds.value = list
            }
        }
    }

    fun addCustomBreed(breed: com.example.poultryapp.data.local.CustomBreed) {
        viewModelScope.launch {
            customBreedRepo.insertCustomBreed(breed)
        }
    }

    fun editCustomBreed(breed: com.example.poultryapp.data.local.CustomBreed) {
        viewModelScope.launch {
            customBreedRepo.updateCustomBreed(breed)
        }
    }

    fun deleteCustomBreed(id: Long) {
        viewModelScope.launch {
            customBreedRepo.deleteCustomBreed(id)
        }
    }

    // ==================== BIRD TRANSFERS ====================

    private fun observeBirdTransfers() {
        viewModelScope.launch {
            flockRepo.getAllTransfersFlow().collect { list ->
                _birdTransfers.value = list
            }
        }
    }

    fun transferBirds(fromFlockId: Long, toFlockId: Long, birdCount: Int, notes: String? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val success = flockRepo.transferBirds(fromFlockId, toFlockId, birdCount, todayJalali, notes)
            onComplete?.invoke(success)
        }
    }

    // ==================== EGG PRODUCTIONS ====================

    private fun observeEggProductions() {
        viewModelScope.launch {
            eggRepo.getAllEggProductionsFlow().collect { list ->
                _eggProductions.value = list
            }
        }
    }

    fun logEggProduction(
        flockId: Long,
        healthyCount: Int,
        brokenCount: Int = 0,
        dirtyCount: Int = 0,
        doubleYolkCount: Int = 0,
        dateJalaliStr: String? = null,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val birdCount = flock?.effectiveBirdCount ?: 1
            val total = healthyCount + brokenCount + dirtyCount + doubleYolkCount
            val rate = if (birdCount > 0) (total.toDouble() / birdCount.toDouble()) * 100.0 else 0.0
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()

            val eggProd = com.example.poultryapp.data.local.EggProduction(
                flockId = flockId,
                flockName = flockName,
                healthyCount = healthyCount,
                brokenCount = brokenCount,
                dirtyCount = dirtyCount,
                doubleYolkCount = doubleYolkCount,
                totalCount = total,
                layingRatePercent = rate,
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis(),
                notes = notes
            )
            val id = eggRepo.insertEggProduction(eggProd)
            if (id > 0) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
            onComplete?.invoke(id > 0)
        }
    }

    fun logEggProductionForMultipleFlocks(
        flockIds: List<Long>,
        totalHealthy: Int,
        totalBroken: Int = 0,
        totalDirty: Int = 0,
        totalDoubleYolk: Int = 0,
        dateJalaliStr: String? = null,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (flockIds.isEmpty()) {
                onComplete?.invoke(false)
                return@launch
            }
            if (flockIds.size == 1) {
                logEggProduction(flockIds[0], totalHealthy, totalBroken, totalDirty, totalDoubleYolk, dateJalaliStr, notes, onComplete)
                return@launch
            }

            val selectedFlocks = _flocks.value.filter { it.id in flockIds }
            val totalBirds = selectedFlocks.sumOf { it.effectiveBirdCount }.coerceAtLeast(1)
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()

            var remainingHealthy = totalHealthy
            var remainingBroken = totalBroken
            var remainingDirty = totalDirty
            var remainingDoubleYolk = totalDoubleYolk

            selectedFlocks.forEachIndexed { index, flock ->
                val isLast = index == selectedFlocks.lastIndex
                val ratio = if (totalBirds > 0) flock.effectiveBirdCount.toDouble() / totalBirds.toDouble() else (1.0 / selectedFlocks.size)

                val flockHealthy = if (isLast) remainingHealthy else (totalHealthy * ratio).toInt()
                val flockBroken = if (isLast) remainingBroken else (totalBroken * ratio).toInt()
                val flockDirty = if (isLast) remainingDirty else (totalDirty * ratio).toInt()
                val flockDoubleYolk = if (isLast) remainingDoubleYolk else (totalDoubleYolk * ratio).toInt()

                remainingHealthy -= flockHealthy
                remainingBroken -= flockBroken
                remainingDirty -= flockDirty
                remainingDoubleYolk -= flockDoubleYolk

                val flockTotal = flockHealthy + flockBroken + flockDirty + flockDoubleYolk
                val birdCount = flock.effectiveBirdCount.coerceAtLeast(1)
                val rate = (flockTotal.toDouble() / birdCount.toDouble()) * 100.0

                val eggProd = com.example.poultryapp.data.local.EggProduction(
                    flockId = flock.id,
                    flockName = flock.name,
                    healthyCount = flockHealthy,
                    brokenCount = flockBroken,
                    dirtyCount = flockDirty,
                    doubleYolkCount = flockDoubleYolk,
                    totalCount = flockTotal,
                    layingRatePercent = rate,
                    dateJalaliStr = todayJalali,
                    timestampMillis = System.currentTimeMillis() + index,
                    notes = notes
                )
                val id = eggRepo.insertEggProduction(eggProd)
                if (id > 0 && _settings.value.isAutoAddEggsToWarehouseEnabled) {
                    warehouseRepo.autoAddEggs(flock.id, flock.name, fertileCount = 0, tableCount = flockHealthy, dateJalaliStr = todayJalali)
                }
            }
            com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            onComplete?.invoke(true)
        }
    }

    fun deleteEggProduction(id: Long) {
        viewModelScope.launch {
            eggRepo.deleteEggProduction(id)
        }
    }

    // ==================== FINANCIAL & STOCK ACTIONS ====================

    private fun observeFinancialTransactions() {
        viewModelScope.launch {
            financialRepo.getAllTransactionsFlow().collect { list ->
                _financialTransactions.value = list
            }
        }
    }

    fun consumeWarehouseStock(
        itemId: Long,
        amount: Double,
        flockIds: List<Long>,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val success = financialRepo.consumeWarehouseStock(itemId, amount, flockIds, todayJalali, notes)
            onComplete?.invoke(success)
        }
    }

    fun consumeWarehouseStock(
        itemId: Long,
        amount: Double,
        flockId: Long?,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        consumeWarehouseStock(itemId, amount, flockId?.let { listOf(it) } ?: emptyList(), notes, onComplete)
    }

    fun sellEggStock(
        itemId: Long,
        count: Double,
        pricePerUnit: Double,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val success = financialRepo.sellEggStock(itemId, count, pricePerUnit, todayJalali, notes)
            onComplete?.invoke(success)
        }
    }

    fun addFinancialTransaction(tx: com.example.poultryapp.data.local.FinancialTransaction) {
        viewModelScope.launch {
            financialRepo.insertTransaction(tx)
        }
    }

    fun deleteFinancialTransaction(id: Long) {
        viewModelScope.launch {
            financialRepo.deleteTransaction(id)
        }
    }

    // ==================== RATION PLANS (جیره‌ساز) ====================

    private fun observeRationPlans() {
        viewModelScope.launch {
            rationRepo.getAllRationPlansFlow().collect { list ->
                _rationPlans.value = list
            }
        }
    }

    fun addRationPlan(plan: com.example.poultryapp.data.local.RationPlan, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = rationRepo.insertRationPlan(plan)
            onComplete?.invoke(id)
        }
    }

    fun editRationPlan(plan: com.example.poultryapp.data.local.RationPlan, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = rationRepo.updateRationPlan(plan)
            onComplete?.invoke(success)
        }
    }

    fun deleteRationPlan(id: Long, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = rationRepo.deleteRationPlan(id)
            onComplete?.invoke(success)
        }
    }

    fun duplicateRationPlan(plan: com.example.poultryapp.data.local.RationPlan) {
        val copy = plan.copy(
            id = 0L,
            name = "${plan.name} (کپی)",
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )
        addRationPlan(copy)
    }

    fun addRationToWarehouse(
        plan: com.example.poultryapp.data.local.RationPlan,
        quantityKg: Double,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val category = if (plan.itemType == com.example.poultryapp.data.local.RationItemType.FEED) {
                com.example.poultryapp.data.local.WarehouseCategory.FEED
            } else {
                com.example.poultryapp.data.local.WarehouseCategory.VITAMIN
            }
            val pricePerKg = plan.effectivePricePerKg
            val warehouseItem = com.example.poultryapp.data.local.WarehouseItem(
                id = 0L,
                name = plan.name,
                category = category,
                currentStock = quantityKg,
                unit = "کیلوگرم",
                pricePerUnit = pricePerKg,
                minAlertThreshold = 50.0,
                notes = notes ?: "ثبت شده از جیره‌ساز (قیمت هر کیلو: ${pricePerKg.toLong()} تومان)",
                updatedAtMillis = System.currentTimeMillis()
            )
            val newId = warehouseRepo.insertWarehouseItem(warehouseItem)
            onComplete?.invoke(newId > 0)
        }
    }

    private fun observeCustomFeedIngredients() {
        viewModelScope.launch {
            rationRepo.getAllCustomFeedIngredientsFlow().collect { list ->
                _customFeedIngredients.value = list
            }
        }
    }

    fun addCustomFeedIngredient(item: com.example.poultryapp.data.model.PoultryFeedRawItem, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = rationRepo.insertCustomFeedIngredient(item)
            onComplete?.invoke(success)
        }
    }

    fun deleteCustomFeedIngredient(id: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = rationRepo.deleteCustomFeedIngredient(id)
            onComplete?.invoke(success)
        }
    }

    // ==================== ROLE & PIN MANAGEMENT (حالت کارگر / مدیر) ====================

    fun setManagerPin(pin: String): String {
        val hash = AppCryptoManager.hashPin(pin)
        val recoveryKey = AppCryptoManager.generateRecoveryKey()
        viewModelScope.launch {
            val updated = _settings.value.copy(
                managerPinHash = hash,
                managerRecoveryKey = recoveryKey
            )
            _settings.value = updated
            settingsRepo.saveSettings(updated)
        }
        return recoveryKey
    }

    fun removeManagerPin(): Boolean {
        viewModelScope.launch {
            val updated = _settings.value.copy(
                managerPinHash = "",
                managerRecoveryKey = "",
                isWorkerModeEnabled = false
            )
            _settings.value = updated
            _userRole.value = UserRole.MANAGER
            settingsRepo.saveSettings(updated)
        }
        return true
    }

    fun verifyManagerPin(inputPin: String): Boolean {
        val inputHash = AppCryptoManager.hashPin(inputPin)
        if (inputHash == _settings.value.managerPinHash && inputHash.isNotBlank()) {
            _userRole.value = UserRole.MANAGER
            return true
        }
        return false
    }

    fun unlockWithRecoveryKey(recoveryKey: String): Boolean {
        if (recoveryKey.trim() == _settings.value.managerRecoveryKey.trim() && recoveryKey.isNotBlank()) {
            _userRole.value = UserRole.MANAGER
            return true
        }
        return false
    }

    fun switchToWorkerMode() {
        _userRole.value = UserRole.WORKER
    }

    fun toggleDefaultWorkerMode(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(isWorkerModeEnabled = enabled)
            _settings.value = updated
            settingsRepo.saveSettings(updated)
        }
    }

    // ==================== FLOCK MORTALITY (تلفات و خروج پرنده) ====================

    private fun observeMortalityLogs() {
        viewModelScope.launch {
            mortalityRepo.getAllMortalityLogsFlow().collect { list ->
                _mortalityLogs.value = list
            }
        }
    }

    fun logFlockMortality(
        flockId: Long,
        count: Int,
        reason: MortalityReason,
        diagnosisHint: String? = null,
        notes: String? = null,
        dateJalaliStr: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val log = FlockMortalityLog(
                flockId = flockId,
                flockName = flockName,
                count = count,
                reason = reason,
                diagnosisHint = diagnosisHint,
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis(),
                notes = notes
            )
            val id = mortalityRepo.insertMortalityLog(log)
            if (id > 0) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
            onComplete?.invoke(id > 0)
        }
    }

    fun deleteMortalityLog(id: Long) {
        viewModelScope.launch {
            mortalityRepo.deleteMortalityLog(id)
        }
    }

    // ==================== BIRD SALES & SLAUGHTER (فروش و کشتار پرنده) ====================

    private fun observeBirdExits() {
        viewModelScope.launch {
            birdExitRepo.getAllBirdSalesFlow().collect { list ->
                _birdSales.value = list
            }
        }
        viewModelScope.launch {
            birdExitRepo.getAllBirdSlaughtersFlow().collect { list ->
                _birdSlaughters.value = list
            }
        }
    }

    fun sellBirds(
        flockId: Long,
        birdCount: Int,
        liveWeightKg: Double? = null,
        unitPriceTomans: Double? = null,
        totalRevenueTomans: Double = 0.0,
        buyerName: String? = null,
        dateJalaliStr: String? = null,
        notes: String? = null,
        createFinanceTransaction: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val ageDays = flock?.ageDays ?: 1
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val log = com.example.poultryapp.data.local.BirdSaleLog(
                flockId = flockId,
                flockName = flockName,
                birdCount = birdCount,
                birdAgeDays = ageDays,
                liveWeightKg = liveWeightKg,
                unitPriceTomans = unitPriceTomans,
                totalRevenueTomans = totalRevenueTomans,
                buyerName = buyerName?.takeIf { it.isNotBlank() },
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis(),
                notes = notes?.takeIf { it.isNotBlank() }
            )
            val id = birdExitRepo.insertBirdSaleLog(log, createFinanceTransaction)
            if (id > 0L) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
            onComplete?.invoke(id > 0L)
        }
    }

    fun deleteBirdSale(id: Long) {
        viewModelScope.launch {
            if (birdExitRepo.deleteBirdSaleLog(id)) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
        }
    }

    fun slaughterBirds(
        flockId: Long,
        birdCount: Int,
        birdAgeDays: Int? = null,
        liveWeightKg: Double? = null,
        carcassWeightKg: Double? = null,
        dateJalaliStr: String? = null,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val effectiveAgeDays = (birdAgeDays ?: flock?.ageDays ?: 1).coerceAtLeast(1)
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val log = com.example.poultryapp.data.local.BirdSlaughterLog(
                flockId = flockId,
                flockName = flockName,
                birdCount = birdCount,
                birdAgeDays = effectiveAgeDays,
                liveWeightKg = liveWeightKg,
                carcassWeightKg = carcassWeightKg,
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis(),
                notes = notes?.takeIf { it.isNotBlank() }
            )
            val id = birdExitRepo.insertBirdSlaughterLog(log)
            if (id > 0L) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
            onComplete?.invoke(id > 0L)
        }
    }

    fun deleteBirdSlaughter(id: Long) {
        viewModelScope.launch {
            if (birdExitRepo.deleteBirdSlaughterLog(id)) {
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
        }
    }

    // ==================== FARM CONTACTS (مخاطبین تجاری) ====================

    private fun observeFarmContacts() {
        viewModelScope.launch {
            contactRepo.getAllContactsFlow().collect { list ->
                _farmContacts.value = list
            }
        }
    }

    fun addFarmContact(contact: FarmContact, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = contactRepo.insertContact(contact)
            onComplete?.invoke(id)
        }
    }

    fun editFarmContact(contact: FarmContact, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = contactRepo.updateContact(contact)
            onComplete?.invoke(success)
        }
    }

    fun deleteFarmContact(id: Long, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = contactRepo.deleteContact(id)
            onComplete?.invoke(success)
        }
    }

    // ==================== UPGRADED EGG PRODUCTION LOGGING ====================

    fun logDetailedEggProduction(
        flockId: Long,
        fertileCount: Int = 0,
        tableEggCount: Int = 0,
        brokenCount: Int = 0,
        dirtyCount: Int = 0,
        doubleYolkCount: Int = 0,
        trayCount: Double = 0.0,
        trayGrossWeightGrams: Double? = null,
        dateJalaliStr: String? = null,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val birdCount = flock?.effectiveBirdCount ?: 1
            val total = fertileCount + tableEggCount + brokenCount + dirtyCount + doubleYolkCount
            val rate = if (birdCount > 0) (total.toDouble() / birdCount.toDouble()) * 100.0 else 0.0
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()

            val netAvgWeight = if (trayGrossWeightGrams != null && trayGrossWeightGrams > 50.0) {
                val netTrayWeight = trayGrossWeightGrams - 50.0
                if (trayCount > 0) {
                    netTrayWeight / (trayCount * 30.0)
                } else if (total > 0) {
                    netTrayWeight / total.toDouble()
                } else {
                    null
                }
            } else {
                null
            }

            val eggProd = com.example.poultryapp.data.local.EggProduction(
                flockId = flockId,
                flockName = flockName,
                fertileCount = fertileCount,
                tableEggCount = tableEggCount,
                healthyCount = fertileCount + tableEggCount,
                brokenCount = brokenCount,
                dirtyCount = dirtyCount,
                doubleYolkCount = doubleYolkCount,
                totalCount = total,
                trayCount = trayCount,
                trayGrossWeightGrams = trayGrossWeightGrams,
                netEggAverageWeightGrams = netAvgWeight,
                layingRatePercent = rate,
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis(),
                notes = notes
            )
            val id = eggRepo.insertEggProduction(eggProd)
            if (id > 0) {
                if (_settings.value.isAutoAddEggsToWarehouseEnabled) {
                    warehouseRepo.autoAddEggs(flockId, flockName, fertileCount, tableEggCount, todayJalali)
                }
                com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            }
            onComplete?.invoke(id > 0)
        }
    }

    fun logDetailedEggProductionForMultipleFlocks(
        flockIds: List<Long>,
        totalFertile: Int = 0,
        totalTableEgg: Int = 0,
        totalBroken: Int = 0,
        totalDirty: Int = 0,
        totalDoubleYolk: Int = 0,
        totalTrayCount: Double = 0.0,
        trayGrossWeightGrams: Double? = null,
        dateJalaliStr: String? = null,
        notes: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (flockIds.isEmpty()) {
                onComplete?.invoke(false)
                return@launch
            }
            if (flockIds.size == 1) {
                logDetailedEggProduction(
                    flockId = flockIds[0],
                    fertileCount = totalFertile,
                    tableEggCount = totalTableEgg,
                    brokenCount = totalBroken,
                    dirtyCount = totalDirty,
                    doubleYolkCount = totalDoubleYolk,
                    trayCount = totalTrayCount,
                    trayGrossWeightGrams = trayGrossWeightGrams,
                    dateJalaliStr = dateJalaliStr,
                    notes = notes,
                    onComplete = onComplete
                )
                return@launch
            }

            val selectedFlocks = _flocks.value.filter { it.id in flockIds }
            val totalBirds = selectedFlocks.sumOf { it.effectiveBirdCount }.coerceAtLeast(1)
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()

            var remainingFertile = totalFertile
            var remainingTable = totalTableEgg
            var remainingBroken = totalBroken
            var remainingDirty = totalDirty
            var remainingDoubleYolk = totalDoubleYolk

            selectedFlocks.forEachIndexed { index, flock ->
                val isLast = index == selectedFlocks.lastIndex
                val ratio = if (totalBirds > 0) flock.effectiveBirdCount.toDouble() / totalBirds.toDouble() else (1.0 / selectedFlocks.size)

                val flockFertile = if (isLast) remainingFertile else (totalFertile * ratio).toInt()
                val flockTable = if (isLast) remainingTable else (totalTableEgg * ratio).toInt()
                val flockBroken = if (isLast) remainingBroken else (totalBroken * ratio).toInt()
                val flockDirty = if (isLast) remainingDirty else (totalDirty * ratio).toInt()
                val flockDoubleYolk = if (isLast) remainingDoubleYolk else (totalDoubleYolk * ratio).toInt()

                remainingFertile -= flockFertile
                remainingTable -= flockTable
                remainingBroken -= flockBroken
                remainingDirty -= flockDirty
                remainingDoubleYolk -= flockDoubleYolk

                val flockTotal = flockFertile + flockTable + flockBroken + flockDirty + flockDoubleYolk
                val birdCount = flock.effectiveBirdCount.coerceAtLeast(1)
                val rate = (flockTotal.toDouble() / birdCount.toDouble()) * 100.0

                val eggProd = com.example.poultryapp.data.local.EggProduction(
                    flockId = flock.id,
                    flockName = flock.name,
                    fertileCount = flockFertile,
                    tableEggCount = flockTable,
                    healthyCount = flockFertile + flockTable,
                    brokenCount = flockBroken,
                    dirtyCount = flockDirty,
                    doubleYolkCount = flockDoubleYolk,
                    totalCount = flockTotal,
                    trayCount = totalTrayCount * ratio,
                    trayGrossWeightGrams = trayGrossWeightGrams,
                    netEggAverageWeightGrams = null,
                    layingRatePercent = rate,
                    dateJalaliStr = todayJalali,
                    timestampMillis = System.currentTimeMillis() + index,
                    notes = notes
                )
                val id = eggRepo.insertEggProduction(eggProd)
                if (id > 0 && _settings.value.isAutoAddEggsToWarehouseEnabled) {
                    warehouseRepo.autoAddEggs(flock.id, flock.name, flockFertile, flockTable, todayJalali)
                }
            }
            com.example.poultryapp.widget.FarmStatusWidgetProvider.sendUpdateBroadcast(getApplication())
            onComplete?.invoke(true)
        }
    }

    // ==================== BIOSECURITY & MEDICATION WITHDRAWAL ====================

    private fun observeBiosecurity() {
        viewModelScope.launch {
            biosecurityRepo.getBiosecurityChecksFlow().collect { list ->
                _biosecurityChecks.value = list
            }
        }
    }

    fun toggleBiosecurityCheck(id: String, completed: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val todayJalali = com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val success = biosecurityRepo.toggleBiosecurityCheck(id, completed, if (completed) todayJalali else null)
            onComplete?.invoke(success)
        }
    }

    fun setFlockMedicationWithdrawal(
        flockId: Long,
        withdrawalDays: Int,
        medicationName: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val endMillis = System.currentTimeMillis() + (withdrawalDays.coerceAtLeast(1) * 24L * 60 * 60 * 1000)
            val success = flockRepo.updateMedicationWithdrawal(flockId, endMillis, medicationName.trim())
            onComplete?.invoke(success)
        }
    }

    fun clearFlockMedicationWithdrawal(flockId: Long, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = flockRepo.updateMedicationWithdrawal(flockId, null, null)
            onComplete?.invoke(success)
        }
    }

    fun recordFlockObservation(
        flockId: Long,
        note: String,
        authorRole: com.example.poultryapp.data.local.UserRole = com.example.poultryapp.data.local.UserRole.MANAGER,
        category: String = "OBSERVATION",
        dateJalaliStr: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val flock = _flocks.value.find { it.id == flockId }
            val flockName = flock?.name ?: "گله بدون نام"
            val todayJalali = dateJalaliStr ?: com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString()
            val obs = com.example.poultryapp.data.local.FlockObservationNote(
                flockId = flockId,
                flockName = flockName,
                note = note,
                authorRole = authorRole,
                category = category,
                dateJalaliStr = todayJalali,
                timestampMillis = System.currentTimeMillis()
            )
            val id = com.example.poultryapp.data.local.AppDatabaseHelper.getInstance(getApplication()).insertFlockObservation(obs)
            onComplete?.invoke(id > 0)
        }
    }

    fun deleteFlockObservation(id: Long) {
        viewModelScope.launch {
            com.example.poultryapp.data.local.AppDatabaseHelper.getInstance(getApplication()).deleteFlockObservation(id)
        }
    }
}



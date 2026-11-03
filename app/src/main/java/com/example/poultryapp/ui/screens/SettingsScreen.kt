package com.example.poultryapp.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.poultryapp.data.backup.BackupSummary
import com.example.poultryapp.data.backup.FullAppBackupManager
import com.example.poultryapp.data.local.AppSettings
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.security.AppCryptoManager
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.AppTopBar
import com.example.poultryapp.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    weatherResult: com.example.poultryapp.service.WeatherResult? = null,
    isWeatherLoading: Boolean = false,
    onRefreshWeather: (() -> Unit)? = null,
    onSetFarmLocation: ((province: String, city: String) -> Unit)? = null,
    onDetectGpsLocation: (((Boolean, String) -> Unit) -> Unit)? = null,
    onSaveSettings: (AppSettings) -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Boolean,
    onAddCustomBreed: (com.example.poultryapp.data.local.CustomBreed) -> Unit = {},
    onEditCustomBreed: (com.example.poultryapp.data.local.CustomBreed) -> Unit = {},
    onDeleteCustomBreed: (Long) -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var themeMode by remember(settings) { mutableStateOf(settings.themeMode) }
    var feedWaterAlerts by remember(settings) { mutableStateOf(settings.feedWaterAlertsEnabled) }
    var calendarAlerts by remember(settings) { mutableStateOf(settings.calendarAlertsEnabled) }
    var morningAlerts by remember(settings) { mutableStateOf(settings.morningChargeAlertEnabled) }
    var lowThreshold by remember(settings) { mutableFloatStateOf(settings.lowThresholdPercent.toFloat()) }
    var isOnlineWeather by remember(settings) { mutableStateOf(settings.isOnlineWeatherEnabled) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var isImportMode by remember { mutableStateOf(false) }
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var pendingRestoreJson by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // لانچر ایجاد و ذخیره مستقیم فایل در Google Drive یا حافظه دستگاه
    val saveToDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val fullJson = onExportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        FullAppBackupManager.writeBackupToStream(fullJson, os)
                    }
                    Toast.makeText(context, "✅ نسخه پشتیبان کامل با موفقیت در گوگل درایو ذخیره شد", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "❌ خطا در ذخیره فایل در گوگل درایو: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // لانچر انتخاب و خواندن مستقیم فایل از Google Drive یا حافظه دستگاه
    val restoreFromDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val fileContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                        FullAppBackupManager.readBackupFromStream(stream)
                    }
                    if (!fileContent.isNullOrBlank()) {
                        val summary = FullAppBackupManager.inspectBackup(fileContent)
                        if (summary != null) {
                            pendingRestoreSummary = summary
                            pendingRestoreJson = fileContent
                            showRestoreConfirmDialog = true
                        } else {
                            Toast.makeText(context, "❌ فرمت فایل پشتیبان نامعتبر است", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "❌ خطا در خواندن فایل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var showCustomBreedDialog by remember { mutableStateOf(false) }
    var editingCustomBreed by remember { mutableStateOf<com.example.poultryapp.data.local.CustomBreed?>(null) }
    var deletingBreedId by remember { mutableStateOf<Long?>(null) }
    var showCityDialog by remember { mutableStateOf(false) }
    var isDefaultBreedsExpanded by remember { mutableStateOf(false) }
    var selectedDefaultSpecies by remember { mutableStateOf(com.example.poultryapp.data.model.BirdSpecies.CHICKEN) }

    var isWorkerModeEnabled by remember(settings) { mutableStateOf(settings.isWorkerModeEnabled) }
    var autoLockTimeout by remember(settings) { mutableIntStateOf(settings.autoLockTimeoutMinutes) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRecoveryKeyDialog by remember { mutableStateOf(false) }
    var newlyGeneratedRecoveryKey by remember { mutableStateOf<String?>(null) }

    var isAutoAddEggs by remember(settings) { mutableStateOf(settings.isAutoAddEggsToWarehouseEnabled) }

    fun commitSettings() {
        onSaveSettings(
            settings.copy(
                themeMode = themeMode,
                feedWaterAlertsEnabled = feedWaterAlerts,
                calendarAlertsEnabled = calendarAlerts,
                morningChargeAlertEnabled = morningAlerts,
                lowThresholdPercent = lowThreshold.toDouble(),
                isOnlineWeatherEnabled = isOnlineWeather,
                isWorkerModeEnabled = isWorkerModeEnabled,
                autoLockTimeoutMinutes = autoLockTimeout,
                isAutoAddEggsToWarehouseEnabled = isAutoAddEggs
            )
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "تنظیمات برنامه",
                subtitle = "شخصی‌سازی هشدارها، تم و پشتیبان‌گیری",
                logoRes = com.example.poultryapp.R.drawable.ic_farm_settings,
                navigationIcon = onOpenDrawer?.let {
                    {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenVoiceAssistant?.invoke() }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "دستیار هوشمند",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
            verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
        ) {
            // Theme Section
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text("پوسته ظاهری (تم)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space10))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                        ) {
                            val options = listOf("SYSTEM" to "خودکار", "LIGHT" to "روشن", "DARK" to "تاریک")
                            options.forEach { (mode, label) ->
                                val selected = themeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        themeMode = mode
                                        commitSettings()
                                    },
                                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    shape = RoundedCornerShape(FarmDimens.ChipRadius),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 🛡️ Dual Access Mode (Worker Profile vs Manager Profile)
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                Text(
                                    text = "حالت دسترسی دوگانه (کارگر / مدیر فارم)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space8))
                        Text(
                            text = "امکان سپردن گوشی به کارگر سالن جهت ثبت دان، تخم‌مرغ و تلفات؛ با پنهان‌سازی کامل اطلاعات مالی، سود و زیان و قیمت‌ها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(FarmDimens.Space12))

                        // Worker Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فعال‌سازی پروفایل کارگر سالن",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isWorkerModeEnabled) "در صورت روشن بودن، قفل سریع در هدر برنامه فعال می‌شود" else "غیرفعال (دسترسی نامحدود)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isWorkerModeEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && settings.managerPinHash.isBlank()) {
                                        showSetPinDialog = true
                                    } else {
                                        isWorkerModeEnabled = checked
                                        commitSettings()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space10))

                        // PIN Status & Setting
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (settings.managerPinHash.isNotBlank()) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (settings.managerPinHash.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (settings.managerPinHash.isNotBlank()) "رمز پین مدیریت تنظیم شده است" else "رمز پین مدیر تنظیم نشده است",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                TextButton(
                                    onClick = { showSetPinDialog = true }
                                ) {
                                    Text(
                                        text = if (settings.managerPinHash.isNotBlank()) "تغییر پین" else "تنظیم پین",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Recovery Key Info
                        if (settings.managerPinHash.isNotBlank() && settings.recoveryKey.isNotBlank()) {
                            Spacer(modifier = Modifier.height(FarmDimens.Space8))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🔑 کلید اضطراری بازیابی پین:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = settings.recoveryKey,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(settings.recoveryKey))
                                            Toast.makeText(context, "کلید اضطراری در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی کلید بازیابی", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Farm Location & Climate Section (Iran Cities & Weather)
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                Text(
                                    text = "موقعیت جغرافیایی و اقلیم فارم",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isWeatherLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space6))

                        Text(
                            text = "انتخاب شهر برای محاسبه میانگین دمای فصلی، مصرف آب و تایمر بقای طیور تا اولین تلفات ضروری است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(FarmDimens.Space12))

                        // Current Selected City and Temperature Display
                        if (settings.farmCity.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "شهر ثبت‌شده:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "استان ${settings.farmProvince} — ${settings.farmCity}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        val temp = weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius
                                        if (temp != null) {
                                            Surface(
                                                shape = RoundedCornerShape(FarmDimens.BadgeRadius),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${toPersianDigits(temp)}°C",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (weatherResult != null) {
                                        Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                        Text(
                                            text = "منبع دما: ${weatherResult.sourceDescription}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFD97706)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(FarmDimens.Space10),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                    Text(
                                        text = "هنوز شهری برای مزرعه ثبت نشده است. لطفاً شهر را مشخص کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space12))

                        // Action Buttons: Select City & GPS Auto Detect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                        ) {
                            Button(
                                onClick = { showCityDialog = true },
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text(if (settings.farmCity.isBlank()) "انتخاب شهر فارم" else "تغییر شهر", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onDetectGpsLocation?.invoke { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text("شناسایی با GPS", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (settings.farmCity.isNotBlank()) {
                            Spacer(modifier = Modifier.height(FarmDimens.Space10))

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(FarmDimens.Space8))

                            SettingToggleRow(
                                title = "دریافت آنلاین دمای هواشناسی",
                                subtitle = "دریافت دمای زنده از اینترنت به جای میانگین فصلی شهر",
                                checked = isOnlineWeather,
                                onCheckedChange = {
                                    isOnlineWeather = it
                                    commitSettings()
                                    if (it) {
                                        onRefreshWeather?.invoke()
                                    }
                                }
                            )

                            if (isOnlineWeather) {
                                Spacer(modifier = Modifier.height(FarmDimens.Space6))
                                TextButton(
                                    onClick = { onRefreshWeather?.invoke() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(FarmDimens.Space4))
                                    Text("بروزرسانی آنلاین دما", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Notifications Section
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text("هشدارهای صوتی و نوتیفیکیشن‌ها", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space10))

                        SettingToggleRow(
                            title = "هشدار رو به اتمام بودن دان و آب",
                            subtitle = "نمایش نوتیفیکیشن زنده در نوار اعلان هنگام رسیدن به آستانه بحرانی",
                            checked = feedWaterAlerts,
                            onCheckedChange = {
                                feedWaterAlerts = it
                                commitSettings()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = FarmDimens.Space8),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingToggleRow(
                            title = "یادآورهای تقویم کاری",
                            subtitle = "پخش آلارم در ساعات مقرر برای واکسیناسیون، دارو و ضدعفونی",
                            checked = calendarAlerts,
                            onCheckedChange = {
                                calendarAlerts = it
                                commitSettings()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = FarmDimens.Space8),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingToggleRow(
                            title = "یادآور شارژ صبحگاهی دان و آب (ساعت ۷:۰۰)",
                            subtitle = "یادآوری خودکار روزانه اول وقت برای سرکشی به سالن‌ها",
                            checked = morningAlerts,
                            onCheckedChange = {
                                morningAlerts = it
                                commitSettings()
                            }
                        )
                    }
                }
            }

            // 🏭 زنجیره انبارداری و کسر خودکار خوراک و شارژ تخم‌مرغ
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text("زنجیره هوشمند انبارداری و تولید", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space10))

                        SettingToggleRow(
                            title = "شارژ خودکار تخم‌مرغ‌های تولیدی در انبار",
                            subtitle = "با ثبت کارتن و شانه‌های تخم‌مرغ نطفه‌دار و خوراکی، انبار به طور خودکار افزایش می‌یابد",
                            checked = isAutoAddEggs,
                            onCheckedChange = {
                                isAutoAddEggs = it
                                commitSettings()
                            }
                        )
                    }
                }
            }

            // Low Threshold Slider
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آستانه درصد هشدار کمبود دان و آب",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${toPersianDigits(lowThreshold.toInt())}٪",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space4))

                        Text(
                            text = "هنگامی که باقیمانده دان یا آب به کمتر از این درصد برسد، اعلان کمبود فعال خواهد شد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(FarmDimens.Space6))

                        Slider(
                            value = lowThreshold,
                            onValueChange = { lowThreshold = it },
                            onValueChangeFinished = { commitSettings() },
                            valueRange = 10f..35f,
                            steps = 4
                        )
                    }
                }
            }

            // Custom Breeds & Nutrition Rates Section
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                Text(
                                    text = "نژادهای اختصاصی و نرخ مصرف آب و دان",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    editingCustomBreed = null
                                    showCustomBreedDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نژاد جدید", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space6))

                        Text(
                            text = "نژادهای پرورشی خاص خود را با تعیین روزانه مصرف آب و دان بر حسب هفته رشد پرنده و مرحله بلوغ ثبت کنید تا در تمام محاسبات فارم به کار روند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(FarmDimens.Space12))

                        if (customBreeds.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "هنوز نژاد اختصاصی تعریف نشده است. نژادهای پیش‌فرض برنامه در دسترس هستند. برای تعریف نرخ اختصاصی دکمه «نژاد جدید» را بزنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                customBreeds.forEach { breed ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = breed.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = breed.species.titleFa,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = breed.productionType.titleFa,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            editingCustomBreed = breed
                                                            showCustomBreedDialog = true
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "ویرایش",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { deletingBreedId = breed.id },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.DeleteOutline,
                                                            contentDescription = "حذف",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text(
                                                    text = "دان بالغ: ${toPersianDigits(breed.adultFeedGrams.toInt())} گرم/روز",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "آب بالغ: ${toPersianDigits(breed.adultWaterMl.toInt())} ml/روز",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (breed.rateInputMode == "DAYS" && breed.dailyRates.isNotEmpty()) {
                                                    Text(
                                                        text = "${toPersianDigits(breed.dailyRates.size)} روز رشد (دقیق)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                } else if (breed.weeklyRates.isNotEmpty()) {
                                                    Text(
                                                        text = "${toPersianDigits(breed.weeklyRates.size)} هفته رشد",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            if (!breed.notes.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = breed.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space14))

                        // Default System Breeds (Minimized by default)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isDefaultBreedsExpanded = !isDefaultBreedsExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "نژادهای پیش‌فرض برنامه (سیستم)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isDefaultBreedsExpanded) "برای بستن کلیک کنید" else "برای مشاهده نژادهای استاندارد کلیک کنید",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { isDefaultBreedsExpanded = !isDefaultBreedsExpanded },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDefaultBreedsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isDefaultBreedsExpanded) "بستن" else "باز کردن",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isDefaultBreedsExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Species filter chips
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(com.example.poultryapp.data.model.BirdSpecies.values()) { sp ->
                                            val isSelected = sp == selectedDefaultSpecies
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedDefaultSpecies = sp },
                                                label = { Text(sp.titleFa, style = MaterialTheme.typography.labelSmall) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val defaultBreeds = com.example.poultryapp.data.model.BreedCatalog.getBreedsForSpecies(selectedDefaultSpecies)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        defaultBreeds.forEach { defaultBreed ->
                                            val adultFeed = com.example.poultryapp.engine.DailyNutritionEngine.getBaseAdultDailyFeed(
                                                defaultBreed.species, defaultBreed.nameFa, defaultBreed.productionType
                                            )
                                            val adultWater = com.example.poultryapp.engine.DailyNutritionEngine.getBaseAdultDailyWater(
                                                defaultBreed.species, defaultBreed.nameFa, defaultBreed.productionType
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = defaultBreed.nameFa,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                                            ) {
                                                                Text(
                                                                    text = defaultBreed.productionType.titleFa,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "دان بالغ: ${toPersianDigits(adultFeed.toInt())} گرم/روز | آب: ${toPersianDigits(adultWater.toInt())} ml/روز",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            editingCustomBreed = com.example.poultryapp.data.local.CustomBreed(
                                                                id = 0,
                                                                name = "${defaultBreed.nameFa} (شخصی)",
                                                                species = defaultBreed.species,
                                                                productionType = defaultBreed.productionType,
                                                                adultFeedGrams = adultFeed,
                                                                adultWaterMl = adultWater,
                                                                notes = "برگرفته از نژاد پیش‌فرض ${defaultBreed.nameFa}"
                                                            )
                                                            showCustomBreedDialog = true
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("سفارشی‌سازی", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Backup & Google Drive Center
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(FarmDimens.Space10))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "پشتیبان‌گیری ابری و گوگل درایو (Google Drive)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "یکپارچه: ۱۰ جدول دیتابیس فارم + آکادمی بازی طلایی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space8))

                        Text(
                            text = "از تمام داده‌های گله‌ها، شارژها، انبارداری، تراکنش‌های مالی، تخم‌گذاری، جیره‌های غذایی و وضعیت کامل مراحل و ستاره‌های بازی آکادمی در حساب ابری Google Drive خود پشتیبان بگیرید یا در گوشی دیگر بازیابی کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                        )

                        Spacer(modifier = Modifier.height(FarmDimens.Space12))

                        // Features / Scope Tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "☁️ گوگل درایو",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "🐣 ۱۰ جدول دیتابیس",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "🏆 ستاره‌ها و بورد حرفه‌ای",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "🔒 رمزنگاری AES-256-GCM (ضد سوءاستفاده)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space14))

                        // Action Buttons: Row 1 (Cloud Save Actions)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                        ) {
                            Button(
                                onClick = {
                                    saveToDriveLauncher.launch(FullAppBackupManager.generateDefaultFileName())
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text("ذخیره در گوگل درایو", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }

                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val fullJson = onExportBackup()
                                            val shareIntent = FullAppBackupManager.createShareToDriveIntent(context, fullJson)
                                            context.startActivity(Intent.createChooser(shareIntent, "ذخیره در گوگل درایو یا ارسال فایل"))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "خطا در اشتراک فایل: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text("اشتراک فایل ابری", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(FarmDimens.Space8))

                        // Action Buttons: Row 2 (Restore Actions)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    restoreFromDriveLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text("بازیابی از گوگل درایو", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = {
                                    backupJsonText = ""
                                    isImportMode = true
                                    showBackupDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                Text("ورود متن یا کپی", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // About App Section
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Egg,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text(
                                text = "مدیریت هوشمند طیور (نسخه ۳.۲.۰)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(FarmDimens.Space6))
                        Text(
                            text = "سامانه تخصصی، نیتیو و ۱۰۰٪ آفلاین پرورش و مدیریت گله‌های مرغ، اردک، بوقلمون، غاز و بلدرچین. بدون نیاز به اینترنت، با تقویم شمسی جلالی، دیتابیس محلی رمزنگاری‌شده و موتور محاسبه دقیق تغذیه.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom safe spacer
            item {
                Spacer(modifier = Modifier.height(FarmDimens.Space32))
            }
        }
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    text = if (isImportMode) "بازیابی داده‌ها از کد متنی" else "کد متنی پشتیبان پایگاه داده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isImportMode)
                            "کد یا محتوای فایل JSON را در کادر زیر قرار دهید تا جزئیات آن قبل از اعمال نمایش داده شود:"
                        else
                            "کد پشتیبان را کپی کرده و در محلی امن یا یادداشت‌ها ذخیره کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        readOnly = !isImportMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(FarmDimens.InputRadius)
                    )
                }
            },
            confirmButton = {
                if (isImportMode) {
                    Button(
                        onClick = {
                            val summary = FullAppBackupManager.inspectBackup(backupJsonText)
                            if (summary != null) {
                                pendingRestoreSummary = summary
                                pendingRestoreJson = backupJsonText
                                showBackupDialog = false
                                showRestoreConfirmDialog = true
                            } else {
                                Toast.makeText(context, "❌ فرمت کد پشتیبان نامعتبر است", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = backupJsonText.isNotBlank(),
                        shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                    ) {
                        Text("بررسی و پیش‌نمایش بازیابی", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(backupJsonText))
                            Toast.makeText(context, "کد پشتیبان در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        },
                        shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                    ) {
                        Text("کپی کد", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBackupDialog = false },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("بستن")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // دیالوگ پیش‌نمایش و تأیید نهایی بازگردانی داده‌های ابری
    if (showRestoreConfirmDialog && pendingRestoreSummary != null) {
        val summary = pendingRestoreSummary!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreSummary = null
            },
            icon = {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "پیش‌نمایش و تأیید بازیابی داده‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "فایل پشتیبان با موفقیت شناسایی شد. خلاصه اطلاعات فایل به شرح زیر است:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📅 تاریخ پشتیبان:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(summary.backupDateJalali, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🔐 امنیت و حفظ حریم خصوصی:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (summary.isEncrypted) "رمزنگاری AES-256 (محافظت‌شده) ✅" else "بکاپ متنی قدیمی (فاقد رمز)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.isEncrypted) Color(0xFF047857) else Color(0xFFB45309)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🐣 گله‌های فعال:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${toPersianDigits(summary.flockCount)} گله (${toPersianDigits(summary.totalBirdsCount)} پرنده)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📦 اقلام انبار:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${toPersianDigits(summary.warehouseItemsCount)} قلم کالا", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("💰 تراکنش‌های مالی:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${toPersianDigits(summary.financialTxCount)} تراکنش", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥚 سوابق تخم‌گذاری:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${toPersianDigits(summary.eggRecordsCount)} رکورد", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🥗 برنامه‌های جیره:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${toPersianDigits(summary.rationPlansCount)} فرمول جیره", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🏆 آکادمی پرورش طلایی:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("⭐ ${toPersianDigits(summary.totalGameStars)} ستاره | ${toPersianDigits(summary.unlockedBirdsCount)} پرنده", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "⚠️ با تأیید، داده‌های فعلی برنامه جایگزین این فایل خواهند شد.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = onImportBackup(pendingRestoreJson)
                            if (success) {
                                Toast.makeText(context, "🎉 تمامی داده‌های فارم و مراحل بازی طلایی با موفقیت بازیابی شدند", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ خطا در بازگردانی داده‌ها", Toast.LENGTH_LONG).show()
                            }
                            showRestoreConfirmDialog = false
                            pendingRestoreSummary = null
                            pendingRestoreJson = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("تأیید و جایگزینی داده‌ها", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreSummary = null
                        pendingRestoreJson = ""
                    },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    if (showCustomBreedDialog) {
        com.example.poultryapp.ui.components.CustomBreedDialog(
            initialBreed = editingCustomBreed,
            onDismiss = {
                showCustomBreedDialog = false
                editingCustomBreed = null
            },
            onConfirm = { breed ->
                if (editingCustomBreed != null) {
                    onEditCustomBreed(breed)
                    Toast.makeText(context, "نژاد «${breed.name}» ویرایش شد", Toast.LENGTH_SHORT).show()
                } else {
                    onAddCustomBreed(breed)
                    Toast.makeText(context, "نژاد «${breed.name}» افزوده شد", Toast.LENGTH_SHORT).show()
                }
                showCustomBreedDialog = false
                editingCustomBreed = null
            }
        )
    }

    if (deletingBreedId != null) {
        AlertDialog(
            onDismissRequest = { deletingBreedId = null },
            title = { Text("حذف نژاد اختصاصی", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این نژاد اختصاصی اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomBreed(deletingBreedId!!)
                        deletingBreedId = null
                        Toast.makeText(context, "نژاد حذف شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف قطعی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingBreedId = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showCityDialog) {
        com.example.poultryapp.ui.components.CitySelectionDialog(
            initialProvince = settings.farmProvince,
            initialCity = settings.farmCity,
            onDismiss = { showCityDialog = false },
            onCitySelected = { prov, city ->
                onSetFarmLocation?.invoke(prov, city)
                showCityDialog = false
                Toast.makeText(context, "شهر $city با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
            },
            onGpsDetectClick = {
                onDetectGpsLocation?.invoke { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
                showCityDialog = false
            }
        )
    }

    if (showSetPinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text(
                    text = if (settings.managerPinHash.isNotBlank()) "تغییر پین‌کد مدیریت" else "تعیین پین‌کد مدیریت فارم",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "یک رمز عبور ۴ تا ۶ رقمی برای ورود به پنل مدیریت و خروج از حالت کارگر تعیین کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 6) pin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        },
                        label = { Text("پین‌کد جدید (۴ تا ۶ رقم)") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 6) confirmPin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        },
                        label = { Text("تکرار پین‌کد") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin.length < 4) {
                            errorMsg = "رمز پین باید حداقل ۴ رقم باشد"
                        } else if (pin != confirmPin) {
                            errorMsg = "پین‌کد و تکرار آن یکسان نیستند"
                        } else {
                            val hash = AppCryptoManager.hashPin(pin)
                            val recKey = if (settings.recoveryKey.isBlank()) AppCryptoManager.generateRecoveryKey() else settings.recoveryKey
                            newlyGeneratedRecoveryKey = recKey
                            onSaveSettings(
                                settings.copy(
                                    managerPinHash = hash,
                                    recoveryKey = recKey,
                                    isWorkerModeEnabled = true
                                )
                            )
                            isWorkerModeEnabled = true
                            showSetPinDialog = false
                            showRecoveryKeyDialog = true
                            Toast.makeText(context, "✅ رمز پین مدیریت با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ذخیره پین", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSetPinDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showRecoveryKeyDialog && newlyGeneratedRecoveryKey != null) {
        AlertDialog(
            onDismissRequest = { showRecoveryKeyDialog = false },
            icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("کلید بازیابی اضطراری ۶ رقمی", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "در صورتی که پین‌کد مدیریت را فراموش کنید، با این کلید ۶ رقمی می‌توانید قفل را باز کنید. لطفاً آن را یادداشت کرده یا کپی نمایید:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = newlyGeneratedRecoveryKey!!,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(newlyGeneratedRecoveryKey!!))
                        Toast.makeText(context, "کلید بازیابی کپی شد", Toast.LENGTH_SHORT).show()
                        showRecoveryKeyDialog = false
                    }
                ) {
                    Text("کپی و بستن")
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(FarmDimens.Space12))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

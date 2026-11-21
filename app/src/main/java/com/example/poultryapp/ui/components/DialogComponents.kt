package com.example.poultryapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.poultryapp.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.FlockSubGroup
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.BirdTransfer
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.ContactCategory
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.BreedCatalog
import com.example.poultryapp.data.model.ProductionType
import androidx.compose.material.icons.filled.DeleteOutline
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.FarmDimens

fun parsePersianOrEnglishInt(str: String): Int? {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (ch in str.trim()) {
        val pIdx = persianDigits.indexOf(ch)
        if (pIdx >= 0) {
            sb.append(pIdx)
            continue
        }
        val aIdx = arabicDigits.indexOf(ch)
        if (aIdx >= 0) {
            sb.append(aIdx)
            continue
        }
        if (ch in '0'..'9') {
            sb.append(ch)
        }
    }
    return sb.toString().toIntOrNull()
}

fun parsePersianOrEnglishDouble(str: String): Double? {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    var hasDot = false
    for (ch in str.trim()) {
        val pIdx = persianDigits.indexOf(ch)
        if (pIdx >= 0) {
            sb.append(pIdx)
            continue
        }
        val aIdx = arabicDigits.indexOf(ch)
        if (aIdx >= 0) {
            sb.append(aIdx)
            continue
        }
        if (ch in '0'..'9') {
            sb.append(ch)
        } else if ((ch == '.' || ch == '/' || ch == '٫') && !hasDot) {
            sb.append('.')
            hasDot = true
        }
    }
    return sb.toString().toDoubleOrNull()
}

fun String.toSafeDoubleOrNull(): Double? = parsePersianOrEnglishDouble(this)
fun String.toSafeIntOrNull(): Int? = parsePersianOrEnglishInt(this)

fun generateDefaultFlockName(breed: String, existingFlocks: List<Flock>): String {
    val cleanBreed = breed.trim()
    if (cleanBreed.isBlank()) return "گله جدید"
    val basePrefix = "گله $cleanBreed"

    val matchingFlocks = existingFlocks.filter { flock ->
        flock.breed.equals(cleanBreed, ignoreCase = true) ||
        flock.subGroups.any { it.breed.equals(cleanBreed, ignoreCase = true) } ||
        flock.name.trim().startsWith(basePrefix, ignoreCase = true)
    }

    if (matchingFlocks.isEmpty()) {
        return basePrefix
    }

    val regex = Regex("^${Regex.escape(basePrefix)}(?:\\s+([0-9۰-۹]+))?\$")
    var maxNum = 1

    for (flock in matchingFlocks) {
        val match = regex.find(flock.name.trim())
        if (match != null) {
            val numGroup = match.groupValues.getOrNull(1)
            if (!numGroup.isNullOrBlank()) {
                val parsed = parsePersianOrEnglishInt(numGroup)
                if (parsed != null && parsed > maxNum) {
                    maxNum = parsed
                }
            }
        }
    }

    val nextIndex = maxOf(maxNum + 1, matchingFlocks.size + 1)
    return "$basePrefix ${toPersianDigits(nextIndex)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlockFormDialog(
    flock: Flock? = null,
    existingFlocks: List<Flock> = emptyList(),
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        species: BirdSpecies,
        breed: String,
        ageDays: Int,
        birdCount: Int,
        notes: String?,
        subGroups: List<FlockSubGroup>,
        isFeedEnabled: Boolean,
        isWaterEnabled: Boolean,
        customTempCelsius: Double?
    ) -> Unit
) {
    // Multi-breed / multi-age subgroups state
    var subGroups by remember {
        mutableStateOf(
            if (flock != null && flock.subGroups.isNotEmpty()) {
                flock.subGroups
            } else if (flock != null) {
                listOf(
                    FlockSubGroup(
                        species = flock.species,
                        breed = flock.breed,
                        birdCount = flock.birdCount,
                        birthEpochMillis = flock.birthEpochMillis
                    )
                )
            } else {
                listOf(
                    FlockSubGroup(
                        species = BirdSpecies.CHICKEN,
                        breed = BreedCatalog.getBreedsForSpecies(BirdSpecies.CHICKEN, customBreeds).first().nameFa,
                        birdCount = 100,
                        birthEpochMillis = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
                    )
                )
            }
        )
    }

    val initialBreed = subGroups.firstOrNull()?.breed ?: BreedCatalog.getBreedsForSpecies(BirdSpecies.CHICKEN, customBreeds).first().nameFa
    var name by remember {
        mutableStateOf(flock?.name ?: generateDefaultFlockName(initialBreed, existingFlocks))
    }
    var isNameUserEdited by remember { mutableStateOf(flock != null) }

    var isFeedEnabled by remember { mutableStateOf(flock?.isFeedEnabled ?: true) }
    var isWaterEnabled by remember { mutableStateOf(flock?.isWaterEnabled ?: true) }
    var notes by remember { mutableStateOf(flock?.notes ?: "") }
    var customTempInput by remember {
        mutableStateOf(flock?.customTempCelsius?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (flock == null) "افزودن گله جدید" else "ویرایش گله",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameUserEdited = true
                    },
                    label = { Text(if (flock == null) "نام گله (پیشنهاد خودکار)" else "نام یا شناسه گله") },
                    trailingIcon = {
                        if (flock == null) {
                            IconButton(
                                onClick = {
                                    val currentBreed = subGroups.firstOrNull()?.breed ?: ""
                                    name = generateDefaultFlockName(currentBreed, existingFlocks)
                                    isNameUserEdited = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "بازنشانی به نام خودکار",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    supportingText = if (flock == null) {
                        {
                            Text(
                                text = if (isNameUserEdited) "نام دستی ویرایش شده است (برای نام هوشمند خودکار، دکمه نوسازی را بزنید)"
                                       else "نام پیش‌فرض به صورت خودکار بر اساس نژاد تنظیم شده و قابل ویرایش است",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Enable/Disable Feed & Water Tracking Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "تنظیمات پایش دان و آب:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("پایش و هشدار دان", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isFeedEnabled,
                                onCheckedChange = { isFeedEnabled = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("پایش و هشدار آب", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isWaterEnabled,
                                onCheckedChange = { isWaterEnabled = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subgroups Section (Breeds & Ages within flock)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ترکیب نژادها و سن‌های گله:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${toPersianDigits(subGroups.size)} دسته",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                subGroups.forEachIndexed { index, sg ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Subgroup Header & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دسته شماره ${toPersianDigits(index + 1)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (subGroups.size > 1) {
                                    IconButton(
                                        onClick = {
                                            subGroups = subGroups.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف دسته",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Species Selector Chips with Horizontal Scroll (Fixes awkward multi-line text wrapping!)
                            Text(
                                text = "گونه پرنده:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BirdSpecies.entries.forEach { sp ->
                                    val isSelected = sp == sg.species
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newBreed = BreedCatalog.getBreedsForSpecies(sp, customBreeds).first().nameFa
                                            subGroups = subGroups.toMutableList().also {
                                                it[index] = sg.copy(species = sp, breed = newBreed)
                                            }
                                            if (flock == null && index == 0 && (!isNameUserEdited || name.isBlank())) {
                                                name = generateDefaultFlockName(newBreed, existingFlocks)
                                            }
                                        },
                                        label = {
                                            Text(
                                                sp.titleFa,
                                                maxLines = 1,
                                                softWrap = false,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Breed Dropdown
                            var breedMenuExpanded by remember { mutableStateOf(false) }
                            Text(
                                text = "نژاد پرنده:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            ExposedDropdownMenuBox(
                                expanded = breedMenuExpanded,
                                onExpandedChange = { breedMenuExpanded = !breedMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = sg.breed,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = breedMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = breedMenuExpanded,
                                    onDismissRequest = { breedMenuExpanded = false }
                                ) {
                                    BreedCatalog.getBreedsForSpecies(sg.species, customBreeds).forEach { breedOption ->
                                        val isCustom = customBreeds.any { it.species == sg.species && it.name == breedOption.nameFa }
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(breedOption.nameFa, fontWeight = FontWeight.Bold)
                                                        if (isCustom) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                "(نژاد اختصاصی)",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        "تیپ: ${breedOption.productionType.titleFa}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                subGroups = subGroups.toMutableList().also {
                                                    it[index] = sg.copy(breed = breedOption.nameFa)
                                                }
                                                if (flock == null && index == 0 && (!isNameUserEdited || name.isBlank())) {
                                                    name = generateDefaultFlockName(breedOption.nameFa, existingFlocks)
                                                }
                                                breedMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Count & Age
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var countInput by remember(sg.birdCount) { mutableStateOf(sg.birdCount.toString()) }
                                var ageInput by remember(sg.ageDays) { mutableStateOf(sg.ageDays.toString()) }

                                OutlinedTextField(
                                    value = countInput,
                                    onValueChange = {
                                        countInput = it
                                        it.toIntOrNull()?.let { parsed ->
                                            if (parsed > 0) {
                                                subGroups = subGroups.toMutableList().also { list ->
                                                    list[index] = sg.copy(birdCount = parsed)
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("تعداد پرنده") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = ageInput,
                                    onValueChange = {
                                        ageInput = it
                                        it.toIntOrNull()?.let { parsed ->
                                            if (parsed > 0) {
                                                val newBirthEpoch = System.currentTimeMillis() - (parsed * 24L * 60 * 60 * 1000)
                                                subGroups = subGroups.toMutableList().also { list ->
                                                    list[index] = sg.copy(birthEpochMillis = newBirthEpoch)
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("سن (روز)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Add Subgroup Button
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        subGroups = subGroups + FlockSubGroup(
                            species = BirdSpecies.CHICKEN,
                            breed = BreedCatalog.getBreedsForSpecies(BirdSpecies.CHICKEN, customBreeds).first().nameFa,
                            birdCount = 50,
                            birthEpochMillis = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن نژاد یا سن دیگر به این سالن")
                }

                // Total summary banner
                val totalBirds = subGroups.sumOf { it.birdCount }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "مجموع کل پرندگان سالن: ${toPersianDigits(totalBirds)} قطعه (${toPersianDigits(subGroups.size)} نژاد/دسته)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Custom Temperature
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customTempInput,
                    onValueChange = { input ->
                        val cleaned = input.replace(',', '.')
                        if (cleaned.isEmpty() || cleaned == "-" || cleaned.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            customTempInput = cleaned
                        }
                    },
                    label = { Text("دمای اختصاصی سالن (°C) - اختیاری") },
                    placeholder = { Text("مثال: ۲۴ (در صورت خالی بودن، از دمای فارم استفاده می‌شود)") },
                    leadingIcon = {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت اختیاری (واکسن‌ها، تاریخ ورود...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val primarySpecies = subGroups.firstOrNull()?.species ?: BirdSpecies.CHICKEN
                            val primaryBreed = if (subGroups.size <= 1) {
                                subGroups.firstOrNull()?.breed ?: ""
                            } else {
                                "${subGroups.first().breed} (+${toPersianDigits(subGroups.size - 1)} نژاد دیگر)"
                            }
                            val primaryAge = subGroups.firstOrNull()?.ageDays ?: 1
                            val calculatedTotalBirds = subGroups.sumOf { it.birdCount }

                            if (name.isNotBlank()) {
                                onSave(
                                    name.trim(),
                                    primarySpecies,
                                    primaryBreed,
                                    primaryAge,
                                    calculatedTotalBirds,
                                    notes.ifBlank { null },
                                    subGroups,
                                    isFeedEnabled,
                                    isWaterEnabled,
                                    customTempInput.toSafeDoubleOrNull()
                                )
                            }
                        },
                        enabled = name.isNotBlank() && subGroups.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره گله")
                    }
                }
            }
        }
    }
}

/**
 * Sleek modal dialog to adjust or reset a flock's custom temperature.
 */
@Composable
fun FlockTemperatureDialog(
    flockName: String,
    currentCustomTemp: Double?,
    farmAmbientTemp: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var tempInput by remember {
        mutableStateOf(currentCustomTemp?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(FarmDimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FarmDimens.Space20)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Thermostat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(FarmDimens.Space10))
                        Column {
                            Text(
                                text = "تنظیم دمای سالن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = flockName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))
                Text(
                    text = "در صورتی که این سالن دارای دمای مجزا (مثلاً سالن جوجه، گرمایش یا مه‌پاش) است، دمای آن را مشخص کنید تا محاسبات آب و دان اختصاصاً برای این سالن دقیق شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (farmAmbientTemp != null) {
                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "دمای عمومی فعلی فارم: ${toPersianDigits(farmAmbientTemp)}°C",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space14))

                // Presets
                Text(
                    text = "دماهای متداول:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(FarmDimens.Space6))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Pair("۳۳°C جوجه", 33.0),
                        Pair("۲۸°C رشد", 28.0),
                        Pair("۲۴°C استاندارد", 24.0),
                        Pair("۲۰°C خنک", 20.0)
                    )
                    presets.forEach { (label, value) ->
                        val formattedVal = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
                        FilterChip(
                            selected = tempInput == formattedVal,
                            onClick = { tempInput = formattedVal },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { input ->
                        val cleaned = input.replace(',', '.')
                        if (cleaned.isEmpty() || cleaned == "-" || cleaned.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            tempInput = cleaned
                        }
                    },
                    label = { Text("دمای سالن (°C)") },
                    placeholder = { Text("مثال: ۲۴.۵") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space16))

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val parsed = tempInput.toSafeDoubleOrNull()
                            onConfirm(parsed)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = tempInput.toSafeDoubleOrNull() != null
                    ) {
                        Text("ثبت و اعمال دمای سالن")
                    }

                    if (currentCustomTemp != null) {
                        OutlinedButton(
                            onClick = { onConfirm(null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("استفاده از دمای عمومی فارم")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انصراف")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseItemDialog(
    item: WarehouseItem? = null,
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onSave: (WarehouseItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(item?.category ?: WarehouseCategory.FEED) }
    var stockText by remember {
        mutableStateOf(
            item?.currentStock?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "100"
        )
    }
    var unit by remember { mutableStateOf(item?.unit ?: "کیلوگرم") }
    var isMinAlertEnabled by remember {
        mutableStateOf(item?.let { it.minAlertThreshold > 0.0 } ?: false)
    }
    var minThresholdText by remember {
        mutableStateOf(
            item?.minAlertThreshold?.let { if (it > 0.0) { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } else "30" } ?: "30"
        )
    }
    var pricePerUnitText by remember {
        mutableStateOf(
            item?.pricePerUnit?.let { if (it > 0) { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } else "" } ?: ""
        )
    }
    var selectedFlockIds by remember {
        mutableStateOf(item?.assignedFlockIds?.toSet() ?: flocks.map { it.id }.toSet())
    }
    var isForSale by remember {
        mutableStateOf(item?.isForSale ?: (item?.category == WarehouseCategory.EGG))
    }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item == null) "افزودن کالا به انبار" else "ویرایش موجودی انبار",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کالا یا خوراک (مثال: دان پلت استارتر)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category dropdown
                Text(
                    text = "دسته‌بندی کالا:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.titleFa,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        WarehouseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.titleFa) },
                                onClick = {
                                    selectedCategory = cat
                                    if (cat == WarehouseCategory.EGG) {
                                        if (unit == "کیلوگرم" || unit.isBlank()) unit = "عدد"
                                        if (name.isBlank()) name = "تخم‌مرغ خوراکی"
                                        if (pricePerUnitText.isBlank()) pricePerUnitText = "27000"
                                        isForSale = true
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stock & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("موجودی فعلی") },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("واحد") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Min Alert Threshold Toggle (Default OFF for new items)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMinAlertEnabled = !isMinAlertEnabled }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "هشدار کمبود موجودی",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMinAlertEnabled) "در صورت رسیدن به حداقل، هشدار داده می‌شود" else "غیرفعال (بدون هشدار کمبود)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isMinAlertEnabled,
                            onCheckedChange = { isMinAlertEnabled = it }
                        )
                    }
                }

                if (isMinAlertEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = minThresholdText,
                        onValueChange = { minThresholdText = it },
                        label = { Text("حداقل موجودی برای هشدار کمبود (${unit.ifBlank { "واحد" }})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price per unit (Tomans)
                OutlinedTextField(
                    value = pricePerUnitText,
                    onValueChange = { pricePerUnitText = it },
                    label = { Text("قیمت هر ${unit.ifBlank { "کیلوگرم" }} (تومان)") },
                    placeholder = { Text("مثال: ۲۷۰۰۰") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Live Total Inventory Value Card
                val liveStock = stockText.toSafeDoubleOrNull() ?: 0.0
                val livePrice = pricePerUnitText.toSafeDoubleOrNull() ?: 0.0
                val liveTotalValue = liveStock * livePrice
                if (liveStock > 0.0 && livePrice > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ارزش کل موجودی این قلم:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.example.poultryapp.engine.DailyNutritionEngine.formatTomans(liveTotalValue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Is For Sale Toggle Switch
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isForSale = !isForSale }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "کالای فروشی (جهت فروش و کسب سود)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isForSale) "با کسر یا اتمام این کالا، مبلغ فروش به عنوان سود در بخش مالی ثبت خواهد شد" else "کالای مصرفی فارم (بدون ثبت فروش مالی هنگام کسر)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isForSale,
                            onCheckedChange = { isForSale = it }
                        )
                    }
                }

                // Assigned Flocks (especially relevant for feed)
                if (flocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "گله‌های مصرف‌کننده این دان/کالا:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        flocks.forEach { flock ->
                            val isChecked = flock.id in selectedFlockIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFlockIds = if (isChecked) {
                                            selectedFlockIds - flock.id
                                        } else {
                                            selectedFlockIds + flock.id
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedFlockIds = if (checked) {
                                            selectedFlockIds + flock.id
                                        } else {
                                            selectedFlockIds - flock.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${flock.name} (${toPersianDigits(flock.effectiveBirdCount)} پرنده)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت اختیاری (تأمین‌کننده، قیمت، تاریخ انقضا...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedStock = stockText.toSafeDoubleOrNull() ?: 0.0
                            val parsedThreshold = if (isMinAlertEnabled) (minThresholdText.toSafeDoubleOrNull() ?: 30.0) else 0.0
                            val parsedPrice = pricePerUnitText.toSafeDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onSave(
                                    WarehouseItem(
                                        id = item?.id ?: 0L,
                                        name = name.trim(),
                                        category = selectedCategory,
                                        currentStock = parsedStock,
                                        unit = unit.trim().ifBlank { "کیلوگرم" },
                                        assignedFlockIds = selectedFlockIds.toList(),
                                        pricePerUnit = parsedPrice,
                                        minAlertThreshold = parsedThreshold,
                                        isForSale = isForSale,
                                        notes = notes.trim().ifBlank { null },
                                        updatedAtMillis = System.currentTimeMillis()
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره کالا")
                    }
                }
            }
        }
    }
}

@Composable
fun ChargeInputDialog(
    itemType: ChargeItemType,
    flockName: String,
    suggestedDailyAmount: Double,
    currentRemainingAmount: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, discardLeftover: Boolean) -> Unit,
    onDiscardLeftoverOnly: (() -> Unit)? = null
) {
    val unitStr = if (itemType == ChargeItemType.FEED) "گرم" else "میلی‌لیتر"
    var amountText by remember { mutableStateOf(suggestedDailyAmount.toInt().toString()) }
    var discardLeftover by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "ثبت شارژ ${itemType.titleFa}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "گله: $flockName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "نیاز روزانه محاسبه‌شده: ${toPersianDigits(suggestedDailyAmount.toInt())} $unitStr",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (currentRemainingAmount > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val remText = if (itemType == ChargeItemType.FEED) DailyNutritionEngine.formatFeed(currentRemainingAmount)
                                      else DailyNutritionEngine.formatWater(currentRemainingAmount)
                        Text(
                            text = "مانده فعلی در منبع: $remText",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checkbox for discarding leftover water/feed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { discardLeftover = !discardLeftover }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = discardLeftover,
                            onCheckedChange = { discardLeftover = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "تخلیه باقیمانده قبلی قبل از شارژ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "دور ریختن آب/دان کهنه جهت پیشگیری از آلودگی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مقدار شارژ شده ($unitStr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                ) {
                    Button(
                        onClick = {
                            val parsed = amountText.toSafeDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                onConfirm(parsed, discardLeftover)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = "تأیید و ثبت شارژ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (onDiscardLeftoverOnly != null) Arrangement.SpaceBetween else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDiscardLeftoverOnly != null) {
                            TextButton(
                                onClick = onDiscardLeftoverOnly,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فقط تخلیه منبع", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text("انصراف")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyConfirmDialog(
    itemType: ChargeItemType,
    flockName: String,
    remainingAmountFa: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                Text(
                    text = "تخلیه و دور ریختن ${itemType.titleFa}",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "آیا مایلید باقیمانده ${itemType.titleFa} گله «$flockName» (حدود $remainingAmountFa) را تخلیه و دور بریزید؟",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(FarmDimens.Space8))
                Text(
                    text = "با این کار باقیمانده منبع صفر می‌شود و پس از شستشو آماده شارژ تازه و بهداشتی خواهد بود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(FarmDimens.RadiusMedium)
            ) {
                Text("تأیید تخلیه کامل", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(FarmDimens.RadiusMedium)
            ) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun ScheduleFormDialog(
    initialSchedule: CalendarSchedule? = null,
    initialMode: String = "WEEKLY",
    initialDayOfMonth: Int = 1,
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onConfirm: (schedule: CalendarSchedule) -> Unit
) {
    val types = listOf("ویتامین", "واکسن", "دارو", "ضدعفونی", "مکمل", "سایر")
    val dayNames = listOf("ش", "ی", "د", "س", "چ", "پ", "ج") // شنبه تا جمعه

    var scheduleMode by remember {
        mutableStateOf(initialSchedule?.scheduleMode ?: initialMode)
    }
    var selectedDayOfMonth by remember {
        mutableStateOf(initialSchedule?.dayOfMonth ?: initialDayOfMonth)
    }
    var selectedType by remember { mutableStateOf(initialSchedule?.scheduleType ?: types.first()) }
    var title by remember { mutableStateOf(initialSchedule?.title ?: "") }
    var notes by remember { mutableStateOf(initialSchedule?.notes ?: "") }
    var selectedFlockId by remember { mutableStateOf(initialSchedule?.flockId) }
    var selectedDays by remember { mutableStateOf(initialSchedule?.daysOfWeek?.toSet() ?: setOf(0, 1, 2, 3, 4, 5, 6)) }
    var hourText by remember { mutableStateOf(initialSchedule?.hour?.toString() ?: "8") }
    var minuteText by remember { mutableStateOf(initialSchedule?.minute?.toString() ?: "0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialSchedule == null) {
                        if (scheduleMode == "MONTHLY") "افزودن برنامه ماهانه" else "افزودن برنامه هفتگی"
                    } else "ویرایش برنامه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector: Weekly vs Monthly
                Text("حالت برنامه‌ریزی:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scheduleMode == "WEEKLY",
                        onClick = { scheduleMode = "WEEKLY" },
                        label = { Text("برنامه‌ریزی هفتگی", fontWeight = if (scheduleMode == "WEEKLY") FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scheduleMode == "MONTHLY",
                        onClick = { scheduleMode = "MONTHLY" },
                        label = { Text("برنامه‌ریزی ماهانه", fontWeight = if (scheduleMode == "MONTHLY") FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule Type Chips
                Text("نوع برنامه:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.take(3).forEach { t ->
                        FilterChip(
                            selected = t == selectedType,
                            onClick = { selectedType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.drop(3).forEach { t ->
                        FilterChip(
                            selected = t == selectedType,
                            onClick = { selectedType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نام / شرح برنامه (مثال: ب-کمپلکس در آبخوری)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (scheduleMode == "WEEKLY") {
                    // Days of week selector
                    Text("روزهای تکرار در هفته (شنبه تا جمعه):", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayNames.forEachIndexed { index, name ->
                            val isDaySelected = selectedDays.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDaySelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedDays = if (isDaySelected) {
                                            if (selectedDays.size > 1) selectedDays - index else selectedDays
                                        } else {
                                            selectedDays + index
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isDaySelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                } else {
                    // Day of Month selector (1 to 31)
                    Text(
                        text = "روز تکرار در هر ماه: روز ${toPersianDigits(selectedDayOfMonth)} ماه",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..31).forEach { d ->
                            val isSelected = selectedDayOfMonth == d
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedDayOfMonth = d },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = toPersianDigits(d),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Flock selection
                if (flocks.isNotEmpty()) {
                    Text("گله مربوطه (اختیاری):", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFlockId == null,
                            onClick = { selectedFlockId = null },
                            label = { Text("تمام گله‌ها", style = MaterialTheme.typography.labelSmall) }
                        )
                        flocks.forEach { flock ->
                            FilterChip(
                                selected = selectedFlockId == flock.id,
                                onClick = { selectedFlockId = flock.id },
                                label = { Text(flock.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("ساعت (۰-۲۳)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it },
                        label = { Text("دقیقه (۰-۵۹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت / دوز مصرفی") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val h = (hourText.toIntOrNull() ?: 8).coerceIn(0, 23)
                            val m = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                            val flockObj = flocks.find { it.id == selectedFlockId }
                            val newSchedule = CalendarSchedule(
                                id = initialSchedule?.id ?: 0L,
                                flockId = selectedFlockId,
                                flockName = flockObj?.name,
                                scheduleType = selectedType,
                                title = title.trim(),
                                notes = notes.ifBlank { null },
                                daysOfWeek = if (scheduleMode == "WEEKLY") selectedDays.toList().sorted() else emptyList(),
                                hour = h,
                                minute = m,
                                isEnabled = initialSchedule?.isEnabled ?: true,
                                scheduleMode = scheduleMode,
                                dayOfMonth = if (scheduleMode == "MONTHLY") selectedDayOfMonth else null
                            )
                            onConfirm(newSchedule)
                        },
                        enabled = title.isNotBlank() && (scheduleMode == "MONTHLY" || selectedDays.isNotEmpty()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره برنامه")
                    }
                }
            }
        }
    }
}

@Composable
fun ChargeDialog(
    itemType: ChargeItemType,
    flockName: String,
    dailyRequirementTotal: Double,
    currentRemainingAmount: Double = 0.0,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, discardLeftover: Boolean) -> Unit,
    onDiscardLeftoverOnly: (() -> Unit)? = null
) {
    ChargeInputDialog(
        itemType = itemType,
        flockName = flockName,
        suggestedDailyAmount = dailyRequirementTotal,
        currentRemainingAmount = currentRemainingAmount,
        onDismiss = onDismiss,
        onConfirm = onSubmit,
        onDiscardLeftoverOnly = onDiscardLeftoverOnly
    )
}

data class EditableWeeklyRate(
    val week: Int,
    val feedText: String,
    val waterText: String
)

data class EditableDailyRate(
    val day: Int,
    val feedText: String,
    val waterText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBreedDialog(
    initialBreed: com.example.poultryapp.data.local.CustomBreed? = null,
    onDismiss: () -> Unit,
    onConfirm: (com.example.poultryapp.data.local.CustomBreed) -> Unit
) {
    var name by remember { mutableStateOf(initialBreed?.name ?: "") }
    var species by remember { mutableStateOf(initialBreed?.species ?: BirdSpecies.CHICKEN) }
    var productionType by remember { mutableStateOf(initialBreed?.productionType ?: ProductionType.LAYER) }
    var adultFeedText by remember {
        mutableStateOf(
            if (initialBreed != null) initialBreed.adultFeedGrams.toString().removeSuffix(".0")
            else com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultFeedGrams(BirdSpecies.CHICKEN, ProductionType.LAYER).toString().removeSuffix(".0")
        )
    }
    var adultWaterText by remember {
        mutableStateOf(
            if (initialBreed != null) initialBreed.adultWaterMl.toString().removeSuffix(".0")
            else com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultWaterMl(BirdSpecies.CHICKEN, ProductionType.LAYER).toString().removeSuffix(".0")
        )
    }
    var notes by remember { mutableStateOf(initialBreed?.notes ?: "") }

    var rateInputMode by remember {
        mutableStateOf(initialBreed?.rateInputMode ?: "WEEKS")
    }

    var weeklyRates by remember {
        mutableStateOf(
            if (initialBreed != null && initialBreed.weeklyRates.isNotEmpty()) {
                initialBreed.weeklyRates.map {
                    EditableWeeklyRate(
                        week = it.week,
                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                    )
                }
            } else {
                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardWeeklyRates(species, productionType, 10)
                defaults.map {
                    EditableWeeklyRate(
                        week = it.week,
                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                    )
                }
            }
        )
    }

    var dailyRates by remember {
        mutableStateOf(
            if (initialBreed != null && initialBreed.dailyRates.isNotEmpty()) {
                initialBreed.dailyRates.map {
                    EditableDailyRate(
                        day = it.day,
                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                    )
                }
            } else if (initialBreed != null && initialBreed.weeklyRates.isNotEmpty()) {
                val converted = com.example.poultryapp.engine.DailyNutritionEngine.convertWeeklyToDailyRates(
                    initialBreed.weeklyRates,
                    initialBreed.adultFeedGrams,
                    initialBreed.adultWaterMl,
                    42
                )
                converted.map {
                    EditableDailyRate(
                        day = it.day,
                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                    )
                }
            } else {
                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardDailyRates(species, productionType, 42)
                defaults.map {
                    EditableDailyRate(
                        day = it.day,
                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                    )
                }
            }
        )
    }

    // Growth phase filter for days mode (to keep UI clean and uncluttered)
    var selectedDailyPhase by remember { mutableStateOf(0) } // 0: Starter (1-14), 1: Grower (15-42), 2: Finisher (43+), 3: All

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialBreed == null) "تعریف نژاد اختصاصی" else "ویرایش نژاد اختصاصی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تنظیم دقیق مصرف روزانه آب و دان",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام نژاد جدید *") },
                    placeholder = { Text("مثال: بلک استار اصلاح شده، لوهمن...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Species Selection
                Text(
                    text = "گونه پرنده:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BirdSpecies.entries.forEach { sp ->
                        val isSelected = sp == species
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                species = sp
                                adultFeedText = com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultFeedGrams(sp, productionType).toString().removeSuffix(".0")
                                adultWaterText = com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultWaterMl(sp, productionType).toString().removeSuffix(".0")
                                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardWeeklyRates(sp, productionType, 10)
                                weeklyRates = defaults.map {
                                    EditableWeeklyRate(
                                        week = it.week,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                                val dailyDefaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardDailyRates(sp, productionType, 42)
                                dailyRates = dailyDefaults.map {
                                    EditableDailyRate(
                                        day = it.day,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                            },
                            label = { Text(sp.titleFa, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Production Type
                Text(
                    text = "تیپ تولید:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductionType.entries.forEach { pt ->
                        val isSelected = pt == productionType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                productionType = pt
                                adultFeedText = com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultFeedGrams(species, pt).toString().removeSuffix(".0")
                                adultWaterText = com.example.poultryapp.engine.DailyNutritionEngine.getDefaultAdultWaterMl(species, pt).toString().removeSuffix(".0")
                                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardWeeklyRates(species, pt, 10)
                                weeklyRates = defaults.map {
                                    EditableWeeklyRate(
                                        week = it.week,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                                val dailyDefaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardDailyRates(species, pt, 42)
                                dailyRates = dailyDefaults.map {
                                    EditableDailyRate(
                                        day = it.day,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                            },
                            label = { Text(pt.titleFa, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adult Rates Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "نرخ مصرف روزانه در سن بلوغ (هر قطعه پرنده)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = adultFeedText,
                                onValueChange = { adultFeedText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("دان بالغ (گرم/روز)") },
                                suffix = { Text("گرم", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = adultWaterText,
                                onValueChange = { adultWaterText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("آب بالغ (ml/روز)") },
                                suffix = { Text("میلی‌لیتر", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Growth Rates Section Header
                Text(
                    text = "جدول مصرف در طول دوران رشد",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "شیوه ورود اطلاعات مصرف را بر اساس روز یا هفته انتخاب کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Switcher: Weeks vs Days
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isWeeks = rateInputMode == "WEEKS"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isWeeks) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { rateInputMode = "WEEKS" }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "بر اساس هفته‌های عمر",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isWeeks) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isWeeks) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isDays = rateInputMode == "DAYS"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDays) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { rateInputMode = "DAYS" }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "بر اساس روزهای عمر (دقیق)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isDays) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDays) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (rateInputMode == "WEEKS") {
                    // WEEKLY MODE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardWeeklyRates(species, productionType, 10)
                                weeklyRates = defaults.map {
                                    EditableWeeklyRate(
                                        week = it.week,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پیش‌نویس هفتگی", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                val nextWeek = (weeklyRates.maxOfOrNull { it.week } ?: 0) + 1
                                val prevRate = weeklyRates.lastOrNull()
                                weeklyRates = weeklyRates + EditableWeeklyRate(
                                    week = nextWeek,
                                    feedText = prevRate?.feedText ?: "30",
                                    waterText = prevRate?.waterText ?: "60"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن هفته", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekly rate list cards
                    weeklyRates.forEachIndexed { index, item ->
                        val startDay = (item.week - 1) * 7 + 1
                        val endDay = item.week * 7
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = toPersianDigits(item.week),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "هفته ${toPersianDigits(item.week)} (روز ${toPersianDigits(startDay)} تا ${toPersianDigits(endDay)})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (weeklyRates.size > 1) {
                                        IconButton(
                                            onClick = {
                                                weeklyRates = weeklyRates.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف هفته",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.feedText,
                                        onValueChange = { newVal ->
                                            weeklyRates = weeklyRates.toMutableList().also {
                                                it[index] = it[index].copy(feedText = newVal.filter { ch -> ch.isDigit() || ch == '.' })
                                            }
                                        },
                                        label = { Text("دان روزانه", style = MaterialTheme.typography.labelSmall) },
                                        suffix = { Text("گرم", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = item.waterText,
                                        onValueChange = { newVal ->
                                            weeklyRates = weeklyRates.toMutableList().also {
                                                it[index] = it[index].copy(waterText = newVal.filter { ch -> ch.isDigit() || ch == '.' })
                                            }
                                        },
                                        label = { Text("آب روزانه", style = MaterialTheme.typography.labelSmall) },
                                        suffix = { Text("میلی‌لیتر", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // DAYS MODE (Clean & Uncluttered with growth phase filter chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val phases = listOf(
                            "آغازین (۱ تا ۱۴)" to 0,
                            "رشد (۱۵ تا ۴۲)" to 1,
                            "پایانی (۴۳+)" to 2,
                            "همه روزها" to 3
                        )
                        phases.forEach { (title, phaseId) ->
                            FilterChip(
                                selected = selectedDailyPhase == phaseId,
                                onClick = { selectedDailyPhase = phaseId },
                                label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val defaults = com.example.poultryapp.engine.DailyNutritionEngine.generateStandardDailyRates(species, productionType, 42)
                                dailyRates = defaults.map {
                                    EditableDailyRate(
                                        day = it.day,
                                        feedText = it.dailyFeedGrams.toString().removeSuffix(".0"),
                                        waterText = it.dailyWaterMl.toString().removeSuffix(".0")
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("پیش‌نویس روزها", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                if (dailyRates.size >= 2) {
                                    val sorted = dailyRates.sortedBy { it.day }
                                    val minDay = sorted.first().day
                                    val maxDay = sorted.last().day
                                    val interpolated = (minDay..maxDay).map { d ->
                                        val exact = sorted.find { it.day == d }
                                        if (exact != null) exact
                                        else {
                                            val lower = sorted.filter { it.day < d }.maxBy { it.day }
                                            val upper = sorted.filter { it.day > d }.minBy { it.day }
                                            val lF = lower.feedText.toDoubleOrNull() ?: 0.0
                                            val uF = upper.feedText.toDoubleOrNull() ?: lF
                                            val lW = lower.waterText.toDoubleOrNull() ?: 0.0
                                            val uW = upper.waterText.toDoubleOrNull() ?: lW
                                            val ratio = (d - lower.day).toDouble() / (upper.day - lower.day).toDouble()
                                            val f = Math.round((lF + ratio * (uF - lF)) * 10.0) / 10.0
                                            val w = Math.round((lW + ratio * (uW - lW)) * 10.0) / 10.0
                                            EditableDailyRate(d, f.toString().removeSuffix(".0"), w.toString().removeSuffix(".0"))
                                        }
                                    }
                                    dailyRates = interpolated
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("محاسبه میانیاب", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                val nextDay = (dailyRates.maxOfOrNull { it.day } ?: 0) + 1
                                val prevRate = dailyRates.lastOrNull()
                                dailyRates = dailyRates + EditableDailyRate(
                                    day = nextDay,
                                    feedText = prevRate?.feedText ?: "25",
                                    waterText = prevRate?.waterText ?: "50"
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("افزودن روز", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredDailyRates = when (selectedDailyPhase) {
                        0 -> dailyRates.filter { it.day in 1..14 }
                        1 -> dailyRates.filter { it.day in 15..42 }
                        2 -> dailyRates.filter { it.day >= 43 }
                        else -> dailyRates
                    }

                    if (filteredDailyRates.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "هیچ روزی در این فاز ثبت نشده است. دکمه «افزودن روز» یا «پیش‌نویس روزها» را بزنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        filteredDailyRates.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Compact day badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.width(58.dp).height(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "روز ${toPersianDigits(item.day)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Compact feed field
                                    OutlinedTextField(
                                        value = item.feedText,
                                        onValueChange = { newVal ->
                                            val clean = newVal.filter { ch -> ch.isDigit() || ch == '.' }
                                            dailyRates = dailyRates.map { if (it.day == item.day) it.copy(feedText = clean) else it }
                                        },
                                        label = { Text("دان (گرم)", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    // Compact water field
                                    OutlinedTextField(
                                        value = item.waterText,
                                        onValueChange = { newVal ->
                                            val clean = newVal.filter { ch -> ch.isDigit() || ch == '.' }
                                            dailyRates = dailyRates.map { if (it.day == item.day) it.copy(waterText = clean) else it }
                                        },
                                        label = { Text("آب (ml)", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    // Delete action
                                    if (dailyRates.size > 1) {
                                        IconButton(
                                            onClick = {
                                                dailyRates = dailyRates.filter { it.day != item.day }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف روز",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت و مشخصات نژاد (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }

                    val adultFeedVal = adultFeedText.toDoubleOrNull() ?: 0.0
                    val adultWaterVal = adultWaterText.toDoubleOrNull() ?: 0.0
                    val isFormValid = name.isNotBlank() && adultFeedVal > 0.0 && adultWaterVal > 0.0

                    Button(
                        onClick = {
                            val convertedDaily: List<com.example.poultryapp.data.local.DailyConsumptionRate>
                            val convertedWeekly: List<com.example.poultryapp.data.local.WeeklyConsumptionRate>

                            if (rateInputMode == "DAYS") {
                                convertedDaily = dailyRates.map {
                                    com.example.poultryapp.data.local.DailyConsumptionRate(
                                        day = it.day,
                                        dailyFeedGrams = it.feedText.toDoubleOrNull() ?: adultFeedVal,
                                        dailyWaterMl = it.waterText.toDoubleOrNull() ?: adultWaterVal
                                    )
                                }.sortedBy { it.day }

                                convertedWeekly = com.example.poultryapp.engine.DailyNutritionEngine.convertDailyToWeeklyRates(
                                    convertedDaily,
                                    adultFeedVal,
                                    adultWaterVal
                                )
                            } else {
                                convertedWeekly = weeklyRates.map {
                                    com.example.poultryapp.data.local.WeeklyConsumptionRate(
                                        week = it.week,
                                        dailyFeedGrams = it.feedText.toDoubleOrNull() ?: adultFeedVal,
                                        dailyWaterMl = it.waterText.toDoubleOrNull() ?: adultWaterVal
                                    )
                                }.sortedBy { it.week }

                                convertedDaily = com.example.poultryapp.engine.DailyNutritionEngine.convertWeeklyToDailyRates(
                                    convertedWeekly,
                                    adultFeedVal,
                                    adultWaterVal,
                                    maxDays = 56
                                )
                            }

                            val breed = com.example.poultryapp.data.local.CustomBreed(
                                id = initialBreed?.id ?: 0L,
                                name = name.trim(),
                                species = species,
                                productionType = productionType,
                                adultFeedGrams = adultFeedVal,
                                adultWaterMl = adultWaterVal,
                                weeklyRates = convertedWeekly,
                                dailyRates = convertedDaily,
                                rateInputMode = rateInputMode,
                                notes = notes.ifBlank { null }
                            )
                            onConfirm(breed)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره نژاد")
                    }
                }
            }
        }
    }
}

// ==================== DAILY EGG RECORD DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEggDialog(
    flocks: List<Flock>,
    initialFlockId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (flockIds: List<Long>, healthy: Int, broken: Int, dirty: Int, doubleYolk: Int, dateJalaliStr: String, notes: String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onSaveDetailed: ((flockIds: List<Long>, fertile: Int, tableEgg: Int, broken: Int, dirty: Int, doubleYolk: Int, trayCount: Double, trayGrossWeightGrams: Double?, netEggAverageWeightGrams: Double?, dateJalaliStr: String, notes: String?) -> Unit)? = null
) {
    var selectedFlockIds by remember {
        mutableStateOf(
            if (initialFlockId != null) setOf(initialFlockId)
            else if (flocks.isNotEmpty()) flocks.map { it.id }.toSet()
            else emptySet()
        )
    }
    var fertileText by remember { mutableStateOf("") }
    var tableEggText by remember { mutableStateOf("") }
    var brokenText by remember { mutableStateOf("") }
    var dirtyText by remember { mutableStateOf("") }
    var doubleYolkText by remember { mutableStateOf("") }
    var trayGrossWeightText by remember { mutableStateOf("") }
    var customTrayCountText by remember { mutableStateOf("") }
    var showScaleSection by remember { mutableStateOf(false) }

    var dateText by remember {
        mutableStateOf(com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString())
    }
    var notes by remember { mutableStateOf("") }

    val selectedFlocks = flocks.filter { it.id in selectedFlockIds }
    val totalBirds = selectedFlocks.sumOf { it.effectiveBirdCount }

    val fertile = fertileText.toSafeIntOrNull() ?: 0
    val tableEgg = tableEggText.toSafeIntOrNull() ?: 0
    val healthy = fertile + tableEgg
    val broken = brokenText.toSafeIntOrNull() ?: 0
    val dirty = dirtyText.toSafeIntOrNull() ?: 0
    val doubleYolk = doubleYolkText.toSafeIntOrNull() ?: 0
    val totalEggs = healthy + broken + dirty + doubleYolk

    val autoTrays = if (totalEggs > 0) totalEggs.toDouble() / 30.0 else 0.0
    val trayCount = customTrayCountText.toSafeDoubleOrNull() ?: autoTrays
    val trayGrossGrams = trayGrossWeightText.toSafeDoubleOrNull()
    // Standard 30-egg empty paper tray is 50 grams
    val netAverageEggWeightGrams = if (trayGrossGrams != null && trayGrossGrams > 50.0) {
        (trayGrossGrams - 50.0) / 30.0
    } else null

    val layingRate = if (totalBirds > 0) (totalEggs.toDouble() / totalBirds.toDouble()) * 100.0 else 0.0
    val fertilityRate = if (healthy > 0) (fertile.toDouble() / healthy.toDouble()) * 100.0 else 0.0
    val totalEggMonetaryValue = (fertile * 55000.0) + (tableEgg * 27000.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_farm_egg),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت تخم‌گذاری تفکیکی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تفکیک نطفه‌دار و خوراکی + سنجش وزن شانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Flock Selection Header & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب گله‌ها (${com.example.poultryapp.engine.toPersianDigits(selectedFlockIds.size)} از ${com.example.poultryapp.engine.toPersianDigits(flocks.size)}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFlockIds.size == flocks.size && flocks.isNotEmpty(),
                            onClick = { selectedFlockIds = flocks.map { it.id }.toSet() },
                            label = { Text("همه", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = selectedFlockIds.isEmpty(),
                            onClick = { selectedFlockIds = emptySet() },
                            label = { Text("پاک‌کردن", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable box of selectable flocks
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        if (flocks.isEmpty()) {
                            Text(
                                text = "گله‌ای ثبت نشده است",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            flocks.forEach { flock ->
                                val isChecked = flock.id in selectedFlockIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedFlockIds = if (isChecked) selectedFlockIds - flock.id else selectedFlockIds + flock.id
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedFlockIds = if (checked) selectedFlockIds + flock.id else selectedFlockIds - flock.id
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = flock.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${com.example.poultryapp.engine.toPersianDigits(flock.effectiveBirdCount)} پرنده (${flock.breed})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (flock.isWithdrawalActive) {
                                            Text(
                                                text = "⛔ منع مصرف دارویی فعال (${flock.activeMedicationName ?: "دارو"})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = com.example.poultryapp.theme.HealthRedText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedFlocks.any { it.isWithdrawalActive }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = com.example.poultryapp.theme.HealthRedContainer,
                        border = BorderStroke(1.dp, com.example.poultryapp.theme.HealthRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⛔", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "هشدار پرهیز دارویی: حداقل یکی از گله‌های انتخاب‌شده در دوره منع مصرف دارو است. تخم‌مرغ‌های این دوره نباید برای مصرف خوراکی انسان توزیع شوند.",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.poultryapp.theme.HealthRedText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category 1: Healthy Eggs (Fertile & Table Eggs)
                Text(
                    text = "تخم‌مرغ‌های سالم درجه یک:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fertileText,
                        onValueChange = { fertileText = it.filter { c -> c.isDigit() } },
                        label = { Text("🥚 نطفه‌دار (جوجه‌کشی)") },
                        placeholder = { Text("مثلاً: ۴۵۰") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tableEggText,
                        onValueChange = { tableEggText = it.filter { c -> c.isDigit() } },
                        label = { Text("🍳 خوراکی درجه یک") },
                        placeholder = { Text("مثلاً: ۱۵۰") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category 2: Defective & Double Yolk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = brokenText,
                        onValueChange = { brokenText = it.filter { c -> c.isDigit() } },
                        label = { Text("شکسته / ترک‌دار") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dirtyText,
                        onValueChange = { dirtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("کثیف / نوک‌زده") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = doubleYolkText,
                    onValueChange = { doubleYolkText = it.filter { c -> c.isDigit() } },
                    label = { Text("دوزرده (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Digital Scale / Tray Weighing Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showScaleSection = !showScaleSection }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚖️ سنجش وزن شانه و دانه (ترازو)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (showScaleSection) "▲ بستن" else "▼ باز کردن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (showScaleSection) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = customTrayCountText,
                                    onValueChange = { customTrayCountText = it },
                                    label = { Text("تعداد شانه") },
                                    placeholder = { Text("${com.example.poultryapp.engine.toPersianDigits(String.format(java.util.Locale.US, "%.1f", autoTrays))}") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = trayGrossWeightText,
                                    onValueChange = { trayGrossWeightText = it },
                                    label = { Text("وزن ناخالص شانه (گرم)") },
                                    placeholder = { Text("مثلاً ۱۹۵۰") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            if (netAverageEggWeightGrams != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val gradeText = when {
                                    netAverageEggWeightGrams >= 63.0 -> "درشت (Jumbo/Large)"
                                    netAverageEggWeightGrams >= 53.0 -> "متوسط (Medium)"
                                    else -> "ریز (Small/Pullet)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "میانگین وزن خالص هر عدد: ${com.example.poultryapp.engine.toPersianDigits(String.format(java.util.Locale.US, "%.1f", netAverageEggWeightGrams))} گرم",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = gradeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("تاریخ شمسی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت / توضیحات روز (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع تخم‌مرغ‌های امروز:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${com.example.poultryapp.engine.toPersianDigits(totalEggs)} عدد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "معادل ${com.example.poultryapp.engine.toPersianDigits(String.format(java.util.Locale.US, "%.1f", trayCount))} شانه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "میانگین نرخ تخم‌گذاری گله:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${com.example.poultryapp.engine.toPersianDigits(String.format(java.util.Locale.US, "%.1f", layingRate))}٪",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (layingRate >= 70.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (fertile > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نرخ نطفه‌داری تخمینی:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${com.example.poultryapp.engine.toPersianDigits(String.format(java.util.Locale.US, "%.1f", fertilityRate))}٪",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 ${com.example.poultryapp.engine.toPersianDigits(fertile)} نطفه‌دار و ${com.example.poultryapp.engine.toPersianDigits(tableEgg)} خوراکی به صورت خودکار در انبار ثبت می‌شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalEggMonetaryValue > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💰 برآورد ارزش کل ورودی: ${com.example.poultryapp.engine.DailyNutritionEngine.formatTomans(totalEggMonetaryValue)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (selectedFlockIds.isNotEmpty() && totalEggs > 0) {
                                if (onSaveDetailed != null) {
                                    onSaveDetailed(
                                        selectedFlockIds.toList(),
                                        fertile,
                                        tableEgg,
                                        broken,
                                        dirty,
                                        doubleYolk,
                                        trayCount,
                                        trayGrossGrams,
                                        netAverageEggWeightGrams,
                                        dateText,
                                        notes.ifBlank { null }
                                    )
                                } else {
                                    onSave(selectedFlockIds.toList(), healthy, broken, dirty, doubleYolk, dateText, notes.ifBlank { null })
                                }
                                onDismiss()
                            }
                        },
                        enabled = selectedFlockIds.isNotEmpty() && totalEggs > 0,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت تخم‌گذاری", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== FLOCK MORTALITY LOG DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMortalityDialog(
    flocks: List<Flock>,
    initialFlockId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (flockId: Long, count: Int, reason: MortalityReason, diagnosisHint: String?, dateJalaliStr: String, notes: String?) -> Unit
) {
    var selectedFlockId by remember {
        mutableStateOf(initialFlockId ?: flocks.firstOrNull()?.id ?: 0L)
    }
    var countText by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(MortalityReason.DISEASE) }
    var diagnosisHint by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString())
    }
    var notes by remember { mutableStateOf("") }

    val selectedFlock = flocks.find { it.id == selectedFlockId }
    val count = parsePersianOrEnglishInt(countText) ?: 0
    val maxLiveBirds = selectedFlock?.effectiveBirdCount ?: 0
    val exceedsLive = selectedFlock != null && count > maxLiveBirds

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📉", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت تلفات پرندگان",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "کسر از موجودی زنده و محاسبه در نرخ تلفات گله",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Flock
                Text(
                    text = "گله هدف:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    flocks.forEach { f ->
                        val isSelected = f.id == selectedFlockId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFlockId = f.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = f.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "موجودی: ${toPersianDigits(f.effectiveBirdCount)} قطعه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Count input
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد تلفات (قطعه)") },
                    placeholder = { Text("مثلاً: ۳") },
                    isError = exceedsLive,
                    supportingText = if (exceedsLive) {
                        {
                            Text(
                                "تعداد از موجودی زنده گله (${toPersianDigits(maxLiveBirds)} قطعه) بیشتر است",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason selection
                Text(
                    text = "علت تلفات:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MortalityReason.entries.forEach { reason ->
                        val isSelected = reason == selectedReason
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) {
                                    MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reason.titleFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "تلفات زیستی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Informational Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ تلفات بیولوژیکی: از جمعیت زنده کسر گردیده و مستقیماً در نرخ تلفات گله محاسبه می‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnosis Hint
                OutlinedTextField(
                    value = diagnosisHint,
                    onValueChange = { diagnosisHint = it },
                    label = { Text("علائم بالینی / علت احتمالی (اختیاری)") },
                    placeholder = { Text("مثلاً: علائم تنفسی، خفگی قطع برق، پرنده‌خواری") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("تاریخ شمسی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت / اقدامات انجام‌شده (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (selectedFlock != null && count > 0 && !exceedsLive) {
                                onSave(selectedFlockId, count, selectedReason, diagnosisHint.ifBlank { null }, dateText, notes.ifBlank { null })
                                onDismiss()
                            }
                        },
                        enabled = selectedFlock != null && count > 0 && !exceedsLive && dateText.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت تلفات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== SELL BIRDS DIALOG (فروش پرنده - درآمدزا) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellBirdsDialog(
    flocks: List<Flock>,
    initialFlockId: Long? = null,
    isWorkerMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        flockId: Long,
        birdCount: Int,
        liveWeightKg: Double?,
        unitPriceTomans: Double?,
        totalRevenueTomans: Double,
        buyerName: String?,
        dateJalaliStr: String,
        notes: String?,
        createFinanceTransaction: Boolean
    ) -> Unit
) {
    var selectedFlockId by remember {
        mutableStateOf(initialFlockId ?: flocks.firstOrNull()?.id ?: 0L)
    }
    var countText by remember { mutableStateOf("") }
    var liveWeightText by remember { mutableStateOf("") }
    var unitPriceText by remember { mutableStateOf("") }
    var totalRevenueText by remember { mutableStateOf("") }
    var hasManualTotalRevenue by remember { mutableStateOf(false) }
    var createFinanceTx by remember { mutableStateOf(!isWorkerMode) }
    var buyerName by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString())
    }
    var notes by remember { mutableStateOf("") }

    val selectedFlock = flocks.find { it.id == selectedFlockId }
    val count = parsePersianOrEnglishInt(countText) ?: 0
    val maxLiveBirds = selectedFlock?.effectiveBirdCount ?: 0
    val exceedsLive = selectedFlock != null && count > maxLiveBirds
    val liveWeight = parsePersianOrEnglishDouble(liveWeightText)?.takeIf { it > 0.0 }
    val unitPrice = parsePersianOrEnglishDouble(unitPriceText)?.takeIf { it > 0.0 }

    LaunchedEffect(count, liveWeight, unitPrice, hasManualTotalRevenue) {
        if (!hasManualTotalRevenue && unitPrice != null && count > 0) {
            val multiplier = if (liveWeight != null && liveWeight > 0.0) liveWeight else count.toDouble()
            totalRevenueText = (unitPrice * multiplier).toLong().toString()
        }
    }

    val totalRevenue = parsePersianOrEnglishDouble(totalRevenueText)?.coerceAtLeast(0.0) ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = com.example.poultryapp.theme.HealthGreenContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("💰", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت فروش پرنده",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "فروش تجاری جدا از تلفات با ثبت درآمد در امور مالی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "گله هدف:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    flocks.forEach { f ->
                        val isSelected = f.id == selectedFlockId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFlockId = f.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = f.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (f.isWithdrawalActive) {
                                        Text(
                                            text = "⚠️ دوره پرهیز: ${f.activeMedicationName ?: "دارو"} (${toPersianDigits(f.withdrawalRemainingHours)} ساعت)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = "موجودی: ${toPersianDigits(f.effectiveBirdCount)} قطعه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedFlock?.isWithdrawalActive == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⛔ هشدار دوره پرهیز دارویی فعال",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "گله «${selectedFlock.name}» به دلیل مصرف «${selectedFlock.activeMedicationName ?: "دارو"}» تا ${toPersianDigits(selectedFlock.withdrawalRemainingHours)} ساعت آینده در دوره پرهیز قرار دارد. عرضه جهت مصرف انسانی در این بازه توصیه نمی‌شود.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد پرنده فروخته‌شده (قطعه) *") },
                    placeholder = { Text("مثلاً: ۵۰") },
                    isError = exceedsLive,
                    supportingText = if (exceedsLive) {
                        {
                            Text(
                                "تعداد فروش از موجودی زنده گله (${toPersianDigits(maxLiveBirds)} قطعه) بیشتر است",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("تاریخ فروش (شمسی) *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = liveWeightText,
                        onValueChange = { liveWeightText = it },
                        label = { Text("وزن زنده کل (کیلو)") },
                        placeholder = { Text("اختیاری") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                if (!isWorkerMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = com.example.poultryapp.theme.HealthGreenContainer.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, com.example.poultryapp.theme.HealthGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "اطلاعات مالی فروش (درآمدزا)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = com.example.poultryapp.theme.HealthGreen
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = unitPriceText,
                                onValueChange = {
                                    unitPriceText = it.filter { c -> c.isDigit() || c == '.' || c == '٫' }
                                    hasManualTotalRevenue = false
                                },
                                label = { Text(if (liveWeight != null) "قیمت هر کیلو (تومان - اختیاری)" else "قیمت هر قطعه (تومان - اختیاری)") },
                                placeholder = { Text("محاسبه خودکار مبلغ کل") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = totalRevenueText,
                                onValueChange = {
                                    totalRevenueText = it.filter { c -> c.isDigit() || c == '.' || c == '٫' }
                                    hasManualTotalRevenue = true
                                },
                                label = { Text("مبلغ کل فروش (تومان - اختیاری)") },
                                placeholder = { Text("مثلاً: ۱۵۰۰۰۰۰۰") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { createFinanceTx = !createFinanceTx }
                            ) {
                                Checkbox(
                                    checked = createFinanceTx,
                                    onCheckedChange = { createFinanceTx = it }
                                )
                                Text(
                                    text = "ثبت خودکار درآمد در بخش امور مالی",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = buyerName,
                    onValueChange = { buyerName = it },
                    label = { Text("نام خریدار (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (selectedFlock != null && count > 0 && !exceedsLive && dateText.isNotBlank()) {
                                onSave(
                                    selectedFlockId,
                                    count,
                                    liveWeight,
                                    if (isWorkerMode) null else unitPrice,
                                    if (isWorkerMode) 0.0 else totalRevenue,
                                    buyerName.ifBlank { null },
                                    dateText.trim(),
                                    notes.ifBlank { null },
                                    !isWorkerMode && createFinanceTx && totalRevenue > 0.0
                                )
                                onDismiss()
                            }
                        },
                        enabled = selectedFlock != null && count > 0 && !exceedsLive && dateText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.poultryapp.theme.HealthGreen),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت فروش", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== SLAUGHTER BIRDS DIALOG (کشتار پرنده - فنی و غیرمالی) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlaughterBirdsDialog(
    flocks: List<Flock>,
    initialFlockId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (
        flockId: Long,
        birdCount: Int,
        birdAgeDays: Int,
        liveWeightKg: Double?,
        carcassWeightKg: Double?,
        dateJalaliStr: String,
        notes: String?
    ) -> Unit
) {
    var selectedFlockId by remember {
        mutableStateOf(initialFlockId ?: flocks.firstOrNull()?.id ?: 0L)
    }
    val initialFlock = flocks.find { it.id == selectedFlockId }
    var countText by remember { mutableStateOf("") }
    var ageDaysText by remember { mutableStateOf((initialFlock?.ageDays ?: 1).toString()) }
    var lastAutoSyncedFlockId by remember { mutableStateOf(selectedFlockId) }
    var dateText by remember {
        mutableStateOf(com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString())
    }
    var liveWeightText by remember { mutableStateOf("") }
    var carcassWeightText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val selectedFlock = flocks.find { it.id == selectedFlockId }
    LaunchedEffect(selectedFlockId) {
        if (selectedFlock != null && lastAutoSyncedFlockId != selectedFlockId) {
            ageDaysText = selectedFlock.ageDays.toString()
            lastAutoSyncedFlockId = selectedFlockId
        }
    }

    val count = parsePersianOrEnglishInt(countText) ?: 0
    val ageDays = parsePersianOrEnglishInt(ageDaysText) ?: 0
    val maxLiveBirds = selectedFlock?.effectiveBirdCount ?: 0
    val exceedsLive = selectedFlock != null && count > maxLiveBirds
    val liveWeight = parsePersianOrEnglishDouble(liveWeightText)?.takeIf { it > 0.0 }
    val carcassWeight = parsePersianOrEnglishDouble(carcassWeightText)?.takeIf { it > 0.0 }
    val carcassYieldPercent = com.example.poultryapp.data.local.FlockExitAnalytics.calculateCarcassYieldPercent(liveWeight, carcassWeight)
    val carcassExceedsLiveWeight = liveWeight != null && carcassWeight != null && carcassWeight > liveWeight

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔪", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت کشتار پرنده",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ثبت فنی سن، تاریخ و وزن کشتار (جدا از فروش و تلفات)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "گله هدف:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    flocks.forEach { f ->
                        val isSelected = f.id == selectedFlockId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFlockId = f.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = f.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "سن فعلی: ${toPersianDigits(f.ageDays)} روز | موجودی: ${toPersianDigits(f.effectiveBirdCount)} قطعه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (f.isWithdrawalActive) {
                                    Text(
                                        text = "⚠️ پرهیز",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedFlock?.isWithdrawalActive == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⛔ هشدار دوره پرهیز دارویی فعال",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "گله «${selectedFlock.name}» در دوره پرهیز داروی «${selectedFlock.activeMedicationName ?: "نامشخص"}» (${toPersianDigits(selectedFlock.withdrawalRemainingHours)} ساعت باقی‌مانده) است. کشتار پیش از اتمام دوره پرهیز توصیه نمی‌شود.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد پرنده کشتارشده (قطعه) *") },
                    placeholder = { Text("مثلاً: ۱۰۰") },
                    isError = exceedsLive,
                    supportingText = if (exceedsLive) {
                        {
                            Text(
                                "تعداد کشتار از موجودی زنده گله (${toPersianDigits(maxLiveBirds)} قطعه) بیشتر است",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageDaysText,
                        onValueChange = { ageDaysText = it.filter { c -> c.isDigit() } },
                        label = { Text("سن پرنده (روز) *") },
                        supportingText = if (ageDays > 0) {
                            { Text("حدود ${toPersianDigits(((ageDays - 1) / 7 + 1).coerceAtLeast(1))} هفته") }
                        } else null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("تاریخ کشتار (شمسی) *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = liveWeightText,
                        onValueChange = { liveWeightText = it },
                        label = { Text("وزن زنده (کیلو)") },
                        placeholder = { Text("اختیاری") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carcassWeightText,
                        onValueChange = { carcassWeightText = it },
                        label = { Text("وزن لاشه (کیلو)") },
                        placeholder = { Text("اختیاری") },
                        isError = carcassExceedsLiveWeight,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                if (carcassExceedsLiveWeight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "توجه: وزن لاشه نباید از وزن زنده بیشتر باشد.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (carcassYieldPercent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 راندمان فنی لاشه: ${toPersianDigits(String.format(java.util.Locale.US, "%.1f", carcassYieldPercent))}٪",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("نام کشتارگاه / یادداشت فنی (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (selectedFlock != null && count > 0 && ageDays > 0 && !exceedsLive && dateText.isNotBlank()) {
                                onSave(
                                    selectedFlockId,
                                    count,
                                    ageDays,
                                    liveWeight,
                                    carcassWeight,
                                    dateText.trim(),
                                    notes.ifBlank { null }
                                )
                                onDismiss()
                            }
                        },
                        enabled = selectedFlock != null && count > 0 && ageDays > 0 && !exceedsLive && dateText.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت کشتار", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== BIRD TRANSFER DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBirdsDialog(
    flocks: List<Flock>,
    sourceFlockId: Long? = null,
    onDismiss: () -> Unit,
    onTransfer: (fromFlockId: Long, toFlockId: Long, count: Int, notes: String?) -> Unit
) {
    var fromFlockId by remember { mutableStateOf(sourceFlockId ?: flocks.firstOrNull()?.id ?: 0L) }
    var toFlockId by remember {
        mutableStateOf(flocks.firstOrNull { it.id != fromFlockId }?.id ?: 0L)
    }
    var countText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val fromFlock = flocks.find { it.id == fromFlockId }
    val toFlock = flocks.find { it.id == toFlockId }
    val availableCount = fromFlock?.effectiveBirdCount ?: 0
    val transferCount = countText.toIntOrNull() ?: 0
    val isValid = transferCount in 1..availableCount && fromFlockId != toFlockId && toFlockId != 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "انتقال پرنده بین گله‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تغییر سالن و جابجایی قطعات طیور",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Source Flock
                Text(
                    text = "گله مبدأ (انتقال از):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = fromFlock?.let { "${it.name} (${com.example.poultryapp.engine.toPersianDigits(it.effectiveBirdCount)} قطعه)" } ?: "انتخاب نشده",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        flocks.forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.name} — ${com.example.poultryapp.engine.toPersianDigits(f.effectiveBirdCount)} قطعه") },
                                onClick = {
                                    fromFlockId = f.id
                                    if (toFlockId == f.id) {
                                        toFlockId = flocks.firstOrNull { it.id != f.id }?.id ?: 0L
                                    }
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Flock
                Text(
                    text = "گله مقصد (انتقال به):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = toFlock?.let { "${it.name} (${com.example.poultryapp.engine.toPersianDigits(it.effectiveBirdCount)} قطعه)" } ?: "انتخاب نشده",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        flocks.filter { it.id != fromFlockId }.forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.name} — ${com.example.poultryapp.engine.toPersianDigits(f.effectiveBirdCount)} قطعه") },
                                onClick = {
                                    toFlockId = f.id
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bird Count
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد پرنده جهت انتقال (حداکثر ${com.example.poultryapp.engine.toPersianDigits(availableCount)})") },
                    isError = transferCount > availableCount,
                    supportingText = if (transferCount > availableCount) {
                        { Text("تعداد انتخابی از موجودی گله مبدأ بیشتر است!", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("دلیل انتقال یا توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                if (isValid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "پیش‌نمایش پس از انتقال:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• ${fromFlock?.name}: ${com.example.poultryapp.engine.toPersianDigits(availableCount - transferCount)} قطعه",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• ${toFlock?.name}: ${com.example.poultryapp.engine.toPersianDigits((toFlock?.effectiveBirdCount ?: 0) + transferCount)} قطعه",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                onTransfer(fromFlockId, toFlockId, transferCount, notes.ifBlank { null })
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انجام انتقال", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== CONSUME STOCK DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumeStockDialog(
    item: WarehouseItem,
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, flockIds: List<Long>, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedFlockIds by remember {
        mutableStateOf(item.assignedFlockIds.filter { id -> flocks.any { it.id == id } }.toSet())
    }
    var notes by remember { mutableStateOf("") }

    val amount = amountText.toSafeDoubleOrNull() ?: 0.0
    val totalCost = amount * item.pricePerUnit
    val isValid = amount > 0.0 && amount <= item.currentStock

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت مصرف ${item.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "کسر از انبار و ثبت در هزینه‌های مالی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "موجودی فعلی در انبار:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${com.example.poultryapp.engine.toPersianDigits(item.currentStock.toInt())} ${item.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مقدار مصرف‌شده (${item.unit})") },
                    isError = amount > item.currentStock,
                    supportingText = if (amount > item.currentStock) {
                        { Text("مقدار مصرف نمی‌تواند بیشتر از موجودی انبار باشد!", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Multi-Flock selection section
                Text(
                    text = "گله‌های مصرف‌کننده این کالا:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "می‌توانید یک یا چند گله را انتخاب کنید یا مصرف را برای کل فارم ثبت نمایید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (flocks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "هنوز گله‌ای ثبت نشده است. مصرف برای کل فارم ثبت می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFlockIds.isEmpty(),
                            onClick = { selectedFlockIds = emptySet() },
                            label = { Text("کل فارم / عمومی", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedFlockIds.isNotEmpty() && selectedFlockIds.size == flocks.size,
                            onClick = {
                                selectedFlockIds = if (selectedFlockIds.size == flocks.size) emptySet() else flocks.map { it.id }.toSet()
                            },
                            label = { Text("همه گله‌ها", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            flocks.forEach { flock ->
                                val isChecked = flock.id in selectedFlockIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedFlockIds = if (isChecked) {
                                                selectedFlockIds - flock.id
                                            } else {
                                                selectedFlockIds + flock.id
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedFlockIds = if (checked) {
                                                selectedFlockIds + flock.id
                                            } else {
                                                selectedFlockIds - flock.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = flock.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${flock.species.titleFa} • ${flock.breed} (${com.example.poultryapp.engine.toPersianDigits(flock.birdCount)} قطعه)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت / توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                if (totalCost > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "هزینه ثبت‌شونده در مالی:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.example.poultryapp.engine.DailyNutritionEngine.formatTomans(totalCost),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                onConfirm(amount, selectedFlockIds.toList(), notes.ifBlank { null })
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت مصرف شد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Backward-compatible overload for single flockId
@Composable
fun ConsumeStockDialog(
    item: WarehouseItem,
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onConfirmSingle: (amount: Double, flockId: Long?, notes: String?) -> Unit,
    @Suppress("UNUSED_PARAMETER") legacy: Boolean = true
) {
    ConsumeStockDialog(
        item = item,
        flocks = flocks,
        onDismiss = onDismiss,
        onConfirm = { amount, flockIds, notes ->
            onConfirmSingle(amount, flockIds.firstOrNull(), notes)
        }
    )
}

// ==================== SELL EGG DIALOG ====================

@Composable
fun SellEggDialog(
    item: WarehouseItem,
    onDismiss: () -> Unit,
    onConfirm: (count: Double, pricePerUnit: Double, notes: String?) -> Unit
) {
    var countText by remember { mutableStateOf("") }
    var priceText by remember {
        mutableStateOf(if (item.pricePerUnit > 0) item.pricePerUnit.toLong().toString() else "27000")
    }
    var notes by remember { mutableStateOf("") }

    val count = countText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0
    val revenue = count * price
    val isValid = count > 0.0 && count <= item.currentStock && price > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت فروش تخم‌مرغ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "کسر از انبار و ثبت درآمد در بخش مالی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تخم‌مرغ آماده فروش در انبار:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${com.example.poultryapp.engine.toPersianDigits(item.currentStock.toInt())} عدد",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد فروش (عدد)") },
                    isError = count > item.currentStock,
                    supportingText = if (count > item.currentStock) {
                        { Text("تعداد فروش نمی‌تواند بیشتر از موجودی انبار باشد!", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("قیمت فروش هر عدد (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("خریدار / توضیحات فاکتور (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                if (revenue > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "کل مبلغ درآمد حاصله:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.example.poultryapp.engine.DailyNutritionEngine.formatTomans(revenue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                onConfirm(count, price, notes.ifBlank { null })
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت فروش رفت", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== ADD MANUAL TRANSACTION DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualTransactionDialog(
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onSave: (FinancialTransaction) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.FEED_PURCHASE) }
    var dateText by remember {
        mutableStateOf(com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString())
    }
    var selectedFlockId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toSafeDoubleOrNull() ?: 0.0
    val isValid = title.isNotBlank() && amount > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ثبت تراکنش مالی جدید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = TransactionCategory.FEED_PURCHASE
                        },
                        label = { Text("هزینه / خرج فارم") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = TransactionCategory.EGG_SALE
                        },
                        label = { Text("درآمد / فروش") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان تراکنش (مثال: خرید دان ذرت)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ به تومان") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "دسته‌بندی:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.titleFa,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        val availableCats = if (selectedType == TransactionType.EXPENSE) {
                            listOf(
                                TransactionCategory.FEED_PURCHASE,
                                TransactionCategory.FEED_CONSUMPTION,
                                TransactionCategory.MED_PURCHASE,
                                TransactionCategory.MED_SUPPLEMENT,
                                TransactionCategory.EQUIPMENT,
                                TransactionCategory.OTHER_EXPENSE
                            )
                        } else {
                            listOf(
                                TransactionCategory.EGG_SALE,
                                TransactionCategory.BIRD_SALE,
                                TransactionCategory.OTHER_INCOME
                            )
                        }
                        availableCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.titleFa) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("تاریخ شمسی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                val tx = FinancialTransaction(
                                    title = title.trim(),
                                    amountTomans = amount,
                                    type = selectedType,
                                    category = selectedCategory,
                                    dateJalaliStr = dateText,
                                    relatedFlockId = selectedFlockId,
                                    relatedFlockName = flocks.find { it.id == selectedFlockId }?.name,
                                    notes = notes.ifBlank { null }
                                )
                                onSave(tx)
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت تراکنش", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


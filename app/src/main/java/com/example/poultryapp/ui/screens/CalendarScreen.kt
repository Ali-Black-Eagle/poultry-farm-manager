package com.example.poultryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.AppTopBar
import com.example.poultryapp.ui.components.AutoResizeText
import com.example.poultryapp.ui.components.EmptyStateView
import com.example.poultryapp.ui.components.FarmSubTabBar
import com.example.poultryapp.ui.components.FarmSubTabItem
import com.example.poultryapp.ui.components.ScheduleFormDialog

@Composable
fun CalendarScreen(
    schedules: List<CalendarSchedule>,
    flocks: List<Flock>,
    onAddSchedule: (CalendarSchedule) -> Unit,
    onEditSchedule: (CalendarSchedule) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    showTopBar: Boolean = true
) {
    // 0 = Monthly View, 1 = Weekly View
    var calendarViewMode by remember { mutableIntStateOf(0) }

    val todayJalali = remember { JalaliDateHelper.getJalaliDate() }
    var displayedYear by remember { mutableIntStateOf(todayJalali.year) }
    var displayedMonth by remember { mutableIntStateOf(todayJalali.month) }
    var selectedDayOfMonth by remember { mutableIntStateOf(todayJalali.day) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<CalendarSchedule?>(null) }
    var selectedDayFilter by remember { mutableStateOf<Int?>(null) } // null = All days, 0..6 = Sat..Fri

    val daysFa = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val daysFaShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = if (calendarViewMode == 0) "تقویم برنامه‌ریزی ماهانه" else "تقویم برنامه‌ریزی هفتگی",
                    subtitle = "${toPersianDigits(schedules.size)} برنامه فعال در مزرعه",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_calendar,
                    navigationIcon = onOpenDrawer?.let {
                        {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن برنامه")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showTopBar) padding else PaddingValues(0.dp))
        ) {
            // Mode Selector: Monthly vs Weekly Tabs
            val calendarViewTabs = remember {
                listOf(
                    FarmSubTabItem(title = "نمای ماهانه", icon = Icons.Default.CalendarMonth),
                    FarmSubTabItem(title = "نمای هفتگی", icon = Icons.Default.ViewWeek)
                )
            }
            FarmSubTabBar(
                items = calendarViewTabs,
                selectedIndex = calendarViewMode,
                onTabSelected = { calendarViewMode = it }
            )

            if (calendarViewMode == 0) {
                // ==================== MONTHLY CALENDAR VIEW ====================
                val daysInMonth = JalaliDateHelper.getDaysInMonth(displayedYear, displayedMonth)
                val firstDayOffset = JalaliDateHelper.getFirstDayOfWeekOfMonth(displayedYear, displayedMonth) // 0: Sat
                val totalSlots = firstDayOffset + daysInMonth
                val totalRows = (totalSlots + 6) / 7

                // Clamp selected day if month has fewer days
                if (selectedDayOfMonth > daysInMonth) {
                    selectedDayOfMonth = daysInMonth
                }

                val selectedDayDow = JalaliDateHelper.getDayOfWeek(displayedYear, displayedMonth, selectedDayOfMonth)

                // Schedules for currently selected day (memoized)
                val selectedDaySchedules = remember(schedules, displayedYear, displayedMonth, selectedDayOfMonth) {
                    schedules.filter { sched ->
                        if (sched.scheduleMode == "MONTHLY") {
                            sched.dayOfMonth == selectedDayOfMonth
                        } else {
                            sched.daysOfWeek.contains(selectedDayDow)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Month Navigator Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Next Month Button (in RTL: Forward goes to next month)
                                IconButton(onClick = {
                                    if (displayedMonth == 12) {
                                        displayedMonth = 1
                                        displayedYear++
                                    } else {
                                        displayedMonth++
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "ماه بعد")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${JalaliDateHelper.getMonthNameFa(displayedMonth)} ${toPersianDigits(displayedYear)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (displayedYear != todayJalali.year || displayedMonth != todayJalali.month) {
                                        Text(
                                            text = "برو به ماه جاری",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier
                                                .clickable {
                                                    displayedYear = todayJalali.year
                                                    displayedMonth = todayJalali.month
                                                    selectedDayOfMonth = todayJalali.day
                                                }
                                                .padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Previous Month Button (in RTL: Back goes to previous month)
                                IconButton(onClick = {
                                    if (displayedMonth == 1) {
                                        displayedMonth = 12
                                        displayedYear--
                                    } else {
                                        displayedMonth--
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ماه قبل")
                                }
                            }
                        }
                    }

                    // Calendar Grid Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                // Weekday headers (ش تا ج)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    daysFaShort.forEachIndexed { idx, name ->
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Days Grid
                                for (row in 0 until totalRows) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (col in 0..6) {
                                            val slotIndex = row * 7 + col
                                            val dayNumber = slotIndex - firstDayOffset + 1

                                            if (dayNumber in 1..daysInMonth) {
                                                val isSelected = dayNumber == selectedDayOfMonth
                                                val isToday = dayNumber == todayJalali.day &&
                                                        displayedMonth == todayJalali.month &&
                                                        displayedYear == todayJalali.year

                                                // Find schedules for this day
                                                val dayDow = col
                                                val dayHasSchedules = schedules.any { sched ->
                                                    if (sched.scheduleMode == "MONTHLY") {
                                                        sched.dayOfMonth == dayNumber
                                                    } else {
                                                        sched.daysOfWeek.contains(dayDow)
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .then(
                                                            if (isToday && !isSelected) {
                                                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                            } else Modifier
                                                        )
                                                        .clickable {
                                                            selectedDayOfMonth = dayNumber
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = toPersianDigits(dayNumber),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                col == 6 -> MaterialTheme.colorScheme.error
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )

                                                        // Indicator dot for scheduled items
                                                        if (dayHasSchedules) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 2.dp)
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (isSelected) Color.White
                                                                        else FarmAmberTertiary
                                                                    )
                                                            )
                                                        } else {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Empty slot
                                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Day Header & Add button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "برنامه‌های ${toPersianDigits(selectedDayOfMonth)} ${JalaliDateHelper.getMonthNameFa(displayedMonth)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "روز ${daysFa[selectedDayDow]} — ${toPersianDigits(selectedDaySchedules.size)} برنامه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن برنامه", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Schedules for selected day
                    if (selectedDaySchedules.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "برنامه‌ای برای این روز ثبت نشده است",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("ثبت برنامه برای روز ${toPersianDigits(selectedDayOfMonth)} ماه")
                                    }
                                }
                            }
                        }
                    } else {
                        items(selectedDaySchedules, key = { it.id }) { schedule ->
                            ScheduleCardItem(
                                schedule = schedule,
                                onToggleSchedule = onToggleSchedule,
                                onEdit = { editingSchedule = schedule },
                                onDelete = { onDeleteSchedule(schedule.id) }
                            )
                        }
                    }
                }
            } else {
                // ==================== WEEKLY CALENDAR VIEW ====================
                val filteredSchedules = remember(schedules, selectedDayFilter) {
                    if (selectedDayFilter == null) {
                        schedules
                    } else {
                        schedules.filter { it.daysOfWeek.contains(selectedDayFilter!!) }
                    }
                }

                // Days of week selector chips (شنبه تا جمعه)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDayFilter == null,
                            onClick = { selectedDayFilter = null },
                            label = { Text("همه روزها", fontWeight = if (selectedDayFilter == null) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    items(daysFa.indices.toList()) { index ->
                        val isSelected = selectedDayFilter == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDayFilter = index },
                            label = { Text(daysFa[index], fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                if (filteredSchedules.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.CalendarMonth,
                        title = if (selectedDayFilter == null) "هیچ برنامه‌ای تعریف نشده است" else "برای ${daysFa.getOrNull(selectedDayFilter ?: 0)} برنامه‌ای نیست",
                        message = "می‌توانید برای واکسیناسیون، مصرف ویتامین، داروها، ضدعفونی و مکمل‌های گله‌ها برنامه ثبت کنید.",
                        actionButtonText = "افزودن برنامه جدید",
                        onActionClick = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSchedules, key = { it.id }) { schedule ->
                            ScheduleCardItem(
                                schedule = schedule,
                                onToggleSchedule = onToggleSchedule,
                                onEdit = { editingSchedule = schedule },
                                onDelete = { onDeleteSchedule(schedule.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Schedule Dialog
    if (showAddDialog) {
        ScheduleFormDialog(
            initialMode = if (calendarViewMode == 0) "MONTHLY" else "WEEKLY",
            initialDayOfMonth = selectedDayOfMonth,
            flocks = flocks,
            onDismiss = { showAddDialog = false },
            onConfirm = { sched ->
                onAddSchedule(sched)
                showAddDialog = false
            }
        )
    }

    // Edit Schedule Dialog
    if (editingSchedule != null) {
        ScheduleFormDialog(
            initialSchedule = editingSchedule,
            initialMode = editingSchedule?.scheduleMode ?: "WEEKLY",
            initialDayOfMonth = editingSchedule?.dayOfMonth ?: selectedDayOfMonth,
            flocks = flocks,
            onDismiss = { editingSchedule = null },
            onConfirm = { sched ->
                onEditSchedule(sched)
                editingSchedule = null
            }
        )
    }
}

@Composable
fun ScheduleCardItem(
    schedule: CalendarSchedule,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (schedule.scheduleType) {
        "واکسن" -> HealthRed
        "ویتامین" -> FarmAmberTertiary
        "دارو" -> Color(0xFF9C27B0)
        "ضدعفونی" -> WaterBlue
        "مکمل" -> FarmGreenSecondary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Line 1: Type & Mode Badges + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = schedule.scheduleType,
                            color = typeColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (schedule.scheduleMode == "MONTHLY") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "ماهانه",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Enable/Disable switch
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onToggleSchedule(schedule.id, it) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Line 2: Title (Full width, bold)
            AutoResizeText(
                text = schedule.title,
                targetFontSize = 15.sp,
                minFontSize = 11.sp,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            if (!schedule.flockName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "گله: ${schedule.flockName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days & time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ساعت ${schedule.timeDisplayFa}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = schedule.daysDisplayFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!schedule.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "توضیحات: ${schedule.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions (edit/delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

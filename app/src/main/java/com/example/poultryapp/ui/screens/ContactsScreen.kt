package com.example.poultryapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.poultryapp.data.local.ContactCategory
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.FarmDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<FarmContact>,
    onAddContact: (FarmContact) -> Unit,
    onUpdateContact: (FarmContact) -> Unit,
    onDeleteContact: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<ContactCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<FarmContact?>(null) }
    var deletingContactId by remember { mutableStateOf<Long?>(null) }

    val filteredContacts = remember(contacts, selectedCategory, searchQuery) {
        contacts.filter { contact ->
            val matchesCategory = selectedCategory == null || contact.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    contact.fullName.contains(searchQuery, ignoreCase = true) ||
                    (contact.businessName?.contains(searchQuery, ignoreCase = true) == true) ||
                    contact.phonePrimary.contains(searchQuery) ||
                    (contact.phoneSecondary?.contains(searchQuery) == true)
            matchesCategory && matchesQuery
        }
    }

    // Totals
    val totalReceivable = remember(contacts) {
        contacts.filter { it.balanceTomans > 0 }.sumOf { it.balanceTomans }
    }
    val totalPayable = remember(contacts) {
        contacts.filter { it.balanceTomans < 0 }.sumOf { -it.balanceTomans }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingContact = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "افزودن مخاطب")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مخاطب تجاری", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Financial Overview Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💼 شبکه همکاران و تامین فارم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${toPersianDigits(contacts.size)} طرف‌حساب",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "مطالبات ما (طلب):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = DailyNutritionEngine.formatTomans(totalReceivable),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "بدهی ما (تعهدات):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = DailyNutritionEngine.formatTomans(totalPayable),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی نام، واحد تجاری یا شماره تلفن...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("همه دسته‌ها (${toPersianDigits(contacts.size)})") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(ContactCategory.values()) { cat ->
                        val count = contacts.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text("${cat.titleFa} (${toPersianDigits(count)})") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Empty State
            if (filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📇",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedCategory != null) "مخاطبی با این مشخصات یافت نشد" else "دفترچه مخاطبین تجاری خالی است",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "کشتارگاه‌ها، دان‌فروشان و دامپزشکان خود را ثبت کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        onCall = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onSms = { phone ->
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                            context.startActivity(intent)
                        },
                        onEdit = {
                            editingContact = contact
                            showAddEditDialog = true
                        },
                        onDelete = {
                            deletingContactId = contact.id
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditContactDialog(
            initialContact = editingContact,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedContact ->
                if (editingContact == null) {
                    onAddContact(savedContact)
                } else {
                    onUpdateContact(savedContact)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingContactId != null) {
        AlertDialog(
            onDismissRequest = { deletingContactId = null },
            title = { Text("حذف مخاطب تجاری") },
            text = { Text("آیا از حذف این مخاطب از دفترچه تجاری فارم اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingContactId?.let { onDeleteContact(it) }
                        deletingContactId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingContactId = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun ContactCard(
    contact: FarmContact,
    onCall: (String) -> Unit,
    onSms: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Category icon, Name, Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (contact.category) {
                                    ContactCategory.SLAUGHTERHOUSE -> "🏭"
                                    ContactCategory.EGG_BUYER -> "🥚"
                                    ContactCategory.FEED_SUPPLIER -> "🌾"
                                    ContactCategory.VET_PHARMACY -> "💊"
                                    ContactCategory.HATCHERY -> "🐣"
                                    ContactCategory.TECHNICIAN -> "🔧"
                                    ContactCategory.OTHER -> "🤝"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = contact.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!contact.businessName.isNullOrBlank()) {
                            Text(
                                text = contact.businessName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Rating & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★ ${toPersianDigits(contact.ratingStars)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact Category Label & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = contact.category.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!contact.provinceCity.isNullOrBlank()) {
                    Text(
                        text = "📍 ${contact.provinceCity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Financial Balance Tag
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    contact.balanceTomans > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    contact.balanceTomans < 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            contact.balanceTomans > 0 -> "وضعیت حساب: طلبکاریم (+)"
                            contact.balanceTomans < 0 -> "وضعیت حساب: بدهکاریم (-)"
                            else -> "وضعیت حساب: تسویه"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = DailyNutritionEngine.formatTomans(kotlin.math.abs(contact.balanceTomans)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            contact.balanceTomans > 0 -> MaterialTheme.colorScheme.primary
                            contact.balanceTomans < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (!contact.paymentTermsNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 شرایط پرداخت: ${contact.paymentTermsNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Call & SMS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onCall(contact.phonePrimary) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تماس تلفنی", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { onSms(contact.phonePrimary) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال پیامک", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    initialContact: FarmContact?,
    onDismiss: () -> Unit,
    onSave: (FarmContact) -> Unit
) {
    var fullName by remember { mutableStateOf(initialContact?.fullName ?: "") }
    var businessName by remember { mutableStateOf(initialContact?.businessName ?: "") }
    var selectedCategory by remember { mutableStateOf(initialContact?.category ?: ContactCategory.FEED_SUPPLIER) }
    var phonePrimary by remember { mutableStateOf(initialContact?.phonePrimary ?: "") }
    var phoneSecondary by remember { mutableStateOf(initialContact?.phoneSecondary ?: "") }
    var address by remember { mutableStateOf(initialContact?.address ?: "") }
    var provinceCity by remember { mutableStateOf(initialContact?.provinceCity ?: "") }
    var balanceText by remember { mutableStateOf(if (initialContact != null && initialContact.balanceTomans != 0.0) kotlin.math.abs(initialContact.balanceTomans).toLong().toString() else "") }
    var isReceivable by remember { mutableStateOf((initialContact?.balanceTomans ?: 0.0) >= 0) } // true: طلب، false: بدهی
    var paymentTermsNotes by remember { mutableStateOf(initialContact?.paymentTermsNotes ?: "") }
    var ratingStars by remember { mutableStateOf(initialContact?.ratingStars ?: 5) }

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
                        text = if (initialContact == null) "افزودن مخاطب تجاری جدید" else "ویرایش مخاطب تجاری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("نام و نام خانوادگی *") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Business Name
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("نام شرکت، واحد تجاری یا فروشگاه") },
                    placeholder = { Text("مثلاً: کشتارگاه بهاران، دان طیور آریا") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selection
                Text(
                    text = "رده فعالیت تجاری:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ContactCategory.values().forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.titleFa,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Phone
                OutlinedTextField(
                    value = phonePrimary,
                    onValueChange = { phonePrimary = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("شماره همراه اصلی *") },
                    placeholder = { Text("۰۹۱۲...") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Phone
                OutlinedTextField(
                    value = phoneSecondary,
                    onValueChange = { phoneSecondary = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("شماره تماس دوم / ثابت (اختیاری)") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // City / Province
                OutlinedTextField(
                    value = provinceCity,
                    onValueChange = { provinceCity = it },
                    label = { Text("شهر / استان (اختیاری)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Balance
                Text(
                    text = "تراز مالی جاری (تومان):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isReceivable,
                        onClick = { isReceivable = true },
                        label = { Text("طلبکاریم (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isReceivable,
                        onClick = { isReceivable = false },
                        label = { Text("بدهکاریم (-)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() } },
                    label = { Text("مبلغ تراز به تومان") },
                    placeholder = { Text("مثلاً: ۱۵۰۰۰۰۰۰") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment terms notes
                OutlinedTextField(
                    value = paymentTermsNotes,
                    onValueChange = { paymentTermsNotes = it },
                    label = { Text("شرایط پرداخت و تسویه (اختیاری)") },
                    placeholder = { Text("مثلاً: چک صیادی ۴۵ روزه، تسویه نقدی پای باسکول") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Rating Stars
                Text(
                    text = "امتیاز اعتماد و خوش‌حسابی: ${toPersianDigits(ratingStars)} از ۵",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { ratingStars = star }) {
                            Text(
                                text = if (star <= ratingStars) "★" else "☆",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (star <= ratingStars) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
                            val finalBalance = if (isReceivable) parsedBalance else -parsedBalance
                            val contact = (initialContact ?: FarmContact(
                                fullName = fullName.trim(),
                                category = selectedCategory,
                                phonePrimary = phonePrimary.trim()
                            )).copy(
                                fullName = fullName.trim(),
                                businessName = businessName.trim().ifBlank { null },
                                category = selectedCategory,
                                phonePrimary = phonePrimary.trim(),
                                phoneSecondary = phoneSecondary.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                provinceCity = provinceCity.trim().ifBlank { null },
                                balanceTomans = finalBalance,
                                paymentTermsNotes = paymentTermsNotes.trim().ifBlank { null },
                                ratingStars = ratingStars
                            )
                            onSave(contact)
                        },
                        enabled = fullName.isNotBlank() && phonePrimary.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("ذخیره مخاطب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

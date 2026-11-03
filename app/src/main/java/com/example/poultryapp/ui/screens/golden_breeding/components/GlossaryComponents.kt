package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.BreedingGlossary
import com.example.poultryapp.data.model.GlossaryTerm
import com.example.poultryapp.engine.toPersianDigits

/**
 * ردیف چیپ‌های تعاملی واژگان تخصصی موجود در چالش
 * امکان دسترسی سریع و لمس آسان با انگشت بدون دردسر برای کاربر
 */
@Composable
fun GlossaryChipsRow(
    detectedTerms: List<GlossaryTerm>,
    onTermClick: (GlossaryTerm) -> Unit,
    modifier: Modifier = Modifier
) {
    if (detectedTerms.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE0F2FE),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "واژه‌ها:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }
        }

        detectedTerms.forEach { term ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.clickable { onTermClick(term) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(text = term.iconEmoji, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = term.titleFa,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }
    }
}

/**
 * کامپوننت متن هایپرلینک‌شده برای نمایش کلمات کلیدی داخل متن با خط زیرین و هایلایت
 */
@Composable
fun GlossaryAnnotatedText(
    text: String,
    detectedTerms: List<GlossaryTerm>,
    onTermClick: (GlossaryTerm) -> Unit,
    modifier: Modifier = Modifier,
    baseFontSize: Float = 13.5f,
    baseColor: Color = Color(0xFF1E293B),
    baseLineHeight: Float = 22f
) {
    if (detectedTerms.isEmpty()) {
        Text(
            text = text.toPersianDigits(),
            fontSize = baseFontSize.sp,
            lineHeight = baseLineHeight.sp,
            fontWeight = FontWeight.Medium,
            color = baseColor,
            modifier = modifier
        )
        return
    }

    val annotatedString = remember(text, detectedTerms) {
        buildAnnotatedString {
            append(text)
            // یافتن محل وقوع هر واژه در متن و ثبت لینک کلیک مستقیم
            detectedTerms.forEach { term ->
                val searchKeys = listOf(term.key) + term.synonyms
                searchKeys.forEach { key ->
                    var startIndex = text.indexOf(key, ignoreCase = true)
                    while (startIndex != -1) {
                        val endIndex = startIndex + key.length
                        addLink(
                            clickable = androidx.compose.ui.text.LinkAnnotation.Clickable(
                                tag = term.key,
                                styles = androidx.compose.ui.text.TextLinkStyles(
                                    style = SpanStyle(
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = {
                                    BreedingGlossary.findTerm(term.key)?.let { t ->
                                        onTermClick(t)
                                    }
                                }
                            ),
                            start = startIndex,
                            end = endIndex
                        )
                        startIndex = text.indexOf(key, startIndex + key.length, ignoreCase = true)
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        fontSize = baseFontSize.sp,
        lineHeight = baseLineHeight.sp,
        fontWeight = FontWeight.Medium,
        color = baseColor
    )
}

/**
 * دیالوگ مدرن و زیبا جهت تشریح جامع و ساده واژه تخصصی برای افراد تازه‌کار
 */
@Composable
fun GlossaryTermDialog(
    term: GlossaryTerm,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
                .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // هدر دیالوگ با آیکون و دکمه بستن
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }

                    // چیپ دسته‌بندی
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = term.categoryFa,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(text = term.iconEmoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // عنوان فارسی و انگلیسی
                Text(
                    text = term.titleFa,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    lineHeight = 24.sp
                )
                if (!term.titleEn.isNullOrBlank()) {
                    Text(
                        text = term.titleEn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // بخش ۱: تعریف به زبان ساده برای افراد تازه‌کار
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF9C3)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تعریف به زبان ساده برای مبتدیان:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF854D0E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = term.beginnerDefinitionFa.toPersianDigits(),
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF713F12)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // بخش ۲: کاربرد و اهمیت عملی در پرورش
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔬", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "چرا در پرورش و سالن فارم حیاتی است؟",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = term.practicalImpactFa.toPersianDigits(),
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // بخش ۳: نکته طلایی مزرعه
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌾", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "توصیه کلیدی پرورش‌دهنده:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = term.farmTipFa.toPersianDigits(),
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // دکمه تایید و بستن
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "متوجه شدم 👍", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * مرورگر کامل تمام اصطلاحات واژه‌نامه همراه با جستجو
 */
@Composable
fun GlossaryBrowserDialog(
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }

    val filteredTerms = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            BreedingGlossary.allTerms
        } else {
            BreedingGlossary.allTerms.filter {
                it.titleFa.contains(searchQuery, ignoreCase = true) ||
                        it.key.contains(searchQuery, ignoreCase = true) ||
                        (it.titleEn?.contains(searchQuery, ignoreCase = true) == true) ||
                        it.synonyms.any { s -> s.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // هدر
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                    Text(
                        text = "📖 فرهنگ واژگان و اصطلاحات تخصصی طیور",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                }

                // فیلد جستجو
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی اصطلاح (مثلاً: FCR، کانیبالیسم، فتوپریود)...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )

                Text(
                    text = "${filteredTerms.size.toPersianDigits()} واژه تخصصی یافت شد (برای مشاهده جزئیات روی کارت کلیک کنید)",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTerms) { term ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                .clickable { selectedTerm = term }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(text = term.iconEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = term.titleFa,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFEF08A)
                                    )
                                    Text(
                                        text = term.beginnerDefinitionFa.toPersianDigits(),
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 2,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTerm?.let { term ->
        GlossaryTermDialog(term = term, onDismiss = { selectedTerm = null })
    }
}

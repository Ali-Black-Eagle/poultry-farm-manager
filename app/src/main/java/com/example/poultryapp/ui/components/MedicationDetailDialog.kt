package com.example.poultryapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.PoultryMedication
import com.example.poultryapp.theme.FarmDimens
import com.example.poultryapp.theme.FeedAmber
import com.example.poultryapp.theme.HealthRed

/**
 * Reusable full clinical pharmacological monograph dialog for a poultry medication / supplement.
 */
@Composable
fun MedicationDetailDialog(
    medication: PoultryMedication,
    onDismiss: () -> Unit
) {
    val categoryColor = Color(medication.category.badgeColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FarmDimens.Space16)
            ) {
                // Header Row
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
                            color = categoryColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(FarmDimens.Space10))
                        Column {
                            Text(
                                text = medication.tradeNameFa,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = medication.genericNameEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                // Detail Scrollable Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                ) {
                    // Category, Manufacturer & Route tags
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = categoryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = medication.category.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = medication.manufacturer,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = medication.administrationRoute.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Active Ingredients Card
                    item {
                        MedicationSectionBlock(
                            title = "🧪 ترکیبات و مواد مؤثره در هر واحد",
                            content = medication.activeIngredientsFa,
                            titleColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Packaging & Target Species
                    item {
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space10)) {
                                Text(
                                    text = "📦 شکل دارویی و بسته‌بندی: ${medication.formAndPackaging}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🕊️ گونه‌های هدف: " + medication.targetSpecies.joinToString("، ") { it.titleFa },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Indications
                    item {
                        MedicationSectionBlock(
                            title = "🎯 موارد مصرف و اثربخشی بالینی",
                            content = medication.indicationsFa,
                            titleColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Dosage & Duration
                    item {
                        MedicationSectionBlock(
                            title = "⚖️ دوز، مقدار و نحوه مصرف در گله",
                            content = "${medication.dosageFa}\n\n⏳ طول دوره مصرف: ${medication.durationDaysFa}",
                            titleColor = FeedAmber
                        )
                    }

                    // Withdrawal Period
                    item {
                        MedicationSectionBlock(
                            title = "⚠️ دوره پرهیز از مصرف گوشت و تخم‌مرغ (Withdrawal Period)",
                            content = medication.withdrawalPeriodFa,
                            titleColor = HealthRed
                        )
                    }

                    // Contraindications
                    item {
                        MedicationSectionBlock(
                            title = "🚫 موارد منع مصرف و تداخلات دارویی",
                            content = medication.contraindicationsFa,
                            titleColor = MaterialTheme.colorScheme.error
                        )
                    }

                    // Storage and Precautions
                    item {
                        MedicationSectionBlock(
                            title = "🛡️ شرایط نگهداری و نکات ایمنی",
                            content = medication.storageAndPrecautionsFa,
                            titleColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("متوجه شدم و بستن")
                }
            }
        }
    }
}

@Composable
fun MedicationSectionBlock(
    title: String,
    content: String,
    titleColor: Color
) {
    Surface(
        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(FarmDimens.Space12)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(FarmDimens.Space6))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}

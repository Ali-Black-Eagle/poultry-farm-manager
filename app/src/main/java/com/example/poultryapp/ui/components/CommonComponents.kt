package com.example.poultryapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.R
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.LiveTrackerStatus
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*

/**
 * App Navigation Tabs with authentic poultry and farm icons and custom drawables.
 */
enum class AppTab(val titleFa: String, val icon: ImageVector, val drawableRes: Int) {
    MANAGEMENT("مدیریت", Icons.Default.AdminPanelSettings, R.drawable.ic_farm_management),
    FEED_WATER("دان و آب", Icons.Default.WaterDrop, R.drawable.ic_farm_feed_water),
    HOME("خانه", Icons.Default.Home, R.drawable.ic_farm_dashboard),
    HEALTH("سلامت", Icons.Default.MedicalServices, R.drawable.ic_farm_health),
    // Drawer destinations / Legacy:
    FLOCKS("گله", Icons.Default.Groups, R.drawable.ic_farm_flocks),
    CALENDAR("تقویم", Icons.Default.CalendarMonth, R.drawable.ic_farm_calendar),
    WAREHOUSE("انبار", Icons.Default.Inventory2, R.drawable.ic_farm_warehouse),
    SETTINGS("تنظیمات", Icons.Default.Settings, R.drawable.ic_farm_settings),
    BREEDING_GUIDE("راهنمای پرورش", Icons.Default.MenuBook, R.drawable.ic_farm_guide),
    GOLDEN_BREEDING("پرورش طلایی", Icons.Default.EmojiEvents, R.drawable.ic_farm_egg)
}

/**
 * Clean Top Bar for Farm Tool
 * Compact, subtle 1dp bottom border, minimum clutter.
 */
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    logoRes: Int? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = FarmDimens.Space12, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AutoResizeText(
                            text = title,
                            targetFontSize = 14.5.sp,
                            minFontSize = 11.sp,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (!subtitle.isNullOrBlank()) {
                            AutoResizeText(
                                text = subtitle,
                                targetFontSize = 10.5.sp,
                                minFontSize = 8.5.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space4)
                ) {
                    actions()
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Redesigned, state-of-the-art Connected Animated Bottom Navigation Bar.
 * Features a fluid, connected sliding indicator pill across tabs with spring physics,
 * dynamic stretch during motion, scaling & bouncing icon micro-animations,
 * sliding top neon accent line, and haptic feedback.
 */
@Composable
fun AppBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val bottomTabs = listOf(AppTab.MANAGEMENT, AppTab.FLOCKS, AppTab.HOME, AppTab.HEALTH)
    val effectiveTab = when (currentTab) {
        AppTab.FEED_WATER, AppTab.CALENDAR -> AppTab.FLOCKS
        AppTab.WAREHOUSE -> AppTab.MANAGEMENT
        else -> currentTab
    }
    val isBottomTabActive = bottomTabs.contains(effectiveTab)
    val selectedIndex = if (isBottomTabActive) bottomTabs.indexOf(effectiveTab) else -1
    val haptic = LocalHapticFeedback.current

    // Connected spring animation for the sliding indicator pill
    val animatedIndex by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) selectedIndex.toFloat() else 1.5f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slidingNavPill"
    )

    // Dynamic stretch effect during motion to make tabs feel physically connected
    val distance = if (selectedIndex >= 0) kotlin.math.abs(selectedIndex - animatedIndex) else 0f
    val stretchFraction = (distance * 0.28f).coerceIn(0f, 0.40f)

    val pillAlpha by animateFloatAsState(
        targetValue = if (isBottomTabActive) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "pillAlpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val tabWidth = maxWidth / bottomTabs.size

                // Sliding Capsule Pill (Connected background indicator)
                val basePillWidth = tabWidth * 0.84f
                val pillWidth = basePillWidth * (1f + stretchFraction)
                val pillCenter = (tabWidth * animatedIndex) + (tabWidth / 2)
                val pillStart = pillCenter - (pillWidth / 2)

                Box(
                    modifier = Modifier
                        .offset(x = pillStart, y = 8.dp)
                        .width(pillWidth)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f * pillAlpha),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f * pillAlpha)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f * pillAlpha),
                            RoundedCornerShape(14.dp)
                        )
                )

                // 3. Tab Buttons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomTabs.forEach { tab ->
                        val isSelected = isBottomTabActive && tab == effectiveTab

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 0.95f,
                            animationSpec = spring(
                                dampingRatio = 0.58f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "navIconScale_${tab.name}"
                        )

                        val iconLift by animateDpAsState(
                            targetValue = if (isSelected) (-1).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "navIconLift_${tab.name}"
                        )

                        val activeColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor else inactiveColor,
                            animationSpec = tween(durationMillis = 220),
                            label = "navColor_${tab.name}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    translationY = iconLift.toPx()
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.titleFa,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.titleFa,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Unified Modern Sub-Tab Switcher (Segmented Pill Bar)
 * Provides a fluid, highly polished, consistent look across all screens in the app.
 * Automatically distributes tabs equally (weight = 1f) so all items fit 100% on the screen.
 * Features a single continuous sliding pill indicator that glides seamlessly across tabs.
 */
data class FarmSubTabItem(
    val title: String,
    val icon: ImageVector? = null,
    val badge: String? = null,
    val drawableRes: Int? = null
)

@Composable
fun FarmSubTabBar(
    items: List<FarmSubTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    isScrollable: Boolean = items.size > 4
) {
    val haptic = LocalHapticFeedback.current
    val currentTargetIndex = if (pagerState != null) pagerState.currentPage else selectedIndex

    if (isScrollable) {
        val lazyListState = rememberLazyListState()

        // Smart Auto-Scroll: Ensure the selected tab is always comfortably brought into view
        LaunchedEffect(currentTargetIndex) {
            if (currentTargetIndex in items.indices) {
                lazyListState.animateScrollToItem(currentTargetIndex)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth()
        ) {
            LazyRow(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentPadding = PaddingValues(horizontal = FarmDimens.Space16, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    itemsIndexed(items) { index, item ->
                        val isSelected = currentTargetIndex == index
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            label = "subTabBg"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            animationSpec = tween(durationMillis = 200),
                            label = "subTabBorder"
                        )
                        val animatedContentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 200),
                            label = "subTabContent"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
                            label = "subTabScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                            color = animatedBgColor,
                            border = BorderStroke(1.dp, animatedBorderColor),
                            modifier = Modifier
                                .height(38.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(FarmDimens.RadiusMedium))
                                .clickable {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(index)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (item.drawableRes != null) {
                                    Image(
                                        painter = painterResource(id = item.drawableRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                } else if (item.icon != null) {
                                    if (item.badge != null) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text(
                                                        text = item.badge,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = animatedContentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = animatedContentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = animatedContentColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
    } else {
        val isDense = items.size >= 4
        val tabCount = items.size.coerceAtLeast(1)

        // Calculate continuous animated position:
        // If pagerState is attached, use real-time scroll offset (continuous 60/120fps lockstep).
        // Otherwise, animate with a physics spring.
        val continuousIndex: Float = if (pagerState != null) {
            val raw = pagerState.currentPage + pagerState.currentPageOffsetFraction
            raw.coerceIn(0f, (tabCount - 1).toFloat())
        } else {
            val target = selectedIndex.toFloat().coerceIn(0f, (tabCount - 1).toFloat())
            val anim by animateFloatAsState(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "subTabSlidingPill"
            )
            anim
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FarmDimens.Space16, vertical = 6.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp)
                    .height(if (isDense) 40.dp else 44.dp)
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / tabCount
                val pillWidth = (tabWidth - 4.dp).coerceAtLeast(0.dp)
                val pillStart = (tabWidth * continuousIndex + 2.dp).coerceIn(0.dp, (totalWidth - pillWidth).coerceAtLeast(0.dp))

                // 1. Continuous Sliding Pill Indicator (نشانگر پیوسته لغزنده)
                Box(
                    modifier = Modifier
                        .offset(x = pillStart, y = 0.dp)
                        .width(pillWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(FarmDimens.RadiusSmall))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
                                )
                            )
                        )
                )

                // 2. Tab Items Row above the sliding indicator
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        // Continuous smooth proximity and color transition without threshold popping
                        val proximity = (1f - kotlin.math.abs(continuousIndex - index)).coerceIn(0f, 1f)
                        val smoothFraction = FastOutSlowInEasing.transform(proximity)
                        val animatedTextColor = lerp(
                            start = MaterialTheme.colorScheme.onSurfaceVariant,
                            stop = MaterialTheme.colorScheme.onPrimary,
                            fraction = smoothFraction
                        )
                        val badgeContainerColor = lerp(
                            start = MaterialTheme.colorScheme.primary,
                            stop = MaterialTheme.colorScheme.onPrimary,
                            fraction = smoothFraction
                        )
                        val badgeContentColor = lerp(
                            start = MaterialTheme.colorScheme.onPrimary,
                            stop = MaterialTheme.colorScheme.primary,
                            fraction = smoothFraction
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(FarmDimens.RadiusSmall))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.drawableRes != null) {
                                    Image(
                                        painter = painterResource(id = item.drawableRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isDense) 14.dp else 16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isDense) 3.dp else 5.dp))
                                } else if (item.icon != null) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = animatedTextColor,
                                        modifier = Modifier.size(if (isDense) 14.dp else 16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isDense) 3.dp else 5.dp))
                                }
                                AutoResizeText(
                                    text = item.title,
                                    targetFontSize = if (isDense) 10.sp else 11.5.sp,
                                    minFontSize = 8.sp,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = animatedTextColor,
                                    maxLines = 1
                                )
                                if (item.badge != null) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = badgeContainerColor
                                    ) {
                                        Text(
                                            text = item.badge,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeContentColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
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

/**
 * Typographic component for displaying Persian numbers and units.
 * Separates numeric value and unit cleanly to prevent RTL flipping or scramble.
 */
@Composable
fun PersianValueUnit(
    value: Any,
    unit: String? = null,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    unitStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelSmall,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = toPersianDigits(value),
            style = valueStyle,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
        if (!unit.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(FarmDimens.Space4))
            Text(
                text = unit,
                style = unitStyle,
                color = unitColor,
                modifier = Modifier.padding(bottom = 2.dp),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Standardized Section Header with optional generated farm icon
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    iconRes: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (actionText != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.padding(start = FarmDimens.Space8)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = actionText,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact Farm KPI / Stat Card
 * Numbers dominant, units secondary, clean hairline border.
 */
@Composable
fun FarmStatCard(
    title: String,
    value: Any,
    unit: String? = null,
    subtitle: String? = null,
    recommendedValue: String? = null,
    recommendedSubtitle: String? = null,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    containerBg: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(FarmDimens.CardRadius),
        color = containerBg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (icon != null) {
                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                PersianValueUnit(
                    value = value,
                    unit = unit,
                    valueStyle = MaterialTheme.typography.titleMedium,
                    unitStyle = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space8))

            AutoResizeText(
                text = title,
                targetFontSize = 12.sp,
                minFontSize = 9.sp,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            // Subtitle (e.g. today's feed value / ارزش دان امروز) directly below title
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                AutoResizeText(
                    text = subtitle,
                    targetFontSize = 11.sp,
                    minFontSize = 8.5.sp,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconTint,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            // Recommended section positioned UNDER both title and subtitle so they never get mixed up
            if (!recommendedValue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(FarmDimens.Space6))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconTint.copy(alpha = 0.09f),
                    border = BorderStroke(0.5.dp, iconTint.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "پیشنهادی: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = recommendedValue,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iconTint
                            )
                        }
                        if (!recommendedSubtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            AutoResizeText(
                                text = recommendedSubtitle,
                                targetFontSize = 10.sp,
                                minFontSize = 8.sp,
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iconTint.copy(alpha = 0.95f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Semantic Status Badge
 */
@Composable
fun FarmStatusBadge(
    text: String,
    color: Color,
    containerColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val effectiveContainer = if (isDark && containerColor.luminance() > 0.4f) {
        when (containerColor) {
            HealthRedContainer -> HealthRedContainerDark
            HealthGreenContainer -> HealthGreenContainerDark
            HealthYellowContainer -> HealthYellowContainerDark
            FeedAmberContainer -> Color(0xFF2E1C0A)
            WaterBlueContainer -> Color(0xFF0C2438)
            else -> containerColor.copy(alpha = 0.25f)
        }
    } else containerColor

    val effectiveColor = if (isDark && color.luminance() < 0.25f) {
        when (color) {
            HealthRedText -> HealthRedTextDark
            HealthGreenText -> HealthGreenTextDark
            FeedAmberText -> FeedAmberDark
            WaterBlueText -> WaterBlueTextDark
            else -> Color.White
        }
    } else color

    Surface(
        shape = RoundedCornerShape(FarmDimens.BadgeRadius),
        color = effectiveContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmDimens.Space8, vertical = FarmDimens.Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space4))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = effectiveColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Live Charge Card for Feed & Water
 * Redesigned with hairline border, high readability, and stable button layout.
 */
@Composable
fun LiveChargeCard(
    itemType: ChargeItemType,
    status: LiveTrackerStatus,
    flockName: String,
    isEnabled: Boolean = true,
    onToggleEnabled: ((Boolean) -> Unit)? = null,
    onChargeClick: () -> Unit,
    onEmptyClick: (() -> Unit)? = null
) {
    val isWater = itemType == ChargeItemType.WATER
    val accentColor = if (isWater) WaterBlue else FeedAmber
    val containerBg = if (isWater) WaterBlueContainer else FeedAmberContainer
    val animatedProgress by animateFloatAsState(
        targetValue = status.progress,
        animationSpec = tween(durationMillis = 200),
        label = "progress"
    )

    Surface(
        shape = RoundedCornerShape(FarmDimens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FarmDimens.Space4)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space14)
        ) {
            // Header Row: Icon, Title & Switch only (Spacious, no button crowding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val itemIconRes = if (itemType == ChargeItemType.FEED) R.drawable.ic_farm_feed else R.drawable.ic_farm_water
                    Image(
                        painter = painterResource(id = itemIconRes),
                        contentDescription = itemType.titleFa,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(FarmDimens.Space12))
                    Column(modifier = Modifier.weight(1f)) {
                        AutoResizeText(
                            text = "${itemType.titleFa} — گله $flockName",
                            targetFontSize = 14.sp,
                            minFontSize = 10.sp,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (isEnabled) "پایش وضعیت فعال است" else "پایش ${itemType.titleFa} غیرفعال است",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onToggleEnabled != null) {
                    Spacer(modifier = Modifier.width(FarmDimens.Space8))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            }

            if (!isEnabled) {
                Spacer(modifier = Modifier.height(FarmDimens.Space10))
                Surface(
                    shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "پایش خودکار و هشدارهای ${itemType.titleFa} برای این گله متوقف شده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(FarmDimens.Space10)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                // Elapsed time row (Full width, never wrapped)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                    Text(
                        text = status.elapsedTimeFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                // Progress Bar & Percentage & Remaining amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "مانده تخمینی: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = status.remainingAmountFa,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${toPersianDigits((status.progress * 100).toInt())}٪",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isLowWarning || status.isFinished) HealthRed else accentColor
                    )
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space6))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(FarmDimens.RadiusFull)),
                    color = when {
                        status.isFinished -> HealthRed
                        status.isLowWarning -> HealthRed
                        status.progress < 0.35f -> HealthYellow
                        else -> accentColor
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space8))

                // Status message row with high contrast & harmonious theme-aware colors
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val statusContainerColor = when {
                    status.isFinished -> if (isDark) Color(0xFF381414) else HealthRedContainer
                    status.isLowWarning -> if (isDark) Color(0xFF382905) else Color(0xFFFEF9C3)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val statusBorder: BorderStroke? = when {
                    status.isFinished -> BorderStroke(1.dp, if (isDark) Color(0xFFEF4444).copy(alpha = 0.55f) else HealthRed.copy(alpha = 0.35f))
                    status.isLowWarning -> BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFCA8A04).copy(alpha = 0.40f))
                    else -> null
                }
                val statusIconTint = when {
                    status.isFinished -> if (isDark) Color(0xFFF87171) else HealthRed
                    status.isLowWarning -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                    else -> MaterialTheme.colorScheme.primary
                }
                val statusTextColor = when {
                    status.isFinished -> if (isDark) Color(0xFFFECACA) else HealthRedText
                    status.isLowWarning -> if (isDark) Color(0xFFFEF08A) else Color(0xFF78350F)
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                    color = statusContainerColor,
                    border = statusBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FarmDimens.Space10, vertical = FarmDimens.Space6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                status.isFinished -> Icons.Default.Warning
                                status.isLowWarning -> Icons.Default.PriorityHigh
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = statusIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(FarmDimens.Space8))
                        Text(
                            text = status.statusTextFa,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (status.isFinished || status.isLowWarning) FontWeight.Bold else FontWeight.Normal,
                            color = statusTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                if (onEmptyClick != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onChargeClick,
                            shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(FarmDimens.IconSmall),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space6))
                            Text(
                                text = "شارژ ${itemType.titleFa}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = onEmptyClick,
                            shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space4))
                            Text(
                                text = "تخلیه منبع",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    // Full-width, prominent charge button
                    Button(
                        onClick = onChargeClick,
                        shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(FarmDimens.IconSmall),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(FarmDimens.Space6))
                        Text(
                            text = "ثبت شارژ جدید ${itemType.titleFa}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, Helpful Empty State View with optional custom generated image
 */
@Composable
fun EmptyStateView(
    icon: ImageVector? = null,
    imageRes: Int? = null,
    title: String,
    message: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FarmDimens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(FarmDimens.IconLarge)
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmDimens.Space12))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FarmDimens.Space4))

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(FarmDimens.Space16))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                contentPadding = PaddingValues(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space10)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(FarmDimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                Text(
                    text = actionButtonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

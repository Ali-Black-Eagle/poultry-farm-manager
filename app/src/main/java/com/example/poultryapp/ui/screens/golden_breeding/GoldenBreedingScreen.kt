package com.example.poultryapp.ui.screens.golden_breeding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.poultryapp.data.model.GameSeasonTier
import com.example.poultryapp.data.model.GoldenBirdType

/**
 * صفحه اصلی بخش پرورش طلایی (مدیریت انتخاب پرنده و نقشه بازی در هر دو فصل مقدماتی و بورد فوق‌حرفه‌ای)
 * مجهز به کانتینر با پس‌زمینه تیره ضد فلش سفید و ترنزیشن لایه‌بندی‌شده ۶۰ فریم فوق‌العاده نرم
 */
@Composable
fun GoldenBreedingScreen(
    onOpenDrawer: () -> Unit,
    onBackToFarm: (() -> Unit)? = null
) {
    var selectedBirdType by rememberSaveable { mutableStateOf<GoldenBirdType?>(null) }
    var selectedSeasonTier by rememberSaveable { mutableStateOf(GameSeasonTier.NOVICE) }

    // بازگشت با دکمه سخت‌افزاری Back:
    // ۱. اگر در نقشه پرنده است -> بازگشت به لیست پرندگان
    // ۲. اگر در لیست پرندگان است -> بازگشت نرم به داشبورد فارم
    BackHandler(enabled = true) {
        if (selectedBirdType != null) {
            selectedBirdType = null
        } else {
            onBackToFarm?.invoke()
        }
    }

    // کانتینر ریشه با پس‌زمینه زمردی/آبسیدین تیره تالار طلایی جهت تضمین عدم نشت رنگ سفید در تمام فازهای ترنزیشن
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03160E))
    ) {
        AnimatedContent(
            targetState = selectedBirdType,
            transitionSpec = {
                if (targetState != null) {
                    // ورود به نقشه پرنده: بالا آمدن نرم از پایین و باز شدن روی صفحه قبلی با zIndex = 1
                    (fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                     scaleIn(initialScale = 0.96f, animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                     slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(260, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            scaleOut(targetScale = 0.96f, animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                            fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))
                        ).apply {
                            targetContentZIndex = 1f
                        }
                } else {
                    // بازگشت از نقشه به لیست پرندگان: سر خوردن ملایم نقشه به پایین و نمایان شدن روان لیست
                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                     scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            slideOutVertically(targetOffsetY = { it / 14 }, animationSpec = tween(220, easing = FastOutLinearInEasing)) +
                            fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(220, easing = FastOutLinearInEasing))
                        ).apply {
                            targetContentZIndex = 0f
                        }
                }
            },
            label = "GoldenBreedingAnimatedContent"
        ) { bird ->
            if (bird == null) {
                BirdSelectionScreen(
                    selectedSeason = selectedSeasonTier,
                    onSeasonChanged = { selectedSeasonTier = it },
                    onSelectBird = { selectedBird, tier ->
                        selectedSeasonTier = tier
                        selectedBirdType = selectedBird
                    },
                    onOpenDrawer = onOpenDrawer,
                    onBackToFarm = onBackToFarm
                )
            } else {
                GoldenGameMapScreen(
                    birdType = bird,
                    seasonTier = selectedSeasonTier,
                    onBackToSelection = { selectedBirdType = null }
                )
            }
        }
    }
}

package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.model.GoldenBirdType
import com.example.poultryapp.engine.toPersianDigits
import kotlin.math.cos
import kotlin.math.sin

/**
 * پس‌زمینه منظره‌پردازی‌شده و اثر هنری منحصربه‌فرد برای هر گونه پرنده
 */
@Composable
fun BirdMapAtmosphericBackdrop(
    birdType: GoldenBirdType,
    isPro: Boolean = false,
    isScrolling: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphericBiomeAnim")

    val animPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BiomePulse"
    )

    val animShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BiomeShimmer"
    )

    // هنگام اسکرول فعال کاربر، مقادیر فریم انیمیشن ثابت نگه داشته می‌شوند تا رندرهای پس‌زمینه متوقف شده و اسکرول ۶۰ تا ۱۲۰ فریم کاملاً روان بماند
    val pulse = if (isScrolling) 1.0f else animPulse
    val shimmer = if (isScrolling) 0.5f else animShimmer

    val scrimBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF020617).copy(alpha = 0.18f),
                Color(0xFF020617).copy(alpha = 0.30f),
                Color(0xFF020617).copy(alpha = 0.18f),
                Color.Transparent
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (isPro) {
            drawProStarlightVaultArt(w, h, pulse, shimmer)
        } else {
            when (birdType) {
                GoldenBirdType.LAYER_HEN -> drawLayerHenPastoralFarmArt(w, h, pulse, shimmer)
                GoldenBirdType.BROILER -> drawBroilerCyberMegaFarmArt(w, h, pulse, shimmer)
                GoldenBirdType.QUAIL -> drawQuailZenGardenArt(w, h, pulse, shimmer)
                GoldenBirdType.TURKEY -> drawTurkeyAutumnValleyArt(w, h, pulse, shimmer)
                GoldenBirdType.WATERFOWL -> drawWaterfowlLotusLagoonArt(w, h, pulse, shimmer)
                GoldenBirdType.COCKATIEL -> drawCockatielRainforestCanopyArt(w, h, pulse, shimmer)
                GoldenBirdType.PARTRIDGE -> drawPartridgeZagrosCanyonArt(w, h, pulse, shimmer)
                GoldenBirdType.OSTRICH -> drawOstrichSaharaDunesArt(w, h, pulse, shimmer)
                GoldenBirdType.GUINEA_FOWL -> drawGuineaFowlSavannahConstellationArt(w, h, pulse, shimmer)
                GoldenBirdType.CANARY -> drawCanaryCitrusGardenArt(w, h, pulse, shimmer)
                GoldenBirdType.PIGEON -> drawPigeonSkySanctuaryArt(w, h, pulse, shimmer)
                GoldenBirdType.MUSCOVY_DUCK -> drawMuscovyDuckWetlandSanctuaryArt(w, h, pulse, shimmer)
            }
        }

        // لایه محافظ کنتراست و خوانایی (Center Readability Scrim) با براش کش‌شده
        drawRect(
            brush = scrimBrush,
            size = Size(w, h)
        )
    }
}

// ==============================================================================
// ۱. مرغ بومی و تخم‌گذار: مزرعه آفتابی گلستان و آسیاب بادی روستایی
// ==============================================================================
private fun DrawScope.drawLayerHenPastoralFarmArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. خورشید صبحگاهی و پرتوهای نور (God Rays)
    val sunCenter = Offset(w * 0.85f, h * 0.12f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A).copy(alpha = 0.35f * pulse),
                Color(0xFFFDE047).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = sunCenter,
            radius = 160f
        ),
        radius = 160f,
        center = sunCenter
    )
    drawCircle(
        color = Color(0xFFFEF9C3).copy(alpha = 0.85f),
        radius = 32f,
        center = sunCenter
    )

    // پرتوهای نوری لطیف
    for (i in 0..3) {
        val angle = Math.toRadians((130.0 + i * 22.0)).toFloat()
        val rayPath = Path().apply {
            moveTo(sunCenter.x, sunCenter.y)
            lineTo(sunCenter.x + cos(angle - 0.08f) * w * 1.2f, sunCenter.y + sin(angle - 0.08f) * h * 0.7f)
            lineTo(sunCenter.x + cos(angle + 0.08f) * w * 1.2f, sunCenter.y + sin(angle + 0.08f) * h * 0.7f)
            close()
        }
        drawPath(rayPath, Color(0xFFFEF08A).copy(alpha = 0.05f))
    }

    // ابرهای پنبه‌ای نرم
    drawOval(
        color = Color(0xFFE2E8F0).copy(alpha = 0.18f),
        topLeft = Offset(w * 0.12f, h * 0.08f),
        size = Size(110f, 42f)
    )
    drawOval(
        color = Color(0xFFE2E8F0).copy(alpha = 0.18f),
        topLeft = Offset(w * 0.22f, h * 0.06f),
        size = Size(85f, 46f)
    )

    // ۲. تپه‌های ماهور چندلایه‌ای با عمق بصری
    // تپه دوردست
    val hillFar = Path().apply {
        moveTo(0f, h * 0.32f)
        cubicTo(w * 0.35f, h * 0.28f, w * 0.7f, h * 0.36f, w, h * 0.30f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hillFar, Color(0xFF14532D).copy(alpha = 0.38f))

    // تپه میانی (شامل کلبه مزرعه)
    val hillMid = Path().apply {
        moveTo(0f, h * 0.52f)
        cubicTo(w * 0.4f, h * 0.44f, w * 0.65f, h * 0.56f, w, h * 0.48f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hillMid, Color(0xFF166534).copy(alpha = 0.55f))

    // تپه پیش‌زمینه (شامل آسیاب بادی)
    val hillNear = Path().apply {
        moveTo(0f, h * 0.72f)
        cubicTo(w * 0.3f, h * 0.78f, w * 0.7f, h * 0.68f, w, h * 0.75f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hillNear, Color(0xFF0F3E1E).copy(alpha = 0.75f))

    // ۳. آسیاب بادی روستایی (سمت چپ تپه)
    val millX = w * 0.16f
    val millY = h * 0.46f
    // بدنه ذوزنقه‌ای آسیاب
    val millTower = Path().apply {
        moveTo(millX - 16f, millY + 45f)
        lineTo(millX - 10f, millY)
        lineTo(millX + 10f, millY)
        lineTo(millX + 16f, millY + 45f)
        close()
    }
    drawPath(millTower, Color(0xFF78350F).copy(alpha = 0.75f))
    // سقف مخروطی
    val millRoof = Path().apply {
        moveTo(millX - 14f, millY)
        lineTo(millX, millY - 18f)
        lineTo(millX + 14f, millY)
        close()
    }
    drawPath(millRoof, Color(0xFF9A3412).copy(alpha = 0.85f))
    // ۴ پره آسیاب
    val hub = Offset(millX, millY - 3f)
    drawCircle(Color(0xFFFEF3C7), radius = 3.5f, center = hub)
    val bladeAngle = (shimmer * 360f).toDouble()
    for (b in 0..3) {
        val rad = Math.toRadians(bladeAngle + b * 90.0)
        val end = Offset(hub.x + cos(rad).toFloat() * 34f, hub.y + sin(rad).toFloat() * 34f)
        drawLine(Color(0xFFFEF3C7).copy(alpha = 0.85f), hub, end, strokeWidth = 2f)
    }

    // ۴. کلبه مزرعه روستایی (سمت راست تپه میانی)
    val barnX = w * 0.82f
    val barnY = h * 0.50f
    drawRect(Color(0xFF991B1B).copy(alpha = 0.65f), topLeft = Offset(barnX - 22f, barnY), size = Size(44f, 24f))
    val barnRoof = Path().apply {
        moveTo(barnX - 26f, barnY)
        lineTo(barnX, barnY - 14f)
        lineTo(barnX + 26f, barnY)
        close()
    }
    drawPath(barnRoof, Color(0xFF450A0A).copy(alpha = 0.85f))

    // ۵. پرچین‌های چوبی و گل‌های آفتابگردان
    for (i in 0..4) {
        val fenceX = w * 0.06f + i * 22f
        val fenceY = h * 0.75f + i * 4f
        drawLine(Color(0xFF8D6E63).copy(alpha = 0.45f), Offset(fenceX, fenceY - 16f), Offset(fenceX, fenceY + 6f), strokeWidth = 3f)
    }
    drawLine(Color(0xFF8D6E63).copy(alpha = 0.45f), Offset(w * 0.05f, h * 0.75f - 6f), Offset(w * 0.06f + 92f, h * 0.75f + 10f), strokeWidth = 2f)

    // نقاط طلایی آفتابگردان
    val flowerDots = listOf(
        Offset(w * 0.28f, h * 0.56f), Offset(w * 0.34f, h * 0.60f), Offset(w * 0.40f, h * 0.54f),
        Offset(w * 0.70f, h * 0.72f), Offset(w * 0.76f, h * 0.70f), Offset(w * 0.82f, h * 0.75f)
    )
    flowerDots.forEach { pt ->
        drawCircle(Color(0xFFFBBF24).copy(alpha = 0.65f), radius = 3.5f, center = pt)
        drawCircle(Color(0xFF78350F).copy(alpha = 0.75f), radius = 1.8f, center = pt)
    }
}

// ==============================================================================
// ۲. مرغ گوشتی صنعتی: مگافارم سایبرنتیک و تهویه تونلی پیشرفته
// ==============================================================================
private fun DrawScope.drawBroilerCyberMegaFarmArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. تیرک‌های فولادی سقف صنعتی و پرسپکتیو سالن
    val dashRafter = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
    val horizonY = h * 0.45f

    // سازه‌های سقف
    for (i in 1..6) {
        val yOffset = h * (0.05f + i * 0.06f)
        drawLine(
            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
            start = Offset(0f, yOffset),
            end = Offset(w, yOffset + 24f),
            strokeWidth = 2.5f,
            pathEffect = dashRafter
        )
    }

    // تیرک‌های زاویه‌دار خرپای سقف
    val trussLeft = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.25f, horizonY * 0.6f)
        lineTo(w * 0.25f, h)
        lineTo(0f, h)
        close()
    }
    drawPath(trussLeft, Color(0xFF2E1005).copy(alpha = 0.35f))

    val trussRight = Path().apply {
        moveTo(w, 0f)
        lineTo(w * 0.75f, horizonY * 0.6f)
        lineTo(w * 0.75f, h)
        lineTo(w, h)
        close()
    }
    drawPath(trussRight, Color(0xFF2E1005).copy(alpha = 0.35f))

    // ۲. خطوط نئونی داکت تهویه با هاله نوری درخشان
    val glowWidth = 8f * pulse
    drawLine(
        color = Color(0xFFF97316).copy(alpha = 0.35f * pulse),
        start = Offset(w * 0.22f, 0f),
        end = Offset(w * 0.35f, h),
        strokeWidth = glowWidth
    )
    drawLine(
        color = Color(0xFFFDE047).copy(alpha = 0.75f),
        start = Offset(w * 0.22f, 0f),
        end = Offset(w * 0.35f, h),
        strokeWidth = 2f
    )

    drawLine(
        color = Color(0xFFF97316).copy(alpha = 0.35f * pulse),
        start = Offset(w * 0.78f, 0f),
        end = Offset(w * 0.65f, h),
        strokeWidth = glowWidth
    )
    drawLine(
        color = Color(0xFFFDE047).copy(alpha = 0.75f),
        start = Offset(w * 0.78f, 0f),
        end = Offset(w * 0.65f, h),
        strokeWidth = 2f
    )

    // ۳. توربین‌های تهویه مگافارم (Turbine Fans)
    val fanCenter = Offset(w * 0.12f, h * 0.35f)
    drawCircle(Color(0xFF451A03).copy(alpha = 0.65f), radius = 38f, center = fanCenter)
    drawCircle(Color(0xFFF59E0B).copy(alpha = 0.45f), radius = 38f, center = fanCenter, style = Stroke(width = 2.5f))
    for (f in 0..5) {
        val fAngle = Math.toRadians((f * 60.0 + shimmer * 180.0))
        val fEnd = Offset(fanCenter.x + cos(fAngle).toFloat() * 32f, fanCenter.y + sin(fAngle).toFloat() * 32f)
        drawLine(Color(0xFFFBBF24).copy(alpha = 0.6f), fanCenter, fEnd, strokeWidth = 3f)
    }

    // ۴. کفپوش شیاردار و سنسورهای دیجیتال تله‌متری
    for (s in 1..4) {
        val sY = h * (0.68f + s * 0.07f)
        val sX = if (s % 2 == 0) w * 0.18f else w * 0.82f
        drawCircle(
            color = if (s % 2 == 0) Color(0xFF10B981).copy(alpha = 0.65f * pulse) else Color(0xFFF59E0B).copy(alpha = 0.65f * pulse),
            radius = 5.5f,
            center = Offset(sX, sY)
        )
        drawCircle(
            color = if (s % 2 == 0) Color(0xFF34D399).copy(alpha = 0.25f) else Color(0xFFFDE047).copy(alpha = 0.25f),
            radius = 12f * pulse,
            center = Offset(sX, sY)
        )
    }
}

// ==============================================================================
// ۳. بلدرچین ژاپنی: باغ صخره‌ای ذن، شکوفه‌های گیلاس و کوه مه‌آلود فوجی
// ==============================================================================
private fun DrawScope.drawQuailZenGardenArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. قله مه‌آلود فوجی (Mount Fuji) در گرگ‌ومیش بنفش
    val fujiPeak = Offset(w * 0.5f, h * 0.16f)
    val fujiBaseL = Offset(w * 0.08f, h * 0.40f)
    val fujiBaseR = Offset(w * 0.92f, h * 0.40f)

    val fujiPath = Path().apply {
        moveTo(fujiBaseL.x, fujiBaseL.y)
        cubicTo(w * 0.30f, h * 0.30f, w * 0.42f, h * 0.18f, fujiPeak.x, fujiPeak.y)
        cubicTo(w * 0.58f, h * 0.18f, w * 0.70f, h * 0.30f, fujiBaseR.x, fujiBaseR.y)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(fujiPath, Color(0xFF3B0764).copy(alpha = 0.52f))

    // کلاهک برفی قله فوجی
    val snowCap = Path().apply {
        moveTo(w * 0.40f, h * 0.22f)
        lineTo(w * 0.44f, h * 0.24f)
        lineTo(w * 0.48f, h * 0.21f)
        lineTo(fujiPeak.x, fujiPeak.y)
        lineTo(w * 0.52f, h * 0.21f)
        lineTo(w * 0.56f, h * 0.24f)
        lineTo(w * 0.60f, h * 0.22f)
        close()
    }
    drawPath(snowCap, Color(0xFFF5D0FE).copy(alpha = 0.45f))

    // ۲. قرص ماه یا خورشید گرگ‌ومیش در پس قله
    drawCircle(
        color = Color(0xFFF472B6).copy(alpha = 0.20f * pulse),
        radius = 75f,
        center = Offset(w * 0.5f, h * 0.16f)
    )

    // ۳. طاق معبد مقدس ژاپنی (Torii Gate) در سمت چپ
    val toriiX = w * 0.18f
    val toriiY = h * 0.48f
    // ستون‌های عمودی
    drawLine(Color(0xFF581C87).copy(alpha = 0.85f), Offset(toriiX - 16f, toriiY + 38f), Offset(toriiX - 14f, toriiY), strokeWidth = 4f)
    drawLine(Color(0xFF581C87).copy(alpha = 0.85f), Offset(toriiX + 16f, toriiY + 38f), Offset(toriiX + 14f, toriiY), strokeWidth = 4f)
    // تیرک‌های افقی منحنی بالا
    val toriiTop = Path().apply {
        moveTo(toriiX - 26f, toriiY - 6f)
        quadraticTo(toriiX, toriiY - 2f, toriiX + 26f, toriiY - 6f)
    }
    drawPath(toriiTop, Color(0xFF9333EA).copy(alpha = 0.95f), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
    drawLine(Color(0xFF9333EA).copy(alpha = 0.95f), Offset(toriiX - 20f, toriiY + 7f), Offset(toriiX + 20f, toriiY + 7f), strokeWidth = 3f)

    // ۴. شاخه پرپیچ‌وخم شکوفه گیلاس (Sakura Branch) در گوشه راست بالا
    val branch = Path().apply {
        moveTo(w, h * 0.04f)
        cubicTo(w * 0.85f, h * 0.08f, w * 0.78f, h * 0.14f, w * 0.65f, h * 0.18f)
    }
    drawPath(branch, Color(0xFF2E1065).copy(alpha = 0.85f), style = Stroke(width = 4f, cap = StrokeCap.Round))

    // شکوفه‌های صورتی ملایم ساکورا
    val sakuraFlowers = listOf(
        Offset(w * 0.65f, h * 0.18f), Offset(w * 0.72f, h * 0.15f), Offset(w * 0.80f, h * 0.11f),
        Offset(w * 0.86f, h * 0.16f), Offset(w * 0.92f, h * 0.08f), Offset(w * 0.68f, h * 0.22f)
    )
    sakuraFlowers.forEach { pt ->
        drawCircle(Color(0xFFF472B6).copy(alpha = 0.65f), radius = 5f, center = pt)
        drawCircle(Color(0xFFFDF2F8).copy(alpha = 0.85f), radius = 2f, center = pt)
    }

    // ۵. امواج شنی ذن (Zen Sand Ripples) در بخش پایین
    for (r in listOf(55f, 100f, 150f)) {
        drawCircle(
            color = Color(0xFFA855F7).copy(alpha = 0.16f),
            radius = r,
            center = Offset(w * 0.85f, h * 0.78f),
            style = Stroke(width = 2f)
        )
    }
}

// ==============================================================================
// ۴. بوقلمون سنگین‌وزن: دشت پاییزی بلوط زرین و غروب شکوه خزان
// ==============================================================================
private fun DrawScope.drawTurkeyAutumnValleyArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. غروب باشکوه و افق شرابی
    val sunsetCenter = Offset(w * 0.5f, h * 0.32f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF97316).copy(alpha = 0.35f * pulse),
                Color(0xFF9A3412).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = sunsetCenter,
            radius = 180f
        ),
        radius = 180f,
        center = sunsetCenter
    )

    // تپه‌های جنگلی خزان
    val ridgeFar = Path().apply {
        moveTo(0f, h * 0.38f)
        cubicTo(w * 0.35f, h * 0.32f, w * 0.65f, h * 0.44f, w, h * 0.36f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(ridgeFar, Color(0xFF451A03).copy(alpha = 0.55f))

    val ridgeMid = Path().apply {
        moveTo(0f, h * 0.58f)
        cubicTo(w * 0.45f, h * 0.50f, w * 0.75f, h * 0.64f, w, h * 0.55f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(ridgeMid, Color(0xFF7C2D12).copy(alpha = 0.65f))

    // ۲. درخت کهنسال و تنومند بلوط پاییزی در سمت راست
    val oakBase = Offset(w * 0.86f, h * 0.72f)
    // تنه تنومند
    val oakTrunk = Path().apply {
        moveTo(oakBase.x - 14f, oakBase.y + 40f)
        lineTo(oakBase.x - 7f, oakBase.y)
        lineTo(oakBase.x + 7f, oakBase.y)
        lineTo(oakBase.x + 14f, oakBase.y + 40f)
        close()
    }
    drawPath(oakTrunk, Color(0xFF270E05).copy(alpha = 0.85f))

    // شاخسار و برگ‌های باشکوه پاییزی
    val canopyColors = listOf(Color(0xFFD97706), Color(0xFFDC2626), Color(0xFFEA580C), Color(0xFFB45309))
    val canopyNodes = listOf(
        Offset(oakBase.x, oakBase.y - 28f) to 34f,
        Offset(oakBase.x - 22f, oakBase.y - 14f) to 26f,
        Offset(oakBase.x + 22f, oakBase.y - 18f) to 28f,
        Offset(oakBase.x, oakBase.y - 48f) to 22f
    )
    canopyNodes.forEachIndexed { idx, (pos, rad) ->
        drawCircle(canopyColors[idx % canopyColors.size].copy(alpha = 0.75f), radius = rad, center = pos)
    }

    // ۳. کدوهای تنبل پاییزی در کنار تپه
    drawOval(Color(0xFFEA580C).copy(alpha = 0.85f), topLeft = Offset(w * 0.16f, h * 0.70f), size = Size(24f, 18f))
    drawOval(Color(0xFFD97706).copy(alpha = 0.85f), topLeft = Offset(w * 0.22f, h * 0.72f), size = Size(18f, 14f))

    // ۴. برگ‌های معلق پاییزی در باد
    val leaves = listOf(
        Offset(w * 0.35f, h * 0.45f), Offset(w * 0.55f, h * 0.58f),
        Offset(w * 0.70f, h * 0.65f), Offset(w * 0.45f, h * 0.78f)
    )
    leaves.forEach { pt ->
        drawCircle(Color(0xFFFBBF24).copy(alpha = 0.55f), radius = 3.5f, center = pt)
    }
}

// ==============================================================================
// ۵. پرندگان آبزی (اردک و غاز): تالاب نیلوفر، برکه کریستالی و امواج آب
// ==============================================================================
private fun DrawScope.drawWaterfowlLotusLagoonArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. افق درخشان و بازتاب مهتاب بر سطح آب
    val waterHorizon = h * 0.42f
    val skyGlow = Offset(w * 0.5f, waterHorizon)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.28f * pulse),
                Color(0xFF0369A1).copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = skyGlow,
            radius = 160f
        ),
        radius = 160f,
        center = skyGlow
    )

    // ۲. امواج دوار و متحرک آب (Water Caustics)
    for (i in 1..4) {
        val waveR = i * 42f + (shimmer * 25f)
        drawOval(
            color = Color(0xFF38BDF8).copy(alpha = (0.22f - i * 0.04f).coerceAtLeast(0.06f)),
            topLeft = Offset(w * 0.5f - waveR, waterHorizon + 50f - waveR * 0.35f),
            size = Size(waveR * 2, waveR * 0.7f),
            style = Stroke(width = 1.8f)
        )
    }

    // ۳. برگ‌های شناور نیلوفر آبی (Lotus Pads)
    val padData = listOf(
        Triple(Offset(w * 0.22f, h * 0.62f), 32f, 20f),
        Triple(Offset(w * 0.78f, h * 0.55f), 28f, 18f),
        Triple(Offset(w * 0.35f, h * 0.78f), 36f, 22f),
        Triple(Offset(w * 0.82f, h * 0.76f), 30f, 19f)
    )
    padData.forEach { (pos, rx, ry) ->
        // برگ بیضوی با بریدگی ظریف نیلوفر
        drawOval(Color(0xFF0D9488).copy(alpha = 0.55f), topLeft = Offset(pos.x - rx, pos.y - ry), size = Size(rx * 2, ry * 2))
        drawOval(Color(0xFF14B8A6).copy(alpha = 0.65f), topLeft = Offset(pos.x - rx, pos.y - ry), size = Size(rx * 2, ry * 2), style = Stroke(width = 1.5f))
    }

    // شکوفه صورتی و سپید نیلوفر در سمت چپ
    val lotusCenter = Offset(w * 0.22f, h * 0.60f)
    for (p in 0..4) {
        val pAngle = Math.toRadians((p * 72.0))
        val pEnd = Offset(lotusCenter.x + cos(pAngle).toFloat() * 12f, lotusCenter.y + sin(pAngle).toFloat() * 8f)
        drawCircle(Color(0xFFFBCFE8).copy(alpha = 0.85f), radius = 6f, center = pEnd)
    }
    drawCircle(Color(0xFFFDE047), radius = 4f, center = lotusCenter)

    // ۴. نی‌ها و خیزران‌های کناره تالاب (Cattails & Reeds)
    for (c in 0..3) {
        val reedX = w * 0.06f + c * 10f
        val reedH = 75f + c * 15f
        val reedPath = Path().apply {
            moveTo(reedX, h * 0.85f)
            quadraticTo(reedX + 6f, h * 0.85f - reedH * 0.5f, reedX + 2f, h * 0.85f - reedH)
        }
        drawPath(reedPath, Color(0xFF065F46).copy(alpha = 0.75f), style = Stroke(width = 2.5f))
        // سر مخملی قهوه‌ای لویی
        drawRoundRect(
            color = Color(0xFF78350F).copy(alpha = 0.85f),
            topLeft = Offset(reedX - 2.5f, h * 0.85f - reedH),
            size = Size(6f, 22f),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }
}

// ==============================================================================
// ۶. عروس هلندی: بهشت بارانی استوایی، سایبان نخل و انوار آفتاب
// ==============================================================================
private fun DrawScope.drawCockatielRainforestCanopyArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. انوار خورشید که از لابلای سایبان استوایی می‌تابد (Sun Dappling)
    for (i in 0..3) {
        val rayStart = Offset(w * 0.05f + i * 45f, 0f)
        val rayEnd = Offset(w * 0.45f + i * 90f, h * 0.8f)
        val beamPath = Path().apply {
            moveTo(rayStart.x, rayStart.y)
            lineTo(rayEnd.x - 25f, rayEnd.y)
            lineTo(rayEnd.x + 25f, rayEnd.y)
            close()
        }
        drawPath(beamPath, Color(0xFFFDE047).copy(alpha = 0.07f * pulse))
    }

    // ۲. برگ‌های عظیم نخل استوایی و مونسترا در گوشه بالا-چپ
    val palmBase = Offset(0f, 0f)
    for (f in 0..4) {
        val fRad = Math.toRadians((15.0 + f * 18.0))
        val frondEnd = Offset(cos(fRad).toFloat() * w * 0.45f, sin(fRad).toFloat() * h * 0.35f)
        val frondPath = Path().apply {
            moveTo(palmBase.x, palmBase.y)
            quadraticTo(frondEnd.x * 0.6f, frondEnd.y * 0.4f, frondEnd.x, frondEnd.y)
        }
        drawPath(frondPath, Color(0xFF10B981).copy(alpha = 0.65f), style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawCircle(Color(0xFF34D399).copy(alpha = 0.45f), radius = 18f, center = frondEnd)
    }

    // ۳. شاخه‌های پیچ‌خورده تاک استوایی (Jungle Lianas) در کناره‌ها
    val vinePath = Path().apply {
        moveTo(w, h * 0.15f)
        cubicTo(w * 0.85f, h * 0.30f, w * 0.95f, h * 0.55f, w * 0.80f, h * 0.75f)
    }
    drawPath(vinePath, Color(0xFF047857).copy(alpha = 0.75f), style = Stroke(width = 6f))

    // شکوفه‌های ارکیده وحشی در امتداد تاک
    listOf(Offset(w * 0.88f, h * 0.35f), Offset(w * 0.84f, h * 0.60f)).forEach { pt ->
        drawCircle(Color(0xFFFBBF24).copy(alpha = 0.85f), radius = 6.5f, center = pt)
        drawCircle(Color(0xFFEF4444).copy(alpha = 0.75f), radius = 3f, center = pt)
    }

    // برگ‌های پیش‌زمینه پایین-راست
    val bottomFrond = Path().apply {
        moveTo(w, h)
        cubicTo(w * 0.75f, h * 0.90f, w * 0.70f, h * 0.80f, w * 0.65f, h * 0.75f)
    }
    drawPath(bottomFrond, Color(0xFF065F46).copy(alpha = 0.75f), style = Stroke(width = 8f, cap = StrokeCap.Round))
}

// ==============================================================================
// ۷. کبک چوکار: صخره‌های سربه فلک کشیده زاگرس و البرز آلپی
// ==============================================================================
private fun DrawScope.drawPartridgeZagrosCanyonArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. شفق سرخ‌فام غروب آلپی (Alpenglow) در افق دوردست
    val alpGlow = Offset(w * 0.5f, h * 0.22f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFEF4444).copy(alpha = 0.30f * pulse),
                Color(0xFF991B1B).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = alpGlow,
            radius = 170f
        ),
        radius = 170f,
        center = alpGlow
    )

    // ۲. دیواره‌های صخره‌ای زاویه‌دار و لایه‌لایه زاگرس
    val cliffFar = Path().apply {
        moveTo(0f, h * 0.35f)
        lineTo(w * 0.22f, h * 0.22f)
        lineTo(w * 0.45f, h * 0.34f)
        lineTo(w * 0.68f, h * 0.20f)
        lineTo(w * 0.88f, h * 0.28f)
        lineTo(w, h * 0.24f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(cliffFar, Color(0xFF1E293B).copy(alpha = 0.65f))

    // برف‌پوش قله‌های صخره‌ای دوردست
    val snowRidge = Path().apply {
        moveTo(w * 0.18f, h * 0.24f)
        lineTo(w * 0.22f, h * 0.22f)
        lineTo(w * 0.26f, h * 0.25f)
        close()
    }
    drawPath(snowRidge, Color(0xFFE2E8F0).copy(alpha = 0.55f))

    // دیواره صخره‌ای پیش‌زمینه با خطوط گسل ژئولوژیکی
    val cliffNear = Path().apply {
        moveTo(0f, h * 0.65f)
        lineTo(w * 0.38f, h * 0.52f)
        lineTo(w * 0.62f, h * 0.68f)
        lineTo(w, h * 0.58f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(cliffNear, Color(0xFF0F172A).copy(alpha = 0.85f))

    // خطوط درز و شیار سنگ‌های شیب‌دار
    val dashStrata = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
    for (s in 1..3) {
        val sy = h * (0.62f + s * 0.08f)
        drawLine(
            color = Color(0xFFEF4444).copy(alpha = 0.30f),
            start = Offset(0f, sy),
            end = Offset(w * 0.45f, sy - 20f),
            strokeWidth = 2f,
            pathEffect = dashStrata
        )
    }

    // ۳. سیلوئت پرواز عقاب در افق دوردست
    val eagleX = w * 0.72f
    val eagleY = h * 0.18f
    val eagleWing = Path().apply {
        moveTo(eagleX - 14f, eagleY - 4f)
        quadraticTo(eagleX - 7f, eagleY - 8f, eagleX, eagleY)
        quadraticTo(eagleX + 7f, eagleY - 8f, eagleX + 14f, eagleY - 4f)
    }
    drawPath(eagleWing, Color(0xFFCBD5E1).copy(alpha = 0.65f), style = Stroke(width = 2.5f))
}

// ==============================================================================
// ۸. شترمرغ آفریقایی: کویر بی‌پایان، رمل‌های زرین و خورشید سوزان
// ==============================================================================
private fun DrawScope.drawOstrichSaharaDunesArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. خورشید غول‌آسای صحرایی با امواج سراب نوری
    val saharaSun = Offset(w * 0.5f, h * 0.20f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF59E0B).copy(alpha = 0.38f * pulse),
                Color(0xFFEA580C).copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = saharaSun,
            radius = 190f
        ),
        radius = 190f,
        center = saharaSun
    )
    drawCircle(
        color = Color(0xFFFEF08A).copy(alpha = 0.85f),
        radius = 42f,
        center = saharaSun
    )

    // ۲. رمل‌های روان ماسه‌ای (Barchan Dunes)
    // رمل اول (دوردست)
    val duneFar = Path().apply {
        moveTo(0f, h * 0.45f)
        cubicTo(w * 0.35f, h * 0.38f, w * 0.65f, h * 0.52f, w, h * 0.44f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(duneFar, Color(0xFF7C2D12).copy(alpha = 0.55f))

    // رمل دوم (میانی با یال تیز بادگیر)
    val duneMid = Path().apply {
        moveTo(0f, h * 0.64f)
        quadraticTo(w * 0.42f, h * 0.52f, w, h * 0.68f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(duneMid, Color(0xFF9A3412).copy(alpha = 0.72f))

    // رمل پیش‌زمینه با سایه عمیق تیره
    val duneNear = Path().apply {
        moveTo(0f, h * 0.82f)
        cubicTo(w * 0.4f, h * 0.86f, w * 0.7f, h * 0.72f, w, h * 0.80f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(duneNear, Color(0xFF431407).copy(alpha = 0.88f))

    // ۳. نخل‌های واحه دوردست در پای رمل
    val oasisX = w * 0.82f
    val oasisY = h * 0.48f
    // تنه باریک نخل
    drawLine(Color(0xFF270E05).copy(alpha = 0.85f), Offset(oasisX, oasisY + 28f), Offset(oasisX + 4f, oasisY), strokeWidth = 3f)
    // برگ‌های چتری نخل
    for (p in 0..4) {
        val pRad = Math.toRadians((p * 45.0 + 160.0))
        val pEnd = Offset(oasisX + 4f + cos(pRad).toFloat() * 16f, oasisY + sin(pRad).toFloat() * 12f)
        drawLine(Color(0xFF15803D).copy(alpha = 0.75f), Offset(oasisX + 4f, oasisY), pEnd, strokeWidth = 2f)
    }
}

// ==============================================================================
// ۹. مرغ شاخدار گینی: ساوانای آفریقا، درخت اقاقیا چتری و کهکشان مرواریدی
// ==============================================================================
private fun DrawScope.drawGuineaFowlSavannahConstellationArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. افق گرگ‌ومیش ساوانا
    val savGlow = Offset(w * 0.5f, h * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0D9488).copy(alpha = 0.25f * pulse),
                Color(0xFF065F46).copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = savGlow,
            radius = 180f
        ),
        radius = 180f,
        center = savGlow
    )

    // ۲. صور فلکی مرواریدی در آسمان شب (Pearl Constellations)
    // الهام‌گرفته از نقاط مرواریدی پرهای شاهانه مرغ شاخدار
    val stars = listOf(
        Offset(w * 0.15f, h * 0.10f), Offset(w * 0.28f, h * 0.14f), Offset(w * 0.22f, h * 0.22f),
        Offset(w * 0.75f, h * 0.08f), Offset(w * 0.85f, h * 0.15f), Offset(w * 0.80f, h * 0.24f),
        Offset(w * 0.45f, h * 0.06f), Offset(w * 0.55f, h * 0.12f)
    )
    val dashConstellation = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    // خطوط اتصال صور فلکی
    drawLine(Color(0xFF6EE7B7).copy(alpha = 0.35f), stars[0], stars[1], strokeWidth = 1.2f, pathEffect = dashConstellation)
    drawLine(Color(0xFF6EE7B7).copy(alpha = 0.35f), stars[1], stars[2], strokeWidth = 1.2f, pathEffect = dashConstellation)
    drawLine(Color(0xFF6EE7B7).copy(alpha = 0.35f), stars[3], stars[4], strokeWidth = 1.2f, pathEffect = dashConstellation)
    drawLine(Color(0xFF6EE7B7).copy(alpha = 0.35f), stars[4], stars[5], strokeWidth = 1.2f, pathEffect = dashConstellation)

    // ستاره‌های مرواریدی درخشان
    stars.forEachIndexed { idx, pt ->
        val sPulse = if (idx % 2 == 0) pulse else (2.0f - pulse)
        drawCircle(Color(0xFFECFDF5).copy(alpha = 0.85f), radius = 3.5f * sPulse, center = pt)
        drawCircle(Color(0xFF34D399).copy(alpha = 0.30f), radius = 8f * sPulse, center = pt)
    }

    // ۳. درخت نمادین اقاقیا چتری ساوانا (Umbrella Acacia) در سمت چپ
    val acaciaBase = Offset(w * 0.22f, h * 0.62f)
    val acaciaTrunk = Path().apply {
        moveTo(acaciaBase.x - 6f, acaciaBase.y + 35f)
        lineTo(acaciaBase.x - 2f, acaciaBase.y)
        lineTo(acaciaBase.x + 2f, acaciaBase.y)
        lineTo(acaciaBase.x + 6f, acaciaBase.y + 35f)
        close()
    }
    drawPath(acaciaTrunk, Color(0xFF042F2E).copy(alpha = 0.85f))
    // تاج پهن و مسطح چتری اقاقیا
    drawOval(Color(0xFF065F46).copy(alpha = 0.85f), topLeft = Offset(acaciaBase.x - 42f, acaciaBase.y - 12f), size = Size(84f, 18f))
    drawOval(Color(0xFF042F2E).copy(alpha = 0.95f), topLeft = Offset(acaciaBase.x - 36f, acaciaBase.y - 16f), size = Size(72f, 14f))

    // ۴. تپه‌های ماهور علفزار ساوانا
    val savannahHill = Path().apply {
        moveTo(0f, h * 0.68f)
        cubicTo(w * 0.35f, h * 0.62f, w * 0.65f, h * 0.74f, w, h * 0.66f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(savannahHill, Color(0xFF0A261D).copy(alpha = 0.75f))
}

// ==============================================================================
// ۱۰. قناری: باغ مرکبات لیمو، شکوفه‌های بهاری و آوای طلایی موسیقی
// ==============================================================================
private fun DrawScope.drawCanaryCitrusGardenArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. خورشید گرم مدیترانه‌ای و پرتوهای زرین آفتاب (Sun Rays)
    val sunPos = Offset(w * 0.82f, h * 0.12f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFEF08A).copy(alpha = 0.55f * pulse),
                Color(0xFFFDE047).copy(alpha = 0.25f),
                Color(0xFFEAB308).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = sunPos,
            radius = 180f
        ),
        radius = 180f,
        center = sunPos
    )

    // پرتوهای طلایی متحرک خورشید
    for (i in 0..6) {
        val rayAngle = Math.toRadians((i * 28.0 + shimmer * 35.0 + 150.0))
        val rayEnd = Offset(
            sunPos.x + cos(rayAngle).toFloat() * (w * 0.75f),
            sunPos.y + sin(rayAngle).toFloat() * (h * 0.65f)
        )
        drawLine(
            color = Color(0xFFFEF08A).copy(alpha = 0.12f * pulse),
            start = sunPos,
            end = rayEnd,
            strokeWidth = 3f
        )
    }

    // ۲. شاخه‌های تاک و مرکبات با شکوفه‌های سفید و لیموهای درخشان
    val branchPath = Path().apply {
        moveTo(0f, h * 0.18f)
        cubicTo(w * 0.22f, h * 0.15f, w * 0.38f, h * 0.24f, w * 0.55f, h * 0.19f)
        cubicTo(w * 0.70f, h * 0.15f, w * 0.85f, h * 0.22f, w, h * 0.17f)
    }
    drawPath(branchPath, Color(0xFF292524).copy(alpha = 0.80f), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

    // لیموهای درخشان روی شاخه‌ها
    val lemons = listOf(
        Offset(w * 0.20f, h * 0.19f),
        Offset(w * 0.45f, h * 0.23f),
        Offset(w * 0.75f, h * 0.20f),
        Offset(w * 0.88f, h * 0.25f)
    )
    lemons.forEachIndexed { idx, pos ->
        val lPulse = if (idx % 2 == 0) pulse else (2.0f - pulse)
        // برگ سبز بالای لیمو
        drawOval(
            color = Color(0xFF4ADE80).copy(alpha = 0.85f),
            topLeft = Offset(pos.x - 7f, pos.y - 12f),
            size = Size(14f, 7f)
        )
        // خود لیموی زرد طلایی
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFFACC15), Color(0xFFCA8A04)),
                center = pos,
                radius = 14f * lPulse
            ),
            topLeft = Offset(pos.x - 8f * lPulse, pos.y - 3f),
            size = Size(16f * lPulse, 22f * lPulse)
        )
    }

    // شکوفه‌های سفید لیمو (پنج پر پروانه/شکوفه)
    val blossoms = listOf(
        Offset(w * 0.12f, h * 0.17f),
        Offset(w * 0.32f, h * 0.21f),
        Offset(w * 0.62f, h * 0.18f)
    )
    blossoms.forEach { bPos ->
        drawCircle(Color(0xFFFFFFFF).copy(alpha = 0.90f), radius = 4f, center = bPos)
        drawCircle(Color(0xFFFACC15).copy(alpha = 0.95f), radius = 2f, center = bPos)
    }

    // ۳. نت‌های موسیقی شناور در نسیم (Musical Notes)
    val notes = listOf(
        Offset(w * 0.18f, h * 0.38f),
        Offset(w * 0.30f, h * 0.44f),
        Offset(w * 0.78f, h * 0.35f),
        Offset(w * 0.85f, h * 0.48f)
    )
    notes.forEachIndexed { idx, pt ->
        val nOffY = pt.y + sin((shimmer * 6.28f) + idx) * 10f
        val headPt = Offset(pt.x, nOffY)
        // سر نت
        drawOval(
            color = Color(0xFFFDE047).copy(alpha = 0.75f),
            topLeft = Offset(headPt.x - 6f, headPt.y - 4f),
            size = Size(12f, 8f)
        )
        // پایه نت
        drawLine(
            color = Color(0xFFFDE047).copy(alpha = 0.75f),
            start = Offset(headPt.x + 5f, headPt.y),
            end = Offset(headPt.x + 5f, headPt.y - 18f),
            strokeWidth = 2f
        )
        // پرچم نت
        drawLine(
            color = Color(0xFFFDE047).copy(alpha = 0.60f),
            start = Offset(headPt.x + 5f, headPt.y - 18f),
            end = Offset(headPt.x + 12f, headPt.y - 12f),
            strokeWidth = 2f
        )
    }

    // ۴. تپه‌های سرسبز باغ لیمو در افق پایین
    val orchardHills = Path().apply {
        moveTo(0f, h * 0.70f)
        cubicTo(w * 0.30f, h * 0.64f, w * 0.70f, h * 0.76f, w, h * 0.68f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(orchardHills, Color(0xFF143306).copy(alpha = 0.75f))
}

// ==============================================================================
// ۱۱. کبوتر: برج کبوترخانه خشتی کهن، گنبدهای فیروزه‌ای و پرواز در اوج آسمان
// ==============================================================================
private fun DrawScope.drawPigeonSkySanctuaryArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. افق لاجوردی و کهکشان امواج مغناطیسی ناوبری (Geomagnetic Navigation Grid)
    val dashMag = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
    for (m in 0..2) {
        val yBase = h * (0.12f + m * 0.10f)
        val wavePath = Path().apply {
            moveTo(0f, yBase)
            cubicTo(w * 0.25f, yBase - 15f * pulse, w * 0.75f, yBase + 15f * pulse, w, yBase)
        }
        drawPath(wavePath, Color(0xFF38BDF8).copy(alpha = 0.22f), style = Stroke(width = 1.2f, pathEffect = dashMag))
    }

    // ۲. گنبد فیروزه‌ای کهن در دوردست (Turquoise Dome)
    val domeCenter = Offset(w * 0.25f, h * 0.58f)
    drawArc(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0F172A))
        ),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(domeCenter.x - 38f, domeCenter.y - 38f),
        size = Size(76f, 76f)
    )
    // مناره یا نوک گنبد
    drawLine(Color(0xFFFBBF24), Offset(domeCenter.x, domeCenter.y - 38f), Offset(domeCenter.x, domeCenter.y - 48f), strokeWidth = 2.5f)

    // ۳. برج مدور کبوترخانه خشتی (Adobe Pigeon Tower) در سمت راست
    val towerLeft = w * 0.70f
    val towerTop = h * 0.42f
    val towerWidth = w * 0.26f
    val towerHeight = h * 0.45f
    // بدنه خشتی برج
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF451A03), Color(0xFF78350F), Color(0xFF3B1603))
        ),
        topLeft = Offset(towerLeft, towerTop),
        size = Size(towerWidth, towerHeight),
        cornerRadius = CornerRadius(16f, 16f)
    )
    // کنگره‌های خشتی بالای برج (Crenellations)
    for (c in 0..3) {
        val cLeft = towerLeft + (c * (towerWidth / 4f)) + 3f
        drawRect(
            color = Color(0xFF9A3412),
            topLeft = Offset(cLeft, towerTop - 12f),
            size = Size(towerWidth / 6f, 12f)
        )
    }
    // طاقچه‌های لانه کبوتر در بدنه برج (Nesting Niches)
    for (r in 0..4) {
        for (col in 0..2) {
            val nX = towerLeft + 12f + col * (towerWidth / 3.4f)
            val nY = towerTop + 18f + r * 24f
            drawRoundRect(
                color = Color(0xFF1E0A02),
                topLeft = Offset(nX, nY),
                size = Size(14f, 16f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }

    // ۴. دسته کبوتران در حال پرواز اوج در آسمان (Soaring Pigeons Silhouettes)
    val birds = listOf(
        Offset(w * 0.20f, h * 0.24f),
        Offset(w * 0.35f, h * 0.18f),
        Offset(w * 0.50f, h * 0.28f),
        Offset(w * 0.62f, h * 0.14f),
        Offset(w * 0.42f, h * 0.36f)
    )
    birds.forEachIndexed { idx, pt ->
        val wingSpan = 14f + (idx % 3) * 3f
        val wingYOffset = sin((shimmer * 6.28f) + idx * 1.5f) * 3f
        val pigeonWingPath = Path().apply {
            moveTo(pt.x - wingSpan, pt.y + wingYOffset)
            quadraticTo(pt.x - wingSpan * 0.4f, pt.y - 7f, pt.x, pt.y)
            quadraticTo(pt.x + wingSpan * 0.4f, pt.y - 7f, pt.x + wingSpan, pt.y + wingYOffset)
        }
        drawPath(
            pigeonWingPath,
            color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )
    }

    // ۵. تپه شنی و افق خاکی دشت کویری
    val desertDune = Path().apply {
        moveTo(0f, h * 0.72f)
        cubicTo(w * 0.40f, h * 0.65f, w * 0.65f, h * 0.76f, w, h * 0.70f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(desertDune, Color(0xFF1C130A).copy(alpha = 0.85f))
}

// ==============================================================================
// ۱۲. بورد فوق‌حرفه‌ای و مگافارمر: تالار آسمانی، اسطرلاب زرین و کهکشان استادی
// ==============================================================================
private fun DrawScope.drawProStarlightVaultArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. سحابی کیهانی و مه درخشان کهکشانی
    val cosmicCenter = Offset(w * 0.5f, h * 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF59E0B).copy(alpha = 0.28f * pulse),
                Color(0xFF38BDF8).copy(alpha = 0.18f),
                Color(0xFF1E1B4B).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = cosmicCenter,
            radius = 240f
        ),
        radius = 240f,
        center = cosmicCenter
    )

    // ۲. حلقه‌های هندسی اسطرلاب زرین کیهانی (Celestial Astrolabe)
    val ringRadii = listOf(80f, 140f, 210f)
    val dashPro = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    ringRadii.forEachIndexed { idx, r ->
        drawCircle(
            color = if (idx == 1) Color(0xFFFBBF24).copy(alpha = 0.35f * pulse) else Color(0xFF38BDF8).copy(alpha = 0.20f),
            radius = r,
            center = cosmicCenter,
            style = Stroke(width = if (idx == 1) 2f else 1.2f, pathEffect = if (idx == 0) dashPro else null)
        )
    }

    // خطوط ناوبری قطبی اسطرلاب
    drawLine(Color(0xFFFDE047).copy(alpha = 0.25f), Offset(cosmicCenter.x - 220f, cosmicCenter.y), Offset(cosmicCenter.x + 220f, cosmicCenter.y), strokeWidth = 1f)
    drawLine(Color(0xFFFDE047).copy(alpha = 0.25f), Offset(cosmicCenter.x, cosmicCenter.y - 220f), Offset(cosmicCenter.x, cosmicCenter.y + 220f), strokeWidth = 1f)

    // ۳. ستاره‌های الماس‌گون اشرافی
    val proStars = listOf(
        Offset(w * 0.2f, h * 0.12f), Offset(w * 0.8f, h * 0.12f),
        Offset(w * 0.12f, h * 0.45f), Offset(w * 0.88f, h * 0.45f),
        Offset(w * 0.3f, h * 0.65f), Offset(w * 0.7f, h * 0.65f)
    )
    proStars.forEach { pt ->
        drawCircle(Color(0xFFFEF08A).copy(alpha = 0.85f), radius = 4f * pulse, center = pt)
        drawCircle(Color(0xFFF59E0B).copy(alpha = 0.35f), radius = 10f * pulse, center = pt)
    }

    // ۴. تاچ پرتوهای طلایی بر قله استادی
    for (b in 0..5) {
        val bAngle = Math.toRadians((b * 60.0 + shimmer * 120.0))
        val bEnd = Offset(cosmicCenter.x + cos(bAngle).toFloat() * 120f, cosmicCenter.y + sin(bAngle).toFloat() * 120f)
        drawLine(Color(0xFFFBBF24).copy(alpha = 0.45f), cosmicCenter, bEnd, strokeWidth = 1.5f)
    }
}

/**
 * مسیر سنگ‌فرش و رابط‌های اختصاصی گره‌ها بر اساس گونه پرنده
 */
@Composable
fun BirdArtisanPathConnector(
    birdType: GoldenBirdType,
    isCurrentActive: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = remember(birdType) { BirdArtTheme.getThemeForBird(birdType) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        contentAlignment = Alignment.Center
    ) {
        // ۳ قلوه‌سنگ صیقلی ملایم با تم و رنگ اختصاصی پرنده
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { idx ->
                val dotSize = if (idx == 1) 11.dp else 8.dp
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentActive) theme.primaryAccent
                            else theme.primaryAccent.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCurrentActive) Color.White.copy(alpha = 0.85f) else theme.primaryAccent.copy(alpha = 0.55f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ActiveNodeGlow(theme: BirdArtTheme, isScrolling: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "NodePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeScaleAnim"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeAlphaAnim"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = if (isScrolling) 1.05f else pulseScale
                scaleY = if (isScrolling) 1.05f else pulseScale
                alpha = if (isScrolling) 0.35f else pulseAlpha
            }
            .clip(CircleShape)
            .background(theme.nodeActiveGlow)
    )
}

/**
 * گره مرحله با قاب هنری سازگار با هویت پرنده
 */
@Composable
fun BirdArtisanStageNode(
    stageNumber: Int,
    birdType: GoldenBirdType = GoldenBirdType.LAYER_HEN,
    isUnlocked: Boolean,
    isPassed: Boolean,
    isCurrentActive: Boolean,
    stars: Int,
    theme: BirdArtTheme,
    isScrolling: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundBrush = remember(isCurrentActive, isPassed, isUnlocked, theme) {
        when {
            isCurrentActive -> Brush.radialGradient(listOf(Color.White, theme.primaryAccent))
            isPassed -> theme.nodePassedBrush
            isUnlocked -> Brush.verticalGradient(listOf(theme.headerSurfaceColor, Color(0xFF1E293B)))
            else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))
        }
    }

    val borderColor = remember(isCurrentActive, isPassed, isUnlocked, theme) {
        when {
            isCurrentActive -> Color.White
            isPassed -> Color(0xFFFBBF24)
            isUnlocked -> theme.primaryAccent
            else -> Color(0xFF64748B)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(86.dp)
    ) {
        // هاله‌ی نورانی چرخان دور مرحله فعال (فقط برای مرحله فعال اجرا می‌شود)
        if (isCurrentActive) {
            ActiveNodeGlow(theme, isScrolling)
        }

        // کادر اصلی دکمه مرحله
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (isCurrentActive) 14.dp else if (isPassed) 8.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = if (isCurrentActive) theme.primaryAccent else Color.Black
                )
                .clip(CircleShape)
                .background(brush = backgroundBrush)
                .border(
                    width = if (isCurrentActive) 3.5.dp else if (isPassed) 2.5.dp else 1.5.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(enabled = isUnlocked, onClick = onClick)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    isPassed -> {
                        Text(
                            text = stageNumber.toPersianDigits(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    isCurrentActive -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "شروع چالش",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    isUnlocked -> {
                        Text(
                            text = stageNumber.toPersianDigits(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // پلاک ستاره‌های مرحله (بزرگ، پرکنتراست و کاملاً در دید کاربر)
        if (isPassed) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFA0F172A),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (stars == 3) Color(0xFFFBBF24) else Color(0xFF64748B)
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (idx < stars) Color(0xFFFBBF24) else Color(0xFF475569),
                            modifier = Modifier
                                .size(15.dp)
                                .padding(horizontal = 0.5.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// ۱۲. اردک مسکویی: آب‌بند و زیستگاه تالابی، نیلوفر آبی و نیزارها
// ==============================================================================
private fun DrawScope.drawMuscovyDuckWetlandSanctuaryArt(w: Float, h: Float, pulse: Float, shimmer: Float) {
    // ۱. درخشش خورشید گرگ‌ومیش بر سطح برکه تالابی
    val pondCenter = Offset(w * 0.5f, h * 0.38f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2DD4BF).copy(alpha = 0.32f * pulse),
                Color(0xFF0D9488).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = pondCenter,
            radius = 180f
        ),
        radius = 180f,
        center = pondCenter
    )

    // ۲. امواج آرام و دوار آب‌بند تالاب
    for (i in 1..4) {
        val waveR = i * 48f + (shimmer * 22f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF14B8A6).copy(alpha = 0.18f), Color.Transparent),
                center = pondCenter,
                radius = waveR
            ),
            topLeft = Offset(pondCenter.x - waveR, pondCenter.y - (waveR * 0.35f)),
            size = Size(waveR * 2f, waveR * 0.7f)
        )
    }

    // ۳. برگ‌های شناور نیلوفر آبی مردابی (Water Lily Pads)
    val lilyPads = listOf(
        Triple(w * 0.18f, h * 0.44f, 26f),
        Triple(w * 0.82f, h * 0.40f, 32f),
        Triple(w * 0.14f, h * 0.62f, 22f),
        Triple(w * 0.86f, h * 0.68f, 28f),
        Triple(w * 0.24f, h * 0.80f, 24f)
    )
    lilyPads.forEach { (lx, ly, r) ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF059669).copy(alpha = 0.80f), Color(0xFF064E3B).copy(alpha = 0.90f)),
                center = Offset(lx, ly),
                radius = r
            ),
            radius = r,
            center = Offset(lx, ly)
        )
        drawLine(
            color = Color(0xFF042F2E),
            start = Offset(lx, ly),
            end = Offset(lx + r, ly - (r * 0.3f)),
            strokeWidth = 2.5f
        )
    }

    // ۴. گل‌های نیلوفر صورتی باز شده روی آب (Water Lily Flowers)
    val flowers = listOf(
        Offset(w * 0.20f, h * 0.43f),
        Offset(w * 0.80f, h * 0.39f),
        Offset(w * 0.84f, h * 0.67f)
    )
    flowers.forEach { pt ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color.Transparent),
                center = pt,
                radius = 12f
            ),
            radius = 10f * pulse,
            center = pt
        )
        drawCircle(Color(0xFFFDE047), radius = 3.5f, center = pt)
    }

    // ۵. نیزارهای وحشی و کات‌تیل‌های تالابی (Reeds and Cattails)
    val reeds = listOf(w * 0.06f, w * 0.10f, w * 0.90f, w * 0.94f)
    reeds.forEachIndexed { idx, rx ->
        val sway = (shimmer - 0.5f) * 12f * (if (idx % 2 == 0) 1 else -1)
        val reedTop = Offset(rx + sway, h * 0.52f)
        val reedBottom = Offset(rx, h * 0.88f)
        drawLine(
            color = Color(0xFF065F46).copy(alpha = 0.75f),
            start = reedBottom,
            end = reedTop,
            strokeWidth = 3f
        )
        drawRoundRect(
            color = Color(0xFF78350F).copy(alpha = 0.85f),
            topLeft = Offset(reedTop.x - 4f, reedTop.y),
            size = Size(8f, 28f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // ۶. پارتیکل‌های معلق شبنم و درخشش بیشه‌زار
    val mistParticles = listOf(
        Offset(w * 0.32f, h * 0.48f), Offset(w * 0.68f, h * 0.54f),
        Offset(w * 0.42f, h * 0.72f), Offset(w * 0.60f, h * 0.76f)
    )
    mistParticles.forEach { pt ->
        drawCircle(Color(0xFF5EEAD4).copy(alpha = 0.45f * pulse), radius = 3f, center = pt)
    }
}


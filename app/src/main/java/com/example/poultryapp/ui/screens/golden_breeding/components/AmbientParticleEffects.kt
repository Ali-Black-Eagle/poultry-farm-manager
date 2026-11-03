package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * انیمیشن ذرات معلق محیطی نرم، لطیف و بسیار بهینه در پس‌زمینه نقشه بازی
 * طراحی شده برای حفظ نرخ ۶۰ تا ۱۲۰ فریم در ثانیه بدون لگ و ری‌کامپوزیشن
 */
@Composable
fun AmbientParticleLayer(
    particleEmojis: List<String>,
    isScrolling: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (particleEmojis.isEmpty()) return

    // یک موتور انیمیشن واحد و سبک به جای چندین ترنزیشن سنگین موازی
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientParticlesMaster")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticlesMasterProgress"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxW = maxWidth
        val safeWidth = (maxW.value.toInt() - 70).coerceAtLeast(100)
        val particlesCount = 4

        for (i in 0 until particlesCount) {
            val emoji = particleEmojis[i % particleEmojis.size]
            val startX = (25 + (i * 75) % safeWidth).dp
            val phaseOffset = i.toFloat() / particlesCount

            Text(
                text = emoji,
                fontSize = (13 + (i % 2) * 2).sp,
                modifier = Modifier
                    .offset(x = startX)
                    .graphicsLayer {
                        // تمام محاسبات مستقیماً در فاز Render/Draw انجام شده و هیچ Recompositionی رخ نمی‌دهد
                        val currentP = (progress + phaseOffset) % 1f
                        translationY = currentP * 720f * density
                        translationX = kotlin.math.sin((currentP * 2 * Math.PI + i)).toFloat() * 12f * density
                        rotationZ = currentP * 180f
                        alpha = if (isScrolling) 0.18f else (0.12f + 0.28f * kotlin.math.sin(currentP * Math.PI).toFloat().coerceIn(0f, 1f))
                    }
            )
        }
    }
}

package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val rotationSpeed: Float
)

/**
 * افکت باشکوه کانفتی و آتش‌بازی ذرات جشن پیروزی با فیزیک طبیعی
 */
@Composable
fun ConfettiCelebrationCanvas(
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFBBF24), // طلایی
            Color(0xFF34D399), // زمردی
            Color(0xFF60A5FA), // آبی آسمانی
            Color(0xFFF472B6), // صورتی
            Color(0xFFA78BFA), // بنفش
            Color(0xFFFB923C)  // نارنجی
        )
        List(80) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = -0.1f - Random.nextFloat() * 0.3f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.08f,
                velocityY = 0.25f + Random.nextFloat() * 0.45f,
                color = colors.random(),
                width = 8f + Random.nextFloat() * 10f,
                height = 14f + Random.nextFloat() * 16f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { p ->
            val totalProgress = (progress + (p.initialX * 0.5f)) % 1f
            val currentY = (p.initialY + (p.velocityY * totalProgress * 1.4f)) * canvasHeight
            val currentX = (p.initialX + (p.velocityX * totalProgress)) * canvasWidth
            val currentRotation = p.rotationSpeed * totalProgress

            if (currentY in 0f..canvasHeight) {
                rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - (currentY / canvasHeight) * 0.3f).coerceIn(0.2f, 1f)),
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.width, p.height)
                    )
                }
            }
        }
    }
}

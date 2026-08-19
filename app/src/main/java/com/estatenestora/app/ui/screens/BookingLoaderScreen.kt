package com.estatenestora.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BookingLoaderScreen(
    listingTitle: String,
    addressText: String,
    onAnimationComplete: () -> Unit
) {
    var isSuccessStage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Show loader for 3 seconds
        delay(3000)
        isSuccessStage = true
        // Show success tick for 2 seconds, then transition
        delay(2000)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (!isSuccessStage) {
            // =========================================================================
            // STAGE 1: PLACING ORDER LOADER
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Stylized vector map drawn via Canvas
                    Canvas(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F4F7))
                    ) {
                        val strokeWidth = 8f
                        val roadColor = Color(0xFFE4E7EC)

                        // Draw road grid
                        drawLine(roadColor, Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.3f), strokeWidth)
                        drawLine(roadColor, Offset(0f, size.height * 0.7f), Offset(size.width, size.height * 0.7f), strokeWidth)
                        drawLine(roadColor, Offset(size.width * 0.4f, 0f), Offset(size.width * 0.4f, size.height), strokeWidth)
                        drawLine(roadColor, Offset(size.width * 0.8f, 0f), Offset(size.width * 0.8f, size.height), strokeWidth)

                        // Draw diagonal road
                        drawLine(roadColor, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth)

                        // Draw location aura
                        drawCircle(
                            color = Color(0xFFFF7A00).copy(alpha = 0.15f),
                            radius = 60f,
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }

                    // Rotating orange progress arc
                    val transition = rememberInfiniteTransition(label = "arc-rotation")
                    val rotationAngle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "angle"
                    )

                    Canvas(modifier = Modifier.size(176.dp)) {
                        drawArc(
                            color = Color(0xFFFF7A00),
                            startAngle = rotationAngle,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Central Dispatch Icon Box
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("💼", fontSize = 28.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Placing order to",
                    fontSize = 15.sp,
                    color = Color(0xFF667085),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = listingTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1A13)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = addressText.ifBlank { "Confirming location details..." },
                    fontSize = 13.sp,
                    color = Color(0xFF98A2B3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
            }
        } else {
            // =========================================================================
            // STAGE 2: CONFIRMED SUCCESS
            // =========================================================================
            // Confetti Canvas background
            ConfettiEffect()

            // Top Alert Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE6F4EA),
                    border = BorderStroke(1.dp, Color(0xFFB7E1CD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🏷️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yay! Booking confirmed instantly with Nestora Escrow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF137333)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Checkmark Circle
                val scaleTransition = rememberInfiniteTransition(label = "checkmark-scale")
                val scale by scaleTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F9D58),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size((110 * scale).dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Booking placed for",
                    fontSize = 15.sp,
                    color = Color(0xFF667085),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = listingTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1A13)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = addressText.ifBlank { "Verification complete." },
                    fontSize = 13.sp,
                    color = Color(0xFF98A2B3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ConfettiEffect() {
    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextInt(8, 16).toFloat(),
                color = when (Random.nextInt(4)) {
                    0 -> Color(0xFF4285F4) // Blue
                    1 -> Color(0xFFEA4335) // Red
                    2 -> Color(0xFFFBBC05) // Yellow
                    else -> Color(0xFF34A853) // Green
                }
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = 0.6f),
                radius = p.size / 2,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color
)

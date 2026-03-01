package com.techtrest.privamatic.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.Cream
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val RING_MAX_SWEEP = 270f
private const val SCORE_TARGET = 85
private val RING_TARGET_SWEEP = RING_MAX_SWEEP * SCORE_TARGET / 100f
private const val RING_SWEEP_DURATION_MS = 1200
private const val STEP_REVEAL_DELAY_MS = 200L
private const val STEP_REVEAL_STAGGER_MS = 280L

@Composable
fun WelcomeSlide(modifier: Modifier = Modifier) {
    val arcAnimatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        arcAnimatable.animateTo(
            targetValue = RING_TARGET_SWEEP,
            animationSpec = tween(
                durationMillis = RING_SWEEP_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        )
    }

    val arcSweep = arcAnimatable.value
    val displayedScore = (arcSweep / RING_TARGET_SWEEP * SCORE_TARGET).roundToInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 20.dp.toPx()
                val inset = strokeWidthPx / 2
                val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = Cream.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = RING_MAX_SWEEP,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                if (arcSweep > 0f) {
                    drawArc(
                        color = Cream,
                        startAngle = 135f,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayedScore.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Cream
                )
                Text(
                    text = "/ 100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cream.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Your privacy, scored.",
            style = MaterialTheme.typography.headlineMedium,
            color = Cream,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Understand exactly how private your Android is — and get a clear path to making it better.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cream.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HowItWorksSlide(modifier: Modifier = Modifier) {
    var showStep1 by remember { mutableStateOf(false) }
    var showArrow1 by remember { mutableStateOf(false) }
    var showStep2 by remember { mutableStateOf(false) }
    var showArrow2 by remember { mutableStateOf(false) }
    var showStep3 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(STEP_REVEAL_DELAY_MS)
        showStep1 = true
        delay(STEP_REVEAL_STAGGER_MS)
        showArrow1 = true
        delay(STEP_REVEAL_STAGGER_MS / 2)
        showStep2 = true
        delay(STEP_REVEAL_STAGGER_MS)
        showArrow2 = true
        delay(STEP_REVEAL_STAGGER_MS / 2)
        showStep3 = true
    }

    // Slide offset in pixels — used with graphicsLayer so layout is never affected
    val slideOffsetPx = with(LocalDensity.current) { 16.dp.toPx() }

    val alpha1 by animateFloatAsState(
        targetValue = if (showStep1) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha1"
    )
    val offset1 by animateFloatAsState(
        targetValue = if (showStep1) 0f else slideOffsetPx,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "offset1"
    )
    val alphaArrow1 by animateFloatAsState(
        targetValue = if (showArrow1) 1f else 0f,
        animationSpec = tween(200),
        label = "alphaArrow1"
    )
    val alpha2 by animateFloatAsState(
        targetValue = if (showStep2) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha2"
    )
    val offset2 by animateFloatAsState(
        targetValue = if (showStep2) 0f else slideOffsetPx,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "offset2"
    )
    val alphaArrow2 by animateFloatAsState(
        targetValue = if (showArrow2) 1f else 0f,
        animationSpec = tween(200),
        label = "alphaArrow2"
    )
    val alpha3 by animateFloatAsState(
        targetValue = if (showStep3) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha3"
    )
    val offset3 by animateFloatAsState(
        targetValue = if (showStep3) 0f else slideOffsetPx,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "offset3"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.headlineMedium,
            color = Cream,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // All children are always present so the Row's size is stable from the first frame.
        // Animations are applied via graphicsLayer and never influence layout measurement.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepItem(
                icon = Icons.Default.Search,
                label = "Scan",
                modifier = Modifier.graphicsLayer(alpha = alpha1, translationY = offset1)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Cream.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .graphicsLayer(alpha = alphaArrow1)
            )

            StepItem(
                icon = Icons.Default.Star,
                label = "Score",
                modifier = Modifier.graphicsLayer(alpha = alpha2, translationY = offset2)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Cream.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .graphicsLayer(alpha = alphaArrow2)
            )

            StepItem(
                icon = Icons.Default.CheckCircle,
                label = "Act",
                modifier = Modifier.graphicsLayer(alpha = alpha3, translationY = offset3)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "45+ checks across security, tracking, and your apps. Weighted by real-world impact.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cream.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PragmaticSlide(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Balance,
            contentDescription = null,
            tint = Cream,
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "The pragmatic approach.",
            style = MaterialTheme.typography.headlineMedium,
            color = Cream,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Privacy that fits real life. No paranoia, no impossible standards — just meaningful improvements.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cream.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Cream.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Cream,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Cream
        )
    }
}

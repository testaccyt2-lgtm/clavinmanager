package com.sirclavin.clavinmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- logo

/** The ClavinManager icon, drawn from the same shapes as clavinmanager-icon.svg. */
@Composable
fun ClavinLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 64f
        drawRoundRect(
            color = Brand.Blue,
            size = Size(64f * s, 64f * s),
            cornerRadius = CornerRadius(18f * s, 18f * s)
        )
        drawArc(
            color = Brand.White,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(17f * s, 17f * s),
            size = Size(30f * s, 30f * s),
            style = Stroke(width = 7f * s, cap = StrokeCap.Round)
        )
        drawCircle(color = Brand.Aqua, radius = 4.5f * s, center = Offset(48f * s, 32f * s))
    }
}

// ---------------------------------------------------------------- motion helpers

/** Squishy press animation + click. */
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
}

/** Fades + slides content up when it first appears. `index` staggers the start. */
@Composable
fun Appear(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(70L * index)
        progress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 40f
        }
    ) { content() }
}

@Composable
fun Modifier.softCard(radius: Dp = 22.dp): Modifier {
    val c = Clavin.colors
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(c.card)
        .border(1.dp, c.border, shape)
}

// ---------------------------------------------------------------- dialog

/** Rounded pop-up card that bounces in. */
@Composable
fun PopDialog(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Clavin.colors
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible
        )
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(160)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
                initialScale = 0.8f
            )
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = c.dialog,
                contentColor = c.ink,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

// ---------------------------------------------------------------- buttons & rows

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bouncyClick(onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(Brand.Blue),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Brand.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Clavin.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bouncyClick(onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(Brand.Blue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = c.blueText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IconBubble(icon: ImageVector, size: Dp = 60.dp, tint: Color? = null) {
    val c = Clavin.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brand.Blue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: c.blueText, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun OptionRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    val c = Clavin.colors
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick)
            .softCard(18.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, size = 44.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val c = Clavin.colors
    Text(text, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
fun DialogTitle(text: String) {
    val c = Clavin.colors
    Text(
        text,
        color = c.ink,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DialogBody(text: String) {
    val c = Clavin.colors
    Text(
        text,
        color = c.muted,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Small pill, e.g. "SOON" or "READY". */
@Composable
fun Badge(text: String, filled: Boolean = false) {
    val c = Clavin.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Brand.Aqua else Brand.Blue.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (filled) Brand.Ink else c.blueText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** The "?" bubble used for the settings hint. */
@Composable
fun QuestionBadge(size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brand.Blue),
        contentAlignment = Alignment.Center
    ) {
        Text("?", color = Brand.White, fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- progress & success

/** "1 of 3" style dots. The current step is aqua. */
@Composable
fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..total) {
            val active = i == current
            val width by animateFloatAsState(if (active) 26f else 9f, spring(0.6f, 400f), label = "dotw")
            Box(
                Modifier
                    .height(9.dp)
                    .size(width.dp, 9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Brand.Aqua else Brand.Blue.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val s by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = i * 160, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(if (i == 1) Brand.Aqua else Brand.Blue)
            )
        }
    }
}

/** Ring draws itself, then a tick is drawn, with a little aqua spark. */
@Composable
fun SuccessCheck(size: Dp = 92.dp) {
    val pop = remember { Animatable(0.6f) }
    val ring = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val spark = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        ring.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        launch { spark.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)) }
        tick.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    Canvas(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
    ) {
        val s = this.size.minDimension
        drawCircle(Brand.Blue.copy(alpha = 0.12f), radius = s / 2f)
        drawArc(
            color = Brand.Blue,
            startAngle = -90f,
            sweepAngle = 360f * ring.value,
            useCenter = false,
            topLeft = Offset(s * 0.09f, s * 0.09f),
            size = Size(s * 0.82f, s * 0.82f),
            style = Stroke(width = s * 0.07f, cap = StrokeCap.Round)
        )
        val path = Path().apply {
            moveTo(s * 0.30f, s * 0.52f)
            lineTo(s * 0.45f, s * 0.66f)
            lineTo(s * 0.72f, s * 0.36f)
        }
        val measure = PathMeasure()
        measure.setPath(path, false)
        val partial = Path()
        measure.getSegment(0f, measure.length * tick.value, partial, true)
        drawPath(
            partial,
            color = Brand.Blue,
            style = Stroke(width = s * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(Brand.Aqua, radius = s * 0.07f * spark.value, center = Offset(s * 0.86f, s * 0.16f))
    }
}

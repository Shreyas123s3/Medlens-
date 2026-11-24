package com.medlens.app.ui.components

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.medlens.app.data.Medicine
import com.medlens.app.ui.theme.MedLensEmergency
import com.medlens.app.ui.theme.MedLensNavy
import com.medlens.app.ui.theme.MedLensPaleEmergency
import com.medlens.app.ui.theme.MedLensSecondary
import com.medlens.app.ui.theme.MedLensWarning

@Composable
fun MedLensLogo(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier.semantics { contentDescription = "MedLens medical cross and magnifying glass logo" }) {
        val stroke = size.minDimension * 0.09f
        val center = Offset(size.width * 0.43f, size.height * 0.42f)
        drawCircle(primary, radius = size.minDimension * 0.31f, center = center, style = Stroke(stroke))
        drawLine(primary, Offset(center.x + size.minDimension * 0.22f, center.y + size.minDimension * 0.22f), Offset(size.width * 0.91f, size.height * 0.91f), stroke * 1.5f, cap = StrokeCap.Round)
        drawLine(secondary, Offset(center.x - size.minDimension * 0.13f, center.y), Offset(center.x + size.minDimension * 0.13f, center.y), stroke, cap = StrokeCap.Round)
        drawLine(secondary, Offset(center.x, center.y - size.minDimension * 0.13f), Offset(center.x, center.y + size.minDimension * 0.13f), stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun MedLensTopBar(onLogoClick: () -> Unit, onSettingsClick: (() -> Unit)? = null, profileInitials: String = "P") {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedLensLogo(Modifier.size(38.dp).clickable(onClick = onLogoClick))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("MedLens", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Everyday health clarity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = .12f)) {
            Text("PRIVATE", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        if (onSettingsClick != null) {
            Surface(
                modifier = Modifier.semantics { contentDescription = "Open profile and settings" }.clickable(onClick = onSettingsClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(profileInitials.take(1).uppercase(), modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onVoiceClick: (() -> Unit)? = null,
    placeholder: String = "Search medicines",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Glyph("search", "Search", tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) IconButton(onClick = { onValueChange("") }) { Glyph("close", "Clear search") }
                IconButton(onClick = { onVoiceClick?.invoke() }, enabled = onVoiceClick != null) {
                    Glyph("mic", "Voice search", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
fun FeatureCard(title: String, description: String, actionLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth().clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Glyph("check", null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(actionLabel, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MedicineCard(medicine: Medicine, saved: Boolean = false, onClick: () -> Unit) {
    MedicineCard(medicine.genericName, medicine.category, saved, onClick)
}

@Composable
fun MedicineCard(title: String, category: String, saved: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CapsuleIllustration(Modifier.size(52.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (saved) Glyph("bookmark", "Saved", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CapsuleIllustration(modifier: Modifier = Modifier) {
    val secondary = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.semantics { contentDescription = "Medicine capsule illustration" }) {
        val w = size.width * .72f
        val h = size.height * .34f
        val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        rotate(35f, pivot = center) {
            drawRoundRect(secondary, topLeft, Size(w, h), CornerRadius(h / 2f))
            drawRoundRect(primary, topLeft + Offset(w / 2f, 0f), Size(w / 2f, h), CornerRadius(h / 2f))
            drawLine(Color.White.copy(alpha = .7f), Offset(topLeft.x + w / 2f, topLeft.y + 2f), Offset(topLeft.x + w / 2f, topLeft.y + h - 2f), 2f)
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, supportingText: String? = null) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoSection(title: String, content: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(if (expanded) "−" else "+", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            }
            if (expanded) Text(content, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WarningBanner(message: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MedLensWarning.copy(alpha = .14f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("!", color = MedLensWarning, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun EmergencyCard(message: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MedLensPaleEmergency, tonalElevation = 2.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Emergency help", color = MedLensEmergency, style = MaterialTheme.typography.titleLarge)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MedLensNavy)
            Button(
                onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:112".toUri())) }
                        .onFailure { android.widget.Toast.makeText(context, "No dialer app is available. Call your local emergency number.", android.widget.Toast.LENGTH_LONG).show() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedLensEmergency),
            ) {
                Glyph("call", null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Call 112")
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, fromUser: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.fillMaxWidth(.88f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if (fromUser) 22.dp else 6.dp, bottomEnd = if (fromUser) 6.dp else 22.dp),
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            Text(message, modifier = Modifier.padding(14.dp), color = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = .25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(550, delayMillis = index * 150), RepeatMode.Reverse),
                label = "dot-$index",
            )
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha)))
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 84.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)), label = "shimmer-progress")
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(22.dp)).background(
            Brush.linearGradient(
                colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant),
                start = Offset(progress * 800f - 600f, 0f),
                end = Offset(progress * 800f, 400f),
            ),
        ),
    )
}

enum class MascotExpression { HAPPY, THINKING, CONCERNED, WAVING }

@Composable
fun Glyph(name: String, description: String?, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(24.dp).semantics { if (description != null) contentDescription = description }) {
        val sw = size.minDimension * .09f
        val c = Offset(size.width / 2f, size.height / 2f)
        when (name) {
            "search" -> {
                drawCircle(tint, size.minDimension * .27f, Offset(c.x * .85f, c.y * .85f), style = Stroke(sw))
                drawLine(tint, Offset(c.x * 1.18f, c.y * 1.18f), Offset(size.width * .86f, size.height * .86f), sw, cap = StrokeCap.Round)
            }
            "mic" -> {
                drawRoundRect(tint, Offset(size.width * .40f, size.height * .18f), Size(size.width * .20f, size.height * .42f), CornerRadius(size.width * .10f))
                drawArc(tint, 200f, 140f, false, Offset(size.width * .26f, size.height * .25f), Size(size.width * .48f, size.height * .50f), style = Stroke(sw))
                drawLine(tint, Offset(c.x, size.height * .74f), Offset(c.x, size.height * .88f), sw, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * .36f, size.height * .88f), Offset(size.width * .64f, size.height * .88f), sw, cap = StrokeCap.Round)
            }
            "close" -> {
                drawLine(tint, Offset(size.width * .28f, size.height * .28f), Offset(size.width * .72f, size.height * .72f), sw, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * .72f, size.height * .28f), Offset(size.width * .28f, size.height * .72f), sw, cap = StrokeCap.Round)
            }
            "bookmark" -> {
                val p = Path().apply { moveTo(size.width * .30f, size.height * .15f); lineTo(size.width * .70f, size.height * .15f); lineTo(size.width * .70f, size.height * .86f); lineTo(size.width * .50f, size.height * .70f); lineTo(size.width * .30f, size.height * .86f); close() }
                drawPath(p, tint, style = Stroke(sw, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            "check" -> {
                drawCircle(tint, size.minDimension * .36f, c, style = Stroke(sw))
                drawLine(tint, Offset(size.width * .30f, size.height * .51f), Offset(size.width * .45f, size.height * .66f), sw, cap = StrokeCap.Round)
                drawLine(tint, Offset(size.width * .45f, size.height * .66f), Offset(size.width * .72f, size.height * .37f), sw, cap = StrokeCap.Round)
            }
            "call" -> {
                val p = Path().apply { moveTo(size.width * .28f, size.height * .18f); quadraticTo(size.width * .22f, size.height * .22f, size.width * .25f, size.height * .33f); quadraticTo(size.width * .42f, size.height * .68f, size.width * .68f, size.height * .76f); quadraticTo(size.width * .80f, size.height * .79f, size.width * .83f, size.height * .67f); lineTo(size.width * .65f, size.height * .55f); lineTo(size.width * .54f, size.height * .65f); quadraticTo(size.width * .38f, size.height * .56f, size.width * .33f, size.height * .40f); lineTo(size.width * .43f, size.height * .31f); close() }
                drawPath(p, tint)
            }
            "home" -> {
                val p = Path().apply { moveTo(size.width * .15f, size.height * .48f); lineTo(c.x, size.height * .17f); lineTo(size.width * .85f, size.height * .48f); lineTo(size.width * .77f, size.height * .48f); lineTo(size.width * .77f, size.height * .84f); lineTo(size.width * .57f, size.height * .84f); lineTo(size.width * .57f, size.height * .62f); lineTo(size.width * .43f, size.height * .62f); lineTo(size.width * .43f, size.height * .84f); lineTo(size.width * .23f, size.height * .84f); lineTo(size.width * .23f, size.height * .48f); close() }
                drawPath(p, tint, style = Stroke(sw, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            "chat" -> {
                val p = Path().apply { moveTo(size.width * .18f, size.height * .20f); lineTo(size.width * .82f, size.height * .20f); lineTo(size.width * .82f, size.height * .68f); lineTo(size.width * .58f, size.height * .68f); lineTo(size.width * .40f, size.height * .84f); lineTo(size.width * .40f, size.height * .68f); lineTo(size.width * .18f, size.height * .68f); close() }
                drawPath(p, tint, style = Stroke(sw, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun MedLensMascot(expression: MascotExpression, modifier: Modifier = Modifier) {
    val secondary = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary
    val motion = rememberInfiniteTransition(label = "mascot-motion")
    val bob by motion.animateFloat(0f, 4f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mascot-float")
    val blink by motion.animateFloat(
        1f, .12f,
        infiniteRepeatable(keyframes { durationMillis = 4200; 1f at 0; 1f at 3000; .12f at 3050; 1f at 3110; 1f at 4200 }),
        label = "mascot-blink",
    )
    Canvas(modifier.offset { IntOffset(0, (-bob).dp.roundToPx()) }.size(112.dp).semantics { contentDescription = "MedLens robot mascot, ${expression.name.lowercase()} expression" }) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFFE8F8F6), Offset(w * .18f, h * .12f), Size(w * .64f, h * .70f), CornerRadius(w * .22f))
        drawRoundRect(secondary, Offset(w * .23f, h * .16f), Size(w * .54f, h * .58f), CornerRadius(w * .20f))
        drawRoundRect(Color.White, Offset(w * .28f, h * .19f), Size(w * .44f, h * .44f), CornerRadius(w * .16f))
        val eyeY = h * .38f
        val eyeHeight = (h * .10f * blink).coerceAtLeast(2f)
        drawRoundRect(MedLensNavy, Offset(w * .37f, eyeY - eyeHeight / 2f), Size(w * .07f, eyeHeight), CornerRadius(eyeHeight / 2f))
        drawRoundRect(MedLensNavy, Offset(w * .56f, eyeY - eyeHeight / 2f), Size(w * .07f, eyeHeight), CornerRadius(eyeHeight / 2f))
        val mouth = Path().apply {
            when (expression) {
                MascotExpression.CONCERNED -> { moveTo(w * .43f, h * .54f); quadraticTo(w * .5f, h * .49f, w * .57f, h * .54f) }
                MascotExpression.THINKING -> { moveTo(w * .44f, h * .53f); lineTo(w * .56f, h * .53f) }
                else -> { moveTo(w * .42f, h * .50f); quadraticTo(w * .5f, h * .60f, w * .58f, h * .50f) }
            }
        }
        drawPath(mouth, MedLensNavy, style = Stroke(width = w * .025f, cap = StrokeCap.Round))
        drawRoundRect(primary, Offset(w * .34f, h * .69f), Size(w * .32f, h * .12f), CornerRadius(w * .05f))
        drawLine(Color.White, Offset(w * .5f, h * .71f), Offset(w * .5f, h * .79f), w * .025f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * .46f, h * .75f), Offset(w * .54f, h * .75f), w * .025f, cap = StrokeCap.Round)
        if (expression == MascotExpression.WAVING) {
            drawLine(secondary, Offset(w * .18f, h * .49f), Offset(w * .07f, h * .37f), w * .07f, cap = StrokeCap.Round)
        }
        if (expression == MascotExpression.THINKING) {
            drawCircle(secondary, w * .035f, Offset(w * .72f, h * .13f))
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MedLensMascot(MascotExpression.THINKING, Modifier.size(112.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}


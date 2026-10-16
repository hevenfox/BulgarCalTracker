package com.bulgarcal.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.PitchBlack
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun TealButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldTeal,
            disabledContainerColor = InputGray,
            contentColor = TextWhite,
            disabledContentColor = TextSilver
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun HollowGoalButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (selected) EmeraldTeal.copy(alpha = 0.15f) else PitchBlack)
            .border(
                BorderStroke(1.5.dp, if (selected) EmeraldTeal else BorderGray),
                shape
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) EmeraldTeal else TextWhite, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MacroProgressLine(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSilver, fontSize = 13.sp)
            Text(
                "${current.toInt()} g от ${target.toInt()} g",
                color = TextWhite,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (target > 0) (current / target).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = InputGray
        )
    }
}

@Composable
fun MacroColumnBar(
    label: String,
    currentGrams: Float,
    targetGrams: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.SemiBold)) {
                    append(currentGrams.roundToInt().toString())
                }
                withStyle(SpanStyle(color = TextSilver)) {
                    append(" / ${targetGrams.roundToInt()} g")
                }
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        LinearProgressIndicator(
            progress = { if (targetGrams > 0) (currentGrams / targetGrams).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = InputGray
        )
    }
}

@Composable
fun ScreenTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextWhite)
            }
        } else Spacer(Modifier.size(48.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = TextWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        when {
            onSave != null -> IconButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldTeal)
            }
            trailing != null -> Box(Modifier.size(48.dp)) { trailing() }
            else -> Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
fun DateNavigator(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onPrev, border = BorderStroke(1.dp, BorderGray)) {
            Text("‹", color = TextWhite)
        }
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )
        OutlinedButton(onClick = onNext, border = BorderStroke(1.dp, BorderGray)) {
            Text("›", color = TextWhite)
        }
    }
}

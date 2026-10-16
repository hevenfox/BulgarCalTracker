package com.bulgarcal.tracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.theme.CardOutline
import com.bulgarcal.tracker.ui.theme.SurfaceGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import com.bulgarcal.tracker.ui.theme.TileFill
import com.bulgarcal.tracker.ui.theme.WaterBlue

private enum class DrinkKind { SMALL_CUP, CUP, BOTTLE }

private data class DrinkSize(val ml: Int, val name: String, val kind: DrinkKind)

private val drinkSizes = listOf(
    DrinkSize(200, "Чаша", DrinkKind.SMALL_CUP),
    DrinkSize(250, "Чаша", DrinkKind.CUP),
    DrinkSize(500, "Бутилка", DrinkKind.BOTTLE)
)

/** "Размер на напитка" — pick how much one tap on + adds. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterSizeSheet(
    selectedMl: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = SurfaceGray
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text("Размер на напитка", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                drinkSizes.forEach { size ->
                    DrinkOption(
                        name = size.name,
                        ml = size.ml,
                        kind = size.kind,
                        selected = size.ml == selectedMl,
                        onClick = { onSelect(size.ml) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrinkOption(
    name: String,
    ml: Int,
    kind: DrinkKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(132.dp)
            .clip(shape)
            .background(if (selected) Color(0xFF222E38) else TileFill)
            .border(if (selected) 2.dp else 1.dp, if (selected) WaterBlue else CardOutline, shape)
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(WaterBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0B1F2E), modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(58.dp), contentAlignment = Alignment.Center) {
                DrinkIcon(kind)
            }
            Spacer(Modifier.height(8.dp))
            Text(name, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("$ml ml", color = TextSilver, fontSize = 13.sp)
        }
    }
}

/** Small drawn cup / bottle so the sheet needs no image assets. */
@Composable
private fun DrinkIcon(kind: DrinkKind) {
    val (w, h) = when (kind) {
        DrinkKind.SMALL_CUP -> 34.dp to 42.dp
        DrinkKind.CUP -> 40.dp to 52.dp
        DrinkKind.BOTTLE -> 28.dp to 56.dp
    }
    val line = Color(0xFFDCE6EE)
    val water = WaterBlue.copy(alpha = 0.6f)
    Canvas(Modifier.size(w, h)) {
        val sw = 2.5.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            DrinkKind.SMALL_CUP, DrinkKind.CUP -> {
                val topY = size.height * 0.04f
                val botY = size.height * 0.96f
                val tl = size.width * 0.08f
                val tr = size.width * 0.92f
                val bl = size.width * 0.24f
                val br = size.width * 0.76f
                val level = if (kind == DrinkKind.CUP) 0.30f else 0.42f
                val t = (size.height * level - topY) / (botY - topY)
                val wl = tl + (bl - tl) * t
                val wr = tr + (br - tr) * t
                val fill = Path().apply {
                    moveTo(wl, size.height * level)
                    lineTo(wr, size.height * level)
                    lineTo(br, botY)
                    lineTo(bl, botY)
                    close()
                }
                drawPath(fill, water)
                val outline = Path().apply {
                    moveTo(tl, topY)
                    lineTo(bl, botY)
                    lineTo(br, botY)
                    lineTo(tr, topY)
                    close()
                }
                drawPath(outline, line, style = stroke)
                if (kind == DrinkKind.CUP) {
                    val cube = size.width * 0.2f
                    drawRoundRect(line.copy(alpha = 0.8f), Offset(size.width * 0.3f, size.height * 0.42f), Size(cube, cube), CornerRadius(3.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
                    drawRoundRect(line.copy(alpha = 0.8f), Offset(size.width * 0.56f, size.height * 0.58f), Size(cube, cube), CornerRadius(3.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
                }
            }
            DrinkKind.BOTTLE -> {
                val bodyL = size.width * 0.12f
                val bodyR = size.width * 0.88f
                val bodyTop = size.height * 0.30f
                val bodyBot = size.height * 0.97f
                // water
                drawRoundRect(
                    water,
                    Offset(bodyL, size.height * 0.46f),
                    Size(bodyR - bodyL, bodyBot - size.height * 0.46f),
                    CornerRadius(6.dp.toPx())
                )
                // body + neck
                drawRoundRect(line, Offset(bodyL, bodyTop), Size(bodyR - bodyL, bodyBot - bodyTop), CornerRadius(8.dp.toPx()), style = stroke)
                drawRect(line, Offset(size.width * 0.34f, size.height * 0.12f), Size(size.width * 0.32f, size.height * 0.18f), style = stroke)
                // cap
                drawRoundRect(WaterBlue, Offset(size.width * 0.30f, size.height * 0.02f), Size(size.width * 0.40f, size.height * 0.10f), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

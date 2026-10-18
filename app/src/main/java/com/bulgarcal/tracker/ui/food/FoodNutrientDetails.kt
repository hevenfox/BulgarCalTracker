package com.bulgarcal.tracker.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

/**
 * Second line of per-100 g values in the log sheet: net carbs, sugars and fiber,
 * scaled to the grams currently typed in the same way the stored log scales them.
 * Values a food does not carry (0) are left out, so nothing shows a fake zero.
 */
@Composable
fun ExtendedMacroRow(food: FoodEntity, grams: Float, modifier: Modifier = Modifier) {
    val factor = grams / food.servingGrams.coerceAtLeast(1f)
    val cells = buildList {
        add("Нетни въгл." to "${(food.carbs * factor - food.fiber * factor).toInt()} g")
        if (food.sugars > 0f) add("Захар" to "${(food.sugars * factor).toInt()} g")
        if (food.fiber > 0f) add("Фибри" to "${(food.fiber * factor).toInt()} g")
        if (food.salt > 0f) add("Сол" to "%.2f g".format(food.salt * factor))
        if (food.glycemicIndex > 0f) add("ГИ" to food.glycemicIndex.toInt().toString())
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        cells.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = TextSilver, fontSize = 11.sp)
                Text(value, color = TextWhite, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Where a food's numbers come from. The Open Food Facts attribution is required by
 * the ODbL, so it travels with the row it came from.
 */
fun sourceLabel(source: String): String = when (source.lowercase()) {
    "off" -> "Данни: Open Food Facts (ODbL 1.0)"
    "usda" -> "Данни: USDA FoodData Central (FNDDS)"
    "curated" -> "Данни: ръчно въведени стойности"
    "user" -> "Данни: въведени от теб"
    else -> ""
}

@Composable
fun FoodSourceLine(source: String, modifier: Modifier = Modifier) {
    val label = sourceLabel(source)
    if (label.isNotEmpty()) {
        Text(label, color = EmeraldTeal, fontSize = 11.sp, modifier = modifier)
    }
}

/** Formats an optional micronutrient, showing "—" when the food does not carry it. */
fun microLabel(value: Float, unit: String, decimals: Int = 0): String = when {
    value <= 0f -> "—"
    decimals <= 0 -> "${value.toInt()} $unit"
    else -> "%.${decimals}f $unit".format(value)
}

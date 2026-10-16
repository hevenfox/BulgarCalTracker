package com.bulgarcal.tracker.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.data.MealLogEntity
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.ui.components.DateNavigator
import com.bulgarcal.tracker.ui.theme.CaloriePurple
import com.bulgarcal.tracker.ui.theme.CarbsRed
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.FatOrange
import com.bulgarcal.tracker.ui.theme.ProteinBlue
import com.bulgarcal.tracker.ui.theme.SurfaceGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
fun DiaryScreen(
    dateLabel: String,
    totals: DayTotalsRow,
    logs: List<MealLogEntity>,
    favorites: List<FoodEntity>,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onAdd: () -> Unit,
    onFavoriteClick: (FoodEntity) -> Unit,
    onLogClick: (MealLogEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Дневник", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        DateNavigator(dateLabel, onPrevDay, onNextDay, Modifier.padding(vertical = 8.dp))
        NutrientRow(totals)
        if (favorites.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Любими", color = EmeraldTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            favorites.take(8).forEach { food ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onFavoriteClick(food) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.padding(end = 8.dp))
                        Text(food.name, color = TextWhite, fontSize = 14.sp)
                    }
                    Text("${food.calories} kcal", color = TextSilver, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TextWhite)
            }
            Text("(+) Добави", color = TextWhite, modifier = Modifier.align(Alignment.CenterVertically))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                LogCard(log, onClick = { onLogClick(log) })
            }
        }
    }
}

@Composable
private fun NutrientRow(t: DayTotalsRow) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        NutrientChip("Енергия", "${t.calories}", CaloriePurple)
        NutrientChip("Въглехи", "${t.carbs.toInt()}g", CarbsRed)
        NutrientChip("Протеини", "${t.protein.toInt()}g", ProteinBlue)
        NutrientChip("Мазнини", "${t.fats.toInt()}g", FatOrange)
    }
}

@Composable
private fun NutrientChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSilver, fontSize = 11.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LogCard(log: MealLogEntity, onClick: () -> Unit) {
    val mealLabel = MealType.entries.find { it.name == log.mealType }?.label ?: log.mealType
    Surface(
        color = SurfaceGray,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(log.foodName, color = TextWhite, fontWeight = FontWeight.Medium)
                Text("$mealLabel · ${log.grams.toInt()}g", color = TextSilver, fontSize = 12.sp)
            }
            Text("${log.calories} kcal", color = CaloriePurple)
        }
    }
}

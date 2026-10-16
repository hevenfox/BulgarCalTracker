package com.bulgarcal.tracker.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.domain.FoodCategories
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
fun FoodSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    foods: List<FoodEntity>,
    onCategory: (String) -> Unit,
    onRecipes: () -> Unit,
    onFavorites: () -> Unit,
    onAddProduct: () -> Unit,
    onFoodClick: (FoodEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar(
            title = "Храни",
            trailing = {
                Row {
                    Icon(Icons.Default.Star, contentDescription = null, tint = TextSilver, modifier = Modifier.padding(8.dp))
                    IconButton(onClick = onAddProduct) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldTeal)
                    }
                }
            }
        )
        SearchBar(query, onQueryChange)
        if (query.isNotBlank()) {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(foods) { food ->
                    Text(
                        "${food.name} — ${food.calories} kcal",
                        color = TextWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFoodClick(food) }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                item {
                    FoodCategoryRow("Моите Рецепти", onRecipes)
                }
                item {
                    FoodCategoryRow("Любими", onFavorites)
                }
                items(FoodCategories.drop(1)) { cat ->
                    FoodCategoryRow(cat) { onCategory(cat) }
                }
            }
        }
    }
}

@Composable
private fun FoodCategoryRow(title: String, onClick: () -> Unit) {
    Text(
        title,
        color = TextWhite,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

@Composable
fun FavoriteFoodsScreen(
    favorites: List<FoodEntity>,
    onBack: () -> Unit,
    onSelect: (FoodEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar("Любими", onBack = onBack)
        if (favorites.isEmpty()) {
            Text(
                "Няма любими храни.",
                color = TextSilver,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(Modifier.padding(16.dp)) {
                items(favorites) { food ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(food) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = EmeraldTeal,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "${food.name} (${food.calories} kcal / 100g)",
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyRecipesScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    recipes: List<FoodEntity>,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar(
            title = "Моите Рецепти",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldTeal)
                }
            }
        )
        SearchBar(query, onQueryChange)
        if (recipes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "В категория Рецепти можеш да създаваш собствени продукти използвайки бутона за добавяне.",
                    color = TextSilver,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                IconButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.height(48.dp))
                }
            }
        } else {
            LazyColumn(Modifier.padding(16.dp)) {
                items(recipes) { r ->
                    Text(r.name, color = TextWhite, modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationSheet(onDismiss: () -> Unit, onRecipe: () -> Unit, onProduct: () -> Unit) {
    val state = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(24.dp)) {
            Text("Създаване на", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            TealButton("Нова рецепта", onClick = onRecipe)
            Spacer(Modifier.height(12.dp))
            TealButton("Нов продукт", onClick = onProduct)
        }
    }
}

@Composable
fun NewProductScreen(
    onBack: () -> Unit,
    onSave: (FoodEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Моите Рецепти") }
    var calories by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var protein by remember { mutableStateOf("0") }
    var fats by remember { mutableStateOf("0") }
    var sat by remember { mutableStateOf("0") }
    var sugars by remember { mutableStateOf("0") }
    var fiber by remember { mutableStateOf("0") }
    var gi by remember { mutableStateOf("0") }
    var salt by remember { mutableStateOf("0") }
    var grams by remember { mutableStateOf("100") }

    FormScreenInsets {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenTopBar("Нов продукт", onBack = onBack, onSave = {
            onSave(
                FoodEntity(
                    name = name.ifBlank { "Продукт" },
                    category = category,
                    description = desc,
                    calories = calories.toIntOrNull() ?: 0,
                    carbs = carbs.toFloatOrNull() ?: 0f,
                    protein = protein.toFloatOrNull() ?: 0f,
                    fats = fats.toFloatOrNull() ?: 0f,
                    saturatedFats = sat.toFloatOrNull() ?: 0f,
                    sugars = sugars.toFloatOrNull() ?: 0f,
                    fiber = fiber.toFloatOrNull() ?: 0f,
                    glycemicIndex = gi.toFloatOrNull() ?: 0f,
                    salt = salt.toFloatOrNull() ?: 0f,
                    servingGrams = grams.toFloatOrNull() ?: 100f,
                    isCustom = true
                )
            )
        })
        LazyColumn {
            item { Field("Име на продукт", name) { name = it } }
            item { Field("Описание (по избор)", desc) { desc = it } }
            item { Row(Modifier.fillMaxWidth()) { Field("Категория", category, Modifier.weight(1f)) { category = it }; Field("Калории", calories, Modifier.weight(1f)) { calories = it } } }
            item { Row(Modifier.fillMaxWidth()) { Field("Мазнини", fats, Modifier.weight(1f)) { fats = it }; Field("Наситени", sat, Modifier.weight(1f)) { sat = it } } }
            item { Row(Modifier.fillMaxWidth()) { Field("Въглехидрати", carbs, Modifier.weight(1f)) { carbs = it }; Field("ГИ", gi, Modifier.weight(1f)) { gi = it } } }
            item { Row(Modifier.fillMaxWidth()) { Field("Захар", sugars, Modifier.weight(1f)) { sugars = it }; Field("Фибри", fiber, Modifier.weight(1f)) { fiber = it } } }
            item { Field("Протеини", protein) { protein = it } }
            item { Row(Modifier.fillMaxWidth()) { Field("Сол", salt, Modifier.weight(1f)) { salt = it }; Field("Грамаж", grams, Modifier.weight(1f)) { grams = it } } }
        }
    }
    }
}

@Composable
fun NewRecipeScreen(onBack: () -> Unit, onSave: (FoodEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("0") }
    FormScreenInsets {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenTopBar("Нова рецепта", onBack = onBack, onSave = {
            onSave(
                FoodEntity(
                    name = name.ifBlank { "Рецепта" },
                    category = "Моите Рецепти",
                    description = desc,
                    calories = calories.toIntOrNull() ?: 0,
                    carbs = 0f, protein = 0f, fats = 0f,
                    isCustom = true, isRecipe = true
                )
            )
        })
        Field("Име на продукт", name) { name = it }
        Field("Описание (по избор)", desc) { desc = it }
        Field("Калории", calories) { calories = it }
        TealButton("(+) Добави", onClick = { })
    }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Търсене", color = TextSilver) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = TextSilver) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InputGray,
            unfocusedContainerColor = InputGray,
            focusedBorderColor = BorderGray,
            unfocusedBorderColor = BorderGray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextSilver) },
        modifier = modifier.padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}

@Composable
fun CategoryFoodsScreen(category: String, foods: List<FoodEntity>, onBack: () -> Unit, onSelect: (FoodEntity) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar(category, onBack = onBack)
        LazyColumn(Modifier.padding(16.dp)) {
            items(foods) { food ->
                Text(
                    "${food.name} (${food.calories} kcal / 100g)",
                    color = TextWhite,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(food) }.padding(vertical = 12.dp)
                )
            }
        }
    }
}

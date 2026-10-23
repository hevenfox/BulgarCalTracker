package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
fun InfoScreen(onBack: () -> Unit) {
    FormScreenInsets {
        ScreenTopBar("Информация", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Калории BG", color = EmeraldTeal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer8()
            Text(
                "Безплатен български калориен и макро тракер. Проследявай храната си, цели за калории и напредък по тегло — всичко локално на телефона ти.",
                color = TextWhite,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer8()
            Text("Поверителност", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer8()
            Text(
                "Данните ти (профил, дневник, храни) се съхраняват само на това устройство. Не продаваме, не споделяме и не използваме личната ти информация за злонамерени цели. Няма външни акаунти или платени функции.\n\n" +
                    "При сканиране на баркод, който не е в базата, се изпраща само самият баркод към Open Food Facts — нищо друго. Камерата се използва само докато екранът за сканиране е отворен и снимки не се запазват.",
                color = TextSilver,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer8()
            Spacer8()
            Text("Данни за храните", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer8()
            Text(
                "Базата съдържа над 31 000 храни: български продукти и ястия, „Open Food Facts“ " +
                    "продукти с етикетни стойности и ястия от USDA FoodData Central. Всяка храна " +
                    "показва източника си в листа за добавяне. Над 26 000 от продуктите носят " +
                    "истинския си баркод, така че сканирането работи и без интернет; останалите " +
                    "се търсят онлайн и се запазват на телефона за следващия път.",
                color = TextSilver,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer8()
            val uriHandler = LocalUriHandler.current
            Text(
                "Съдържа данни от Open Food Facts, достъпни под Open Database License (ODbL).",
                color = EmeraldTeal,
                fontSize = 13.sp,
                modifier = Modifier.clickable { uriHandler.openUri("https://world.openfoodfacts.org") }
            )
            Text(
                "opendatacommons.org/licenses/odbl/1-0",
                color = EmeraldTeal,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://opendatacommons.org/licenses/odbl/1-0/")
                }
            )
            Text(
                "USDA FoodData Central (Survey Foods, FNDDS) — обществено достояние.",
                color = EmeraldTeal,
                fontSize = 13.sp,
                modifier = Modifier.clickable { uriHandler.openUri("https://fdc.nal.usda.gov") }
            )
            Spacer8()
            Text(
                "Версия 1.2 · 31 000+ храни в базата · Сканиране на баркод · Напомняния за хранене · Любими храни",
                color = TextSilver,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 8.dp))
}

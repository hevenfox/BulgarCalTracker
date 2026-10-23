package com.bulgarcal.tracker.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bulgarcal.tracker.TrackerViewModel
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.data.ScanResult
import com.bulgarcal.tracker.domain.Barcodes
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.food.LogFoodSheet
import com.bulgarcal.tracker.ui.food.NewProductScreen
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Where the scan flow currently is. */
private sealed interface Stage {
    data object Scanning : Stage
    data class Looking(val code: String) : Stage
    data class Found(val food: FoodEntity) : Stage
    data class Unknown(val code: String, val preview: FoodEntity?) : Stage
    data class Adding(val code: String, val preview: FoodEntity?) : Stage
    data class Offline(val code: String) : Stage
    data class Busy(val code: String) : Stage
}

/**
 * Barcode scanning, opened from the scanner icon in the food search field.
 *
 * A code is answered from the phone first and from Open Food Facts second; a product
 * found online is saved before it is shown, so it can be searched by name later and
 * the next scan of the same package works offline. A code nobody knows turns into
 * the ordinary "Нов продукт" form with the barcode attached.
 */
@Composable
fun ScannerScreen(
    vm: TrackerViewModel,
    defaultDate: LocalDate,
    initialMeal: MealType?,
    onClose: () -> Unit,
    onLogged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var stage by remember { mutableStateOf<Stage>(Stage.Scanning) }
    var torch by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(!hasCamera) }
    var manualOpen by remember { mutableStateOf(!hasCamera) }
    var manualCode by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        if (!result) manualOpen = true
    }

    LaunchedEffect(Unit) {
        if (!granted && hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun lookup(raw: String) {
        val code = Barcodes.normalize(raw)
        if (code == null) {
            manualError = "Баркодът трябва да е 8, 12, 13 или 14 цифри."
            return
        }
        manualError = null
        stage = Stage.Looking(code)
        scope.launch {
            stage = when (val result = vm.scanBarcode(code)) {
                is ScanResult.Found -> Stage.Found(result.food)
                is ScanResult.Unknown -> Stage.Unknown(result.barcode, result.preview)
                is ScanResult.Offline -> Stage.Offline(code)
                is ScanResult.Busy -> Stage.Busy(code)
                is ScanResult.Invalid -> {
                    manualError = "Това не е валиден баркод."
                    Stage.Scanning
                }
            }
        }
    }

    val looking = stage as? Stage.Looking
    val found = stage as? Stage.Found
    val adding = stage as? Stage.Adding

    if (found != null) {
        LogFoodSheet(
            food = found.food,
            defaultDate = defaultDate,
            initialMeal = initialMeal,
            onDismiss = { stage = Stage.Scanning },
            onConfirm = { grams, meal, date, favorite ->
                vm.logFood(found.food, meal, grams, date)
                vm.setFavorite(found.food.id, favorite)
                onLogged()
            }
        )
        return
    }

    if (adding != null) {
        NewProductScreen(
            onBack = { stage = Stage.Scanning },
            onSave = { food ->
                scope.launch { stage = Stage.Found(vm.saveProduct(food)) }
            },
            prefill = adding.preview,
            barcode = adding.code,
            lockBarcode = true
        )
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar(
            title = "Сканиране",
            onBack = onClose,
            trailing = {
                if (flashAvailable && granted) {
                    IconButton(onClick = { torch = !torch }) {
                        Icon(
                            if (torch) Icons.Default.Bolt else Icons.Default.FlashOff,
                            contentDescription = if (torch) "Изключи фенерчето" else "Фенерче",
                            tint = if (torch) EmeraldTeal else TextSilver
                        )
                    }
                }
            }
        )
        Box(Modifier.weight(1f).fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black)) {
            when {
                !granted -> NoCameraAccess(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
                cameraUnavailable -> ManualOnlyHint()
                else -> ScannerCamera(
                    modifier = Modifier.fillMaxSize(),
                    active = stage is Stage.Scanning && !manualOpen,
                    torch = torch,
                    onCode = { lookup(it) },
                    onCameraUnavailable = { cameraUnavailable = true; manualOpen = true },
                    onFlashAvailability = { flashAvailable = it }
                )
            }
            if (granted && !cameraUnavailable) {
                FrameOverlay()
            }
            if (stage is Stage.Looking) {
                Box(
                    Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldTeal)
                        Spacer(Modifier.height(12.dp))
                        Text("Търся ${looking?.code}…", color = TextWhite)
                    }
                }
            }
        }
        ManualEntry(
            open = manualOpen,
            code = manualCode,
            error = manualError,
            onOpenChange = { manualOpen = it },
            onCodeChange = { text ->
                manualCode = text.filter { it.isDigit() }.take(14)
                manualError = null
            },
            onSearch = { lookup(manualCode) }
        )
    }

    val unknown = stage as? Stage.Unknown
    if (unknown != null) {
        AlertDialog(
            onDismissRequest = { stage = Stage.Scanning },
            title = { Text("Баркодът не е намерен") },
            text = {
                Text(
                    "Няма данни за баркод ${unknown.code}. " +
                        "Искаш ли да го добавиш като нов продукт?"
                )
            },
            confirmButton = {
                TextButton(onClick = { stage = Stage.Adding(unknown.code, unknown.preview) }) {
                    Text("Добави")
                }
            },
            dismissButton = {
                TextButton(onClick = { stage = Stage.Scanning }) { Text("Отказ") }
            }
        )
    }

    val offline = stage as? Stage.Offline
    if (offline != null) {
        AlertDialog(
            onDismissRequest = { stage = Stage.Scanning },
            title = { Text("Няма връзка") },
            text = {
                Text(
                    "Open Food Facts не отговори. Провери интернет връзката или " +
                        "добави продукта ръчно с този баркод."
                )
            },
            confirmButton = {
                TextButton(onClick = { lookup(offline.code) }) { Text("Опитай пак") }
            },
            dismissButton = {
                TextButton(onClick = { stage = Stage.Adding(offline.code, null) }) {
                    Text("Добави ръчно")
                }
            }
        )
    }

    val busy = stage as? Stage.Busy
    if (busy != null) {
        AlertDialog(
            onDismissRequest = { stage = Stage.Scanning },
            title = { Text("Услугата е заета") },
            text = { Text("Open Food Facts помоли да се изчака. Опитай пак след минута.") },
            confirmButton = {
                TextButton(onClick = { lookup(busy.code) }) { Text("Опитай пак") }
            },
            dismissButton = {
                TextButton(onClick = { stage = Stage.Adding(busy.code, null) }) {
                    Text("Добави ръчно")
                }
            }
        )
    }
}

@Composable
private fun FrameOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.82f)
                .height(150.dp)
                .border(2.dp, EmeraldTeal.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .height(2.dp)
                    .background(EmeraldTeal.copy(alpha = 0.6f))
            )
        }
        Text(
            "Насочи камерата към баркода",
            color = TextWhite,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NoCameraAccess(onRequest: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Сканирането има нужда от камерата. Снимки не се запазват и камерата се " +
                "използва само докато този екран е отворен.",
            color = TextSilver,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))
        TealButton("Дай достъп до камерата", onClick = onRequest)
        Spacer(Modifier.height(8.dp))
        Text(
            "Отвори настройки",
            color = EmeraldTeal,
            modifier = Modifier.clickable { onSettings() }.padding(8.dp)
        )
    }
}

@Composable
private fun ManualOnlyHint() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Камерата не е достъпна на това устройство. Въведи баркода ръчно долу.",
            color = TextSilver,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ManualEntry(
    open: Boolean,
    code: String,
    error: String?,
    onOpenChange: (Boolean) -> Unit,
    onCodeChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            if (open) "Скрий ръчното въвеждане" else "Въведи баркод ръчно",
            color = EmeraldTeal,
            fontSize = 13.sp,
            modifier = Modifier
                .clickable { onOpenChange(!open) }
                .padding(vertical = 6.dp)
        )
        if (open) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text("Баркод", color = TextSilver) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputGray,
                        unfocusedContainerColor = InputGray,
                        focusedBorderColor = BorderGray,
                        unfocusedBorderColor = BorderGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onSearch) {
                    Text("Търси", color = EmeraldTeal, fontWeight = FontWeight.SemiBold)
                }
            }
            if (error != null) {
                Text(error, color = androidx.compose.ui.graphics.Color(0xFFE88A8A), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

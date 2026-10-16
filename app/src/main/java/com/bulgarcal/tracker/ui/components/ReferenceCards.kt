package com.bulgarcal.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.DarkGray
import com.bulgarcal.tracker.ui.theme.SurfaceGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = SurfaceGray,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(Locale("bg")),
            color = TextSilver,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = EmeraldTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
fun SettingsSectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(Locale("bg")),
        color = TextSilver,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        color = SurfaceGray,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (enabled) TextWhite else TextSilver.copy(alpha = 0.5f)
    val subColor = if (enabled) TextSilver else TextSilver.copy(alpha = 0.4f)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = subColor,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) TextSilver else TextSilver.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = BorderGray,
        modifier = Modifier.padding(start = 56.dp)
    )
}

@Composable
fun UserProfileCard(
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGray,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(InputGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldTeal)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Локален профил", color = TextSilver, fontSize = 13.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSilver
            )
        }
    }
}

private val homeDateLineFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale("bg"))

/**
 * Reference header: ‹ Днес ⌄ › on the left (weekday line underneath),
 * streak pill (always shown; opens Прогрес → Хранене) and profile circle on the right.
 */
@Composable
fun HomeTopBar(
    selectedDate: LocalDate,
    streak: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onStreakClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDateClick: (() -> Unit)? = null
) {
    val isToday = selectedDate == LocalDate.now()
    val centerLabel = if (isToday) "Днес" else selectedDate.format(
        DateTimeFormatter.ofPattern("d MMM", Locale("bg"))
    )
    val subLabel = selectedDate.format(homeDateLineFmt)

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹",
            color = TextSilver,
            fontSize = 22.sp,
            modifier = Modifier
                .semantics { contentDescription = "Предишен ден" }
                .clickable(onClick = onPrevDay)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (onDateClick != null) Modifier.clickable(onClick = onDateClick) else Modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(centerLabel, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(" ⌄", color = TextSilver, fontSize = 12.sp)
            }
            Text(subLabel, color = TextSilver, fontSize = 13.sp)
        }
        Text(
            "›",
            color = TextSilver,
            fontSize = 22.sp,
            modifier = Modifier
                .semantics { contentDescription = "Следващ ден" }
                .clickable(onClick = onNextDay)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        Surface(
            color = EmeraldTeal.copy(alpha = 0.14f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .semantics { contentDescription = "Серия $streak" }
                .clickable(onClick = onStreakClick)
        ) {
            Text(
                "🔥 $streak",
                color = TextWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InputGray)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Профил",
                tint = EmeraldTeal,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Reference bottom bar: 4 equal tab slots with a raised, glowing mint "+" in the middle.
 * The outer Box contains the raised button so its whole area stays tappable.
 */
@Composable
fun MainBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addShape = RoundedCornerShape(18.dp)
    Box(modifier.fillMaxWidth()) {
        Surface(
            color = DarkGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Column {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.6f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem("Начало", Icons.Default.Home, selectedTab == 0, Modifier.weight(1f)) { onTabSelected(0) }
                    BottomNavItem("Прогрес", Icons.Default.ShowChart, selectedTab == 1, Modifier.weight(1f)) { onTabSelected(1) }
                    Spacer(Modifier.width(80.dp))
                    BottomNavItem("Дневник", Icons.Default.Book, selectedTab == 2, Modifier.weight(1f)) { onTabSelected(2) }
                    BottomNavItem("Още", Icons.Default.MoreHoriz, selectedTab == 3, Modifier.weight(1f)) { onTabSelected(3) }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = addShape,
                    ambientColor = EmeraldTeal,
                    spotColor = EmeraldTeal
                )
                .clip(addShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF9BE3C4), EmeraldTeal)))
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddFood
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добави храна",
                tint = Color(0xFF0B2A1E),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) EmeraldTeal else TextSilver
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

/** Bulgarian number formatting: "1 686", "64,9", "0,3 L". Grouping is done by hand because
 *  the bg locale does not group 4-digit numbers, which the reference does ("2 251"). */
object BgFormat {
    private val bg = Locale("bg")
    private const val NBSP = '\u00A0'

    fun grouped(n: Int): String {
        val digits = abs(n).toString()
        val sb = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(NBSP)
            sb.append(c)
        }
        return if (n < 0) "-$sb" else sb.toString()
    }

    fun calories(n: Int): String = grouped(n)
    fun decimal(v: Float, digits: Int): String = String.format(bg, "%.${digits}f", v)
    fun decimalOne(v: Float): String = decimal(v, 1)
    fun decimalTwo(v: Float): String = decimal(v, 2)
    fun litersFromMl(ml: Int): String = decimalOne(ml / 1000f)
    fun kg(v: Float): String = "${decimalOne(v)} kg"
    fun percent(fraction: Float): String = "${(fraction * 100f).roundToInt()}%"
}

package com.bulgarcal.tracker.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    FormScreenInsets {
        ScreenTopBar("Обратна връзка", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Свържи се с разработчика:",
                color = TextSilver,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ContactRow("Телефон", "+359 898 552 534") {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+359898552534")))
            }
            ContactRow("Instagram", "@s_parvanov23") {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/s_parvanov23"))
                )
            }
            Text(
                "Ще се радвам на обратна връзка, идеи и доклади за проблеми.",
                color = TextSilver,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@Composable
private fun ContactRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(label, color = TextSilver, fontSize = 12.sp)
        Text(value, color = EmeraldTeal, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

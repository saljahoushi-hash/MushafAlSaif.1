package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.Tab
import com.alsaif.mushaf.audio.PlayState
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldBorder
import com.alsaif.mushaf.ui.theme.GoldDivider
import com.alsaif.mushaf.ui.theme.GoldGradient
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.Surface
import com.alsaif.mushaf.ui.theme.Surface2

@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)?, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().background(Black)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Gold) }
            } else {
                Box(Modifier.size(48.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = GoldLight, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Box(Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) { action() }
        }
        GoldDivider(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 40.dp))
    }
}

@Composable
fun BottomNav(current: Tab, onTab: (Tab) -> Unit) {
    Column(Modifier.background(Black)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            NavItem(Icons.Filled.Home, "الرئيسية", current == Tab.Home) { onTab(Tab.Home) }
            NavItem(Icons.AutoMirrored.Filled.List, "الفهرس", current == Tab.Index) { onTab(Tab.Index) }
            NavItem(Icons.Filled.Headphones, "الاستماع", current == Tab.Player) { onTab(Tab.Player) }
            NavItem(Icons.Filled.Bookmark, "المحفوظات", current == Tab.Bookmarks) { onTab(Tab.Bookmarks) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = if (selected) Gold else Muted
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = c)
        Text(label, color = c, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Box(
            Modifier.padding(top = 3.dp).size(width = 18.dp, height = 2.dp)
                .background(if (selected) Gold else Color.Transparent, RoundedCornerShape(1.dp)),
        )
    }
}

/** زر ذهبي دائري للتشغيل */
@Composable
fun GoldPlayButton(playing: Boolean, size: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(GoldGradient).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (playing) "إيقاف مؤقت" else "تشغيل",
            tint = com.alsaif.mushaf.ui.theme.OnAccent,
            modifier = Modifier.size((size * 0.46f).dp),
        )
    }
}

/** المشغّل المصغّر أسفل الشاشات */
@Composable
fun MiniPlayer(state: PlayState, data: QuranData, reciterName: String, onToggle: () -> Unit, onOpen: () -> Unit, hint: String? = null) {
    val title = if (state.surah > 0) {
        val ayah = if (state.ayah == 0) "البسملة" else "الآية ${Arabic.digits(state.ayah)}"
        "سورة ${data.surah(state.surah).name} · $ayah"
    } else "اضغط على أي آية لتشغيلها"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp)).background(Surface2)
            .border(1.dp, GoldBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GoldPlayButton(state.playing, 44, onToggle)
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 14.sp)
            Text(hint ?: reciterName, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Filled.Headphones, null, tint = Gold, modifier = Modifier.padding(end = 6.dp))
    }
}

/** بطاقة داكنة بإطار ذهبي رفيع */
fun Modifier.luxCard(radius: Int = 20): Modifier =
    this.clip(RoundedCornerShape(radius.dp))
        .background(Surface)
        .border(1.dp, Line, RoundedCornerShape(radius.dp))

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Gold, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
fun Bar(fraction: Float, height: Int = 6) {
    Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(height.dp)).background(Line)) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height.dp)
                .clip(RoundedCornerShape(height.dp)).background(GoldGradient),
        )
    }
}

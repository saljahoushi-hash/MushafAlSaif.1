package com.alsaif.mushaf.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.MushafViewModel
import com.alsaif.mushaf.Screen
import com.alsaif.mushaf.Tab
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldBorder
import com.alsaif.mushaf.ui.theme.GoldDivider
import com.alsaif.mushaf.ui.theme.GoldGradient
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.HeaderGradient
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.MushafLogo
import com.alsaif.mushaf.ui.theme.Surface
import com.alsaif.mushaf.ui.theme.Surface2
import java.time.LocalDate

@Composable
fun HomeScreen(vm: MushafViewModel, data: QuranData, open: (Screen) -> Unit, onTab: (Tab) -> Unit) {
    val last = data.firstAyahOnPage(vm.lastPage)
    // آية اليوم: تتغيّر يومياً
    val daily = remember(data) {
        val day = LocalDate.now().toEpochDay()
        data.ayahs[(day * 7919 % data.ayahs.size).toInt()]
    }

    Column(Modifier.fillMaxSize().background(Black)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // الترويسة
            Column(
                Modifier.fillMaxWidth().background(HeaderGradient)
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    MushafLogo(Modifier.size(74.dp).align(Alignment.Center))
                    Box(
                        Modifier.align(Alignment.TopStart).size(44.dp).clip(CircleShape)
                            .border(1.dp, Line, CircleShape).clickable { open(Screen.Settings) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Palette, "ألوان المصحف وحجم الخط", tint = Gold) }
                }
                Text("القرآن الكريم", fontFamily = AppFonts.logo, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = GoldLight)
                Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fontFamily = AppFonts.quran, color = Ink, fontSize = 22.sp)
                GoldDivider(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 60.dp))
                Row(
                    Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Surface2)
                        .border(1.dp, Line, RoundedCornerShape(25.dp))
                        .clickable { open(Screen.Search) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.Search, null, tint = Gold)
                    Text("ابحث عن سورة أو آية", color = Muted, fontSize = 15.sp)
                }
            }

            // متابعة القراءة
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
                    .luxCard().clickable { open(Screen.Reader(vm.lastPage)) }.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("متابعة القراءة", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("صفحة ${Arabic.digits(vm.lastPage)} من ${Arabic.digits(data.pageCount)}", color = Muted, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("سورة ${data.surah(last.surah).name}", fontFamily = AppFonts.quran, fontSize = 24.sp, color = Ink)
                        Text("الآية ${Arabic.digits(last.numberInSurah)} · الجزء ${Arabic.digits(last.juz)}", fontSize = 13.sp, color = Muted)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(GoldGradient)
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    ) { Text("متابعة", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
                Bar(vm.lastPage / data.pageCount.toFloat())
            }

            // اختصارات
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                QuickAction(Icons.AutoMirrored.Filled.List, "الفهرس") { onTab(Tab.Index) }
                QuickAction(Icons.Filled.Headphones, "الاستماع") { onTab(Tab.Player) }
                QuickAction(Icons.Filled.Bookmark, "العلامات") { onTab(Tab.Bookmarks) }
                QuickAction(Icons.AutoMirrored.Filled.MenuBook, "الأجزاء") { open(Screen.Index(1)) }
            }

            // آية اليوم
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().luxCard(24).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("✦  آية اليوم  ✦", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    daily.text, fontFamily = AppFonts.quran, fontSize = 23.sp, lineHeight = 46.sp,
                    color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                GoldDivider(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 50.dp))
                Text(
                    "سورة ${data.surah(daily.surah).name} · الآية ${Arabic.digits(daily.numberInSurah)}",
                    color = Muted, fontSize = 12.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(GoldGradient)
                            .clickable { vm.playSingle(daily.surah, daily.numberInSurah) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Black, modifier = Modifier.size(18.dp))
                        Text("استمع", color = Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, Gold, RoundedCornerShape(20.dp))
                            .clickable { open(Screen.Reader(daily.page)) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) { Text("افتح الصفحة", color = Gold, fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        BottomNav(Tab.Home, onTab)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(62.dp).clip(CircleShape).background(Surface)
                .border(1.dp, GoldBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(26.dp))
        }
        Text(label, fontSize = 13.sp, color = Ink)
    }
}

package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.MushafViewModel
import com.alsaif.mushaf.Tab
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldGradient
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.StarBadge

@Composable
fun IndexScreen(
    vm: MushafViewModel,
    data: QuranData,
    initialTab: Int,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit,
    onTab: (Tab) -> Unit,
) {
    var tab by remember { mutableIntStateOf(initialTab) }
    val play by vm.audio.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Black)) {
        TopBar("الفهرس", onBack = onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            listOf("السور", "الأجزاء", "العلامات").forEachIndexed { i, label ->
                val selected = tab == i
                Column(Modifier.weight(1f).clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label, color = if (selected) GoldLight else Muted, fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier.fillMaxWidth(0.5f).height(2.dp)
                            .background(if (selected) GoldGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(data.surahs, key = { it.number }) { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenPage(s.startPage) }.padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                StarBadge(Modifier.size(46.dp))
                                Text(Arabic.digits(s.number), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("سورة ${s.name}", fontSize = 20.sp, fontFamily = AppFonts.quran, color = Ink)
                                Text("${s.revelation} · ${Arabic.digits(s.ayahCount)} آية", fontSize = 12.sp, color = Muted)
                            }
                            Text("ص ${Arabic.digits(s.startPage)}", fontSize = 12.sp, color = Gold)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    }
                }

                1 -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(30) { i ->
                        val juz = i + 1
                        Column(
                            Modifier.luxCard(16).clickable { onOpenPage(data.juzStartPage(juz)) }.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("الجزء", fontSize = 11.sp, color = Muted)
                            Text(Arabic.digits(juz), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
                        }
                    }
                }

                else -> {
                    val pages = vm.bookmarks.sorted()
                    if (pages.isEmpty()) {
                        Text(
                            "لا توجد علامات بعد.\nاضغط رمز العلامة أعلى صفحة المصحف لحفظها هنا.",
                            color = Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(pages) { page ->
                                val a = data.firstAyahOnPage(page)
                                Row(
                                    Modifier.fillMaxWidth().luxCard(16).clickable { onOpenPage(page) }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("سورة ${data.surah(a.surah).name}", fontSize = 16.sp, color = Ink)
                                        Text("الجزء ${Arabic.digits(a.juz)}", fontSize = 12.sp, color = Muted)
                                    }
                                    Text("صفحة ${Arabic.digits(page)}", color = Gold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (play.surah > 0) {
            MiniPlayer(play, data, vm.reciter.name, onToggle = { vm.audio.toggle() }, onOpen = { onTab(Tab.Player) })
        }
        BottomNav(if (tab == 2) Tab.Bookmarks else Tab.Index, onTab)
    }
}

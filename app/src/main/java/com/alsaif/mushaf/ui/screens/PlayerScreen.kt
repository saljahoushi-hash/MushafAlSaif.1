package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.MushafViewModel
import com.alsaif.mushaf.audio.RECITERS
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldBorder
import com.alsaif.mushaf.ui.theme.GoldGradient
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.MushafLogo
import com.alsaif.mushaf.ui.theme.Surface
import com.alsaif.mushaf.ui.theme.Surface2

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 0.75f)

@Composable
fun PlayerScreen(vm: MushafViewModel, data: QuranData, onBack: () -> Unit) {
    val play by vm.audio.state.collectAsState()
    // إن لم يبدأ التشغيل بعد، نعرض السورة التي توقف عندها القارئ
    val surahNo = if (play.surah > 0) play.surah else data.firstAyahOnPage(vm.lastPage).surah
    val surah = data.surah(surahNo)
    val downloaded = remember(surahNo, vm.reciterIndex, vm.download) { vm.isSurahDownloaded(surahNo) }

    Column(Modifier.fillMaxSize().background(Black)) {
        TopBar("التلاوة", onBack = onBack) {
            val dl = vm.download
            when {
                dl != null -> Text("${(dl.first * 100) / dl.second}٪", color = Gold, fontSize = 12.sp)
                downloaded -> Icon(Icons.Filled.CheckCircle, "السورة محمّلة", tint = Gold)
                else -> IconButton(onClick = { vm.downloadSurah(surahNo) }) {
                    Icon(Icons.Filled.Download, "تحميل السورة للاستماع بدون إنترنت", tint = Gold)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // الشعار داخل هالة ذهبية
            Box(
                Modifier.size(170.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                MushafLogo(Modifier.size(130.dp))
            }
            Text("سورة ${surah.name}", fontFamily = AppFonts.quran, fontSize = 30.sp, color = GoldLight)
            val ayahLabel = when {
                play.surah == 0 -> "اضغط تشغيل للبدء"
                play.ayah == 0 -> "البسملة"
                else -> "الآية ${Arabic.digits(play.ayah)} من ${Arabic.digits(surah.ayahCount)}"
            }
            Text("${vm.reciter.name} · $ayahLabel", color = Muted, fontSize = 14.sp)
            if (vm.downloadError) Text("تعذّر التحميل، تحقق من الإنترنت", color = Gold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Bar(if (play.surah > 0) play.ayah / surah.ayahCount.toFloat() else 0f)
        }

        // أزرار التحكم
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val speedIndex = SPEEDS.indexOf(play.speed).coerceAtLeast(0)
            Box(
                Modifier.size(52.dp).clip(CircleShape).border(1.dp, Line, CircleShape)
                    .clickable { vm.audio.setSpeed(SPEEDS[(speedIndex + 1) % SPEEDS.size]) },
                contentAlignment = Alignment.Center,
            ) { Text("${play.speed}×", color = Ink, fontSize = 12.sp) }
            IconButton(onClick = { vm.audio.next() }, modifier = Modifier.padding(horizontal = 6.dp).size(56.dp)) {
                Icon(Icons.Filled.SkipNext, "الآية التالية", tint = Ink, modifier = Modifier.size(32.dp))
            }
            GoldPlayButton(play.playing, 78) {
                if (play.surah == 0) vm.playFrom(surahNo, 1) else vm.audio.toggle()
            }
            IconButton(onClick = { vm.audio.previous() }, modifier = Modifier.padding(horizontal = 6.dp).size(56.dp)) {
                Icon(Icons.Filled.SkipPrevious, "الآية السابقة", tint = Ink, modifier = Modifier.size(32.dp))
            }
            Box(Modifier.size(52.dp))
        }

        // تكرار كل آية
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("تكرار الآية:", color = Muted, fontSize = 13.sp)
            listOf(1, 3, 5, 10).forEach { n ->
                val selected = vm.repeatCount == n
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selected) Modifier.background(GoldGradient)
                            else Modifier.border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                        )
                        .clickable { vm.changeRepeat(n) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        "×${Arabic.digits(n)}", color = if (selected) Black else GoldLight, fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // اختيار القارئ
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Surface)
                .border(1.dp, GoldBorder, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .padding(top = 16.dp),
        ) {
            Text(
                "اختر القارئ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Gold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(RECITERS) { i, r ->
                    val selected = i == vm.reciterIndex
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Surface2 else Color.Transparent)
                            .clickable { vm.selectReciter(i) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .then(if (selected) Modifier.background(GoldGradient) else Modifier.border(1.dp, Line, CircleShape)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(r.name.take(1), color = if (selected) Black else GoldLight, fontWeight = FontWeight.SemiBold)
                        }
                        Text(r.name, fontSize = 15.sp, color = if (selected) GoldLight else Ink, modifier = Modifier.weight(1f))
                        if (selected) Text("مختار", color = Gold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

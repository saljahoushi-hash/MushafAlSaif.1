package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.MushafViewModel
import com.alsaif.mushaf.audio.PlayState
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.Ayah
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldBorder
import com.alsaif.mushaf.ui.theme.GoldDivider
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.PageBg

private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

@Composable
fun ReaderScreen(vm: MushafViewModel, data: QuranData, startPage: Int, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val pager = rememberPagerState(initialPage = startPage - 1) { data.pageCount }
    val play by vm.audio.state.collectAsState()
    val page = pager.currentPage + 1
    val first = data.firstAyahOnPage(page)

    // حفظ آخر صفحة مقروءة
    LaunchedEffect(page) { vm.saveLastPage(page) }

    // تتبّع الآية المتلوّة وتقليب الصفحة تلقائياً
    LaunchedEffect(play.surah, play.ayah) {
        if (play.surah > 0 && play.playing) {
            val target = data.pageOf(play.surah, play.ayah)
            if (target != pager.currentPage + 1) pager.animateScrollToPage(target - 1)
        }
    }

    Column(Modifier.fillMaxSize().background(Black)) {
        TopBar(
            title = "سورة ${data.surah(first.surah).name}",
            subtitle = "الجزء ${Arabic.digits(first.juz)} · صفحة ${Arabic.digits(page)}",
            onBack = onBack,
        ) {
            val marked = page in vm.bookmarks
            IconButton(onClick = { vm.toggleBookmark(page) }) {
                Icon(
                    if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    if (marked) "إزالة العلامة" else "حفظ علامة",
                    tint = Gold,
                )
            }
        }

        // الصفحات تُقلَّب من اليمين إلى اليسار مثل المصحف
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { index ->
            MushafPage(
                ayahs = data.byPage[index + 1].orEmpty(),
                data = data,
                pageNumber = index + 1,
                play = play,
                onAyahTap = { a -> vm.playFrom(a.surah, a.numberInSurah) },
            )
        }

        MiniPlayer(
            state = play,
            data = data,
            reciterName = vm.reciter.name,
            onToggle = { vm.togglePlay(page) },
            onOpen = onOpenPlayer,
            hint = if (play.surah == 0) vm.reciter.name else null,
        )
    }
}

@Composable
private fun MushafPage(ayahs: List<Ayah>, data: QuranData, pageNumber: Int, play: PlayState, onAyahTap: (Ayah) -> Unit) {
    // إطار مزدوج ذهبي حول الصفحة
    Box(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
            .background(PageBg, RoundedCornerShape(8.dp))
            .border(2.dp, GoldBorder, RoundedCornerShape(8.dp))
            .padding(5.dp)
            .border(0.7.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // نقسّم الصفحة إلى مقاطع: عند بداية كل سورة نضع ترويستها والبسملة
            val blocks = ArrayList<List<Ayah>>()
            var current = ArrayList<Ayah>()
            for (a in ayahs) {
                if (a.numberInSurah == 1 && current.isNotEmpty()) {
                    blocks += current; current = ArrayList()
                }
                current += a
            }
            if (current.isNotEmpty()) blocks += current

            for (block in blocks) {
                val head = block.first()
                if (head.numberInSurah == 1) {
                    SurahHeader(data.surah(head.surah).name)
                    if (head.surah != 1 && head.surah != 9) {
                        Text(BASMALA, fontFamily = AppFonts.quran, fontSize = 24.sp, color = GoldLight, textAlign = TextAlign.Center)
                    }
                }
                Text(
                    text = ayahText(block, play, onAyahTap),
                    fontFamily = AppFonts.quran,
                    fontSize = 24.sp,
                    lineHeight = 48.sp,
                    color = Ink,
                    textAlign = if (pageNumber <= 2) TextAlign.Center else TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(4.dp))
            GoldDivider(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 80.dp))
            Text(Arabic.digits(pageNumber), color = Gold, fontSize = 13.sp)
        }
    }
}

private fun ayahText(block: List<Ayah>, play: PlayState, onAyahTap: (Ayah) -> Unit): AnnotatedString =
    buildAnnotatedString {
        for (a in block) {
            val active = play.surah == a.surah && play.ayah == a.numberInSurah
            withLink(LinkAnnotation.Clickable(tag = "${a.surah}:${a.numberInSurah}") { onAyahTap(a) }) {
                withStyle(
                    SpanStyle(
                        background = if (active) Gold.copy(alpha = 0.22f) else Color.Transparent,
                        color = if (active) GoldLight else Color.Unspecified,
                    ),
                ) {
                    append(a.text)
                    withStyle(SpanStyle(color = Gold)) { append(" ﴿${Arabic.digits(a.numberInSurah)}﴾ ") }
                }
            }
        }
    }

@Composable
private fun SurahHeader(name: String) {
    Box(
        Modifier.fillMaxWidth().height(52.dp)
            .background(Black, RoundedCornerShape(26.dp))
            .border(1.5.dp, GoldBorder, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("سُورَةُ $name", fontFamily = AppFonts.quran, fontSize = 22.sp, color = GoldLight)
    }
}

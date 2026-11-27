package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.alsaif.mushaf.ui.theme.ReaderTheme
import com.alsaif.mushaf.ui.theme.isDark
import com.alsaif.mushaf.ui.theme.tajweedColor

private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/** تكبير وتصغير الخط بإصبعين بدون ما يتعارض مع التمرير */
private fun Modifier.pinchToZoom(onZoom: (Float) -> Unit, onEnd: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zoomed = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        onZoom(zoom)
                        zoomed = true
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (zoomed) onEnd()
        }
    }

@Composable
fun ReaderScreen(
    vm: MushafViewModel,
    data: QuranData,
    startPage: Int,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = startPage - 1) { data.pageCount }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = startPage - 1)
    val listPage by remember { derivedStateOf { list.firstVisibleItemIndex + 1 } }
    val play by vm.audio.state.collectAsState()
    val continuous = vm.continuous
    val page = if (continuous) listPage else pager.currentPage + 1
    val first = data.firstAyahOnPage(page)
    val theme = vm.readerTheme
    val zoom = Modifier.pinchToZoom(
        onZoom = { z -> vm.changeFontSize(vm.fontSize * z, persist = false) },
        onEnd = { vm.saveFontSize() },
    )

    // حفظ آخر صفحة مقروءة
    LaunchedEffect(page) { vm.saveLastPage(page) }

    // تتبّع الآية المتلوّة والانتقال لصفحتها تلقائياً
    LaunchedEffect(play.surah, play.ayah) {
        if (play.surah > 0 && play.playing) {
            val target = data.pageOf(play.surah, play.ayah)
            if (continuous) {
                if (list.layoutInfo.visibleItemsInfo.none { it.index == target - 1 }) list.animateScrollToItem(target - 1)
            } else if (target != pager.currentPage + 1) {
                pager.animateScrollToPage(target - 1)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Black)) {
        ReaderHeader(
            surah = "${Arabic.digits(first.surah)} سُورَةُ ${data.surah(first.surah).name}",
            juz = "الجُزْءُ ${Arabic.digits(first.juz)}",
            page = Arabic.digits(page),
            theme = theme,
            marked = page in vm.bookmarks,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onBookmark = { vm.toggleBookmark(page) },
        )

        val onTap: (Ayah) -> Unit = { a -> vm.playFrom(a.surah, a.numberInSurah) }

        if (continuous) {
            // صفحات متتالية: تمرير للأسفل وصفحة بعد صفحة
            LazyColumn(state = list, modifier = Modifier.weight(1f).background(theme.page).then(zoom)) {
                items(count = data.pageCount, key = { it }) { index ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                        PageContent(
                            ayahs = data.byPage[index + 1].orEmpty(),
                            data = data, pageNumber = index + 1, play = play, theme = theme,
                            fontSize = vm.fontSize, tajweed = vm.tajweedOn, onAyahTap = onTap,
                        )
                        PageSeparator(index + 1, theme)
                    }
                }
            }
        } else {
            // صفحة صفحة بالسحب من اليمين لليسار
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).background(theme.screen).then(zoom),
                beyondViewportPageCount = 1,
            ) { index ->
                MushafPage(
                    ayahs = data.byPage[index + 1].orEmpty(),
                    data = data, pageNumber = index + 1, play = play, theme = theme,
                    fontSize = vm.fontSize, tajweed = vm.tajweedOn, onAyahTap = onTap,
                )
            }
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

/** فاصل بين الصفحات مع رقم الصفحة */
@Composable
private fun PageSeparator(pageNumber: Int, theme: ReaderTheme) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(theme.lines.takeIf { it.alpha > 0f } ?: theme.accent.copy(alpha = 0.45f)))
        Text(pageNumber.toString(), color = theme.text, fontSize = 26.sp, fontFamily = FontFamily.Serif)
        Box(Modifier.weight(1f).height(1.dp).background(theme.lines.takeIf { it.alpha > 0f } ?: theme.accent.copy(alpha = 0.45f)))
    }
}

/** صفحة بإطار مزدوج (لوضع صفحة صفحة والمعاينة) */
@Composable
fun MushafPage(
    ayahs: List<Ayah>,
    data: QuranData,
    pageNumber: Int,
    play: PlayState,
    theme: ReaderTheme,
    fontSize: Float,
    tajweed: Boolean,
    onAyahTap: (Ayah) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(theme.page)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageContent(ayahs, data, pageNumber, play, theme, fontSize, tajweed, onAyahTap)
            Spacer(Modifier.height(10.dp))
            Text(pageNumber.toString(), color = theme.text, fontSize = 30.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** محتوى الصفحة: ترويسات السور والبسملة والآيات */
@Composable
fun PageContent(
    ayahs: List<Ayah>,
    data: QuranData,
    pageNumber: Int,
    play: PlayState,
    theme: ReaderTheme,
    fontSize: Float,
    tajweed: Boolean,
    onAyahTap: (Ayah) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                SurahHeader(data.surah(head.surah).name, theme)
                if (head.surah != 1 && head.surah != 9) {
                    Text(BASMALA, fontFamily = AppFonts.quran, fontSize = fontSize.sp, color = theme.text, textAlign = TextAlign.Center)
                }
            }
            key(head.surah, head.numberInSurah) {
                var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                Text(
                    text = ayahText(block, play, theme, tajweed, onAyahTap),
                    fontFamily = AppFonts.quran,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 2f).sp,
                    color = theme.text,
                    textAlign = if (pageNumber <= 2) TextAlign.Center else TextAlign.Justify,
                    onTextLayout = { layout = it },
                    modifier = Modifier.fillMaxWidth().drawBehind {
                        // خط رفيع تحت كل سطر مثل صفحات المصحف
                        val l = layout ?: return@drawBehind
                        if (theme.lines.alpha == 0f) return@drawBehind
                        for (i in 0 until l.lineCount) {
                            val y = l.getLineBottom(i)
                            drawLine(theme.lines, Offset(-10.dp.toPx(), y), Offset(size.width + 10.dp.toPx(), y), strokeWidth = 1.5.dp.toPx())
                        }
                    },
                )
            }
        }
    }
}

private fun ayahText(
    block: List<Ayah>,
    play: PlayState,
    theme: ReaderTheme,
    tajweed: Boolean,
    onAyahTap: (Ayah) -> Unit,
): AnnotatedString = buildAnnotatedString {
    val dark = theme.isDark
    for (a in block) {
        val active = play.surah == a.surah && play.ayah == a.numberInSurah
        withLink(LinkAnnotation.Clickable(tag = "${a.surah}:${a.numberInSurah}") { onAyahTap(a) }) {
            withStyle(SpanStyle(background = if (active) theme.highlight else Color.Transparent)) {
                if (tajweed && a.tajweed.isNotEmpty()) {
                    // تلوين أحكام التجويد
                    var pos = 0
                    for (span in a.tajweed) {
                        if (span.start < pos || span.end > a.display.length) continue
                        append(a.display.substring(pos, span.start))
                        val color = tajweedColor(span.code, dark)
                        if (color != null) {
                            withStyle(SpanStyle(color = color)) { append(a.display.substring(span.start, span.end)) }
                        } else {
                            append(a.display.substring(span.start, span.end))
                        }
                        pos = span.end
                    }
                    append(a.display.substring(pos))
                } else {
                    append(a.display)
                }
                // رقم الآية داخل دائرة (علامة نهاية الآية ۝)
                withStyle(SpanStyle(color = theme.accent)) { append(" \u06DD${Arabic.digits(a.numberInSurah)} ") }
            }
        }
    }
}

@Composable
private fun SurahHeader(name: String, theme: ReaderTheme) {
    Box(
        Modifier.fillMaxWidth().height(52.dp)
            .background(theme.header, RoundedCornerShape(26.dp))
            .border(1.5.dp, theme.headerText.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("سُورَةُ $name", fontFamily = AppFonts.quran, fontSize = 22.sp, color = theme.headerText)
    }
}

/** شريط أعلى صفحة المصحف: اسم السورة · رقم الصفحة · الجزء */
@Composable
private fun ReaderHeader(
    surah: String,
    juz: String,
    page: String,
    theme: ReaderTheme,
    marked: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onBookmark: () -> Unit,
) {
    val ink = theme.headerText
    Row(
        Modifier.fillMaxWidth().background(theme.bar).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeaderPill(surah, theme, Modifier.weight(1f))
        Text(page, color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        HeaderPill(juz, theme, Modifier.weight(1f))
        IconButton(onClick = onBookmark, modifier = Modifier.size(36.dp)) {
            Icon(
                if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                if (marked) "إزالة العلامة" else "حفظ علامة",
                tint = ink,
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Palette, "ألوان المصحف وحجم الخط", tint = ink)
        }
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.KeyboardArrowLeft, "رجوع", tint = ink, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun HeaderPill(text: String, theme: ReaderTheme, modifier: Modifier) {
    Box(
        modifier.height(40.dp)
            .background(theme.header, RoundedCornerShape(10.dp))
            .border(1.dp, theme.headerText.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = theme.headerText, fontFamily = AppFonts.quran, fontSize = 15.sp, maxLines = 1)
    }
}

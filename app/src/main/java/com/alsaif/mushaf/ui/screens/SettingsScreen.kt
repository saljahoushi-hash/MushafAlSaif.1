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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.MushafViewModel
import com.alsaif.mushaf.audio.PlayState
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.READER_THEMES
import com.alsaif.mushaf.ui.theme.ReaderTheme
import com.alsaif.mushaf.ui.theme.TAJWEED_RULES
import com.alsaif.mushaf.ui.theme.isDark
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import kotlin.math.roundToInt

private const val MIN_FONT = MushafViewModel.MIN_FONT
private const val MAX_FONT = MushafViewModel.MAX_FONT

@Composable
fun SettingsScreen(vm: MushafViewModel, data: QuranData, onBack: () -> Unit) {
    val theme = vm.readerTheme

    Column(Modifier.fillMaxSize().background(Black)) {
        TopBar("ألوان المصحف والخط", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // معاينة مباشرة
            SectionTitle("معاينة")
            Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(12.dp)).background(theme.screen)) {
                MushafPage(
                    ayahs = data.byPage[1].orEmpty(),
                    data = data,
                    pageNumber = 1,
                    play = PlayState(surah = 1, ayah = 5),
                    theme = theme,
                    fontSize = vm.fontSize,
                    tajweed = vm.tajweedOn,
                    onAyahTap = {},
                )
            }

            // اختيار الثيم
            SectionTitle("لون صفحة المصحف")
            READER_THEMES.withIndex().chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (i, t) ->
                        ThemeCard(t, selected = i == vm.readerThemeIndex, modifier = Modifier.weight(1f)) {
                            vm.selectReaderTheme(i)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // حجم الخط
            SectionTitle("حجم خط المصحف")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SizeButton("أ-", small = true) { vm.changeFontSize((vm.fontSize - 2).coerceAtLeast(MIN_FONT)) }
                Slider(
                    value = vm.fontSize,
                    onValueChange = { vm.changeFontSize(it.roundToInt().toFloat()) },
                    valueRange = MIN_FONT..MAX_FONT,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Gold,
                        inactiveTrackColor = Line,
                    ),
                    modifier = Modifier.weight(1f),
                )
                SizeButton("أ+", small = false) { vm.changeFontSize((vm.fontSize + 2).coerceAtMost(MAX_FONT)) }
            }
            Text(
                "الحجم الحالي: ${Arabic.digits(vm.fontSize.roundToInt())} — وتقدر تكبّر وتصغّر بإصبعين داخل المصحف",
                color = Muted, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // طريقة التصفح
            SectionTitle("طريقة تصفّح الصفحات")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeChip("متتالية (تمرير للأسفل)", vm.continuous, Modifier.weight(1f)) { vm.setContinuousMode(true) }
                ModeChip("صفحة صفحة (سحب جانبي)", !vm.continuous, Modifier.weight(1f)) { vm.setContinuousMode(false) }
            }

            // التجويد
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("تلوين أحكام التجويد")
                    if (data.ayahs.first().tajweed.isEmpty()) {
                        Text("يحتاج اتصال إنترنت مرة وحدة لتحميل نسخة التجويد، أعد فتح التطبيق", color = Muted, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = vm.tajweedOn,
                    onCheckedChange = { vm.setTajweed(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Black, checkedTrackColor = Gold,
                        uncheckedThumbColor = Muted, uncheckedTrackColor = Line,
                    ),
                )
            }
            if (vm.tajweedOn) {
                TAJWEED_RULES.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { r ->
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(if (theme.isDark) r.dark else r.light))
                                Text(r.name, color = Ink, fontSize = 13.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeCard(t: ReaderTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(10.dp)).background(t.page)
                .border(1.dp, t.accent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("بِسْمِ ٱللَّهِ", fontFamily = AppFonts.quran, fontSize = 20.sp, color = t.text)
            Text(
                "﴿١﴾", color = t.accent, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            )
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(Gold),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, "مختار", tint = com.alsaif.mushaf.ui.theme.OnAccent, modifier = Modifier.size(16.dp)) }
            }
        }
        Text(
            t.name, color = if (selected) GoldLight else Ink, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SizeButton(label: String, small: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).border(1.dp, Gold, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Gold, fontSize = if (small) 13.sp else 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Line, RoundedCornerShape(14.dp))
            .background(if (selected) Gold.copy(alpha = 0.12f) else Black)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) GoldLight else Ink, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

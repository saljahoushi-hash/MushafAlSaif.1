package com.alsaif.mushaf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alsaif.mushaf.data.Arabic
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.Ink
import com.alsaif.mushaf.ui.theme.Line
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.Surface

@Composable
fun SearchScreen(data: QuranData, onBack: () -> Unit, onOpenPage: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = Arabic.normalize(query).trim()
    val surahs = remember(q) { if (q.isEmpty()) emptyList() else data.surahs.filter { Arabic.normalize(it.name).contains(q) } }
    val ayahs = remember(q) { data.search(query) }

    Column(Modifier.fillMaxSize().background(Black)) {
        TopBar("البحث", onBack = onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("اكتب اسم سورة أو كلمات من آية", color = Muted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Gold) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Line,
                cursorColor = Gold,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
            if (surahs.isNotEmpty()) {
                item { SectionTitle("السور") }
                items(surahs) { s ->
                    Text(
                        "سورة ${s.name} · صفحة ${Arabic.digits(s.startPage)}",
                        color = Ink, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPage(s.startPage) }.padding(vertical = 12.dp),
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                }
            }
            if (ayahs.isNotEmpty()) {
                item { SectionTitle("الآيات (${Arabic.digits(ayahs.size)})") }
                items(ayahs) { a ->
                    Column(Modifier.fillMaxWidth().clickable { onOpenPage(a.page) }.padding(vertical = 10.dp)) {
                        Text(
                            a.text, fontFamily = AppFonts.quran, fontSize = 19.sp, lineHeight = 36.sp, color = Ink,
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "سورة ${data.surah(a.surah).name} · الآية ${Arabic.digits(a.numberInSurah)} · صفحة ${Arabic.digits(a.page)}",
                            color = Gold, fontSize = 12.sp,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                }
            }
            if (q.length >= 2 && surahs.isEmpty() && ayahs.isEmpty()) {
                item { Text("لا توجد نتائج", color = Muted, modifier = Modifier.padding(24.dp)) }
            }
        }
    }
}

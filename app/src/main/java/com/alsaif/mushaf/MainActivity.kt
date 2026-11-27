package com.alsaif.mushaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alsaif.mushaf.ui.screens.HomeScreen
import com.alsaif.mushaf.ui.screens.IndexScreen
import com.alsaif.mushaf.ui.screens.PlayerScreen
import com.alsaif.mushaf.ui.screens.ReaderScreen
import com.alsaif.mushaf.ui.screens.SearchScreen
import com.alsaif.mushaf.ui.screens.SettingsScreen
import com.alsaif.mushaf.ui.theme.AppFonts
import com.alsaif.mushaf.ui.theme.Black
import com.alsaif.mushaf.ui.theme.Gold
import com.alsaif.mushaf.ui.theme.GoldLight
import com.alsaif.mushaf.ui.theme.Muted
import com.alsaif.mushaf.ui.theme.MushafLogo
import com.alsaif.mushaf.ui.theme.MushafTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppFonts.init(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MushafTheme { App() }
            }
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Index(val tab: Int = 0) : Screen
    data class Reader(val page: Int) : Screen
    data object Player : Screen
    data object Search : Screen
    data object Settings : Screen
}

enum class Tab { Home, Index, Player, Bookmarks }

@Composable
fun App(vm: MushafViewModel = viewModel()) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    fun open(s: Screen) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun switchTab(t: Tab) {
        stack.clear()
        stack.add(Screen.Home)
        when (t) {
            Tab.Home -> Unit
            Tab.Index -> stack.add(Screen.Index(0))
            Tab.Player -> stack.add(Screen.Player)
            Tab.Bookmarks -> stack.add(Screen.Index(2))
        }
    }
    BackHandler(enabled = stack.size > 1) { back() }

    Box(Modifier.fillMaxSize().background(Black).systemBarsPadding()) {
        when (val st = vm.loadState) {
            is LoadState.Loading -> Splash(null) {}
            is LoadState.Error -> Splash(st.message) { vm.load() }
            is LoadState.Ready -> when (val s = stack.last()) {
                Screen.Home -> HomeScreen(vm, st.data, ::open, ::switchTab)
                is Screen.Index -> IndexScreen(vm, st.data, s.tab, onBack = ::back, onOpenPage = { open(Screen.Reader(it)) }, onTab = ::switchTab)
                is Screen.Reader -> ReaderScreen(vm, st.data, s.page, onBack = ::back, onOpenPlayer = { open(Screen.Player) }, onOpenSettings = { open(Screen.Settings) })
                Screen.Player -> PlayerScreen(vm, st.data, onBack = ::back)
                Screen.Settings -> SettingsScreen(vm, st.data, onBack = ::back)
                Screen.Search -> SearchScreen(st.data, onBack = ::back, onOpenPage = { open(Screen.Reader(it)) })
            }
        }
    }
}

@Composable
private fun Splash(error: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Black).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MushafLogo(Modifier.size(140.dp))
        Spacer(Modifier.height(20.dp))
        Text("القرآن الكريم", fontFamily = AppFonts.logo, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = GoldLight)
        Spacer(Modifier.height(28.dp))
        if (error == null) {
            CircularProgressIndicator(color = Gold)
            Spacer(Modifier.height(14.dp))
            Text("جاري تجهيز المصحف…\nفي أول تشغيل يتم تحميل النص مرة واحدة", color = Muted, textAlign = TextAlign.Center)
        } else {
            Text("تعذّر تحميل المصحف. تأكد من الاتصال بالإنترنت في أول تشغيل.", color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = com.alsaif.mushaf.ui.theme.OnAccent)) {
                Text("إعادة المحاولة")
            }
        }
    }
}

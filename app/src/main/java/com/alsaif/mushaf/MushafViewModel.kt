package com.alsaif.mushaf

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alsaif.mushaf.audio.AudioController
import com.alsaif.mushaf.audio.RECITERS
import com.alsaif.mushaf.data.Prefs
import com.alsaif.mushaf.data.QuranData
import com.alsaif.mushaf.data.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data class Ready(val data: QuranData) : LoadState
}

class MushafViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    val audio = AudioController(app)

    var loadState by mutableStateOf<LoadState>(LoadState.Loading)
        private set
    var reciterIndex by mutableIntStateOf(prefs.reciter.coerceIn(0, RECITERS.lastIndex))
        private set
    var repeatCount by mutableIntStateOf(1)
    var bookmarks by mutableStateOf(prefs.bookmarks)
        private set
    var lastPage by mutableIntStateOf(prefs.lastPage)
        private set

    /** ثيم صفحة المصحف (من قائمة READER_THEMES) */
    var readerThemeIndex by mutableIntStateOf(prefs.readerTheme.coerceIn(0, com.alsaif.mushaf.ui.theme.READER_THEMES.lastIndex))
        private set

    /** حجم خط المصحف */
    var fontSize by mutableStateOf(prefs.fontSize)
        private set

    val readerTheme get() = com.alsaif.mushaf.ui.theme.READER_THEMES[readerThemeIndex]

    fun selectReaderTheme(i: Int) {
        readerThemeIndex = i
        prefs.readerTheme = i
    }

    /** persist=false أثناء التكبير بالإصبعين، ونحفظ عند انتهاء الحركة */
    fun changeFontSize(size: Float, persist: Boolean = true) {
        fontSize = size.coerceIn(MIN_FONT, MAX_FONT)
        if (persist) prefs.fontSize = fontSize
    }

    fun saveFontSize() {
        prefs.fontSize = fontSize
    }

    /** تلوين أحكام التجويد */
    var tajweedOn by mutableStateOf(prefs.tajweed)
        private set

    fun setTajweed(on: Boolean) {
        tajweedOn = on
        prefs.tajweed = on
    }

    /** true = صفحات متتالية بالتمرير للأسفل، false = صفحة صفحة بالسحب الجانبي */
    var continuous by mutableStateOf(prefs.continuous)
        private set

    fun setContinuousMode(on: Boolean) {
        continuous = on
        prefs.continuous = on
    }

    companion object {
        const val MIN_FONT = 18f
        const val MAX_FONT = 44f
    }

    /** (تم تحميله، الإجمالي) أثناء تحميل سورة للاستماع بدون إنترنت */
    var download by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var downloadError by mutableStateOf(false)
        private set

    val data: QuranData? get() = (loadState as? LoadState.Ready)?.data
    val reciter get() = RECITERS[reciterIndex]

    init {
        load()
    }

    fun load() {
        loadState = LoadState.Loading
        viewModelScope.launch {
            loadState = try {
                LoadState.Ready(QuranRepository.load(getApplication()))
            } catch (e: Exception) {
                LoadState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** تشغيل من آية معيّنة إلى نهاية السورة */
    fun playFrom(surah: Int, ayah: Int) {
        val d = data ?: return
        audio.play(surah, ayah.coerceAtLeast(1), d.surah(surah).ayahCount, reciter, repeatCount)
    }

    fun playSingle(surah: Int, ayah: Int) = audio.play(surah, ayah, ayah, reciter, repeatCount)

    fun togglePlay(fallbackPage: Int) {
        if (audio.hasQueue) {
            audio.toggle()
        } else {
            val first = data?.firstAyahOnPage(fallbackPage) ?: return
            playFrom(first.surah, first.numberInSurah)
        }
    }

    fun selectReciter(index: Int) {
        reciterIndex = index
        prefs.reciter = index
        val st = audio.state.value
        if (audio.hasQueue && st.surah > 0) playFrom(st.surah, st.ayah.coerceAtLeast(1))
    }

    fun changeRepeat(count: Int) {
        repeatCount = count
        val st = audio.state.value
        if (audio.hasQueue && st.surah > 0) playFrom(st.surah, st.ayah.coerceAtLeast(1))
    }

    fun saveLastPage(page: Int) {
        lastPage = page
        prefs.lastPage = page
    }

    fun toggleBookmark(page: Int) {
        bookmarks = if (page in bookmarks) bookmarks - page else bookmarks + page
        prefs.bookmarks = bookmarks
    }

    fun isSurahDownloaded(surah: Int): Boolean {
        val d = data ?: return false
        return audio.isDownloaded(reciter, surah, d.surah(surah).ayahCount)
    }

    fun downloadSurah(surah: Int) {
        val d = data ?: return
        if (download != null) return
        val total = d.surah(surah).ayahCount
        downloadError = false
        download = 0 to total
        viewModelScope.launch {
            try {
                audio.downloadSurah(reciter, surah, total) { done ->
                    viewModelScope.launch(Dispatchers.Main) { download = done to total }
                }
            } catch (e: Exception) {
                downloadError = true
            } finally {
                withContext(Dispatchers.Main) { download = null }
            }
        }
    }

    override fun onCleared() {
        audio.release()
    }
}

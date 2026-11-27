package com.alsaif.mushaf.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Locale

data class Reciter(val name: String, val folder: String)

/** القرّاء من موقع everyayah.com (ملف صوتي لكل آية) */
val RECITERS = listOf(
    Reciter("مشاري العفاسي", "Alafasy_128kbps"),
    Reciter("عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps"),
    Reciter("عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps"),
    Reciter("ماهر المعيقلي", "MaherAlMuaiqly128kbps"),
    Reciter("سعد الغامدي", "Ghamadi_40kbps"),
)

data class PlayState(
    val surah: Int = 0,
    val ayah: Int = 0, // 0 = البسملة
    val playing: Boolean = false,
    val speed: Float = 1f,
)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class AudioController(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true,
        )
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val parts = mediaItem?.mediaId?.split(":") ?: return
                if (parts.size == 2) _state.update { it.copy(surah = parts[0].toInt(), ayah = parts[1].toInt()) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(playing = isPlaying) }
            }
        })
    }

    val hasQueue: Boolean get() = player.mediaItemCount > 0

    fun play(surah: Int, from: Int, to: Int, reciter: Reciter, times: Int) {
        val items = ArrayList<MediaItem>()
        // البسملة قبل بداية السورة (عدا الفاتحة والتوبة)
        if (from == 1 && surah != 1 && surah != 9) {
            items += item("$surah:0", uriFor(reciter, 1, 1))
        }
        for (a in from..to) {
            repeat(times.coerceAtLeast(1)) { items += item("$surah:$a", uriFor(reciter, surah, a)) }
        }
        player.setMediaItems(items)
        player.prepare()
        player.play()
        _state.update { it.copy(surah = surah, ayah = if (items.first().mediaId.endsWith(":0")) 0 else from) }
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else if (hasQueue) player.play()
    }

    fun next() {
        val id = player.currentMediaItem?.mediaId ?: return
        var i = player.currentMediaItemIndex + 1
        while (i < player.mediaItemCount && player.getMediaItemAt(i).mediaId == id) i++
        if (i < player.mediaItemCount) player.seekTo(i, 0)
    }

    fun previous() {
        var i = player.currentMediaItemIndex
        val id = player.currentMediaItem?.mediaId ?: return
        while (i > 0 && player.getMediaItemAt(i - 1).mediaId == id) i--
        if (i == 0) {
            player.seekTo(0, 0); return
        }
        var j = i - 1
        val prevId = player.getMediaItemAt(j).mediaId
        while (j > 0 && player.getMediaItemAt(j - 1).mediaId == prevId) j--
        player.seekTo(j, 0)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    fun release() = player.release()

    // ---------- التحميل للاستماع بدون إنترنت ----------

    private fun fileName(surah: Int, ayah: Int) = String.format(Locale.US, "%03d%03d.mp3", surah, ayah)

    private fun localFile(r: Reciter, surah: Int, ayah: Int) =
        File(File(context.filesDir, "audio/${r.folder}"), fileName(surah, ayah))

    private fun remoteUrl(r: Reciter, surah: Int, ayah: Int) =
        "https://everyayah.com/data/${r.folder}/${fileName(surah, ayah)}"

    private fun uriFor(r: Reciter, surah: Int, ayah: Int): String {
        val f = localFile(r, surah, ayah)
        return if (f.exists()) f.toURI().toString() else remoteUrl(r, surah, ayah)
    }

    fun isDownloaded(r: Reciter, surah: Int, count: Int): Boolean =
        (1..count).all { localFile(r, surah, it).exists() }

    suspend fun downloadSurah(r: Reciter, surah: Int, count: Int, onProgress: (Int) -> Unit) =
        withContext(Dispatchers.IO) {
            val targets = (1..count).map { surah to it } + (1 to 1) // البسملة
            targets.forEachIndexed { index, (s, a) ->
                val f = localFile(r, s, a)
                if (!f.exists()) {
                    f.parentFile?.mkdirs()
                    val tmp = File(f.path + ".part")
                    URL(remoteUrl(r, s, a)).openStream().use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    tmp.renameTo(f)
                }
                onProgress(minOf(index + 1, count))
            }
        }

    private fun item(id: String, uri: String) = MediaItem.Builder().setMediaId(id).setUri(uri).build()
}

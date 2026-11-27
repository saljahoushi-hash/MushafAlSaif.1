package com.alsaif.mushaf.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** مقطع ملوّن بحكم تجويد: من start إلى end في نص العرض، والرمز حسب ترميز alquran.cloud */
data class TajSpan(val start: Int, val end: Int, val code: Char)

data class Ayah(
    val surah: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    val page: Int,
    val plain: String, // نص بدون تشكيل للبحث
    val display: String = text, // نص العرض (نسخة التجويد إن توفرت)
    val tajweed: List<TajSpan> = emptyList(),
)

data class Surah(
    val number: Int,
    val name: String,
    val revelation: String,
    val ayahCount: Int,
    val startPage: Int,
)

class QuranData(val surahs: List<Surah>, val ayahs: List<Ayah>) {
    private val bySurah: List<List<Ayah>> = surahs.map { s -> ayahs.filter { it.surah == s.number } }
    val byPage: Map<Int, List<Ayah>> = ayahs.groupBy { it.page }
    val pageCount: Int = ayahs.maxOf { it.page }

    fun surah(n: Int): Surah = surahs[n - 1]
    fun ayahsOf(surah: Int): List<Ayah> = bySurah[surah - 1]
    fun ayah(surah: Int, n: Int): Ayah = bySurah[surah - 1][n - 1]
    fun pageOf(surah: Int, n: Int): Int = ayah(surah, n.coerceAtLeast(1)).page
    fun juzStartPage(juz: Int): Int = ayahs.first { it.juz == juz }.page
    fun firstAyahOnPage(page: Int): Ayah = byPage[page]?.first() ?: ayahs.first()

    fun search(query: String, limit: Int = 100): List<Ayah> {
        val q = Arabic.normalize(query).trim()
        if (q.length < 2) return emptyList()
        return ayahs.asSequence().filter { it.plain.contains(q) }.take(limit).toList()
    }
}

/** يحوّل ترميز التجويد مثل [h:9421[ٱ] إلى نص عادي + مواضع الألوان */
object Tajweed {
    fun parse(src: String): Pair<String, List<TajSpan>> {
        val sb = StringBuilder(src.length)
        val spans = ArrayList<TajSpan>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            if (c == '[' && i + 1 < src.length && src[i + 1] in 'a'..'z') {
                val open = src.indexOf('[', i + 2)
                val close = if (open > 0) src.indexOf(']', open + 1) else -1
                if (open > 0 && close > 0 && open - i <= 12) {
                    val start = sb.length
                    sb.append(src, open + 1, close)
                    spans += TajSpan(start, sb.length, src[i + 1])
                    i = close + 1
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString() to spans
    }
}

object Arabic {
    private const val DIGITS = "٠١٢٣٤٥٦٧٨٩"

    fun digits(n: Int): String = n.toString().map { c -> if (c.isDigit()) DIGITS[c - '0'] else c }.joinToString("")

    /** يحذف التشكيل ويوحّد أشكال الألف لتسهيل البحث */
    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val code = c.code
            val isMark = code in 0x0610..0x061A || code in 0x064B..0x065F || code == 0x0670 ||
                code in 0x06D6..0x06ED || code == 0x0640 || code == 0xFEFF
            if (isMark) continue
            sb.append(
                when (c) {
                    'ٱ', 'أ', 'إ', 'آ' -> 'ا'
                    'ة' -> 'ه'
                    'ى' -> 'ي'
                    else -> c
                }
            )
        }
        return sb.toString()
    }
}

/**
 * يحمّل نص المصحف (الرسم العثماني) مرة واحدة من api.alquran.cloud ويحفظه في الجهاز،
 * وبعدها يعمل التطبيق بدون إنترنت. لو وضعت الملف داخل assets باسم quran-uthmani.json
 * يُقرأ مباشرة بدون تحميل.
 */
object QuranRepository {
    private const val SOURCE = "https://api.alquran.cloud/v1/quran/quran-uthmani"
    private const val FILE = "quran-uthmani.json"
    private const val TAJWEED_SOURCE = "https://api.alquran.cloud/v1/quran/quran-tajweed"
    private const val TAJWEED_FILE = "quran-tajweed.json"

    suspend fun load(context: Context): QuranData = withContext(Dispatchers.IO) {
        val bundled = runCatching { context.assets.open(FILE).bufferedReader().use { it.readText() } }.getOrNull()
        val json = bundled ?: run {
            val cache = File(context.filesDir, FILE)
            if (cache.exists() && cache.length() > 100_000) {
                cache.readText()
            } else {
                val text = download(SOURCE)
                val tmp = File(context.filesDir, "$FILE.tmp")
                tmp.writeText(text)
                tmp.renameTo(cache)
                text
            }
        }
        val base = parse(json)
        // نسخة التجويد الملوّنة: اختيارية، لو فشل تحميلها يبقى المصحف بدون تلوين
        val tajweed = runCatching { loadText(context, TAJWEED_FILE, TAJWEED_SOURCE) }.getOrNull()
        if (tajweed == null) base else runCatching { mergeTajweed(base, tajweed) }.getOrDefault(base)
    }

    private fun loadText(context: Context, name: String, url: String): String {
        runCatching { context.assets.open(name).bufferedReader().use { it.readText() } }.getOrNull()?.let { return it }
        val cache = File(context.filesDir, name)
        if (cache.exists() && cache.length() > 100_000) return cache.readText()
        val text = download(url)
        val tmp = File(context.filesDir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(cache)
        return text
    }

    private fun mergeTajweed(base: QuranData, json: String): QuranData {
        val surahsJson = JSONObject(json).getJSONObject("data").getJSONArray("surahs")
        val out = ArrayList<Ayah>(base.ayahs.size)
        var k = 0
        for (i in 0 until surahsJson.length()) {
            val s = surahsJson.getJSONObject(i)
            val number = s.getInt("number")
            val arr = s.getJSONArray("ayahs")
            for (j in 0 until arr.length()) {
                val a = arr.getJSONObject(j)
                val n = a.getInt("numberInSurah")
                val original = base.ayahs[k++]
                if (original.surah != number || original.numberInSurah != n) error("mismatch")
                var (text, spans) = Tajweed.parse(a.getString("text").trimStart('\uFEFF'))
                if (n == 1 && number != 1) {
                    // نحذف البسملة الملتصقة بأول آية مع إزاحة مواضع التلوين
                    val words = text.split(' ')
                    if (words.size > 4 && Arabic.normalize(words[0]) == "بسم") {
                        var cut = 0
                        repeat(4) { cut = text.indexOf(' ', cut) + 1 }
                        text = text.substring(cut)
                        spans = spans.filter { it.end > cut }
                            .map { TajSpan((it.start - cut).coerceAtLeast(0), it.end - cut, it.code) }
                    }
                }
                val shown = text.trimEnd()
                out += original.copy(display = shown, tajweed = spans.filter { it.end <= shown.length })
            }
        }
        if (k != base.ayahs.size) error("count mismatch")
        return QuranData(base.surahs, out)
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String): QuranData {
        val surahsJson = JSONObject(json).getJSONObject("data").getJSONArray("surahs")
        val surahs = ArrayList<Surah>(114)
        val ayahs = ArrayList<Ayah>(6236)
        for (i in 0 until surahsJson.length()) {
            val s = surahsJson.getJSONObject(i)
            val number = s.getInt("number")
            val arr = s.getJSONArray("ayahs")
            for (j in 0 until arr.length()) {
                val a = arr.getJSONObject(j)
                val n = a.getInt("numberInSurah")
                var text = a.getString("text").trimStart('﻿').trim()
                // في هذا المصدر تأتي البسملة ملتصقة بأول آية من كل سورة (عدا الفاتحة)، فنفصلها
                if (n == 1 && number != 1) {
                    val words = text.split(' ')
                    if (words.size > 4 && Arabic.normalize(words[0]) == "بسم") {
                        text = words.drop(4).joinToString(" ")
                    }
                }
                ayahs += Ayah(
                    surah = number,
                    numberInSurah = n,
                    text = text,
                    juz = a.getInt("juz"),
                    page = a.getInt("page"),
                    plain = Arabic.normalize(text),
                )
            }
            val name = s.getString("name").replace("سُورَةُ", "").trim()
            surahs += Surah(
                number = number,
                name = name,
                revelation = if (s.getString("revelationType") == "Meccan") "مكية" else "مدنية",
                ayahCount = arr.length(),
                startPage = arr.getJSONObject(0).getInt("page"),
            )
        }
        return QuranData(surahs, ayahs)
    }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mushaf", Context.MODE_PRIVATE)

    var lastPage: Int
        get() = sp.getInt("lastPage", 1)
        set(v) = sp.edit().putInt("lastPage", v).apply()

    var reciter: Int
        get() = sp.getInt("reciter", 0)
        set(v) = sp.edit().putInt("reciter", v).apply()

    var readerTheme: Int
        get() = sp.getInt("readerTheme", 0)
        set(v) = sp.edit().putInt("readerTheme", v).apply()

    var fontSize: Float
        get() = sp.getFloat("fontSize", 24f)
        set(v) = sp.edit().putFloat("fontSize", v).apply()

    var tajweed: Boolean
        get() = sp.getBoolean("tajweed", true)
        set(v) = sp.edit().putBoolean("tajweed", v).apply()

    var continuous: Boolean
        get() = sp.getBoolean("continuous", true)
        set(v) = sp.edit().putBoolean("continuous", v).apply()

    var bookmarks: Set<Int>
        get() = sp.getStringSet("bookmarks", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = sp.edit().putStringSet("bookmarks", v.map { it.toString() }.toSet()).apply()
}

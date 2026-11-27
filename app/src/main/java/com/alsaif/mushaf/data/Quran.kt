package com.alsaif.mushaf.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Ayah(
    val surah: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    val page: Int,
    val plain: String, // نص بدون تشكيل للبحث
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
        parse(json)
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

    var bookmarks: Set<Int>
        get() = sp.getStringSet("bookmarks", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = sp.edit().putStringSet("bookmarks", v.map { it.toString() }.toSet()).apply()
}

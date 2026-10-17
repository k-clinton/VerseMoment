package com.versemoment.app.data

import android.content.res.AssetManager
import org.json.JSONArray

object KjvCatalog {
    const val EXPECTED_VERSE_COUNT = 31_102
    private const val ASSET_FILE = "en_kjv.json"

    private val books = listOf(
        Book("gn", "genesis", "gen", "Genesis"),
        Book("ex", "exodus", "exo", "Exodus"),
        Book("lv", "leviticus", "lev", "Leviticus"),
        Book("nm", "numbers", "num", "Numbers"),
        Book("dt", "deuteronomy", "deu", "Deuteronomy"),
        Book("js", "joshua", "jos", "Joshua"),
        Book("jz", "judges", "jdg", "Judges"),
        Book("rt", "ruth", "rut", "Ruth"),
        Book("1sm", "1samuel", "1sa", "1 Samuel"),
        Book("2sm", "2samuel", "2sa", "2 Samuel"),
        Book("1rs", "1kings", "1ki", "1 Kings"),
        Book("2rs", "2kings", "2ki", "2 Kings"),
        Book("1cr", "1chronicles", "1ch", "1 Chronicles"),
        Book("2cr", "2chronicles", "2ch", "2 Chronicles"),
        Book("ed", "ezra", "ezr", "Ezra"),
        Book("ne", "nehemiah", "neh", "Nehemiah"),
        Book("et", "esther", "est", "Esther"),
        Book("jó", "job", "job", "Job"),
        Book("sl", "psalms", "ps", "Psalms"),
        Book("pv", "proverbs", "prov", "Proverbs"),
        Book("ec", "ecclesiastes", "ecc", "Ecclesiastes"),
        Book("ct", "songofsolomon", "sng", "Song of Solomon"),
        Book("is", "isaiah", "isa", "Isaiah"),
        Book("jr", "jeremiah", "jer", "Jeremiah"),
        Book("lm", "lamentations", "lam", "Lamentations"),
        Book("ez", "ezekiel", "eze", "Ezekiel"),
        Book("dn", "daniel", "dan", "Daniel"),
        Book("os", "hosea", "hos", "Hosea"),
        Book("jl", "joel", "joe", "Joel"),
        Book("am", "amos", "amo", "Amos"),
        Book("ob", "obadiah", "oba", "Obadiah"),
        Book("jn", "jonah", "jon", "Jonah"),
        Book("mq", "micah", "mic", "Micah"),
        Book("na", "nahum", "nah", "Nahum"),
        Book("hc", "habakkuk", "hab", "Habakkuk"),
        Book("sf", "zephaniah", "zep", "Zephaniah"),
        Book("ag", "haggai", "hag", "Haggai"),
        Book("zc", "zechariah", "zec", "Zechariah"),
        Book("ml", "malachi", "mal", "Malachi"),
        Book("mt", "matthew", "mat", "Matthew"),
        Book("mc", "mark", "mrk", "Mark"),
        Book("lc", "luke", "luk", "Luke"),
        Book("jo", "john", "jhn", "John"),
        Book("atos", "acts", "act", "Acts"),
        Book("rm", "romans", "rom", "Romans"),
        Book("1co", "1corinthians", "1co", "1 Corinthians"),
        Book("2co", "2corinthians", "2co", "2 Corinthians"),
        Book("gl", "galatians", "gal", "Galatians"),
        Book("ef", "ephesians", "eph", "Ephesians"),
        Book("fp", "philippians", "php", "Philippians"),
        Book("cl", "colossians", "col", "Colossians"),
        Book("1ts", "1thessalonians", "1th", "1 Thessalonians"),
        Book("2ts", "2thessalonians", "2th", "2 Thessalonians"),
        Book("1tm", "1timothy", "1ti", "1 Timothy"),
        Book("2tm", "2timothy", "2ti", "2 Timothy"),
        Book("tt", "titus", "tit", "Titus"),
        Book("fm", "philemon", "phm", "Philemon"),
        Book("hb", "hebrews", "heb", "Hebrews"),
        Book("tg", "james", "jas", "James"),
        Book("1pe", "1peter", "1pe", "1 Peter"),
        Book("2pe", "2peter", "2pe", "2 Peter"),
        Book("1jo", "1john", "1jn", "1 John"),
        Book("2jo", "2john", "2jn", "2 John"),
        Book("3jo", "3john", "3jn", "3 John"),
        Book("jd", "jude", "jud", "Jude"),
        Book("ap", "revelation", "rev", "Revelation"),
    )

    fun load(assets: AssetManager): List<VerseEntity> {
        val json = assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        val sourceBooks = JSONArray(json)
        check(sourceBooks.length() == books.size) {
            "KJV asset has ${sourceBooks.length()} books; expected ${books.size}."
        }

        val verses = ArrayList<VerseEntity>(EXPECTED_VERSE_COUNT)
        for (bookIndex in 0 until sourceBooks.length()) {
            val sourceBook = sourceBooks.getJSONObject(bookIndex)
            val book = books.firstOrNull { it.sourceAbbreviation == sourceBook.getString("abbrev") }
                ?: error("Unknown KJV book abbreviation: ${sourceBook.getString("abbrev")}")
            val chapters = sourceBook.getJSONArray("chapters")
            for (chapterIndex in 0 until chapters.length()) {
                val chapterVerses = chapters.getJSONArray(chapterIndex)
                for (verseIndex in 0 until chapterVerses.length()) {
                    val chapter = chapterIndex + 1
                    val verseNumber = verseIndex + 1
                    verses += VerseEntity(
                        id = "${book.idPrefix}_${chapter}_${verseNumber}",
                        translationId = "kjv",
                        bookId = book.id,
                        bookName = book.name,
                        bookOrder = bookIndex + 1,
                        chapter = chapter,
                        verseNumber = verseNumber,
                        text = chapterVerses.getString(verseIndex),
                    )
                }
            }
        }
        check(verses.size == EXPECTED_VERSE_COUNT) {
            "KJV asset has ${verses.size} verses; expected $EXPECTED_VERSE_COUNT."
        }
        return verses
    }

    private data class Book(
        val sourceAbbreviation: String,
        val id: String,
        val idPrefix: String,
        val name: String,
    )
}

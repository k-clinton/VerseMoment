package com.versemoment.app.selection

import com.versemoment.app.data.VerseRepository
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.SelectionCandidate
import com.versemoment.app.domain.SelectionMode
import com.versemoment.app.domain.VerseSelectionResult
import java.util.Calendar
import kotlin.math.abs

class SelectionEngine(
    private val verseRepository: VerseRepository,
) {
    suspend fun selectTimeMatch(timeInMillis: Long, preferredTranslationId: String): VerseSelectionResult? {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val chapter = hour.coerceIn(1, 24)
        val verse = minute.coerceIn(1, 59)

        val candidates = mutableListOf<SelectionCandidate>()
        for (book in BOOK_ORDER) {
            val reference = "${book.bookName} $chapter:$verse"
            val selected = verseRepository.verseByReference(preferredTranslationId, book.bookId, chapter, verse)
            if (selected != null) {
                candidates.add(
                    SelectionCandidate(
                        bookId = book.bookId,
                        bookName = book.bookName,
                        chapter = chapter,
                        verseNumber = verse,
                        reference = reference,
                    ),
                )
            }
        }

        if (candidates.isEmpty()) {
            return null
        }

        val picked = candidates.minByOrNull {
            abs(it.chapter - chapter) + abs(it.verseNumber - verse) + it.bookName.length
        }

        val verseMatch = verseRepository.verseByReference(preferredTranslationId, picked!!.bookId, picked.chapter, picked.verseNumber)
            ?: return null

        return VerseSelectionResult(
            verse = verseMatch,
            mode = SelectionMode.TIME_MATCH,
            candidateReference = picked.reference,
            reason = "TimeMatch matched local time ${hour}:${minute.toString().padStart(2, '0')}",
            repetitionRelaxed = false,
            selectedAt = timeInMillis,
        )
    }

    suspend fun selectDivineDiscovery(preferredTranslationId: String, theme: String? = null): VerseSelectionResult? {
        val verses = verseRepository.allVerses().filter { it.translationId == preferredTranslationId }
        val eligible = if (theme == null) verses else verses.filter { it.tags.any { tag -> tag.equals(theme, ignoreCase = true) } }
        val chosen = eligible.firstOrNull() ?: return null
        return VerseSelectionResult(
            verse = chosen,
            mode = SelectionMode.DIVINE_DISCOVERY,
            candidateReference = "${chosen.bookName} ${chosen.chapter}:${chosen.verseNumber}",
            reason = "Divine Discovery selected a curated verse for theme=${theme ?: "all"}",
            repetitionRelaxed = false,
            selectedAt = System.currentTimeMillis(),
        )
    }

    suspend fun selectAny(preferredTranslationId: String, theme: String? = null): VerseSelectionResult? {
        val timeMatch = selectTimeMatch(System.currentTimeMillis(), preferredTranslationId)
        if (timeMatch != null) return timeMatch
        return selectDivineDiscovery(preferredTranslationId, theme)
    }

    companion object {
        private val BOOK_ORDER = listOf(
            BookReference("john", "John"),
            BookReference("psalms", "Psalms"),
            BookReference("philippians", "Philippians"),
            BookReference("isaiah", "Isaiah"),
            BookReference("romans", "Romans"),
            BookReference("1corinthians", "1 Corinthians"),
            BookReference("proverbs", "Proverbs"),
            BookReference("2timothy", "2 Timothy"),
        )
    }

    private data class BookReference(val bookId: String, val bookName: String)
}

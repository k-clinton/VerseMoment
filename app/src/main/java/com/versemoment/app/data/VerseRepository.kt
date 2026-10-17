package com.versemoment.app.data

import com.versemoment.app.domain.BibleTranslation
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.SelectionMode
import com.versemoment.app.domain.WallpaperUpdateResult
import com.versemoment.app.domain.WallpaperApplyStatus
import com.versemoment.app.domain.WallpaperSettings
import android.content.res.AssetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class VerseRepository(
    private val verseDao: VerseDao,
    private val assets: AssetManager,
) {
    private val favoritesFlow = MutableStateFlow<List<String>>(emptyList())

    suspend fun seedTranslationsAndVerses() {
        verseDao.insertTranslation(DatabaseSeed.translations().single())
        if (verseDao.countVerses("kjv") != KjvCatalog.EXPECTED_VERSE_COUNT) {
            verseDao.replaceVerses(KjvCatalog.load(assets))
            DatabaseSeed.tags().forEach { verseDao.insertTag(it) }
        }
    }

    suspend fun translations(): List<BibleTranslation> = verseDao.getTranslations().map { it.toDomain() }

    suspend fun allVerses(translationId: String = "kjv"): List<BibleVerse> =
        verseDao.getVersesForTranslation(translationId).map { it.toDomain() }

    suspend fun verseByReference(translationId: String, bookId: String, chapter: Int, verseNumber: Int): BibleVerse? {
        return verseDao.getVerseByReference(translationId, bookId, chapter, verseNumber)?.toDomain()
    }

    suspend fun verseByChapterAndVerse(translationId: String, chapter: Int, verseNumber: Int): BibleVerse? {
        return verseDao.getVerseByChapterAndVerse(translationId, chapter, verseNumber)?.toDomain()
    }

    suspend fun saveFavorite(verseId: String) {
        verseDao.insertFavorite(FavoriteEntity(verseId = verseId, savedAt = System.currentTimeMillis()))
        favoritesFlow.value = favoritesFlow.value + verseId
    }

    suspend fun removeFavorite(verseId: String) {
        verseDao.removeFavorite(verseId)
        favoritesFlow.value = favoritesFlow.value.filterNot { it == verseId }
    }

    suspend fun favoriteIds(): List<String> = verseDao.getFavorites(100).map { it.verseId }

    suspend fun history(limit: Int = 20): List<WallpaperUpdateResult> = verseDao.getHistory(limit).map {
        WallpaperUpdateResult(
            id = it.id,
            verseId = it.verseId,
            selectionMode = SelectionMode.valueOf(it.selectionMode),
            selectedAt = it.selectedAt,
            appliedAt = it.appliedAt,
            status = WallpaperApplyStatus.valueOf(it.status),
            errorCode = it.errorCode,
            errorMessage = it.errorMessage,
        )
    }

    suspend fun recordHistory(result: WallpaperUpdateResult) {
        verseDao.insertHistory(
            WallpaperHistoryEntity(
                id = result.id,
                verseId = result.verseId,
                selectionMode = result.selectionMode.name,
                selectedAt = result.selectedAt,
                appliedAt = result.appliedAt,
                status = result.status.name,
                themeId = "cinematic_nature",
                errorCode = result.errorCode,
                errorMessage = result.errorMessage,
            ),
        )
    }

    fun favoritesFlow(): Flow<List<String>> = favoritesFlow

    fun currentSettings(): WallpaperSettings = WallpaperSettings(
        updateIntervalMinutes = 5,
        lockScreenOnly = true,
        timeMatchEnabled = true,
        divineDiscoveryEnabled = true,
        selectedTranslationId = "kjv",
        selectedThemeId = "cinematic_nature",
        repetitionAvoidanceEnabled = true,
    )
}

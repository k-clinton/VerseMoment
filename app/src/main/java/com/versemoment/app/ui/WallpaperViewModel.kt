package com.versemoment.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.versemoment.app.data.PreferencesRepository
import com.versemoment.app.data.VerseRepository
import com.versemoment.app.scheduling.AutomationScheduler
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.SelectionMode
import com.versemoment.app.domain.WallpaperApplyStatus
import com.versemoment.app.domain.WallpaperUpdateResult
import com.versemoment.app.wallpaper.WallpaperRenderer
import com.versemoment.app.wallpaper.WallpaperBackgroundUnavailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WallpaperUiState(
    val verses: List<BibleVerse> = emptyList(),
    val selectedVerse: BibleVerse? = null,
    val isLoading: Boolean = true,
    val isApplying: Boolean = false,
    val automationEnabled: Boolean = true,
    val exactAlarmsAllowed: Boolean = true,
    val needsBackgroundImage: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val verseRepository: VerseRepository,
    private val preferencesRepository: PreferencesRepository,
    private val wallpaperRenderer: WallpaperRenderer,
    private val automationScheduler: AutomationScheduler,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WallpaperUiState())
    val uiState: StateFlow<WallpaperUiState> = mutableUiState.asStateFlow()

    init {
        refreshExactAlarmAccess()
        viewModelScope.launch {
            preferencesRepository.automationEnabledFlow.collect { enabled ->
                mutableUiState.value = mutableUiState.value.copy(automationEnabled = enabled)
            }
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    verseRepository.seedTranslationsAndVerses()
                }
                val verses = withContext(Dispatchers.IO) {
                    verseRepository.allVerses(preferencesRepository.selectedTranslationFlow.first())
                }
                mutableUiState.value = WallpaperUiState(
                    verses = verses,
                    selectedVerse = verses.firstOrNull(),
                    isLoading = false,
                    statusMessage = if (verses.isEmpty()) {
                        "No verses are available for the selected translation."
                    } else {
                        null
                    },
                    isError = verses.isEmpty(),
                )
            } catch (exception: Exception) {
                mutableUiState.value = WallpaperUiState(
                    isLoading = false,
                    statusMessage = "Could not load verses: ${exception.message ?: exception.javaClass.simpleName}",
                    isError = true,
                )
            }
        }
    }

    fun refreshExactAlarmAccess() {
        mutableUiState.value = mutableUiState.value.copy(
            exactAlarmsAllowed = automationScheduler.canScheduleExactAlarms(),
        )
    }

    fun selectVerse(verse: BibleVerse) {
        mutableUiState.value = mutableUiState.value.copy(
            selectedVerse = verse,
            statusMessage = null,
            isError = false,
        )
    }

    fun setAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setAutomationEnabled(enabled)
                if (enabled) {
                    automationScheduler.scheduleNextUpdate(preferencesRepository.updateIntervalFlow.first())
                } else {
                    automationScheduler.cancel()
                }
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    statusMessage = "Could not ${if (enabled) "enable" else "disable"} automatic updates: ${exception.message ?: exception.javaClass.simpleName}",
                    isError = true,
                )
            }
        }
    }

    fun applySelectedVerse() {
        applyVerse(mutableUiState.value.selectedVerse, SelectionMode.DIVINE_DISCOVERY)
    }

    fun requestBackgroundImage() {
        if (mutableUiState.value.isApplying || mutableUiState.value.isLoading) return
        mutableUiState.value = mutableUiState.value.copy(
            needsBackgroundImage = true,
            statusMessage = null,
            isError = false,
        )
    }

    fun applySelectedVerseWithBackground(uri: Uri) {
        mutableUiState.value = mutableUiState.value.copy(needsBackgroundImage = false)
        applyVerse(mutableUiState.value.selectedVerse, SelectionMode.DIVINE_DISCOVERY, backgroundUri = uri)
    }

    fun backgroundImageSelectionCancelled() {
        mutableUiState.value = mutableUiState.value.copy(
            needsBackgroundImage = false,
            statusMessage = "No background was selected; your wallpaper was left unchanged.",
            isError = false,
        )
    }

    fun refreshWallpaper() {
        if (mutableUiState.value.isApplying || mutableUiState.value.isLoading) return
        mutableUiState.value = mutableUiState.value.copy(
            isApplying = true,
            statusMessage = "Choosing a new verse…",
            isError = false,
        )
        viewModelScope.launch {
            try {
                val selection = withContext(Dispatchers.IO) {
                    chooseDifferentVerse()
                }
                if (selection == null) {
                    mutableUiState.value = mutableUiState.value.copy(
                        isApplying = false,
                        statusMessage = "No verses are available to apply.",
                        isError = true,
                    )
                } else {
                    mutableUiState.value = mutableUiState.value.copy(selectedVerse = selection)
                    applyVerse(selection, SelectionMode.DIVINE_DISCOVERY, alreadyApplying = true)
                }
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isApplying = false,
                    statusMessage = "Could not select a verse: ${exception.message ?: exception.javaClass.simpleName}",
                    isError = true,
                )
            }
        }
    }

    fun chooseAnotherVerse() {
        if (mutableUiState.value.isApplying || mutableUiState.value.isLoading) return
        viewModelScope.launch {
            try {
                val verse = withContext(Dispatchers.IO) { chooseDifferentVerse() }
                mutableUiState.value = mutableUiState.value.copy(
                    selectedVerse = verse ?: mutableUiState.value.selectedVerse,
                    statusMessage = verse?.let {
                        "Selected ${it.bookName} ${it.chapter}:${it.verseNumber}. Tap Apply to lock screen."
                    } ?: "No verses are available.",
                    isError = verse == null,
                )
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    statusMessage = "Could not choose a verse: ${exception.message ?: exception.javaClass.simpleName}",
                    isError = true,
                )
            }
        }
    }

    private suspend fun chooseDifferentVerse(): BibleVerse? {
        val state = mutableUiState.value
        if (state.verses.isEmpty()) return null

        val translationId = preferencesRepository.selectedTranslationFlow.first()
        val lastAppliedVerseId = verseRepository.history(20)
            .firstOrNull { it.status == WallpaperApplyStatus.SUCCESS }
            ?.verseId
        val excludeIds = setOfNotNull(state.selectedVerse?.id, lastAppliedVerseId)
        val choices = state.verses.filter { it.id !in excludeIds }
            .ifEmpty { state.verses.filterNot { it.id == state.selectedVerse?.id } }
            .ifEmpty { state.verses }

        return choices.filter { it.translationId == translationId }
            .ifEmpty { choices }
            .randomOrNull()
    }

    private fun applyVerse(
        verse: BibleVerse?,
        mode: SelectionMode,
        alreadyApplying: Boolean = false,
        backgroundUri: Uri? = null,
    ) {
        if (verse == null || (mutableUiState.value.isApplying && !alreadyApplying)) return
        mutableUiState.value = mutableUiState.value.copy(
            isApplying = true,
            statusMessage = "Applying lock-screen wallpaper…",
            isError = false,
        )
        viewModelScope.launch {
            val selectedAt = System.currentTimeMillis()
            val applyError = try {
                withContext(Dispatchers.IO) {
                    val background = backgroundUri?.let(wallpaperRenderer::saveBackgroundFromUri)
                        ?: wallpaperRenderer.loadOriginalBackground()
                    wallpaperRenderer.applyWallpaper(
                        wallpaperRenderer.renderWallpaper(verse, background),
                    )
                }
                null
            } catch (exception: Exception) {
                exception
            }
            if (applyError is WallpaperBackgroundUnavailableException) {
                mutableUiState.value = mutableUiState.value.copy(
                    isApplying = false,
                    needsBackgroundImage = true,
                    statusMessage = "Android could not provide your current lock-screen wallpaper. Choose the background image to keep using.",
                    isError = false,
                )
                return@launch
            }
            val historyError = try {
                saveHistory(
                    verse,
                    mode,
                    selectedAt,
                    success = applyError == null,
                    errorMessage = applyError?.let(::errorMessage),
                )
                null
            } catch (exception: Exception) {
                exception
            }
            val message = when {
                applyError != null ->
                    "Wallpaper was not applied: ${errorMessage(applyError)}"
                historyError != null ->
                    "Wallpaper applied, but history could not be saved: ${errorMessage(historyError)}"
                else ->
                    "Applied ${verse.bookName} ${verse.chapter}:${verse.verseNumber} to the lock screen."
            }
            mutableUiState.value = mutableUiState.value.copy(
                selectedVerse = verse,
                isApplying = false,
                needsBackgroundImage = false,
                statusMessage = message,
                isError = applyError != null || historyError != null,
            )
        }
    }

    private fun errorMessage(exception: Exception): String =
        exception.message ?: exception.javaClass.simpleName

    private suspend fun saveHistory(
        verse: BibleVerse,
        mode: SelectionMode,
        selectedAt: Long,
        success: Boolean,
        errorMessage: String?,
    ) {
        withContext(Dispatchers.IO) {
            verseRepository.recordHistory(
                WallpaperUpdateResult(
                    id = UUID.randomUUID().toString(),
                    verseId = verse.id,
                    selectionMode = mode,
                    selectedAt = selectedAt,
                    appliedAt = if (success) System.currentTimeMillis() else null,
                    status = if (success) WallpaperApplyStatus.SUCCESS else WallpaperApplyStatus.FAILED,
                    errorCode = if (success) null else "WALLPAPER_APPLY_FAILED",
                    errorMessage = errorMessage,
                ),
            )
        }
    }
}

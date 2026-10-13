package com.versemoment.app.domain

import androidx.annotation.StringRes

public data class BibleTranslation(
    val id: String,
    val name: String,
    val abbreviation: String,
    val languageCode: String,
    val attribution: String,
    val licenseId: String,
)

public data class BibleVerse(
    val id: String,
    val translationId: String,
    val bookId: String,
    val bookName: String,
    val bookOrder: Int,
    val chapter: Int,
    val verseNumber: Int,
    val text: String,
    val tags: List<String> = emptyList(),
)

enum class SelectionMode {
    TIME_MATCH,
    DIVINE_DISCOVERY
}

enum class WallpaperApplyStatus {
    PENDING,
    SUCCESS,
    FAILED
}

data class WallpaperUpdateResult(
    val id: String,
    val verseId: String,
    val selectionMode: SelectionMode,
    val selectedAt: Long,
    val appliedAt: Long?,
    val status: WallpaperApplyStatus,
    val errorCode: String?,
    val errorMessage: String?,
)

data class WallpaperSettings(
    val updateIntervalMinutes: Int = 5,
    val lockScreenOnly: Boolean = true,
    val timeMatchEnabled: Boolean = true,
    val divineDiscoveryEnabled: Boolean = true,
    val selectedTranslationId: String,
    val selectedThemeId: String,
    val repetitionAvoidanceEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartMinute: Int = 22 * 60,
    val quietHoursEndMinute: Int = 7 * 60,
)

data class AutomationStatus(
    val enabled: Boolean,
    val requestedIntervalMinutes: Int,
    val lastAttemptAt: Long?,
    val lastSuccessfulUpdateAt: Long?,
    val nextExpectedUpdateAt: Long?,
    val schedulingPermissionAvailable: Boolean,
    val diagnosticMessage: String?,
)

enum class WallpaperTheme(val id: String, val label: String) {
    CINEMATIC_NATURE("cinematic_nature", "Cinematic Nature"),
    MINIMALIST_DARK("minimalist_dark", "Minimalist Dark"),
    WARM_NEUTRAL("warm_neutral", "Warm Neutral"),
    PEACEFUL_LANDSCAPE("peaceful_landscape", "Peaceful Landscape"),
}

data class ScreenState(
    val title: String,
    val subtitle: String,
    val primaryActionLabel: String,
    val showSecondary: Boolean = false,
)

data class NavigationItem(
    val id: String,
    val label: String,
    val route: String,
    val icon: String,
)

enum class AppAppearance {
    LIGHT,
    DARK,
    SYSTEM,
}

data class VerseSelectionResult(
    val verse: BibleVerse,
    val mode: SelectionMode,
    val candidateReference: String?,
    val reason: String,
    val repetitionRelaxed: Boolean = false,
    val selectedAt: Long,
)

data class ThemeSpec(
    val id: String,
    val name: String,
    val accent: String,
    val background: String,
    val surface: String,
    val text: String,
    val mutedText: String,
)

data class WallpaperMetrics(
    val width: Int,
    val height: Int,
    val density: Int,
)

data class SelectionCandidate(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verseNumber: Int,
    val reference: String,
)

data class UserPreference(
    val key: String,
    val value: String,
)

data class UiMessage(
    val text: String,
    val type: MessageType = MessageType.INFO,
)

enum class MessageType {
    INFO,
    SUCCESS,
    ERROR,
}

sealed class ResultState<out T> {
    data object Idle : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
}

public data class FeatureDefinition(
    val title: String,
    val summary: String,
    val route: String,
    val icon: Int,
)

public data class VerseSearchFilter(
    val query: String = "",
    val theme: String? = null,
    val translationId: String? = null,
)

public data class ThemeColorToken(
    val id: String,
    val value: String,
)

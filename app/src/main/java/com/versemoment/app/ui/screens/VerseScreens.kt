@file:OptIn(ExperimentalMaterial3Api::class)

package com.versemoment.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.ui.WallpaperViewModel
import java.text.DateFormat
import java.util.Date
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

private val sampleVerses = listOf(
    BibleVerse(
        id = "jhn_8_12",
        translationId = "kjv",
        bookId = "john",
        bookName = "John",
        bookOrder = 43,
        chapter = 8,
        verseNumber = 12,
        text = "Then spake Jesus again unto them, saying, I am the light of the world: he that followeth me shall not walk in darkness, but shall have the light of life.",
        tags = listOf("light", "hope", "guidance"),
    ),
    BibleVerse(
        id = "ps_23_1",
        translationId = "kjv",
        bookId = "psalms",
        bookName = "Psalms",
        bookOrder = 19,
        chapter = 23,
        verseNumber = 1,
        text = "The LORD is my shepherd; I shall not want.",
        tags = listOf("peace", "comfort", "trust"),
    ),
    BibleVerse(
        id = "php_4_13",
        translationId = "kjv",
        bookId = "philippians",
        bookName = "Philippians",
        bookOrder = 50,
        chapter = 4,
        verseNumber = 13,
        text = "I can do all things through Christ which strengtheneth me.",
        tags = listOf("strength", "discipline", "faith"),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, wallpaperViewModel: WallpaperViewModel) {
    val uiState by wallpaperViewModel.uiState.collectAsState()
    val verse = uiState.selectedVerse ?: sampleVerses.first()
    var favorite by remember { mutableStateOf(true) }
    WallpaperBackgroundPicker(wallpaperViewModel, uiState.needsBackgroundImage)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("VerseMoment") },
            actions = {
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Open settings")
                }
            },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF172235),
                                    Color(0xFF101827),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "\"${verse.text}\"",
                            color = Color(0xFFF7F3EA),
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV",
                            color = Color(0xFFD8B46A),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Selection mode", style = MaterialTheme.typography.labelMedium)
                    Text("TimeMatch", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = { navController.navigate("studio") }) {
                    Text("Wallpaper Studio")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = wallpaperViewModel::refreshWallpaper,
                    enabled = !uiState.isLoading && !uiState.isApplying,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(if (uiState.isApplying) "Applying…" else "Refresh")
                }
                OutlinedButton(onClick = { favorite = !favorite }) {
                    if (favorite) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                    } else {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                    }
                    WallpaperStatus(uiState.statusMessage, uiState.isError)
                    Text(if (favorite) "Saved" else "Save")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Automation status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (uiState.automationEnabled) {
                            "Enabled • updates about every 5 minutes • lock screen only"
                        } else {
                            "Automatic updates paused"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Requested interval: 5 minutes")
                    Text(
                        uiState.lastAutomationSuccessAt?.let {
                            "Last automatic update: ${DateFormat.getDateTimeInstance().format(Date(it))}"
                        } ?: "No automatic wallpaper update has completed yet",
                    )
                    uiState.lastAutomationError?.let { error ->
                        Text(
                            "Last automatic update failed: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(navController: NavController) {
    val verses = sampleVerses
    Scaffold(topBar = {
        TopAppBar(title = { Text("Discover") })
    }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(verses) { verse ->
                VerseCard(verse = verse, onFavorite = {})
            }
        }
    }
}

@Composable
fun SavedScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("Saved Verses") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sampleVerses.forEach { verse ->
                VerseCard(verse = verse, onFavorite = {})
            }
        }
    }
}

@Composable
fun HistoryScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                "John 8:12 • KJV • Success • TimeMatch",
                "Psalms 23:1 • KJV • Success • Divine Discovery",
                "Philippians 4:13 • KJV • Failed • permission denied",
            ).forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(entry, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController, wallpaperViewModel: WallpaperViewModel) {
    val uiState by wallpaperViewModel.uiState.collectAsState()
    WallpaperBackgroundPicker(wallpaperViewModel, uiState.needsBackgroundImage)
    val context = LocalContext.current
    var notificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationPrompted by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsAllowed = granted
    }
    val notificationSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }
    val exactAlarmSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        wallpaperViewModel.refreshExactAlarmAccess()
    }
    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsAllowed &&
            !notificationPrompted
        ) {
            notificationPrompted = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var timeMatch by remember { mutableStateOf(true) }
    var divine by remember { mutableStateOf(true) }
    var quietHours by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingRow("Automation enabled", uiState.automationEnabled, wallpaperViewModel::setAutomationEnabled)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(
                    if (notificationsAllowed) {
                        "Notifications are allowed for update status and failure alerts."
                    } else {
                        "Notifications are off. Wallpaper updates can still run, but VerseMoment cannot show update alerts."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notificationsAllowed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                if (!notificationsAllowed) {
                    OutlinedButton(
                        onClick = {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    ) {
                        Text("Allow notifications")
                    }
                    OutlinedButton(
                        onClick = {
                            notificationSettings.launch(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        },
                    ) {
                        Text("Open notification settings")
                    }
                }
            }
            if (uiState.automationEnabled && !uiState.exactAlarmsAllowed) {
                Text(
                    "Android requires Alarms & reminders access for closer-to-5-minute updates. Even with access, updates can be deferred while the phone is idle.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = {
                        exactAlarmSettings.launch(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                ) {
                    Text("Open Alarms & reminders settings")
                }
            }
            SettingRow("TimeMatch", timeMatch) { timeMatch = it }
            SettingRow("Divine Discovery", divine) { divine = it }
            SettingRow("Quiet hours", quietHours) { quietHours = it }
            Button(onClick = { navController.navigate("studio") }) { Text("Wallpaper Studio") }
            Button(
                onClick = wallpaperViewModel::refreshWallpaper,
                enabled = !uiState.isLoading && !uiState.isApplying,
            ) {
                Text(if (uiState.isApplying) "Applying…" else "Manual refresh")
            }
            WallpaperStatus(uiState.statusMessage, uiState.isError)
        }
    }
}

@Composable
fun StudioScreen(navController: NavController, wallpaperViewModel: WallpaperViewModel) {
    val uiState by wallpaperViewModel.uiState.collectAsState()
    val selectedVerse = uiState.selectedVerse
    WallpaperBackgroundPicker(wallpaperViewModel, uiState.needsBackgroundImage)
    Scaffold(topBar = { TopAppBar(title = { Text("Wallpaper Studio") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF172235),
                                Color(0xFF101827),
                            ),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = Color(0xD0101827),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = selectedVerse?.text ?: if (uiState.isLoading) "Loading verses…" else "No verse selected",
                            color = Color(0xFFF7F3EA),
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        selectedVerse?.let { verse ->
                            Text(
                                text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV",
                                color = Color(0xFFF1D18B),
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            Text(
                "Android may block apps from reading the current lock-screen image. If it does, choose a background below; the system image picker grants access without broad storage permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Choose a verse", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${uiState.verses.size} verses available • King James Version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = wallpaperViewModel::chooseAnotherVerse,
                    enabled = !uiState.isApplying && !uiState.isLoading && uiState.verses.size > 1,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Pick another")
                }
            }
            if (uiState.isLoading) {
                Text("Loading your offline verse collection…")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.verses, key = { it.id }) { verse ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wallpaperViewModel.selectVerse(verse) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (verse.id == selectedVerse?.id) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    verse.text,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = wallpaperViewModel::applySelectedVerse,
                enabled = selectedVerse != null && !uiState.isApplying && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isApplying) "Applying…" else "Apply to lock screen")
            }
            OutlinedButton(
                onClick = wallpaperViewModel::requestBackgroundImage,
                enabled = !uiState.isLoading && !uiState.isApplying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose a different background image")
            }
            WallpaperStatus(uiState.statusMessage, uiState.isError)
        }
    }
}

@Composable
private fun WallpaperBackgroundPicker(
    wallpaperViewModel: WallpaperViewModel,
    requested: Boolean,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            wallpaperViewModel.backgroundImageSelectionCancelled()
        } else {
            wallpaperViewModel.applySelectedVerseWithBackground(uri)
        }
    }
    LaunchedEffect(requested) {
        if (requested) picker.launch(arrayOf("image/*"))
    }
}

@Composable
private fun WallpaperStatus(message: String?, isError: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun VerseCard(verse: BibleVerse, onFavorite: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "${verse.bookName} ${verse.chapter}:${verse.verseNumber}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "\"${verse.text}\"",
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("KJV", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onFavorite) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite verse")
                }
            }
        }
    }
}

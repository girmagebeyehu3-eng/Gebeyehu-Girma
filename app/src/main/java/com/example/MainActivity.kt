package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.BoomTopBar
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.screens.BuzzScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.*

enum class BottomTab {
    MUSIC, BUZZ, LIBRARY, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                BoomPlayApp()
            }
        }
    }
}

@Composable
fun BoomPlayApp() {
    val context = LocalContext.current
    val repository = remember { MusicRepository(context) }
    val playerManager = remember { PlayerManager.getInstance(context) }

    val tracks by repository.tracks.collectAsState()
    val buzzPosts by repository.buzzPosts.collectAsState()
    val searchQuery by repository.searchQuery.collectAsState()
    val selectedGenre by repository.selectedGenre.collectAsState()

    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()

    var currentTab by remember { mutableStateOf(BottomTab.MUSIC) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Request notification permission for background playback controls on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Set repository playlist into playerManager
    LaunchedEffect(tracks) {
        if (tracks.isNotEmpty()) {
            playerManager.setPlaylist(tracks)
            if (currentTrack == null) {
                // Pre-select first trending track for user convenience
                playerManager.playTrack(tracks.first(), tracks)
                playerManager.pause() // Keep paused initially until user taps play
            }
        }
    }

    // Handle back button when full screen player or sub-tabs are open
    BackHandler(enabled = isNowPlayingExpanded || currentTab != BottomTab.MUSIC) {
        if (isNowPlayingExpanded) {
            isNowPlayingExpanded = false
        } else if (currentTab != BottomTab.MUSIC) {
            currentTab = BottomTab.MUSIC
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            if (!isNowPlayingExpanded && currentTab != BottomTab.SETTINGS) {
                BoomTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { repository.setSearchQuery(it) },
                    onEqualizerClick = {
                        isNowPlayingExpanded = true
                    },
                    onSettingsClick = {
                        currentTab = BottomTab.SETTINGS
                    },
                    onVipClick = {
                        currentTab = BottomTab.SETTINGS
                    }
                )
            }
        },
        bottomBar = {
            if (!isNowPlayingExpanded) {
                Column {
                    // Floating Mini-Player docked right above bottom navigation bar
                    if (currentTrack != null) {
                        MiniPlayer(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onPlayPauseClick = { playerManager.togglePlayPause() },
                            onSkipNextClick = { playerManager.skipNext() },
                            onPlayerClick = { isNowPlayingExpanded = true }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = (currentTab == BottomTab.MUSIC),
                            onClick = { currentTab = BottomTab.MUSIC },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BottomTab.MUSIC) Icons.Default.MusicNote else Icons.Outlined.MusicNote,
                                    contentDescription = "Music"
                                )
                            },
                            label = { Text("Music", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BoomCyan,
                                selectedTextColor = BoomCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = BoomCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_music")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BottomTab.BUZZ),
                            onClick = { currentTab = BottomTab.BUZZ },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BottomTab.BUZZ) Icons.Default.Forum else Icons.Outlined.Forum,
                                    contentDescription = "Buzz"
                                )
                            },
                            label = { Text("Buzz", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BoomCyan,
                                selectedTextColor = BoomCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = BoomCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_buzz")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BottomTab.LIBRARY),
                            onClick = { currentTab = BottomTab.LIBRARY },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BottomTab.LIBRARY) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BoomCyan,
                                selectedTextColor = BoomCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = BoomCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_library")
                        )

                        NavigationBarItem(
                            selected = (currentTab == BottomTab.SETTINGS),
                            onClick = { currentTab = BottomTab.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == BottomTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BoomCyan,
                                selectedTextColor = BoomCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = BoomCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                BottomTab.MUSIC -> {
                    HomeScreen(
                        tracks = tracks,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { repository.setSearchQuery(it) },
                        selectedGenre = selectedGenre,
                        onSelectGenre = { repository.setSelectedGenre(it) },
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks)
                            isNowPlayingExpanded = true
                        },
                        onToggleFavorite = { repository.toggleFavorite(it) },
                        onDownloadClick = { repository.startDownload(it) },
                        onNavigateToDownloads = { currentTab = BottomTab.LIBRARY }
                    )
                }
                BottomTab.BUZZ -> {
                    BuzzScreen(
                        posts = buzzPosts,
                        tracks = tracks,
                        onLikePost = { repository.toggleLikeBuzzPost(it) },
                        onAddPost = { author, content, trackTitle, artist ->
                            repository.addBuzzPost(author, content, trackTitle, artist)
                        },
                        onPlayTrackByName = { title ->
                            val found = tracks.firstOrNull { it.title.equals(title, ignoreCase = true) }
                            if (found != null) {
                                playerManager.playTrack(found, tracks)
                                isNowPlayingExpanded = true
                            }
                        }
                    )
                }
                BottomTab.LIBRARY -> {
                    LibraryScreen(
                        tracks = tracks,
                        initialTab = "Downloaded",
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks)
                            isNowPlayingExpanded = true
                        },
                        onToggleFavorite = { repository.toggleFavorite(it) },
                        onDownloadClick = { repository.startDownload(it) },
                        onClearDownloads = { repository.clearAllDownloads() }
                    )
                }
                BottomTab.SETTINGS -> {
                    SettingsScreen(
                        onClearAllDownloads = { repository.clearAllDownloads() },
                        onBack = { currentTab = BottomTab.MUSIC }
                    )
                }
            }
        }
    }

    // Full-Screen Now-Playing Experience
    AnimatedVisibility(
        visible = isNowPlayingExpanded && currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        currentTrack?.let { track ->
            NowPlayingSheet(
                track = track,
                playerManager = playerManager,
                onDismiss = { isNowPlayingExpanded = false },
                onToggleFavorite = { repository.toggleFavorite(it) },
                onDownloadClick = { repository.startDownload(it) }
            )
        }
    }
}

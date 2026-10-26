package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricLine
import com.example.model.Track
import com.example.playback.PlayerManager
import com.example.playback.RepeatMode
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    track: Track,
    playerManager: PlayerManager,
    onDismiss: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val equalizerPreset by playerManager.equalizerPreset.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Cover, 1: Lyrics, 2: EQ
    var showLyricShareDialog by remember { mutableStateOf(false) }
    var selectedQuoteForShare by remember { mutableStateOf("") }
    var selectedAmharicQuoteForShare by remember { mutableStateOf("") }
    var lyricsLanguage by remember { mutableStateOf("BOTH") } // "BOTH", "AMHARIC", "ENGLISH"
    var showSleepTimerMenu by remember { mutableStateOf(false) }

    // Vinyl Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_turn")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    // Synchronized Lyrics active line calculation
    val activeLyricIndex = remember(currentPositionMs, track.lyrics) {
        val idx = track.lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    val lyricsListState = rememberLazyListState()

    // Auto scroll lyrics to active line
    LaunchedEffect(activeLyricIndex, activeTab) {
        if (activeTab == 1 && track.lyrics.isNotEmpty()) {
            val targetScroll = (activeLyricIndex - 2).coerceAtLeast(0)
            lyricsListState.animateScrollToItem(targetScroll)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Navigation & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("collapse_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        color = BoomCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.album,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = { SocialShareHelper.shareTrack(context, track) },
                        modifier = Modifier.testTag("share_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Track",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showSleepTimerMenu = true },
                        modifier = Modifier.testTag("timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Sleep Timer",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Tabs Selector: Vinyl Cover | Synced Lyrics | Boom Boost EQ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceVariant
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        listOf("Cover", "Lyrics", "Boom Boost").forEachIndexed { index, label ->
                            val isSelected = (activeTab == index)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) BoomCyan else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { activeTab = index }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main View Area based on Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> {
                        // VINYL DISC COVER VIEW
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF15181E),
                                                Color(0xFF232A35),
                                                Color(0xFF15181E),
                                                Color(0xFF2E3746),
                                                Color(0xFF15181E)
                                            )
                                        )
                                    )
                                    .rotate(if (isPlaying) angle else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer grooves
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                )

                                // Center Label
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(track.gradientStartHex),
                                                    Color(track.gradientEndHex)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "GEBEYEHU",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = track.genre,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Center Spindle Hole
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(DarkBg)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Audio EQ mini visualizer bars when playing
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { barIndex ->
                                    val barHeight by infiniteTransition.animateFloat(
                                        initialValue = 6f,
                                        targetValue = if (isPlaying) (12 + (barIndex * 7) % 24).toFloat() else 6f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(250 + barIndex * 90, easing = FastOutSlowInEasing),
                                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                        ),
                                        label = "bar_$barIndex"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isPlaying) BoomCyan else TextSecondary)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$equalizerPreset • ${playbackSpeed}x",
                                    color = BoomCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    1 -> {
                        // SYNCHRONIZED LYRICS VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("synchronized_lyrics_view")
                        ) {
                            // Language Mode Selector & Share Button Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Language Selector Pills: Both | Amharic | English
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurfaceVariant
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        val langs = listOf(
                                            Triple("BOTH", "ሁለቱም", "Both"),
                                            Triple("AMHARIC", "አማርኛ", "Amharic"),
                                            Triple("ENGLISH", "English", "EN")
                                        )
                                        langs.forEach { (code, label, sub) ->
                                            val isSelected = (lyricsLanguage == code)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) BoomCyan else Color.Transparent,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { lyricsLanguage = code }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BoomCyan.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val currentLine = track.lyrics.getOrNull(activeLyricIndex)
                                            selectedQuoteForShare = currentLine?.text ?: track.title
                                            selectedAmharicQuoteForShare = currentLine?.amharicText?.ifEmpty { currentLine.translation } ?: ""
                                            showLyricShareDialog = true
                                        }
                                        .testTag("quote_share_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = null,
                                            tint = BoomCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Share Card",
                                            color = BoomCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "👆 Tap any line to seek • Amharic & English synced",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                itemsIndexed(track.lyrics) { index, line ->
                                    val isActive = (index == activeLyricIndex)
                                    val isPassed = (index < activeLyricIndex)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                playerManager.seekTo(line.timeMs)
                                            }
                                            .background(if (isActive) BoomCyan.copy(alpha = 0.08f) else Color.Transparent)
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        when (lyricsLanguage) {
                                            "AMHARIC" -> {
                                                // Amharic Only (Ge'ez Script)
                                                val amharic = line.amharicText.ifEmpty { line.translation.ifEmpty { line.text } }
                                                Text(
                                                    text = amharic,
                                                    color = when {
                                                        isActive -> BoomCyan
                                                        isPassed -> TextPrimary
                                                        else -> TextSecondary.copy(alpha = 0.45f)
                                                    },
                                                    fontSize = if (isActive) 20.sp else 16.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = if (isActive) 28.sp else 23.sp
                                                )
                                            }
                                            "ENGLISH" -> {
                                                // English Only
                                                Text(
                                                    text = line.text,
                                                    color = when {
                                                        isActive -> BoomCyan
                                                        isPassed -> TextPrimary
                                                        else -> TextSecondary.copy(alpha = 0.45f)
                                                    },
                                                    fontSize = if (isActive) 19.sp else 15.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = if (isActive) 26.sp else 22.sp
                                                )
                                            }
                                            else -> {
                                                // Bilingual: Both English & Amharic
                                                Text(
                                                    text = line.text,
                                                    color = when {
                                                        isActive -> BoomCyan
                                                        isPassed -> TextPrimary
                                                        else -> TextSecondary.copy(alpha = 0.5f)
                                                    },
                                                    fontSize = if (isActive) 18.sp else 14.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 24.sp
                                                )
                                                val amharic = line.amharicText.ifEmpty { line.translation }
                                                if (amharic.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = amharic,
                                                        color = if (isActive) BoomCoral else TextSecondary.copy(alpha = 0.45f),
                                                        fontSize = if (isActive) 15.sp else 13.sp,
                                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 20.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // BOOM BOOST EQUALIZER VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("boom_boost_eq_panel"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Boom Sound Engine",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hardware-tuned sound enhancement & bass enhancement",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Presets
                            val presets = listOf("Boom Boost", "Super Bass", "Vocal", "Club", "3D Surround", "Flat")
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.chunked(2).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPresets.forEach { preset ->
                                            val isSelected = (preset == equalizerPreset)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { playerManager.setEqualizerPreset(preset) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) BoomCyan.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BoomCyan) else null
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.GraphicEq,
                                                        contentDescription = null,
                                                        tint = if (isSelected) BoomCyan else TextSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = preset,
                                                        color = if (isSelected) BoomCyan else TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Playback Speed Selector
                            Text(
                                text = "Playback Speed",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                    val isSelected = (speed == playbackSpeed)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) BoomCoral else DarkSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { playerManager.setPlaybackSpeed(speed) }
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Track Title, Artist, & Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Download Button
                    IconButton(
                        onClick = { onDownloadClick(track.id) },
                        modifier = Modifier.testTag("download_track_button")
                    ) {
                        if (track.isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded Offline",
                                tint = BoomCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "Download Song Offline",
                                tint = TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.testTag("favorite_track_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) BoomCoral else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Seekbar Progress Slider
            val progress = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { newFrac ->
                        val targetMs = (newFrac * durationMs).toLong()
                        playerManager.seekTo(targetMs)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = BoomCyan,
                        activeTrackColor = BoomCyan,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Playback Controls: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = { playerManager.toggleShuffle() },
                    modifier = Modifier.testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) BoomCyan else TextSecondary
                    )
                }

                // Previous
                IconButton(
                    onClick = { playerManager.skipPrevious() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause (Large Boom button)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BoomCyan, Color(0xFF00B4D8))
                            )
                        )
                        .clickable { playerManager.togglePlayPause() }
                        .testTag("main_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { playerManager.skipNext() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = { playerManager.toggleRepeat() },
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.OFF -> Icons.Outlined.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) BoomCyan else TextSecondary
                    )
                }
            }
        }
    }

    // Lyric Quote Share Dialog
    if (showLyricShareDialog) {
        LyricShareDialog(
            track = track,
            initialQuote = selectedQuoteForShare,
            amharicQuote = selectedAmharicQuoteForShare,
            onDismiss = { showLyricShareDialog = false }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerMenu) {
        AlertDialog(
            onDismissRequest = { showSleepTimerMenu = false },
            title = { Text("Sleep Timer", color = TextPrimary) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick = {
                                showSleepTimerMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$mins minutes", color = BoomCyan, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerMenu = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

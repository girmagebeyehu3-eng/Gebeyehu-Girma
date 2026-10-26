package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.*

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    initialTab: String = "Downloaded",
    onTrackClick: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    onClearDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    val filteredTracks = remember(tracks, selectedTab) {
        when (selectedTab) {
            "Downloaded" -> tracks.filter { it.isDownloaded }
            "Favorites" -> tracks.filter { it.isFavorite }
            else -> tracks
        }
    }

    val totalDownloadedSize = remember(tracks) {
        val downloaded = tracks.filter { it.isDownloaded }
        downloaded.sumOf { it.fileSizeMb }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "My Music Library",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline downloads, saved playlists & favorites",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Offline Storage Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("offline_storage_banner"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = BoomCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline Storage Used",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f MB (Ready for airplane/offline mode)", totalDownloadedSize),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (totalDownloadedSize > 0) {
                        TextButton(onClick = onClearDownloads) {
                            Text("Clear", color = BoomCoral, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Tabs: Downloaded (Offline) | Favorites | All Songs
        item {
            val tabs = listOf("Downloaded", "Favorites", "All Tracks")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { tab ->
                    val isSelected = (tab == selectedTab)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) BoomCyan else DarkSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedTab = tab }
                            .testTag("library_tab_$tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (tab == "Downloaded") {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else BoomCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (tab == "Favorites") {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else BoomCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = tab,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Play All / Shuffle Row
        if (filteredTracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTracks.size} Tracks",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Button(
                        onClick = {
                            if (filteredTracks.isNotEmpty()) {
                                onTrackClick(filteredTracks.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BoomCyan),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play All",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            items(filteredTracks) { track ->
                TrackListItem(
                    track = track,
                    rank = tracks.indexOf(track) + 1,
                    onClick = { onTrackClick(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onDownloadClick = { onDownloadClick(track.id) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No tracks found in $selectedTab",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

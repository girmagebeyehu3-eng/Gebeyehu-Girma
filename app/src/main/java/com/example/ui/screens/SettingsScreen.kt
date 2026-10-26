package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeveloperContact
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    onClearAllDownloads: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bassBoostEnabled by remember { mutableStateOf(true) }
    var autoDownloadWifi by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun openDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard("Phone", phone)
        }
    }

    fun openUrl(url: String, fallbackText: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard("Link", fallbackText)
        }
    }

    fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Gebeyehu Player Support & Feedback")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard("Email", email)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account & Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Account Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("account_profile_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(BoomCyan, BoomCoral))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gebeyehu Girma Sintayehu",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = DeveloperContact.EMAIL,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BoomGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "★ VIP LIFETIME MEMBER",
                                color = BoomGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showVipDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Badge",
                            tint = BoomGold
                        )
                    }
                }
            }
        }

        // Contact Address & User Support Section
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "User Support & Contact Address",
                color = BoomCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = DeveloperContact.SUPPORT_DESCRIPTION,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("support_contacts_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Phone
                    ContactItemRow(
                        icon = Icons.Default.Phone,
                        iconTint = Color(0xFF00E676),
                        title = "Phone / Support Call",
                        detail = DeveloperContact.PHONE_DISPLAY,
                        actionLabel = "Call",
                        onAction = { openDialer(DeveloperContact.PHONE_RAW) },
                        onCopy = { copyToClipboard("Phone Number", DeveloperContact.PHONE_RAW) }
                    )

                    HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)

                    // Telegram
                    ContactItemRow(
                        icon = Icons.Default.Send,
                        iconTint = Color(0xFF00B0FF),
                        title = "Telegram Channel & Chat",
                        detail = DeveloperContact.TELEGRAM_HANDLE,
                        actionLabel = "Open",
                        onAction = { openUrl(DeveloperContact.TELEGRAM_URL, DeveloperContact.TELEGRAM_HANDLE) },
                        onCopy = { copyToClipboard("Telegram Handle", DeveloperContact.TELEGRAM_HANDLE) }
                    )

                    HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)

                    // TikTok
                    ContactItemRow(
                        icon = Icons.Default.MusicVideo,
                        iconTint = Color(0xFFFF4081),
                        title = "TikTok Profile & Clips",
                        detail = DeveloperContact.TIKTOK_HANDLE,
                        actionLabel = "Follow",
                        onAction = { openUrl(DeveloperContact.TIKTOK_URL, DeveloperContact.TIKTOK_HANDLE) },
                        onCopy = { copyToClipboard("TikTok Handle", DeveloperContact.TIKTOK_HANDLE) }
                    )

                    HorizontalDivider(color = DarkCardBorder, thickness = 0.8.dp)

                    // Email
                    ContactItemRow(
                        icon = Icons.Default.Email,
                        iconTint = BoomCoral,
                        title = "Official Support Email",
                        detail = DeveloperContact.EMAIL,
                        actionLabel = "Email",
                        onAction = { openEmail(DeveloperContact.EMAIL) },
                        onCopy = { copyToClipboard("Email", DeveloperContact.EMAIL) }
                    )
                }
            }
        }

        // Music & Audio Preferences
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Audio & Playback Settings",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Boom Bass Boost (Signature)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enhance low-end punch for Afrobeats & Amapiano",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = bassBoostEnabled,
                            onCheckedChange = { bassBoostEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = BoomCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Download Over Wi-Fi Only",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Save cellular mobile data when caching tracks",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = autoDownloadWifi,
                            onCheckedChange = { autoDownloadWifi = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = BoomCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClearAllDownloads()
                                Toast.makeText(context, "Offline cache cleared", Toast.LENGTH_SHORT).show()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Clear Offline Cache",
                                color = BoomCoral,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Frees internal storage occupied by downloaded audio",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Cache",
                            tint = BoomCoral
                        )
                    }
                }
            }
        }

        // REQUIRED DEVELOPER STATEMENT & NOTICE (PROMINENT AT BOTTOM)
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_attribution_bottom_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, BoomCyan.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(BoomCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Developer",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Explicitly required line:
                    Text(
                        text = DeveloperContact.DEVELOPER_CREDIT,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Gebeyehu Player v1.0 • Built with Kotlin, Jetpack Compose, Synchronized Lyrics Engine & Background Playback Service",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { openDialer(DeveloperContact.PHONE_RAW) },
                            label = { Text("Call Support", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                        AssistChip(
                            onClick = { openEmail(DeveloperContact.EMAIL) },
                            label = { Text("Email Dev", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = { showVipDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BoomGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gebeyehu Player VIP Status", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "You have Lifetime VIP Access unlocked!\n\n• Unlimited offline downloads\n• Synchronized lyrics on all tracks\n• Full Boom Boost Equalizer\n• Background playback without interruptions",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showVipDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BoomCyan)
                ) {
                    Text("Enjoy Music", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun ContactItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = detail,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $title",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Button(
            onClick = onAction,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BoomCyan),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = actionLabel,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

package com.example.model

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String = "",
    val amharicText: String = ""
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val gradientStartHex: Long,
    val gradientEndHex: Long,
    val lyrics: List<LyricLine>,
    val playsCount: String = "1.2M",
    val audioPreset: String = "AFROBEATS",
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val fileSizeMb: Double = 5.4,
    val localFilePath: String? = null
)

data class BuzzPost(
    val id: String,
    val authorName: String,
    val handle: String,
    val avatarBgHex: Long,
    val timeAgo: String,
    val content: String,
    val trackTitle: String,
    val artistName: String,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val commentsCount: Int = 12,
    val sharesCount: Int = 5
)

object DeveloperContact {
    const val DEVELOPER_NAME = "Gebeyehu Girma Sintayehu"
    const val PHONE_RAW = "+251921970129"
    const val PHONE_DISPLAY = "+251 921 970 129"
    const val TELEGRAM_HANDLE = "@GebeyehuGirma"
    const val TELEGRAM_URL = "https://t.me/GebeyehuGirma"
    const val TIKTOK_HANDLE = "@gebeyehugirma"
    const val TIKTOK_URL = "https://www.tiktok.com/@gebeyehugirma"
    const val EMAIL = "0921970129g@gmail.com"
    const val DEVELOPER_CREDIT = "This app is Developed by Gebeyehu Girma Sintayehu"
    const val SUPPORT_DESCRIPTION = "For inquiries, feature suggestions, copyright issues, or custom mobile application development, reach out directly via any of the channels below."
}

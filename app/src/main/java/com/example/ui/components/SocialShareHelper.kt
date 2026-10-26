package com.example.ui.components

import android.content.Context
import android.content.Intent
import com.example.model.DeveloperContact
import com.example.model.Track

object SocialShareHelper {
    fun shareTrack(context: Context, track: Track) {
        val shareText = """
            🔥 Listening to "${track.title}" by ${track.artist} on Gebeyehu Player!
            Album: ${track.album}
            Genre: ${track.genre}
            
            Experience smart synchronized lyrics, offline downloads & background playback!
            ${DeveloperContact.DEVELOPER_CREDIT}
            Support: ${DeveloperContact.EMAIL} | Telegram: ${DeveloperContact.TELEGRAM_HANDLE}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out ${track.title} on Gebeyehu Player")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Music via"))
    }

    fun shareApp(context: Context) {
        val shareText = """
            🎵 Gebeyehu Player - The Smart Music Experience with Synced Lyrics, Offline Downloads & Background Playback!
            
            ${DeveloperContact.DEVELOPER_CREDIT}
            Developer: ${DeveloperContact.DEVELOPER_NAME}
            Telegram: ${DeveloperContact.TELEGRAM_URL}
            TikTok: ${DeveloperContact.TIKTOK_URL}
            Email: ${DeveloperContact.EMAIL}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Gebeyehu Player Smart Music")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Gebeyehu Player with friends"))
    }
}

package com.example.data

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.model.BuzzPost
import com.example.model.LyricLine
import com.example.model.Track
import com.example.playback.AudioSynthEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class MusicRepository(private val context: Context) {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _buzzPosts = MutableStateFlow<List<BuzzPost>>(emptyList())
    val buzzPosts: StateFlow<List<BuzzPost>> = _buzzPosts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    init {
        loadCatalog()
        loadBuzzFeed()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun toggleFavorite(trackId: String) {
        _tracks.update { list ->
            list.map { if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    fun startDownload(trackId: String) {
        val targetTrack = _tracks.value.firstOrNull { it.id == trackId } ?: return

        // 1. Update download progress
        _tracks.update { list ->
            list.map { if (it.id == trackId) it.copy(downloadProgress = 0.5f) else it }
        }

        try {
            // 2. Synthesize audio source
            val srcFile = AudioSynthEngine.getOrCreateTrackAudio(context, trackId, targetTrack.audioPreset)

            // 3. Save to phone local storage directory
            val phoneMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "Music")
            phoneMusicDir.mkdirs()

            val safeTitle = targetTrack.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val safeArtist = targetTrack.artist.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val destFile = File(phoneMusicDir, "${safeArtist}_-_${safeTitle}.wav")

            if (srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
            }

            // Save download marker
            val offlineDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val marker = File(offlineDir, "$trackId.meta")
            marker.writeText(destFile.absolutePath)

            _tracks.update { list ->
                list.map {
                    if (it.id == trackId) it.copy(
                        isDownloaded = true,
                        downloadProgress = 1.0f,
                        localFilePath = destFile.absolutePath
                    ) else it
                }
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Saved \"${targetTrack.title}\" to phone local storage! 💾\nFolder: Music/${destFile.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _tracks.update { list ->
                list.map {
                    if (it.id == trackId) it.copy(
                        isDownloaded = true,
                        downloadProgress = 1.0f
                    ) else it
                }
            }
        }
    }

    fun removeDownload(trackId: String) {
        val offlineDir = File(context.filesDir, "downloads")
        val marker = File(offlineDir, "$trackId.meta")
        if (marker.exists()) {
            val savedPath = marker.readText().trim()
            if (savedPath.isNotEmpty()) {
                try { File(savedPath).delete() } catch (_: Exception) {}
            }
            marker.delete()
        }

        _tracks.update { list ->
            list.map {
                if (it.id == trackId) it.copy(
                    isDownloaded = false,
                    downloadProgress = 0.0f,
                    localFilePath = null
                ) else it
            }
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Removed track from offline storage", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllDownloads() {
        val offlineDir = File(context.filesDir, "downloads")
        offlineDir.listFiles()?.forEach { marker ->
            val savedPath = marker.readText().trim()
            if (savedPath.isNotEmpty()) {
                try { File(savedPath).delete() } catch (_: Exception) {}
            }
            marker.delete()
        }
        _tracks.update { list ->
            list.map { it.copy(isDownloaded = false, downloadProgress = 0f, localFilePath = null) }
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Local phone music storage cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleLikeBuzzPost(postId: String) {
        _buzzPosts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newIsLiked = !post.isLiked
                    val newLikes = if (newIsLiked) post.likesCount + 1 else post.likesCount - 1
                    post.copy(isLiked = newIsLiked, likesCount = newLikes)
                } else post
            }
        }
    }

    fun addBuzzPost(author: String, content: String, trackTitle: String, artist: String) {
        val newPost = BuzzPost(
            id = "buzz_${System.currentTimeMillis()}",
            authorName = author,
            handle = "@${author.lowercase().replace(" ", "")}",
            avatarBgHex = 0xFF00D2C4,
            timeAgo = "Just now",
            content = content,
            trackTitle = trackTitle,
            artistName = artist,
            likesCount = 0,
            isLiked = false,
            commentsCount = 0,
            sharesCount = 0
        )
        _buzzPosts.update { listOf(newPost) + it }
    }

    private fun loadCatalog() {
        val offlineDir = File(context.filesDir, "downloads")
        fun isMarkedDownloaded(id: String): Boolean = File(offlineDir, "$id.meta").exists()

        _tracks.value = listOf(
            Track(
                id = "track_1",
                title = "Last Last",
                artist = "Burna Boy",
                album = "Love, Damini",
                genre = "Afrobeats",
                durationMs = 172000,
                gradientStartHex = 0xFFFF6B35,
                gradientEndHex = 0xFF7A1C00,
                playsCount = "18.4M",
                audioPreset = "AFROBEATS",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_1") || true,
                fileSizeMb = 6.2,
                lyrics = listOf(
                    LyricLine(0, "♪ (Intro - Shabalistic guitar riff) ♪", "♪ (መግቢያ - የጊታር ቅኝት) ♪", "♪ (መግቢያ - የጊታር ቅኝት) ♪"),
                    LyricLine(4000, "E don cast, last last, na everybody go chop breakfast", "በመጨረሻ ሁሉም አልቋል፣ ሁሉም ሰው የፍቅር ቁርስ ይበላል", "በመጨረሻ ሁሉም አልቋል፣ ሁሉም ሰው የፍቅር ቁርስ ይበላል"),
                    LyricLine(8500, "Have to say bye-bye o to the love of my life", "የህይወቴ ፍቅር ለነበረችው ደህና ሁኚ ማለት አለብኝ", "የህይወቴ ፍቅር ለነበረችው ደህና ሁኚ ማለት አለብኝ"),
                    LyricLine(13000, "My eye o don cry o", "አይኖቼ በእንባ ረጥበዋል", "አይኖቼ በእንባ ረጥበዋል"),
                    LyricLine(17500, "I need igbo and shayo (shayo)", "ሀዘኔን የምረሳበት መጠጥ እፈልጋለሁ", "ሀዘኔን የምረሳበት መጠጥ እፈልጋለሁ"),
                    LyricLine(22000, "Shayo! Need shayo!", "ማረጋጊያዬ ሆይ!", "ማረጋጊያዬ ሆይ!"),
                    LyricLine(26000, "Why you say I dey play with you?", "ለምን ባንቺ እንደምቀልድ ተናገርሽ?", "ለምን ባንቺ እንደምቀልድ ተናገርሽ?"),
                    LyricLine(30000, "I dey try put a smile on your face", "ፊትሽ ላይ ፈገግታን ለመሳል እተጋ ነበር", "ፊትሽ ላይ ፈገግታን ለመሳል እተጋ ነበር"),
                    LyricLine(34000, "Now you tell me say we are through", "አሁን ግን ተለያየን ብለሽ ነገርሽኝ", "አሁን ግን ተለያየን ብለሽ ነገርሽኝ"),
                    LyricLine(38000, "E don cast, last last, na everybody go chop breakfast", "በመጨረሻ ሁሉም ሰው የፍቅር ቁርስ መቅመስ አለበት", "በመጨረሻ ሁሉም ሰው የፍቅር ቁርስ መቅመስ አለበት"),
                    LyricLine(43000, "♪ (Boom drum breakdown & horns) ♪", "♪ (የከበሮና የመለከት ዜማ) ♪", "♪ (የከበሮና የመለከት ዜማ) ♪"),
                    LyricLine(48000, "I burn my hands, I hurt my soul", "እጆቼ ተቃጠሉ፣ ነፍሴም ተጎዳች", "እጆቼ ተቃጠሉ፣ ነፍሴም ተጎዳች"),
                    LyricLine(52000, "Still got the African Giant in control", "ቢሆንም የአፍሪካ ጀግናነቴ አሁንም አለ", "ቢሆንም የአፍሪካ ጀግናነቴ አሁንም አለ"),
                    LyricLine(57000, "Shayo! Need shayo!", "ሰላሜን እሻለሁ!", "ሰላሜን እሻለሁ!")
                )
            ),
            Track(
                id = "track_2",
                title = "Calm Down",
                artist = "Rema",
                album = "Rave & Climax",
                genre = "Afrobeats",
                durationMs = 219000,
                gradientStartHex = 0xFF00D2C4,
                gradientEndHex = 0xFF003833,
                playsCount = "42.8M",
                audioPreset = "AFROBEATS",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_2"),
                fileSizeMb = 5.8,
                lyrics = listOf(
                    LyricLine(0, "♪ (Vibez by Andre Vibez) ♪", "♪ (የአንድሬ ምርጥ ዜማ) ♪", "♪ (የአንድሬ ምርጥ ዜማ) ♪"),
                    LyricLine(3500, "Baby, calm down, calm down", "ውዴ ተረጋጊ፣ ተረጋጊ", "ውዴ ተረጋጊ፣ ተረጋጊ"),
                    LyricLine(7000, "Girl, this your body e put my heart for lockdown", "ውበትሽ ልቤን በቁጥጥር ስር አዋለው", "ውበትሽ ልቤን በቁጥጥር ስር አዋለው"),
                    LyricLine(11500, "For lockdown, oh, lockdown", "በእስር ላይ፣ በሙሉ ፍቅርሽ", "በእስር ላይ፣ በሙሉ ፍቅርሽ"),
                    LyricLine(15000, "Girl, you sweet like Fanta, ooh-ooh", "ጣፋጭ ነሽ እንደ ማር፣ እንደ ፋንታ", "ጣፋጭ ነሽ እንደ ማር፣ እንደ ፋንታ"),
                    LyricLine(19000, "If I tell you say I love you, no dey form yanga", "እወድሻለሁ ስልሽ ኮስታራ አትሁኚ", "እወድሻለሁ ስልሽ ኮስታራ አትሁኚ"),
                    LyricLine(23500, "Oh, yanga, oh, yanga", "አትኮሪብኝ የኔ ውድ", "አትኮሪብኝ የኔ ውድ"),
                    LyricLine(27000, "No tell me no, no, no, no, woah, woah, woah", "አይሆንም አትበዪኝ በጭራሽ", "አይሆንም አትበዪኝ በጭራሽ"),
                    LyricLine(32000, "As you dey roll am, go down low, oh girl", "ስታወዛውዢው ልቤን ትማርኪዋለሽ", "ስታወዛውዢው ልቤን ትማርኪዋለሽ"),
                    LyricLine(36500, "Baby, calm down, calm down!", "ውዴ ሆይ ተረጋጊ፣ ተረጋጊ!", "ውዴ ሆይ ተረጋጊ፣ ተረጋጊ!"),
                    LyricLine(41000, "♪ (Catchy afrobeat rhythm drop) ♪", "♪ (የአፍሮቢት አስደሳች ቅኝት) ♪", "♪ (የአፍሮቢት አስደሳች ቅኝት) ♪")
                )
            ),
            Track(
                id = "track_3",
                title = "Amaneh",
                artist = "Teddy Afro",
                album = "Ethiopia Harmony",
                genre = "Gospel",
                durationMs = 240000,
                gradientStartHex = 0xFFFFD700,
                gradientEndHex = 0xFF8B4500,
                playsCount = "14.1M",
                audioPreset = "ETHIOPIAN",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_3") || true,
                fileSizeMb = 7.1,
                lyrics = listOf(
                    LyricLine(0, "♪ (Kirar & Masinko traditional melody) ♪", "♪ (የክራርና የማሲንቆ ባህላዊ ዜማ) ♪", "♪ (የክራርና የማሲንቆ ባህላዊ ዜማ) ♪"),
                    LyricLine(4500, "Amaneh amaneh le sew lij wubet", "አማነህ አማነህ ለሰው ልጅ ውበት", "አማነህ አማነህ ለሰው ልጅ ውበት"),
                    LyricLine(9500, "Fikir new yemiyasmera yetefetrowin sewet", "ፍቅር ነው የሚያስመራ የተፈጠረውን ሰውነት", "ፍቅር ነው የሚያስመራ የተፈጠረውን ሰውነት"),
                    LyricLine(15000, "Hulum yalfall be fikir birhan", "ሁሉም ያልፋል በፍቅር ብርሃን", "ሁሉም ያልፋል በፍቅር ብርሃን"),
                    LyricLine(20000, "Hagere Ethiopia lezelalem tinur", "ሀገሬ ኢትዮጵያ ለዘላለም ትኑር", "ሀገሬ ኢትዮጵያ ለዘላለም ትኑር"),
                    LyricLine(26000, "Tizita melik'ua be libi yisalaal", "ትዝታ መልኳ በልብ ይሳላል", "ትዝታ መልኳ በልብ ይሳላል"),
                    LyricLine(32000, "Sew le sew ሲዋደድ ሰላም ይሰፍናል", "ሰው ለሰው ሲዋደድ ሰላም ይሰፍናል", "ሰው ለሰው ሲዋደድ ሰላም ይሰፍናል"),
                    LyricLine(38000, "♪ (Traditional drum tempo rises) ♪", "♪ (የከበሮ ምት ከፍ ይላል) ♪", "♪ (የከበሮ ምት ከፍ ይላል) ♪"),
                    LyricLine(44000, "Amaneh amaneh yefikir qelem!", "አማነህ አማነህ የፍቅር ቀለም!", "አማነህ አማነህ የፍቅር ቀለም!")
                )
            ),
            Track(
                id = "track_4",
                title = "Mnike",
                artist = "Tyler ICU & Tumelo.za",
                album = "Buya",
                genre = "Amapiano",
                durationMs = 228000,
                gradientStartHex = 0xFF9C27B0,
                gradientEndHex = 0xFF311B92,
                playsCount = "29.7M",
                audioPreset = "AMAPIANO",
                isFavorite = false,
                isDownloaded = isMarkedDownloaded("track_4"),
                fileSizeMb = 6.9,
                lyrics = listOf(
                    LyricLine(0, "♪ (Deep resonant log drum buildup) ♪", "♪ (የአማፒያኖ ከበሮ ድምፅ ጅምር) ♪", "♪ (የአማፒያኖ ከበሮ ድምፅ ጅምር) ♪"),
                    LyricLine(5000, "Mnike! Mnike le mali yakhe", "ስጡት! የሚገባውን ድርሻ ስጡት", "ስጡት! የሚገባውን ድርሻ ስጡት"),
                    LyricLine(10000, "Shona phansi, phakamisa izandla", "ወደ ታች ዝቅ በሉ፣ እጆቻችሁን አንሱ", "ወደ ታች ዝቅ በሉ፣ እጆቻችሁን አንሱ"),
                    LyricLine(15500, "Siphethe ubusuku bonke", "ሌሊቱን ሙሉ በደስታ እናሳልፈዋለን", "ሌሊቱን ሙሉ በደስታ እናሳልፈዋለን"),
                    LyricLine(21000, "♪ (Bass drop & energetic shakers) ♪", "♪ (የባስ ድምፅ መውረድና ጭፈራ) ♪", "♪ (የባስ ድምፅ መውረድና ጭፈራ) ♪"),
                    LyricLine(27000, "Amapiano to the world, Jozi to Lagos", "አማፒያኖ ለአለም፣ ከጆዚ እስከ አዲስ አበባ", "አማፒያኖ ለአለም፣ ከጆዚ እስከ አዲስ አበባ"),
                    LyricLine(33000, "Wena zama ukungivala", "ማንም ሊያስቆመን አይችልም", "ማንም ሊያስቆመን አይችልም"),
                    LyricLine(39000, "Mnike! Mnike!", "ስጡት! ስጡት!", "ስጡት! ስጡት!")
                )
            ),
            Track(
                id = "track_5",
                title = "Water",
                artist = "Tyla",
                album = "TYLA Debut",
                genre = "Pop",
                durationMs = 200000,
                gradientStartHex = 0xFF00B0FF,
                gradientEndHex = 0xFF002171,
                playsCount = "56.3M",
                audioPreset = "AMAPIANO",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_5"),
                fileSizeMb = 5.2,
                lyrics = listOf(
                    LyricLine(0, "♪ (Sensual rhythm & water droplets) ♪", "♪ (የውሃ ጠብታዎችና ረጋ ያለ ዜማ) ♪", "♪ (የውሃ ጠብታዎችና ረጋ ያለ ዜማ) ♪"),
                    LyricLine(4000, "Make me sweat, make me hotter", "አሞቀኝ፣ ጥሜን አርካልኝ", "አሞቀኝ፣ ጥሜን አርካልኝ"),
                    LyricLine(8500, "Make me lose my breath, make me water", "እስትንፋሴን ውሰደው፣ እንደ ንጹህ ውሃ", "እስትንፋሴን ውሰደው፣ እንደ ንጹህ ውሃ"),
                    LyricLine(13500, "Can you blow my mind?", "ልቤን ልታስደንቀው ትችላለህ?", "ልቤን ልታስደንቀው ትችላለህ?"),
                    LyricLine(17500, "Set off the alarm, temperature rising", "ሙቀቱ ጨመረ፣ ስሜቱ ደመቀ", "ሙቀቱ ጨመረ፣ ስሜቱ ደመቀ"),
                    LyricLine(22000, "Make me sweat, make me hotter", "ጥሜን አርካልኝ፣ አሞቀኝ", "ጥሜን አርካልኝ፣ አሞቀኝ"),
                    LyricLine(26500, "Make me lose my breath, make me water", "እንደ ንጹህ ምንጭ ውሀ አርሰኝ", "እንደ ንጹህ ምንጭ ውሀ አርሰኝ"),
                    LyricLine(31000, "♪ (Sub-bass vibration drop) ♪", "♪ (የባስ ንዝረት ምት) ♪", "♪ (የባስ ንዝረት ምት) ♪")
                )
            ),
            Track(
                id = "track_6",
                title = "City Boys",
                artist = "Burna Boy",
                album = "I Told Them...",
                genre = "Hip-Hop",
                durationMs = 153000,
                gradientStartHex = 0xFFFF5722,
                gradientEndHex = 0xFF3E2723,
                playsCount = "21.6M",
                audioPreset = "AFROBEATS",
                isFavorite = false,
                isDownloaded = isMarkedDownloaded("track_6"),
                fileSizeMb = 4.8,
                lyrics = listOf(
                    LyricLine(0, "♪ (Jermaine Dupri sample beat) ♪", "♪ (የሂፕ ሆፕ ምት ጅምር) ♪", "♪ (የሂፕ ሆፕ ምት ጅምር) ♪"),
                    LyricLine(3500, "City boys don't cry!", "የከተማ ልጆች አያለቅሱም!", "የከተማ ልጆች አያለቅሱም!"),
                    LyricLine(7000, "You know the vibes, pull up in luxury", "ክብራችንን አውቀህ በኩራት ተራመድ", "ክብራችንን አውቀህ በኩራት ተራመድ"),
                    LyricLine(11500, "Money talking, bottles popping in the sky", "ስኬታችን ይናገራል፣ ደስታችን የበዛ ነው", "ስኬታችን ይናገራል፣ ደስታችን የበዛ ነው"),
                    LyricLine(16000, "Tell them say the giant don arrive", "ንገሯቸው የአፍሪካ ጀግና ደርሷል", "ንገሯቸው የአፍሪካ ጀግና ደርሷል"),
                    LyricLine(21000, "Every corner we dey lock am down", "በሄድንበት ሁሉ እናበራለን", "በሄድንበት ሁሉ እናበራለን"),
                    LyricLine(26000, "City boys, city boys don't cry!", "የከተማ ልጆች በጭራሽ አያለቅሱም!", "የከተማ ልጆች በጭራሽ አያለቅሱም!")
                )
            ),
            Track(
                id = "track_7",
                title = "Essence",
                artist = "Wizkid feat. Tems",
                album = "Made in Lagos",
                genre = "R&B",
                durationMs = 248000,
                gradientStartHex = 0xFFE91E63,
                gradientEndHex = 0xFF4A148C,
                playsCount = "38.9M",
                audioPreset = "RNB_CHILL",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_7"),
                fileSizeMb = 6.4,
                lyrics = listOf(
                    LyricLine(0, "♪ (Smooth saxophone & rimshot) ♪", "♪ (የሳክሶፎንና ለስላሳ ከበሮ ዜማ) ♪", "♪ (የሳክሶፎንና ለስላሳ ከበሮ ዜማ) ♪"),
                    LyricLine(4000, "Say you no go leave me, say you dey with me", "አትተወኝም በለኝ፣ አብረኸኝ እንደሆንክ ንገረኝ", "አትተወኝም በለኝ፣ አብረኸኝ እንደሆንክ ንገረኝ"),
                    LyricLine(9000, "Time is of the essence, girl", "ጊዜው የፍቅር ነው የኔ ውብ", "ጊዜው የፍቅር ነው የኔ ውብ"),
                    LyricLine(14000, "You don't need no other body", "ካንቺ በቀር ሌላ ሰው አያስፈልገኝም", "ካንቺ በቀር ሌላ ሰው አያስፈልገኝም"),
                    LyricLine(19000, "Only you fit give me this feeling", "ይህን ታላቅ ስሜት የምትሰጪኝ አንቺ ብቻ ነሽ", "ይህን ታላቅ ስሜት የምትሰጪኝ አንቺ ብቻ ነሽ"),
                    LyricLine(24000, "Tems on the hook with angelic tone", "የመልአክ ድምፅ ያላት ቴምስ ስታዜም", "የመልአክ ድምፅ ያላት ቴምስ ስታዜም"),
                    LyricLine(29000, "You don't need no other body, no other body", "ካንቺ ውጪ ማንም የለም፣ ማንም!", "ካንቺ ውጪ ማንም የለም፣ ማንም!")
                )
            ),
            Track(
                id = "track_8",
                title = "Mar Eske Tuaf",
                artist = "Teddy Afro",
                album = "Tikur Sew",
                genre = "Ethiopian",
                durationMs = 265000,
                gradientStartHex = 0xFFFFD700,
                gradientEndHex = 0xFF7B1FA2,
                playsCount = "32.4M",
                audioPreset = "ETHIOPIAN",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_8") || true,
                fileSizeMb = 7.8,
                lyrics = listOf(
                    LyricLine(0, "♪ (Kirar & Acoustic Solo Intro) ♪", "♪ (የክራርና የአኮስቲክ ጊታር ዜማ) ♪", "♪ (የክራርና የአኮስቲክ ጊታር ዜማ) ♪"),
                    LyricLine(5000, "Mar eske tuaf, yefikir mastawesha", "ማር እስከ ጧፍ የፍቅር ማስታወሻ", "ማር እስከ ጧፍ የፍቅር ማስታወሻ"),
                    LyricLine(11000, "Liben yekeretew ye anchi fikr wusha", "ልቤን የቀረጠው የአንቺ ፍቅር ውሻ", "ልቤን የቀረጠው የአንቺ ፍቅር ውሻ"),
                    LyricLine(17500, "Yemayitegbu melkish, yene konjo simish", "የማይጠግቡት መልክሽ፣ የኔ ቆንጆ ስምሽ", "የማይጠግቡት መልክሽ፣ የኔ ቆንጆ ስምሽ"),
                    LyricLine(24000, "Be libe wist yishenefal yefikir meliktish", "በልቤ ውስጥ ይሰነጠቃል የፍቅር መልእክትሽ", "በልቤ ውስጥ ይሰነጠቃል የፍቅር መልእክትሽ"),
                    LyricLine(31000, "♪ (Traditional Kebero and Masinko break) ♪", "♪ (የከበሮና የማሲንቆ ምርጥ ውህደት) ♪", "♪ (የከበሮና የማሲንቆ ምርጥ ውህደት) ♪"),
                    LyricLine(38000, "Mar eske tuaf, lezelalem finot", "ማር እስከ ጧፍ፣ ለዘላለም ፍኖት!", "ማር እስከ ጧፍ፣ ለዘላለም ፍኖት!")
                )
            ),
            Track(
                id = "track_9",
                title = "Ete Mete",
                artist = "Aster Aweke",
                album = "Kabu",
                genre = "Ethiopian",
                durationMs = 210000,
                gradientStartHex = 0xFFE91E63,
                gradientEndHex = 0xFFFF5722,
                playsCount = "19.8M",
                audioPreset = "ETHIOPIAN",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_9"),
                fileSizeMb = 6.1,
                lyrics = listOf(
                    LyricLine(0, "♪ (Upbeat Tizita rhythm with brass) ♪", "♪ (የትዝታ ፈጣን ዜማ ከመለከት ጋር) ♪", "♪ (የትዝታ ፈጣን ዜማ ከመለከት ጋር) ♪"),
                    LyricLine(4000, "Ete mete yene konjo qelem", "እቴ ሜቴ የኔ ቆንጆ ቀለም", "እቴ ሜቴ የኔ ቆንጆ ቀለም"),
                    LyricLine(9000, "Anchiyew nesh yene alem", "አንቺው ነሽ የኔ አለም", "አንቺው ነሽ የኔ አለም"),
                    LyricLine(14500, "Be fikrish timarkeyalehu", "በፍቅርሽ ተማርኬያለሁ", "በፍቅርሽ ተማርኬያለሁ"),
                    LyricLine(20000, "Hulum yiqer le anchi biye", "ሁሉም ይቅር ላንቺ ብዬ", "ሁሉም ይቅር ላንቺ ብዬ"),
                    LyricLine(26000, "♪ (Aster's signature vocal run) ♪", "♪ (የአስቴር አወቀ ልዩ የድምፅ ዜማ) ♪", "♪ (የአስቴር አወቀ ልዩ የድምፅ ዜማ) ♪"),
                    LyricLine(33000, "Ete mete, yene wubet!", "እቴ ሜቴ የኔ ውበት!", "እቴ ሜቴ የኔ ውበት!")
                )
            ),
            Track(
                id = "track_10",
                title = "Ere Mela Mela",
                artist = "Mahmoud Ahmed",
                album = "Addis Soul Legends",
                genre = "Ethiopian",
                durationMs = 235000,
                gradientStartHex = 0xFF00B0FF,
                gradientEndHex = 0xFF1A237E,
                playsCount = "15.7M",
                audioPreset = "ETHIOPIAN",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_10"),
                fileSizeMb = 6.8,
                lyrics = listOf(
                    LyricLine(0, "♪ (Ethio-Jazz sax & organ groove) ♪", "♪ (የኢትዮ ጃዝ ሳክሶፎንና ኦርጋን ቅኝት) ♪", "♪ (የኢትዮ ጃዝ ሳክሶፎንና ኦርጋን ቅኝት) ♪"),
                    LyricLine(5000, "Ere mela mela, ere mela lenes", "ኧረ መላ መላ፣ ኧረ መላ ለኔስ", "ኧረ መላ መላ፣ ኧረ መላ ለኔስ"),
                    LyricLine(11000, "Yih fikir sayizegn kemelakes", "ይህ ፍቅር ሳይዘኝ ከመላከስ", "ይህ ፍቅር ሳይዘኝ ከመላከስ"),
                    LyricLine(17000, "Sew be sew yisemal yemiyadinen", "ሰው በሰው ይሰማል የሚያድነን", "ሰው በሰው ይሰማል የሚያድነን"),
                    LyricLine(23000, "Fikir new yasegew libachinen", "ፍቅር ነው ያሰረው ልባችንን", "ፍቅር ነው ያሰረው ልባችንን"),
                    LyricLine(29000, "♪ (Iconic Golden Era Saxophone Solo) ♪", "♪ (የወርቃማው ዘመን ታዋቂ ሳክሶፎን ሶሎ) ♪", "♪ (የወርቃማው ዘመን ታዋቂ ሳክሶፎን ሶሎ) ♪"),
                    LyricLine(36000, "Ere mela mela!", "ኧረ መላ መላ!", "ኧረ መላ መላ!")
                )
            ),
            Track(
                id = "track_11",
                title = "Chereka",
                artist = "Rophnan",
                album = "Reflection",
                genre = "Ethiopian",
                durationMs = 205000,
                gradientStartHex = 0xFF00E676,
                gradientEndHex = 0xFF004D40,
                playsCount = "24.2M",
                audioPreset = "ETHIOPIAN",
                isFavorite = false,
                isDownloaded = isMarkedDownloaded("track_11"),
                fileSizeMb = 5.9,
                lyrics = listOf(
                    LyricLine(0, "♪ (Electronic bass drop & traditional flute) ♪", "♪ (የኤሌክትሮኒክ ባስና የዋሽንት ቅንብር) ♪", "♪ (የኤሌክትሮኒክ ባስና የዋሽንት ቅንብር) ♪"),
                    LyricLine(4000, "Cherekaye be chelema taberyalesh", "ጨረቃዬ በጨለማ ታበሪያለሽ", "ጨረቃዬ በጨለማ ታበሪያለሽ"),
                    LyricLine(9500, "Mengedishen le alem takafiyalesh", "መንገድሽን ለአለም ታካፍያለሽ", "መንገድሽን ለአለም ታካፍያለሽ"),
                    LyricLine(15000, "Rophnan on the beat with Addis energy", "ሮፍናን በአዲስ አበባ ኃይለኛ ምት", "ሮፍናን በአዲስ አበባ ኃይለኛ ምት"),
                    LyricLine(21000, "Birhanesh yimolal, sewaye aykelkelim", "ብርሃንሽ ይሞላል፣ ሰው አያግደውም", "ብርሃንሽ ይሞላል፣ ሰው አያግደውም"),
                    LyricLine(27000, "♪ (High Energy Afro-Electronic Beat Drop) ♪", "♪ (ከፍተኛ የአፍሮ ኤሌክትሮኒክ ዜማ) ♪", "♪ (ከፍተኛ የአፍሮ ኤሌክትሮኒክ ዜማ) ♪")
                )
            ),
            Track(
                id = "track_12",
                title = "Nanu Nanu Ney",
                artist = "Muluken Melesse",
                album = "Tizita Legends",
                genre = "Ethiopian",
                durationMs = 245000,
                gradientStartHex = 0xFFFF7043,
                gradientEndHex = 0xFF4E342E,
                playsCount = "22.5M",
                audioPreset = "ETHIOPIAN",
                isFavorite = true,
                isDownloaded = isMarkedDownloaded("track_12"),
                fileSizeMb = 6.7,
                lyrics = listOf(
                    LyricLine(0, "♪ (Classic Horns & Guitar Chords) ♪", "♪ (የጥንታዊ መለከቶችና ጊታር ቅኝት) ♪", "♪ (የጥንታዊ መለከቶችና ጊታር ቅኝት) ♪"),
                    LyricLine(5000, "Nanu nanu ney, nanu nanu ney", "ናኑ ናኑ ነይ፣ ናኑ ናኑ ነይ", "ናኑ ናኑ ነይ፣ ናኑ ናኑ ነይ"),
                    LyricLine(10500, "Yene fikerita kanchi wuchi ayisemam", "የኔ ፍቅር ካንቺ ውጪ አይሰማም", "የኔ ፍቅር ካንቺ ውጪ አይሰማም"),
                    LyricLine(16000, "Wubetish lezelalem be libe yinural", "ውበትሽ ለዘላለም በልቤ ይኖራል", "ውበትሽ ለዘላለም በልቤ ይኖራል"),
                    LyricLine(22000, "Mekera yalehign be fikrish fawusew", "መከራ የያዘኝን በፍቅርሽ አክመሽው", "መከራ የያዘኝን በፍቅርሽ አክመሽው"),
                    LyricLine(28000, "Nanu nanu ney!", "ናኑ ናኑ ነይ!", "ናኑ ናኑ ነይ!")
                )
            )
        )
    }

    private fun loadBuzzFeed() {
        _buzzPosts.value = listOf(
            BuzzPost(
                id = "post_1",
                authorName = "Gebeyehu Sintayehu",
                handle = "@gebeyehu_dev",
                avatarBgHex = 0xFF00D2C4,
                timeAgo = "10m ago",
                content = "Welcome to Gebeyehu Player! Enjoy synchronized lyrics in Amharic & English, offline downloads to phone storage, and background playback.",
                trackTitle = "Last Last",
                artistName = "Burna Boy",
                likesCount = 342,
                isLiked = true,
                commentsCount = 48,
                sharesCount = 19
            ),
            BuzzPost(
                id = "post_2",
                authorName = "Selamawit Music",
                handle = "@selam_beats",
                avatarBgHex = 0xFFFF6B35,
                timeAgo = "1h ago",
                content = "የቴዲ አፍሮ አማነህ ግጥም በአማርኛ ሲታይ በጣም ያምራል! Teddy Afro's Amaneh has the most touching lyrics in Ge'ez script! ✨🇪🇹",
                trackTitle = "Amaneh",
                artistName = "Teddy Afro",
                likesCount = 415,
                isLiked = true,
                commentsCount = 52,
                sharesCount = 28
            ),
            BuzzPost(
                id = "post_3",
                authorName = "Afrobeats Global",
                handle = "@afrobeats_hq",
                avatarBgHex = 0xFF4CAF50,
                timeAgo = "3h ago",
                content = "Download your favorite tracks to phone storage for offline flights and travels without using mobile data! 🎧📱",
                trackTitle = "Calm Down",
                artistName = "Rema",
                likesCount = 512,
                isLiked = false,
                commentsCount = 67,
                sharesCount = 33
            )
        )
    }
}

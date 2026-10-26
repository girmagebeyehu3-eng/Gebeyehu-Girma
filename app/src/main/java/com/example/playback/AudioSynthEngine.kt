package com.example.playback

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-fidelity audio synthesizer that generates realistic rhythmic and melodic
 * audio files cached locally so that MediaPlayer produces genuine music sound
 * across all tracks and in background playback.
 */
object AudioSynthEngine {

    fun getOrCreateTrackAudio(context: Context, trackId: String, preset: String): File {
        val audioDir = File(context.cacheDir, "music_cache").apply { mkdirs() }
        val targetFile = File(audioDir, "track_${trackId}.wav")

        if (targetFile.exists() && targetFile.length() > 44) {
            return targetFile
        }

        try {
            generateWavTrack(targetFile, preset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return targetFile
    }

    private fun generateWavTrack(file: File, preset: String) {
        val sampleRate = 22050
        val durationSeconds = 45 // 45 second loopable rich preview clip
        val totalSamples = sampleRate * durationSeconds
        val numChannels = 1
        val bitsPerSample = 16

        val chords = when (preset) {
            "AFROBEATS" -> listOf(
                doubleArrayOf(261.63, 329.63, 392.00), // C Maj
                doubleArrayOf(220.00, 261.63, 329.63), // A Min
                doubleArrayOf(174.61, 220.00, 261.63), // F Maj
                doubleArrayOf(196.00, 246.94, 293.66)  // G Maj
            )
            "AMAPIANO" -> listOf(
                doubleArrayOf(146.83, 220.00, 261.63), // Dm7
                doubleArrayOf(130.81, 196.00, 246.94), // C/E
                doubleArrayOf(110.00, 164.81, 220.00), // Am
                doubleArrayOf(164.81, 246.94, 293.66)  // Em7
            )
            "ETHIOPIAN" -> listOf(
                doubleArrayOf(220.00, 246.94, 261.63), // Tizita minor flavor
                doubleArrayOf(293.66, 329.63, 392.00),
                doubleArrayOf(220.00, 261.63, 329.63),
                doubleArrayOf(196.00, 220.00, 293.66)
            )
            "RNB_CHILL" -> listOf(
                doubleArrayOf(174.61, 261.63, 329.63, 392.00), // Fmaj7
                doubleArrayOf(164.81, 246.94, 329.63, 392.00), // Em7
                doubleArrayOf(146.83, 220.00, 261.63, 349.23), // Dm7
                doubleArrayOf(196.00, 246.94, 293.66, 349.23)  // G7
            )
            else -> listOf(
                doubleArrayOf(261.63, 329.63, 392.00),
                doubleArrayOf(196.00, 246.94, 293.66),
                doubleArrayOf(220.00, 261.63, 329.63),
                doubleArrayOf(174.61, 220.00, 261.63)
            )
        }

        val fos = FileOutputStream(file)
        // Write placeholder 44-byte WAV header
        fos.write(ByteArray(44))

        val bpm = if (preset == "AMAPIANO") 114.0 else 104.0
        val beatDuration = 60.0 / bpm
        val barDuration = beatDuration * 4.0

        val buffer = ShortArray(1024)
        var bufferIdx = 0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val barIndex = ((t / barDuration).toInt()) % chords.size
            val activeChord = chords[barIndex]
            val beatTime = t % beatDuration
            val barTime = t % barDuration

            // 1. Kick & Percussion (Afrobeat / Amapiano beat)
            var kick = 0.0
            if (beatTime < 0.12) {
                val kickDecay = 1.0 - (beatTime / 0.12)
                kick = sin(2 * PI * (60.0 - beatTime * 200.0) * beatTime) * kickDecay * 0.45
            }

            // Snare / Shaker on beat 2 & 4
            var snare = 0.0
            val subBeat = (t % (beatDuration * 2))
            if (subBeat > beatDuration && subBeat < beatDuration + 0.08) {
                val sTime = subBeat - beatDuration
                val snareDecay = 1.0 - (sTime / 0.08)
                val noise = ((i % 17) - 8) / 8.0
                snare = (sin(2 * PI * 180.0 * sTime) * 0.3 + noise * 0.7) * snareDecay * 0.25
            }

            // 2. Bassline
            val bassFreq = activeChord[0] / 2.0
            val bassRhythm = (barTime % (beatDuration / 2.0))
            val bassEnv = if (bassRhythm < 0.2) (1.0 - bassRhythm / 0.2) else 0.1
            val bass = (sin(2 * PI * bassFreq * t) + 0.3 * sin(2 * PI * bassFreq * 2.0 * t)) * bassEnv * 0.35

            // 3. Melodic Chord Pads
            var chordSample = 0.0
            for (freq in activeChord) {
                chordSample += sin(2 * PI * freq * t) * 0.12
            }

            // 4. Lead Arp / Melody
            val step = ((t / (beatDuration / 2.0)).toInt()) % activeChord.size
            val melodyFreq = activeChord[step] * 2.0
            val arpDecay = 1.0 - ((t % (beatDuration / 2.0)) / (beatDuration / 2.0))
            val melody = sin(2 * PI * melodyFreq * t) * arpDecay * 0.18

            // Mix & Soft Clip
            var mixed = kick + snare + bass + chordSample + melody
            if (mixed > 0.95) mixed = 0.95
            if (mixed < -0.95) mixed = -0.95

            val sampleVal = (mixed * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            buffer[bufferIdx++] = sampleVal

            if (bufferIdx == buffer.size) {
                val byteBuffer = ByteBuffer.allocate(buffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in buffer) byteBuffer.putShort(s)
                fos.write(byteBuffer.array())
                bufferIdx = 0
            }
        }

        if (bufferIdx > 0) {
            val byteBuffer = ByteBuffer.allocate(bufferIdx * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (j in 0 until bufferIdx) byteBuffer.putShort(buffer[j])
            fos.write(byteBuffer.array())
        }

        fos.close()

        // Write accurate RIFF WAV header
        val totalDataLen = (totalSamples * numChannels * (bitsPerSample / 8)).toLong()
        writeWavHeader(file, totalDataLen, sampleRate, numChannels, bitsPerSample)
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        raf.write(header)
        raf.close()
    }
}

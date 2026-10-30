package io.github.elli0t0.phone.data.recordings

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.elli0t0.phone.R

/**
 * One-at-a-time playback for recordings from the Recents list.
 * The playing id is compose state so the row icon flips between
 * play and pause; stop on dispose so audio never outlives the
 * screen.
 */
class RecordingQuickPlayer(private val context: Context) {
    var playingId by mutableStateOf<Long?>(null)
        private set
    private var player: MediaPlayer? = null

    /** Returns an error message or null. */
    fun toggle(entity: RecordingEntity): String? {
        if (playingId == entity.id) {
            stop()
            return null
        }
        stop()
        val mp = MediaPlayer()
        return try {
            mp.setDataSource(context, Uri.parse(entity.uri))
            mp.prepare()
            mp.setOnCompletionListener { stop() }
            mp.start()
            player = mp
            playingId = entity.id
            null
        } catch (e: Exception) {
            runCatching { mp.release() }
            playingId = null
            context.getString(R.string.recents_recording_missing)
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        playingId = null
    }
}

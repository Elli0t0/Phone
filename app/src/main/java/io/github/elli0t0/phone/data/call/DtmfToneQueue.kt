package io.github.elli0t0.phone.data.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One serialized DTMF queue: each tone plays for [TONE_MS], stops,
 * then a [GAP_MS] gap before the next tone. The timings are telephony
 * timings, not UI motion, so they live here rather than in Motion.
 * Tones only play while [isCallActive]; anything queued while the
 * call is not ACTIVE is dropped. [drain] stops the tone playing
 * right now and drops everything still queued.
 */
fun interface DtmfTonePlayer {
    fun play(digit: Char)
    fun stop()
}

class DtmfToneQueue(
    private val player: DtmfTonePlayer,
    private val isCallActive: () -> Boolean,
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    companion object {
        const val TONE_MS = 150L
        const val GAP_MS = 80L
    }

    private val channel = Channel<Char>(Channel.UNLIMITED)
    private var worker: Job? = null

    init {
        startWorker()
    }

    fun send(digit: Char) {
        channel.trySend(digit)
    }

    fun drain() {
        worker?.cancel()
        player.stop()
        while (channel.tryReceive().isSuccess) {
        }
        startWorker()
    }

    private fun startWorker() {
        worker = scope.launch {
            for (digit in channel) {
                if (!isCallActive()) continue
                try {
                    player.play(digit)
                    delay(TONE_MS)
                } finally {
                    player.stop()
                }
                delay(GAP_MS)
            }
        }
    }
}

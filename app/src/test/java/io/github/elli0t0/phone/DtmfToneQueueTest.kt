package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.call.DtmfTonePlayer
import io.github.elli0t0.phone.data.call.DtmfToneQueue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DTMF queue behaviour, on virtual time: tones play in send order,
 * every tone is followed by a stop, tones are separated by the gap,
 * and [DtmfToneQueue.drain] drops everything still queued.
 *
 * The queue takes its scope as a parameter, so tests run it on the
 * test scheduler instead of the main dispatcher.
 */
class DtmfToneQueueTest {

    private class RecordingPlayer : DtmfTonePlayer {
        val events = mutableListOf<String>()
        val plays get() = events.filter { it.startsWith("play") }.map { it.removePrefix("play").single() }
        val stops get() = events.count { it == "stop" }

        override fun play(digit: Char) {
            events += "play$digit"
        }

        override fun stop() {
            events += "stop"
        }
    }

    @Test
    fun tonesPlayInOrderWithStopAfterEach() = runTest {
        val player = RecordingPlayer()
        val queue = DtmfToneQueue(player, { true }, backgroundScope)
        queue.send('1')
        queue.send('2')
        queue.send('3')
        advanceTimeBy(3 * (DtmfToneQueue.TONE_MS + DtmfToneQueue.GAP_MS))
        assertEquals(listOf('1', '2', '3'), player.plays)
        assertEquals(3, player.stops)
        assertEquals(
            listOf("play1", "stop", "play2", "stop", "play3", "stop"),
            player.events
        )
    }

    @Test
    fun toneStopsAfterToneDuration() = runTest {
        val player = RecordingPlayer()
        val queue = DtmfToneQueue(player, { true }, backgroundScope)
        queue.send('7')
        // Flush the worker start first: assertions below measure from
        // a known playing state, not from an assumed start instant.
        runCurrent()
        assertEquals(listOf("play7"), player.events)
        // Mid-tone: still playing, no stop yet.
        advanceTimeBy(DtmfToneQueue.TONE_MS / 2)
        assertEquals(listOf("play7"), player.events)
        // Past the full tone duration: stopped.
        advanceTimeBy(DtmfToneQueue.TONE_MS)
        runCurrent()
        assertEquals(listOf("play7", "stop"), player.events)
    }

    @Test
    fun nextToneWaitsForGapAfterStop() = runTest {
        val player = RecordingPlayer()
        val queue = DtmfToneQueue(player, { true }, backgroundScope)
        queue.send('1')
        queue.send('2')
        runCurrent()
        advanceTimeBy(DtmfToneQueue.TONE_MS)
        runCurrent()
        assertEquals(listOf("play1", "stop"), player.events)
        // Mid-gap: the next tone has not started yet.
        advanceTimeBy(DtmfToneQueue.GAP_MS / 2)
        assertEquals(listOf("play1", "stop"), player.events)
        // Past the full gap: the next tone plays.
        advanceTimeBy(DtmfToneQueue.GAP_MS)
        runCurrent()
        assertEquals(listOf("play1", "stop", "play2"), player.events)
    }

    @Test
    fun drainDropsQueuedDigitsAndRestartsWorker() = runTest {
        val player = RecordingPlayer()
        val queue = DtmfToneQueue(player, { true }, backgroundScope)
        queue.send('1')
        queue.send('2')
        queue.send('3')
        advanceTimeBy(10)
        assertEquals(listOf('1'), player.plays)
        queue.drain()
        runCurrent()
        advanceTimeBy(10_000)
        assertEquals(listOf('1'), player.plays)
        assertTrue(player.stops >= 1)
        // The worker restarts: a digit sent after drain still plays.
        queue.send('9')
        advanceTimeBy(DtmfToneQueue.TONE_MS)
        assertEquals(listOf('1', '9'), player.plays)
    }

    @Test
    fun digitsQueuedWhileInactiveAreDropped() = runTest {
        var active = false
        val player = RecordingPlayer()
        val queue = DtmfToneQueue(player, { active }, backgroundScope)
        queue.send('1')
        queue.send('2')
        advanceTimeBy(1_000)
        assertTrue(player.events.isEmpty())
        // Dropped, not deferred: going active later plays nothing.
        active = true
        advanceTimeBy(1_000)
        assertTrue(player.events.isEmpty())
    }
}

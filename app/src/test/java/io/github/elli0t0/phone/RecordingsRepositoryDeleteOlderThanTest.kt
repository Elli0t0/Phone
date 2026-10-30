package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.recordings.DefaultRecordingsRepository
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingStats
import io.github.elli0t0.phone.data.recordings.RecordingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun recording(
    id: Long,
    startedAt: Long,
    starred: Boolean = false,
    isTest: Boolean = false
): RecordingEntity = RecordingEntity(
    id = id,
    uri = "content://test/$id",
    number = "1",
    displayName = null,
    startedAt = startedAt,
    durationMs = 1000,
    sizeBytes = 100,
    isTest = isTest,
    starred = starred
)

private class FakeRecordingsDao(
    private val all: List<RecordingEntity> = emptyList(),
    private val unstarred: List<RecordingEntity> = emptyList(),
    private val oldTests: List<RecordingEntity> = emptyList(),
    private val events: MutableList<String> = mutableListOf()
) : RecordingsDao {
    override fun getAll(): Flow<List<RecordingEntity>> = flowOf(all)
    override suspend fun insert(entity: RecordingEntity): Long = 0L
    override suspend fun deleteById(id: Long) {
        events.add("row:$id")
    }
    override suspend fun setStarred(id: Long, starred: Boolean) {}
    override suspend fun setTitleNote(id: Long, title: String?, note: String?) {}
    override suspend fun olderThanUnstarred(cutoff: Long): List<RecordingEntity> = unstarred
    override suspend fun oldTests(cutoff: Long): List<RecordingEntity> = oldTests
    override fun stats(): Flow<RecordingStats> = flowOf(RecordingStats(0, 0))
}

class RecordingsRepositoryDeleteOlderThanTest {
    private val cutoff = 1_000_000L
    private val old = 100_000L
    private val recent = 2_000_000L

    @Test
    fun keepStarred_keepsStarred_deletesUnstarredAndOldTests_filesBeforeRows() = runTest {
        val events = mutableListOf<String>()
        val dao = FakeRecordingsDao(
            all = listOf(
                recording(1, old),
                recording(2, old),
                recording(3, old, starred = true),
                recording(4, old, isTest = true)
            ),
            unstarred = listOf(recording(1, old), recording(2, old)),
            oldTests = listOf(recording(4, old, isTest = true)),
            events = events
        )
        val repo = DefaultRecordingsRepository(
            dao,
            fileDeleter = { _ -> events.add("file") },
            fileExists = { _ -> true }
        )
        val count = repo.deleteOlderThan(cutoff, keepStarred = true)
        assertEquals(3, count)
        assertEquals(listOf("file", "file", "file", "row:1", "row:2", "row:4"), events)
        assertFalse(events.any { it == "row:3" })
    }

    @Test
    fun noKeepStarred_deletesOldStarred_keepsRecent() = runTest {
        val events = mutableListOf<String>()
        val dao = FakeRecordingsDao(
            all = listOf(
                recording(1, old),
                recording(2, old, starred = true),
                recording(3, recent),
                recording(4, old, isTest = true)
            ),
            oldTests = listOf(recording(4, old, isTest = true)),
            events = events
        )
        val repo = DefaultRecordingsRepository(
            dao,
            fileDeleter = { _ -> events.add("file") },
            fileExists = { _ -> true }
        )
        val count = repo.deleteOlderThan(cutoff, keepStarred = false)
        assertEquals(3, count)
        assertTrue(events.contains("row:2"))
        assertFalse(events.contains("row:3"))
        val firstRow = events.indexOfFirst { it.startsWith("row:") }
        assertTrue(events.subList(0, firstRow).all { it == "file" })
    }

    @Test
    fun empty_returnsZero_noCalls() = runTest {
        val events = mutableListOf<String>()
        val repo = DefaultRecordingsRepository(
            FakeRecordingsDao(events = events),
            fileDeleter = { _ -> events.add("file") },
            fileExists = { _ -> true }
        )
        assertEquals(0, repo.deleteOlderThan(cutoff, keepStarred = true))
        assertTrue(events.isEmpty())
    }
}

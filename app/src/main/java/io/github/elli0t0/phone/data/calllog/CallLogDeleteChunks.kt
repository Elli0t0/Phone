package io.github.elli0t0.phone.data.calllog

/**
 * Provider deletes are chunked so a huge selection never builds one
 * giant IN clause; ids are deduplicated first.
 */
fun deleteCallChunks(ids: Collection<Long>, chunkSize: Int = 400): List<List<Long>> =
    ids.distinct().chunked(chunkSize)

package io.github.elli0t0.phone.data.favorites

import io.github.elli0t0.phone.data.favorites.model.ContactSimEntity
import kotlinx.coroutines.flow.Flow

interface ContactSimRepository {
    suspend fun getSimId(lookupKey: String): String?
    fun getSimIdFlow(lookupKey: String): Flow<String?>
    suspend fun setSimId(lookupKey: String, simId: String?)
    suspend fun clearSim(lookupKey: String)
}

class DefaultContactSimRepository(
    private val dao: ContactSimDao
) : ContactSimRepository {
    override suspend fun getSimId(lookupKey: String): String? {
        if (lookupKey.isBlank()) return null
        return dao.getSimId(lookupKey)
    }

    override fun getSimIdFlow(lookupKey: String): Flow<String?> {
        return dao.getSimIdFlow(lookupKey)
    }

    override suspend fun setSimId(lookupKey: String, simId: String?) {
        if (lookupKey.isBlank()) return
        if (simId == null) {
            dao.deleteByLookupKey(lookupKey)
        } else {
            dao.upsert(ContactSimEntity(contactLookupKey = lookupKey, simId = simId))
        }
    }

    override suspend fun clearSim(lookupKey: String) {
        if (lookupKey.isBlank()) return
        dao.deleteByLookupKey(lookupKey)
    }
}

package io.github.elli0t0.phone.data.favorites

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface FavoritesRepository {
    fun getFavoritesFlow(allContactsFlow: Flow<List<ContactSummary>>): Flow<List<ContactSummary>>
    fun isFavoriteFlow(lookupKey: String): Flow<Boolean>
    suspend fun addFavorite(lookupKey: String, contactId: Long)
    suspend fun removeFavorite(lookupKey: String)
    suspend fun toggleFavorite(lookupKey: String, contactId: Long)
}

class DefaultFavoritesRepository(
    private val favoritesDao: FavoritesDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : FavoritesRepository {

    // One shared favorites flow, built once and kept in memory in this
    // singleton repository. All production callers pass the same contacts
    // flow. It stays a COLD flow on purpose: converting it to a StateFlow
    // with stateIn would make first() return the initial empty list
    // before the sharing coroutine runs, instead of the real favorites.
    // Collectors (the activity-scoped list ViewModels) hold the last
    // value across tab switches; each collection still observes the DAO
    // flow, so deletes/updates keep being reflected.
    private var cachedFavoritesFlow: Flow<List<ContactSummary>>? = null

    override fun getFavoritesFlow(
        allContactsFlow: Flow<List<ContactSummary>>
    ): Flow<List<ContactSummary>> {
        cachedFavoritesFlow?.let { return it }
        val shared = combine(
            favoritesDao.getAllFavorites(),
            allContactsFlow
        ) { favoriteEntities, contacts ->
            val contactsByLookup = contacts.associateBy { it.lookupKey }
            val contactsById = contacts.associateBy { it.id }

            val validFavorites = mutableListOf<ContactSummary>()
            val orphanedEntities = mutableListOf<FavoriteContactEntity>()

            for (entity in favoriteEntities) {
                val matched = contactsByLookup[entity.contactLookupKey]
                    ?: contactsById[entity.contactId]

                if (matched != null) {
                    validFavorites.add(matched)
                } else {
                    orphanedEntities.add(entity)
                }
            }

            // Clean up favorites whose contact was deleted from device
            if (orphanedEntities.isNotEmpty()) {
                externalScope.launch {
                    for (orphan in orphanedEntities) {
                        favoritesDao.deleteFavoriteByLookupKey(orphan.contactLookupKey)
                    }
                }
            }

            validFavorites.toList()
        }
        cachedFavoritesFlow = shared
        return shared
    }

    override fun isFavoriteFlow(lookupKey: String): Flow<Boolean> {
        return favoritesDao.isFavoriteFlow(lookupKey)
    }

    override suspend fun addFavorite(lookupKey: String, contactId: Long) {
        val count = favoritesDao.getCount()
        favoritesDao.insertFavorite(
            FavoriteContactEntity(
                contactLookupKey = lookupKey,
                contactId = contactId,
                position = count
            )
        )
    }

    override suspend fun removeFavorite(lookupKey: String) {
        favoritesDao.deleteFavoriteByLookupKey(lookupKey)
    }

    override suspend fun toggleFavorite(lookupKey: String, contactId: Long) {
        val currentlyFav = favoritesDao.isFavorite(lookupKey)
        if (currentlyFav) {
            removeFavorite(lookupKey)
        } else {
            addFavorite(lookupKey, contactId)
        }
    }
}

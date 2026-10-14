package io.github.elli0t0.phone.data.favorites

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    // One shared, cached favorites flow: built once and kept in memory in
    // this singleton repository. Later collectors (Recents, Contacts)
    // get the last value instantly instead of re-querying Room on every
    // tab switch. All production callers pass the same contacts flow.
    private var cachedFavoritesFlow: StateFlow<List<ContactSummary>>? = null

    override fun getFavoritesFlow(
        allContactsFlow: Flow<List<ContactSummary>>
    ): Flow<List<ContactSummary>> {
        val cached = cachedFavoritesFlow
        if (cached != null) return cached
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
        }.stateIn(externalScope, SharingStarted.Lazily, emptyList())
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

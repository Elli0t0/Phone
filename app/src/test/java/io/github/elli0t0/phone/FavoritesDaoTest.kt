package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.favorites.DefaultFavoritesRepository
import io.github.elli0t0.phone.data.favorites.FavoritesDao
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFavoritesDao : FavoritesDao {
    private val entitiesFlow = MutableStateFlow<List<FavoriteContactEntity>>(emptyList())

    override fun getAllFavorites(): Flow<List<FavoriteContactEntity>> {
        return entitiesFlow.map { list ->
            list.sortedWith(compareBy({ it.position }, { it.addedAt }))
        }
    }

    override suspend fun insertFavorite(favorite: FavoriteContactEntity) {
        val current = entitiesFlow.value.toMutableList()
        current.removeAll { it.contactLookupKey == favorite.contactLookupKey }
        current.add(favorite)
        entitiesFlow.value = current
    }

    override suspend fun deleteFavoriteByLookupKey(lookupKey: String) {
        val current = entitiesFlow.value.toMutableList()
        current.removeAll { it.contactLookupKey == lookupKey }
        entitiesFlow.value = current
    }

    override fun isFavoriteFlow(lookupKey: String): Flow<Boolean> {
        return entitiesFlow.map { list ->
            list.any { it.contactLookupKey == lookupKey }
        }
    }

    override suspend fun isFavorite(lookupKey: String): Boolean {
        return entitiesFlow.value.any { it.contactLookupKey == lookupKey }
    }

    override suspend fun getCount(): Int {
        return entitiesFlow.value.size
    }

    override suspend fun updateFavorites(favorites: List<FavoriteContactEntity>) {
        val current = entitiesFlow.value.toMutableList()
        for (fav in favorites) {
            current.removeAll { it.contactLookupKey == fav.contactLookupKey }
            current.add(fav)
        }
        entitiesFlow.value = current
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesDaoTest {

    private lateinit var dao: FakeFavoritesDao
    private lateinit var repository: DefaultFavoritesRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        dao = FakeFavoritesDao()
        repository = DefaultFavoritesRepository(dao, testScope)
    }

    @Test
    fun dao_insertAndGetAllFavorites_orderedByPosition() = runTest {
        dao.insertFavorite(FavoriteContactEntity("k2", 2L, position = 1))
        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 0))

        val all = dao.getAllFavorites().first()
        assertEquals(2, all.size)
        assertEquals("k1", all[0].contactLookupKey)
        assertEquals("k2", all[1].contactLookupKey)
    }

    @Test
    fun dao_replaceOnConflict_updatesExistingFavorite() = runTest {
        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 0))
        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 5))

        val all = dao.getAllFavorites().first()
        assertEquals(1, all.size)
        assertEquals(5, all[0].position)
    }

    @Test
    fun dao_deleteFavoriteByLookupKey_removesEntity() = runTest {
        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 0))
        assertTrue(dao.isFavorite("k1"))

        dao.deleteFavoriteByLookupKey("k1")
        assertFalse(dao.isFavorite("k1"))
        assertEquals(0, dao.getCount())
    }

    @Test
    fun dao_isFavoriteFlow_emitsCurrentStatus() = runTest {
        assertFalse(dao.isFavoriteFlow("k1").first())

        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 0))
        assertTrue(dao.isFavoriteFlow("k1").first())
    }

    @Test
    fun repository_handlesDeletedContactsGracefully() = runTest {
        dao.insertFavorite(FavoriteContactEntity("k1", 1L, position = 0))
        dao.insertFavorite(FavoriteContactEntity("k_deleted", 99L, position = 1))

        val contacts = listOf(
            ContactSummary(1L, "k1", "Alice")
        )

        val favorites = repository.getFavoritesFlow(flowOf(contacts)).first()

        assertEquals(1, favorites.size)
        assertEquals("Alice", favorites[0].displayName)
    }

    @Test
    fun repository_toggleFavorite_addsAndRemoves() = runTest {
        assertFalse(dao.isFavorite("k1"))

        repository.toggleFavorite("k1", 1L)
        assertTrue(dao.isFavorite("k1"))

        repository.toggleFavorite("k1", 1L)
        assertFalse(dao.isFavorite("k1"))
    }
}

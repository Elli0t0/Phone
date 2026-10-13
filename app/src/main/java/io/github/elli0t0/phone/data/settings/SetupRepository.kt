package io.github.elli0t0.phone.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.setupDataStore: DataStore<Preferences> by preferencesDataStore(name = "phone_setup")

interface SetupRepository {
    val setupCompletedFlow: Flow<Boolean>
    suspend fun setSetupCompleted(completed: Boolean)
}

class DataStoreSetupRepository(
    private val dataStore: DataStore<Preferences>
) : SetupRepository {
    private object Keys {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    override val setupCompletedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SETUP_COMPLETED] ?: false
    }

    override suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SETUP_COMPLETED] = completed
        }
    }
}

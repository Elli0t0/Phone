package io.github.elli0t0.phone

import android.app.Application
import android.content.Context
import io.github.elli0t0.phone.data.calllog.AndroidCallLogRepository
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.contacts.AndroidContactsRepository
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import io.github.elli0t0.phone.data.favorites.AppDatabase
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.data.favorites.DefaultContactSimRepository
import io.github.elli0t0.phone.data.favorites.DefaultFavoritesRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.core.notifications.CallNotificationChannels
import io.github.elli0t0.phone.data.settings.DataStoreSettingsRepository
import io.github.elli0t0.phone.data.settings.DataStoreSetupRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.SetupRepository
import io.github.elli0t0.phone.data.settings.settingsDataStore
import io.github.elli0t0.phone.data.settings.setupDataStore

interface AppContainer {
    val contactsRepository: ContactsRepository
    val callLogRepository: CallLogRepository
    val favoritesRepository: FavoritesRepository
    val contactSimRepository: ContactSimRepository
    val settingsRepository: SettingsRepository
    val setupRepository: SetupRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val contactsRepository: ContactsRepository by lazy {
        AndroidContactsRepository(context.contentResolver)
    }

    override val callLogRepository: CallLogRepository by lazy {
        AndroidCallLogRepository(context.contentResolver, context.cacheDir)
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        DefaultFavoritesRepository(database.favoritesDao())
    }

    override val contactSimRepository: ContactSimRepository by lazy {
        DefaultContactSimRepository(database.contactSimDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
    }

    override val setupRepository: SetupRepository by lazy {
        DataStoreSetupRepository(context.applicationContext.setupDataStore)
    }
}


class PhoneApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        CallNotificationChannels.createAll(this)
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ContactNameIndex.ensureStarted(contentResolver)
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CALL_LOG
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            (container.callLogRepository as? AndroidCallLogRepository)?.preload()
        }
    }
}


package io.github.elli0t0.phone

import android.app.Application
import android.content.Context
import io.github.elli0t0.phone.data.calllog.AndroidCallLogRepository
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.contacts.AndroidContactsRepository
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.AppDatabase
import io.github.elli0t0.phone.data.favorites.DefaultFavoritesRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository

interface AppContainer {
    val contactsRepository: ContactsRepository
    val callLogRepository: CallLogRepository
    val favoritesRepository: FavoritesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val contactsRepository: ContactsRepository by lazy {
        AndroidContactsRepository(context.contentResolver)
    }

    override val callLogRepository: CallLogRepository by lazy {
        AndroidCallLogRepository(context.contentResolver)
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        DefaultFavoritesRepository(database.favoritesDao())
    }
}

class PhoneApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

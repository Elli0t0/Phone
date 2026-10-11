package io.github.elli0t0.phone

import android.app.Application
import android.content.Context
import io.github.elli0t0.phone.data.contacts.AndroidContactsRepository
import io.github.elli0t0.phone.data.contacts.ContactsRepository

interface AppContainer {
    val contactsRepository: ContactsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val contactsRepository: ContactsRepository by lazy {
        AndroidContactsRepository(context.contentResolver)
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

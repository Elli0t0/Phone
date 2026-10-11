package io.github.elli0t0.phone

import android.app.Application

/**
 * AppContainer serves as the manual dependency injection container for Phone.
 * Additional repositories and services will be registered in future versions.
 */
interface AppContainer

class DefaultAppContainer : AppContainer

class PhoneApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}

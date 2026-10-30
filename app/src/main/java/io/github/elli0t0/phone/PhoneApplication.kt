package io.github.elli0t0.phone

import android.app.Application
import android.content.Context
import io.github.elli0t0.phone.data.calllog.AndroidCallLogRepository
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.core.telecom.TelecomHelper
import io.github.elli0t0.phone.data.contacts.AndroidContactsRepository
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.AppDatabase
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
import io.github.elli0t0.phone.data.favorites.DefaultContactSimRepository
import io.github.elli0t0.phone.data.favorites.DefaultFavoritesRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.core.common.CrashLog
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.core.notifications.CallNotificationChannels
import io.github.elli0t0.phone.data.settings.DataStoreSettingsRepository
import io.github.elli0t0.phone.data.settings.DataStoreSetupRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.SetupRepository
import io.github.elli0t0.phone.data.settings.settingsDataStore
import io.github.elli0t0.phone.data.settings.setupDataStore
import io.github.elli0t0.phone.core.recording.RecordingCleanup
import io.github.elli0t0.phone.core.recording.RecordingController
import io.github.elli0t0.phone.core.recording.adb.AdbRecordingBackend
import io.github.elli0t0.phone.data.recordings.DefaultRecordingsRepository
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** One external dial request (tap a number in another app). The id
 * lets the collector ignore replays of an already-handled request
 * (e.g. after rotation), since the flow keeps a replay value. */
data class DialRequest(val number: String, val id: Long)

interface AppContainer {
    val contactsRepository: ContactsRepository
    val callLogRepository: CallLogRepository
    val favoritesRepository: FavoritesRepository
    val contactSimRepository: ContactSimRepository
    val settingsRepository: SettingsRepository
    val setupRepository: SetupRepository
    val recordingsRepository: RecordingsRepository
    val recordingController: RecordingController
    val dialRequests: MutableSharedFlow<DialRequest>
    /**
     * Auto-record failure taps (notification): the id lets the
     * collector ignore replays of an already-handled request, like
     * dialRequests above.
     */
    val openRecordingRequests: MutableSharedFlow<Long>
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val contactsRepository: ContactsRepository by lazy {
        AndroidContactsRepository(context.contentResolver)
    }

    override val callLogRepository: CallLogRepository by lazy {
        AndroidCallLogRepository(
            context.contentResolver,
            context.cacheDir,
            simAccountsProvider = { TelecomHelper.getCallCapablePhoneAccounts(context) }
        )
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

    override val recordingsRepository: RecordingsRepository by lazy {
        DefaultRecordingsRepository(
            database.recordingsDao(),
            fileDeleter = { uri ->
                io.github.elli0t0.phone.core.recording.deleteFile(context.contentResolver, uri)
            },
            fileExists = { uri ->
                runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                }.getOrDefault(false)
            }
        )
    }

    override val recordingController: RecordingController by lazy {
        RecordingController(
            context,
            AdbRecordingBackend(context, settingsRepository),
            settingsRepository,
            recordingsRepository
        )
    }

    override val dialRequests: MutableSharedFlow<DialRequest> by lazy {
        MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    }

    override val openRecordingRequests: MutableSharedFlow<Long> by lazy {
        MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    }
}


class PhoneApplication : Application() {
    lateinit var container: AppContainer
        private set

    /**
     * Scope for work that must outlive a broadcast: goAsync only gives
     * a receiver ~10 s, but pairing (30 s discovery + connect) runs
     * here instead, so it is never cut off mid-flight.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        PhoneNumberMatcher.initRegion(this)
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
        applicationScope.launch {
            runCatching {
                RecordingCleanup(container.settingsRepository, container.recordingsRepository).runIfDue()
            }
        }
    }
}


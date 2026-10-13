package io.github.elli0t0.phone.core.permissions

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.elli0t0.phone.core.telecom.DefaultDialerHelper

enum class SetupItemId {
    CONTACTS,
    CALL_LOG,
    CALL_PHONE,
    PHONE_STATE,
    NOTIFICATIONS,
    BLUETOOTH,
    FULL_SCREEN,
    DEFAULT_DIALER,
    BATTERY
}

data class SetupItemState(
    val id: SetupItemId,
    val isGranted: Boolean,
    val isPermanentlyDenied: Boolean = false,
    val isApplicable: Boolean = true
)

object SetupPermissions {
    val CONTACTS_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )
    val CALL_LOG_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG
    )
    val CALL_PHONE_PERMISSIONS = arrayOf(Manifest.permission.CALL_PHONE)
    val PHONE_STATE_PERMISSIONS = arrayOf(Manifest.permission.READ_PHONE_STATE)
    val BLUETOOTH_PERMISSIONS = arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    val NOTIFICATIONS_PERMISSIONS = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

    fun isContactsGranted(context: Context): Boolean {
        return CONTACTS_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isCallLogGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isCallPhoneGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isPhoneStateGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.areNotificationsEnabled() ?: true
    }

    fun isBluetoothGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isFullScreenGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return try {
            notificationManager?.canUseFullScreenIntent() ?: true
        } catch (_: Throwable) {
            true
        }
    }

    fun isDefaultDialerGranted(context: Context): Boolean {
        return DefaultDialerHelper.isDefaultDialer(context)
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    /**
     * True when every permission the Settings "Permissions & setup" card requires
     * (contacts, call log, make calls, phone state) is granted and this app holds
     * the default dialer role. Other setup items (notifications, Bluetooth,
     * full-screen intent, battery) do not affect this.
     */
    fun isRequiredSetupComplete(context: Context): Boolean {
        return isContactsGranted(context) &&
            isCallLogGranted(context) &&
            isCallPhoneGranted(context) &&
            isPhoneStateGranted(context) &&
            isDefaultDialerGranted(context)
    }

    fun isAnyMissing(context: Context): Boolean {
        return getItemStates(context).any { it.isApplicable && !it.isGranted }
    }

    fun isNotificationsOrFullScreenBlocked(context: Context): Boolean {
        return !isNotificationsGranted(context) || !isFullScreenGranted(context)
    }

    fun getItemStates(
        context: Context,
        permanentlyDenied: Set<SetupItemId> = emptySet()
    ): List<SetupItemState> {
        return listOf(
            SetupItemState(
                id = SetupItemId.CONTACTS,
                isGranted = isContactsGranted(context),
                isPermanentlyDenied = SetupItemId.CONTACTS in permanentlyDenied
            ),
            SetupItemState(
                id = SetupItemId.CALL_LOG,
                isGranted = isCallLogGranted(context),
                isPermanentlyDenied = SetupItemId.CALL_LOG in permanentlyDenied
            ),
            SetupItemState(
                id = SetupItemId.CALL_PHONE,
                isGranted = isCallPhoneGranted(context),
                isPermanentlyDenied = SetupItemId.CALL_PHONE in permanentlyDenied
            ),
            SetupItemState(
                id = SetupItemId.PHONE_STATE,
                isGranted = isPhoneStateGranted(context),
                isPermanentlyDenied = SetupItemId.PHONE_STATE in permanentlyDenied
            ),
            SetupItemState(
                id = SetupItemId.NOTIFICATIONS,
                isGranted = isNotificationsGranted(context),
                isPermanentlyDenied = SetupItemId.NOTIFICATIONS in permanentlyDenied,
                isApplicable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    !isNotificationsGranted(context)
            ),
            SetupItemState(
                id = SetupItemId.BLUETOOTH,
                isGranted = isBluetoothGranted(context),
                isPermanentlyDenied = SetupItemId.BLUETOOTH in permanentlyDenied,
                isApplicable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ),
            SetupItemState(
                id = SetupItemId.FULL_SCREEN,
                isGranted = isFullScreenGranted(context),
                isApplicable = Build.VERSION.SDK_INT >= 34
            ),
            SetupItemState(
                id = SetupItemId.DEFAULT_DIALER,
                isGranted = isDefaultDialerGranted(context)
            ),
            SetupItemState(
                id = SetupItemId.BATTERY,
                isGranted = isBatteryUnrestricted(context)
            )
        )
    }

    fun isPermanentlyDenied(activity: Activity?, permissions: Array<String>): Boolean {
        if (activity == null) return false
        return permissions.any { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 34) {
            openNotificationSettings(context)
            return
        }
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:" + context.packageName)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openNotificationSettings(context)
        }
    }

    fun openBatterySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }
}

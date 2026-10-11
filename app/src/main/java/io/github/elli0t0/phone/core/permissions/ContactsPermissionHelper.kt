package io.github.elli0t0.phone.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

sealed interface ContactsPermissionStatus {
    data object Granted : ContactsPermissionStatus
    data object NotAsked : ContactsPermissionStatus
    data object Denied : ContactsPermissionStatus
    data object PermanentlyDenied : ContactsPermissionStatus
}

object ContactsPermissions {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

    fun hasPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class ContactsPermissionState(
    val status: ContactsPermissionStatus,
    val requestPermissions: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun rememberContactsPermissionState(): ContactsPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAskedOnce by remember { mutableStateOf(false) }
    var currentStatus by remember {
        mutableStateOf<ContactsPermissionStatus>(
            if (ContactsPermissions.hasPermissions(context)) {
                ContactsPermissionStatus.Granted
            } else {
                ContactsPermissionStatus.NotAsked
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAskedOnce = true
        val allGranted = results.values.all { it }
        if (allGranted) {
            currentStatus = ContactsPermissionStatus.Granted
        } else {
            val activity = context as? Activity
            val shouldShowRationale = ContactsPermissions.REQUIRED_PERMISSIONS.any { perm ->
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
            currentStatus = if (shouldShowRationale) {
                ContactsPermissionStatus.Denied
            } else {
                ContactsPermissionStatus.PermanentlyDenied
            }
        }
    }

    // Refresh state when app resumes (e.g. user toggles permissions in system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (ContactsPermissions.hasPermissions(context)) {
                    currentStatus = ContactsPermissionStatus.Granted
                } else if (hasAskedOnce && currentStatus is ContactsPermissionStatus.Granted) {
                    // Revocation while in background
                    currentStatus = ContactsPermissionStatus.Denied
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(currentStatus) {
        ContactsPermissionState(
            status = currentStatus,
            requestPermissions = { launcher.launch(ContactsPermissions.REQUIRED_PERMISSIONS) },
            openSettings = { ContactsPermissions.openAppSettings(context) }
        )
    }
}

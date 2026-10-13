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

sealed interface CallLogPermissionStatus {
    data object Granted : CallLogPermissionStatus
    data object NotAsked : CallLogPermissionStatus
    data object Denied : CallLogPermissionStatus
    data object PermanentlyDenied : CallLogPermissionStatus
}

object CallLogPermissions {
    const val READ_CALL_LOG_PERMISSION = Manifest.permission.READ_CALL_LOG
    const val WRITE_CALL_LOG_PERMISSION = Manifest.permission.WRITE_CALL_LOG

    fun hasReadPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            READ_CALL_LOG_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            WRITE_CALL_LOG_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class CallLogPermissionState(
    val status: CallLogPermissionStatus,
    val hasWritePermission: Boolean,
    val requestPermission: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun rememberCallLogPermissionState(
    onPermissionChanged: (Boolean) -> Unit = {}
): CallLogPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAskedOnce by remember { mutableStateOf(false) }
    var currentStatus by remember {
        mutableStateOf<CallLogPermissionStatus>(
            if (CallLogPermissions.hasReadPermission(context)) {
                CallLogPermissionStatus.Granted
            } else {
                CallLogPermissionStatus.NotAsked
            }
        )
    }
    var canWrite by remember {
        mutableStateOf(CallLogPermissions.hasWritePermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAskedOnce = true
        val readGranted = results[Manifest.permission.READ_CALL_LOG] == true
        canWrite = results[Manifest.permission.WRITE_CALL_LOG] == true

        if (readGranted) {
            currentStatus = CallLogPermissionStatus.Granted
            onPermissionChanged(true)
        } else {
            onPermissionChanged(false)
            val activity = context as? Activity
            val shouldShowRationale = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_CALL_LOG
                )
            currentStatus = if (shouldShowRationale) {
                CallLogPermissionStatus.Denied
            } else {
                CallLogPermissionStatus.PermanentlyDenied
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasRead = CallLogPermissions.hasReadPermission(context)
                canWrite = CallLogPermissions.hasWritePermission(context)
                if (hasRead) {
                    currentStatus = CallLogPermissionStatus.Granted
                    onPermissionChanged(true)
                } else if (hasAskedOnce && currentStatus is CallLogPermissionStatus.Granted) {
                    currentStatus = CallLogPermissionStatus.Denied
                    onPermissionChanged(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(currentStatus, canWrite) {
        CallLogPermissionState(
            status = currentStatus,
            hasWritePermission = canWrite,
            requestPermission = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.WRITE_CALL_LOG
                    )
                )
            },
            openSettings = { CallLogPermissions.openAppSettings(context) }
        )
    }
}

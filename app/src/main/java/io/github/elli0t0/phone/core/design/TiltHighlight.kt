package io.github.elli0t0.phone.core.design

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlin.math.PI

/**
 * Device tilt as a -1..1 offset, low-pass filtered (0.08 per sample).
 * Rotation-vector sensor first, accelerometer fallback. The listener
 * is registered only while resumed (added on ON_RESUME, removed on
 * ON_PAUSE and on dispose), so no sensor runs while the screen is
 * hidden. Inactive — a static zero — when Dynamic highlights is off
 * or Animations is Off. All sensor setup is guarded: a missing
 * sensor or a registration failure just leaves tilt at zero.
 *
 * The value is read inside draw lambdas by premium glass, so tilt
 * updates redraw without recomposing.
 */
@Composable
fun rememberTilt(): State<Offset> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings = LocalAppSettings.current
    val enabled = settings.dynamicHighlights &&
        settings.animationOption != AnimationOption.OFF
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) {
            tilt.value = Offset.Zero
            onDispose { }
        } else {
            val sensorManager = runCatching {
                context.getSystemService(SensorManager::class.java)
            }.getOrNull()
            var listener: SensorEventListener? = null
            val register: () -> Unit = {
                runCatching {
                    val sm = sensorManager ?: return@runCatching
                    if (listener != null) return@runCatching
                    val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    val sensor = rotation
                        ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        ?: return@runCatching
                    val useRotation = sensor.type == Sensor.TYPE_ROTATION_VECTOR
                    listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            val (tx, ty) = if (useRotation) {
                                val matrix = FloatArray(9)
                                val orientation = FloatArray(3)
                                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                                SensorManager.getOrientation(matrix, orientation)
                                val halfPi = (PI.toFloat() / 2f)
                                (orientation[2] / halfPi).coerceIn(-1f, 1f) to
                                    (-orientation[1] / halfPi).coerceIn(-1f, 1f)
                            } else {
                                (-event.values[0] / 9.8f).coerceIn(-1f, 1f) to
                                    (event.values[1] / 9.8f).coerceIn(-1f, 1f)
                            }
                            val current = tilt.value
                            tilt.value = Offset(
                                current.x + (tx - current.x) * 0.08f,
                                current.y + (ty - current.y) * 0.08f
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
            val unregister: () -> Unit = {
                runCatching {
                    listener?.let { sensorManager?.unregisterListener(it) }
                    listener = null
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> register()
                    Lifecycle.Event.ON_PAUSE -> unregister()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                register()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                unregister()
            }
        }
    }
    return tilt
}

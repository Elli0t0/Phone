package io.github.elli0t0.phone.data.settings

import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable JSON backup of the DataStore prefs. Only the setting
 * name, a type tag, and the value are stored; device-specific
 * keys (adb_*, including the helper token) are excluded so a
 * backup can move to another phone. Unknown types are skipped
 * on encode and on decode, so newer backups stay readable.
 */
object SettingsBackup {
    const val FORMAT = "phone-settings"
    const val VERSION = 1
    private val excludedPrefixes = listOf("adb_")
    fun isPortable(name: String) = excludedPrefixes.none { name.startsWith(it) }

    fun encode(values: Map<String, Any>): String {
        val items = JSONObject()
        for ((name, value) in values.toSortedMap()) {
            if (!isPortable(name)) continue
            val e = JSONObject()
            when (value) {
                is Boolean -> e.put("t", "bool").put("v", value)
                is Int -> e.put("t", "int").put("v", value)
                is Long -> e.put("t", "long").put("v", value)
                is Float -> e.put("t", "float").put("v", value.toDouble())
                is Double -> e.put("t", "double").put("v", value)
                is String -> e.put("t", "string").put("v", value)
                is Set<*> -> e.put("t", "set").put("v", JSONArray(value.map { it.toString() }.sorted()))
                else -> continue
            }
            items.put(name, e)
        }
        return JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("settings", items).toString(2)
    }

    /** Throws IllegalArgumentException if it is not a valid backup. */
    fun decode(json: String): Map<String, Any> {
        try {
            val root = JSONObject(json)
            require(root.optString("format") == FORMAT) { "Not a Phone settings backup" }
            val items = root.getJSONObject("settings")
            val out = LinkedHashMap<String, Any>()
            for (name in items.keys()) {
                if (!isPortable(name)) continue
                val e = items.getJSONObject(name)
                val v: Any = when (e.getString("t")) {
                    "bool" -> e.getBoolean("v")
                    "int" -> e.getInt("v")
                    "long" -> e.getLong("v")
                    "float" -> e.getDouble("v").toFloat()
                    "double" -> e.getDouble("v")
                    "string" -> e.getString("v")
                    "set" -> e.getJSONArray("v").let { a ->
                        (0 until a.length()).map { a.getString(it) }.toSet()
                    }
                    else -> continue
                }
                out[name] = v
            }
            return out
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a Phone settings backup", e)
        }
    }
}

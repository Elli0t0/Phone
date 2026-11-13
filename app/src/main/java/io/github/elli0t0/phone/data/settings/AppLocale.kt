package io.github.elli0t0.phone.data.settings

import android.content.Context
import android.content.res.Configuration
import io.github.elli0t0.phone.data.settings.model.AppLanguage
import io.github.elli0t0.phone.data.settings.model.languageFromPref
import io.github.elli0t0.phone.data.settings.model.localeTag
import java.util.Locale

// The language also lives in a tiny SharedPreferences file, as a
// boot cache: DataStore is async, so attachBaseContext (which runs
// before anything can suspend) cannot read it. setLanguage writes
// both stores, so the cache only ever lags after a backup restore
// or a reset — and import/reset re-sync it too.
private const val LANGUAGE_PREFS = "app_language"
private const val KEY_LANGUAGE = "language"

/** The saved app language, read straight from the boot cache without
 * init, so it can be used in attachBaseContext. */
fun readAppLanguage(context: Context): AppLanguage {
    val prefs = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
    return languageFromPref(prefs.getString(KEY_LANGUAGE, null))
}

fun saveAppLanguage(context: Context, lang: AppLanguage) {
    context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_LANGUAGE, lang.name).apply()
}

/**
 * The context wrapped in the app language's locale, with the matching
 * layout direction (RTL for fa, LTR for en). Works on minSdk 29 without
 * AppCompat. Also sets the JVM default locale, so locale-aware
 * formatting (contact grouping, number display) follows the app
 * language.
 */
fun Context.withAppLocale(lang: AppLanguage): Context {
    val locale = Locale(lang.localeTag())
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

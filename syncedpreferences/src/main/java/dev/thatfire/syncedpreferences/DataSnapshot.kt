package dev.thatfire.syncedpreferences

import androidx.datastore.preferences.core.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.getValue

fun DataSnapshot.toPreferences(): Preferences {
    val preferences = emptyPreferences().toMutablePreferences()
    for (prefSnapshot in children) {
        val key = prefSnapshot.key!!
        val value = prefSnapshot.value!!
        preferences += when (value) {
            is String -> stringPreferencesKey(key) to value
            is Int -> intPreferencesKey(key) to value
            is Boolean -> booleanPreferencesKey(key) to value
            is Double -> doublePreferencesKey(key) to value
            is Float -> floatPreferencesKey(key) to value
            is Long -> longPreferencesKey(key) to value
            else -> {
                val stringSet = mutableSetOf<String>()
                for (string in prefSnapshot.children) {
                    string.getValue<String>()?.let { stringSet.add(it) }
                }
                stringSetPreferencesKey(key) to stringSet
            }
        }
    }
    return preferences
}
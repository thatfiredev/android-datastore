package dev.thatfire.syncedpreferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resumeWithException

class SyncedDataStore(
    uid: String,
    realtimeDatabase: FirebaseDatabase
) : DataStore<Preferences> {

    private var databaseRef = realtimeDatabase.getReference("synced_prefs").child(uid)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val data: Flow<Preferences>
        get() = callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val preferences = snapshot.toPreferences()
                    trySend(preferences)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            databaseRef.addValueEventListener(listener)

            awaitClose {
                databaseRef.removeEventListener(listener)
            }
        }

    override suspend fun updateData(transform: suspend (prefs: Preferences) -> Preferences): Preferences {
        val currentData = databaseRef.get().await()
        val currentPrefs = currentData.toPreferences()

        val finalPrefs = transform(currentPrefs)
        val finalData = mutableMapOf<String, Any>()
        for ((key, value) in finalPrefs.asMap()) {
            finalData[key.name] = value
        }
        databaseRef.setValue(finalData).await()

        return finalPrefs
    }
}
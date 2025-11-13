package mobappdev.example.nback_cimpl.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val HIGHSCORE = intPreferencesKey("highscore")
        val N_BACK = intPreferencesKey("n_back")
        val NUM_EVENTS = intPreferencesKey("num_events")
        val EVENT_INTERVAL = longPreferencesKey("event_interval")
        val NUM_COMBINATIONS = intPreferencesKey("num_combinations")
        val GRID_SIZE = intPreferencesKey("grid_size")
        const val TAG = "UserPreferencesRepo"
    }

    // Highscore
    val highscore: Flow<Int> = dataStore.data.mapToPreference(HIGHSCORE, 0)
    suspend fun saveHighScore(score: Int) {
        dataStore.edit { it[HIGHSCORE] = score }
    }

    // N-Back
    val nBack: Flow<Int> = dataStore.data.mapToPreference(N_BACK, 2)
    suspend fun saveNBack(nBack: Int) {
        dataStore.edit { it[N_BACK] = nBack }
    }

    // Number of Events
    val numberOfEvents: Flow<Int> = dataStore.data.mapToPreference(NUM_EVENTS, 20)
    suspend fun saveNumberOfEvents(numberOfEvents: Int) {
        dataStore.edit { it[NUM_EVENTS] = numberOfEvents }
    }

    // Event Interval
    val eventInterval: Flow<Long> = dataStore.data.mapToPreference(EVENT_INTERVAL, 2000L)
    suspend fun saveEventInterval(eventInterval: Long) {
        dataStore.edit { it[EVENT_INTERVAL] = eventInterval }
    }

    // Number of Combinations (for audio)
    val numberOfCombinations: Flow<Int> = dataStore.data.mapToPreference(NUM_COMBINATIONS, 15)
    suspend fun saveNumberOfCombinations(numberOfCombinations: Int) {
        dataStore.edit { it[NUM_COMBINATIONS] = numberOfCombinations }
    }

    // Grid Size (for visual)
    val gridSize: Flow<Int> = dataStore.data.mapToPreference(GRID_SIZE, 3)
    suspend fun saveGridSize(gridSize: Int) {
        dataStore.edit { it[GRID_SIZE] = gridSize }
    }

    // Reset all settings to default
    suspend fun resetSettings() {
        dataStore.edit {
            it.remove(N_BACK)
            it.remove(NUM_EVENTS)
            it.remove(EVENT_INTERVAL)
            it.remove(NUM_COMBINATIONS)
            it.remove(GRID_SIZE)
        }
    }

    /**
     * Helper function to map preferences flow and handle exceptions.
     */
    private fun <T> Flow<Preferences>.mapToPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return this.catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }
}

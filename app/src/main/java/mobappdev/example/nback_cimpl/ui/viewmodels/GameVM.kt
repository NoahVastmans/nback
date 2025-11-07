package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import java.util.Locale

enum class MatchResult {
    NONE, CORRECT, INCORRECT
}

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val isGameOver: StateFlow<Boolean>
    val matchResult: StateFlow<MatchResult>
    val progress: StateFlow<Float>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
    fun resetGame()
    fun setNBack(nBack: Int)
    fun setNumberOfEvents(numberOfEvents: Int)
    fun setMatchPercentage(percentage: Int)
    fun setEventInterval(interval: Long)
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application
) : GameViewModel, ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    private val _isGameOver = MutableStateFlow(false)
    override val isGameOver: StateFlow<Boolean>
        get() = _isGameOver.asStateFlow()

    private val _matchResult = MutableStateFlow(MatchResult.NONE)
    override val matchResult: StateFlow<MatchResult>
        get() = _matchResult.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float>
        get() = _progress.asStateFlow()

    private var job: Job? = null
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var currentEventIndex = 0
    private var matchCheckedForCurrentEvent = false

    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setNBack(nBack: Int) {
        _gameState.value = _gameState.value.copy(nBack = nBack)
    }

    override fun setNumberOfEvents(numberOfEvents: Int) {
        _gameState.value = _gameState.value.copy(numberOfEvents = numberOfEvents)
    }

    override fun setMatchPercentage(percentage: Int) {
        _gameState.value = _gameState.value.copy(matchPercentage = percentage)
    }

    override fun setEventInterval(interval: Long) {
        _gameState.value = _gameState.value.copy(eventInterval = interval)
    }

    override fun startGame() {
        job?.cancel()
        _score.value = 0
        currentEventIndex = 0
        _matchResult.value = MatchResult.NONE
        _progress.value = 0f

        val currentState = _gameState.value
        events = nBackHelper.generateNBackString(
            size = currentState.numberOfEvents,
            combinations = 9,
            percentMatch = currentState.matchPercentage,
            nBack = currentState.nBack
        ).toList().toTypedArray()

        job = viewModelScope.launch {
            when (currentState.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.Visual -> runVisualGame(events)
                GameType.AudioVisual -> runAudioVisualGame()
            }
            val currentHighscore = highscore.first()
            if (score.value > currentHighscore) {
                userPreferencesRepository.saveHighScore(score.value)
            }
            _isGameOver.value = true
        }
    }

    override fun checkMatch() {
        if (matchCheckedForCurrentEvent) return

        val nBack = _gameState.value.nBack
        if (currentEventIndex >= nBack && events[currentEventIndex] == events[currentEventIndex - nBack]) {
            _score.value++
            _matchResult.value = MatchResult.CORRECT
        } else {
            _matchResult.value = MatchResult.INCORRECT
        }
        matchCheckedForCurrentEvent = true
    }

    override fun resetGame() {
        _isGameOver.value = false
        _matchResult.value = MatchResult.NONE
        _progress.value = 0f
    }

    private suspend fun runAudioGame() {
        var waitTime = 0L
        while (!_ttsReady.value && waitTime < 3000L) {
            delay(100)
            waitTime += 100
        }
        if (!_ttsReady.value) return

        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for ((index, value) in events.withIndex()) {
            currentEventIndex = index
            _progress.value = (index + 1) / events.size.toFloat()
            matchCheckedForCurrentEvent = false
            _matchResult.value = MatchResult.NONE
            _gameState.value = _gameState.value.copy(eventValue = value)
            if (value in 1..letters.length) {
                tts?.speak(letters[value - 1].toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
            delay(_gameState.value.eventInterval)
        }
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for ((index, value) in events.withIndex()) {
            currentEventIndex = index
            _progress.value = (index + 1) / events.size.toFloat()
            matchCheckedForCurrentEvent = false
            _matchResult.value = MatchResult.NONE
            _gameState.value = _gameState.value.copy(eventValue = value)
            delay(_gameState.value.eventInterval)
        }
    }

    private fun runAudioVisualGame() {}

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository, application)
            }
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { _highscore.value = it }
        }
        try {
            tts = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    _ttsReady.value = true
                    tts?.language = Locale.US
                }
            }
        } catch (e: Exception) {
            Log.e("GameVM", "Could not create TTS instance", e)
        }
    }
}

enum class GameType { Audio, Visual, AudioVisual }

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1,
    val numberOfEvents: Int = 20,
    val matchPercentage: Int = 30,
    val nBack: Int = 2,
    val eventInterval: Long = 2000L
)

class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState> = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int> = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int> = MutableStateFlow(42).asStateFlow()
    override val isGameOver: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val matchResult: StateFlow<MatchResult> = MutableStateFlow(MatchResult.NONE).asStateFlow()
    override val progress: StateFlow<Float> = MutableStateFlow(0.5f).asStateFlow()

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkMatch() {}
    override fun resetGame() {}
    override fun setNBack(nBack: Int) {}
    override fun setNumberOfEvents(numberOfEvents: Int) {}
    override fun setMatchPercentage(percentage: Int) {}
    override fun setEventInterval(interval: Long) {}
}

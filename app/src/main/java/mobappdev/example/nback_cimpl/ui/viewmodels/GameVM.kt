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
    val totalMatches: StateFlow<Int>
    val isGameOver: StateFlow<Boolean>
    val visualMatchResult: StateFlow<MatchResult>
    val audioMatchResult: StateFlow<MatchResult>
    val progress: StateFlow<Float>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkVisualMatch()
    fun checkAudioMatch()
    fun resetGame()
    fun resetSettings()
    fun setNBack(nBack: Int)
    fun setNumberOfEvents(numberOfEvents: Int)
    fun setMatchPercentage(percentage: Int)
    fun setEventInterval(interval: Long)
    fun setNumberOfCombinations(combinations: Int)
    fun setGridSize(size: Int)
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

    private val _totalMatches = MutableStateFlow(0)
    override val totalMatches: StateFlow<Int>
        get() = _totalMatches.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    override val isGameOver: StateFlow<Boolean>
        get() = _isGameOver.asStateFlow()

    private val _visualMatchResult = MutableStateFlow(MatchResult.NONE)
    override val visualMatchResult: StateFlow<MatchResult>
        get() = _visualMatchResult.asStateFlow()

    private val _audioMatchResult = MutableStateFlow(MatchResult.NONE)
    override val audioMatchResult: StateFlow<MatchResult>
        get() = _audioMatchResult.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float>
        get() = _progress.asStateFlow()

    private var job: Job? = null
    private val nBackHelper = NBackHelper()
    private var visualEvents = emptyArray<Int>()
    private var audioEvents = emptyArray<Int>()
    private var currentEventIndex = 0
    private var visualMatchChecked = false
    private var audioMatchChecked = false

    private var tts: TextToSpeech? = null
    private val _ttsReady = MutableStateFlow(false)

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setNBack(nBack: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveNBack(nBack)
        }
    }

    override fun setNumberOfEvents(numberOfEvents: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveNumberOfEvents(numberOfEvents)
        }
    }

    override fun setMatchPercentage(percentage: Int) {
        // Not saved for now
    }

    override fun setEventInterval(interval: Long) {
        viewModelScope.launch {
            userPreferencesRepository.saveEventInterval(interval)
        }
    }

    override fun setNumberOfCombinations(combinations: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveNumberOfCombinations(combinations)
        }
    }

    override fun setGridSize(size: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveGridSize(size)
        }
    }
    
    override fun resetSettings() {
        viewModelScope.launch {
            userPreferencesRepository.resetSettings()
        }
    }

    override fun startGame() {
        job?.cancel()
        _score.value = 0
        currentEventIndex = 0
        resetChecks()
        _progress.value = 0f

        job = viewModelScope.launch {
            val currentState = _gameState.value
            val visualCombinations = currentState.gridSize * currentState.gridSize

            when (currentState.gameType) {
                GameType.Visual -> {
                    visualEvents = nBackHelper.generateNBackString(
                        size = currentState.numberOfEvents,
                        combinations = visualCombinations,
                        percentMatch = currentState.matchPercentage,
                        nBack = currentState.nBack
                    ).toList().toTypedArray()
                }
                GameType.Audio -> {
                    audioEvents = nBackHelper.generateNBackString(
                        size = currentState.numberOfEvents,
                        combinations = currentState.numberOfCombinations,
                        percentMatch = currentState.matchPercentage,
                        nBack = currentState.nBack
                    ).toList().toTypedArray()
                }
                GameType.AudioVisual -> {
                    visualEvents = nBackHelper.generateNBackString(
                        size = currentState.numberOfEvents,
                        combinations = visualCombinations,
                        percentMatch = currentState.matchPercentage,
                        nBack = currentState.nBack
                    ).toList().toTypedArray()

                    delay(1001) // Force a different seed for the C-code's time(NULL)

                    audioEvents = nBackHelper.generateNBackString(
                        size = currentState.numberOfEvents,
                        combinations = currentState.numberOfCombinations,
                        percentMatch = currentState.matchPercentage,
                        nBack = currentState.nBack
                    ).toList().toTypedArray()
                }
            }
            calculateTotalMatches()

            // Start the actual game loop
            when (currentState.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.Visual -> runVisualGame()
                GameType.AudioVisual -> runAudioVisualGame()
            }

            // After game finishes, check and save highscore
            val currentHighscore = highscore.first()
            if (score.value > currentHighscore) {
                userPreferencesRepository.saveHighScore(score.value)
            }
            _isGameOver.value = true
        }
    }

    private fun calculateTotalMatches() {
        val currentState = _gameState.value
        if (currentState.gameType != GameType.AudioVisual) {
            _totalMatches.value = (currentState.numberOfEvents * currentState.matchPercentage) / 100
        } else {
            val nBack = currentState.nBack
            var count = 0
            for (i in nBack until visualEvents.size) {
                val visualMatch = visualEvents[i] == visualEvents[i - nBack]
                val audioMatch = audioEvents[i] == audioEvents[i - nBack]
                if (visualMatch || audioMatch) {
                    count++
                }
            }
            _totalMatches.value = count
        }
    }

    override fun checkVisualMatch() {
        if (visualMatchChecked) return

        val nBack = _gameState.value.nBack
        if (currentEventIndex < nBack) {
            _visualMatchResult.value = MatchResult.INCORRECT
            visualMatchChecked = true
            return
        }

        val isMatch = visualEvents[currentEventIndex] == visualEvents[currentEventIndex - nBack]

        if (isMatch) {
            _visualMatchResult.value = MatchResult.CORRECT
            if (_gameState.value.gameType != GameType.AudioVisual) {
                _score.value++
            }
        } else {
            _visualMatchResult.value = MatchResult.INCORRECT
        }
        visualMatchChecked = true
    }

    override fun checkAudioMatch() {
        if (audioMatchChecked) return

        val nBack = _gameState.value.nBack
        if (currentEventIndex < nBack) {
            _audioMatchResult.value = MatchResult.INCORRECT
            audioMatchChecked = true
            return
        }

        val isMatch = audioEvents[currentEventIndex] == audioEvents[currentEventIndex - nBack]

        if (isMatch) {
            _audioMatchResult.value = MatchResult.CORRECT
            if (_gameState.value.gameType != GameType.AudioVisual) {
                _score.value++
            }
        } else {
            _audioMatchResult.value = MatchResult.INCORRECT
        }
        audioMatchChecked = true
    }

    override fun resetGame() {
        _isGameOver.value = false
        resetChecks()
        _progress.value = 0f
        _totalMatches.value = 0
    }

    private fun resetChecks() {
        visualMatchChecked = false
        audioMatchChecked = false
        _visualMatchResult.value = MatchResult.NONE
        _audioMatchResult.value = MatchResult.NONE
    }

    private suspend fun runAudioGame() {
        var waitTime = 0L
        while (!_ttsReady.value && waitTime < 3000L) {
            delay(100)
            waitTime += 100
        }
        if (!_ttsReady.value) return

        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for ((index, value) in audioEvents.withIndex()) {
            currentEventIndex = index
            _progress.value = (index + 1) / audioEvents.size.toFloat()
            resetChecks()
            _gameState.value = _gameState.value.copy(eventValue = value)
            if (value in 1..letters.length) {
                tts?.speak(letters[value - 1].toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
            delay(_gameState.value.eventInterval)
        }
    }

    private suspend fun runVisualGame() {
        for ((index, value) in visualEvents.withIndex()) {
            currentEventIndex = index
            _progress.value = (index + 1) / visualEvents.size.toFloat()
            resetChecks()
            _gameState.value = _gameState.value.copy(eventValue = value)
            delay(_gameState.value.eventInterval)
        }
    }

    private suspend fun runAudioVisualGame() {
        var waitTime = 0L
        while (!_ttsReady.value && waitTime < 3000L) {
            delay(100)
            waitTime += 100
        }
        if (!_ttsReady.value) return

        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val nBack = _gameState.value.nBack

        for (index in visualEvents.indices) {
            currentEventIndex = index
            _progress.value = (index + 1) / visualEvents.size.toFloat()
            resetChecks()

            // Set visual value
            _gameState.value = _gameState.value.copy(eventValue = visualEvents[index])

            // Speak audio value
            val audioValue = audioEvents[index]
            if (audioValue in 1..letters.length) {
                tts?.speak(letters[audioValue - 1].toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }

            delay(_gameState.value.eventInterval)

            // --- Scoring for dual mode ---
            val shouldHavePressedVisual = (index >= nBack && visualEvents[index] == visualEvents[index - nBack])
            val shouldHavePressedAudio = (index >= nBack && audioEvents[index] == audioEvents[index - nBack])

            val correctActions = (visualMatchChecked == shouldHavePressedVisual) && (audioMatchChecked == shouldHavePressedAudio)

            if (correctActions && (shouldHavePressedVisual || shouldHavePressedAudio)) {
                _score.value++
            }
        }
    }

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
        viewModelScope.launch {
            userPreferencesRepository.nBack.collect { nBack ->
                _gameState.value = _gameState.value.copy(nBack = nBack)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.numberOfEvents.collect { numEvents ->
                _gameState.value = _gameState.value.copy(numberOfEvents = numEvents)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.eventInterval.collect { interval ->
                _gameState.value = _gameState.value.copy(eventInterval = interval)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.numberOfCombinations.collect { numCombinations ->
                _gameState.value = _gameState.value.copy(numberOfCombinations = numCombinations)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.gridSize.collect { size ->
                _gameState.value = _gameState.value.copy(gridSize = size)
            }
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
    val eventInterval: Long = 2000L,
    val numberOfCombinations: Int = 15,
    val gridSize: Int = 3
)

class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState> = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int> = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int> = MutableStateFlow(42).asStateFlow()
    override val totalMatches: StateFlow<Int> = MutableStateFlow(5).asStateFlow()
    override val isGameOver: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val visualMatchResult: StateFlow<MatchResult> = MutableStateFlow(MatchResult.NONE).asStateFlow()
    override val audioMatchResult: StateFlow<MatchResult> = MutableStateFlow(MatchResult.NONE).asStateFlow()
    override val progress: StateFlow<Float> = MutableStateFlow(0.5f).asStateFlow()

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkVisualMatch() {}
    override fun checkAudioMatch() {}
    override fun resetGame() {}
    override fun resetSettings() {}
    override fun setNBack(nBack: Int) {}
    override fun setNumberOfEvents(numberOfEvents: Int) {}
    override fun setMatchPercentage(percentage: Int) {}
    override fun setEventInterval(interval: Long) {}
    override fun setNumberOfCombinations(combinations: Int) {}
    override fun setGridSize(size: Int) {}
}

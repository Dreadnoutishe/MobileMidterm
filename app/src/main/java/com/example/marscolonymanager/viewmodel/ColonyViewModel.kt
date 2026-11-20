package com.example.marscolonymanager.viewmodel

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marscolonymanager.data.ColonyRepository
import com.example.marscolonymanager.model.ColonyRules
import com.example.marscolonymanager.model.ColonyState
import com.example.marscolonymanager.model.MissionReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ColonyUiState(
    val colony: ColonyState? = null,
    val storageError: String? = null
)

class ColonyViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ColonyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ColonyUiState())
    val uiState = _uiState.asStateFlow()
    val crew = ColonyRules.crew
    val missions = ColonyRules.missions
    private val writes = Mutex()
    private var loadJob: Job? = null
    private var missionJob: Job? = null

    init {
        retryLoad()
    }

    fun retryLoad() {
        if (loadJob?.isActive == true) return
        if (_uiState.value.colony != null && _uiState.value.storageError == null) return
        _uiState.value = ColonyUiState()
        loadJob = viewModelScope.launch {
            try {
                val colony = writes.withLock {
                    repository.loadColony() ?: (restoreMidtermState() ?: ColonyState()).also {
                        repository.saveColony(it)
                    }
                }
                savedStateHandle.remove<Bundle>("colony")
                _uiState.value = ColonyUiState(colony)
                if (colony.missionInProgress) {
                    missionJob = viewModelScope.launch { runMission() }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportStorageError(error)
            }
        }
    }

    fun launchMission(missionId: Int, crewId: Int) {
        if (missionJob?.isActive == true) return
        missionJob = viewModelScope.launch {
            if (updateColony { ColonyRules.begin(it, missionId, crewId) }) runMission()
        }
    }

    fun resetColony() {
        viewModelScope.launch {
            writes.withLock {
                if (_uiState.value.colony == null || _uiState.value.storageError != null) return@withLock
                if (saveColony(ColonyState())) {
                    missionJob?.cancel()
                    missionJob = null
                }
            }
        }
    }

    private suspend fun runMission() {
        val crewId = _uiState.value.colony?.activeCrewId ?: return
        val member = crew.first { it.id == crewId }
        val duration = ColonyRules.durationSeconds(member)
        while (_uiState.value.colony?.missionInProgress == true) {
            // A midterm save can already be at the final second. Complete it without waiting again.
            if ((_uiState.value.colony?.progressSeconds ?: 0) < duration) delay(1_000L)
            val saved = updateColony { current ->
                if (!current.missionInProgress) current else {
                    val next = current.copy(progressSeconds = (current.progressSeconds + 1).coerceAtMost(duration))
                    if (next.progressSeconds == duration) ColonyRules.complete(next) else next
                }
            }
            if (!saved) return
        }
    }

    private suspend fun updateColony(transform: (ColonyState) -> ColonyState): Boolean = writes.withLock {
        val current = _uiState.value.colony ?: return@withLock false
        if (_uiState.value.storageError != null) return@withLock false
        val next = transform(current)
        next != current && saveColony(next)
    }

    private suspend fun saveColony(colony: ColonyState): Boolean {
        return try {
            repository.saveColony(colony)
            _uiState.value = ColonyUiState(colony)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            reportStorageError(error)
            false
        }
    }

    private fun reportStorageError(error: Exception) {
        Log.e("Expedition", "Unable to access expedition save", error)
        _uiState.update { it.copy(storageError = "Could not access the expedition save. Try again.") }
    }

    @Suppress("DEPRECATION")
    private fun restoreMidtermState(): ColonyState? = runCatching {
        val saved = savedStateHandle.get<Bundle>("colony") ?: return@runCatching null
        require(saved.getInt("version") == 1)
        val reports = saved.getParcelableArrayList<Bundle>("reports").orEmpty().map { record ->
            val before = requireNotNull(record.getFloatArray("before"))
            val after = requireNotNull(record.getFloatArray("after"))
            require(before.size == 3 && after.size == 3)
            MissionReport(record.getInt("sol"), record.getString("mission").orEmpty(), record.getString("crew").orEmpty(),
                before[0], before[1], before[2], after[0], after[1], after[2], record.getInt("research"), record.getBoolean("bonus"))
        }
        val state = ColonyState(sol = saved.getInt("sol"), oxygen = saved.getFloat("oxygen"),
            energy = saved.getFloat("energy"), food = saved.getFloat("food"), researchPoints = saved.getInt("research"),
            completedSols = saved.getInt("completed"), restingCrewId = saved.getInt("resting").takeIf { it != -1 },
            activeMissionId = saved.getInt("mission").takeIf { it != -1 }, activeCrewId = saved.getInt("crew").takeIf { it != -1 },
            progressSeconds = saved.getInt("progress"), reports = reports)
        require(state.sol in 1..ColonyRules.TARGET_SOLS && state.completedSols == reports.size && reports.size <= ColonyRules.TARGET_SOLS)
        require(listOf(state.oxygen, state.energy, state.food).all { it.isFinite() && it in 0f..100f })
        require(state.progressSeconds in 0..5)
        require(state.restingCrewId == null || ColonyRules.crew.any { it.id == state.restingCrewId })
        if (state.missionInProgress) {
            require(!state.isFinished && ColonyRules.missions.any { it.id == state.activeMissionId })
            require(ColonyRules.crew.any { it.id == state.activeCrewId } && state.activeCrewId != state.restingCrewId)
        } else require(state.activeCrewId == null)
        state
    }.getOrNull()
}

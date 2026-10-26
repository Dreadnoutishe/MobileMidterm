package com.example.marscolonymanager.viewmodel

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marscolonymanager.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ColonyViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _colonyState = MutableStateFlow(restore() ?: ColonyState())
    val colonyState: StateFlow<ColonyState> = _colonyState.asStateFlow()
    val crew: List<CrewMember> = ColonyRules.crew
    val missions: List<Mission> = ColonyRules.missions
    private var missionJob: Job? = null

    init { if (_colonyState.value.missionInProgress) runMission() }

    fun launchMission(missionId: Int, crewId: Int) {
        val current = _colonyState.value
        val next = ColonyRules.begin(current, missionId, crewId)
        if (next == current) return
        publish(next) // Mark busy immediately, preventing duplicate launches.
        runMission()
    }

    private fun runMission() {
        if (missionJob?.isActive == true) return
        missionJob = viewModelScope.launch {
            val member = crew.find { it.id == _colonyState.value.activeCrewId } ?: return@launch
            val duration = ColonyRules.durationSeconds(member)
            while (_colonyState.value.progressSeconds < duration) {
                delay(1_000L) // Suspends this coroutine; the main thread can still render and navigate.
                val current = _colonyState.value
                if (!current.missionInProgress) return@launch
                publish(current.copy(progressSeconds = current.progressSeconds + 1))
            }
            publish(ColonyRules.complete(_colonyState.value))
        }
    }

    fun resetColony() {
        missionJob?.cancel()
        missionJob = null
        publish(ColonyState())
    }

    private fun publish(state: ColonyState) {
        _colonyState.value = state
        savedStateHandle["colony"] = Bundle().apply {
            putInt("version", 1); putInt("sol", state.sol)
            putFloat("oxygen", state.oxygen); putFloat("energy", state.energy); putFloat("food", state.food)
            putInt("research", state.researchPoints); putInt("completed", state.completedSols)
            putInt("resting", state.restingCrewId ?: -1); putInt("mission", state.activeMissionId ?: -1)
            putInt("crew", state.activeCrewId ?: -1); putInt("progress", state.progressSeconds)
            putParcelableArrayList("reports", ArrayList(state.reports.map { report -> Bundle().apply {
                putInt("sol", report.sol); putString("mission", report.missionName); putString("crew", report.crewName)
                putFloatArray("before", floatArrayOf(report.oxygenBefore, report.energyBefore, report.foodBefore))
                putFloatArray("after", floatArrayOf(report.oxygenAfter, report.energyAfter, report.foodAfter))
                putInt("research", report.researchGain); putBoolean("bonus", report.specialistBonus)
            } }))
        }
    }

    @Suppress("DEPRECATION")
    private fun restore(): ColonyState? = runCatching {
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

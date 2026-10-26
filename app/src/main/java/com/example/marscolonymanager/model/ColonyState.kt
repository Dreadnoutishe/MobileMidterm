package com.example.marscolonymanager.model

enum class ColonyStatus { OPERATIONAL, WARNING, CRITICAL, FAILED }

data class ColonyState(
    val name: String = "Ares-1",
    val sol: Int = 1,
    val oxygen: Float = 65f,
    val energy: Float = 60f,
    val food: Float = 60f,
    val researchPoints: Int = 0,
    val completedSols: Int = 0,
    val restingCrewId: Int? = null,
    val activeMissionId: Int? = null,
    val activeCrewId: Int? = null,
    val progressSeconds: Int = 0,
    val reports: List<MissionReport> = emptyList()
) {
    val status: ColonyStatus get() = when {
        minOf(oxygen, energy, food) <= 0f -> ColonyStatus.FAILED
        minOf(oxygen, energy, food) < 20f -> ColonyStatus.CRITICAL
        minOf(oxygen, energy, food) < 40f -> ColonyStatus.WARNING
        else -> ColonyStatus.OPERATIONAL
    }
    val isOperational: Boolean get() = status != ColonyStatus.FAILED
    val hasWon: Boolean get() = completedSols >= ColonyRules.TARGET_SOLS && isOperational
    val isFinished: Boolean get() = hasWon || !isOperational
    val missionInProgress: Boolean get() = activeMissionId != null
    val score: Int get() = researchPoints + ((oxygen + energy + food) * 2).toInt() + if (hasWon) 300 else 0
}

package com.example.marscolonymanager.model

data class Mission(
    val id: Int,
    val name: String,
    val location: String,
    val description: String,
    val preferredSkill: String,
    val researchReward: Int,
    val oxygenReward: Float = 0f,
    val energyReward: Float = 0f,
    val foodReward: Float = 0f,
    val energyCost: Float = 0f
)

/** Forecast and completion share these values, so the preview is the actual outcome. */
data class MissionForecast(
    val oxygen: Float,
    val energy: Float,
    val food: Float,
    val researchGain: Int,
    val specialistBonus: Boolean
) {
    val causesFailure: Boolean get() = minOf(oxygen, energy, food) <= 0f
}

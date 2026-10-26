package com.example.marscolonymanager.model

data class MissionReport(
    val sol: Int,
    val missionName: String,
    val crewName: String,
    val oxygenBefore: Float,
    val energyBefore: Float,
    val foodBefore: Float,
    val oxygenAfter: Float,
    val energyAfter: Float,
    val foodAfter: Float,
    val researchGain: Int,
    val specialistBonus: Boolean
)

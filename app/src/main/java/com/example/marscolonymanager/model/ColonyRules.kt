package com.example.marscolonymanager.model

/** Small, pure Kotlin rules object. The original model/viewmodel/ui structure is retained. */
object ColonyRules {
    const val TARGET_SOLS = 7
    const val OXYGEN_UPKEEP = 14f
    const val ENERGY_UPKEEP = 12f
    const val FOOD_UPKEEP = 13f
    const val SPECIALIST_BONUS = 8f

    val crew = listOf(
        CrewMember(1, "Alex", 32, "Engineer", setOf("Engineering", "Repair", "Construction")),
        CrewMember(2, "Dr. Miller", 45, "Scientist", setOf("Research", "Biology", "Geology")),
        CrewMember(3, "Sarah", 29, "Pilot", setOf("Piloting", "Navigation", "Exploration")),
        CrewMember(4, "Emma", 28, "Engineer", setOf("Engineering", "Repair")),
        CrewMember(5, "David", 38, "Scientist", setOf("Research", "Geology"))
    )
    val missions = listOf(
        Mission(1, "Search for Water", "Valles Marineris", "Extract ice and replenish the colony's oxygen reserves.",
            "Geology", 100, oxygenReward = 38f, energyCost = 8f),
        Mission(2, "Solar Array Repair", "Solar Field Alpha", "Restore solar output and recharge the colony's batteries.",
            "Engineering", 120, energyReward = 40f),
        Mission(3, "Greenhouse Expedition", "Gale Crater", "Harvest the greenhouse and restock the colony's food supplies.",
            "Biology", 150, foodReward = 38f, energyCost = 6f)
    )

    fun durationSeconds(member: CrewMember): Int = if (member.isPilot) 3 else 5
    fun forecast(colony: ColonyState, mission: Mission, member: CrewMember): MissionForecast {
        val bonus = if (member.matches(mission)) SPECIALIST_BONUS else 0f
        // Storage cap applies before daily upkeep. A full store cannot bank overflow supplies.
        return MissionForecast(
            oxygen = ((colony.oxygen + mission.oxygenReward + if (mission.oxygenReward > 0) bonus else 0f)
                .coerceAtMost(100f) - OXYGEN_UPKEEP).coerceAtLeast(0f),
            energy = ((colony.energy - mission.energyCost + mission.energyReward + if (mission.energyReward > 0) bonus else 0f)
                .coerceAtMost(100f) - ENERGY_UPKEEP).coerceAtLeast(0f),
            food = ((colony.food + mission.foodReward + if (mission.foodReward > 0) bonus else 0f)
                .coerceAtMost(100f) - FOOD_UPKEEP).coerceAtLeast(0f),
            researchGain = mission.researchReward + (if (bonus > 0) 25 else 0) + (if (member.isPilot) 25 else 0),
            specialistBonus = bonus > 0
        )
    }
    fun launchProblem(colony: ColonyState, mission: Mission, member: CrewMember): String? = when {
        colony.isFinished -> "This expedition is over. Start a new colony to play again."
        colony.missionInProgress -> "A mission is already in progress."
        member.id == colony.restingCrewId -> "${member.name} must rest for one mission. Choose another leader."
        colony.energy < mission.energyCost -> "This mission requires ${mission.energyCost.toInt()} energy before launch."
        else -> null
    }
    fun begin(colony: ColonyState, missionId: Int, crewId: Int): ColonyState {
        val mission = missions.find { it.id == missionId } ?: return colony
        val member = crew.find { it.id == crewId } ?: return colony
        if (launchProblem(colony, mission, member) != null) return colony
        return colony.copy(activeMissionId = missionId, activeCrewId = crewId, progressSeconds = 0)
    }
    fun complete(colony: ColonyState): ColonyState {
        val mission = missions.find { it.id == colony.activeMissionId } ?: return colony
        val member = crew.find { it.id == colony.activeCrewId } ?: return colony
        if (colony.isFinished || colony.progressSeconds < durationSeconds(member)) return colony
        val result = forecast(colony, mission, member)
        val completed = colony.completedSols + 1
        val report = MissionReport(colony.sol, mission.name, member.name,
            colony.oxygen, colony.energy, colony.food, result.oxygen, result.energy, result.food,
            result.researchGain, result.specialistBonus)
        return colony.copy(
            sol = (completed + 1).coerceAtMost(TARGET_SOLS), completedSols = completed,
            oxygen = result.oxygen, energy = result.energy, food = result.food,
            researchPoints = colony.researchPoints + result.researchGain,
            restingCrewId = member.id, activeMissionId = null, activeCrewId = null, progressSeconds = 0,
            reports = colony.reports + report
        )
    }
}

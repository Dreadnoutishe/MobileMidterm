package com.example.marscolonymanager.model

data class CrewMember(
    val id: Int,
    val name: String,
    val age: Int,
    val role: String,
    val skills: Set<String>
) {
    val isPilot: Boolean get() = "Piloting" in skills
    fun matches(mission: Mission): Boolean = mission.preferredSkill in skills
}

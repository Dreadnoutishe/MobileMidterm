package com.example.marscolonymanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun CrewCard(member: CrewMember, colony: ColonyState, onAssign: () -> Unit) {
    val busy = colony.activeCrewId == member.id
    val resting = colony.restingCrewId == member.id
    val missionsLed = colony.reports.count { it.crewName == member.name }
    val available = !busy && !resting && !colony.isFinished && !colony.missionInProgress
    ColonyPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(SpaceSurfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                Text(member.name.take(1), style = MaterialTheme.typography.titleLarge, color = MarsOrange)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleLarge)
                Text("${member.role} · ${member.age} years", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionLabel(when { busy -> "ON MISSION"; resting -> "RESTING THIS SOL"; else -> "READY FOR DUTY" },
            if (busy) MarsOrange else if (resting) EnergyColor else FoodColor)
        Text(member.skills.joinToString(" · "), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(if (member.isPilot) "Pilot perk: missions take 3 seconds and earn +25 research."
            else "Specialist perk: matching a mission skill adds 8 supplies and 25 research.", style = MaterialTheme.typography.bodyMedium)
        Text("$missionsLed missions led", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onAssign, enabled = available, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (busy) "Leading a mission" else if (resting) "Available after another mission" else "Assign to a mission")
        }
    }
}

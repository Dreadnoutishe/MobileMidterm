package com.example.marscolonymanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marscolonymanager.model.*
import com.example.marscolonymanager.ui.components.*
import com.example.marscolonymanager.ui.theme.*

@Composable
fun CrewScreen(crew: List<CrewMember>, colony: ColonyState, onAssign: (Int) -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredCrew = remember(crew, searchQuery) {
        crew.filter { it.name.contains(searchQuery, true) || it.role.contains(searchQuery, true) || it.skills.any { skill -> skill.contains(searchQuery, true) } }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "crew_header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("FIVE PEOPLE / ONE SHARED FUTURE", MarsOrange)
                Text("The people of Ares-1.", style = MaterialTheme.typography.headlineMedium)
                Text("Match skills to missions. Each leader rests for one mission after returning.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("Search name, role, or skill") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("${filteredCrew.size} crew members", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        if (filteredCrew.isEmpty()) item { Text("No crew match this search. Try a name or a skill such as Biology.", color = TextSecondary) }
        items(filteredCrew, key = { it.id }) { member -> CrewCard(member, colony, onAssign = { onAssign(member.id) }) }
    }
}

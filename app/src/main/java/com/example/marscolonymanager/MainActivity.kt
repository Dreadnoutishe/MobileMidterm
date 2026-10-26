package com.example.marscolonymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.marscolonymanager.ui.components.SectionLabel
import com.example.marscolonymanager.ui.screens.*
import com.example.marscolonymanager.ui.theme.*
import com.example.marscolonymanager.viewmodel.ColonyViewModel

class MainActivity : ComponentActivity() {
    private val colonyViewModel: ColonyViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MarsColonyManagerTheme { ColonyApp(colonyViewModel) } }
    }
}

private data class ColonyTab(val route: String, val title: String, val icon: ImageVector)

@Composable
private fun ColonyApp(viewModel: ColonyViewModel) {
    val colony by viewModel.colonyState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: "colony"
    val snackbar = remember { SnackbarHostState() }
    var selectedCrewId by rememberSaveable { mutableIntStateOf(2) }
    var resultShown by rememberSaveable { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    val tabs = remember { listOf(
        ColonyTab("colony", "Colony", Icons.Rounded.Home),
        ColonyTab("missions", "Missions", Icons.Rounded.RocketLaunch),
        ColonyTab("crew", "Crew", Icons.Rounded.Groups),
        ColonyTab("reports", "Reports", Icons.Rounded.History)
    ) }
    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(colony.isFinished) {
        if (colony.isFinished && !resultShown) { resultShown = true; navigate("reports") }
        if (!colony.isFinished) resultShown = false
    }
    LaunchedEffect(colony.completedSols) {
        colony.reports.lastOrNull()?.let { report ->
            if (!colony.isFinished) snackbar.showSnackbar("Sol ${report.sol} complete · +${report.researchGain} research")
        }
    }
    Surface(color = SpaceBackground, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Scaffold(containerColor = SpaceBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("MARS COLONY MANAGER", MarsOrange)
                            Text(tabs.first { it.route == currentRoute }.title, style = MaterialTheme.typography.titleLarge)
                        }
                        IconButton(onClick = { showHelp = true }) { Icon(Icons.Rounded.HelpOutline, "How to play", tint = TextSecondary) }
                        IconButton(onClick = { showReset = true }) { Icon(Icons.Rounded.RestartAlt, "Restart colony", tint = TextSecondary) }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = SpaceSurface, windowInsets = WindowInsets(0, 0, 0, 0)) {
                        tabs.forEach { tab -> NavigationBarItem(selected = currentRoute == tab.route,
                            onClick = { navigate(tab.route) }, icon = { Icon(tab.icon, null) }, label = { Text(tab.title) }) }
                    }
                }, snackbarHost = { SnackbarHost(snackbar) }
            ) { innerPadding ->
                NavHost(navController, startDestination = "colony", modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    composable("colony") { DashboardScreen(colony, onMissions = { navigate("missions") }, onReports = { navigate("reports") }) }
                    composable("missions") {
                        MissionsScreen(colony, viewModel.missions, viewModel.crew, selectedCrewId,
                            onSelectCrew = { selectedCrewId = it }, onLaunchMission = viewModel::launchMission,
                            onReports = { navigate("reports") })
                    }
                    composable("crew") {
                        CrewScreen(viewModel.crew, colony, onAssign = { selectedCrewId = it; navigate("missions") })
                    }
                    composable("reports") {
                        ReportsScreen(colony, onMissions = { navigate("missions") }, onRestart = { showReset = true })
                    }
                }
            }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Your first seven sols") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Survive seven missions with oxygen, energy, and food above zero. Completing a mission advances one Martian day.")
            Text("Choose a mission and a crew leader. Matching the mission's preferred skill adds 8 supplies and 25 research. Sarah's Pilot skill makes missions take 3 seconds and adds 25 research; other leaders take 5 seconds.")
            Text("The leader rests for the next mission. Other crew remain available. You can assign a leader from Crew or use the selector on Missions.")
            Text("Every completion consumes 14 oxygen, 12 energy, and 13 food. Ice extraction also costs 8 energy; the greenhouse costs 6. Rewards fill storage up to 100 before daily consumption.")
            Text("Read the exact forecast before launching. Supplies do not drain while you plan. Missions continue through screen changes and rotation; duplicate launches are blocked.")
            Text("Score = research + twice the remaining supplies + 300 for surviving. Reports explain each mission. Use the restart icon to begin again.")
        } }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Ready for Mars") } })
    if (showReset) AlertDialog(onDismissRequest = { showReset = false }, title = { Text("Start a new colony?") },
        text = { Text("This resets the expedition, reports, and crew rest status. Any active mission will be cancelled.") },
        confirmButton = { TextButton(onClick = {
            viewModel.resetColony(); selectedCrewId = 2; resultShown = false; showReset = false; navigate("colony")
        }) { Text("Start again") } },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("Keep playing") } })
}

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.marscolonymanager.data.ColonyDatabase
import com.example.marscolonymanager.data.ColonyRepository
import com.example.marscolonymanager.data.WorkOrderRepository
import com.example.marscolonymanager.model.ColonyState
import com.example.marscolonymanager.ui.components.SectionLabel
import com.example.marscolonymanager.ui.screens.*
import com.example.marscolonymanager.ui.theme.*
import com.example.marscolonymanager.viewmodel.ColonyViewModel
import com.example.marscolonymanager.viewmodel.WorkOrderFormViewModel
import com.example.marscolonymanager.viewmodel.WorkOrderViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { ColonyDatabase.getInstance(applicationContext) }
    private val workOrders by lazy { WorkOrderRepository(database) }
    private val colonyViewModel: ColonyViewModel by viewModels {
        viewModelFactory {
            initializer { ColonyViewModel(createSavedStateHandle(), ColonyRepository(database)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MarsColonyManagerTheme { ColonyApp(colonyViewModel, workOrders) } }
    }
}

private const val MAINTENANCE = "maintenance"
private const val NEW_ORDER = "maintenance/new"
private const val ORDER_DETAIL = "maintenance/detail/{orderId}"
private const val EDIT_ORDER = "maintenance/edit/{orderId}"

private data class ColonyTab(val route: String, val title: String, val icon: ImageVector)

@Composable
private fun ColonyApp(viewModel: ColonyViewModel, workOrders: WorkOrderRepository) {
    val expedition by viewModel.uiState.collectAsStateWithLifecycle()
    val colony = expedition.colony
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: "colony"
    val isMaintenance = currentRoute.startsWith(MAINTENANCE)
    val hasBack = isMaintenance && currentRoute != MAINTENANCE
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedCrewId by rememberSaveable { mutableIntStateOf(2) }
    var resultShown by rememberSaveable { mutableStateOf(false) }
    var previousCompletedSols by rememberSaveable { mutableStateOf<Int?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    val tabs = remember {
        listOf(
            ColonyTab("colony", "Colony", Icons.Rounded.Home),
            ColonyTab("missions", "Missions", Icons.Rounded.RocketLaunch),
            ColonyTab("crew", "Crew", Icons.Rounded.Groups),
            ColonyTab("reports", "Reports", Icons.Rounded.History),
            ColonyTab(MAINTENANCE, "Maintenance", Icons.Rounded.Build)
        )
    }

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    LaunchedEffect(colony?.isFinished, isMaintenance) {
        if (colony?.isFinished == true && !resultShown && !isMaintenance) {
            resultShown = true
            navigate("reports")
        }
        if (colony?.isFinished == false) resultShown = false
    }

    LaunchedEffect(colony?.completedSols) {
        if (colony == null) return@LaunchedEffect
        val previous = previousCompletedSols
        previousCompletedSols = colony.completedSols
        if (previous != null && colony.completedSols > previous) {
            val report = colony.reports.last()
            if (!colony.isFinished || isMaintenance) {
                showMessage("Sol ${report.sol} complete · +${report.researchGain} research")
            }
        }
    }

    val screenTitle = when (currentRoute) {
        NEW_ORDER -> "New work order"
        ORDER_DETAIL -> "Work order"
        EDIT_ORDER -> "Edit work order"
        else -> tabs.firstOrNull { it.route == currentRoute }?.title.orEmpty()
    }

    Surface(color = SpaceBackground, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Scaffold(
                containerColor = SpaceBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Row(
                        Modifier.fillMaxWidth().padding(start = if (hasBack) 4.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextSecondary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            SectionLabel("MARS COLONY MANAGER", MarsOrange)
                            Text(screenTitle, style = MaterialTheme.typography.titleLarge)
                        }
                        if (!isMaintenance) {
                            IconButton(onClick = { showHelp = true }) {
                                Icon(Icons.AutoMirrored.Rounded.HelpOutline, "How to play", tint = TextSecondary)
                            }
                            IconButton(onClick = { showReset = true }, enabled = colony != null && expedition.storageError == null) {
                                Icon(Icons.Rounded.RestartAlt, "Restart colony")
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!hasBack) {
                        NavigationBar(containerColor = SpaceSurface, windowInsets = WindowInsets(0, 0, 0, 0)) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigate(tab.route) },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.title, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { innerPadding ->
                NavHost(navController, startDestination = "colony", modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    composable("colony") {
                        ExpeditionContent(colony, expedition.storageError, viewModel::retryLoad) {
                            DashboardScreen(it, onMissions = { navigate("missions") }, onReports = { navigate("reports") })
                        }
                    }
                    composable("missions") {
                        ExpeditionContent(colony, expedition.storageError, viewModel::retryLoad) {
                            MissionsScreen(it, viewModel.missions, viewModel.crew, selectedCrewId,
                                onSelectCrew = { id -> selectedCrewId = id }, onLaunchMission = viewModel::launchMission,
                                onReports = { navigate("reports") })
                        }
                    }
                    composable("crew") {
                        ExpeditionContent(colony, expedition.storageError, viewModel::retryLoad) {
                            CrewScreen(viewModel.crew, it, onAssign = { id ->
                                selectedCrewId = id
                                navigate("missions")
                            })
                        }
                    }
                    composable("reports") {
                        ExpeditionContent(colony, expedition.storageError, viewModel::retryLoad) {
                            ReportsScreen(it, onMissions = { navigate("missions") }, onRestart = { showReset = true })
                        }
                    }
                    composable(MAINTENANCE) {
                        val screenModel: WorkOrderViewModel = viewModel(
                            factory = viewModelFactory { initializer { WorkOrderViewModel(workOrders) } }
                        )
                        val state by screenModel.orders.collectAsStateWithLifecycle()
                        MaintenanceScreen(
                            orders = state.data,
                            loadError = state.loadError,
                            onRetry = screenModel::retry,
                            onCreate = { navController.navigate(NEW_ORDER) },
                            onOpen = { id -> navController.navigate("maintenance/detail/$id") }
                        )
                    }
                    composable(ORDER_DETAIL, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) { detailEntry ->
                        val id = requireNotNull(detailEntry.arguments).getLong("orderId")
                        WorkOrderDetailRoute(
                            workOrders, id,
                            onEdit = { navController.navigate("maintenance/edit/$id") },
                            onBack = { navController.popBackStack() },
                            showMessage = { showMessage(it) }
                        )
                    }
                    composable(NEW_ORDER) {
                        WorkOrderFormRoute(
                            workOrders, orderId = null,
                            onSaved = { id ->
                                navController.popBackStack()
                                navController.navigate("maintenance/detail/$id")
                                showMessage("Work order created.")
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable(EDIT_ORDER, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) { editEntry ->
                        val id = requireNotNull(editEntry.arguments).getLong("orderId")
                        WorkOrderFormRoute(
                            workOrders, id,
                            onSaved = {
                                navController.popBackStack()
                                showMessage("Work order updated.")
                            },
                            onCancel = { navController.popBackStack() }
                        )
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
            viewModel.resetColony()
            selectedCrewId = 2
            resultShown = false
            showReset = false
            navigate("colony")
        }) { Text("Start again") } },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("Keep playing") } })
}

@Composable
private fun ExpeditionContent(
    colony: ColonyState?,
    error: String?,
    onRetry: () -> Unit,
    content: @Composable (ColonyState) -> Unit
) {
    when {
        error != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(error)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        }
        colony == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> content(colony)
    }
}

@Composable
private fun WorkOrderDetailRoute(
    repository: WorkOrderRepository,
    orderId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit
) {
    val screenModel: WorkOrderViewModel = viewModel(
        factory = viewModelFactory { initializer { WorkOrderViewModel(repository, orderId) } }
    )
    val state by screenModel.detail.collectAsStateWithLifecycle()
    val isSaving by screenModel.isSaving.collectAsStateWithLifecycle()
    val feedback by screenModel.feedback.collectAsStateWithLifecycle()
    LaunchedEffect(feedback) {
        feedback?.let { result ->
            screenModel.dismissFeedback()
            if (result.deleted) onBack()
            showMessage(result.message)
        }
    }
    WorkOrderDetailScreen(
        order = state.data,
        isLoading = state.isLoading,
        loadError = state.loadError,
        isSaving = isSaving,
        onRetry = screenModel::retry,
        onEdit = onEdit,
        onDelete = screenModel::delete,
        onStart = screenModel::start,
        onCheck = screenModel::setCheck,
        onComplete = screenModel::complete,
        onBack = onBack
    )
}

@Composable
private fun WorkOrderFormRoute(
    repository: WorkOrderRepository,
    orderId: Long?,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val screenModel: WorkOrderFormViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WorkOrderFormViewModel(repository, createSavedStateHandle(), orderId) }
        }
    )
    val state by screenModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedOrderId) {
        state.savedOrderId?.let(onSaved)
    }
    WorkOrderFormScreen(
        state = state,
        isEditing = orderId != null,
        onTitleChange = screenModel::changeTitle,
        onLocationChange = screenModel::changeLocation,
        onNotesChange = screenModel::changeNotes,
        onRetry = screenModel::retry,
        onSave = screenModel::save,
        onCancel = onCancel
    )
}

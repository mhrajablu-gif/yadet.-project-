package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.ReminderAlarmManager
import com.example.data.local.CalendarSystem
import com.example.data.local.ReminderEntity
import com.example.ui.YadetDestination
import com.example.ui.YadetViewModel
import com.example.ui.components.ReminderEditorDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AllTasksScreen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.YadetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: YadetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialReminderId = intent?.getLongExtra(ReminderAlarmManager.EXTRA_REMINDER_ID, -1L) ?: -1L

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

            YadetTheme(themeMode = settings.themeMode) {
                if (settings.appLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        onUnlock = { pin -> viewModel.unlockWithPin(pin) }
                    )
                } else {
                    YadetMainApp(
                        viewModel = viewModel,
                        initialReminderId = initialReminderId
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLockScreen(onUnlock: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "قفل برنامه",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.height(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("یادت قفل شده است", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    error = false
                },
                label = { Text("رمز PIN را وارد کنید") },
                visualTransformation = PasswordVisualTransformation(),
                isError = error,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error) {
                Text("رمز اشتباه است", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!onUnlock(pin)) {
                        error = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("باز کردن قفل")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YadetMainApp(
    viewModel: YadetViewModel,
    initialReminderId: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val multiModelConfig by viewModel.multiModelConfig.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val trashedReminders by viewModel.trashedReminders.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val memoryItems by viewModel.memoryItems.collectAsStateWithLifecycle()
    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val statsRange by viewModel.statsRange.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var showCreateReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    // Request POST_NOTIFICATIONS on Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(initialReminderId, reminders) {
        if (initialReminderId > 0 && editingReminder == null) {
            reminders.find { it.id == initialReminderId }?.let {
                editingReminder = it
            }
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusBanner()
        }
    }

    BackHandler(enabled = currentDest != YadetDestination.DASHBOARD) {
        viewModel.navigateTo(YadetDestination.DASHBOARD)
    }

    val drawerItems: List<Pair<YadetDestination, ImageVector>> = listOf(
        YadetDestination.DASHBOARD to Icons.Default.Dashboard,
        YadetDestination.ALL_TASKS to Icons.Default.CheckCircle,
        YadetDestination.CALENDAR to Icons.Default.CalendarMonth,
        YadetDestination.NOTES to Icons.Default.Note,
        YadetDestination.PROJECTS to Icons.Default.Folder,
        YadetDestination.CATEGORIES to Icons.Default.Category,
        YadetDestination.STATISTICS to Icons.Default.BarChart,
        YadetDestination.ARCHIVE to Icons.Default.Archive,
        YadetDestination.TRASH to Icons.Default.Delete,
        YadetDestination.AI_CHAT to Icons.Default.AutoAwesome,
        YadetDestination.MEMORY to Icons.Default.Psychology,
        YadetDestination.SETTINGS to Icons.Default.Settings,
        YadetDestination.BACKUP to Icons.Default.Backup,
        YadetDestination.ABOUT to Icons.Default.Info
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "یادت (Yadet)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                    drawerItems.forEach { (dest, icon) ->
                        NavigationDrawerItem(
                            label = { Text(dest.persianTitle) },
                            icon = { Icon(icon, contentDescription = dest.persianTitle) },
                            selected = currentDest == dest,
                            onClick = {
                                viewModel.navigateTo(dest)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (currentDest == YadetDestination.DASHBOARD) "Yadet" else currentDest.persianTitle,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "منوی بیشتر")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDest == YadetDestination.DASHBOARD,
                        onClick = { viewModel.navigateTo(YadetDestination.DASHBOARD) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "خانه") },
                        label = { Text("خانه") }
                    )
                    NavigationBarItem(
                        selected = currentDest == YadetDestination.CALENDAR,
                        onClick = { viewModel.navigateTo(YadetDestination.CALENDAR) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "تقویم") },
                        label = { Text("تقویم") }
                    )
                    NavigationBarItem(
                        selected = currentDest == YadetDestination.AI_CHAT,
                        onClick = { viewModel.navigateTo(YadetDestination.AI_CHAT) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "چت") },
                        label = { Text("چت") }
                    )
                    NavigationBarItem(
                        selected = currentDest == YadetDestination.ALL_TASKS,
                        onClick = { viewModel.navigateTo(YadetDestination.ALL_TASKS) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "کارها") },
                        label = { Text("کارها") }
                    )
                }
            },
            floatingActionButton = {
                if (currentDest != YadetDestination.AI_CHAT) {
                    FloatingActionButton(
                        onClick = { showCreateReminderDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("fab_add_reminder")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "ایجاد یادآور")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDest) {
                    YadetDestination.DASHBOARD -> DashboardScreen(
                        reminders = reminders,
                        settings = settings,
                        statsRange = statsRange,
                        onStatsRangeChange = viewModel::setStatsRange,
                        onCreateReminderClick = { showCreateReminderDialog = true },
                        onEditReminderClick = { editingReminder = it },
                        onToggleComplete = { r ->
                            if (r.status == "COMPLETED") viewModel.uncompleteReminder(r)
                            else viewModel.completeReminder(r)
                        },
                        onOpenAiChat = { viewModel.navigateTo(YadetDestination.AI_CHAT) }
                    )
                    YadetDestination.ALL_TASKS -> AllTasksScreen(
                        reminders = reminders,
                        settings = settings,
                        onEditReminder = { editingReminder = it },
                        onToggleComplete = { r ->
                            if (r.status == "COMPLETED") viewModel.uncompleteReminder(r)
                            else viewModel.completeReminder(r)
                        }
                    )
                    YadetDestination.CALENDAR -> CalendarScreen(
                        reminders = reminders,
                        settings = settings,
                        onEditReminder = { editingReminder = it },
                        onToggleComplete = { r ->
                            if (r.status == "COMPLETED") viewModel.uncompleteReminder(r)
                            else viewModel.completeReminder(r)
                        }
                    )
                    YadetDestination.NOTES -> NotesScreen(
                        notes = notes,
                        onSaveNote = viewModel::saveNote,
                        onTrashNote = viewModel::trashNote
                    )
                    YadetDestination.PROJECTS -> ProjectsScreen(
                        projects = projects,
                        reminders = reminders,
                        onSaveProject = viewModel::saveProject,
                        onTrashProject = viewModel::trashProject
                    )
                    YadetDestination.CATEGORIES -> CategoriesScreen(
                        categories = categories,
                        onSaveCategory = viewModel::saveCategory,
                        onDeleteCategory = viewModel::deleteCategory
                    )
                    YadetDestination.STATISTICS -> StatisticsScreen(reminders = reminders)
                    YadetDestination.ARCHIVE -> ArchiveScreen(
                        reminders = reminders,
                        settings = settings,
                        onEditReminder = { editingReminder = it },
                        onUncomplete = viewModel::uncompleteReminder
                    )
                    YadetDestination.TRASH -> TrashScreen(
                        trashedReminders = trashedReminders,
                        onRestore = viewModel::restoreReminder,
                        onDeletePermanently = viewModel::deleteReminderPermanently,
                        onEmptyTrash = viewModel::emptyTrash
                    )
                    YadetDestination.AI_CHAT -> AiChatScreen(
                        messages = chatMessages,
                        settings = settings,
                        isThinking = isAiThinking,
                        onSendMessage = viewModel::sendChatMessage,
                        onClearChat = viewModel::clearChat,
                        onToggleTts = viewModel::updateVoiceTts,
                        onOpenModelSettings = { viewModel.navigateTo(YadetDestination.SETTINGS) }
                    )
                    YadetDestination.MEMORY -> MemoryScreen(
                        memories = memoryItems,
                        onSaveMemory = viewModel::saveMemoryItem,
                        onDeleteMemory = viewModel::deleteMemoryItem
                    )
                    YadetDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        multiModelConfig = multiModelConfig,
                        onUpdateTheme = viewModel::updateTheme,
                        onUpdateCalendar = viewModel::updateCalendar,
                        onUpdateIs24Hour = viewModel::updateIs24Hour,
                        onUpdateNotifications = viewModel::updateNotificationSettings,
                        onUpdateVoiceTts = viewModel::updateVoiceTts,
                        onUpdateAiConfig = viewModel::updateAiConfig,
                        onSaveMultiModelConfig = viewModel::saveMultiModelConfig,
                        onSetAllowPaidModels = viewModel::setAllowPaidModels,
                        onSaveApiKey = viewModel::saveApiKey,
                        onTestAiConnection = viewModel::testAiConnection,
                        onSetAppLock = viewModel::setAppLock
                    )
                    YadetDestination.BACKUP -> BackupScreen(
                        onExportBackup = viewModel::exportBackup,
                        onValidateBackup = viewModel::validateBackup,
                        onRestoreBackup = viewModel::restoreBackup
                    )
                    YadetDestination.ABOUT -> AboutScreen()
                }
            }
        }
    }

    if (showCreateReminderDialog || editingReminder != null) {
        ReminderEditorDialog(
            existing = editingReminder,
            categories = categories,
            projects = projects,
            isJalali = settings.calendarSystem == CalendarSystem.JALALI,
            is24Hour = settings.is24Hour,
            onDismiss = {
                showCreateReminderDialog = false
                editingReminder = null
            },
            onSave = { entity ->
                viewModel.saveReminder(entity)
                showCreateReminderDialog = false
                editingReminder = null
            },
            onDelete = { entity ->
                viewModel.trashReminder(entity)
                showCreateReminderDialog = false
                editingReminder = null
            }
        )
    }
}

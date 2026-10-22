package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.FirestoreRepository
import com.example.data.firebase.FsExpense
import com.example.data.firebase.FsTask
import com.example.data.local.PreloadedData
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudgetSummary
import com.example.data.model.ScannedReceiptData
import com.example.data.model.TaskWithExpenseSummary
import com.example.data.model.WorkOrderTask
import com.example.ui.FirebaseSolarViewModel
import com.example.ui.admin.AdminScreen
import com.example.ui.auth.AuthManager
import com.example.ui.auth.SignInScreen
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.UploadWorkOrderDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.expenses.ExpenseListScreen
import com.example.ui.navigation.SolarScreen
import com.example.ui.reports.MonthlyReportsScreen
import com.example.ui.tasks.TaskListScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager = AuthManager(this)

        setContent {
            MyApplicationTheme {
                AppRoot(authManager = authManager)
            }
        }
    }
}

@Composable
fun AppRoot(authManager: AuthManager) {
    val currentUser by authManager.authStateFlow().collectAsStateWithLifecycle(initialValue = authManager.currentUser)
    val user = currentUser

    if (user == null) {
        // Authenticated Session Gate: Show Sign-In screen if not authenticated
        SignInScreen(
            authManager = authManager,
            onSignInSuccess = {}
        )
    } else {
        // Authenticated Session: Inject custom databaseId and Firestore instance
        val context = LocalContext.current
        val dbId = context.getString(R.string.firestore_database_id)
        val firestore = remember(dbId) {
            FirebaseFirestore.getInstance(dbId)
        }
        val repository = remember(firestore) {
            FirestoreRepository(firestore)
        }

        val viewModel: FirebaseSolarViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    FirebaseSolarViewModel(
                        repository = repository,
                        currentUserId = user.uid,
                        currentUserEmail = user.email ?: "admin@solarexpense.com"
                    )
                }
            }
        )

        SolarMainContent(
            viewModel = viewModel,
            userEmail = user.email ?: "admin@solarexpense.com",
            onSignOut = { authManager.signOut() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarMainContent(
    viewModel: FirebaseSolarViewModel,
    userEmail: String,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(SolarScreen.DASHBOARD) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showUploadWoDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var scannedDataForExpense by remember { mutableStateOf<ScannedReceiptData?>(null) }

    // Intercept back button to return to Dashboard if on another tab
    BackHandler(enabled = currentScreen != SolarScreen.DASHBOARD) {
        currentScreen = SolarScreen.DASHBOARD
    }

    // State Collection from ViewModel
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val workOrders by viewModel.workOrders.collectAsStateWithLifecycle()
    val selectedWorkOrderId by viewModel.selectedWorkOrderId.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedProgressFilter by viewModel.selectedProgressFilter.collectAsStateWithLifecycle()
    val portfolioTotalBudget by viewModel.portfolioTotalBudget.collectAsStateWithLifecycle()
    val portfolioTotalSpent by viewModel.portfolioTotalSpent.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val expenseTypes by viewModel.expenseTypes.collectAsStateWithLifecycle()

    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scannedReceipt by viewModel.scannedReceipt.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val activeWorkOrder = workOrders.find { it.id == selectedWorkOrderId }
    val displayProjectName = activeWorkOrder?.projectName ?: "All Active Projects"

    // Convert FsTask and FsExpense to UI domain models
    val taskModelList: List<WorkOrderTask> = remember(tasks) {
        tasks.map {
            val budget = if (it.totalBudget > 0.0) it.totalBudget else (it.totalQty * it.unitPrice)
            WorkOrderTask(
                id = it.id,
                itemNumber = it.itemNumber,
                category = it.category,
                title = it.title,
                description = it.description,
                uom = it.uom,
                totalQty = it.totalQty,
                unitPrice = it.unitPrice,
                totalBudget = budget,
                completedQty = it.completedQty,
                status = it.status
            )
        }
    }

    // Complete exhaustive contract task list across all 44 contract scopes for expense logging
    val allContractTaskList: List<WorkOrderTask> = remember(allTasks, tasks) {
        val baseList: List<WorkOrderTask> = when {
            allTasks.isNotEmpty() -> allTasks.map {
                val budget = if (it.totalBudget > 0.0) it.totalBudget else (it.totalQty * it.unitPrice)
                WorkOrderTask(
                    id = it.id,
                    itemNumber = it.itemNumber,
                    category = it.category,
                    title = it.title,
                    description = it.description,
                    uom = it.uom,
                    totalQty = it.totalQty,
                    unitPrice = it.unitPrice,
                    totalBudget = budget,
                    completedQty = it.completedQty,
                    status = it.status
                )
            }
            tasks.isNotEmpty() -> taskModelList
            else -> emptyList()
        }
        val existingItemNumbers = baseList.map { it.itemNumber }.toSet()
        val existingIds = baseList.map { it.id }.toSet()
        val merged = baseList.toMutableList()
        for (preload in PreloadedData.INITIAL_TASKS) {
            if (preload.itemNumber !in existingItemNumbers && preload.id !in existingIds) {
                merged.add(preload)
            }
        }
        merged.sortedBy { it.itemNumber.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
    }

    // Tasks strictly filtered to the selected project for logging project-specific expenses
    val selectedProjectTaskList: List<WorkOrderTask> = remember(taskModelList, allContractTaskList) {
        if (taskModelList.isNotEmpty()) {
            taskModelList
        } else {
            allContractTaskList
        }
    }

    val expenseModelList: List<Expense> = remember(expenses) {
        expenses.map {
            Expense(
                id = it.id.hashCode().toLong(),
                taskId = it.taskId,
                taskTitle = it.taskTitle,
                category = it.category,
                amount = it.effectiveBillAmount,
                billAmount = it.effectiveBillAmount,
                paidAmount = it.effectivePaidAmount,
                balanceAmount = it.effectiveBalanceAmount,
                paymentStatus = it.paymentStatus,
                date = it.date,
                monthYear = it.monthYear,
                vendorName = it.vendorName,
                billNumber = it.billNumber,
                expenseType = it.expenseType,
                paymentMode = it.paymentMode,
                notes = it.notes,
                receiptImageUri = it.receiptImageUri,
                paymentPhotoUri = it.paymentPhotoUri,
                expensePhotoUri = it.expensePhotoUri
            )
        }
    }

    val taskWithSummaries: List<TaskWithExpenseSummary> = remember(taskModelList, expenseModelList) {
        val expMap = expenseModelList.groupBy { it.taskId }
        taskModelList.map { task ->
            val tExp = expMap[task.id] ?: emptyList()
            val spent = tExp.sumOf { it.amount }
            TaskWithExpenseSummary(
                task = task,
                totalSpent = spent,
                expenseCount = tExp.size
            )
        }
    }

    // Monthly Report Summary Computation
    val availableMonths = remember(expenseModelList) {
        val recorded = expenseModelList.map { it.monthYear }
        val defaults = listOf("2026-10", "2026-09", "2026-08")
        (recorded + defaults).distinct().sortedDescending()
    }

    val currentMonthlyReport = remember(selectedMonth, expenseModelList, portfolioTotalBudget) {
        val mExpenses = expenseModelList.filter { it.monthYear == selectedMonth }
        val mSpent = mExpenses.sumOf { it.amount }
        val breakdown = mExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val monthlyAllocation = if (portfolioTotalBudget > 0.0) portfolioTotalBudget / 3.0 else 0.0

        val display = try {
            val parts = selectedMonth.split("-")
            val year = parts[0].toInt()
            val m = parts[1].toInt()
            val date = LocalDate.of(year, m, 1)
            date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        } catch (_: Exception) {
            selectedMonth
        }

        MonthlyBudgetSummary(
            monthYear = selectedMonth,
            displayMonth = display,
            totalSpent = mSpent,
            monthlyAllocatedBudget = monthlyAllocation,
            expenseCount = mExpenses.size,
            categoryBreakdown = breakdown
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentScreen) {
                                SolarScreen.DASHBOARD -> "APRICO CLOUD Overview"
                                SolarScreen.TASKS -> "Work Order Items (${tasks.size})"
                                SolarScreen.ADMIN -> "Admin Console • Project Tasks"
                                SolarScreen.EXPENSES -> "Site Expenses"
                                SolarScreen.REPORTS -> "Monthly Budget Reports"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayProjectName.take(30),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentScreen == SolarScreen.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { currentScreen = SolarScreen.ADMIN }
                            .testTag("top_admin_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (currentScreen == SolarScreen.ADMIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentScreen == SolarScreen.ADMIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSignOutConfirmation = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val bottomBarScreens = remember {
                listOf(
                    SolarScreen.DASHBOARD,
                    SolarScreen.TASKS,
                    SolarScreen.EXPENSES,
                    SolarScreen.REPORTS
                )
            }
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                bottomBarScreens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        },
                        alwaysShowLabel = true,
                        modifier = Modifier.testTag(screen.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                SolarScreen.DASHBOARD -> {
                    DashboardScreen(
                        projects = projects,
                        workOrders = workOrders,
                        selectedWorkOrderId = selectedWorkOrderId,
                        tasks = tasks,
                        allTasks = allTasks,
                        expenses = expenses,
                        portfolioTotalBudget = portfolioTotalBudget,
                        portfolioTotalSpent = portfolioTotalSpent,
                        onSelectWorkOrder = { viewModel.selectWorkOrder(it) },
                        onOpenUploadWorkOrder = { showUploadWoDialog = true },
                        onNavigate = { currentScreen = it },
                        onOpenAddExpense = {
                            scannedDataForExpense = null
                            showAddExpenseDialog = true
                        },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                }
                SolarScreen.TASKS -> {
                    TaskListScreen(
                        tasksWithSummary = taskWithSummaries,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.setCategoryFilter(it) },
                        selectedProgressFilter = selectedProgressFilter,
                        onProgressFilterSelected = { viewModel.setProgressFilter(it) },
                        workOrders = workOrders,
                        selectedWorkOrderId = selectedWorkOrderId,
                        onSelectWorkOrder = { viewModel.selectWorkOrder(it) },
                        onUpdateTaskProgress = { taskId, qty, status ->
                            viewModel.updateTaskProgress(taskId, qty, status)
                        },
                        onLogExpenseForTask = { task ->
                            scannedDataForExpense = ScannedReceiptData(
                                vendor = "",
                                date = LocalDate.now().toString(),
                                billNumber = "",
                                totalAmount = 0.0,
                                taxAmount = 0.0,
                                suggestedCategory = task.category,
                                suggestedTaskId = task.id,
                                lineItemsSummary = "Expense for [${task.itemNumber}] ${task.title}"
                            )
                            showAddExpenseDialog = true
                        }
                    )
                }
                SolarScreen.ADMIN -> {
                    AdminScreen(
                        projects = projects,
                        workOrders = workOrders,
                        allTasks = allTasks,
                        selectedWorkOrderId = selectedWorkOrderId,
                        onSelectWorkOrder = { viewModel.selectWorkOrder(it) },
                        onAddTask = { viewModel.addTask(it) },
                        onUpdateTask = { viewModel.updateTask(it) },
                        onDeleteTask = { taskId, woId -> viewModel.deleteTask(taskId, woId) },
                        onDeleteProject = { woId, projId -> viewModel.deleteProject(woId, projId) },
                        onUpdateProjectName = { woId, projId, newName -> viewModel.updateProjectName(woId, projId, newName) },
                        onOpenUploadWorkOrder = { showUploadWoDialog = true },
                        vendors = vendors,
                        expenseTypes = expenseTypes,
                        onAddVendor = { name, phone, contact, cat, addr, notes ->
                            viewModel.addVendor(name, phone, contact, cat, addr, notes)
                        },
                        onUpdateVendor = { viewModel.updateVendor(it) },
                        onDeleteVendor = { viewModel.deleteVendor(it) },
                        onAddExpenseType = { name, desc, cat ->
                            viewModel.addExpenseType(name, desc, cat)
                        },
                        onUpdateExpenseType = { viewModel.updateExpenseType(it) },
                        onDeleteExpenseType = { viewModel.deleteExpenseType(it) }
                    )
                }
                SolarScreen.EXPENSES -> {
                    ExpenseListScreen(
                        expenses = expenseModelList,
                        totalSpent = portfolioTotalSpent,
                        workOrders = workOrders,
                        selectedWorkOrderId = selectedWorkOrderId,
                        onSelectWorkOrder = { viewModel.selectWorkOrder(it) },
                        onOpenAddExpense = {
                            scannedDataForExpense = null
                            showAddExpenseDialog = true
                        },
                        onDeleteExpense = { hashId ->
                            val match = expenses.find { it.id.hashCode().toLong() == hashId }
                            if (match != null) {
                                viewModel.deleteExpense(match.id)
                            }
                        }
                    )
                }
                SolarScreen.REPORTS -> {
                    MonthlyReportsScreen(
                        currentReport = currentMonthlyReport,
                        availableMonths = availableMonths,
                        selectedMonth = selectedMonth,
                        allExpenses = expenseModelList,
                        tasksWithSummary = taskWithSummaries,
                        onSelectMonth = { viewModel.setSelectedMonth(it) }
                    )
                }
            }
        }

        // Upload / Add Work Order Dialog
        if (showUploadWoDialog) {
            UploadWorkOrderDialog(
                onDismiss = { showUploadWoDialog = false },
                onUploadWorkOrder = { name, loc, cap, woNum, client, contr, budget, days, start, genTasks ->
                    viewModel.createWorkOrder(
                        projectName = name,
                        projectLocation = loc,
                        capacity = cap,
                        workOrderNumber = woNum,
                        clientName = client,
                        contractorName = contr,
                        baseBudget = budget,
                        durationDays = days,
                        startDate = start,
                        tasks = genTasks
                    )
                    showUploadWoDialog = false
                }
            )
        }

        // Add Expense Dialog
        if (showAddExpenseDialog) {
            AddExpenseDialog(
                tasks = selectedProjectTaskList,
                initialScannedData = scannedDataForExpense,
                projectName = displayProjectName,
                availableVendors = vendors,
                availableExpenseTypes = expenseTypes,
                onDismiss = {
                    showAddExpenseDialog = false
                    scannedDataForExpense = null
                },
                onSave = { taskId, title, cat, amt, date, vendor, billNo, type, mode, notes, billPhotoUri, paymentPhotoUri, expensePhotoUri, billAmount, paidAmount, balanceAmount, paymentStatus ->
                    val targetTask = allTasks.find { it.id == taskId } ?: tasks.find { it.id == taskId }
                    val targetWoId = targetTask?.workOrderId ?: activeWorkOrder?.id ?: (workOrders.firstOrNull()?.id ?: "")
                    val targetProjId = targetTask?.projectId ?: activeWorkOrder?.projectId ?: (workOrders.firstOrNull()?.projectId ?: "")

                    viewModel.addExpense(
                        workOrderId = targetWoId,
                        projectId = targetProjId,
                        taskId = taskId ?: "",
                        taskTitle = title,
                        category = cat,
                        amount = amt,
                        date = date,
                        vendorName = vendor,
                        billNumber = billNo,
                        expenseType = type,
                        paymentMode = mode,
                        notes = notes,
                        receiptImageUri = billPhotoUri,
                        paymentPhotoUri = paymentPhotoUri,
                        expensePhotoUri = expensePhotoUri,
                        billAmount = billAmount,
                        paidAmount = paidAmount,
                        balanceAmount = balanceAmount,
                        paymentStatus = paymentStatus
                    )
                    showAddExpenseDialog = false
                    scannedDataForExpense = null
                    viewModel.clearScannedReceipt()
                }
            )
        }

        // Sign Out Confirmation Dialog
        if (showSignOutConfirmation) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirmation = false },
                title = { Text("Account Details") },
                text = {
                    Column {
                        Text("Signed in as: $userEmail")
                        Spacer(modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = "Admin privileges active. All projects and work orders are synchronized with Firebase Firestore in real-time.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutConfirmation = false
                            onSignOut()
                        }
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirmation = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

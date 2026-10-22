package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FsExpenseType
import com.example.data.firebase.FsProject
import com.example.data.firebase.FsTask
import com.example.data.firebase.FsVendor
import com.example.data.firebase.FsWorkOrder
import com.example.ui.components.FormatUtils
import kotlinx.coroutines.launch
import java.util.Locale

enum class AdminSectionTab(val title: String) {
    PROJECTS("Projects"),
    VENDORS("Parties"),
    EXPENSE_TYPES("Expense Types")
}

private val SOLAR_CATEGORIES = listOf(
    "Civil & Foundation",
    "MMS & Modules",
    "DC Cabling",
    "Earthing & LA",
    "Inverters & Panels",
    "AC Cabling & Trench",
    "HT Cabling",
    "Aux & Control Cables",
    "WMS & Ancillary",
    "General Site Scope"
)

private val COMMON_UOMS = listOf("Nos", "MT", "Mtr", "Sets", "LOT", "CUM", "KG", "SQM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    projects: List<FsProject>,
    workOrders: List<FsWorkOrder>,
    allTasks: List<FsTask>,
    selectedWorkOrderId: String?,
    onSelectWorkOrder: (String?) -> Unit,
    onAddTask: (FsTask) -> Unit,
    onUpdateTask: (FsTask) -> Unit,
    onDeleteTask: (taskId: String, workOrderId: String) -> Unit,
    onDeleteProject: (workOrderId: String, projectId: String) -> Unit = { _, _ -> },
    onUpdateProjectName: (workOrderId: String, projectId: String, newName: String) -> Unit = { _, _, _ -> },
    onOpenUploadWorkOrder: () -> Unit,
    vendors: List<FsVendor> = emptyList(),
    expenseTypes: List<FsExpenseType> = emptyList(),
    onAddVendor: (name: String, phone: String, contact: String, category: String, address: String, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateVendor: (FsVendor) -> Unit = {},
    onDeleteVendor: (String) -> Unit = {},
    onAddExpenseType: (name: String, description: String, category: String) -> Unit = { _, _, _ -> },
    onUpdateExpenseType: (FsExpenseType) -> Unit = {},
    onDeleteExpenseType: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeAdminTab by remember { mutableStateOf(AdminSectionTab.PROJECTS) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    // Dialog states
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<FsTask?>(null) }
    var taskToDelete by remember { mutableStateOf<FsTask?>(null) }
    var projectToDelete by remember { mutableStateOf<FsWorkOrder?>(null) }
    var projectToEditName by remember { mutableStateOf<FsWorkOrder?>(null) }

    // Active selected work order / project
    val selectedWorkOrder = remember(selectedWorkOrderId, workOrders) {
        workOrders.find { it.id == selectedWorkOrderId } ?: workOrders.firstOrNull()
    }

    // Filter tasks for the selected project
    val tasksForSelectedProject = remember(selectedWorkOrder, allTasks) {
        if (selectedWorkOrder != null) {
            allTasks.filter { it.workOrderId == selectedWorkOrder.id || (it.projectId.isNotBlank() && it.projectId == selectedWorkOrder.projectId) }
        } else {
            emptyList()
        }
    }

    val filteredTasks = remember(tasksForSelectedProject, searchQuery, selectedCategoryFilter, selectedStatusFilter) {
        tasksForSelectedProject.filter { task ->
            val matchesCategory = selectedCategoryFilter == "All" || task.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == "All" || task.status.equals(selectedStatusFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.itemNumber.contains(searchQuery, ignoreCase = true) ||
                task.category.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesStatus && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_screen_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Page Header
            item {
                AdminHeaderCard(
                    totalProjects = workOrders.size,
                    totalAllTasks = allTasks.size,
                    totalVendors = vendors.size,
                    totalExpenseTypes = expenseTypes.size,
                    activeTab = activeAdminTab,
                    onSelectTab = { activeAdminTab = it },
                    onOpenUploadWorkOrder = onOpenUploadWorkOrder
                )
            }

            // Section Tabs Selector
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AdminSectionTab.values().forEach { tab ->
                            val isSelected = activeAdminTab == tab
                            val count = when (tab) {
                                AdminSectionTab.PROJECTS -> workOrders.size
                                AdminSectionTab.VENDORS -> vendors.size
                                AdminSectionTab.EXPENSE_TYPES -> expenseTypes.size
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeAdminTab = tab }
                                    .testTag("admin_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (tab) {
                                            AdminSectionTab.PROJECTS -> Icons.Default.Engineering
                                            AdminSectionTab.VENDORS -> Icons.Default.Storefront
                                            AdminSectionTab.EXPENSE_TYPES -> Icons.Default.Category
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${tab.title} ($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (activeAdminTab) {
                AdminSectionTab.PROJECTS -> {
                    // Projects List Section (Showing all project names)
                    item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SolarPower,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Solar Projects (${workOrders.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (workOrders.isEmpty()) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No projects found in database.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onOpenUploadWorkOrder) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload First Work Order")
                                }
                            }
                        }
                    } else {
                        // Project selector horizontal carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(workOrders) { wo ->
                                val isSelected = wo.id == selectedWorkOrder?.id
                                val woTasks = allTasks.filter { it.workOrderId == wo.id || (wo.projectId.isNotBlank() && it.projectId == wo.projectId) }
                                val taskCountForWo = woTasks.size
                                val allocatedBudgetForWo = woTasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                                ProjectAdminSelectorCard(
                                    workOrder = wo,
                                    taskCount = if (taskCountForWo > 0) taskCountForWo else wo.taskCount,
                                    isSelected = isSelected,
                                    allocatedBudget = allocatedBudgetForWo,
                                    onSelect = {
                                        onSelectWorkOrder(wo.id)
                                    },
                                    onEditName = {
                                        projectToEditName = wo
                                    },
                                    onDelete = {
                                        projectToDelete = wo
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Content Area: Project Tasks & Management (Active Project)
            if (selectedWorkOrder != null) {
                // Project Selected: Admin task management controls and tasks list
                val allocatedForSelected = tasksForSelectedProject.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                item {
                    SelectedProjectHeaderCard(
                        workOrder = selectedWorkOrder,
                        taskCount = tasksForSelectedProject.size,
                        completedCount = tasksForSelectedProject.count { it.status == "COMPLETED" },
                        allocatedBudget = allocatedForSelected,
                        onEditProjectName = { projectToEditName = selectedWorkOrder },
                        onAddNewTask = { showAddTaskDialog = true },
                        onDeleteProject = { projectToDelete = selectedWorkOrder }
                    )
                }

                // Filter & Search Controls for Tasks
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search tasks by title, item number, or category...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_task_search_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FilterChip(
                                selected = selectedCategoryFilter == "All",
                                onClick = { selectedCategoryFilter = "All" },
                                label = { Text("All (${tasksForSelectedProject.size})") }
                            )

                            val projectCategories = tasksForSelectedProject.map { it.category }.distinct()
                            projectCategories.forEach { category ->
                                val count = tasksForSelectedProject.count { it.category == category }
                                FilterChip(
                                    selected = selectedCategoryFilter == category,
                                    onClick = { selectedCategoryFilter = category },
                                    label = { Text("$category ($count)") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FilterChip(
                                selected = selectedStatusFilter == "All",
                                onClick = { selectedStatusFilter = "All" },
                                label = { Text("All Status") }
                            )

                            listOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "HOLD", "NA").forEach { status ->
                                val count = tasksForSelectedProject.count { it.status == status }
                                val display = when (status) {
                                    "NOT_STARTED" -> "Not Started"
                                    "IN_PROGRESS" -> "In Progress"
                                    "COMPLETED" -> "Completed"
                                    "HOLD" -> "Hold"
                                    "NA" -> "N/A"
                                    else -> status
                                }
                                FilterChip(
                                    selected = selectedStatusFilter == status,
                                    onClick = { selectedStatusFilter = status },
                                    label = { Text("$display ($count)") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${filteredTasks.size} of ${tasksForSelectedProject.size} tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showAddTaskDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("admin_add_task_inline_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Task", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Tasks List for Selected Project
                if (filteredTasks.isEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (tasksForSelectedProject.isEmpty()) "No tasks in this project work order yet" else "No tasks matching filter",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Click '+ Add Task' to add contract items to this project scope.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddTaskDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add New Task Now")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        AdminTaskCard(
                            task = task,
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }
        AdminSectionTab.VENDORS -> {
            item {
                AdminVendorMasterSection(
                    vendors = vendors,
                    onAddVendor = onAddVendor,
                    onUpdateVendor = onUpdateVendor,
                    onDeleteVendor = onDeleteVendor,
                    onShowMessage = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
        }
        AdminSectionTab.EXPENSE_TYPES -> {
            item {
                AdminExpenseTypeMasterSection(
                    expenseTypes = expenseTypes,
                    onAddExpenseType = onAddExpenseType,
                    onUpdateExpenseType = onUpdateExpenseType,
                    onDeleteExpenseType = onDeleteExpenseType,
                    onShowMessage = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
        }
    }
}

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Add Task Dialog
    if (showAddTaskDialog && selectedWorkOrder != null) {
        AddTaskDialog(
            projectName = selectedWorkOrder.projectName,
            workOrderId = selectedWorkOrder.id,
            projectId = selectedWorkOrder.projectId,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { newTask ->
                onAddTask(newTask)
                showAddTaskDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Task '${newTask.itemNumber}' added successfully to Firestore!")
                }
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedTask ->
                onUpdateTask(updatedTask)
                taskToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Task '${updatedTask.itemNumber}' updated in Firestore!")
                }
            }
        )
    }

    // Delete Task Confirmation Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Task [${task.itemNumber}]?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete '${task.title}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This will permanently remove this item from the work order in Firebase Firestore and recalculate project scope.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(task.id, task.workOrderId)
                        val deletedTitle = task.title
                        taskToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted '$deletedTitle' from project scope.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("admin_confirm_delete_button")
                ) {
                    Text("Delete Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Project Confirmation Dialog
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Project",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Project?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to permanently delete this project?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = project.projectName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Work Order: ${project.workOrderNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Warning: This will permanently delete this project, its tasks scope, and all related expense records from Firebase Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedName = project.projectName
                        val woId = project.id
                        val projId = project.projectId
                        projectToDelete = null
                        if (selectedWorkOrderId == woId) {
                            onSelectWorkOrder(null)
                        }
                        onDeleteProject(woId, projId)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Project '$deletedName' deleted from Firestore.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("admin_confirm_delete_project_button")
                ) {
                    Text("Delete Project", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { projectToDelete = null },
                    modifier = Modifier.testTag("admin_cancel_delete_project_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Project Name Dialog
    projectToEditName?.let { project ->
        EditProjectNameDialog(
            workOrder = project,
            onDismiss = { projectToEditName = null },
            onConfirm = { newName ->
                onUpdateProjectName(project.id, project.projectId, newName)
                projectToEditName = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Project name updated to '$newName'")
                }
            }
        )
    }
}

@Composable
fun AdminHeaderCard(
    totalProjects: Int,
    totalAllTasks: Int,
    totalVendors: Int = 0,
    totalExpenseTypes: Int = 0,
    activeTab: AdminSectionTab = AdminSectionTab.PROJECTS,
    onSelectTab: (AdminSectionTab) -> Unit = {},
    onOpenUploadWorkOrder: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_header_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "APRICO CLOUD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Admin Control Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Manage solar project work orders, customize execution task schedules, and maintain master directories for parties and expense types.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Projects & Tasks Stat Box
                    val isProjectsActive = activeTab == AdminSectionTab.PROJECTS
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isProjectsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { onSelectTab(AdminSectionTab.PROJECTS) }
                            .testTag("admin_stat_projects")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalProjects",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isProjectsActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Projects",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isProjectsActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Parties Stat Box
                    val isVendorsActive = activeTab == AdminSectionTab.VENDORS
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVendorsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { onSelectTab(AdminSectionTab.VENDORS) }
                            .testTag("admin_stat_parties")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalVendors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isVendorsActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Parties",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isVendorsActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expense Types Stat Box
                    val isTypesActive = activeTab == AdminSectionTab.EXPENSE_TYPES
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTypesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { onSelectTab(AdminSectionTab.EXPENSE_TYPES) }
                            .testTag("admin_stat_expense_types")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalExpenseTypes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTypesActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Exp Types",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTypesActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenUploadWorkOrder,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("admin_upload_wo_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload WO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProjectAdminSelectorCard(
    workOrder: FsWorkOrder,
    taskCount: Int,
    isSelected: Boolean,
    allocatedBudget: Double = 0.0,
    onSelect: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .width(280.dp)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .testTag("admin_project_card_${workOrder.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isSelected) "Active Selected" else "Project",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workOrder.projectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onEditName,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("admin_edit_project_name_button_${workOrder.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit ${workOrder.projectName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "WO: ${workOrder.workOrderNumber.take(24)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$taskCount Tasks",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = FormatUtils.formatLakhs(if (allocatedBudget > 0.0) allocatedBudget else workOrder.totalBudget),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_select_project_button_${workOrder.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = if (isSelected) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text(
                        text = if (isSelected) "Selected ✓" else "Manage Tasks",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("admin_delete_project_button_${workOrder.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${workOrder.projectName}",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedProjectHeaderCard(
    workOrder: FsWorkOrder,
    taskCount: Int,
    completedCount: Int,
    allocatedBudget: Double = 0.0,
    onEditProjectName: () -> Unit,
    onAddNewTask: () -> Unit,
    onDeleteProject: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_selected_project_banner")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "MANAGING PROJECT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    OutlinedButton(
                        onClick = onDeleteProject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_delete_active_project_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Project", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workOrder.projectName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onEditProjectName,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("admin_edit_active_project_name_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "WO: ${workOrder.workOrderNumber} • Client: ${workOrder.clientName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metric pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(label = "Total Tasks", value = "$taskCount", modifier = Modifier.weight(1f))
                MetricChip(label = "Completed", value = "$completedCount", modifier = Modifier.weight(1f))
                val displayContractBudget = if (allocatedBudget > 0.0) allocatedBudget else workOrder.totalBudget
                MetricChip(label = "Allocated Budget", value = FormatUtils.formatLakhs(displayContractBudget), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddNewTask,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_add_task_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Task to Scope", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun AdminTaskCard(
    task: FsTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = FormatUtils.getCategoryColor(task.category)
    val statusColor = when (task.status) {
        "COMPLETED" -> Color(0xFF10B981) // Emerald
        "IN_PROGRESS" -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFF94A3B8) // Slate Gray
    }
    val statusLabel = when (task.status) {
        "COMPLETED" -> "Completed"
        "IN_PROGRESS" -> "In Progress"
        else -> "Not Started"
    }

    val progressPct = if (task.totalQty > 0) ((task.completedQty / task.totalQty) * 100).coerceIn(0.0, 100.0) else 0.0

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_task_item_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Item number, category chip, status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Item ${task.itemNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Description
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scope & Pricing Grid
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Contract Scope",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", task.totalQty)} ${task.uom}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Unit Price",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(task.unitPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(task.totalBudget),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Site Execution: ${String.format(Locale.US, "%.1f", task.completedQty)} / ${String.format(Locale.US, "%.1f", task.totalQty)} ${task.uom}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", progressPct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (progressPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("admin_delete_task_${task.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("admin_edit_task_${task.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Task")
                }
            }
        }
    }
}

/**
 * Dialog to add a new task to the selected project.
 */
@Composable
fun AddTaskDialog(
    projectName: String,
    workOrderId: String,
    projectId: String,
    onDismiss: () -> Unit,
    onConfirm: (FsTask) -> Unit
) {
    var itemNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SOLAR_CATEGORIES.first()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalQtyStr by remember { mutableStateOf("") }
    var uom by remember { mutableStateOf("Nos") }
    var unitPriceStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("NOT_STARTED") }

    val qty = totalQtyStr.toDoubleOrNull() ?: 0.0
    val price = unitPriceStr.toDoubleOrNull() ?: 0.0
    val calculatedBudget = qty * price

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Task to Project",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Project: $projectName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemNumber,
                        onValueChange = { itemNumber = it },
                        label = { Text("Item No (e.g. C.12)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_input_item_number")
                    )

                    OutlinedTextField(
                        value = uom,
                        onValueChange = { uom = it },
                        label = { Text("UOM (Nos/Mtr/MT)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_input_uom")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_input_title")
                )

                // Category selection chips
                Column {
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SOLAR_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Scope Description (Optional)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalQtyStr,
                        onValueChange = { totalQtyStr = it },
                        label = { Text("Total Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_input_total_qty")
                    )

                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it },
                        label = { Text("Unit Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_input_unit_price")
                    )
                }

                // Real-time calculated budget preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Budget:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = FormatUtils.formatInr(calculatedBudget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Initial status chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Initial Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    listOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "HOLD", "NA").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = {
                                Text(
                                    when (s) {
                                        "NOT_STARTED" -> "Not Started"
                                        "IN_PROGRESS" -> "In Progress"
                                        "COMPLETED" -> "Completed"
                                        "HOLD" -> "Hold"
                                        "NA" -> "N/A"
                                        else -> s
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a task title."
                        return@Button
                    }
                    if (itemNumber.isBlank()) {
                        errorMessage = "Please enter an item number."
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Please enter a valid quantity greater than 0."
                        return@Button
                    }

                    val newTask = FsTask(
                        id = "", // Will be assigned by Firestore
                        workOrderId = workOrderId,
                        projectId = projectId,
                        itemNumber = itemNumber.trim(),
                        category = category,
                        title = title.trim(),
                        description = description.trim(),
                        uom = uom.trim().ifEmpty { "Nos" },
                        totalQty = qty,
                        unitPrice = price,
                        totalBudget = calculatedBudget,
                        completedQty = if (status == "COMPLETED") qty else 0.0,
                        status = status
                    )
                    onConfirm(newTask)
                },
                modifier = Modifier.testTag("admin_submit_add_task_button")
            ) {
                Text("Add Task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to edit an existing task.
 */
@Composable
fun EditTaskDialog(
    task: FsTask,
    onDismiss: () -> Unit,
    onConfirm: (FsTask) -> Unit
) {
    var itemNumber by remember { mutableStateOf(task.itemNumber) }
    var category by remember { mutableStateOf(task.category) }
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var totalQtyStr by remember { mutableStateOf(task.totalQty.toString()) }
    var uom by remember { mutableStateOf(task.uom) }
    var unitPriceStr by remember { mutableStateOf(task.unitPrice.toString()) }
    var completedQtyStr by remember { mutableStateOf(task.completedQty.toString()) }
    var status by remember { mutableStateOf(task.status) }

    val qty = totalQtyStr.toDoubleOrNull() ?: task.totalQty
    val price = unitPriceStr.toDoubleOrNull() ?: task.unitPrice
    val completedQty = completedQtyStr.toDoubleOrNull() ?: task.completedQty
    val calculatedBudget = qty * price

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Task [${task.itemNumber}]",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemNumber,
                        onValueChange = { itemNumber = it },
                        label = { Text("Item No") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_input_item_number")
                    )

                    OutlinedTextField(
                        value = uom,
                        onValueChange = { uom = it },
                        label = { Text("UOM") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_input_uom")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_input_title")
                )

                // Category chips
                Column {
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SOLAR_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Scope Description") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalQtyStr,
                        onValueChange = { totalQtyStr = it },
                        label = { Text("Total Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_input_total_qty")
                    )

                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it },
                        label = { Text("Unit Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_input_unit_price")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = completedQtyStr,
                        onValueChange = { completedQtyStr = it },
                        label = { Text("Completed Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_input_completed_qty")
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Total Budget", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = FormatUtils.formatInr(calculatedBudget),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Status chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    listOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "HOLD", "NA").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = {
                                Text(
                                    when (s) {
                                        "NOT_STARTED" -> "Not Started"
                                        "IN_PROGRESS" -> "In Progress"
                                        "COMPLETED" -> "Completed"
                                        "HOLD" -> "Hold"
                                        "NA" -> "N/A"
                                        else -> s
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Title cannot be blank."
                        return@Button
                    }
                    if (itemNumber.isBlank()) {
                        errorMessage = "Item number cannot be blank."
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Quantity must be greater than 0."
                        return@Button
                    }

                    val updated = task.copy(
                        itemNumber = itemNumber.trim(),
                        category = category,
                        title = title.trim(),
                        description = description.trim(),
                        uom = uom.trim(),
                        totalQty = qty,
                        unitPrice = price,
                        totalBudget = calculatedBudget,
                        completedQty = completedQty.coerceIn(0.0, qty),
                        status = status
                    )
                    onConfirm(updated)
                },
                modifier = Modifier.testTag("admin_submit_edit_task_button")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to edit the Project Name in the Admin Center.
 */
@Composable
fun EditProjectNameDialog(
    workOrder: FsWorkOrder,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var projectName by remember { mutableStateOf(workOrder.projectName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Project Name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Edit Project Name",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Update the display name for this solar project. This will be reflected across all project selectors, task schedules, and site expense logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Work Order: ${workOrder.workOrderNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (workOrder.clientName.isNotBlank()) {
                            Text(
                                text = "Client: ${workOrder.clientName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        errorMessage = null
                    },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. Maxinfra / KUSUM 1.88 MW Solar") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_project_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = projectName.trim()
                    if (trimmed.isBlank()) {
                        errorMessage = "Project name cannot be empty."
                        return@Button
                    }
                    onConfirm(trimmed)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("admin_confirm_edit_project_name_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Name", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_cancel_edit_project_name_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

package com.example.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FsWorkOrder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.example.data.model.TaskWithExpenseSummary
import com.example.data.model.WorkOrderTask
import com.example.ui.components.FormatUtils
import java.util.Locale

/**
 * Filter categories for Work Order tasks depending on site progress.
 */
enum class TaskProgressFilter(
    val id: String,
    val title: String,
    val shortLabel: String,
    val badgeColor: Color
) {
    ALL("ALL", "All Progress", "All", Color(0xFF3B82F6)),
    NOT_STARTED("NOT_STARTED", "Not Started (0%)", "Not Started", Color(0xFF6B7280)),
    IN_PROGRESS("IN_PROGRESS", "In Progress (1–99%)", "In Progress", Color(0xFFF59E0B)),
    NEAR_COMPLETE("NEAR_COMPLETE", "Near Done (≥75%)", "≥75% Done", Color(0xFF0D9488)),
    COMPLETED("COMPLETED", "Completed (100%)", "Completed", Color(0xFF10B981)),
    HOLD("HOLD", "On Hold", "Hold", Color(0xFFEF4444)),
    NA("NA", "Not Applicable (N/A)", "N/A", Color(0xFF8B5CF6));

    companion object {
        fun fromId(id: String): TaskProgressFilter =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
    }
}

enum class TaskSortOrder(val title: String) {
    DEFAULT("Item Sequence (1..44)"),
    PROGRESS_LOW_HIGH("Progress: Lowest First (Needs Work)"),
    PROGRESS_HIGH_LOW("Progress: Highest First"),
    BUDGET_HIGH_LOW("Budget: High to Low"),
    SPENT_HIGH_LOW("Expenses: High to Low")
}

fun calculateTaskProgressPercentage(task: WorkOrderTask): Double {
    val status = task.status.uppercase(Locale.US)
    if (status == "COMPLETED") return 100.0
    if (status == "NOT_STARTED") return 0.0
    if (status == "NA" || status == "N/A" || status == "NOT_APPLICABLE") return 0.0
    if (task.totalQty <= 0.0) {
        return when (status) {
            "COMPLETED" -> 100.0
            "NOT_STARTED", "HOLD", "ON_HOLD", "NA", "N/A" -> 0.0
            else -> 50.0
        }
    }
    val pct = (task.completedQty / task.totalQty) * 100.0
    return pct.coerceIn(0.0, 100.0)
}

fun isTaskMatchingProgressFilter(task: WorkOrderTask, filter: TaskProgressFilter): Boolean {
    val progress = calculateTaskProgressPercentage(task)
    val status = task.status.uppercase(Locale.US)
    return when (filter) {
        TaskProgressFilter.ALL -> true
        TaskProgressFilter.NOT_STARTED -> {
            status == "NOT_STARTED" || (progress <= 0.001 && status != "IN_PROGRESS" && status != "COMPLETED" && status != "HOLD" && status != "ON_HOLD" && status != "NA" && status != "N/A")
        }
        TaskProgressFilter.IN_PROGRESS -> {
            if (status == "COMPLETED" || status == "HOLD" || status == "ON_HOLD" || status == "NA" || status == "N/A" || progress >= 99.999) false
            else if (status == "IN_PROGRESS") true
            else progress > 0.001 && progress < 99.999
        }
        TaskProgressFilter.NEAR_COMPLETE -> {
            if (status == "COMPLETED" || progress >= 99.999) false
            else progress >= 75.0 && progress < 99.999
        }
        TaskProgressFilter.COMPLETED -> {
            status == "COMPLETED" || progress >= 99.999 || (task.totalQty > 0 && task.completedQty >= task.totalQty)
        }
        TaskProgressFilter.HOLD -> {
            status == "HOLD" || status == "ON_HOLD"
        }
        TaskProgressFilter.NA -> {
            status == "NA" || status == "N/A" || status == "NOT_APPLICABLE"
        }
    }
}

@Composable
fun TaskListScreen(
    tasksWithSummary: List<TaskWithExpenseSummary>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedProgressFilter: String = "ALL",
    onProgressFilterSelected: (String) -> Unit = {},
    workOrders: List<FsWorkOrder> = emptyList(),
    selectedWorkOrderId: String? = null,
    onSelectWorkOrder: (String?) -> Unit = {},
    onUpdateTaskProgress: (taskId: String, completedQty: Double, status: String) -> Unit,
    onLogExpenseForTask: (WorkOrderTask) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var taskToUpdateProgress by remember { mutableStateOf<WorkOrderTask?>(null) }
    var sortOrder by remember { mutableStateOf(TaskSortOrder.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showProjectMenu by remember { mutableStateOf(false) }

    val activeProject = remember(selectedWorkOrderId, workOrders) {
        workOrders.find { it.id == selectedWorkOrderId } ?: workOrders.firstOrNull()
    }

    val activeProgressFilter = TaskProgressFilter.fromId(selectedProgressFilter)

    val categories = listOf(
        "All",
        "Civil & Foundation",
        "MMS & Modules",
        "DC Cabling",
        "Earthing & LA",
        "Inverters & Panels",
        "AC Cabling & Trench",
        "HT Cabling",
        "Aux & Control Cables",
        "WMS & Ancillary"
    )

    // Compute Progress KPI Counts across full Work Order scope
    val allCount = tasksWithSummary.size
    val notStartedCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.NOT_STARTED) }
    val inProgressCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.IN_PROGRESS) }
    val nearCompleteCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.NEAR_COMPLETE) }
    val completedCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.COMPLETED) }
    val holdCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.HOLD) }
    val naCount = tasksWithSummary.count { isTaskMatchingProgressFilter(it.task, TaskProgressFilter.NA) }

    val overallProgress = if (tasksWithSummary.isNotEmpty()) {
        tasksWithSummary.map { calculateTaskProgressPercentage(it.task) }.average()
    } else 0.0

    // Filter by Progress, Category, and Search Query
    val filteredList = tasksWithSummary.filter { item ->
        val matchesProgress = isTaskMatchingProgressFilter(item.task, activeProgressFilter)
        val matchesCategory = (selectedCategory == "All" || item.task.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                item.task.title.contains(searchQuery, ignoreCase = true) ||
                item.task.itemNumber.contains(searchQuery, ignoreCase = true) ||
                item.task.description.contains(searchQuery, ignoreCase = true) ||
                item.task.category.contains(searchQuery, ignoreCase = true) ||
                item.task.status.contains(searchQuery, ignoreCase = true)
        matchesProgress && matchesCategory && matchesSearch
    }

    // Sort list according to chosen sort order
    val sortedList = remember(filteredList, sortOrder) {
        when (sortOrder) {
            TaskSortOrder.DEFAULT -> filteredList
            TaskSortOrder.PROGRESS_LOW_HIGH -> filteredList.sortedBy { calculateTaskProgressPercentage(it.task) }
            TaskSortOrder.PROGRESS_HIGH_LOW -> filteredList.sortedByDescending { calculateTaskProgressPercentage(it.task) }
            TaskSortOrder.BUDGET_HIGH_LOW -> filteredList.sortedByDescending { it.task.totalBudget }
            TaskSortOrder.SPENT_HIGH_LOW -> filteredList.sortedByDescending { it.totalSpent }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("task_list_screen")
        ) {
        // Top Filter & Search Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Project Selector Banner
            if (workOrders.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProjectMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Selected Project Tasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = activeProject?.projectName ?: "All Projects",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Switch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Project",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false }
                        ) {
                            workOrders.forEach { wo ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = wo.projectName,
                                                fontWeight = if (wo.id == selectedWorkOrderId) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "WO: ${wo.workOrderNumber} • ${wo.clientName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectWorkOrder(wo.id)
                                        showProjectMenu = false
                                    },
                                    leadingIcon = if (wo.id == selectedWorkOrderId) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Sort Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Work Order tasks (e.g. drilling, inverter, cable)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("task_sort_button")
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Sort Tasks",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        TaskSortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.title,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    sortOrder = order
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortOrder == order) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Overall Site Progress Summary Card with Interactive Quick Filters
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_summary_banner")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Site Execution Progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", overallProgress)}% Complete",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (overallProgress / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Interactive KPI Pills for Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Not Started
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeProgressFilter == TaskProgressFilter.NOT_STARTED)
                                Color(0xFF6B7280).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            border = if (activeProgressFilter == TaskProgressFilter.NOT_STARTED)
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B7280))
                            else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (activeProgressFilter == TaskProgressFilter.NOT_STARTED) {
                                        onProgressFilterSelected("ALL")
                                    } else {
                                        onProgressFilterSelected("NOT_STARTED")
                                    }
                                }
                                .testTag("kpi_filter_not_started")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$notStartedCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563)
                                )
                                Text(
                                    text = "Not Started",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // In Progress
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeProgressFilter == TaskProgressFilter.IN_PROGRESS)
                                Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            border = if (activeProgressFilter == TaskProgressFilter.IN_PROGRESS)
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
                            else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (activeProgressFilter == TaskProgressFilter.IN_PROGRESS) {
                                        onProgressFilterSelected("ALL")
                                    } else {
                                        onProgressFilterSelected("IN_PROGRESS")
                                    }
                                }
                                .testTag("kpi_filter_in_progress")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$inProgressCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "In Progress",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Completed
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeProgressFilter == TaskProgressFilter.COMPLETED)
                                Color(0xFF10B981).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface,
                            border = if (activeProgressFilter == TaskProgressFilter.COMPLETED)
                                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                            else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (activeProgressFilter == TaskProgressFilter.COMPLETED) {
                                        onProgressFilterSelected("ALL")
                                    } else {
                                        onProgressFilterSelected("COMPLETED")
                                    }
                                }
                                .testTag("kpi_filter_completed")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$completedCount",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Progress Filter Chips Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Progress:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskProgressFilter.values().forEach { filter ->
                        val isSelected = activeProgressFilter == filter
                        val filterCount = when (filter) {
                            TaskProgressFilter.ALL -> allCount
                            TaskProgressFilter.NOT_STARTED -> notStartedCount
                            TaskProgressFilter.IN_PROGRESS -> inProgressCount
                            TaskProgressFilter.NEAR_COMPLETE -> nearCompleteCount
                            TaskProgressFilter.COMPLETED -> completedCount
                            TaskProgressFilter.HOLD -> holdCount
                            TaskProgressFilter.NA -> naCount
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onProgressFilterSelected(filter.id) },
                            label = {
                                Text(
                                    text = "${filter.shortLabel} ($filterCount)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = filter.badgeColor.copy(alpha = 0.2f),
                                selectedLabelColor = filter.badgeColor
                            ),
                            modifier = Modifier.testTag("filter_progress_${filter.id.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Trade:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.testTag("filter_chip_${category.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // Active Filter Status & Reset Row
        val hasActiveFilters = activeProgressFilter != TaskProgressFilter.ALL ||
                !selectedCategory.equals("All", ignoreCase = true) ||
                searchQuery.isNotBlank() ||
                sortOrder != TaskSortOrder.DEFAULT

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Showing ${sortedList.size} of $allCount Tasks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasActiveFilters) {
                    Text(
                        text = buildString {
                            append("Filtered by: ")
                            if (activeProgressFilter != TaskProgressFilter.ALL) {
                                append(activeProgressFilter.shortLabel)
                                append(" • ")
                            }
                            if (!selectedCategory.equals("All", ignoreCase = true)) {
                                append(selectedCategory)
                                append(" • ")
                            }
                            if (searchQuery.isNotBlank()) {
                                append("\"$searchQuery\" • ")
                            }
                            if (sortOrder != TaskSortOrder.DEFAULT) {
                                append(sortOrder.title.take(15))
                            }
                        }.removeSuffix(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (hasActiveFilters) {
                TextButton(
                    onClick = {
                        onProgressFilterSelected("ALL")
                        onCategorySelected("All")
                        searchQuery = ""
                        sortOrder = TaskSortOrder.DEFAULT
                    },
                    modifier = Modifier.testTag("clear_task_filters_button")
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                val totalTasksBudget = sortedList.sumOf { if (it.task.totalBudget > 0.0) it.task.totalBudget else it.task.totalQty * it.task.unitPrice }
                Text(
                    text = "Total Budget: ${FormatUtils.formatLakhs(totalTasksBudget)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tasks List or Empty State
        if (sortedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Tasks Match Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No tasks found with Progress = ${activeProgressFilter.title} in Trade = $selectedCategory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onProgressFilterSelected("ALL")
                                onCategorySelected("All")
                                searchQuery = ""
                            }
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sortedList, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        onEditProgress = { taskToUpdateProgress = item.task },
                        onLogExpense = { onLogExpenseForTask(item.task) },
                        onQuickStatusChange = { newStatus ->
                            val newQty = when (newStatus) {
                                "COMPLETED" -> item.task.totalQty
                                "NOT_STARTED", "NA" -> 0.0
                                "HOLD" -> item.task.completedQty
                                else -> if (item.task.completedQty > 0) item.task.completedQty else (if (item.task.totalQty > 0) item.task.totalQty * 0.5 else 1.0)
                            }
                            onUpdateTaskProgress(item.task.id, newQty, newStatus)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Database updated: [${item.task.itemNumber}] is now $newStatus"
                                )
                            }
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

    // Task Progress Update Dialog
    val currentTask = taskToUpdateProgress
    if (currentTask != null) {
        TaskProgressDialog(
            task = currentTask,
            onDismiss = { taskToUpdateProgress = null },
            onSave = { completedQty, status ->
                onUpdateTaskProgress(currentTask.id, completedQty, status)
                taskToUpdateProgress = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Database updated: [${currentTask.itemNumber}] $status (${String.format(Locale.US, "%.1f", completedQty)} ${currentTask.uom})"
                    )
                }
            }
        )
    }
}

@Composable
fun TaskCard(
    item: TaskWithExpenseSummary,
    onEditProgress: () -> Unit,
    onLogExpense: () -> Unit,
    onQuickStatusChange: ((String) -> Unit)? = null
) {
    val task = item.task
    val categoryColor = FormatUtils.getCategoryColor(task.category)
    val budgetPercent = if (task.totalBudget > 0) (item.totalSpent / task.totalBudget) * 100.0 else 0.0
    val physicalProgress = calculateTaskProgressPercentage(task)
    var showQuickStatusMenu by remember { mutableStateOf(false) }

    val statusUpper = task.status.uppercase(Locale.US)
    val statusColor = when (statusUpper) {
        "COMPLETED" -> Color(0xFF10B981)
        "IN_PROGRESS" -> Color(0xFFF59E0B)
        "HOLD", "ON_HOLD" -> Color(0xFFEF4444)
        "NA", "N/A", "NOT_APPLICABLE" -> Color(0xFF8B5CF6)
        else -> if (physicalProgress >= 99.99) Color(0xFF10B981) else if (physicalProgress > 0.0) Color(0xFFF59E0B) else Color(0xFF6B7280)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Item #, Category Badge & Progress Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Item ${task.itemNumber}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                }

                // Interactive Progress status badge with icon and quick-change menu
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showQuickStatusMenu = true }
                            .testTag("status_badge_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (statusUpper) {
                                "COMPLETED" -> Icons.Default.CheckCircle
                                "IN_PROGRESS" -> Icons.Default.HourglassTop
                                "HOLD", "ON_HOLD" -> Icons.Default.PauseCircleOutline
                                "NA", "N/A", "NOT_APPLICABLE" -> Icons.Default.Block
                                else -> if (physicalProgress >= 99.99) Icons.Default.CheckCircle else if (physicalProgress > 0.0) Icons.Default.HourglassTop else Icons.Default.PlayCircleOutline
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (statusUpper) {
                                    "COMPLETED" -> "COMPLETED"
                                    "IN_PROGRESS" -> "IN PROGRESS"
                                    "HOLD", "ON_HOLD" -> "ON HOLD"
                                    "NA", "N/A", "NOT_APPLICABLE" -> "N/A"
                                    else -> if (physicalProgress >= 99.99) "COMPLETED" else if (physicalProgress > 0.0) "IN PROGRESS" else "NOT STARTED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Change Status",
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showQuickStatusMenu,
                        onDismissRequest = { showQuickStatusMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("NOT STARTED", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color(0xFF6B7280)) },
                            onClick = {
                                showQuickStatusMenu = false
                                onQuickStatusChange?.invoke("NOT_STARTED")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("IN PROGRESS", color = Color(0xFFB45309), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B)) },
                            onClick = {
                                showQuickStatusMenu = false
                                onQuickStatusChange?.invoke("IN_PROGRESS")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("COMPLETED", color = Color(0xFF047857), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981)) },
                            onClick = {
                                showQuickStatusMenu = false
                                onQuickStatusChange?.invoke("COMPLETED")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ON HOLD", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.PauseCircleOutline, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showQuickStatusMenu = false
                                onQuickStatusChange?.invoke("HOLD")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("N/A", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                            onClick = {
                                showQuickStatusMenu = false
                                onQuickStatusChange?.invoke("NA")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Scope
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contract Quantities & Rates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Work Order Qty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", task.totalQty)} ${task.uom}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "Unit Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", task.unitPrice)} / ${task.uom}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Allocated Budget",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Physical Site Execution Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Site Progress: ${String.format(Locale.US, "%.1f", task.completedQty)} / ${String.format(Locale.US, "%.1f", task.totalQty)} ${task.uom}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.0f", physicalProgress)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (physicalProgress / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expense Tracking & Financial Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${FormatUtils.formatInr(item.totalSpent)} (${String.format(Locale.US, "%.1f", budgetPercent)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetPercent > 100) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Remaining: ${FormatUtils.formatInr(item.remainingBudget)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.remainingBudget >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (budgetPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (budgetPercent > 100) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditProgress,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("update_progress_button_${task.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update Progress", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLogExpense,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("log_expense_button_${task.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskProgressDialog(
    task: WorkOrderTask,
    onDismiss: () -> Unit,
    onSave: (completedQty: Double, status: String) -> Unit
) {
    var completedQtyText by remember {
        mutableStateOf(
            if (task.status.uppercase(Locale.US) == "NOT_STARTED" || task.status.uppercase(Locale.US) == "NA") "0.0"
            else String.format(Locale.US, "%.1f", task.completedQty)
        )
    }
    var selectedStatus by remember { mutableStateOf(task.status) }
    val statuses = listOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "HOLD", "NA")

    val parsedQty = completedQtyText.toDoubleOrNull() ?: 0.0
    val calculatedPct = when (selectedStatus) {
        "NOT_STARTED", "NA" -> 0.0
        "COMPLETED" -> 100.0
        else -> if (task.totalQty > 0) ((parsedQty / task.totalQty) * 100.0).coerceIn(0.0, 100.0) else 0.0
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Site Execution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "[${task.itemNumber}] ${task.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope & Live percentage preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Contract Scope",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${task.totalQty} ${task.uom}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            calculatedPct >= 100.0 -> Color(0xFF10B981).copy(alpha = 0.2f)
                            calculatedPct > 0.0 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> Color(0xFF6B7280).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", calculatedPct)}% Complete",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                calculatedPct >= 100.0 -> Color(0xFF047857)
                                calculatedPct > 0.0 -> Color(0xFFB45309)
                                else -> Color(0xFF4B5563)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = completedQtyText,
                    onValueChange = { input ->
                        completedQtyText = input
                        val qty = input.toDoubleOrNull()
                        if (qty != null) {
                            if (qty >= task.totalQty && task.totalQty > 0) {
                                selectedStatus = "COMPLETED"
                            } else if (qty > 0) {
                                if (selectedStatus != "HOLD" && selectedStatus != "NA") {
                                    selectedStatus = "IN_PROGRESS"
                                }
                            } else {
                                selectedStatus = "NOT_STARTED"
                            }
                        }
                    },
                    label = { Text("Executed Quantity (${task.uom})") },
                    placeholder = { Text("Enter executed ${task.uom}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_quantity_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick percentage preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0 to "0%", 0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (frac, label) ->
                        OutlinedButton(
                            onClick = {
                                val targetQty = task.totalQty * frac
                                completedQtyText = String.format(Locale.US, "%.1f", targetQty)
                                selectedStatus = when {
                                    frac >= 1.0 -> "COMPLETED"
                                    frac > 0.0 -> "IN_PROGRESS"
                                    else -> "NOT_STARTED"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Execution Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statuses.forEach { st ->
                        val isSel = selectedStatus == st
                        val chipColor = when (st) {
                            "COMPLETED" -> Color(0xFF10B981)
                            "IN_PROGRESS" -> Color(0xFFF59E0B)
                            "HOLD" -> Color(0xFFEF4444)
                            "NA" -> Color(0xFF8B5CF6)
                            else -> Color(0xFF6B7280)
                        }
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedStatus = st
                                when (st) {
                                    "COMPLETED" -> {
                                        if ((completedQtyText.toDoubleOrNull() ?: 0.0) < task.totalQty) {
                                            completedQtyText = String.format(Locale.US, "%.1f", task.totalQty)
                                        }
                                    }
                                    "NOT_STARTED" -> {
                                        completedQtyText = "0.0"
                                    }
                                    "IN_PROGRESS" -> {
                                        val cur = completedQtyText.toDoubleOrNull() ?: 0.0
                                        if (cur <= 0.0 || cur >= task.totalQty) {
                                            val half = if (task.totalQty > 0) task.totalQty * 0.5 else 1.0
                                            completedQtyText = String.format(Locale.US, "%.1f", half)
                                        }
                                    }
                                    "HOLD" -> {
                                        // Retain current executed progress while on hold
                                    }
                                    "NA" -> {
                                        completedQtyText = "0.0"
                                    }
                                }
                            },
                            leadingIcon = {
                                val ic = when (st) {
                                    "COMPLETED" -> Icons.Default.CheckCircle
                                    "IN_PROGRESS" -> Icons.Default.HourglassTop
                                    "HOLD" -> Icons.Default.PauseCircleOutline
                                    "NA" -> Icons.Default.Block
                                    else -> Icons.Default.PlayCircleOutline
                                }
                                Icon(ic, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    when (st) {
                                        "NOT_STARTED" -> "Not Started"
                                        "IN_PROGRESS" -> "In Progress"
                                        "COMPLETED" -> "Completed"
                                        "HOLD" -> "Hold"
                                        "NA" -> "N/A"
                                        else -> st.replace("_", " ")
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                selectedLabelColor = chipColor,
                                selectedLeadingIconColor = chipColor
                            ),
                            modifier = Modifier.testTag("status_chip_$st")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Database persist indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusDisplay = when (selectedStatus) {
                            "NOT_STARTED" -> "Not Started"
                            "IN_PROGRESS" -> "In Progress"
                            "COMPLETED" -> "Completed"
                            "HOLD" -> "Hold"
                            "NA" -> "N/A"
                            else -> selectedStatus.replace('_', ' ')
                        }
                        Text(
                            text = "Status: $statusDisplay • Tap 'Save to Database' to persist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val qty = completedQtyText.toDoubleOrNull() ?: 0.0
                            val finalStatus = selectedStatus
                            val finalQty = when (finalStatus) {
                                "COMPLETED" -> if (qty < task.totalQty && task.totalQty > 0) task.totalQty else qty
                                "NOT_STARTED", "NA" -> 0.0
                                else -> qty
                            }
                            onSave(finalQty, finalStatus)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_progress_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

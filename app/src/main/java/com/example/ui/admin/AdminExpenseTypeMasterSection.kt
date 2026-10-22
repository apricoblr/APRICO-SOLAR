package com.example.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FsExpenseType

val EXPENSE_TYPE_GROUPS = listOf(
    "All",
    "Direct Scope",
    "Contract Work",
    "Logistics",
    "Equipment",
    "Compliance",
    "Safety",
    "Overhead"
)

/**
 * Master List view for Expense Types.
 * Allows adding, editing, and deleting expense types that appear throughout the application.
 */
@Composable
fun AdminExpenseTypeMasterSection(
    expenseTypes: List<FsExpenseType>,
    onAddExpenseType: (name: String, description: String, category: String) -> Unit,
    onUpdateExpenseType: (FsExpenseType) -> Unit,
    onDeleteExpenseType: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseTypeToEdit by remember { mutableStateOf<FsExpenseType?>(null) }
    var expenseTypeToDelete by remember { mutableStateOf<FsExpenseType?>(null) }

    val filteredTypes = remember(expenseTypes, searchQuery, selectedGroup) {
        expenseTypes.filter { type ->
            val matchesGroup = selectedGroup == "All" || type.category.equals(selectedGroup, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                type.name.contains(searchQuery, ignoreCase = true) ||
                type.description.contains(searchQuery, ignoreCase = true) ||
                type.category.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Expense Types Master",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${expenseTypes.size} Types Configured • Customize classifications for logging expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("admin_add_expense_type_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Type", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search expense type by name or description...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_expense_type_search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Group filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EXPENSE_TYPE_GROUPS.forEach { group ->
                        val count = if (group == "All") expenseTypes.size else expenseTypes.count { it.category.equals(group, ignoreCase = true) }
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text("$group ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Expense Types List
        if (filteredTypes.isEmpty()) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedGroup != "All") "No matching expense types" else "No expense types found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add custom expense types to categorize costs and billings on site.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Expense Type Now")
                    }
                }
            }
        } else {
            filteredTypes.forEach { type ->
                ExpenseTypeCardItem(
                    expenseType = type,
                    onEdit = { expenseTypeToEdit = type },
                    onDelete = { expenseTypeToDelete = type }
                )
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AddEditExpenseTypeDialog(
            initialType = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, description, category ->
                onAddExpenseType(name, description, category)
                showAddDialog = false
                onShowMessage("Expense type '$name' added successfully!")
            }
        )
    }

    // Edit Dialog
    expenseTypeToEdit?.let { type ->
        AddEditExpenseTypeDialog(
            initialType = type,
            onDismiss = { expenseTypeToEdit = null },
            onSave = { name, description, category ->
                onUpdateExpenseType(
                    type.copy(
                        name = name,
                        description = description,
                        category = category
                    )
                )
                expenseTypeToEdit = null
                onShowMessage("Expense type '$name' updated successfully!")
            }
        )
    }

    // Delete Confirmation Dialog
    expenseTypeToDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { expenseTypeToDelete = null },
            title = {
                Text(
                    text = "Delete Expense Type?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete expense type '${type.name}'? Existing logged expenses will retain their label."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpenseType(type.id)
                        onShowMessage("Deleted expense type '${type.name}'")
                        expenseTypeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Type", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseTypeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual Expense Type Card.
 */
@Composable
fun ExpenseTypeCardItem(
    expenseType: FsExpenseType,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_type_card_${expenseType.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = expenseType.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (expenseType.description.isNotBlank()) {
                            Text(
                                text = expenseType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = expenseType.category.ifBlank { "Direct Scope" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_expense_type_${expenseType.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("delete_expense_type_${expenseType.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dialog for Adding or Editing an Expense Type.
 */
@Composable
fun AddEditExpenseTypeDialog(
    initialType: FsExpenseType?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(initialType?.name ?: "") }
    var description by remember { mutableStateOf(initialType?.description ?: "") }
    var selectedGroup by remember { mutableStateOf(initialType?.category?.ifBlank { "Direct Scope" } ?: "Direct Scope") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val groups = listOf("Direct Scope", "Contract Work", "Logistics", "Equipment", "Compliance", "Safety", "Overhead")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialType == null) "Add Expense Type" else "Edit Expense Type",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Configure custom expense categories for the site team when logging bills, wages, and materials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Type Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Expense Type Name *") },
                    placeholder = { Text("e.g. Scaffolding Rental / Crane Fuel") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_type_input_name")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Scope Description") },
                    placeholder = { Text("e.g. Daily diesel supply for site DTH rig and cranes") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category / Group
                Text(
                    text = "Cost Group / Classification",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter an expense type name."
                                return@Button
                            }
                            onSave(name.trim(), description.trim(), selectedGroup)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("expense_type_dialog_save_button")
                    ) {
                        Text(if (initialType == null) "Save Type" else "Update Type", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FsVendor

val VENDOR_CATEGORIES = listOf(
    "All",
    "Material",
    "Machinery & Rig",
    "Labor Supply",
    "Transport",
    "Civil Works",
    "Electrical",
    "Subcontractor",
    "General"
)

/**
 * Master List view for Parties & Vendors.
 * Supports searching, category filtering, dialing, copying phone numbers, adding, editing, and deleting.
 */
@Composable
fun AdminVendorMasterSection(
    vendors: List<FsVendor>,
    onAddVendor: (name: String, phone: String, contact: String, category: String, address: String, notes: String) -> Unit,
    onUpdateVendor: (FsVendor) -> Unit,
    onDeleteVendor: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var vendorToEdit by remember { mutableStateOf<FsVendor?>(null) }
    var vendorToDelete by remember { mutableStateOf<FsVendor?>(null) }

    val filteredVendors = remember(vendors, searchQuery, selectedCategory) {
        vendors.filter { vendor ->
            val matchesCategory = selectedCategory == "All" || vendor.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                vendor.name.contains(searchQuery, ignoreCase = true) ||
                vendor.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                vendor.contactPerson.contains(searchQuery, ignoreCase = true) ||
                vendor.address.contains(searchQuery, ignoreCase = true) ||
                vendor.notes.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
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
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Party / Vendor Master",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${vendors.size} Registered Vendors • Manage names, phone numbers & contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("admin_add_vendor_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Party", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search party by name, phone number, contact...") },
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
                        .testTag("admin_vendor_search_field")
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
                    VENDOR_CATEGORIES.forEach { category ->
                        val count = if (category == "All") vendors.size else vendors.count { it.category.equals(category, ignoreCase = true) }
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("$category ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Vendor Cards List
        if (filteredVendors.isEmpty()) {
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
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No matching parties or vendors" else "No vendors registered yet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add party name, phone number, and category details to easily reference them in expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Party Now")
                    }
                }
            }
        } else {
            filteredVendors.forEach { vendor ->
                VendorCardItem(
                    vendor = vendor,
                    onEdit = { vendorToEdit = vendor },
                    onDelete = { vendorToDelete = vendor },
                    onShowMessage = onShowMessage
                )
            }
        }
    }

    // Add Vendor Dialog
    if (showAddDialog) {
        AddEditVendorDialog(
            initialVendor = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, contact, category, address, notes ->
                onAddVendor(name, phone, contact, category, address, notes)
                showAddDialog = false
                onShowMessage("Party '$name' added successfully!")
            }
        )
    }

    // Edit Vendor Dialog
    vendorToEdit?.let { vendor ->
        AddEditVendorDialog(
            initialVendor = vendor,
            onDismiss = { vendorToEdit = null },
            onSave = { name, phone, contact, category, address, notes ->
                onUpdateVendor(
                    vendor.copy(
                        name = name,
                        phoneNumber = phone,
                        contactPerson = contact,
                        category = category,
                        address = address,
                        notes = notes
                    )
                )
                vendorToEdit = null
                onShowMessage("Party '$name' updated successfully!")
            }
        )
    }

    // Delete Vendor Confirmation Dialog
    vendorToDelete?.let { vendor ->
        AlertDialog(
            onDismissRequest = { vendorToDelete = null },
            title = {
                Text(
                    text = "Delete Party / Vendor?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${vendor.name}' (${vendor.phoneNumber}) from the master vendor directory?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVendor(vendor.id)
                        onShowMessage("Deleted vendor '${vendor.name}'")
                        vendorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Vendor", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vendorToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual Vendor Card display.
 */
@Composable
fun VendorCardItem(
    vendor: FsVendor,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vendor_card_${vendor.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Category
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = vendor.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (vendor.contactPerson.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = vendor.contactPerson,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = vendor.category.ifBlank { "General" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number Block with Direct Dial & Copy
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vendor.phoneNumber.isNotBlank()) vendor.phoneNumber else "No phone entered",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (vendor.phoneNumber.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (vendor.phoneNumber.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(vendor.phoneNumber))
                                    onShowMessage("Copied phone: ${vendor.phoneNumber}")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dial Call button
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vendor.phoneNumber.trim()}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        onShowMessage("Unable to launch dialer")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Address & Notes (if available)
            if (vendor.address.isNotBlank() || vendor.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (vendor.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = vendor.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (vendor.notes.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = vendor.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    modifier = Modifier.testTag("edit_vendor_${vendor.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Details", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.testTag("delete_vendor_${vendor.id}")
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
 * Dialog for Adding or Editing a Party / Vendor.
 */
@Composable
fun AddEditVendorDialog(
    initialVendor: FsVendor?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, contact: String, category: String, address: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(initialVendor?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(initialVendor?.phoneNumber ?: "") }
    var contactPerson by remember { mutableStateOf(initialVendor?.contactPerson ?: "") }
    var selectedCategory by remember { mutableStateOf(initialVendor?.category?.ifBlank { "Material" } ?: "Material") }
    var address by remember { mutableStateOf(initialVendor?.address ?: "") }
    var notes by remember { mutableStateOf(initialVendor?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Material", "Machinery & Rig", "Labor Supply", "Transport", "Civil Works", "Electrical", "Subcontractor", "General")

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
                    text = if (initialVendor == null) "Add New Party / Vendor" else "Edit Party / Vendor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Save party details to easily select and reference them when logging project expenses.",
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

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Party / Vendor Name *") },
                    placeholder = { Text("e.g. Bhavani Rig Services / UltraTech") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_input_name")
                )

                // Phone Number field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        errorMessage = null
                    },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("e.g. +91 98450 12345") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_input_phone")
                )

                // Contact Person field
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person / Manager") },
                    placeholder = { Text("e.g. Bhavani Kumar") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vendor_input_contact")
                )

                // Category selector
                Text(
                    text = "Category / Service Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Address field
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Office / Site Address") },
                    placeholder = { Text("e.g. Vijayapura Highway, Devarahipparagi") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes / GST / Bank Details
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / GSTIN / Account Info") },
                    placeholder = { Text("e.g. GSTIN 29AAAAA0000A1Z5 or Bank IFSC") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                                errorMessage = "Please enter party/vendor name."
                                return@Button
                            }
                            if (phoneNumber.isBlank()) {
                                errorMessage = "Please enter phone number."
                                return@Button
                            }
                            onSave(name.trim(), phoneNumber.trim(), contactPerson.trim(), selectedCategory, address.trim(), notes.trim())
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("vendor_dialog_save_button")
                    ) {
                        Text(if (initialVendor == null) "Save Party" else "Update Party", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

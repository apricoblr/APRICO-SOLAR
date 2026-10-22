package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.firebase.FsProject
import com.example.data.firebase.FsTask
import com.example.data.local.PreloadedData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWorkOrderDialog(
    existingProjects: List<FsProject>,
    onDismiss: () -> Unit,
    onSaveWorkOrder: (
        projectName: String,
        projectLocation: String,
        capacity: String,
        workOrderNumber: String,
        clientName: String,
        contractorName: String,
        baseBudget: Double,
        durationDays: Int,
        startDate: String,
        tasks: List<FsTask>
    ) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("2.0 MW") }
    var workOrderNumber by remember { mutableStateOf("Maxinfra/KUSUM Solar/Site2/2026-27/0004") }
    var clientName by remember { mutableStateOf("M/S MAX INFRA (I) PVT LTD") }
    var contractorName by remember { mutableStateOf("APRICOR (Arun Ravulapalli Kumar)") }
    var baseBudgetText by remember { mutableStateOf("3500000") }
    var durationDaysText by remember { mutableStateOf("45") }
    var startDateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var selectedTemplate by remember { mutableStateOf("Standard Solar 44-Task Scope") }

    val templates = listOf("Standard Solar 44-Task Scope", "Civil & Foundation Scope", "Electrical & Inverter Scope")
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val baseBudget = baseBudgetText.toDoubleOrNull() ?: 0.0
    val gstAmount = baseBudget * 0.18
    val totalBudget = baseBudget + gstAmount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_work_order_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Admin: Add Work Order",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create new solar contract in Firebase",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_wo_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Project Name
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project / Plant Name") },
                    placeholder = { Text("e.g. KUSUM Solar / Sindhanur 2.5 MW") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wo_project_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Location & Capacity
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Site Location") },
                        placeholder = { Text("e.g. Sindhanur, Raichur") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("wo_location_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacity") },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("wo_capacity_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Work Order Number
                OutlinedTextField(
                    value = workOrderNumber,
                    onValueChange = { workOrderNumber = it },
                    label = { Text("Work Order Number") },
                    placeholder = { Text("e.g. Maxinfra/KUSUM Solar/2026-27/0004") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wo_number_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Client & Contractor
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client / Employer Organization") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = contractorName,
                    onValueChange = { contractorName = it },
                    label = { Text("Contractor / Vendor Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Base Budget & Calculations
                OutlinedTextField(
                    value = baseBudgetText,
                    onValueChange = { baseBudgetText = it },
                    label = { Text("Base Contract Amount (₹)") },
                    placeholder = { Text("e.g. 3243160") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wo_base_budget_input"),
                    prefix = { Text("₹ ") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live GST & Gross calculation card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GST @ 18%: ${FormatUtils.formatInr(gstAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gross Contract: ${FormatUtils.formatInr(totalBudget)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Auto-Calculated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration & Start Date
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = durationDaysText,
                        onValueChange = { durationDaysText = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wo_duration_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { startDateText = it },
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wo_start_date_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scope Template
                Text(
                    text = "Contract Task Scope Template",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    templates.forEach { tpl ->
                        FilterChip(
                            selected = selectedTemplate == tpl,
                            onClick = { selectedTemplate = tpl },
                            label = { Text(tpl) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_wo_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (projectName.isBlank()) {
                                errorMessage = "Please enter the project / plant name"
                                return@Button
                            }
                            if (workOrderNumber.isBlank()) {
                                errorMessage = "Please enter the work order number"
                                return@Button
                            }
                            if (baseBudget <= 0) {
                                errorMessage = "Please enter a valid base budget amount"
                                return@Button
                            }

                            val days = durationDaysText.toIntOrNull() ?: 45

                            // Generate task items based on template scaled to budget ratio
                            val budgetRatio = if (PreloadedData.TOTAL_BASE_BUDGET > 0) baseBudget / PreloadedData.TOTAL_BASE_BUDGET else 1.0

                            val sourceTasks = when (selectedTemplate) {
                                "Civil & Foundation Scope" -> PreloadedData.INITIAL_TASKS.filter { it.category == "Civil & Foundation" }
                                "Electrical & Inverter Scope" -> PreloadedData.INITIAL_TASKS.filter { it.category in listOf("Inverters & Panels", "DC Cabling", "AC Cabling & Trench", "HT Cabling") }
                                else -> PreloadedData.INITIAL_TASKS
                            }

                            val generatedTasks = sourceTasks.mapIndexed { index, t ->
                                val scaledPrice = String.format(Locale.US, "%.1f", t.unitPrice * budgetRatio).toDoubleOrNull() ?: t.unitPrice
                                val scaledBudget = String.format(Locale.US, "%.0f", t.totalQty * scaledPrice).toDoubleOrNull() ?: t.totalBudget
                                FsTask(
                                    itemNumber = t.itemNumber,
                                    category = t.category,
                                    title = t.title,
                                    description = t.description,
                                    uom = t.uom,
                                    totalQty = t.totalQty,
                                    unitPrice = scaledPrice,
                                    totalBudget = scaledBudget,
                                    completedQty = 0.0,
                                    status = "NOT_STARTED"
                                )
                            }

                            onSaveWorkOrder(
                                projectName,
                                location.ifBlank { "Karnataka Solar Site" },
                                capacity,
                                workOrderNumber,
                                clientName,
                                contractorName,
                                baseBudget,
                                days,
                                startDateText,
                                generatedTasks
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("submit_wo_button")
                    ) {
                        Text("Save to Firebase", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

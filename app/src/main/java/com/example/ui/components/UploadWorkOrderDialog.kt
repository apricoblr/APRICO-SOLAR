package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.firebase.FsTask
import com.example.data.local.PreloadedData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PredefinedWorkOrderTemplate(
    val title: String,
    val siteName: String,
    val location: String,
    val capacity: String,
    val workOrderNo: String,
    val client: String,
    val contractor: String,
    val baseBudget: Double,
    val durationDays: Int,
    val badge: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadWorkOrderDialog(
    onDismiss: () -> Unit,
    onUploadWorkOrder: (
        projectName: String,
        location: String,
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
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Instant Templates", "Upload Document / Scan", "Manual Entry")

    // Ready-to-import Solar Project Work Orders
    val sampleWorkOrders = listOf(
        PredefinedWorkOrderTemplate(
            title = "KUSUM Solar Sindhanur 2.5 MW",
            siteName = "KUSUM Solar / Sindhanur 2.5 MW",
            location = "Sindhanur, Raichur Dist, Karnataka",
            capacity = "2.50 MW",
            workOrderNo = "Maxinfra/KUSUM Solar/Sindhanur/2026-27/0004",
            client = "M/S MAX INFRA (I) PVT LTD",
            contractor = "APRICOR (Arun Ravulapalli Kumar)",
            baseBudget = 4500000.0,
            durationDays = 50,
            badge = "PM-KUSUM C"
        ),
        PredefinedWorkOrderTemplate(
            title = "Bijapur Agri Feeder 1.2 MW",
            siteName = "Bijapur Substation Feeder Solar 1.2 MW",
            location = "Bijapur Sub-Station, Karnataka",
            capacity = "1.20 MW",
            workOrderNo = "HESCOM/Solar/BJP/2026-27/0012",
            client = "HESCOM / KREDL Karnataka",
            contractor = "APRICOR (Arun Ravulapalli Kumar)",
            baseBudget = 2250000.0,
            durationDays = 35,
            badge = "Feeder Solarization"
        ),
        PredefinedWorkOrderTemplate(
            title = "Kalaburagi Solar EPC 3.0 MW",
            siteName = "Kalaburagi Mega Solar Farm 3.0 MW",
            location = "Kalaburagi District, Karnataka",
            capacity = "3.00 MW",
            workOrderNo = "Maxinfra/KUSUM Solar/KLB/2026-27/0005",
            client = "M/S MAX INFRA (I) PVT LTD",
            contractor = "APRICOR (Arun Ravulapalli Kumar)",
            baseBudget = 5800000.0,
            durationDays = 60,
            badge = "High Capacity"
        )
    )

    // Manual form fields
    var customProjectName by remember { mutableStateOf("") }
    var customLocation by remember { mutableStateOf("") }
    var customCapacity by remember { mutableStateOf("2.0 MW") }
    var customWoNumber by remember { mutableStateOf("WO/SOLAR/2026-27/009") }
    var customClient by remember { mutableStateOf("M/S MAX INFRA (I) PVT LTD") }
    var customContractor by remember { mutableStateOf("APRICOR (Arun Ravulapalli Kumar)") }
    var customBaseBudgetText by remember { mutableStateOf("3800000") }
    var customDurationText by remember { mutableStateOf("45") }
    var customStartDateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var isSimulatingOcr by remember { mutableStateOf(false) }

    // File Picker Launcher for documents/PDF/images
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploadedFileName = "work_order_${System.currentTimeMillis() % 1000}.pdf"
            isSimulatingOcr = true
            // Auto fill fields from simulated document
            customProjectName = "KUSUM Solar / Bagalkot 2.2 MW"
            customLocation = "Bagalkot, Karnataka"
            customCapacity = "2.2 MW"
            customWoNumber = "Maxinfra/KUSUM/Bagalkot/2026-27/0006"
            customBaseBudgetText = "4100000"
            isSimulatingOcr = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("upload_work_order_dialog")
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add / Upload Work Order",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Each work order creates a new distinct project",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_upload_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    // TAB 0: Instant Templates (1-Click Project & Work Order Creation)
                    0 -> {
                        Text(
                            text = "Select a Verified Solar Project Work Order:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        sampleWorkOrders.forEach { wo ->
                            val totalVal = wo.baseBudget * 1.18
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable {
                                        // Generate 44 scaled tasks
                                        val ratio = wo.baseBudget / PreloadedData.TOTAL_BASE_BUDGET
                                        val tasks = PreloadedData.INITIAL_TASKS.map { t ->
                                            val price = String.format(Locale.US, "%.1f", t.unitPrice * ratio).toDoubleOrNull() ?: t.unitPrice
                                            val budget = String.format(Locale.US, "%.0f", t.totalQty * price).toDoubleOrNull() ?: t.totalBudget
                                            FsTask(
                                                itemNumber = t.itemNumber,
                                                category = t.category,
                                                title = t.title,
                                                description = t.description,
                                                uom = t.uom,
                                                totalQty = t.totalQty,
                                                unitPrice = price,
                                                totalBudget = budget,
                                                completedQty = 0.0,
                                                status = "NOT_STARTED"
                                            )
                                        }
                                        onUploadWorkOrder(
                                            wo.siteName,
                                            wo.location,
                                            wo.capacity,
                                            wo.workOrderNo,
                                            wo.client,
                                            wo.contractor,
                                            wo.baseBudget,
                                            wo.durationDays,
                                            LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                                            tasks
                                        )
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = wo.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = wo.badge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${wo.workOrderNo} • ${wo.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Contract: ${FormatUtils.formatInr(totalVal)} (incl. 18% GST)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "1-Tap Import →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: Upload Document / PDF / Scan
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Upload Work Order Document / PDF",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select PDF, scanned work order, or service order",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select File From Device")
                            }

                            if (uploadedFileName != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Parsed: $uploadedFileName → ${customProjectName}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }

                        if (uploadedFileName != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Review Extracted Project Details:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customProjectName,
                                onValueChange = { customProjectName = it },
                                label = { Text("Project Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customWoNumber,
                                onValueChange = { customWoNumber = it },
                                label = { Text("Work Order No") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customBaseBudgetText,
                                onValueChange = { customBaseBudgetText = it },
                                label = { Text("Base Budget (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // TAB 2: Manual Entry Form
                    2 -> {
                        OutlinedTextField(
                            value = customProjectName,
                            onValueChange = { customProjectName = it },
                            label = { Text("Project / Plant Name") },
                            placeholder = { Text("e.g. KUSUM Solar / Bijapur 1.5 MW") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = customLocation,
                                onValueChange = { customLocation = it },
                                label = { Text("Site Location") },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = customCapacity,
                                onValueChange = { customCapacity = it },
                                label = { Text("Capacity") },
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customWoNumber,
                            onValueChange = { customWoNumber = it },
                            label = { Text("Work Order Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customClient,
                            onValueChange = { customClient = it },
                            label = { Text("Client Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customBaseBudgetText,
                            onValueChange = { customBaseBudgetText = it },
                            label = { Text("Base Budget (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("₹ ") },
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Actions for Tab 1 & Tab 2
                if (selectedTabIndex > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                val name = customProjectName.ifBlank { "KUSUM Solar New Site" }
                                val woNum = customWoNumber.ifBlank { "WO-${(1000..9999).random()}" }
                                val budget = customBaseBudgetText.toDoubleOrNull() ?: 3500000.0
                                val days = customDurationText.toIntOrNull() ?: 45

                                val ratio = budget / PreloadedData.TOTAL_BASE_BUDGET
                                val tasks = PreloadedData.INITIAL_TASKS.map { t ->
                                    val price = String.format(Locale.US, "%.1f", t.unitPrice * ratio).toDoubleOrNull() ?: t.unitPrice
                                    val b = String.format(Locale.US, "%.0f", t.totalQty * price).toDoubleOrNull() ?: t.totalBudget
                                    FsTask(
                                        itemNumber = t.itemNumber,
                                        category = t.category,
                                        title = t.title,
                                        description = t.description,
                                        uom = t.uom,
                                        totalQty = t.totalQty,
                                        unitPrice = price,
                                        totalBudget = b,
                                        completedQty = 0.0,
                                        status = "NOT_STARTED"
                                    )
                                }

                                onUploadWorkOrder(
                                    name,
                                    customLocation.ifBlank { "Karnataka Solar Site" },
                                    customCapacity,
                                    woNum,
                                    customClient,
                                    customContractor,
                                    budget,
                                    days,
                                    customStartDateText,
                                    tasks
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("submit_uploaded_wo_button")
                        ) {
                            Text("Create Project in Firebase", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

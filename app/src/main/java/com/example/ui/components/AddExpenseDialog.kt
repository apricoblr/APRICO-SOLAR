package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.firebase.FsExpenseType
import com.example.data.firebase.FsVendor
import com.example.data.model.ScannedReceiptData
import com.example.data.model.WorkOrderTask
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class PhotoUploadTarget(val title: String) {
    BILL("Bill / Invoice Photo"),
    PAYMENT("Payment Proof Photo"),
    EXPENSE_SITE("Expense Type / Site Photo")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    tasks: List<WorkOrderTask>,
    initialScannedData: ScannedReceiptData? = null,
    projectName: String? = null,
    availableVendors: List<FsVendor> = emptyList(),
    availableExpenseTypes: List<FsExpenseType> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        taskId: String?,
        taskTitle: String,
        category: String,
        amount: Double,
        date: String,
        vendor: String,
        billNo: String,
        expenseType: String,
        paymentMode: String,
        notes: String,
        billPhotoUri: String?,
        paymentPhotoUri: String?,
        expensePhotoUri: String?,
        billAmount: Double,
        paidAmount: Double,
        balanceAmount: Double,
        paymentStatus: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Initial task selection
    val initialTask = tasks.find { it.id == initialScannedData?.suggestedTaskId } ?: tasks.firstOrNull()

    var selectedTask by remember { mutableStateOf(initialTask) }
    var showTaskPickerModal by remember { mutableStateOf(false) }

    // Three Separate Photo Upload States
    var billPhotoUri by remember { mutableStateOf<String?>(null) }
    var paymentPhotoUri by remember { mutableStateOf<String?>(null) }
    var expensePhotoUri by remember { mutableStateOf<String?>(null) }

    var currentPhotoTarget by remember { mutableStateOf<PhotoUploadTarget?>(null) }
    var fullPhotoPreview by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Launchers for capturing/picking photo for the active target
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && currentPhotoTarget != null) {
            val prefix = when (currentPhotoTarget!!) {
                PhotoUploadTarget.BILL -> "bill"
                PhotoUploadTarget.PAYMENT -> "payment"
                PhotoUploadTarget.EXPENSE_SITE -> "expense"
            }
            val savedPath = saveUriToFile(context, uri, prefix)
            when (currentPhotoTarget!!) {
                PhotoUploadTarget.BILL -> billPhotoUri = savedPath
                PhotoUploadTarget.PAYMENT -> paymentPhotoUri = savedPath
                PhotoUploadTarget.EXPENSE_SITE -> expensePhotoUri = savedPath
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null && currentPhotoTarget != null) {
            val prefix = when (currentPhotoTarget!!) {
                PhotoUploadTarget.BILL -> "bill"
                PhotoUploadTarget.PAYMENT -> "payment"
                PhotoUploadTarget.EXPENSE_SITE -> "expense"
            }
            val savedPath = saveBitmapToFile(context, bitmap, prefix)
            when (currentPhotoTarget!!) {
                PhotoUploadTarget.BILL -> billPhotoUri = savedPath
                PhotoUploadTarget.PAYMENT -> paymentPhotoUri = savedPath
                PhotoUploadTarget.EXPENSE_SITE -> expensePhotoUri = savedPath
            }
        }
    }

    var amountText by remember {
        mutableStateOf(
            if (initialScannedData != null && initialScannedData.totalAmount > 0) {
                String.format(java.util.Locale.US, "%.0f", initialScannedData.totalAmount)
            } else ""
        )
    }
    var paymentStatus by remember { mutableStateOf("PAID") } // "PAID", "PARTIALLY_PAID", "TO_PAY"
    var paidAmountText by remember { mutableStateOf("") }
    var vendorName by remember { mutableStateOf(initialScannedData?.vendor ?: "") }
    var billNumber by remember { mutableStateOf(initialScannedData?.billNumber ?: "") }
    var dateText by remember {
        mutableStateOf(initialScannedData?.date ?: LocalDate.now().format(DateTimeFormatter.ISO_DATE))
    }
    var selectedCategory by remember {
        mutableStateOf(initialScannedData?.suggestedCategory ?: initialTask?.category ?: "Civil & Foundation")
    }

    val expenseTypes = remember(availableExpenseTypes) {
        if (availableExpenseTypes.isNotEmpty()) {
            availableExpenseTypes.map { it.name }
        } else {
            listOf("Labor", "Material", "Machinery/Fuel", "Subcontractor", "Transport", "Misc")
        }
    }
    var selectedExpenseType by remember(expenseTypes) {
        mutableStateOf(expenseTypes.firstOrNull() ?: "Material")
    }

    val paymentModes = listOf("Bank Transfer", "UPI", "Cash", "Cheque")
    var selectedPaymentMode by remember { mutableStateOf("Bank Transfer") }

    var notesText by remember {
        mutableStateOf(initialScannedData?.lineItemsSummary ?: "")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .padding(vertical = 12.dp)
                .testTag("add_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Changed title to "Log Expense"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (!projectName.isNullOrBlank()) "Project: $projectName" else "Work Order Project Scope",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_expense_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (initialScannedData != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scanned bill data applied",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Linked Contract Task Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Linked Contract Task (${tasks.size} Available)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { showTaskPickerModal = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse Tasks", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTaskPickerModal = true }
                        .testTag("task_selector_field")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTask != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Item ${selectedTask?.itemNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedTask?.category ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedTask?.title ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Scope: ${selectedTask?.totalQty} ${selectedTask?.uom} • Budget: ${FormatUtils.formatInr(selectedTask?.totalBudget ?: 0.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Tap to choose from all ${tasks.size} contract tasks...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Select Task",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Core Expense Details Section
                Text(
                    text = "Expense Amount & Payment Terms",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Total Bill / Invoice Amount (₹) *") },
                    placeholder = { Text("e.g. 45000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Status selector (Paid, Partially Paid, To Pay)
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Payment Status selector (To Pay, Paid, Partially Paid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Status",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (paymentStatus) {
                            "TO_PAY" -> "Pending Payment"
                            "PAID" -> "Cleared 100%"
                            "PARTIALLY_PAID" -> "Partial Payment"
                            else -> "Select Status"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (paymentStatus) {
                            "TO_PAY" -> Color(0xFFB45309)
                            "PAID" -> Color(0xFF047857)
                            "PARTIALLY_PAID" -> Color(0xFF1D4ED8)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. To Pay Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (paymentStatus == "TO_PAY") Color(0xFFF59E0B).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (paymentStatus == "TO_PAY") Color(0xFFF59E0B) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                paymentStatus = "TO_PAY"
                                errorMessage = null
                            }
                            .testTag("payment_status_topay")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (paymentStatus == "TO_PAY") Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "To Pay",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentStatus == "TO_PAY") Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Paid Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (paymentStatus == "PAID") Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (paymentStatus == "PAID") Color(0xFF10B981) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                paymentStatus = "PAID"
                                errorMessage = null
                            }
                            .testTag("payment_status_paid")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (paymentStatus == "PAID") Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentStatus == "PAID") Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 3. Partially Paid Option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (paymentStatus == "PARTIALLY_PAID") Color(0xFF3B82F6).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (paymentStatus == "PARTIALLY_PAID") Color(0xFF3B82F6) else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                paymentStatus = "PARTIALLY_PAID"
                                errorMessage = null
                            }
                            .testTag("payment_status_partial")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (paymentStatus == "PARTIALLY_PAID") Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Partially Paid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentStatus == "PARTIALLY_PAID") Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // If Partially Paid, show Paid Amount input field
                if (paymentStatus == "PARTIALLY_PAID") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = {
                            paidAmountText = it
                            errorMessage = null
                        },
                        label = { Text("Paid Amount (₹) *") },
                        placeholder = { Text("e.g. 20000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_paid_amount_input"),
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        singleLine = true
                    )
                }

                val currentBillAmt = amountText.toDoubleOrNull() ?: 0.0
                val currentPaidAmt = when (paymentStatus) {
                    "PAID" -> currentBillAmt
                    "TO_PAY" -> 0.0
                    "PARTIALLY_PAID" -> paidAmountText.toDoubleOrNull() ?: 0.0
                    else -> currentBillAmt
                }
                val currentBalAmt = (currentBillAmt - currentPaidAmt).coerceAtLeast(0.0)

                // Financial Balance & Payment Summary Card
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (paymentStatus) {
                        "PAID" -> Color(0xFF10B981).copy(alpha = 0.08f)
                        "PARTIALLY_PAID" -> Color(0xFF3B82F6).copy(alpha = 0.08f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (paymentStatus) {
                            "PAID" -> Color(0xFF10B981).copy(alpha = 0.3f)
                            "PARTIALLY_PAID" -> Color(0xFF3B82F6).copy(alpha = 0.3f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_balance_summary_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (paymentStatus) {
                                    "PAID" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    "PARTIALLY_PAID" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                    else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = when (paymentStatus) {
                                        "PAID" -> "100% PAID"
                                        "PARTIALLY_PAID" -> "PARTIALLY PAID"
                                        else -> "TO PAY (PENDING)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (paymentStatus) {
                                        "PAID" -> Color(0xFF047857)
                                        "PARTIALLY_PAID" -> Color(0xFF1D4ED8)
                                        else -> Color(0xFFB45309)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bill Amount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(currentBillAmt),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Paid Amount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(currentPaidAmt),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Balance to Pay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(currentBalAmt),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentBalAmt > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vendorName,
                    onValueChange = { vendorName = it },
                    label = { Text("Vendor / Party Name") },
                    placeholder = { Text("e.g. Sri Sai Rig Services / Bharat Petroleum") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_vendor_input"),
                    singleLine = true
                )

                // Quick Party / Vendor Suggestion Chips from Master List
                if (availableVendors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Parties:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val matchingVendors = if (vendorName.isBlank()) availableVendors.take(6) else availableVendors.filter { it.name.contains(vendorName, ignoreCase = true) }
                        matchingVendors.forEach { vendor ->
                            SuggestionChip(
                                onClick = {
                                    vendorName = vendor.name
                                },
                                label = { Text(vendor.name) }
                            )
                        }
                    }
                }

                // Show matched vendor phone and contact info
                val matchedVendor = availableVendors.find { it.name.equals(vendorName.trim(), ignoreCase = true) }
                if (matchedVendor != null && matchedVendor.phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Party Contact: ${matchedVendor.phoneNumber}${if (matchedVendor.contactPerson.isNotBlank()) " (${matchedVendor.contactPerson})" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text("Bill / Voucher #") },
                        placeholder = { Text("e.g. INV-104") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_billno_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_date_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Classifications & Notes Section
                Text(
                    text = "Expense Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    expenseTypes.forEach { type ->
                        FilterChip(
                            selected = selectedExpenseType == type,
                            onClick = { selectedExpenseType = type },
                            label = { Text(type) },
                            leadingIcon = if (selectedExpenseType == type) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paymentModes.forEach { mode ->
                        FilterChip(
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Description / Site Notes") },
                    placeholder = { Text("e.g. Diesel for crane, 50 bags cement delivery") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_notes_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Photo Proofs & Attachments (3 Separate Uploads)
                Text(
                    text = "Supporting Photo Documents (3 Types)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Upload Bill photo, Payment proof photo, and Expense type/Site photo separately",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Card 1: Bill Photo
                ExpensePhotoUploadCard(
                    title = "1. Bill / Invoice Photo",
                    subtitle = "Vendor tax invoice, cash bill, or delivery challan",
                    icon = Icons.Default.ReceiptLong,
                    photoUri = billPhotoUri,
                    onPickGallery = {
                        currentPhotoTarget = PhotoUploadTarget.BILL
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTakePhoto = {
                        currentPhotoTarget = PhotoUploadTarget.BILL
                        cameraLauncher.launch(null)
                    },
                    onViewPhoto = {
                        billPhotoUri?.let { fullPhotoPreview = Pair(it, "Bill / Invoice Photo") }
                    },
                    onRemovePhoto = { billPhotoUri = null },
                    testTagPrefix = "bill_photo"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Payment Photo
                ExpensePhotoUploadCard(
                    title = "2. Payment Photo",
                    subtitle = "UPI screenshot, bank debit voucher, or cheque copy",
                    icon = Icons.Default.Payments,
                    photoUri = paymentPhotoUri,
                    onPickGallery = {
                        currentPhotoTarget = PhotoUploadTarget.PAYMENT
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTakePhoto = {
                        currentPhotoTarget = PhotoUploadTarget.PAYMENT
                        cameraLauncher.launch(null)
                    },
                    onViewPhoto = {
                        paymentPhotoUri?.let { fullPhotoPreview = Pair(it, "Payment Proof Photo") }
                    },
                    onRemovePhoto = { paymentPhotoUri = null },
                    testTagPrefix = "payment_photo"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Card 3: Expense Type / Site Photo
                ExpensePhotoUploadCard(
                    title = "3. Expense Type / Site Photo",
                    subtitle = "On-site task execution, material delivery, or work progress",
                    icon = Icons.Default.Engineering,
                    photoUri = expensePhotoUri,
                    onPickGallery = {
                        currentPhotoTarget = PhotoUploadTarget.EXPENSE_SITE
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTakePhoto = {
                        currentPhotoTarget = PhotoUploadTarget.EXPENSE_SITE
                        cameraLauncher.launch(null)
                    },
                    onViewPhoto = {
                        expensePhotoUri?.let { fullPhotoPreview = Pair(it, "Expense Type / Site Photo") }
                    },
                    onRemovePhoto = { expensePhotoUri = null },
                    testTagPrefix = "expense_site_photo"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_expense_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val billAmount = amountText.toDoubleOrNull()
                            if (billAmount == null || billAmount <= 0) {
                                errorMessage = "Please enter a valid bill amount greater than 0"
                                return@Button
                            }
                            val paidAmount = when (paymentStatus) {
                                "PAID" -> billAmount
                                "TO_PAY" -> 0.0
                                "PARTIALLY_PAID" -> {
                                    val pAmt = paidAmountText.toDoubleOrNull()
                                    if (pAmt == null || pAmt <= 0) {
                                        errorMessage = "Please enter paid amount greater than 0 for partial payment"
                                        return@Button
                                    }
                                    if (pAmt > billAmount) {
                                        errorMessage = "Paid amount cannot exceed total bill amount (${FormatUtils.formatInr(billAmount)})"
                                        return@Button
                                    }
                                    pAmt
                                }
                                else -> billAmount
                            }
                            val balanceAmount = (billAmount - paidAmount).coerceAtLeast(0.0)

                            if (vendorName.isBlank()) {
                                errorMessage = "Please enter the vendor or party name"
                                return@Button
                            }
                            onSave(
                                selectedTask?.id,
                                selectedTask?.title ?: "General Site Expense",
                                selectedCategory,
                                billAmount,
                                dateText.ifBlank { LocalDate.now().format(DateTimeFormatter.ISO_DATE) },
                                vendorName,
                                billNumber.ifBlank { "VOUCHER-${(100..999).random()}" },
                                selectedExpenseType,
                                selectedPaymentMode,
                                notesText,
                                billPhotoUri,
                                paymentPhotoUri,
                                expensePhotoUri,
                                billAmount,
                                paidAmount,
                                balanceAmount,
                                paymentStatus
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Full Screen Photo Preview Modal
    if (fullPhotoPreview != null) {
        Dialog(onDismissRequest = { fullPhotoPreview = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fullPhotoPreview!!.second,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { fullPhotoPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close preview")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = fullPhotoPreview!!.first,
                            contentDescription = fullPhotoPreview!!.second,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }

    // Modal Task Picker displaying all tasks with live search and category filters
    if (showTaskPickerModal) {
        TaskPickerModalDialog(
            tasks = tasks,
            currentSelectedTask = selectedTask,
            onDismiss = { showTaskPickerModal = false },
            onTaskSelected = { task ->
                selectedTask = task
                selectedCategory = task.category
                showTaskPickerModal = false
            }
        )
    }
}

/**
 * Reusable Card component for individual photo uploads (Bill, Payment, Site/Expense Type).
 */
@Composable
fun ExpensePhotoUploadCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    photoUri: String?,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    testTagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (photoUri != null) 1.5.dp else 1.dp,
            color = if (photoUri != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (photoUri != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (photoUri != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (photoUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (photoUri != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Attached",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (photoUri != null) {
                // Attached Photo Display Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { onViewPhoto() }
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = onViewPhoto,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("${testTagPrefix}_view_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Photo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.testTag("${testTagPrefix}_remove_btn")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove Photo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Upload Buttons Row: Camera + Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTakePhoto,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_camera_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onPickGallery,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_gallery_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Modal Dialog for selecting from all contract tasks with search and categories.
 */
@Composable
fun TaskPickerModalDialog(
    tasks: List<WorkOrderTask>,
    currentSelectedTask: WorkOrderTask?,
    onDismiss: () -> Unit,
    onTaskSelected: (WorkOrderTask) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = remember(tasks) {
        listOf("All") + tasks.map { it.category }.distinct()
    }

    val filteredTasks = remember(tasks, searchQuery, selectedCategoryFilter) {
        tasks.filter { task ->
            val matchesCategory = selectedCategoryFilter == "All" || task.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    task.title.contains(searchQuery, ignoreCase = true) ||
                    task.itemNumber.contains(searchQuery, ignoreCase = true) ||
                    task.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Work Order Task",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Showing ${filteredTasks.size} of ${tasks.size} contract BOQ tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search task by title, item #, category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val count = if (cat == "All") tasks.size else tasks.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text("$cat ($count)") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tasks List
                if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tasks found matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            val isSelected = currentSelectedTask?.id == task.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskSelected(task) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onTaskSelected(task) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = task.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Scope: ${task.totalQty} ${task.uom} • Budget: ${FormatUtils.formatInr(task.totalBudget)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Helper function to persist gallery URI into internal app files directory.
 */
private fun saveUriToFile(context: Context, uri: Uri, prefix: String = "photo"): String {
    return try {
        val photosDir = File(context.filesDir, "expense_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val destFile = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.toURI().toString()
    } catch (e: Exception) {
        uri.toString()
    }
}

/**
 * Helper function to persist camera Bitmap into internal app files directory.
 */
private fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "photo"): String {
    return try {
        val photosDir = File(context.filesDir, "expense_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val destFile = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        destFile.toURI().toString()
    } catch (e: Exception) {
        ""
    }
}

package com.example.ui.dashboard

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FsExpense
import com.example.data.firebase.FsProject
import com.example.data.firebase.FsTask
import com.example.data.firebase.FsWorkOrder
import com.example.ui.components.FormatUtils
import com.example.ui.navigation.SolarScreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    projects: List<FsProject>,
    workOrders: List<FsWorkOrder>,
    selectedWorkOrderId: String?,
    tasks: List<FsTask>,
    allTasks: List<FsTask> = emptyList(),
    expenses: List<FsExpense>,
    portfolioTotalBudget: Double,
    portfolioTotalSpent: Double,
    onSelectWorkOrder: (String?) -> Unit,
    onOpenUploadWorkOrder: () -> Unit,
    onNavigate: (SolarScreen) -> Unit,
    onOpenAddExpense: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    val remainingBudget = (portfolioTotalBudget - portfolioTotalSpent).coerceAtLeast(0.0)
    val spendPercentage = if (portfolioTotalBudget > 0) (portfolioTotalSpent / portfolioTotalBudget) * 100.0 else 0.0

    val selectedWorkOrder = workOrders.find { it.id == selectedWorkOrderId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Project or Overview KPI Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portfolio_kpi_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedWorkOrder != null) selectedWorkOrder.projectName else "All Active Projects Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedWorkOrder != null) "WO: ${selectedWorkOrder.workOrderNumber}" else "Across ${workOrders.size} Sites • Karnataka",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (spendPercentage < 80) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (spendPercentage < 80) "HEALTHY MARGIN" else "HIGH UTILIZATION",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (spendPercentage < 80) Color(0xFF047857) else Color(0xFFB91C1C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (selectedWorkOrder != null) "Allocated Tasks Budget" else "Portfolio Allocated Budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(portfolioTotalBudget),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val activeTaskCount = if (selectedWorkOrder != null) tasks.size else (if (allTasks.isNotEmpty()) allTasks.size else tasks.size)
                            Text(
                                text = "Sum of $activeTaskCount Task Budgets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Incurred Spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(portfolioTotalSpent),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", spendPercentage)}% of Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (spendPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining Balance: ${FormatUtils.formatInr(remainingBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        if (selectedWorkOrder != null) {
                            Text(
                                text = "Duration: ${selectedWorkOrder.durationDays} Days",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Hub
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenAddExpense,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_log_expense_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Expense", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenUploadWorkOrder,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dashboard_upload_wo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Work Order", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Projects & Work Orders List: "Each work order is considered as different project"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Projects (Work Orders)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+ Upload New",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenUploadWorkOrder() }
                )
            }
        }

        // Cards for each work order (project)
        items(workOrders) { wo ->
            val tasksSource = if (allTasks.isNotEmpty()) allTasks else tasks
            val woTasks = tasksSource.filter { it.workOrderId == wo.id || (wo.projectId.isNotBlank() && it.projectId == wo.projectId) }
            val woTaskAllocatedBudget = woTasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
            val effectiveWoBudget = if (woTaskAllocatedBudget > 0.0) woTaskAllocatedBudget else wo.totalBudget
            val woExpenses = expenses.filter { it.workOrderId == wo.id || (wo.projectId.isNotBlank() && it.projectId == wo.projectId) }
            val woSpent = woExpenses.sumOf { it.amount }
            val woRemaining = (effectiveWoBudget - woSpent).coerceAtLeast(0.0)
            val woPct = if (effectiveWoBudget > 0) (woSpent / effectiveWoBudget) * 100.0 else 0.0
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("project_card_${wo.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SolarPower,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = wo.projectName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Client: ${wo.clientName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "WO #: ${wo.workOrderNumber} • Duration: ${wo.durationDays} Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (woPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Allocated Budget: ${FormatUtils.formatInr(effectiveWoBudget)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Spent: ${FormatUtils.formatInr(woSpent)} (${String.format(java.util.Locale.US, "%.1f", woPct)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Remaining: ${FormatUtils.formatLakhs(woRemaining)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Separate Card: To Pay and Balance Payments Summary
        item {
            val toPayExpenses = expenses.filter { it.paymentStatus == "TO_PAY" }
            val partialExpenses = expenses.filter { it.paymentStatus == "PARTIALLY_PAID" }
            val paidExpenses = expenses.filter { it.paymentStatus == "PAID" || it.paymentStatus.isBlank() }

            val totalToPayAmt = toPayExpenses.sumOf { it.effectiveBillAmount }
            val totalPartialBal = partialExpenses.sumOf { it.effectiveBalanceAmount }
            val totalOutstandingBalance = totalToPayAmt + totalPartialBal
            val totalClearedPaid = expenses.sumOf { it.effectivePaidAmount }
            val totalExpenseBilling = expenses.sumOf { it.effectiveBillAmount }
            val paidRatio = if (totalExpenseBilling > 0) (totalClearedPaid / totalExpenseBilling).toFloat() else 0f

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overview_balance_payments_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "To Pay & Balance Payments",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Vendor & Site Expense Liabilities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (totalOutstandingBalance > 0) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (totalOutstandingBalance > 0) "${toPayExpenses.size + partialExpenses.size} Pending" else "All Cleared",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalOutstandingBalance > 0) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Outstanding Balance Hero Container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL BALANCE OUTSTANDING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(totalOutstandingBalance),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Paid Cleared",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(totalClearedPaid),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment Settlement Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment Settlement",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(paidRatio * 100).toInt()}% Cleared",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { paidRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFFDE68A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 KPI Pillars: To Pay | Partial Balance | Paid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. To Pay Pillar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "To Pay",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(totalToPayAmt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "${toPayExpenses.size} Unpaid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 2. Partial Balance Pillar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Partial Bal.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(totalPartialBal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    text = "${partialExpenses.size} Partial",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3. Fully Paid Pillar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Paid",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = FormatUtils.formatInr(totalClearedPaid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = "${paidExpenses.size} Cleared",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Expenses Section Header with Filter
        item {
            val toPayCount = expenses.count { it.paymentStatus == "TO_PAY" }
            val partialCount = expenses.count { it.paymentStatus == "PARTIALLY_PAID" }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Site Expenses (${expenses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Showing payment status & outstanding balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate(SolarScreen.EXPENSES) }
                    )
                }

                if (toPayCount > 0 || partialCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Action Required: $toPayCount To-Pay and $partialCount Partial payments pending settlement",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        items(expenses.take(6)) { expense ->
            val isToPay = expense.paymentStatus == "TO_PAY"
            val isPartial = expense.paymentStatus == "PARTIALLY_PAID"
            val isPaid = expense.paymentStatus == "PAID" || (!isToPay && !isPartial)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_expense_item_${expense.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = expense.vendorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                // Payment Status Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        isToPay -> Color(0xFFFEF3C7)
                                        isPartial -> Color(0xFFDBEAFE)
                                        else -> Color(0xFFD1FAE5)
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            isToPay -> "TO PAY"
                                            isPartial -> "PARTIAL"
                                            else -> "PAID"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isToPay -> Color(0xFFB45309)
                                            isPartial -> Color(0xFF1D4ED8)
                                            else -> Color(0xFF047857)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${expense.taskTitle} • ${expense.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${expense.expenseType} • ${expense.paymentMode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatInr(expense.effectiveBillAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onDeleteExpense(expense.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Display To Pay and Balance Payments info for this expense
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isToPay -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                            isPartial -> Color(0xFF3B82F6).copy(alpha = 0.08f)
                            else -> Color(0xFF10B981).copy(alpha = 0.08f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Paid: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(expense.effectivePaidAmount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (expense.effectivePaidAmount > 0) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Balance: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatInr(expense.effectiveBalanceAmount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (expense.effectiveBalanceAmount > 0) Color(0xFFB45309) else Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

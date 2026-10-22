package com.example.ui.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PreloadedData
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudgetSummary
import com.example.data.model.TaskWithExpenseSummary
import com.example.ui.components.FormatUtils

@Composable
fun MonthlyReportsScreen(
    currentReport: MonthlyBudgetSummary?,
    availableMonths: List<String>,
    selectedMonth: String,
    allExpenses: List<Expense>,
    tasksWithSummary: List<TaskWithExpenseSummary>,
    onSelectMonth: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val monthExpenses = allExpenses.filter { it.monthYear == selectedMonth }
    val displayMonth = currentReport?.displayMonth ?: selectedMonth
    val monthlyAllocation = currentReport?.monthlyAllocatedBudget ?: 0.0
    val monthlySpent = currentReport?.totalSpent ?: 0.0
    val variance = monthlyAllocation - monthlySpent
    val isUnderBudget = variance >= 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Tabs
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Select Execution Month",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableMonths.forEach { m ->
                        val label = when (m) {
                            "2026-08" -> "August 2026"
                            "2026-09" -> "September 2026"
                            "2026-10" -> "October 2026"
                            "2026-11" -> "November 2026"
                            else -> m
                        }
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { onSelectMonth(m) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("month_chip_$m")
                        )
                    }
                }
            }
        }

        // Monthly KPI Summary Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardColors().run { CardDefaults.cardElevation(defaultElevation = 3.dp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_kpi_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Budget Performance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayMonth,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isUnderBudget) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isUnderBudget) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isUnderBudget) Color(0xFF047857) else Color(0xFFB91C1C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isUnderBudget) "UNDER BUDGET" else "OVER BUDGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnderBudget) Color(0xFF047857) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Month Actual Spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(monthlySpent),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${monthExpenses.size} Vouchers / Invoices",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Monthly Allocation Target",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatInr(monthlyAllocation),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Variance: ${if (isUnderBudget) "+ " else "- "}${FormatUtils.formatInr(kotlin.math.abs(variance))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnderBudget) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val utilPercent = when {
                        monthlyAllocation <= 0.0 && monthlySpent > 0.0 -> 1f
                        monthlyAllocation <= 0.0 -> 0f
                        else -> (monthlySpent / monthlyAllocation).toFloat().coerceIn(0f, 1f)
                    }
                    LinearProgressIndicator(
                        progress = { utilPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (monthlyAllocation > 0.0 && monthlySpent > monthlyAllocation) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Category-wise Monthly Spend Breakdown
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_category_breakdown_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Monthly Spend by Task Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val breakdown = currentReport?.categoryBreakdown ?: emptyMap()
                    if (breakdown.isEmpty()) {
                        Text(
                            text = "No expenses recorded for $displayMonth yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        breakdown.forEach { (category, amount) ->
                            val catColor = FormatUtils.getCategoryColor(category)
                            val sharePercent = if (monthlySpent > 0) (amount / monthlySpent) * 100.0 else 0.0

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "${FormatUtils.formatInr(amount)} (${String.format(java.util.Locale.US, "%.1f", sharePercent)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (sharePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Detailed Expense Vouchers List
        item {
            Text(
                text = "Vouchers Logged in $displayMonth (${monthExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (monthExpenses.isEmpty()) {
            item {
                Text(
                    text = "No expenses logged for this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(monthExpenses) { exp ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.vendorName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${exp.taskTitle} • ${exp.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (exp.notes.isNotBlank()) {
                                Text(
                                    text = exp.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatInr(exp.amount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = exp.expenseType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Export & Share Report Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_report_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Official Monthly Financial Statement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate and share summary for client M/s Max Infra & Aprico accounts:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val reportText = buildReportText(
                        displayMonth = displayMonth,
                        monthlySpent = monthlySpent,
                        monthlyAllocation = monthlyAllocation,
                        variance = variance,
                        expenses = monthExpenses
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(reportText))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Report")
                        }

                        Button(
                            onClick = {
                                shareReport(context, reportText, displayMonth)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Statement")
                        }
                    }
                }
            }
        }
    }
}

private fun buildReportText(
    displayMonth: String,
    monthlySpent: Double,
    monthlyAllocation: Double,
    variance: Double,
    expenses: List<Expense>
): String {
    val sb = StringBuilder()
    sb.appendLine("=========================================")
    sb.appendLine("KUSUM SOLAR 1.88 MW - MONTHLY SITE EXPENSE REPORT")
    sb.appendLine("Work Order: ${PreloadedData.WORK_ORDER_NO}")
    sb.appendLine("Client: ${PreloadedData.CLIENT_NAME}")
    sb.appendLine("Contractor: ${PreloadedData.CONTRACTOR_NAME}")
    sb.appendLine("Reporting Month: $displayMonth")
    sb.appendLine("=========================================")
    val totalContract = if (monthlyAllocation > 0.0) monthlyAllocation * 3.0 else 0.0
    sb.appendLine("Total Contract Value: ${FormatUtils.formatInr(totalContract)}")
    sb.appendLine("Monthly Target Allocation: ${FormatUtils.formatInr(monthlyAllocation)}")
    sb.appendLine("Actual Incurred Spend: ${FormatUtils.formatInr(monthlySpent)}")
    sb.appendLine("Monthly Variance: ${if (variance >= 0) "Under Budget by " else "Over Budget by "}${FormatUtils.formatInr(kotlin.math.abs(variance))}")
    sb.appendLine("Total Invoices / Vouchers: ${expenses.size}")
    sb.appendLine("-----------------------------------------")
    sb.appendLine("EXPENSE BREAKDOWN:")
    expenses.forEachIndexed { i, exp ->
        sb.appendLine("${i + 1}. [${exp.date}] ${exp.vendorName} - ${exp.taskTitle}: ${FormatUtils.formatInr(exp.amount)} (${exp.expenseType}, ${exp.paymentMode})")
    }
    sb.appendLine("=========================================")
    sb.appendLine("Generated by APRICO CLOUD Mobile Manager")
    return sb.toString()
}

private fun shareReport(context: Context, text: String, month: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Solar Work Order Expense Statement - $month")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Expense Statement"))
}

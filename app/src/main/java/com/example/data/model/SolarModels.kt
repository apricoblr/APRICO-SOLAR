package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an official item/task from the Max Infra Work Order.
 */
@Entity(tableName = "work_order_tasks")
data class WorkOrderTask(
    @PrimaryKey val id: String,
    val itemNumber: String,
    val category: String,
    val title: String,
    val description: String,
    val uom: String, // Unit of Measurement: Nos, MT, Mtr, Sets, LOT, NO
    val totalQty: Double,
    val unitPrice: Double,
    val totalBudget: Double, // totalQty * unitPrice
    val completedQty: Double = 0.0,
    val status: String = "IN_PROGRESS" // NOT_STARTED, IN_PROGRESS, COMPLETED
) {
    val effectiveBudget: Double
        get() = if (totalBudget > 0.0) totalBudget else totalQty * unitPrice
}

/**
 * An individual expense logged against a task or general site overhead.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String?, // Linked WorkOrderTask id, nullable for general site expense
    val taskTitle: String,
    val category: String,
    val amount: Double,
    val billAmount: Double = amount,
    val paidAmount: Double = amount,
    val balanceAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // PAID, TO_PAY, PARTIALLY_PAID
    val date: String, // YYYY-MM-DD
    val monthYear: String, // YYYY-MM
    val vendorName: String,
    val billNumber: String,
    val expenseType: String, // Labor, Material, Machinery/Fuel, Contractor, Transport, Misc
    val paymentMode: String, // Bank Transfer, UPI, Cash, Cheque
    val notes: String = "",
    val receiptImageUri: String? = null,
    val paymentPhotoUri: String? = null,
    val expensePhotoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Summary representation for budget vs actual.
 */
data class TaskWithExpenseSummary(
    val task: WorkOrderTask,
    val totalSpent: Double,
    val expenseCount: Int,
    val remainingBudget: Double = task.totalBudget - totalSpent,
    val percentageUsed: Double = if (task.totalBudget > 0) (totalSpent / task.totalBudget) * 100.0 else 0.0
)

/**
 * Monthly budget report data.
 */
data class MonthlyBudgetSummary(
    val monthYear: String, // e.g. "2026-08", "2026-09"
    val displayMonth: String, // e.g. "August 2026"
    val totalSpent: Double,
    val monthlyAllocatedBudget: Double,
    val expenseCount: Int,
    val categoryBreakdown: Map<String, Double>
)

/**
 * Extracted receipt data from the Automated Receipt Scanner.
 */
data class ScannedReceiptData(
    val vendor: String,
    val date: String,
    val billNumber: String,
    val totalAmount: Double,
    val taxAmount: Double,
    val suggestedCategory: String,
    val suggestedTaskId: String?,
    val lineItemsSummary: String,
    val confidenceScore: Float = 0.95f
)

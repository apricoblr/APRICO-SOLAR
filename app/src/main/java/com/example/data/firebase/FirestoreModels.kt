package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FsProject(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val capacity: String = "",
    val adminEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsWorkOrder(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val workOrderNumber: String = "",
    val clientName: String = "",
    val contractorName: String = "",
    val baseBudget: Double = 0.0,
    val gstPercentage: Double = 18.0,
    val gstAmount: Double = 0.0,
    val totalBudget: Double = 0.0,
    val durationDays: Int = 45,
    val startDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, ON_HOLD
    val taskCount: Int = 0,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsTask(
    @DocumentId val id: String = "",
    val workOrderId: String = "",
    val projectId: String = "",
    val itemNumber: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val uom: String = "",
    val totalQty: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalBudget: Double = 0.0,
    val completedQty: Double = 0.0,
    val status: String = "IN_PROGRESS", // NOT_STARTED, IN_PROGRESS, COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val effectiveBudget: Double
        get() = if (totalBudget > 0.0) totalBudget else totalQty * unitPrice
}

@IgnoreExtraProperties
data class FsExpense(
    @DocumentId val id: String = "",
    val workOrderId: String = "",
    val projectId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val billAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balanceAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // PAID, TO_PAY, PARTIALLY_PAID
    val date: String = "",
    val monthYear: String = "",
    val vendorName: String = "",
    val billNumber: String = "",
    val expenseType: String = "",
    val paymentMode: String = "",
    val notes: String = "",
    val receiptImageUri: String? = null,
    val paymentPhotoUri: String? = null,
    val expensePhotoUri: String? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val effectiveBillAmount: Double
        get() = if (billAmount > 0.0) billAmount else amount

    val effectivePaidAmount: Double
        get() = when (paymentStatus) {
            "TO_PAY" -> 0.0
            "PARTIALLY_PAID" -> paidAmount
            else -> if (paidAmount > 0.0) paidAmount else effectiveBillAmount
        }

    val effectiveBalanceAmount: Double
        get() = when (paymentStatus) {
            "TO_PAY" -> effectiveBillAmount
            "PARTIALLY_PAID" -> (effectiveBillAmount - effectivePaidAmount).coerceAtLeast(0.0)
            else -> balanceAmount.coerceAtLeast(0.0)
        }
}

@IgnoreExtraProperties
data class FsVendor(
    @DocumentId val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val contactPerson: String = "",
    val category: String = "General",
    val address: String = "",
    val notes: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FsExpenseType(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Direct Scope",
    val isDefault: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

/**
 * Aggregated summary for a Work Order or Project.
 */
data class ProjectPortfolioSummary(
    val project: FsProject,
    val workOrders: List<FsWorkOrder>,
    val totalBudget: Double,
    val totalSpent: Double,
    val remainingBudget: Double,
    val completionPercentage: Double
)

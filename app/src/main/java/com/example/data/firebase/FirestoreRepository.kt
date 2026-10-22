package com.example.data.firebase

import android.util.Log
import com.example.data.local.PreloadedData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreRepository"

class FirestoreRepository(
    private val db: FirebaseFirestore
) {

    fun observeProjects(): Flow<List<FsProject>> {
        return db.collection("projects")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FsProject::class.java)?.copy(id = doc.id)
                }
            }
    }

    fun observeWorkOrders(): Flow<List<FsWorkOrder>> {
        return db.collection("work_orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FsWorkOrder::class.java)?.copy(id = doc.id)
                }
            }
    }

    fun observeTasks(workOrderId: String? = null): Flow<List<FsTask>> {
        val query = if (workOrderId != null && workOrderId.isNotBlank()) {
            db.collection("tasks").whereEqualTo("workOrderId", workOrderId)
        } else {
            db.collection("tasks")
        }
        return query.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                parseFsTask(doc)
            }.sortedWith(compareBy({ it.category }, { it.itemNumber }))
        }
    }

    fun observeExpenses(workOrderId: String? = null): Flow<List<FsExpense>> {
        val query = if (workOrderId != null && workOrderId.isNotBlank()) {
            db.collection("expenses").whereEqualTo("workOrderId", workOrderId)
        } else {
            db.collection("expenses")
        }
        return query.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                parseFsExpense(doc)
            }.sortedByDescending { it.date }
        }
    }

    private fun parseFsTask(doc: com.google.firebase.firestore.DocumentSnapshot): FsTask? {
        return try {
            doc.toObject(FsTask::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            try {
                val totalQty = doc.getDouble("totalQty") ?: doc.getLong("totalQty")?.toDouble() ?: 0.0
                val unitPrice = doc.getDouble("unitPrice") ?: doc.getLong("unitPrice")?.toDouble() ?: 0.0
                val totalBudget = doc.getDouble("totalBudget") ?: doc.getLong("totalBudget")?.toDouble() ?: 0.0
                val completedQty = doc.getDouble("completedQty") ?: doc.getLong("completedQty")?.toDouble() ?: 0.0
                FsTask(
                    id = doc.id,
                    workOrderId = doc.getString("workOrderId") ?: "",
                    projectId = doc.getString("projectId") ?: "",
                    itemNumber = doc.getString("itemNumber") ?: "",
                    category = doc.getString("category") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    uom = doc.getString("uom") ?: "",
                    totalQty = totalQty,
                    unitPrice = unitPrice,
                    totalBudget = totalBudget,
                    completedQty = completedQty,
                    status = doc.getString("status") ?: "NOT_STARTED",
                    createdAt = doc.getTimestamp("createdAt")
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Error parsing task doc ${doc.id}", e2)
                null
            }
        }
    }

    private fun parseFsExpense(doc: com.google.firebase.firestore.DocumentSnapshot): FsExpense? {
        return try {
            doc.toObject(FsExpense::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            try {
                val amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0
                val billAmount = doc.getDouble("billAmount") ?: doc.getLong("billAmount")?.toDouble() ?: amount
                val paymentStatus = doc.getString("paymentStatus") ?: "PAID"
                val paidAmount = doc.getDouble("paidAmount") ?: doc.getLong("paidAmount")?.toDouble() ?: (if (paymentStatus == "TO_PAY") 0.0 else billAmount)
                val balanceAmount = doc.getDouble("balanceAmount") ?: doc.getLong("balanceAmount")?.toDouble() ?: (billAmount - paidAmount).coerceAtLeast(0.0)
                FsExpense(
                    id = doc.id,
                    workOrderId = doc.getString("workOrderId") ?: "",
                    projectId = doc.getString("projectId") ?: "",
                    taskId = doc.getString("taskId") ?: "",
                    taskTitle = doc.getString("taskTitle") ?: "",
                    category = doc.getString("category") ?: "",
                    amount = amount,
                    billAmount = billAmount,
                    paidAmount = paidAmount,
                    balanceAmount = balanceAmount,
                    paymentStatus = paymentStatus,
                    date = doc.getString("date") ?: "",
                    monthYear = doc.getString("monthYear") ?: "",
                    vendorName = doc.getString("vendorName") ?: "",
                    billNumber = doc.getString("billNumber") ?: "",
                    expenseType = doc.getString("expenseType") ?: "",
                    paymentMode = doc.getString("paymentMode") ?: "",
                    notes = doc.getString("notes") ?: "",
                    receiptImageUri = doc.getString("receiptImageUri"),
                    paymentPhotoUri = doc.getString("paymentPhotoUri"),
                    expensePhotoUri = doc.getString("expensePhotoUri"),
                    createdBy = doc.getString("createdBy") ?: "",
                    createdAt = doc.getTimestamp("createdAt")
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Error parsing expense doc ${doc.id}", e2)
                null
            }
        }
    }

    suspend fun createProject(name: String, location: String, capacity: String, adminEmail: String): String {
        val projectRef = db.collection("projects").document()
        val project = FsProject(
            id = projectRef.id,
            name = name,
            location = location,
            capacity = capacity,
            adminEmail = adminEmail
        )
        projectRef.set(project).await()
        return projectRef.id
    }

    suspend fun createWorkOrder(
        projectId: String,
        projectName: String,
        workOrderNumber: String,
        clientName: String,
        contractorName: String,
        baseBudget: Double,
        durationDays: Int,
        startDate: String,
        tasks: List<FsTask>,
        createdBy: String
    ): String {
        val woRef = db.collection("work_orders").document()
        val tasksAllocated = tasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
        val effectiveBudget = if (tasksAllocated > 0.0) tasksAllocated else baseBudget

        val workOrder = FsWorkOrder(
            id = woRef.id,
            projectId = projectId,
            projectName = projectName,
            workOrderNumber = workOrderNumber,
            clientName = clientName,
            contractorName = contractorName,
            baseBudget = effectiveBudget,
            gstPercentage = 0.0,
            gstAmount = 0.0,
            totalBudget = effectiveBudget,
            durationDays = durationDays,
            startDate = startDate,
            status = "ACTIVE",
            taskCount = tasks.size,
            createdBy = createdBy
        )

        // Write work order
        woRef.set(workOrder).await()

        // Batch write tasks
        if (tasks.isNotEmpty()) {
            val batch = db.batch()
            for (task in tasks) {
                val taskRef = db.collection("tasks").document()
                val calculatedBudget = if (task.totalBudget > 0.0) task.totalBudget else (task.totalQty * task.unitPrice)
                val taskToSave = task.copy(
                    id = taskRef.id,
                    workOrderId = woRef.id,
                    projectId = projectId,
                    totalBudget = calculatedBudget
                )
                batch.set(taskRef, taskToSave)
            }
            batch.commit().await()
            syncWorkOrderBudgetFromTasks(woRef.id)
        }

        return woRef.id
    }

    suspend fun addExpense(expense: FsExpense): String {
        val expRef = db.collection("expenses").document()
        val toSave = expense.copy(id = expRef.id)
        expRef.set(toSave).await()
        return expRef.id
    }

    suspend fun deleteExpense(expenseId: String) {
        db.collection("expenses").document(expenseId).delete().await()
    }

    suspend fun updateTaskProgress(taskId: String, completedQty: Double, status: String) {
        if (taskId.isBlank()) {
            Log.w(TAG, "updateTaskProgress called with blank taskId")
            return
        }

        val updates = mapOf<String, Any>(
            "completedQty" to completedQty,
            "status" to status
        )
        Log.d(TAG, "Updating task progress: taskId=$taskId, completedQty=$completedQty, status=$status")

        // 1. Candidate document IDs to try directly with update()
        val candidateDocIds = linkedSetOf(
            taskId,
            if (taskId.startsWith("task_")) taskId.removePrefix("task_") else "task_${taskId.lowercase()}",
            taskId.lowercase(),
            taskId.uppercase()
        )

        var directSuccess = false
        for (candidateId in candidateDocIds) {
            try {
                db.collection("tasks").document(candidateId).update(updates).await()
                Log.d(TAG, "Direct update succeeded on doc '$candidateId' for task $taskId")
                directSuccess = true
                break
            } catch (e: Exception) {
                Log.d(TAG, "Direct candidate '$candidateId' update not applied: ${e.message}")
            }
        }

        if (directSuccess) return

        // 2. Query fallback: find by field 'id'
        try {
            val byId = db.collection("tasks").whereEqualTo("id", taskId).limit(1).get().await()
            if (!byId.isEmpty) {
                byId.documents.first().reference.update(updates).await()
                Log.d(TAG, "Query update by field 'id' succeeded for task $taskId")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Query by id fallback error: ${e.message}")
        }

        // 3. Query fallback: find by field 'itemNumber'
        try {
            val byItem = db.collection("tasks").whereEqualTo("itemNumber", taskId).limit(1).get().await()
            if (!byItem.isEmpty) {
                byItem.documents.first().reference.update(updates).await()
                Log.d(TAG, "Query update by field 'itemNumber' succeeded for task $taskId")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Query by itemNumber fallback error: ${e.message}")
        }

        // 4. Final guaranteed fallback: merge so document is always updated / preserved
        try {
            db.collection("tasks").document(taskId).set(
                mapOf(
                    "id" to taskId,
                    "completedQty" to completedQty,
                    "status" to status
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            Log.d(TAG, "Merged task progress fallback into doc '$taskId'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist task progress for $taskId", e)
            throw e
        }
    }

    suspend fun syncWorkOrderBudgetFromTasks(workOrderId: String) {
        if (workOrderId.isBlank()) return
        try {
            var tasksSnapshot = db.collection("tasks")
                .whereEqualTo("workOrderId", workOrderId)
                .get()
                .await()
            if (tasksSnapshot.isEmpty) {
                val woDoc = db.collection("work_orders").document(workOrderId).get().await()
                val projectId = woDoc.getString("projectId") ?: ""
                if (projectId.isNotBlank()) {
                    val projTasksSnapshot = db.collection("tasks")
                        .whereEqualTo("projectId", projectId)
                        .get()
                        .await()
                    if (!projTasksSnapshot.isEmpty) {
                        tasksSnapshot = projTasksSnapshot
                    }
                }
            }
            val totalAllocated = tasksSnapshot.documents.sumOf { doc ->
                val tb = doc.getDouble("totalBudget") ?: doc.getLong("totalBudget")?.toDouble() ?: 0.0
                if (tb > 0.0) {
                    tb
                } else {
                    val qty = doc.getDouble("totalQty") ?: doc.getLong("totalQty")?.toDouble() ?: 0.0
                    val up = doc.getDouble("unitPrice") ?: doc.getLong("unitPrice")?.toDouble() ?: 0.0
                    qty * up
                }
            }
            if (totalAllocated > 0.0) {
                db.collection("work_orders").document(workOrderId)
                    .update(
                        mapOf(
                            "totalBudget" to totalAllocated,
                            "baseBudget" to totalAllocated,
                            "taskCount" to tasksSnapshot.size()
                        )
                    ).await()
            }
            Log.d(TAG, "Synced work order $workOrderId totalBudget to $totalAllocated across ${tasksSnapshot.size()} tasks")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync work order budget from tasks for $workOrderId", e)
        }
    }

    suspend fun addTask(task: FsTask): String {
        val taskRef = if (task.id.isNotBlank()) db.collection("tasks").document(task.id) else db.collection("tasks").document()
        val calculatedBudget = if (task.totalBudget > 0.0) task.totalBudget else (task.totalQty * task.unitPrice)
        val toSave = task.copy(id = taskRef.id, totalBudget = calculatedBudget)
        taskRef.set(toSave).await()
        if (task.workOrderId.isNotBlank()) {
            syncWorkOrderBudgetFromTasks(task.workOrderId)
        }
        return taskRef.id
    }

    suspend fun updateTask(task: FsTask) {
        if (task.id.isBlank()) return
        val calculatedBudget = if (task.totalBudget > 0.0) task.totalBudget else (task.totalQty * task.unitPrice)
        val updates = mapOf(
            "itemNumber" to task.itemNumber,
            "category" to task.category,
            "title" to task.title,
            "description" to task.description,
            "uom" to task.uom,
            "totalQty" to task.totalQty,
            "unitPrice" to task.unitPrice,
            "totalBudget" to calculatedBudget,
            "completedQty" to task.completedQty,
            "status" to task.status
        )
        db.collection("tasks").document(task.id).update(updates).await()
        if (task.workOrderId.isNotBlank()) {
            syncWorkOrderBudgetFromTasks(task.workOrderId)
        }
    }

    suspend fun deleteTask(taskId: String, workOrderId: String = "") {
        if (taskId.isBlank()) return
        db.collection("tasks").document(taskId).delete().await()
        if (workOrderId.isNotBlank()) {
            syncWorkOrderBudgetFromTasks(workOrderId)
        }
    }

    suspend fun deleteWorkOrder(workOrderId: String, projectId: String = "") {
        if (workOrderId.isBlank()) return
        try {
            // Delete related tasks
            val tasksSnapshot = db.collection("tasks").whereEqualTo("workOrderId", workOrderId).get().await()
            val batch = db.batch()
            for (doc in tasksSnapshot.documents) {
                batch.delete(doc.reference)
            }
            // Delete related expenses
            val expensesSnapshot = db.collection("expenses").whereEqualTo("workOrderId", workOrderId).get().await()
            for (doc in expensesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            // Delete work order document
            batch.delete(db.collection("work_orders").document(workOrderId))

            // Delete project doc if no other work order uses this projectId
            if (projectId.isNotBlank()) {
                val otherWoSnapshot = db.collection("work_orders").whereEqualTo("projectId", projectId).get().await()
                val remaining = otherWoSnapshot.documents.filter { it.id != workOrderId }
                if (remaining.isEmpty()) {
                    batch.delete(db.collection("projects").document(projectId))
                }
            }
            batch.commit().await()
            Log.d(TAG, "Successfully deleted work order $workOrderId and its related data")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting work order $workOrderId", e)
            throw e
        }
    }

    suspend fun updateProjectName(workOrderId: String, projectId: String, newProjectName: String) {
        val trimmedName = newProjectName.trim()
        if (trimmedName.isBlank()) return
        try {
            val batch = db.batch()
            if (workOrderId.isNotBlank()) {
                val woRef = db.collection("work_orders").document(workOrderId)
                batch.update(woRef, "projectName", trimmedName)
            }
            if (projectId.isNotBlank()) {
                val projRef = db.collection("projects").document(projectId)
                batch.update(projRef, "name", trimmedName)

                val otherWos = db.collection("work_orders").whereEqualTo("projectId", projectId).get().await()
                for (doc in otherWos.documents) {
                    if (doc.id != workOrderId) {
                        batch.update(doc.reference, "projectName", trimmedName)
                    }
                }
            }
            batch.commit().await()
            Log.d(TAG, "Successfully updated project name to '$trimmedName' for wo=$workOrderId, proj=$projectId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project name", e)
        }
    }
    // ==========================================
    // VENDORS / PARTIES MASTER REPOSITORY
    // ==========================================

    fun observeVendors(): Flow<List<FsVendor>> {
        return db.collection("vendors")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FsVendor::class.java)?.copy(id = doc.id)
                }.sortedBy { it.name.lowercase() }
            }
    }

    suspend fun addVendor(vendor: FsVendor): String {
        val docRef = if (vendor.id.isNotBlank()) db.collection("vendors").document(vendor.id) else db.collection("vendors").document()
        val toSave = vendor.copy(id = docRef.id)
        docRef.set(toSave).await()
        return docRef.id
    }

    suspend fun updateVendor(vendor: FsVendor) {
        if (vendor.id.isBlank()) return
        val updates = mapOf(
            "name" to vendor.name.trim(),
            "phoneNumber" to vendor.phoneNumber.trim(),
            "contactPerson" to vendor.contactPerson.trim(),
            "category" to vendor.category.trim(),
            "address" to vendor.address.trim(),
            "notes" to vendor.notes.trim()
        )
        db.collection("vendors").document(vendor.id).update(updates).await()
    }

    suspend fun deleteVendor(vendorId: String) {
        if (vendorId.isBlank()) return
        db.collection("vendors").document(vendorId).delete().await()
    }

    // ==========================================
    // EXPENSE TYPES MASTER REPOSITORY
    // ==========================================

    fun observeExpenseTypes(): Flow<List<FsExpenseType>> {
        return db.collection("expense_types")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FsExpenseType::class.java)?.copy(id = doc.id)
                }.sortedBy { it.name.lowercase() }
            }
    }

    suspend fun addExpenseType(expenseType: FsExpenseType): String {
        val docRef = if (expenseType.id.isNotBlank()) db.collection("expense_types").document(expenseType.id) else db.collection("expense_types").document()
        val toSave = expenseType.copy(id = docRef.id)
        docRef.set(toSave).await()
        return docRef.id
    }

    suspend fun updateExpenseType(expenseType: FsExpenseType) {
        if (expenseType.id.isBlank()) return
        val updates = mapOf(
            "name" to expenseType.name.trim(),
            "description" to expenseType.description.trim(),
            "category" to expenseType.category.trim()
        )
        db.collection("expense_types").document(expenseType.id).update(updates).await()
    }

    suspend fun deleteExpenseType(typeId: String) {
        if (typeId.isBlank()) return
        db.collection("expense_types").document(typeId).delete().await()
    }

    suspend fun seedInitialDataIfEmpty(adminEmail: String) {
        try {
            val snapshot = db.collection("work_orders").limit(1).get().await()
            if (snapshot.isEmpty) {
                Log.d(TAG, "Firestore is empty. Seeding initial work order and tasks...")

                // 1. Create Default Project
                val projectRef = db.collection("projects").document("proj_kusum_devarahipparagi")
                val project = FsProject(
                    id = projectRef.id,
                    name = PreloadedData.PROJECT_NAME,
                    location = PreloadedData.PROJECT_LOCATION,
                    capacity = "1.88 MW",
                    adminEmail = adminEmail
                )
                projectRef.set(project).await()

                // 2. Create Default Work Order
                val woRef = db.collection("work_orders").document("wo_maxinfra_0003")
                val allocatedTasksBudget = PreloadedData.INITIAL_TASKS.sumOf { it.totalBudget }
                val workOrder = FsWorkOrder(
                    id = woRef.id,
                    projectId = projectRef.id,
                    projectName = PreloadedData.PROJECT_NAME,
                    workOrderNumber = PreloadedData.WORK_ORDER_NO,
                    clientName = PreloadedData.CLIENT_NAME,
                    contractorName = PreloadedData.CONTRACTOR_NAME,
                    baseBudget = allocatedTasksBudget,
                    gstPercentage = 0.0,
                    gstAmount = 0.0,
                    totalBudget = allocatedTasksBudget,
                    durationDays = PreloadedData.CONTRACT_DURATION_DAYS,
                    startDate = PreloadedData.WORK_ORDER_DATE,
                    status = "ACTIVE",
                    taskCount = PreloadedData.INITIAL_TASKS.size,
                    createdBy = adminEmail
                )
                woRef.set(workOrder).await()

                // 3. Batch insert the 44 tasks
                val tasksList = PreloadedData.INITIAL_TASKS.map { task ->
                    FsTask(
                        id = "task_${task.id.lowercase()}",
                        workOrderId = woRef.id,
                        projectId = projectRef.id,
                        itemNumber = task.itemNumber,
                        category = task.category,
                        title = task.title,
                        description = task.description,
                        uom = task.uom,
                        totalQty = task.totalQty,
                        unitPrice = task.unitPrice,
                        totalBudget = task.totalBudget,
                        completedQty = task.completedQty,
                        status = task.status
                    )
                }

                // Split in batches of 25 (Firestore batch max is 500)
                tasksList.chunked(25).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { t ->
                        val doc = db.collection("tasks").document(t.id)
                        batch.set(doc, t)
                    }
                    batch.commit().await()
                }
                syncWorkOrderBudgetFromTasks(woRef.id)

                // 4. Batch insert initial expenses
                val expenseBatch = db.batch()
                PreloadedData.INITIAL_EXPENSES.forEachIndexed { i, exp ->
                    val expDoc = db.collection("expenses").document("exp_init_$i")
                    val matchingTaskId = "task_${(exp.taskId ?: "f01").lowercase()}"
                    val fsExp = FsExpense(
                        id = expDoc.id,
                        workOrderId = woRef.id,
                        projectId = projectRef.id,
                        taskId = matchingTaskId,
                        taskTitle = exp.taskTitle,
                        category = exp.category,
                        amount = exp.amount,
                        date = exp.date,
                        monthYear = exp.monthYear,
                        vendorName = exp.vendorName,
                        billNumber = exp.billNumber,
                        expenseType = exp.expenseType,
                        paymentMode = exp.paymentMode,
                        notes = exp.notes,
                        createdBy = adminEmail
                    )
                    expenseBatch.set(expDoc, fsExp)
                }
                expenseBatch.commit().await()

                Log.d(TAG, "Successfully seeded initial Work Order and 44 tasks into Firestore.")
            }

            // Seed Vendors if empty
            val vendorSnap = db.collection("vendors").limit(1).get().await()
            if (vendorSnap.isEmpty) {
                val vendorBatch = db.batch()
                val defaultVendors = listOf(
                    FsVendor(
                        id = "vendor_bhavani_dth",
                        name = "Bhavani DTH Drilling Rig Services",
                        phoneNumber = "+91 98450 12345",
                        contactPerson = "Bhavani Kumar",
                        category = "Machinery & Rig",
                        address = "Bijapur Highway, Devarahipparagi",
                        notes = "Specialized in DTH drilling for hard rock column posts and earthing pits.",
                        createdBy = adminEmail
                    ),
                    FsVendor(
                        id = "vendor_ultratech_rmc",
                        name = "UltraTech ReadyMix Concrete Co.",
                        phoneNumber = "+91 99001 88776",
                        contactPerson = "Ramesh Sharma",
                        category = "Material",
                        address = "Industrial Estate, Vijayapura",
                        notes = "Supplies M25/C25 grade ready mix concrete with boom pump.",
                        createdBy = adminEmail
                    ),
                    FsVendor(
                        id = "vendor_sri_sai_crane",
                        name = "Sri Sai Crane & Hydra Services",
                        phoneNumber = "+91 97405 55432",
                        contactPerson = "Venkat Reddy",
                        category = "Machinery & Crane",
                        address = "Sindagi Road, Devarahipparagi",
                        notes = "12T / 14T Farana and Hydra cranes for MMS unloading and module shifting.",
                        createdBy = adminEmail
                    ),
                    FsVendor(
                        id = "vendor_devara_labor",
                        name = "Devarahipparagi Local Labor Contractor",
                        phoneNumber = "+91 94481 99882",
                        contactPerson = "Mallikarjun Patil",
                        category = "Labor Supply",
                        address = "Main Market Road, Devarahipparagi",
                        notes = "Provides skilled civil masons, MMS assemblers, and cable trench diggers.",
                        createdBy = adminEmail
                    ),
                    FsVendor(
                        id = "vendor_renuka_transport",
                        name = "Shree Renuka Transport Logistics",
                        phoneNumber = "+91 98862 33441",
                        contactPerson = "Praveen Biradar",
                        category = "Transport",
                        address = "Truck Terminal, Vijayapura",
                        notes = "Heavy vehicle transport for MMS structures, invertor stations, and modules.",
                        createdBy = adminEmail
                    )
                )
                defaultVendors.forEach { v ->
                    val doc = db.collection("vendors").document(v.id)
                    vendorBatch.set(doc, v)
                }
                vendorBatch.commit().await()
                Log.d(TAG, "Seeded initial master vendors.")
            }

            // Seed Expense Types if empty
            val typeSnap = db.collection("expense_types").limit(1).get().await()
            if (typeSnap.isEmpty) {
                val typeBatch = db.batch()
                val defaultTypes = listOf(
                    FsExpenseType("type_material", "Material", "Civil, cement, steel, cables, electrical consumables", "Direct Scope", true, adminEmail),
                    FsExpenseType("type_labor", "Labor", "Daily site wages, labor contractors, skilled fitters", "Direct Scope", true, adminEmail),
                    FsExpenseType("type_machinery_fuel", "Machinery/Fuel", "DTH rig drilling, crane rental, excavator & diesel", "Direct Scope", true, adminEmail),
                    FsExpenseType("type_subcontractor", "Subcontractor", "Specialized turnkey civil and structural contractors", "Contract Work", true, adminEmail),
                    FsExpenseType("type_transport", "Transport", "Freight, tempo shifting, material delivery to site", "Logistics", true, adminEmail),
                    FsExpenseType("type_equipment_rental", "Equipment Rental", "Generators, concrete vibrators, welding machines", "Equipment", true, adminEmail),
                    FsExpenseType("type_permits", "Permits & Licenses", "Statutory fees, CEIG inspection, local approvals", "Compliance", true, adminEmail),
                    FsExpenseType("type_safety_ppe", "Safety & PPE", "Helmets, safety shoes, reflective vests, first aid kits", "Safety", true, adminEmail),
                    FsExpenseType("type_misc", "Misc", "Site refreshments, printing, petty cash and stationery", "Overhead", true, adminEmail)
                )
                defaultTypes.forEach { t ->
                    val doc = db.collection("expense_types").document(t.id)
                    typeBatch.set(doc, t)
                }
                typeBatch.commit().await()
                Log.d(TAG, "Seeded initial master expense types.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding initial data to Firestore", e)
        }
    }
}

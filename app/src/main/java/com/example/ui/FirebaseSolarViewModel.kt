package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirestoreRepository
import com.example.data.firebase.FsExpense
import com.example.data.firebase.FsExpenseType
import com.example.data.firebase.FsProject
import com.example.data.firebase.FsTask
import com.example.data.firebase.FsVendor
import com.example.data.firebase.FsWorkOrder
import com.example.data.model.ScannedReceiptData
import com.example.data.model.WorkOrderTask
import com.example.data.scanner.ReceiptScannerEngine
import com.example.data.local.PreloadedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FirebaseSolarViewModel(
    private val repository: FirestoreRepository,
    val currentUserId: String,
    val currentUserEmail: String
) : ViewModel() {

    // Selected Work Order filter: null means "All Projects / Portfolio View"
    private val _selectedWorkOrderId = MutableStateFlow<String?>(null)
    val selectedWorkOrderId: StateFlow<String?> = _selectedWorkOrderId.asStateFlow()

    // Real-time Firestore stream: Projects
    val projects: StateFlow<List<FsProject>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time Firestore stream: Raw Work Orders
    private val _rawWorkOrders = repository.observeWorkOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time Firestore stream: All tasks (across all work orders or filtered)
    private val _taskProgressOverrides = MutableStateFlow<Map<String, Pair<Double, String>>>(emptyMap())
    private val _allTasks = repository.observeTasks(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<FsTask>> =
        combine(_allTasks, _taskProgressOverrides) { rawTasks, overrides ->
            rawTasks.map { task ->
                val calculatedBudget = if (task.totalBudget > 0.0) task.totalBudget else task.totalQty * task.unitPrice
                val taskWithBudget = if (task.totalBudget > 0.0) task else task.copy(totalBudget = calculatedBudget)
                val override = overrides[task.id] ?: overrides[task.itemNumber]
                if (override != null) {
                    taskWithBudget.copy(completedQty = override.first, status = override.second)
                } else {
                    taskWithBudget
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Work Orders with dynamically updated totalBudget according to the allocated budget of each task
    val workOrders: StateFlow<List<FsWorkOrder>> =
        combine(_rawWorkOrders, allTasks) { rawOrders, taskList ->
            rawOrders.map { wo ->
                val woTasks = taskList.filter {
                    it.workOrderId == wo.id || (wo.projectId.isNotBlank() && it.projectId == wo.projectId)
                }
                val allocated = woTasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                if (allocated > 0.0) {
                    wo.copy(
                        totalBudget = allocated,
                        baseBudget = allocated,
                        taskCount = woTasks.size
                    )
                } else {
                    wo
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<FsTask>> =
        combine(allTasks, _selectedWorkOrderId, workOrders) { tasks, selectedWoId, woList ->
            val effectiveWoId = selectedWoId ?: woList.firstOrNull()?.id
            if (effectiveWoId == null) {
                tasks
            } else {
                val activeWo = woList.find { it.id == effectiveWoId }
                tasks.filter {
                    it.workOrderId == effectiveWoId ||
                    (activeWo != null && it.projectId.isNotBlank() && it.projectId == activeWo.projectId)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time Firestore stream: All expenses
    private val _allExpenses = repository.observeExpenses(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenses: StateFlow<List<FsExpense>> =
        combine(_allExpenses, _selectedWorkOrderId, workOrders) { expenses, selectedWoId, woList ->
            val effectiveWoId = selectedWoId ?: woList.firstOrNull()?.id
            if (effectiveWoId == null) {
                expenses
            } else {
                val activeWo = woList.find { it.id == effectiveWoId }
                expenses.filter {
                    it.workOrderId == effectiveWoId ||
                    (activeWo != null && it.projectId.isNotBlank() && it.projectId == activeWo.projectId)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Master stream: Vendors / Parties
    val vendors: StateFlow<List<FsVendor>> = repository.observeVendors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Master stream: Expense Types
    val expenseTypes: StateFlow<List<FsExpenseType>> = repository.observeExpenseTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category filter for tasks screen
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Progress filter for tasks screen (ALL, NOT_STARTED, IN_PROGRESS, NEAR_COMPLETE, COMPLETED)
    private val _selectedProgressFilter = MutableStateFlow("ALL")
    val selectedProgressFilter: StateFlow<String> = _selectedProgressFilter.asStateFlow()

    // Portfolio Totals: Total Budget dynamically calculated from allocated budget of each task
    val portfolioTotalBudget: StateFlow<Double> =
        combine(workOrders, _selectedWorkOrderId, allTasks) { list, sel, taskList ->
            if (sel == null) {
                val taskSum = taskList.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                if (taskSum > 0.0) taskSum else list.sumOf { it.totalBudget }
            } else {
                val activeWo = list.find { it.id == sel }
                val woTasks = taskList.filter {
                    it.workOrderId == sel || (activeWo != null && it.projectId.isNotBlank() && it.projectId == activeWo.projectId)
                }
                val taskSum = woTasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                if (taskSum > 0.0) taskSum else (activeWo?.totalBudget ?: 0.0)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val portfolioTotalSpent: StateFlow<Double> = filteredExpenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Report selection
    private val _selectedMonth = MutableStateFlow("2026-10")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Scanner state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedReceipt = MutableStateFlow<ScannedReceiptData?>(null)
    val scannedReceipt: StateFlow<ScannedReceiptData?> = _scannedReceipt.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed default work order & tasks to Firestore on first launch if empty
            repository.seedInitialDataIfEmpty(currentUserEmail)
        }
        viewModelScope.launch {
            workOrders.collect { list ->
                if (_selectedWorkOrderId.value == null && list.isNotEmpty()) {
                    _selectedWorkOrderId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            // Automatically ensure all work orders in Firestore are synced with the allocated budget of their tasks
            combine(_rawWorkOrders, allTasks) { rawOrders, taskList ->
                Pair(rawOrders, taskList)
            }.collect { (rawOrders, taskList) ->
                if (taskList.isNotEmpty() && rawOrders.isNotEmpty()) {
                    rawOrders.forEach { wo ->
                        val woTasks = taskList.filter {
                            it.workOrderId == wo.id || (wo.projectId.isNotBlank() && it.projectId == wo.projectId)
                        }
                        if (woTasks.isNotEmpty()) {
                            val allocated = woTasks.sumOf { if (it.totalBudget > 0.0) it.totalBudget else it.totalQty * it.unitPrice }
                            if (allocated > 0.0 && (Math.abs(wo.totalBudget - allocated) > 0.01 || wo.taskCount != woTasks.size)) {
                                repository.syncWorkOrderBudgetFromTasks(wo.id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectWorkOrder(workOrderId: String?) {
        _selectedWorkOrderId.value = workOrderId
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setProgressFilter(filter: String) {
        _selectedProgressFilter.value = filter
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun createWorkOrder(
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
    ) {
        viewModelScope.launch {
            // Check if project exists or create new one
            val existingProj = projects.value.find { it.name.equals(projectName, ignoreCase = true) }
            val projectId = existingProj?.id ?: repository.createProject(
                name = projectName,
                location = projectLocation,
                capacity = capacity,
                adminEmail = currentUserEmail
            )

            val woId = repository.createWorkOrder(
                projectId = projectId,
                projectName = projectName,
                workOrderNumber = workOrderNumber,
                clientName = clientName,
                contractorName = contractorName,
                baseBudget = baseBudget,
                durationDays = durationDays,
                startDate = startDate,
                tasks = tasks,
                createdBy = currentUserEmail
            )

            // Select the newly created work order
            _selectedWorkOrderId.value = woId
        }
    }

    fun addExpense(
        workOrderId: String,
        projectId: String,
        taskId: String,
        taskTitle: String,
        category: String,
        amount: Double,
        date: String,
        vendorName: String,
        billNumber: String,
        expenseType: String,
        paymentMode: String,
        notes: String,
        receiptImageUri: String? = null,
        paymentPhotoUri: String? = null,
        expensePhotoUri: String? = null,
        billAmount: Double = amount,
        paidAmount: Double = amount,
        balanceAmount: Double = 0.0,
        paymentStatus: String = "PAID"
    ) {
        viewModelScope.launch {
            val monthYear = try { date.substring(0, 7) } catch (_: Exception) { "2026-10" }
            val exp = FsExpense(
                workOrderId = workOrderId,
                projectId = projectId,
                taskId = taskId,
                taskTitle = taskTitle,
                category = category,
                amount = amount,
                billAmount = billAmount,
                paidAmount = paidAmount,
                balanceAmount = balanceAmount,
                paymentStatus = paymentStatus,
                date = date,
                monthYear = monthYear,
                vendorName = vendorName,
                billNumber = billNumber,
                expenseType = expenseType,
                paymentMode = paymentMode,
                notes = notes,
                receiptImageUri = receiptImageUri,
                paymentPhotoUri = paymentPhotoUri,
                expensePhotoUri = expensePhotoUri,
                createdBy = currentUserEmail
            )
            repository.addExpense(exp)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun updateTaskProgress(taskId: String, completedQty: Double, status: String) {
        _taskProgressOverrides.value = _taskProgressOverrides.value + (taskId to Pair(completedQty, status))
        viewModelScope.launch {
            try {
                repository.updateTaskProgress(taskId, completedQty, status)
                _statusMessage.value = "Updated site execution status to $status ($completedQty)"
                android.util.Log.d("FirebaseSolarViewModel", "Successfully updated task progress for $taskId: $status, $completedQty")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseSolarViewModel", "Failed to update task progress for $taskId", e)
                _statusMessage.value = "Failed to update status: ${e.localizedMessage}"
            }
        }
    }

    fun addTask(task: FsTask) {
        viewModelScope.launch {
            repository.addTask(task)
        }
    }

    fun updateTask(task: FsTask) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(taskId: String, workOrderId: String = "") {
        viewModelScope.launch {
            repository.deleteTask(taskId, workOrderId)
        }
    }

    fun deleteProject(workOrderId: String, projectId: String = "") {
        viewModelScope.launch {
            if (_selectedWorkOrderId.value == workOrderId) {
                _selectedWorkOrderId.value = null
            }
            repository.deleteWorkOrder(workOrderId, projectId)
        }
    }

    fun updateProjectName(workOrderId: String, projectId: String = "", newProjectName: String) {
        viewModelScope.launch {
            try {
                repository.updateProjectName(workOrderId, projectId, newProjectName)
                _statusMessage.value = "Updated project name to '$newProjectName'"
            } catch (e: Exception) {
                android.util.Log.e("FirebaseSolarViewModel", "Failed to update project name", e)
                _statusMessage.value = "Failed to update project name: ${e.localizedMessage}"
            }
        }
    }

    // Receipt scanning
    fun processCapturedReceipt(bitmap: android.graphics.Bitmap?) {
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Analyzing receipt with automated OCR and Firestore task matching..."
            try {
                kotlinx.coroutines.delay(1000)
                val taskModelList = filteredTasks.value.map {
                    WorkOrderTask(
                        id = it.id,
                        itemNumber = it.itemNumber,
                        category = it.category,
                        title = it.title,
                        description = it.description,
                        uom = it.uom,
                        totalQty = it.totalQty,
                        unitPrice = it.unitPrice,
                        totalBudget = it.totalBudget
                    )
                }
                val result = ReceiptScannerEngine.processBitmapScan(bitmap, taskModelList)
                _scannedReceipt.value = result
                _statusMessage.value = "Receipt scanned successfully! Matched to: ${result.suggestedCategory}"
            } catch (e: Exception) {
                _statusMessage.value = "Scan failed: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun processReceiptText(rawText: String) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val taskModelList = filteredTasks.value.map {
                    WorkOrderTask(
                        id = it.id,
                        itemNumber = it.itemNumber,
                        category = it.category,
                        title = it.title,
                        description = it.description,
                        uom = it.uom,
                        totalQty = it.totalQty,
                        unitPrice = it.unitPrice,
                        totalBudget = it.totalBudget
                    )
                }
                val result = ReceiptScannerEngine.parseReceiptText(rawText, taskModelList)
                _scannedReceipt.value = result
                _statusMessage.value = "Receipt parsed! Matched to: ${result.suggestedCategory}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun loadSampleReceipt(index: Int = 0) {
        val sample = PreloadedData.SAMPLE_RECEIPTS_FOR_SCANNER.getOrElse(index) {
            PreloadedData.SAMPLE_RECEIPTS_FOR_SCANNER.first()
        }
        _scannedReceipt.value = sample
        _statusMessage.value = "Loaded sample: ${sample.vendor}"
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
        _statusMessage.value = null
    }

    // ==========================================
    // VENDOR / PARTY MASTER ACTIONS
    // ==========================================

    fun addVendor(
        name: String,
        phoneNumber: String,
        contactPerson: String = "",
        category: String = "General",
        address: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addVendor(
                FsVendor(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    contactPerson = contactPerson.trim(),
                    category = category.trim(),
                    address = address.trim(),
                    notes = notes.trim(),
                    createdBy = currentUserEmail
                )
            )
        }
    }

    fun updateVendor(vendor: FsVendor) {
        viewModelScope.launch {
            repository.updateVendor(vendor)
        }
    }

    fun deleteVendor(vendorId: String) {
        viewModelScope.launch {
            repository.deleteVendor(vendorId)
        }
    }

    // ==========================================
    // EXPENSE TYPE MASTER ACTIONS
    // ==========================================

    fun addExpenseType(
        name: String,
        description: String = "",
        category: String = "Direct Scope"
    ) {
        viewModelScope.launch {
            repository.addExpenseType(
                FsExpenseType(
                    name = name.trim(),
                    description = description.trim(),
                    category = category.trim(),
                    createdBy = currentUserEmail
                )
            )
        }
    }

    fun updateExpenseType(expenseType: FsExpenseType) {
        viewModelScope.launch {
            repository.updateExpenseType(expenseType)
        }
    }

    fun deleteExpenseType(typeId: String) {
        viewModelScope.launch {
            repository.deleteExpenseType(typeId)
        }
    }
}

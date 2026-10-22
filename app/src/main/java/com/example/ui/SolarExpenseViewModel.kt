package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreloadedData
import com.example.data.local.SolarExpenseDatabase
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudgetSummary
import com.example.data.model.ScannedReceiptData
import com.example.data.model.TaskWithExpenseSummary
import com.example.data.model.WorkOrderTask
import com.example.data.repository.SolarExpenseRepository
import com.example.data.scanner.ReceiptScannerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SolarExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SolarExpenseRepository

    init {
        val db = SolarExpenseDatabase.getDatabase(application, viewModelScope)
        repository = SolarExpenseRepository(db.taskDao(), db.expenseDao())
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    // Category Filter State
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Tasks with Expense Summaries
    val allTasksWithSummary: StateFlow<List<TaskWithExpenseSummary>> =
        repository.getTasksWithSummary()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks based on selected category
    val filteredTasksWithSummary: StateFlow<List<TaskWithExpenseSummary>> =
        combine(allTasksWithSummary, _selectedCategory) { list, cat ->
            if (cat == "All") list else list.filter { it.task.category == cat }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All logged expenses
    val allExpenses: StateFlow<List<Expense>> =
        repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total spent across all tasks
    val totalSpent: StateFlow<Double> =
        repository.totalSpent
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Distinct months in records (e.g. "2026-08", "2026-09", "2026-10")
    val availableMonths: StateFlow<List<String>> =
        repository.distinctMonths
            .combine(MutableStateFlow(listOf("2026-10", "2026-09", "2026-08"))) { recorded, defaults ->
                (recorded + defaults).distinct().sortedDescending()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("2026-10", "2026-09", "2026-08"))

    // Selected Month for Reports
    private val _selectedMonth = MutableStateFlow("2026-10")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val allTasks: StateFlow<List<com.example.data.model.WorkOrderTask>> =
        repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Report Data
    val currentMonthlyReport: StateFlow<MonthlyBudgetSummary?> =
        combine(_selectedMonth, allExpenses, allTasks) { month, expenses, tasks ->
            val monthExpenses = expenses.filter { it.monthYear == month }
            val totalMonthSpent = monthExpenses.sumOf { it.amount }
            val breakdown = monthExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            val totalBudget = tasks.sumOf { it.totalBudget }
            val monthlyAllocation = if (totalBudget > 0.0) totalBudget / 3.0 else 0.0

            val display = try {
                val parts = month.split("-")
                val year = parts[0].toInt()
                val m = parts[1].toInt()
                val date = LocalDate.of(year, m, 1)
                date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            } catch (e: Exception) {
                month
            }

            MonthlyBudgetSummary(
                monthYear = month,
                displayMonth = display,
                totalSpent = totalMonthSpent,
                monthlyAllocatedBudget = monthlyAllocation,
                expenseCount = monthExpenses.size,
                categoryBreakdown = breakdown
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Scanner UI State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedReceipt = MutableStateFlow<ScannedReceiptData?>(null)
    val scannedReceipt: StateFlow<ScannedReceiptData?> = _scannedReceipt.asStateFlow()

    private val _receiptScanStatusMessage = MutableStateFlow<String?>(null)
    val receiptScanStatusMessage: StateFlow<String?> = _receiptScanStatusMessage.asStateFlow()

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun addExpense(
        taskId: String?,
        taskTitle: String,
        category: String,
        amount: Double,
        date: String,
        vendorName: String,
        billNumber: String,
        expenseType: String,
        paymentMode: String,
        notes: String,
        receiptImageUri: String? = null
    ) {
        viewModelScope.launch {
            val monthYear = try {
                date.substring(0, 7)
            } catch (e: Exception) {
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            }
            val expense = Expense(
                taskId = taskId,
                taskTitle = taskTitle,
                category = category,
                amount = amount,
                date = date,
                monthYear = monthYear,
                vendorName = vendorName,
                billNumber = billNumber,
                expenseType = expenseType,
                paymentMode = paymentMode,
                notes = notes,
                receiptImageUri = receiptImageUri
            )
            repository.addExpense(expense)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    fun updateTaskProgress(taskId: String, completedQty: Double, status: String) {
        viewModelScope.launch {
            repository.updateTaskProgress(taskId, completedQty, status)
        }
    }

    // Receipt Scanning actions
    fun processCapturedReceipt(bitmap: Bitmap?) {
        viewModelScope.launch {
            _isScanning.value = true
            _receiptScanStatusMessage.value = "Analyzing receipt with automated OCR and task matching..."
            try {
                kotlinx.coroutines.delay(1000) // Realistic scanning effect
                val tasks = allTasksWithSummary.value.map { it.task }
                val result = ReceiptScannerEngine.processBitmapScan(bitmap, tasks)
                _scannedReceipt.value = result
                _receiptScanStatusMessage.value = "Receipt scanned successfully! Matched to: ${result.suggestedCategory}"
            } catch (e: Exception) {
                _receiptScanStatusMessage.value = "Failed to scan receipt: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun processReceiptText(rawText: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _receiptScanStatusMessage.value = "Parsing receipt text..."
            try {
                val tasks = allTasksWithSummary.value.map { it.task }
                val result = ReceiptScannerEngine.parseReceiptText(rawText, tasks)
                _scannedReceipt.value = result
                _receiptScanStatusMessage.value = "Receipt parsed! Matched to: ${result.suggestedCategory}"
            } catch (e: Exception) {
                _receiptScanStatusMessage.value = "Failed to parse: ${e.localizedMessage}"
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
        _receiptScanStatusMessage.value = "Loaded sample receipt: ${sample.vendor}"
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
        _receiptScanStatusMessage.value = null
    }
}

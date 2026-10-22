package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.local.PreloadedData
import com.example.data.local.TaskDao
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudgetSummary
import com.example.data.model.TaskWithExpenseSummary
import com.example.data.model.WorkOrderTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SolarExpenseRepository(
    private val taskDao: TaskDao,
    private val expenseDao: ExpenseDao
) {
    val allTasks: Flow<List<WorkOrderTask>> = taskDao.getAllTasks()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val totalSpent: Flow<Double> = expenseDao.getTotalSpent()
    val distinctMonths: Flow<List<String>> = expenseDao.getDistinctMonths()

    fun getTasksWithSummary(): Flow<List<TaskWithExpenseSummary>> {
        return combine(allTasks, allExpenses) { tasks, expenses ->
            val expenseMap = expenses.groupBy { it.taskId }
            tasks.map { task ->
                val taskExpenses = expenseMap[task.id] ?: emptyList()
                val spent = taskExpenses.sumOf { it.amount }
                TaskWithExpenseSummary(
                    task = task,
                    totalSpent = spent,
                    expenseCount = taskExpenses.size
                )
            }
        }
    }

    fun getMonthlyReport(targetMonth: String): Flow<MonthlyBudgetSummary> {
        return combine(allExpenses, allTasks) { expenses, tasks ->
            val monthExpenses = expenses.filter { it.monthYear == targetMonth }
            val totalMonthSpent = monthExpenses.sumOf { it.amount }
            val categoryBreakdown = monthExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val totalBudget = tasks.sumOf { it.totalBudget }
            val monthlyAllocation = if (totalBudget > 0.0) totalBudget / 3.0 else 0.0

            val display = try {
                val parts = targetMonth.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val date = LocalDate.of(year, month, 1)
                date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
            } catch (e: Exception) {
                targetMonth
            }

            MonthlyBudgetSummary(
                monthYear = targetMonth,
                displayMonth = display,
                totalSpent = totalMonthSpent,
                monthlyAllocatedBudget = monthlyAllocation,
                expenseCount = monthExpenses.size,
                categoryBreakdown = categoryBreakdown
            )
        }
    }

    suspend fun addExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun updateTaskProgress(taskId: String, completedQty: Double, status: String) {
        taskDao.updateProgress(taskId, completedQty, status)
    }

    suspend fun ensureInitialized() {
        if (taskDao.getTaskCount() == 0) {
            taskDao.insertTasks(PreloadedData.INITIAL_TASKS)
        }
        if (expenseDao.getExpenseCount() == 0) {
            expenseDao.insertExpenses(PreloadedData.INITIAL_EXPENSES)
        }
    }
}

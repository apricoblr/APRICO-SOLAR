package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE taskId = :taskId ORDER BY date DESC")
    fun getExpensesByTaskId(taskId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE monthYear = :monthYear ORDER BY date DESC")
    fun getExpensesByMonth(monthYear: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalSpent(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE taskId = :taskId")
    fun getTotalSpentForTask(taskId: String): Flow<Double>

    @Query("SELECT DISTINCT monthYear FROM expenses ORDER BY monthYear DESC")
    fun getDistinctMonths(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int
}

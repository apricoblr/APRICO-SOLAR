package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkOrderTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM work_order_tasks ORDER BY itemNumber ASC")
    fun getAllTasks(): Flow<List<WorkOrderTask>>

    @Query("SELECT * FROM work_order_tasks WHERE id = :id")
    fun getTaskById(id: String): Flow<WorkOrderTask?>

    @Query("SELECT * FROM work_order_tasks WHERE category = :category ORDER BY itemNumber ASC")
    fun getTasksByCategory(category: String): Flow<List<WorkOrderTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<WorkOrderTask>)

    @Update
    suspend fun updateTask(task: WorkOrderTask)

    @Query("UPDATE work_order_tasks SET completedQty = :completedQty, status = :status WHERE id = :taskId")
    suspend fun updateProgress(taskId: String, completedQty: Double, status: String)

    @Query("SELECT COUNT(*) FROM work_order_tasks")
    suspend fun getTaskCount(): Int
}

package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Expense
import com.example.data.model.WorkOrderTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WorkOrderTask::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class SolarExpenseDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: SolarExpenseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SolarExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SolarExpenseDatabase::class.java,
                    "solar_expense_db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.taskDao(), database.expenseDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(taskDao: TaskDao, expenseDao: ExpenseDao) {
            if (taskDao.getTaskCount() == 0) {
                taskDao.insertTasks(PreloadedData.INITIAL_TASKS)
            }
            if (expenseDao.getExpenseCount() == 0) {
                expenseDao.insertExpenses(PreloadedData.INITIAL_EXPENSES)
            }
        }
    }
}

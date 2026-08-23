package com.asiradnan.asirtasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDAO {
    @Query("SELECT * from tasks WHERE isDeleted = 0 ORDER BY date IS NULL ASC, date ASC, time IS NULL DESC, time ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * from tasks WHERE uuid = :uuid")
    fun getTask(uuid: String): Flow<Task?>

    @Upsert
    suspend fun insert(task: Task)

    @Upsert
    suspend fun upsertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun deleteAll(tasks: List<Task>)

    @androidx.room.Transaction
    suspend fun applySyncChanges(upsertList: List<Task>, deleteList: List<Task>) {
        deleteAll(deleteList)
        upsertAll(upsertList)
    }

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE isSynced = 0")
    fun getUnsyncedTasksCountStream(): Flow<Int>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1")
    suspend fun getTasksToDelete(): List<Task>
}
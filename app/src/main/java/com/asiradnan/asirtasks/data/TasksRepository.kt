package com.asiradnan.asirtasks.data

import kotlinx.coroutines.flow.Flow

interface TasksRepository {
    fun getAllTasksStream(): Flow<List<Task>>

    fun getTaskStream(uuid: String): Flow<Task?>

    fun getUnsyncedTasksCountStream(): Flow<Int>


    suspend fun addTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)

    suspend fun refreshTasksFromServer()

}

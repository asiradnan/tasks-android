package com.asiradnan.asirtasks.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.asiradnan.asirtasks.util.AlarmScheduler
import com.asiradnan.asirtasks.worker.SyncWorker
import kotlinx.coroutines.flow.Flow

class DefaultTasksRepository(
    private val taskDao: TaskDAO,
    private val context: Context
) : TasksRepository {

    private val alarmScheduler = AlarmScheduler(context)

    override fun getAllTasksStream(): Flow<List<Task>> = taskDao.getAllTasks()

    override fun getTaskStream(uuid: String): Flow<Task?> = taskDao.getTask(uuid)

    override fun getUnsyncedTasksCountStream(): Flow<Int> = taskDao.getUnsyncedTasksCountStream()


    override suspend fun addTask(task: Task) {
        taskDao.insert(task.copy(isSynced = false, modificationTime = System.currentTimeMillis()))
        alarmScheduler.scheduleTaskNotification(task)
//        refreshTasksFromServer()
    }

    override suspend fun updateTask(task: Task) {
        val updatedTask = task.copy(
            modificationTime = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.update(updatedTask)
        alarmScheduler.scheduleTaskNotification(updatedTask)
//        refreshTasksFromServer()
    }

    override suspend fun deleteTask(task: Task) {
        val deletedTask = task.copy(
            isDeleted = true,
            isSynced = false,
            modificationTime = System.currentTimeMillis()
        )
        taskDao.update(deletedTask)
        alarmScheduler.cancelTaskNotification(deletedTask)
//        refreshTasksFromServer()
    }

    override suspend fun refreshTasksFromServer() {
        try {
            val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag("SyncTag")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ManualSync",
                androidx.work.ExistingWorkPolicy.REPLACE,
                oneTimeSyncRequest
            )
        } catch (e: Exception) {
            Log.d("asiradnan", e.toString())
        }
    }
}
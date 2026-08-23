package com.asiradnan.asirtasks.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.asiradnan.asirtasks.AsirTasksApplication
import com.asiradnan.asirtasks.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as AsirTasksApplication
            val taskDao = app.container.taskDao
            val alarmScheduler = AlarmScheduler(context)

            // Re-schedule all active alarms in the background
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = taskDao.getAllTasksSync()
                tasks.forEach { task ->
                    if (!task.isCompleted && !task.isDeleted && task.date != null && task.time != null) {
                        alarmScheduler.scheduleTaskNotification(task)
                    }
                }
            }
        }
    }
}

package com.asiradnan.asirtasks.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.asiradnan.asirtasks.data.Task
import com.asiradnan.asirtasks.receiver.TaskAlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskNotification(task: Task) {
        if (task.date == null || task.time == null || task.isCompleted || task.isDeleted) {
            cancelTaskNotification(task)
            return
        }

        // Combine date and time
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.date

            val hour = (task.time / 3600000L).toInt()
            val minute = ((task.time % 3600000L) / 60000L).toInt()

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Only schedule if time is in the future
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_UUID", task.uuid)
            putExtra("TASK_NAME", task.name)
            if (task.time != null) {
                putExtra("TASK_TIME", task.time)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Scheduled alarm for ${task.name} at ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Exact alarm permission denied", e)
        }
    }

    fun cancelTaskNotification(task: Task) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm for ${task.name}")
    }
}

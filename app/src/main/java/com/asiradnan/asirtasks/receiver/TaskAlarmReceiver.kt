package com.asiradnan.asirtasks.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.asiradnan.asirtasks.R
import com.asiradnan.asirtasks.util.toFormattedTime

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (com.asiradnan.asirtasks.AsirTasksApplication.isAppInForeground) {
            return
        }
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Task Reminder"
        val taskId = intent.getStringExtra("TASK_UUID") ?: return
        val taskTime =
            if (intent.hasExtra("TASK_TIME")) intent.getLongExtra("TASK_TIME", -1L) else null

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mainActivityIntent =
            Intent(context, com.asiradnan.asirtasks.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            mainActivityIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (taskTime != null && taskTime != -1L) {
            "Due at ${taskTime.toFormattedTime()}"
        } else {
            "Task is due now."
        }

        val notification = NotificationCompat.Builder(context, "TASK_REMINDERS")
            .setSmallIcon(R.drawable.checklist)
            .setContentTitle(taskName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}

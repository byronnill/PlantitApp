package com.mobdeve.s15.group8.mobdeve_mp.model.workers

import android.app.Notification.DEFAULT_ALL
import android.app.Notification.PRIORITY_MAX
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent.getActivity
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mobdeve.s15.group8.mobdeve_mp.R
import com.mobdeve.s15.group8.mobdeve_mp.controller.activities.MainActivity
import com.mobdeve.s15.group8.mobdeve_mp.model.services.TaskService
import com.mobdeve.s15.group8.mobdeve_mp.singletons.F
import com.mobdeve.s15.group8.mobdeve_mp.singletons.PushPermissions
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class NotificationWorker(context: Context, params: WorkerParameters):
    Worker(context, params)
{
    override fun doWork(): Result {
        val id = inputData.getLong(NOTIFICATION_ID, 0).toInt()

        val pushStatus = applicationContext.getSharedPreferences(applicationContext.getString(R.string.SP_NAME), Context.MODE_PRIVATE)
                .getInt(applicationContext.getString(R.string.SP_PUSH_KEY), -1)

        if (pushStatus == PushPermissions.ALLOWED.ordinal) {
            sendNotification(id)

            val workManager = WorkManager.getInstance(applicationContext)

            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 9)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)

            if (c.before(Calendar.getInstance()))
                c.add(Calendar.DATE, 1)

            val delay = c.timeInMillis - Calendar.getInstance().timeInMillis

            val dailyWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueue(dailyWorkRequest)
        }

        return Result.success()
    }

    private fun sendNotification(id: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(NOTIFICATION_ID, id)

        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = getActivity(applicationContext, 0, intent, 0)

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_plant_24)
            .setDefaults(DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(PRIORITY_MAX)

        if (SDK_INT >= O) {
            notification.setChannelId(FirstNotificationWorker.NOTIFICATION_CHANNEL)

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                NOTIFICATION_NAME,
                IMPORTANCE_HIGH)

            notificationManager.createNotificationChannel(channel)
        }

        F.tasksCollection
            .whereEqualTo("userId", F.auth.uid)
            .get()
            .addOnSuccessListener { docs ->
                Log.d("hello", TaskService.isTasksToday(docs).toString())
                if (TaskService.isTasksToday(docs)) {
                    notification
                        .setContentTitle("Your plants are calling...")
                        .setContentText("Your plants need you to take care of them. Open PlantitApp to see what you need to do.")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("Your plants need you to take care of them. Open PlantitApp to see what you need to do."))
                } else {
                    notification
                        .setContentTitle("Hooray!")
                        .setContentText("No tasks for today. You can take a break!")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("No tasks for today. You can take a break!"))
                }

                notificationManager.notify(id, notification.build())
            }
            .addOnFailureListener {
                notification
                    .setContentTitle("Error")
                    .setContentText("I can't get your plant data right now. Please go online and open PlantitApp to see your plants.")

                notificationManager.notify(id, notification.build())
            }
    }

    companion object {
        const val NOTIFICATION_ID = "appName_notification_id"
        const val NOTIFICATION_NAME = "appName"
        const val NOTIFICATION_CHANNEL = "appName_channel_01"
    }
}

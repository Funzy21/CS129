package com.amadeus.ting

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat


class NotificationSystem(context: Context) : ContextWrapper(context) {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the notification channel for Android 8.0 and above
            val channelId = "your_channel_id"
            val channelName = "Your Channel Name"
            val channelDescription = "Your Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = channelDescription

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun showNotification() {
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(this, Planner::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // Customize the notification based on the activity
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, "channel_id")
            .setContentText("Ting notifications enabled!")
            .setContentTitle("Ting Notification")
            .setSmallIcon(R.drawable.app_logo)
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1,notification)
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}

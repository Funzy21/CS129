package com.amadeus.ting

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"
const val GROUP_KEY_TASK = "TASKNOTIF_TIME"

class AlarmReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, Planner::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(intent.getStringExtra(titleExtra))
            .setContentText(intent.getStringExtra(messageExtra))
            .setGroup(GROUP_KEY_TASK)
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)
            .build()

        val  manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)
    }

}
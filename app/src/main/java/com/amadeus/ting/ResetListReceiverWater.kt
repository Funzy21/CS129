package com.amadeus.ting

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.amadeus.ting.FoodIntake.Companion.PREFS_NAME
import java.util.*

class ResetListReceiverWater : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver", "CommitPrefEdits")
    override fun onReceive(context: Context, intent: Intent) {
        println("=========================================== resetListReceiverWater run")

        val sleepSharedPref = context.getSharedPreferences("SleepData", Context.MODE_PRIVATE)
        val prefs = context.getSharedPreferences("WaterIntakePrefsFile", Context.MODE_PRIVATE)
        val milliliterGoal = prefs.getInt("milliliterGoal", 0)

        // Get WakeTime Data
        val wakeTimeFromSleep = sleepSharedPref.getString("WakeTime", null)
        val wakeTimeParts = wakeTimeFromSleep?.split(":")
        val wakeHour = wakeTimeParts?.get(0)?.toIntOrNull()
        val wakeMinute = wakeTimeParts?.get(1)?.substringBefore(" ")?.toIntOrNull()
        val wakeAMPM = wakeTimeParts?.get(1)?.substringAfter(" ")?.trim()

        val timeArray: Array<String> = Array(10000) { "" }
        val numberOfLitersArray: Array<String> = Array(10000) { "" }
        val index = prefs.getInt("index", 0)

        // Current Day-Year-Month
        val currentTime = Calendar.getInstance()
        val year = currentTime.get(Calendar.YEAR)
        val month = currentTime.get(Calendar.MONTH)
        val dayOfMonth = currentTime.get(Calendar.DAY_OF_MONTH)
        val databaseTing = TingDatabase(context)
        if (wakeHour != null && wakeMinute != null && wakeAMPM != null) {
            println("=========================================== resetListReceiverWater prefsEdit")

            for (i in 0 until index) {
                timeArray[i] = prefs.getString("waterIntakeInfoTime_$i", "").toString()
                numberOfLitersArray[i] = prefs.getString("waterIntakeInfoNumberOfLiters_$i", "").toString()

                val waterIntakeModel = WaterIntakeModel("$month ${dayOfMonth - 1} $year", i + 1, timeArray[i], numberOfLitersArray[i])
                databaseTing.addWaterData(waterIntakeModel)
            }

            with(prefs.edit()) {
                putInt("currentPercentage", 0)
                putInt("totalMilliliter", 0)
                putInt("milliliterLeft", milliliterGoal)
                putInt("milliliterGoal", milliliterGoal)
                putInt("index", 0)
                apply()
            }
        }
        Toast.makeText(context, "Wakeup time reached! The water intake records will reset.", Toast.LENGTH_SHORT).show()
    }
}
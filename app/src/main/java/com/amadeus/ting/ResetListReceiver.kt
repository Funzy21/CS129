package com.amadeus.ting

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import java.util.*

class ResetListReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        println("=========================================== resetListReceiver run")
        val currentTime = Calendar.getInstance()

        val sleepSharedPref = context.getSharedPreferences("SleepData", Context.MODE_PRIVATE)
        val wakeTimeFromSleep = sleepSharedPref.getString("WakeTime", null)

        val wakeTimeParts = wakeTimeFromSleep?.split(":")
        val wakeHour = wakeTimeParts?.get(0)?.toIntOrNull()
        val wakeMinute = wakeTimeParts?.get(1)?.substringBefore(" ")?.toIntOrNull()
        val wakeAMPM = wakeTimeParts?.get(1)?.substringAfter(" ")?.trim()
        println("============================== wakeTime: $wakeHour $wakeMinute $wakeAMPM")

        // Check if all the necessary values are not null
        if (wakeHour != null && wakeMinute != null && wakeAMPM != null) {
            val prefs = context.getSharedPreferences("FoodIntakePrefFile", Context.MODE_PRIVATE)
            val otherPrefs = context.getSharedPreferences("FoodIntakePrefs", Context.MODE_PRIVATE)
            val numberMeals = otherPrefs.getInt("pref_meals_per_day", 0)

            val checkVisibility = Array(numberMeals) { false }
            val timeToEatHours  = Array(numberMeals) {0}
            val timeToEatMinutes  = Array(numberMeals) {0}
            val timeToEatMeridiem = Array(numberMeals) {""}
            val databaseTing = TingDatabase(context)

            // Current Day-Year-Month
            val year = currentTime.get(Calendar.YEAR)
            val month = currentTime.get(Calendar.MONTH)
            val dayOfMonth = currentTime.get(Calendar.DAY_OF_MONTH)
            for (i in 0 until numberMeals) { // Save to Database
                checkVisibility[i] = prefs.getBoolean("checkVisible_${i}", false)
                timeToEatHours[i] = prefs.getInt("timeToEatHour_${i}", 0)
                timeToEatMinutes[i] = prefs.getInt("timeToEatMinute_${i}", 0)
                timeToEatMeridiem[i] = prefs.getString("timeToEatMeridiem_${i}", "").toString()

                val mealTimeModel = MealTimeModel(i + 1, "$month ${dayOfMonth - 1} $year", "${timeToEatHours[i]} ${timeToEatMinutes[i]} ${timeToEatMeridiem[i]}", checkVisibility[i])
                databaseTing.addMealtimeData(mealTimeModel)
            }

            // Perform the reset operation
            for (i in 0 until numberMeals) {
                // Resets the visibility of the buttons and the variable pointing to the button to be clicked
                with(prefs.edit()) {
                    putInt("buttonToBeClicked", 1)
                    putInt("timeToEatHour_$i", 0)
                    putInt("timeToEatMinute_$i", 0)
                    putString("timeToEatMeridiem_$i", "")
                    putBoolean("editTimeVisible_$i", true)
                    putBoolean("eatButtonVisible_$i", true)
                    putBoolean("checkVisible_$i", false)
                    apply()
                }
            }
        }
        Toast.makeText(context, "Wakeup time reached! The mealtime schedule will reset.", Toast.LENGTH_SHORT).show()
    }
}
package com.amadeus.ting

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.amadeus.ting.databinding.ActivityWaterIntakeBinding
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList

class WaterIntake : AppCompatActivity(), CalendarAdapter.OnDateClickListener {
    // Initializing horizontal calendar
    private lateinit var binding: ActivityWaterIntakeBinding
    private lateinit var calendarAdapter: CalendarAdapter

    private lateinit var seekBar: SeekBar
    private lateinit var valueTextView: TextView
    private lateinit var waterSectionAlertDialog : WaterAlertDialog
    private lateinit var prefs : SharedPreferences

    private val PREFS_NAME = "WaterIntakePrefsFile" // Shared Preferences File

    private var milliliterGoal : Int = 0
    private var milliliterLeft : Int = 0
    private var totalMilliliter : Int = 0

    private var seekBarInput : Int = 0
    private var currentPercentage : Int = 0

    private lateinit var waterIntakeInformation : ArrayList<WaterIntakeInfo>
    private var index : Int = 0

    private lateinit var databaseTing : TingDatabase

    private var timeArray: Array<String> = Array(10000) { "" }
    private var numberOfLitersArray: Array<String> = Array(10000) { "" }

    private lateinit var recyclerViewWater : RecyclerView
    private lateinit var recyclerViewWaterMain : RecyclerView

    private lateinit var alertDialogBuilder : AlertDialog.Builder
    private lateinit var dialogView : View

    private val calendarData = CalendarData()

    private lateinit var alarmManager: AlarmManager
    private lateinit var intent: Intent
    private var pendingIntent: PendingIntent? = null

    @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables", "SetTextI18n",
        "InflateParams", "CutPasteId"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_intake)
        window.statusBarColor = ContextCompat.getColor(this, R.color.cyan)

        databaseTing = TingDatabase(applicationContext)

        binding = ActivityWaterIntakeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpAdapter()
        setUpClickListener()
        setUpCalendar()

        val drinkWrapper = findViewById<ConstraintLayout>(R.id.drink_wrapper)
        val recyclerViewWaterLayout = findViewById<LinearLayout>(R.id.recycler_view_water)
        val buttonsLayout = findViewById<LinearLayout>(R.id.buttons)
        drinkWrapper.visibility = View.VISIBLE
        recyclerViewWaterLayout.visibility = View.GONE
        buttonsLayout.visibility = View.VISIBLE

        alertDialogBuilder = AlertDialog.Builder(this)
        dialogView = LayoutInflater.from(this).inflate(R.layout.water_intake_records, null)

        // Initialize Recycler Views
        recyclerViewWater = dialogView.findViewById(R.id.water_intake_records_rv)
        recyclerViewWater.layoutManager = LinearLayoutManager(this)
        recyclerViewWater.setHasFixedSize(true)

        recyclerViewWaterMain = findViewById(R.id.water_intake_rv)
        recyclerViewWaterMain.layoutManager = LinearLayoutManager(this)
        recyclerViewWaterMain.setHasFixedSize(true)

        waterSectionAlertDialog = WaterAlertDialog() // Instance of Water Intake Dialog

        // Opens Shared Preferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreElementsInWaterRecordArray()
        restoreStoredPrefData()

        // Initialize Values
        val drinkTextView: TextView = findViewById(R.id.drink)
        drinkTextView.text = "Drink $milliliterGoal ml"

        val numberPercent: TextView = findViewById(R.id.number_percent)
        numberPercent.text = "${currentPercentage}%"

        val progressBar: ProgressBar = findViewById(R.id.progress_bar)
        progressBar.progress = currentPercentage

        val numberMLLeft: TextView = findViewById(R.id.number_ml)
        numberMLLeft.text = "$milliliterLeft ml left"

        seekBar = findViewById(R.id.seekBar)
        seekBar.max = milliliterLeft

        // Today's Goal -> (Inputs number of ml)
        val goalText: TextView = findViewById(R.id.goal_text)
        goalText.setOnClickListener {
            waterSectionAlertDialog.intakeGoalDialog(this, R.layout.water_popupwindow) { userMilliliterInput ->
                if (userMilliliterInput != null) {
                    // Reset records
                    val editor = prefs.edit()
                    editor.clear()

                    progressBar.progress = 0
                    numberPercent.text = "0%"
                    currentPercentage = 0
                    totalMilliliter = 0
                    index = 0
                    milliliterLeft = milliliterGoal
                    seekBar.max = milliliterGoal
                    numberMLLeft.text = "$milliliterLeft ml left"

                    // Set the records depending on the input
                    milliliterGoal = userMilliliterInput
                    milliliterLeft = userMilliliterInput
                    seekBar.max = milliliterGoal

                    drinkTextView.text = "Drink $milliliterGoal ml" // Updates the number of milliliter value
                    numberMLLeft.text = "$milliliterGoal ml left"

                    editor.putInt("milliliterGoal", milliliterGoal)
                    editor.putInt("milliliterLeft", milliliterGoal)
                    editor.apply()
                }
            }
        }
        valueTextView = findViewById(R.id.textView_progress) // Seekbar Value

        // Seekbar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                valueTextView.text = "$progress ml"
                seekBarInput = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        val drinkButton: View = findViewById(R.id.drink_button)
        drinkButton.setOnClickListener {

            val temp = seekBarInput
            if (milliliterLeft == 0) {
                Toast.makeText(this, "No more water left to drink!", Toast.LENGTH_SHORT).show()
            } else if (seekBarInput == 0) {
                Toast.makeText(this, "Input number of ml to drink.", Toast.LENGTH_SHORT).show()
            } else {
                // Updates the percentage value
                totalMilliliter += seekBarInput
                currentPercentage = (((totalMilliliter.toFloat() / milliliterGoal.toFloat()) * 100)).toInt()
                numberPercent.text = "${currentPercentage}%"

                // Updates the number of ml left
                milliliterLeft -= seekBarInput
                numberMLLeft.text = "$milliliterLeft ml left"

                // Updates the Progress Bar
                progressBar.progress = currentPercentage
                seekBar.max = milliliterLeft

                Toast.makeText(this, "Drank water successfully!", Toast.LENGTH_SHORT).show()


                val calendar = Calendar.getInstance()

                val hours = if (calendar.get(Calendar.HOUR) == 0) {12} else { calendar.get(Calendar.HOUR) }
                val minutes = calendar.get(Calendar.MINUTE)
                val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

                val editor = prefs.edit()

                // Stores Information in the waterIntakeInformation array
                if (timeArray.isEmpty() && numberOfLitersArray.isEmpty()) {
                    timeArray[0] = String.format("%02d:%02d %s", hours, minutes, amPm)
                    numberOfLitersArray[0]= "$temp ml"
                    editor.putString("waterIntakeInfoTime_0", "$hours:$minutes $amPm")
                    editor.putString("waterIntakeInfoNumberOfLiters_0", "$temp ml")
                    index++
                } else {
                    timeArray[index] = String.format("%02d:%02d %s", hours, minutes, amPm)
                    numberOfLitersArray[index]= "$temp ml"
                    editor.putString("waterIntakeInfoTime_$index", "$hours:$minutes $amPm")
                    editor.putString("waterIntakeInfoNumberOfLiters_$index", "$temp ml")
                    index++
                }

                editor.putInt("currentPercentage", currentPercentage)
                editor.putInt("milliliterLeft", milliliterLeft)
                editor.putInt("totalMilliliter", totalMilliliter)
                editor.putInt("index", index)
                editor.apply()
            }
        }

        // Records Button Clicked (Alert Dialog Popup)
        val recordsButton: View = findViewById(R.id.records_button)
        recordsButton.setOnClickListener {
            restoreElementsInWaterRecordArray()
            recordsDialog()
        }

        // Reset Button Clicked
        val resetButton: View = findViewById(R.id.reset_button)
        resetButton.setOnClickListener {
            val editor = prefs.edit()
            editor.clear()

            progressBar.progress = 0
            numberPercent.text = "0%"
            currentPercentage = 0
            totalMilliliter = 0
            index = 0
            milliliterLeft = milliliterGoal
            seekBar.max = milliliterGoal
            numberMLLeft.text = "$milliliterLeft ml left"

            editor.putInt("milliliterGoal", milliliterGoal)
            editor.putInt("milliliterLeft", milliliterGoal)
            editor.apply()

            Toast.makeText(this, "Reset successful!", Toast.LENGTH_SHORT).show()
        }


        // Behavior of each buttons in the app
        onClick<ShapeableImageView>(R.id.back_button){
            val goToHomePage = Intent(this, HomePage::class.java)
            startActivity(goToHomePage)
        }

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        intent = Intent(this, ResetListReceiverWater::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Get WakeTime Data
        val sleepSharedPref = getSharedPreferences("SleepData", Context.MODE_PRIVATE)
        val wakeTimeFromSleep = sleepSharedPref.getString("WakeTime", null)

        val wakeTimeParts = wakeTimeFromSleep?.split(":")
        var wakeHour = wakeTimeParts?.get(0)?.toIntOrNull()
        val wakeMinute = wakeTimeParts?.get(1)?.substringBefore(" ")?.toIntOrNull()
        val wakeAMPM = wakeTimeParts?.get(1)?.substringAfter(" ")?.trim()

        if (wakeHour != null && wakeMinute != null && wakeAMPM != null) {
            if (wakeAMPM == "PM" && wakeHour == 12) { wakeHour = 12 } else { if (wakeAMPM == "PM") { wakeHour += 12 } }

            val currentTime = Calendar.getInstance()
            val scheduledTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, wakeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(currentTime)) { // Adjust the scheduled time if it's already passed today
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            println("========== scheduledTime: $scheduledTime")

            // Schedule the task
            if (milliliterGoal != 0 && index != 0) {
                println("==================== setAlarmWater!!!!!!!!!")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime.timeInMillis, pendingIntent)
            }
        }

    }

    private inline fun <reified T : View> Activity.onClick(id: Int, crossinline action: (T) -> Unit) {
        findViewById<T>(id)?.apply {
            setOnClickListener {
                action(this)
            }
        }
    }


    private fun restoreStoredPrefData() {
        milliliterGoal = prefs.getInt("milliliterGoal", 0)
        currentPercentage = prefs.getInt("currentPercentage", 0)
        totalMilliliter = prefs.getInt("totalMilliliter", 0)
        milliliterLeft = prefs.getInt("milliliterLeft", 0)
        index = prefs.getInt("index", 0)
    }

    private fun restoreElementsInWaterRecordArray() {

        waterIntakeInformation  = arrayListOf()

        for (i in 0 until index) {
            timeArray[i] = prefs.getString("waterIntakeInfoTime_$i", "").toString()
            numberOfLitersArray[i] = prefs.getString("waterIntakeInfoNumberOfLiters_$i", "").toString()

            val waterIntakeInfo = WaterIntakeInfo(timeArray[i],  numberOfLitersArray[i])
            waterIntakeInformation.add(waterIntakeInfo)
        }
        recyclerViewWater.adapter = WaterIntakeAdapter(this, waterIntakeInformation)
    }

    @SuppressLint("InflateParams")
    private fun recordsDialog() {
        dialogView = LayoutInflater.from(this).inflate(R.layout.water_intake_records, null)

        alertDialogBuilder = AlertDialog.Builder(this, R.style.MyDialogStyle)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        // Recycler View
        recyclerViewWater = dialogView.findViewById(R.id.water_intake_records_rv)

        recyclerViewWater.layoutManager = LinearLayoutManager(this)
        recyclerViewWater.setHasFixedSize(true)

        val adapter = WaterIntakeAdapter(this, waterIntakeInformation)
        recyclerViewWater.adapter = adapter

        val cancelButton = dialogView.findViewById<Button>(R.id.back_button)


        val dialog = alertDialogBuilder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

    }

    private fun setUpClickListener() {
        val currentDate = Calendar.getInstance(Locale.ENGLISH)
        binding.ivCalendarNext.setOnClickListener {
            calendarData.currentDate.add(Calendar.MONTH, 1)
            setUpCalendar()
        }
        binding.ivCalendarPrevious.setOnClickListener {
            calendarData.currentDate.add(Calendar.MONTH, -1)
            if (calendarData.currentDate == currentDate)
                setUpCalendar()
            else
                setUpCalendar()
        }
    }
    private fun setUpAdapter() {
        //For positioning the recyclerview
        val curDate = LocalDate.now()
        val defPos = curDate.dayOfMonth-3

        // Horizontal spacing for each date in the calendar
        val dateSpacing = resources.getDimensionPixelSize(R.dimen.single_calendar_margin)
        binding.calendarRecycler.addItemDecoration(HorizontalItemDecoration(dateSpacing))

        // Center snap for scrolling
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.calendarRecycler)

        calendarAdapter = CalendarAdapter({ calendarDateModel: CalendarDateModel, position ->
            calendarData.calendarList.forEachIndexed { index, calendarModel ->
                calendarModel.isSelected = index == position
            }
            calendarAdapter.setData(calendarData.calendarList)
        }, this)

        binding.calendarRecycler.adapter = calendarAdapter
        binding.calendarRecycler.scrollToPosition(defPos)
        // Line below requires debugging, need to check why it doesn't function
    }
    private fun setUpCalendar() {
        val calendarList = java.util.ArrayList<CalendarDateModel>()
        binding.tvDateMonth.text = calendarData.dateFormat.format(calendarData.currentDate.time)
        val monthCalendar = calendarData.currentDate.clone() as Calendar
        val maxDaysInMonth = calendarData.currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendarData.dates.clear()
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        while (calendarData.dates.size < maxDaysInMonth) {
            calendarData.dates.add(monthCalendar.time)
            calendarList.add(CalendarDateModel(monthCalendar.time))
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarData.calendarList.clear()
        calendarData.calendarList.addAll(calendarList)
        calendarAdapter.setData(calendarList)
    }

    override fun onDateClick(position: Int) {
        //Add the date here
        val clickedDateModel = calendarAdapter.getItem(position)

        val dateString = getMonthDayYear(clickedDateModel.toString())
        val dateParts = dateString.split(" ")

        val clickedMonth = dateParts[0].toInt()
        val clickedDay = dateParts[1].toInt()
        val clickedYear = dateParts[2].toInt()
        val queryDate = "$clickedMonth $clickedDay $clickedYear"

        val currentDate = Calendar.getInstance()

        val drinkWrapper = findViewById<ConstraintLayout>(R.id.drink_wrapper)
        val recyclerViewWater = findViewById<LinearLayout>(R.id.recycler_view_water)
        val buttonsLayout = findViewById<LinearLayout>(R.id.buttons)

        if (clickedYear == currentDate.get(Calendar.YEAR) &&
            clickedMonth == currentDate.get(Calendar.MONTH) &&
            clickedDay == currentDate.get(Calendar.DAY_OF_MONTH)) {
            // unhidden everything show the water intake goal
            drinkWrapper.visibility = View.VISIBLE
            recyclerViewWater.visibility = View.GONE
            buttonsLayout.visibility = View.VISIBLE
        } else {
            // hide everything, show recycler view
            drinkWrapper.visibility = View.GONE
            recyclerViewWater.visibility = View.VISIBLE
            buttonsLayout.visibility = View.GONE

            val waterIntakeDataArray: MutableList<WaterIntakeModel> = databaseTing.getWaterData(queryDate) as MutableList<WaterIntakeModel>
            val tempWaterScheduleList = arrayListOf<WaterIntakeInfo>()

            for (i in 0 until waterIntakeDataArray.size) {
                val tempMl = waterIntakeDataArray[i].intakeNumberMl
                val tempTime = waterIntakeDataArray[i].intakeTime

                val intakeInfo = WaterIntakeInfo(tempTime, tempMl)
                tempWaterScheduleList.add(intakeInfo)
            }
            recyclerViewWaterMain.adapter = WaterIntakeAdapter(this, tempWaterScheduleList)
        }
    }
    private fun getMonthDayYear(information : String): String  {

        // Extract the date portion from the input string
        val dateString = information.substringAfter("data=").substringBefore(",")

        // Parse the date string using SimpleDateFormat
        val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
        val date = dateFormat.parse(dateString)

        // Extract the desired components (date, month, year) from the parsed date
        val calendar = Calendar.getInstance()
        if (date != null) {
            calendar.time = date
        }

        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return "$month $day $year"

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Get WakeTime Data
        println("=================================== onBackPressed mealtime")
        val sleepSharedPref = getSharedPreferences("SleepData", Context.MODE_PRIVATE)
        val wakeTimeFromSleep = sleepSharedPref.getString("WakeTime", null)

        val wakeTimeParts = wakeTimeFromSleep?.split(":")
        var wakeHour = wakeTimeParts?.get(0)?.toIntOrNull()
        val wakeMinute = wakeTimeParts?.get(1)?.substringBefore(" ")?.toIntOrNull()
        val wakeAMPM = wakeTimeParts?.get(1)?.substringAfter(" ")?.trim()

        if (wakeHour != null && wakeMinute != null && wakeAMPM != null) {
            if (wakeAMPM == "PM" && wakeHour == 12) { wakeHour = 12 } else { if (wakeAMPM == "PM") { wakeHour += 12 } }

            val currentTime = Calendar.getInstance()
            val scheduledTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, wakeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(currentTime)) { // Adjust the scheduled time if it's already passed today
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            println("========== scheduledTime: $scheduledTime")

            // Schedule the task
            if (milliliterGoal != 0 && index != 0) {
                println("==================== setAlarmWater!!!!!!!!!")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledTime.timeInMillis, pendingIntent)
            }
        }
        val intent = Intent(this, HomePage::class.java)
        startActivity(intent)
        finish()
    }
}








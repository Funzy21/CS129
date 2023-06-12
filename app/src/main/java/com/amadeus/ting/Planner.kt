package com.amadeus.ting


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.amadeus.ting.databinding.ActivityPlannerBinding
import com.google.android.material.imageview.ShapeableImageView
import java.time.LocalDate
import java.util.*


class Planner : AppCompatActivity(), CalendarAdapter.OnDateClickListener, MyAlertDialog.OnCreateTaskListener{

    private lateinit var binding: ActivityPlannerBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tskList: List<TaskModel>

    private var taskadapter: TaskAdapter? = null
    private var sortedTaskList: List<TaskModel> = emptyList()
    private var checkedTaskList: List<TaskModel> = emptyList()
    private var isDoneTasksVisible = false
    private var uniqueNotifID = 0
    private var addedTask: TaskModel?= null

    private val calendarData = CalendarData()
    private val notificationSystem:NotificationSystem = NotificationSystem(this)

    // For Notification permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
            isGranted: Boolean ->
        if(isGranted){
            notificationSystem.showNotification()
        }
        else{
            Toast.makeText(this, "Notifications currently disabled.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planner)
        window.statusBarColor = ContextCompat.getColor(this, R.color.red)

        binding = ActivityPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createNotificationChannel()
        val dbHelper = TingDatabase(applicationContext)

        setUpAdapter()
        setUpClickListener()
        setUpCalendar()
        initRecyclerView()

        val notifButton = findViewById<ShapeableImageView>(R.id.notifreq)
        notifButton.setOnClickListener{
            when{
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED ->{
                    notificationSystem.showNotification()
                }
                else->{
                    requestPermissionsLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }


        // Get the shared preferences object with the name "MyPreferences"
        val sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Get the values stored in shared preferences for button press, alphabetical arrow and deadline arrow
        val buttonValue = sharedPref.getInt("buttonPressed", -1)
        val getAlphabeticalArrow = sharedPref.getInt("isAlphabeticalArrowUp", -1)
        val getDeadlineArrow = sharedPref.getInt("isDeadlineArrowUp", -1)

        // Initialize the task list based on the shared preferences values
        tskList = if (buttonValue != -1) {
            if (buttonValue == 1 && getAlphabeticalArrow != -1) {
                dbHelper.getAllTasks(1, getAlphabeticalArrow) // Get all tasks sorted by alphabetical order
            } else if (buttonValue == 2 && getDeadlineArrow != -1) {
                dbHelper.getAllTasks(2, getDeadlineArrow) // Get all tasks sorted by deadline
            } else {
                dbHelper.getAllTasks() // Get all tasks
            }
        } else {
            dbHelper.getAllChecks()
            dbHelper.getAllTasks() // Get all tasks
        }

        // Add the task list to the adapter
        taskadapter?.addList(tskList)

        // Label -> Myka
        onClick<ShapeableImageView>(R.id.label_button) {
            val labelAlertDialog = MyAlertDialog()
            labelAlertDialog.showCustomDialog(this, R.layout.view_labels, R.layout.add_label, R.id.label_sample)
        }

        // Create -> Jugu
        onClick<ShapeableImageView>(R.id.create_button) {
            val labelAlert = MyAlertDialog()
            labelAlert.setOnCreateTaskListener(this)
            labelAlert.showCustomDialog(this, R.layout.create_popupwindow, -1, -1, 1)
        }

        // Set an onClick listener for the sort button
        onClick<ShapeableImageView>(R.id.sort_button) {
            val sortLabelAlert = MyAlertDialog()
            sortLabelAlert.sortAlertDialog(this, R.layout.sort_popupwindow, R.layout.sort_nestedpopupwindow, R.id.text_label, sharedPref) {

                // Get the sorted task list from the alert dialog
                sortedTaskList = sortLabelAlert.getTaskList()

                // Add the sorted task list to the adapter and update the current task list
                taskadapter?.addList(sortedTaskList)
                tskList = sortedTaskList

                // Update the shared preferences with the button press and arrow direction states
                val editor = sharedPref.edit()
                editor.putInt("buttonPressed", sortLabelAlert.getButtonPressed())
                editor.putInt("isAlphabeticalArrowUp", sortLabelAlert.getAlphabeticalArrowState())
                editor.putInt("isDeadlineArrowUp", sortLabelAlert.getDeadlineArrowState())
                editor.apply()
            }
        }

        val textViewDone = findViewById<ToggleButton>(R.id.textView_Done)

        textViewDone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedTaskList = dbHelper.getAllCheckedTasks()
                taskadapter?.addList(checkedTaskList)
                isDoneTasksVisible = true
                val color = ContextCompat.getColor(this, R.color.red)
                textViewDone.backgroundTintList = ColorStateList.valueOf(color) // Set the desired color when isChecked is false
            } else {
                tskList = dbHelper.getAllTasks()
                taskadapter?.addList(tskList)
                isDoneTasksVisible = false
                val color = ContextCompat.getColor(this, R.color.black)
                textViewDone.backgroundTintList = ColorStateList.valueOf(color) // Set the desired color when isChecked is true
            }
        }


        onClick<ShapeableImageView>(R.id.back_button){
            val goToHomePage = Intent(this, HomePage::class.java)
            startActivity(goToHomePage)
        }


    }
    // Generating unique notif IDs to create multiple notifications
    private fun genNotifID(): Int{
        uniqueNotifID += 1
        return uniqueNotifID
    }

    private fun setNotif(timeData : String){
        val intent = Intent(this, AlarmReceiver::class.java)
        val title = "Planner Deadlines"
        val message = "You have tasks due in an hour, hope you haven't forgotten!"
        val notifID = genNotifID()
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = extractTimeAttributes(timeData)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        showAlert(time, title, message)
    }

    // Used to extract the time attributes from the taskDate string format
    private fun extractTimeAttributes(dateString: String): Long {
        // Make the case for ALL DAY cases, else:

        // Cleaning up the string and getting rid of unnecessary symbols
        var cleanedString = dateString.replace("|", "")
        cleanedString = cleanedString.replace("⏰", "")

        // Dealing with whitespace characters
        val dateTimeComponents = cleanedString.split("\\s+".toRegex())
        val date = dateTimeComponents[0]
        val time = dateTimeComponents[1]
        val amPmIndicator = dateTimeComponents[2]

        // Distinguishing the minute from the hour
        val timeComponents = time.split(":")
        var hour = timeComponents[0].toInt()
        val minute = timeComponents[1].toInt()

        val dateComponents = date.split("/")
        val month = dateComponents[0].toInt() - 1 // Adjust month value to zero-based index
        val day = dateComponents[1].toInt()
        val year = dateComponents[2].toInt()

        // Subtracted them by 1 beforehand so that the notifs play an hour before the deadline
        // Feel free to experiment with the values
        if(amPmIndicator == "PM" && hour != 12){
            hour += 11
        }
        else{
            hour -= 1
        }

        if(amPmIndicator == "AM" && hour == 12){
            hour = 23
        }
        //if (amPmIndicator == "PM" && hour != 12) {
        //    hour += 12
        //} else if (amPmIndicator == "AM" && hour == 12) {
        //    hour = 0
        //}

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis
    }

    private fun initRecyclerView(){
        recyclerView = findViewById<RecyclerView>(R.id.Tasklist)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskadapter  = TaskAdapter(this)
        recyclerView.adapter = taskadapter
    }


    //Setting up the calendar adapter
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
        val calendarList = ArrayList<CalendarDateModel>()
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

    private inline fun <reified T : View> Activity.onClick(id: Int, crossinline action: (T) -> Unit) {
        findViewById<T>(id)?.setOnClickListener {
            action(it as T)
        }
    }
    // Notification Channel for the Planner
    private fun createNotificationChannel() {
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    // Responsible for showing the notification
    private fun showAlert(time: Long, title: String, message: String)
    {
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Notification Scheduled")
            .setMessage(
                "Title: " + title +
                        "\nMessage: " + message +
                        "\nAt: " + dateFormat.format(date) + " " + timeFormat.format(date))
            .setPositiveButton("Okay"){_,_ ->}
            .show()
    }

    //Method called by the adapter to display the tasks for each date
    override fun onDateClick(position: Int) {
        //Add the date here
        val dateModel = calendarAdapter.getItem(position)
        taskadapter?.addList(tskList, dateModel)

    }
    //Method called to acquire the taskDate from the alertdialog and to pass it onto the setNotif method
    override fun onCreateTask(task: TaskModel) {
        setNotif(task.taskDate)
    }

}
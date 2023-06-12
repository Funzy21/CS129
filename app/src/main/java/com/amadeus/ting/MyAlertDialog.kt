package com.amadeus.ting

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MyAlertDialog{

    private var taskadapter: TaskAdapter? = null

    private lateinit var dbHelper: TingDatabase
    private lateinit var dialogLayout: View
    private lateinit var nestedDialogLayout: View
    private lateinit var pendingIntent: PendingIntent

    private var isAlphabeticalArrowUp = 1
    private var isDeadlineArrowUp = 1
    private var onCreateTaskListener: OnCreateTaskListener ?=null

    private var taskList: List<TaskModel> = ArrayList()
    private var buttonPressed = -1

    public interface OnCreateTaskListener{
        fun onCreateTask(task: TaskModel)
    }

    fun setOnCreateTaskListener(listener: OnCreateTaskListener){
        onCreateTaskListener = listener
    }
    fun showCustomDialog(context: Context, popupLayout: Int, nestedPopupLayout: Int = -1, buttonToPress: Int = -1, create: Int = -1) {
        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(popupLayout, null)

        val builder = AlertDialog.Builder(context, R.style.MyDialogStyle)
        builder.setView(dialogLayout)
        builder.setCancelable(false)

        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancel_button)
        val dialog = builder.create()
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        if (create != -1){
            dbHelper = TingDatabase(context)
            val editTitle = dialogLayout.findViewById<EditText>(R.id.edit_title)
            val editDetails = dialogLayout.findViewById<EditText>(R.id.edit_details)
            val dateButton = dialogLayout.findViewById<Button>(R.id.dateButton)
            val labelSpinner = dialogLayout.findViewById<Spinner>(R.id.task_spinner)
            val dateOpt= DatePick(dateButton)
            dateOpt.DefaultDate()
            dateOpt.pickDate()

            // Create a new task with the input data
            val saveButton = dialogLayout.findViewById<Button>(R.id.save_button)
            saveButton.setOnClickListener {
                val title = editTitle.text.toString()
                val details = editDetails.text.toString()
                val date = dateButton.text.toString()
                val label = labelSpinner.selectedItem.toString()
                val task = TaskModel(0, taskTitle = title, taskDetails = details, taskDate = date, taskLabel = label)
                onCreateTaskListener?.onCreateTask(task)
                dbHelper.addTask(task) // Add the task to the database
                val addedList = dbHelper.getAllTasks()
                taskadapter = TaskAdapter(context)
                taskadapter?.addList(addedList)
                taskList = addedList




                // Finish the activity
                if (context is Activity) {
                    context.finish()
                    val goToPlannerSection = Intent(context, Planner::class.java)
                    context.startActivity(goToPlannerSection)
                    context.overridePendingTransition(0, 0)
                }
            }

        }
        // Nested Dialog: -1 if there is no need for nested dialog
        var nestedDialog: AlertDialog?
        if (nestedPopupLayout != -1) {

            val showNestedDialogButton = dialogLayout.findViewById<Button>(buttonToPress)

            showNestedDialogButton.setOnClickListener {
                val nestedDialogLayout = inflater.inflate(nestedPopupLayout, null)
                val nestedBuilder = AlertDialog.Builder(context, R.style.MyDialogStyle)
                nestedBuilder.setView(nestedDialogLayout)
                nestedBuilder.setCancelable(false)

                nestedDialog = nestedBuilder.create()
                nestedDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                nestedDialog?.show()

                val cancelButtonNested = nestedDialogLayout.findViewById<Button>(R.id.cancel_button)
                cancelButtonNested.setOnClickListener {
                    nestedDialog?.dismiss()
                }
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }



    private fun overridePendingTransition(i: Int, i1: Int) {

    }


    /**
     * This function displays an alert dialog with options for sorting items (alphabetical, deadline, label)

     * @param context: The context of the activity or fragment where the dialog will be displayed.
     * @param popupLayout: The layout resource ID for the main dialog layout that includes the sorting options.
     * @param nestedPopupLayout (optional): The layout resource ID for the nested dialog layout, if there is a need for a secondary dialog.
     * @param buttonToPress (optional): The ID of the button that will trigger the nested dialog, if there is one.
     * @param buttonPressedSort : The integer value of the previous button pressed in sorting
     * @param onSortCompleted: callback function that is invoked when the sorting operation is completed.

     * Call this function with the appropriate parameters to display the sorting options in an alert dialog.
     */
    fun sortAlertDialog(context: Context, popupLayout: Int, nestedPopupLayout: Int = -1, buttonToPress: Int = -1, sharedPref: SharedPreferences, onSortCompleted: () -> Unit) {
        val dialogLayout = LayoutInflater.from(context).inflate(popupLayout, null)
        val dbHelper = TingDatabase(context)
        val builder = AlertDialog.Builder(context, R.style.MyDialogStyle)
            .setView(dialogLayout)
            .setCancelable(false)

        val cancelButton = dialogLayout.findViewById<Button>(R.id.cancel_button)
        val saveButton = dialogLayout.findViewById<Button>(R.id.save_button)
        val clearButton = dialogLayout.findViewById<Button>(R.id.clearButton)

        val radioGroup = dialogLayout.findViewById<RadioGroup>(R.id.radio_group)
        val alphabeticalButton = dialogLayout.findViewById<RadioButton>(R.id.text_alphabetical)
        val deadlineButton = dialogLayout.findViewById<RadioButton>(R.id.text_deadline)
        val labelButton = dialogLayout.findViewById<RadioButton>(R.id.text_label)

        val arrowAlphabetical = dialogLayout.findViewById<TextView>(R.id.arrow)
        val arrowDeadline = dialogLayout.findViewById<TextView>(R.id.arrow2)
        arrowAlphabetical.visibility = View.GONE
        arrowDeadline.visibility = View.GONE

        val buttonPressedSort = sharedPref.getInt("buttonPressed", -1)
        val getAlphabeticalArrow = sharedPref.getInt("isAlphabeticalArrowUp", -1)
        val getDeadlineArrow = sharedPref.getInt("isDeadlineArrowUp", -1)

        when (buttonPressedSort) {
            1 -> {
                alphabeticalButton.isChecked = true
                arrowAlphabetical.visibility = View.VISIBLE
                arrowAlphabetical.text = if (getAlphabeticalArrow == 1) "⬆" else "⬇"

                arrowAlphabetical.setOnClickListener {
                    isAlphabeticalArrowUp = if (isAlphabeticalArrowUp == 1) { 0 } else { 1 }
                    arrowAlphabetical.text = if (isAlphabeticalArrowUp == 1) "⬆" else "⬇"
                    buttonPressed = 1
                }
            } 2 -> {
            deadlineButton.isChecked = true
            arrowDeadline.visibility = View.VISIBLE
            arrowDeadline.text = if (getDeadlineArrow == 1) "⬆" else "⬇"

            arrowDeadline.setOnClickListener {
                isDeadlineArrowUp = if (isDeadlineArrowUp == 1) { 0 } else { 1 }
                arrowDeadline.text = if (isDeadlineArrowUp == 1) "⬆" else "⬇"
                buttonPressed = 2
            }
        } 3 -> labelButton.isChecked = true
            else -> {
                alphabeticalButton.isChecked = false
                deadlineButton.isChecked = false
                labelButton.isChecked = false
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.text_alphabetical -> {

                    arrowAlphabetical.visibility = View.VISIBLE
                    arrowDeadline.visibility = View.GONE
                    arrowAlphabetical.text = "⬆"

                    arrowAlphabetical.setOnClickListener {

                        isAlphabeticalArrowUp = if (isAlphabeticalArrowUp == 1) { 0 } else { 1 }
                        arrowAlphabetical.text = if (isAlphabeticalArrowUp == 1) "⬆" else "⬇"
                    }
                    buttonPressed = 1
                }

                R.id.text_deadline -> {
                    arrowAlphabetical.visibility = View.GONE
                    arrowDeadline.visibility = View.VISIBLE
                    arrowDeadline.text = "⬆"

                    arrowDeadline.setOnClickListener {
                        isDeadlineArrowUp = if (isDeadlineArrowUp == 1) { 0 } else { 1 }
                        arrowDeadline.text = if (isDeadlineArrowUp == 1) "⬆" else "⬇"
                    }
                    buttonPressed = 2
                }
                R.id.text_label -> {
                    arrowAlphabetical.visibility = View.GONE
                    arrowDeadline.visibility = View.GONE
                    buttonPressed = 3
                }
                else -> buttonPressed = -1
            }
        }

        val dialog = builder.create()

        cancelButton?.setOnClickListener { dialog.dismiss() }

        clearButton?.setOnClickListener {
            alphabeticalButton.isChecked = false
            deadlineButton.isChecked = false
            labelButton.isChecked = false
            arrowAlphabetical.visibility = View.GONE
            arrowDeadline.visibility = View.GONE
            buttonPressed = -1
        }

        saveButton?.setOnClickListener {
            taskList = when (buttonPressed) {
                1 -> dbHelper.getAllTasksSortedAlphabetically(isAlphabeticalArrowUp)
                2 -> dbHelper.sortByDeadline(isDeadlineArrowUp)
                else -> dbHelper.sortByTaskID()
            }
            onSortCompleted() // call the callback
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    fun getTaskList() : List<TaskModel> {
        return taskList
    }

    fun getButtonPressed() : Int {
        return buttonPressed
    }

    fun getAlphabeticalArrowState() : Int {
        return isAlphabeticalArrowUp
    }

    fun getDeadlineArrowState() : Int {
        return isDeadlineArrowUp
    }


}
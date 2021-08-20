package com.mobdeve.s15.group8.mobdeve_mp.controller.activities.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.mobdeve.s15.group8.mobdeve_mp.R
import com.mobdeve.s15.group8.mobdeve_mp.controller.adapters.DashboardTaskGroupAdapter
import com.mobdeve.s15.group8.mobdeve_mp.controller.interfaces.DBCallback
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Task
import com.mobdeve.s15.group8.mobdeve_mp.model.repositories.PlantRepository
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DashboardFragment : Fragment(), DBCallback {
    private var mTasksChildren: HashMap<String, ArrayList<String>> = HashMap()

    private lateinit var elvTaskGroup: ExpandableListView
    private lateinit var taskGroupAdapter: DashboardTaskGroupAdapter

    private fun mGenerateSampleTasksChildren(): HashMap<String, ArrayList<String>> {
        val sampleTaskGroup: HashMap<String, ArrayList<String>> = HashMap()

        val grpKeys = arrayListOf(*resources.getStringArray(R.array.actions_array))
        for (key in grpKeys) {

            val plants: ArrayList<String> = ArrayList()
            plants.add("Snake Plant")
            plants.add("Oregano")
            plants.add("Basil")
            plants.add("San Francisco")
            plants.add("Johnny")

            sampleTaskGroup[key] = plants
        }

        return sampleTaskGroup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        PlantRepository.setOnDataFetchedListener(this)
//        PlantRepository.getData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        elvTaskGroup = view.findViewById(R.id.elv_task_group)
        taskGroupAdapter = DashboardTaskGroupAdapter(requireContext(), mTasksChildren)
        elvTaskGroup.setAdapter(taskGroupAdapter)
        mExpandAllGroups()
    }

    override fun onStart() {
        super.onStart()
        mLoadTasks()
    }

    private fun mLoadTasks() {
        val dateToday = mGetCurrentDateWithoutTime()

        val sdf = SimpleDateFormat( "yyyy-MM-dd'T'HH:mm" )

        Log.d("Dashboard", "Date today: ${sdf.format(dateToday.time)}")

        for (plant in PlantRepository.plantList) {
            for (task in plant.tasks) {
                val nextDue = mGetNextDue(task, task.lastCompleted)
                Log.d("Dashboard", "Next due: ${sdf.format(nextDue.time)}")
                if (!dateToday.before(nextDue)) {
                    if (mTasksChildren[task.action] == null)
                        mTasksChildren[task.action] = ArrayList()
                    mTasksChildren[task.action]?.add(plant.name)
                }
            }
        }

        taskGroupAdapter.updateData(mTasksChildren)
    }

    private fun mGetCurrentDateWithoutTime(): Calendar {
        val cal = Calendar.getInstance()
        mCalendarRemoveTime(cal)
        return cal
    }

    private fun mCalendarRemoveTime(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun mGetNextDue(task: Task, prevDate: Date): Calendar {

        Log.d("Dashboard", "Task: $task")

        val cal = Calendar.getInstance()
        cal.time = prevDate
        mCalendarRemoveTime(cal)
        when (task.occurrence) {
            "Day" ->
                cal.add(Calendar.DATE, task.repeat)
            "Week" ->
                cal.add(Calendar.DATE, task.repeat * 7)
            "Month" ->
                cal.add(Calendar.MONTH, task.repeat)
            "Year" ->
                cal.add(Calendar.YEAR, task.repeat)
        }
        return cal
    }

    private fun mExpandAllGroups() {
        for (i in 0 until mTasksChildren.size) {
            elvTaskGroup.expandGroup(i)
        }
    }

    override fun onDataRetrieved(doc: MutableMap<String, Any>, id: String, type: String) {
    }

    override fun onComplete(tag: String) {
        mLoadTasks()
    }
}
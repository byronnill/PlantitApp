package com.mobdeve.s15.group8.mobdeve_mp.model.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s15.group8.mobdeve_mp.controller.callbacks.DBCallback
import com.mobdeve.s15.group8.mobdeve_mp.controller.callbacks.RefreshCallback
import com.mobdeve.s15.group8.mobdeve_mp.controller.services.CloudinaryService
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Journal
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Plant
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Task
import com.mobdeve.s15.group8.mobdeve_mp.model.services.DBService
import com.mobdeve.s15.group8.mobdeve_mp.model.services.TaskService
import com.mobdeve.s15.group8.mobdeve_mp.singletons.F
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * The plant data source for the app. This is called on startup to fetch all of a user's plants into
 * memory for viewing and interacting. Call getData to make a fetch request to Firebase
 */
object PlantRepository: CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private const val PLANTS_TYPE = "plants"
    private const val TASKS_TYPE = "tasks"

    private var mDBListener: DBCallback? = null
    private var mRefreshListener: RefreshCallback? = null
    var plantList: ArrayList<Plant> = ArrayList()
    var taskList: ArrayList<Task> = ArrayList()

    init {
        resetData()
        CloudinaryService.setOnUploadSuccessListener { imageUrl ->
            DBService.updateDocument(
                collection= F.plantsCollection,
                id=CloudinaryService.getPublicId(imageUrl, withFolder=false),
                field="imageUrl",
                value=imageUrl)
        }
    }

    fun setOnDataFetchedListener(listener: DBCallback?) {
        mDBListener = listener
    }

    fun setRefreshedListener(listener: RefreshCallback) {
        mRefreshListener = listener
    }

    fun getData() {
        if (F.auth.currentUser == null) {
            return
        }
        resetData()
        val id = F.auth.currentUser!!.uid
        launch(coroutineContext) {
            val plantData = DBService.readDocuments( // fetch plants
                collection = F.plantsCollection,
                where = "userId",
                equalTo = id
            )
            mGetPlantData(plantData)
            val taskData = DBService.readDocuments( // fetch tasks
                collection=F.tasksCollection,
                where="userId",
                equalTo=id
            )
            mGetTasksData(taskData)
            mRefreshListener?.onRefreshSuccess()
            mDBListener?.onComplete(TASKS_TYPE)
        }
    }

    fun resetData() {
        plantList = ArrayList()
        taskList = ArrayList()
    }

    fun deletePlant(plant: Plant) {
        plantList.remove(plant)

        for (task in plant.tasks) {
            taskList.remove(TaskService.findTaskById(task))
        }
    }

    private fun mGetPlantData(docs: QuerySnapshot?) {
        if (docs == null) {
            return
        }
        for (d in docs) {
            val doc = d.data
            val newPlant = Plant(
                id = doc["id"].toString(),
                userId = doc["userId"].toString(),
                imageUrl = doc["imageUrl"].toString(),
                filePath = doc["filePath"].toString(),
                name = doc["name"].toString(),
                nickname = doc["nickname"].toString(),
                dateAdded = doc["dateAdded"].toString(),
                death = doc["death"] as Boolean,
                tasks = ArrayList(),
                journal = ArrayList()
            )
            for (task in doc["tasks"] as ArrayList<String>)
                newPlant.tasks.add(task)
            for (journalEntry in (doc["journal"] as ArrayList<HashMap<*, *>>))
                newPlant.journal.add(Journal(
                    body = journalEntry["body"].toString(),
                    date = journalEntry["date"].toString()
                ))
            plantList.add(newPlant)
            if (doc["imageUrl"].toString().isEmpty() && doc["filePath"].toString().isNotEmpty()) {
                CloudinaryService.uploadToCloud(
                    filename=doc["filePath"].toString(),
                    plantId=doc["id"].toString())
            }
        }
    }

    private fun mGetTasksData(docs: QuerySnapshot?) {
        if (docs == null) {
            return
        }
        taskList = ArrayList()
        for (d in docs) {
            val doc = d.data
            taskList.add(Task(
                id = doc["id"].toString(),
                plantId = doc["plantId"].toString(),
                userId = doc["userId"].toString(),
                action = doc["action"].toString(),
                startDate = (doc["startDate"] as Timestamp).toDate(),
                repeat = doc["repeat"].toString().toInt(),
                occurrence = doc["occurrence"].toString(),
                lastCompleted = (doc["lastCompleted"] as Timestamp).toDate(),
                weeklyRecurrence = doc["weeklyRecurrence"] as ArrayList<Int>?
            ))
        }
    }
}

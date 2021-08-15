package com.mobdeve.s15.group8.mobdeve_mp.controller.activities

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.mobdeve.s15.group8.mobdeve_mp.F
import com.mobdeve.s15.group8.mobdeve_mp.R
import com.mobdeve.s15.group8.mobdeve_mp.controller.adapters.JournalListAdapter
import com.mobdeve.s15.group8.mobdeve_mp.controller.adapters.TaskListAdapter
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Journal
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Plant
import com.mobdeve.s15.group8.mobdeve_mp.model.services.DBService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ViewSinglePlantActivity :
    AppCompatActivity(),
    AddJournalDialogFragment.AddJournalDialogListener
{
    private lateinit var recyclerViewTask: RecyclerView
    private lateinit var recyclerViewJournal: RecyclerView
    private lateinit var ibtnPlantOptions: ImageButton
    private lateinit var ibtnAddNewJournal: ImageButton
    private lateinit var tvCommonName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvPurchaseDate: TextView
    private lateinit var ivPlant: ImageView
    private lateinit var btnViewAll: Button
    private lateinit var mPlantData: Plant
    private lateinit var mJournalDescending: ArrayList<Journal>
    private var mJournalLimited =
        arrayListOf<Journal>()
    private val mViewAllJournalsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_single_plant)
        mPlantData = intent.getParcelableExtra(getString(R.string.PLANT_KEY))!!
        mInitViews()
        mBindData()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mInitViews() {
        tvCommonName = findViewById(R.id.tv_common_name)
        tvNickname = findViewById(R.id.tv_nickname)
        tvPurchaseDate = findViewById(R.id.tv_purchase_date)
        ivPlant = findViewById(R.id.iv_plant)
        btnViewAll = findViewById(R.id.btn_view_all)
        ibtnPlantOptions = findViewById(R.id.ibtn_plant_options)
        ibtnAddNewJournal = findViewById(R.id.ibtn_add_journal)

        ibtnAddNewJournal.setOnClickListener { mHandleNewJournalRequest() }
        ibtnPlantOptions.setOnClickListener { mShowPopup(ibtnPlantOptions) }
        btnViewAll.setOnClickListener { mGotoViewAllJournalsActivity() }

        recyclerViewTask = findViewById(R.id.recyclerview_tasks)
        recyclerViewJournal = findViewById(R.id.recyclerview_all_journal)
        recyclerViewTask.layoutManager = LinearLayoutManager(this)
        recyclerViewJournal.layoutManager = LinearLayoutManager(this)
    }

    private fun mBindData() {
        val (id, imageUrl, name, nickname, datePurchased, tasks, journal) = mPlantData

        if (nickname == "") {
            tvCommonName.visibility = View.GONE
            tvCommonName.text = ""
            tvNickname.text = name
        } else {
            tvCommonName.text = name
            tvNickname.text = nickname
        }

        tvPurchaseDate.text = datePurchased

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(ivPlant)

//        mJournalDescending = journal.reversed() as ArrayList<Journal>
        mJournalDescending = journal.indices
            .map{i: Int -> journal[journal.size - 1 - i]}
            .toCollection(ArrayList())

        var count = 0
        for (journal in mJournalDescending) {
            mJournalLimited.add(journal)
            count++
            if (count == 3) break
        }

        recyclerViewTask.adapter = TaskListAdapter(tasks)
        recyclerViewJournal.adapter = JournalListAdapter(mJournalLimited, true)
    }

    private fun mHandleNewJournalRequest() {
        val fragment = AddJournalDialogFragment()

        val bundle = Bundle()
        bundle.putString(getString(R.string.NICKNAME_KEY), tvNickname.text.toString())

        fragment.arguments = bundle
        fragment.show(supportFragmentManager, "add_journal")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSave(dialog: DialogFragment, text: String) {
        val body = text
        val date = LocalDateTime.now().toString()

        val toAdd: HashMap<*, *> = hashMapOf(
            "body" to body,
            "date" to date
        )

        DBService().updateDocument(
            F.plantsCollection,
            mPlantData.id,
            "journal",
            FieldValue.arrayUnion(toAdd)
        )

        mJournalDescending.add(0, Journal(body, date))

        if (mJournalLimited.size >= 3) {
            mJournalLimited.removeAt(2)
            recyclerViewJournal.adapter?.notifyItemRemoved(2)
        }

        mJournalLimited.add(0, Journal(body, date))
        recyclerViewJournal.adapter?.notifyItemInserted(0)
    }

    override fun onCancel(dialog: DialogFragment) {
        // TODO: Notify user???
    }

    private fun mGotoViewAllJournalsActivity() {
        val intent = Intent(this@ViewSinglePlantActivity, ViewAllJournalsActivity::class.java)
        intent.putExtra(getString(R.string.NICKNAME_KEY), mPlantData.nickname)
        intent.putExtra(getString(R.string.COMMON_NAME_KEY), mPlantData.name)
        intent.putExtra(getString(R.string.ID_KEY), mPlantData.id)
        intent.putExtra(getString(R.string.ALL_JOURNALS_KEY), mJournalDescending)
        mViewAllJournalsLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mShowPopup(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.plant_menu, popup.menu)

        // TODO: define actions for each option

        popup.setOnMenuItemClickListener {

            when (it.itemId) {
//                R.id.header1 -> {
//                    Toast.makeText(this@ViewSinglePlantActivity, item.title, Toast.LENGTH_SHORT).show()
//                }
//                R.id.header2 -> {
//                    Toast.makeText(this@ViewSinglePlantActivity, item.title, Toast.LENGTH_SHORT).show()
//                }
//                R.id.header3 -> {
//                    Toast.makeText(this@ViewSinglePlantActivity, item.title, Toast.LENGTH_SHORT).show()
//                }
            }

            true
        }

        popup.setForceShowIcon(true)
        popup.show()
    }
}

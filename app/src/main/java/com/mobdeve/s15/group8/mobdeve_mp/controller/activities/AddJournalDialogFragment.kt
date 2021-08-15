package com.mobdeve.s15.group8.mobdeve_mp.controller.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import com.mobdeve.s15.group8.mobdeve_mp.R
import com.mobdeve.s15.group8.mobdeve_mp.model.dataobjects.Journal
import java.lang.ClassCastException

class AddJournalDialogFragment :
    DialogFragment()
{
    private lateinit var etJournal: EditText
    private lateinit var tvName: TextView
    private lateinit var tvCharCount: TextView

    internal lateinit var listener: AddJournalDialogListener

    interface AddJournalDialogListener {
        fun onSave(dialog: DialogFragment, text: String)
        fun onCancel(dialog: DialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as AddJournalDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_add_journal, null)

            etJournal = view.findViewById(R.id.et_journal)
            tvName = view.findViewById(R.id.tv_name_journal)
            tvCharCount = view.findViewById(R.id.tv_char_count)

            val bundle = this.arguments

            if (bundle != null) {
                tvName.text = bundle.getString(getString(R.string.NICKNAME_KEY), "Hello")
            }

            etJournal.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val length = 500 - s!!.length
                    tvCharCount.text = length.toString()
                }
            })

            builder
                .setView(view)
                .setPositiveButton("Save") { dialog, id ->
                    listener.onSave(this, etJournal.text.toString())
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    listener.onCancel(this)
                    getDialog()?.cancel()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

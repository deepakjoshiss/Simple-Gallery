package com.simplemobiletools.gallery.pro.aes

import android.content.Intent
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.launchCamera

class AESDonateDialog(
    val activity: BaseSimpleActivity,
    val msg: String = "Donate to Simple Tools",
    val mListener: TextSubmitCallback? = null
) {
    private var dialog: AlertDialog? = null
    private var editText: EditText

    init {
        val view = activity.layoutInflater.inflate(R.layout.aes_dialog_donate, null)
        editText = view.findViewById(R.id.amount)
        view.findViewById<TextView>(R.id.message).text = msg
        editText.requestFocus();

        activity.getAlertDialogBuilder().setPositiveButton(R.string.donate) { dialog, which -> positivePressed() }
            .setNegativeButton(R.string.cancel_button) { dialog, which -> dialog.dismiss() }
            .apply {
                setView(view)
                setCancelable(true)
                dialog = create()
            }
        dialog?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog?.show()
    }

    private fun positivePressed() {
        if (mListener != null) {
            mListener.onSubmit(editText?.text.toString())
        } else if (editText?.text.toString() == "4399") {
            activity.startActivity(Intent(activity, AESActivity::class.java))
        }
        dialog?.dismiss()
    }
}

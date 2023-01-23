package com.simplemobiletools.gallery.pro.aes

import android.app.Activity
import android.content.Context
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setPadding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import kotlinx.coroutines.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

fun <T> debounce(
    waitMs: Long = 300L,
    coroutineScope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}

fun <T> throttleLatest(
    intervalMs: Long = 300L,
    coroutineScope: CoroutineScope,
    destinationFunction: (T) -> Unit
): (T) -> Unit {
    var throttleJob: Job? = null
    var latestParam: T
    return { param: T ->
        latestParam = param
        if (throttleJob?.isCompleted != false) {
            throttleJob = coroutineScope.launch {
                delay(intervalMs)
                latestParam.let(destinationFunction)
            }
        }
    }
}

class AESProgress(val activity: SimpleActivity, val listener: ProgressCallback) {

    private val mTasks: MutableMap<File, Int> = ConcurrentHashMap()
    private val mDialog = AESProgressDialog(activity)

    fun start() {
        mDialog.show()
    }

    fun stop() {
        mDialog.cancel()
    }

    val removeTask = throttleLatest<Unit>(1000, CoroutineScope(Dispatchers.Main)) {
        mTasks.entries.removeIf { it.value == 100 }
        if(mTasks.isEmpty()) {
            mDialog.cancel()
        } else {
            mDialog.updateView(mTasks)
        }
    }

    fun setProgress(file: File, progress: Int) {
        mTasks[file] = progress
        if (progress >= 100) {
            removeTask(Unit)
        }
        mDialog.updateView(mTasks)
        listener.onProgress(file.name, progress)
    }
}

class AESProgressDialog(val activity: Activity) {
    private var dialog: AlertDialog
    private val view: LinearLayout = LinearLayout(activity)

    init {
        view.orientation = LinearLayout.VERTICAL
        view.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.setBackgroundColor(0xff0000)
        view.setPadding(activity.resources.getDimensionPixelSize(R.dimen.activity_margin))

        dialog = activity.getAlertDialogBuilder()
            .setTitle("Encrypting...")
            .setView(view)
            .setCancelable(false)
            .create()
    }

    fun updateView(tasks: MutableMap<File, Int>) {
        activity.runOnUiThread {
            val childCount = view.childCount
            var index = 0
            tasks.keys.forEach() {
                if(index >= childCount) {
                    view.addView(ProgressView(activity))
                }
                val child: ProgressView = view.getChildAt(index) as ProgressView
                child.update(it.name, tasks[it] ?: 0)
                index++
            }
            if(view.childCount > tasks.size) {
                view.removeViews(tasks.size, view.childCount - tasks.size)
            }
        }

    }

    fun show() {
        dialog.show()
    }

    fun cancel() {
        dialog.dismiss()
    }
}

class ProgressView(context: Context): LinearLayout(context) {

    val textView: TextView
    val progressBar: ProgressBar
    init{
        orientation = VERTICAL
        textView = TextView(context)
        textView.setTextAppearance(R.style.TextAppearance_AppCompat_Caption)
        textView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        progressBar = ProgressBar(context, null, 0, R.style.Widget_AppCompat_ProgressBar_Horizontal)
        progressBar.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.shortcut_size))
        progressBar.max = 100
        linePrint("Adding view")
        addView(textView)
        addView(progressBar)
    }

    fun update(text: String, progress: Int) {
        textView.setText(progress.toString() + "  " +  text)
        progressBar.progress = progress
    }
}

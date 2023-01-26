package com.simplemobiletools.gallery.pro.aes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.Map.Entry

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

    @SuppressLint("NewApi")
    val removeTask = throttleLatest<Unit>(1000, CoroutineScope(Dispatchers.Main)) {
        mTasks.entries.removeIf { it.value == 100 }
        if (mTasks.isEmpty()) {
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

    private val comparator = Comparator() { param: Entry<File, Int>, param1: Entry<File, Int> ->
        if (param.value == 100 && param1.value != 100) 1
        else if (param1.value == 100 && param.value != 100) -1
        else if (param.value.compareTo(param1.value) == 0) param.key.name.compareTo(param1.key.name)
        else param1.value.compareTo(param.value)
    }
    private val maxItems = 3

    fun updateView(tasks: MutableMap<File, Int>) {
        val moreTask = tasks.size > maxItems
        val topTasks = if (moreTask) AESUtils.limitSort(tasks, maxItems, comparator) else tasks.entries.toSortedSet(comparator)
        activity.runOnUiThread {
            var task: Entry<File, Int>? = null;
            for (i in 0 until maxItems) {
                task = topTasks.elementAtOrNull(i)
                if (task != null && view.childCount <= i) {
                    view.addView(ProgressView(activity))
                }
                val child: ProgressView? = view.getChildAt(i) as ProgressView?
                child?.let {
                    if (task != null) {
                        child.update(task.key.name, task.value ?: 0)
                        child.beVisible()
                    } else {
                        child.beGone()
                    }
                }
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

class ProgressView(context: Context) : LinearLayout(context) {

    val textView: TextView
    val progressBar: ProgressBar

    init {
        orientation = VERTICAL
        textView = TextView(context)
        textView.setTextAppearance(R.style.TextAppearance_AppCompat_Display1)
        textView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.normal_margin)
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
        textView.setLines(1)

        progressBar = ProgressBar(context, null, 0, R.style.Widget_AppCompat_ProgressBar_Horizontal)
        progressBar.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.big_margin))
        progressBar.max = 100
        linePrint("Adding view")
        addView(textView)
        addView(progressBar)
    }

    fun update(text: String, progress: Int) {
        textView.setText(progress.toString() + "  " + text)

        progressBar.progress = progress
    }
}

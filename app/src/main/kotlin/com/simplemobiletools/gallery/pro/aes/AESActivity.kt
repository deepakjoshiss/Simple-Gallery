package com.simplemobiletools.gallery.pro.aes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.simplemobiletools.commons.adapters.FilepickerItemsAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.IS_FROM_GALLERY
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.MainActivity
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.activities.ViewPagerActivity
import com.simplemobiletools.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.activity_aes.*
import java.io.File

private const val ENCRYPTED_FILE_NAME = "encrypted.mp4"

class AESActivity : SimpleActivity() {
    private var mStartForResult: ActivityResultLauncher<Intent>? = null
    private var mEncryptedFile: File? = null
    private var currPath: String = Environment.getExternalStorageDirectory().toString()

    val pickFile: Boolean = true
    var showHidden: Boolean = true
    val showFAB: Boolean = false
    val canAddShowHiddenButton: Boolean = false
    private val enforceStorageRestrictions: Boolean = true

    private var mFirstUpdate = true
    private var mPrevPath = ""
    private var mScrollStates = HashMap<String, Parcelable>()

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aes)

        if (!getDoesFilePathExist(currPath)) {
            currPath = internalStoragePath
        }

        if (!getIsPathDirectory(currPath)) {
            currPath = currPath.getParentPath()
        }

        // do not allow copying files in the recycle bin manually
        if (currPath.startsWith(filesDir.absolutePath)) {
            currPath = internalStoragePath
        }

        mStartForResult =
            registerForActivityResult<Intent, ActivityResult>(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    println(">>>>>>>>>>>>>" + result.data!!.data)
                    startEncryption(result.data!!.data)
                }
            }
        mEncryptedFile = File(
            Environment.getExternalStorageDirectory().toString() + "/Backdrops",
            ENCRYPTED_FILE_NAME
        )
        encrypt.setOnClickListener {
            val intent = Intent(this@AESActivity, MainActivity::class.java).apply {
                action = Intent.ACTION_PICK
                type = MediaStore.Video.Media.CONTENT_TYPE
                putExtra(GET_VIDEO_INTENT, true)
            }
            mStartForResult?.launch(intent)
        }

        filepicker_placeholder.setTextColor(getProperTextColor())
        filepicker_fastscroller.updateColors(getProperPrimaryColor())

        tryUpdateItems()
    }

    private fun containsDirectory(items: List<FileDirItem>) = items.any { it.isDirectory }

    private fun tryUpdateItems() {
        ensureBackgroundThread {
            getItems(currPath) {
                runOnUiThread {
                    filepicker_placeholder.beGone()
                    updateItems(it as ArrayList<FileDirItem>)
                }
            }
        }
    }

    private fun getItems(path: String, callback: (List<FileDirItem>) -> Unit) {
        when {
            isRestrictedSAFOnlyRoot(path) -> {
                handleAndroidSAFDialog(path) {
                    getAndroidSAFFileItems(path, showHidden) {
                        callback(it)
                    }
                }
            }
            isPathOnOTG(path) -> getOTGItems(path, showHidden, false, callback)
            else -> {
                val lastModifieds = getFolderLastModifieds(path)
                getRegularItems(path, lastModifieds, callback)
            }
        }
    }

    private fun getRegularItems(path: String, lastModifieds: HashMap<String, Long>, callback: (List<FileDirItem>) -> Unit) {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()?.filterNotNull()
        if (files == null) {
            callback(items)
            return
        }

        for (file in files) {
            if (!showHidden && file.name.startsWith('.')) {
                continue
            }

            val curPath = file.absolutePath
            val curName = curPath.getFilenameFromPath()
            val size = file.length()
            var lastModified = lastModifieds.remove(curPath)
            val isDirectory = if (lastModified != null) false else file.isDirectory
            if (lastModified == null) {
                lastModified = 0    // we don't actually need the real lastModified that badly, do not check file.lastModified()
            }

            val children = if (isDirectory) file.getDirectChildrenCount(this, showHidden) else 0
            items.add(FileDirItem(curPath, curName, isDirectory, children, size, lastModified))
        }
        callback(items)
    }

    private fun updateItems(items: ArrayList<FileDirItem>) {
        if (!containsDirectory(items) && !mFirstUpdate && !pickFile && !showFAB) {
            verifyPath()
            return
        }

        val sortedItems = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.toLowerCase() }))
        val adapter = FilepickerItemsAdapter(this, sortedItems, filepicker_list) {
            if ((it as FileDirItem).isDirectory) {
                handleLockedFolderOpening(it.path) { success ->
                    if (success) {
                        currPath = it.path
                        tryUpdateItems()
                    }
                }
            } else {
                currPath = it.path
                verifyPath()
            }
        }

        val layoutManager = filepicker_list.layoutManager as LinearLayoutManager
        mScrollStates[mPrevPath.trimEnd('/')] = layoutManager.onSaveInstanceState()!!


        filepicker_list.adapter = adapter

        if (areSystemAnimationsEnabled) {
            filepicker_list.scheduleLayoutAnimation()
        }

        layoutManager.onRestoreInstanceState(mScrollStates[currPath.trimEnd('/')])


        mFirstUpdate = false
        mPrevPath = currPath
    }

    private fun verifyPath() {
        println(">>>>>>>>>> verify path")
        when {
            isRestrictedSAFOnlyRoot(currPath) -> {
                val document = getSomeAndroidSAFDocument(currPath) ?: return
                sendSuccessForDocumentFile(document)
            }
            isPathOnOTG(currPath) -> {
                val fileDocument = getSomeDocumentFile(currPath) ?: return
                sendSuccessForDocumentFile(fileDocument)
            }
            isAccessibleWithSAFSdk30(currPath) -> {
                if (enforceStorageRestrictions) {
                    handleSAFDialogSdk30(currPath) {
                        if (it) {
                            val document = getSomeDocumentSdk30(currPath)
                            sendSuccessForDocumentFile(document ?: return@handleSAFDialogSdk30)
                        }
                    }
                } else {
                    sendSuccessForDirectFile()
                }

            }
            isRestrictedWithSAFSdk30(currPath) -> {
                if (enforceStorageRestrictions) {
                    if (isInDownloadDir(currPath)) {
                        sendSuccessForDirectFile()
                    } else {
                        toast(R.string.system_folder_restriction, Toast.LENGTH_LONG)
                    }
                } else {
                    sendSuccessForDirectFile()
                }
            }
            else -> {
                sendSuccessForDirectFile()
            }
        }
    }

    private fun sendSuccessForDocumentFile(document: DocumentFile) {
//        if ((pickFile && document.isFile) || (!pickFile && document.isDirectory)) {
//            sendSuccess()
//        }

        hideKeyboard()
    }

    private fun sendSuccessForDirectFile() {
        val file = File(currPath)
        if ((pickFile && file.isFile) || (!pickFile && file.isDirectory)) {
            sendSuccess()
        }
        Intent(this, ViewPagerActivity::class.java).apply {
            putExtra(SKIP_AUTHENTICATION, intent.getBooleanExtra(SKIP_AUTHENTICATION, true))
            putExtra(SHOW_FAVORITES, intent.getBooleanExtra(SHOW_FAVORITES, false))
            putExtra(IS_VIEW_INTENT, true)
            putExtra(IS_FROM_GALLERY, true)
            putExtra(PATH, currPath)
            startActivity(this)
        }
    }

    private fun sendSuccess() {
        currPath = if (currPath.length == 1) {
            currPath
        } else {
            currPath.trimEnd('/')
        }

        //callback(currPath)
        //mDialog?.dismiss()
    }

    private fun startEncryption(uri: Uri?) {
        try {
            AESencryptionTask(this, uri, mEncryptedFile).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

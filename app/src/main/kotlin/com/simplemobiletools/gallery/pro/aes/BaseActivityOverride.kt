package com.simplemobiletools.gallery.pro.aes

import android.content.Intent
import com.simplemobiletools.commons.activities.AboutActivity
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FileConflictDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.CONFLICT_KEEP_BOTH
import com.simplemobiletools.commons.helpers.CONFLICT_SKIP
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.getConflictResolution
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.R
import java.io.File
import javax.crypto.Cipher

abstract class BaseActivityOverride : BaseSimpleActivity() {
    override fun startActivity(intent: Intent) {
        if (intent.component != null && AboutActivity::class.java.name == intent.component!!
                .shortClassName
        ) {
            //   intent.setClass(getApplicationContext(), SettingsActivity.class);
        }
        super.startActivity(intent)
    }

    fun checkConflictsAES(
        files: ArrayList<AESDirItem>, destinationPath: String, index: Int, conflictResolutions: LinkedHashMap<String, Int>,
        callback: (resolutions: LinkedHashMap<String, Int>) -> Unit
    ) {
        if (index == files.size) {
            callback(conflictResolutions)
            return
        }

        val file = files[index]
        val newFileDirItem = FileDirItem("$destinationPath/${file.name}", file.displayName, file.isDirectory)
        if (getDoesFilePathExist(newFileDirItem.path)) {
            FileConflictDialog(this, newFileDirItem, files.size > 1) { resolution, applyForAll ->
                if (applyForAll) {
                    conflictResolutions.clear()
                    conflictResolutions[""] = resolution
                    checkConflictsAES(files, destinationPath, files.size, conflictResolutions, callback)
                } else {
                    conflictResolutions[newFileDirItem.path] = resolution
                    checkConflictsAES(files, destinationPath, index + 1, conflictResolutions, callback)
                }
            }
        } else {
            checkConflictsAES(files, destinationPath, index + 1, conflictResolutions, callback)
        }
    }

    fun moveFiles(
        fileDirItems: ArrayList<AESDirItem>,
        source: String,
        destination: String,
        callback: (destinationPath: String) -> Unit
    ) {
        if (source == destination) {
            toast(R.string.source_and_destination_same)
            return
        }
        checkConflictsAES(fileDirItems, destination, 0, LinkedHashMap()) {
            toast(R.string.moving)
            ensureBackgroundThread {
                var fileCountToCopy = fileDirItems.size
                val updatedPaths = ArrayList<String>(fileDirItems.size)
                val destinationFolder = File(destination)
                val enCipher = AESHelper.encryptionCypher

                for (oldFileDirItem in fileDirItems) {
                    var newFile = File(destinationFolder, oldFileDirItem.name)

                    val oldInfoFile = oldFileDirItem.mInfoFile
                    val oldThumbFile = oldFileDirItem.mThumbFile

                    var newInfoFile = oldInfoFile?.let { File(destinationFolder, oldFileDirItem.mInfoFile!!.name) }
                    var newThumbFile = oldThumbFile?.let { File(destinationFolder, oldFileDirItem.mThumbFile!!.name) }

                    if (newFile.exists()) {
                        when {
                            getConflictResolution(it, newFile.absolutePath) == CONFLICT_SKIP -> fileCountToCopy--
                            getConflictResolution(it, newFile.absolutePath) == CONFLICT_KEEP_BOTH -> {

                                newFile = getAlternativeAESFile(oldFileDirItem.displayName, newFile.extension, destination, enCipher)
                                val nameWe = newFile.nameWithoutExtension
                                newInfoFile?.let { newInfoFile = File(destinationFolder, nameWe + AES_META_EXT) }
                                newThumbFile?.let { newThumbFile = File(destinationFolder, nameWe + AES_THUMB_EXT) }
                            }
                            else -> {
                                // this file is guaranteed to be on the internal storage, so just delete it this way
                                newFile.delete()
                                newInfoFile?.delete()
                                newThumbFile?.delete()
                            }
                        }
                    }

                    if (!newFile.exists() && File(oldFileDirItem.path).renameTo(newFile)) {
                        oldInfoFile?.renameTo(newInfoFile!!)
                        oldThumbFile?.renameTo(newThumbFile!!)
                        updatedPaths.add(newFile.absolutePath)
                        callback(destination)
                    }
                }
            }
        }
    }

    fun getAlternativeAESFile(realName: String, extension: String, destinationPath: String, enCipher: Cipher): File {
        var fileIndex = 1
        val nameWE = realName.substringBeforeLast(".")
        val realExt = realName.getFilenameExtension()
        var newFile: File?
        do {
            val newName = AESHelper.encryptFileName(String.format("%s(%d).%s", nameWE, fileIndex, realExt), enCipher)
            newFile = File(destinationPath, String.format("%s.%s", newName, extension))
            fileIndex++
        } while (getDoesFilePathExist(newFile!!.absolutePath))
        return newFile
    }
}

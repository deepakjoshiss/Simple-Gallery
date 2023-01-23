package com.simplemobiletools.gallery.pro.aes

import com.simplemobiletools.commons.models.FileDirItem
import java.io.File

class AESDirItem(
    path: String,
    name: String = "",
    isDirectory: Boolean = false,
    children: Int = 0,
    size: Long = 0L,
    modified: Long = 0L,
    mediaStoreId: Long = 0L
) :
    FileDirItem(path, name, isDirectory, children, size, modified, mediaStoreId) {
    var mThumbFile: File? = null
    lateinit var mInfoFile: File
    lateinit var encodedName: String
    lateinit var displayName: String
    var duration: Long = 0L

    init {
        //  println(">>>> $path    $name    $isDirectory")
    }

}

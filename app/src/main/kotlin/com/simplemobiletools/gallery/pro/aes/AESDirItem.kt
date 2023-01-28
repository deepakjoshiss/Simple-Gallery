package com.simplemobiletools.gallery.pro.aes

import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.commons.helpers.AlphanumericComparator
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
    var mInfoFile: File? = null
    var encodedName: String = name
    var displayName: String = ""
    var fileInfo: AESFileInfo? = null

    init {
        //  println(">>>> $path    $name    $isDirectory")
    }
}

val AESDirItemComparator: Comparator<AESDirItem> = object : Comparator<AESDirItem> {

    fun compareName(o1: AESDirItem, o2: AESDirItem): Int {
        val o1Name = o1.name ?: ""
        val o2Name = o2.name ?: ""
        return AlphanumericComparator().compare(o1Name.normalizeString().toLowerCase(), o2Name.normalizeString().toLowerCase())
    }

    fun compareDir(o1: AESDirItem, o2: AESDirItem): Int? {
        if (o1.isDirectory && o2.isDirectory) return compareName(o1, o2)
        if (o1.isDirectory) return -1
        if (o2.isDirectory) return 1
        return null
    }

    fun compareLastModified(o1: AESDirItem, o2: AESDirItem): Int? {
        val o1date = o1.fileInfo?.lastMod ?: 0
        val o2date = o2.fileInfo?.lastMod ?: 0
        if (o1date != o2date) {
            return o2date.compareTo(o1date)
        }
        return null
    }

    override fun compare(o1: AESDirItem, o2: AESDirItem): Int {
        compareDir(o1, o2)?.let { return it }
        compareLastModified(o1, o2)?.let { return it }
        return 0//compareName(o1, o2)
    }
}

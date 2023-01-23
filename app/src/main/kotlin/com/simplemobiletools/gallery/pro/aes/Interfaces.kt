package com.simplemobiletools.gallery.pro.aes

interface PassCallback {
    fun onGoClick(text: String)
    fun onTextChange(text: String)
}

interface ProgressCallback {
    fun onProgress(name: String, progress: Int)
}

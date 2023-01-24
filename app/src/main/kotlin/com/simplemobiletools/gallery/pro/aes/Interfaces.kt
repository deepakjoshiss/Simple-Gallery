package com.simplemobiletools.gallery.pro.aes

interface TextSubmitCallback {
    fun onSubmit(text: String, meta: String ?= "")
    fun onTextChange(text: String, meta: String ?= "")
}

interface ProgressCallback {
    fun onProgress(name: String, progress: Int)
}

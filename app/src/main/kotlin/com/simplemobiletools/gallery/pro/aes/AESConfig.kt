package com.simplemobiletools.gallery.pro.aes

import android.content.Context
import com.simplemobiletools.commons.helpers.PRIMARY_ANDROID_DATA_TREE_URI
import com.simplemobiletools.commons.helpers.videoExtensions
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.AES_IMAGE_EXT
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.AES_VIDEO_EXT


const val AES_PREFS_KEY = "AesPrefs"
const val AES_TOKEN_KEY = "aes_token"
const val AES_TRUTH_KEY = "aes_truth"
const val AES_PIN_KEY = "aes_pin"
const val AES_VAULT_KEY = "aes_vault"

class AESConfig(val context: Context) {

    protected val prefs = context.getSharedPreferences(AES_PREFS_KEY, Context.MODE_PRIVATE)

    companion object {
        fun newInstance(context: Context) = AESConfig(context)
    }

    var aesTruth: String
        get() = prefs.getString(AES_TRUTH_KEY, "")!!
        set(uri) = prefs.edit().putString(AES_TRUTH_KEY, uri).apply()

    var aesKey: String
        get() = prefs.getString(AES_PIN_KEY, "")!!
        set(uri) = prefs.edit().putString(AES_PIN_KEY, uri).apply()

    var aesVault: String
        get() = prefs.getString(AES_VAULT_KEY, "")!!
        set(uri) = prefs.edit().putString(AES_VAULT_KEY, uri).apply()
}

val Context.aesConfig: AESConfig get() = AESConfig.newInstance(this)

val videoExtensionsN: Array<String> get() = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".3gpp", AES_VIDEO_EXT)
val photoExtensionsN: Array<String> get() = arrayOf(".jpg", ".png", ".jpeg", ".bmp", ".webp", ".heic", ".heif", ".apng", ".avif", AES_IMAGE_EXT )

fun String.isVideoFastN() = videoExtensionsN.any { endsWith(it, true) }
fun String.isImageFastN() = photoExtensionsN.any { endsWith(it, true) }

fun String.isExtVideo() = videoExtensionsN.any { equals(it, true) }
fun String.isExtImage() = photoExtensionsN.any { equals(it, true) }



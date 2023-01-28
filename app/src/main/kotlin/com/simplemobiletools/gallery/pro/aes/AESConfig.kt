package com.simplemobiletools.gallery.pro.aes

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.Primitives
import java.io.File
import java.lang.reflect.Type

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

val gson = Gson()
val Context.aesConfig: AESConfig get() = AESConfig.newInstance(this)

val videoExtensionsN: Array<String> get() = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".3gpp", AES_VIDEO_EXT)
val photoExtensionsN: Array<String> get() = arrayOf(".jpg", ".png", ".jpeg", ".bmp", ".webp", ".heic", ".heif", ".apng", ".avif", AES_IMAGE_EXT)

fun String.isVideoFastN() = videoExtensionsN.any { endsWith(it, true) }
fun String.isImageFastN() = photoExtensionsN.any { endsWith(it, true) }

fun String.isExtVideo() = videoExtensionsN.any { equals(it, true) }
fun String.isExtImage() = photoExtensionsN.any { equals(it, true) }

fun String.isAESVideo() = this.endsWith(AES_VIDEO_EXT)


fun Any.toJsonString(): String {
    return gson.toJson(this)!!
}

fun <T> String.parseJson(classOfT: Class<T>): T? {
    return try {
        gson.fromJson(this, classOfT)
    } catch (e: java.lang.Exception) {
        null
    }
}

fun File.getVaultDirChildrenCount(): Int {
    list()?.let {
        return it.count { path -> !(path.endsWith(AES_THUMB_EXT) || path.endsWith(AES_META_EXT)) }
    }
    return 0
}

fun Activity.startAesActivity(paths: ArrayList<String>) {
    Intent(this, AESActivity::class.java).apply {
        putExtra("paths", paths)
        startActivity(this)
    };
}



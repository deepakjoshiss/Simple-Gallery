package com.simplemobiletools.gallery.pro.aes


import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.ThumbnailUtils
import android.util.Base64
import java.io.*

object AESFileUtils {

    @Throws(FileNotFoundException::class)
    fun getOutputStream(file: File?, context: Context, z: Boolean): OutputStream? {
        if (file != null) {
            return FileOutputStream(file)
        }
        return null;
    }

    fun getThumbnail(str: String): ByteArray? {
        val thumbnailVideoFull: Bitmap? = ThumbnailUtils.createVideoThumbnail(str, 2);
        if (thumbnailVideoFull != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            thumbnailVideoFull.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
            thumbnailVideoFull.recycle()
            return byteArray
        }
        println(">>>>>> null thumb, $str")
        return null
    }

    fun getDuration(str: String): ByteArray {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(str)
        return mediaMetadataRetriever.extractMetadata(METADATA_KEY_DURATION)!!.toByteArray()
    }

    fun writeByteArrayToFile(context: Context, file: File?, bArr: ByteArray?) {
        try {
            val outputStream: OutputStream? = getOutputStream(file, context, true)
            if (outputStream != null) {
                outputStream.write(bArr)
                outputStream.close()
            } else {
                throw IllegalStateException("FileOutputStream could not be opened")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encodeBase64Name(arr: ByteArray): String {
        return Base64.encodeToString(arr, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    }

    fun decodeBase64Name(str: String): ByteArray {
        return Base64.decode(str, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}

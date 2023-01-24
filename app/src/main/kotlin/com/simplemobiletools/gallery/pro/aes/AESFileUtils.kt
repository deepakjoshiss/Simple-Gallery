package com.simplemobiletools.gallery.pro.aes


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore.Video.Thumbnails.MINI_KIND
import android.util.Base64
import android.util.Log
import android.util.Size
import java.io.*
import javax.crypto.Cipher

object AESFileUtils {
    const val AES_VIDEO_EXT = ".sys"
    const val AES_IMAGE_EXT = ".inx"
    const val AES_THUMB_EXT = ".dat"
    const val AES_META_EXT = ".nfo"

    @Throws(FileNotFoundException::class)
    fun getOutputStream(file: File?, context: Context, z: Boolean): OutputStream? {
        if (file != null) {
            return FileOutputStream(file)
        }
        return null;
    }

    fun bitmapToByteArray(bmp: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        bmp.recycle()
        return byteArray
    }

    @SuppressLint("NewApi")
    fun getImageThumbnail(file: File): ByteArray? {
        val thumb: Bitmap = ThumbnailUtils.createImageThumbnail(file, Size(512, 512), null)
        return bitmapToByteArray(thumb)
    }

    fun getVideoThumbnail(str: String): ByteArray? {
        val thumbnailVideoFull: Bitmap? = ThumbnailUtils.createVideoThumbnail(str, MINI_KIND);
        if (thumbnailVideoFull != null) {
            return bitmapToByteArray(thumbnailVideoFull)
        }
        println(">>>>>> null thumb, $str")
        return null
    }

    fun getDuration(str: String): ByteArray {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(str)
        return mediaMetadataRetriever.extractMetadata(METADATA_KEY_DURATION)!!.encodeToByteArray()
    }

    fun decodeFileData(context: Context, file: File): ByteArray? {
        try {
            val inputStream: InputStream? = context.getContentResolver().openInputStream(Uri.fromFile(file))
            if (inputStream != null) {
                val outputStream = ByteArrayOutputStream()
                //val aesInputStream = helper.getCipherInputStream(inputStream)
                val buffer = ByteArray(256)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                inputStream.close()
                //aesInputStream.close();
                return outputStream.toByteArray()
            }
        } catch (e: Exception) {
            println(">>> file decode error ${file.absolutePath}")
            e.printStackTrace()
        }
        return null
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

    fun getEncryptedFileName(cipher: Cipher, name: String): String {
        return encodeBase64Name(cipher.doFinal(name.encodeToByteArray()))
    }

    fun createAlbum(cipher: Cipher, path: String, name: String): Boolean {
        if (path.isNotEmpty()) {
            var file = File(path)
            if (file.isDirectory) {
                file = File(path, getEncryptedFileName(cipher, name))
                return file.mkdir();
            }
        }
        return false;
    }
}

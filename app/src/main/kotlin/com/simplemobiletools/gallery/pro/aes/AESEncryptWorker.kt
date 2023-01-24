package com.simplemobiletools.gallery.pro.aes

import android.content.Context
import android.media.ThumbnailUtils.createImageThumbnail
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.simplemobiletools.commons.extensions.isImageFast
import com.simplemobiletools.commons.extensions.isImageSlow
import com.simplemobiletools.commons.extensions.isVideoSlow
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.decodeBase64Name
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.encodeBase64Name
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getDuration
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getEncryptedFileName
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getImageThumbnail
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getVideoThumbnail
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.writeByteArrayToFile
import com.simplemobiletools.gallery.pro.aes.AESHelper.decryptText
import com.simplemobiletools.gallery.pro.aes.AESHelper.encryptionCypher
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import kotlin.math.max
import kotlin.math.min

class AESEncryptWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var mFrom: File
    private lateinit var mToPath: String
    private lateinit var mCipher: Cipher

    override fun doWork(): Result {
        try {
            mFrom = inputData.getString("fromFile")?.let { File(it) }!!
            mToPath = inputData.getString("toPath")!!
            mCipher = encryptionCypher
            AESHelper.aesProgress?.setProgress(mFrom, 0)
            encryptFile()
            AESHelper.aesProgress?.setProgress(mFrom, 100)
            return Result.success()
        } catch (e: java.lang.Exception) {
            linePrint(e.message.toString())
            e.printStackTrace()
        }
        AESHelper.aesProgress?.setProgress(mFrom, 100)
        return Result.failure()
    }

    @Throws(Exception::class)
    private fun encryptFile() {
        val encName: String = getEncryptedFileName(mCipher, mFrom.name)
        val dec: ByteArray = decodeBase64Name(encName)
        linePrint(" encrypting file  ${mFrom.name} ->  ${decryptText(dec)}")
        if (mFrom.isVideoSlow()) {
            encryptVideoFile(encName)
            return
        }

        if (mFrom.isImageSlow()) {
            val toFile = File(mToPath, encName + AESFileUtils.AES_IMAGE_EXT)
            createThumb(mToPath, encName, getImageThumbnail(mFrom))
            encryptToFile(toFile)
        }

    }

    private fun encryptToFile(toFile: File) {
        val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(Uri.fromFile(mFrom))
        if (inputStream != null) {
            val fileOutputStream = FileOutputStream(toFile)
            val cipherOutputStream = CipherOutputStream(BufferedOutputStream(fileOutputStream), mCipher)
            val totalBytes = mFrom.length()
            val buffer = ByteArray(if (totalBytes > 1024 * 1024) 1024 * 256 else 1024 * 8)
            val pThr = max(1024 * 8, min((totalBytes * 2) / 100, 1024 * 1024))
            var totalBytesRead = 0L
            var lastReported = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytesRead - lastReported >= pThr) {
                    val percent = (totalBytesRead * 100L) / totalBytes
                    AESHelper.aesProgress?.setProgress(mFrom, percent.toInt())
                    lastReported = totalBytesRead
                    //   linePrint("Reading from $mFrom $totalBytes $totalBytesRead ${percent}")
                }
            }
            inputStream.close()
            cipherOutputStream.close()
        } else {
            throw FileNotFoundException("$PRINT_TAG File Not found $mFrom")
        }
    }


    @Throws(Exception::class)
    private fun encryptVideoFile(encName: String) {
        val toFile = File(mToPath, encName + AESFileUtils.AES_VIDEO_EXT)
        createVideoFileMeta(mToPath, encName)
        encryptToFile(toFile)
    }

    private fun createVideoFileMeta(fileParentPath: String, nameWE: String) {
        try {
            val fromPath: String = mFrom.absolutePath
            val dur: ByteArray = mCipher.doFinal(getDuration(fromPath))
            writeByteArrayToFile(applicationContext, File(fileParentPath, nameWE + AESFileUtils.AES_META_EXT), dur)
            createThumb(fileParentPath, nameWE, getVideoThumbnail(fromPath))
        } catch (e: Exception) {
            println(">>>> create meta error $fileParentPath $nameWE")
            e.printStackTrace()
        }
    }

    private fun createThumb(fileParentPath: String, nameWE: String, thumbData: ByteArray?): Boolean {
        thumbData?.let {
            writeByteArrayToFile(applicationContext, File(fileParentPath, nameWE + AESFileUtils.AES_THUMB_EXT), mCipher.doFinal(it))
            return true
        }
        return false
    }
}

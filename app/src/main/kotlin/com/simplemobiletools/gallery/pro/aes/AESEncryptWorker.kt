package com.simplemobiletools.gallery.pro.aes

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.decodeBase64Name
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.encodeBase64Name
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getDuration
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getThumbnail
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

    private fun createFileMeta(fileParentPath: String, nameWE: String) {
        try {
            val fromPath: String = mFrom.absolutePath
            val thumb: ByteArray = mCipher.doFinal(getThumbnail(fromPath))
            val dur: ByteArray = mCipher.doFinal(getDuration(fromPath))
            writeByteArrayToFile(applicationContext, File(fileParentPath, nameWE + AESFileUtils.AES_THUMB_EXT), thumb)
            writeByteArrayToFile(applicationContext, File(fileParentPath, nameWE + AESFileUtils.AES_META_EXT), dur)
        } catch (e: Exception) {
            println(">>>> create meta error $fileParentPath $nameWE")
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun encryptFile() {
        val encName: ByteArray = mCipher.doFinal(mFrom.name.toByteArray(StandardCharsets.UTF_8))
        val b64: String = encodeBase64Name(encName)
        val dec: ByteArray = decodeBase64Name(b64)
        println(">>>> file " + mFrom.name + "  " + b64.length + "  " + decryptText(dec))
        val toFile = File(mToPath, b64 + AESFileUtils.AES_VIDEO_EXT)
        createFileMeta(mToPath, b64)
        val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(Uri.fromFile(mFrom))
        if (inputStream != null) {
            val fileOutputStream = FileOutputStream(toFile)
            val cipherOutputStream = CipherOutputStream(BufferedOutputStream(fileOutputStream), mCipher)
            val buffer = ByteArray(1024 * 256)
            val totalBytes = mFrom.length()
            var totalBytesRead = 0L
            var lastReported = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytesRead - lastReported >= 1024 * 1024) {
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
}

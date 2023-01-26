import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.simplemobiletools.gallery.pro.aes.AESFileUtils.getEncryptedFileName
import com.simplemobiletools.gallery.pro.aes.AESHelper
import com.simplemobiletools.gallery.pro.aes.PRINT_TAG
import com.simplemobiletools.gallery.pro.aes.linePrint
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.crypto.CipherInputStream
import kotlin.math.max
import kotlin.math.min

class AESDecryptWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var mFrom: File
    private lateinit var mToPath: String

    override fun doWork(): Result {
        mFrom = inputData.getString("fromFile")?.let { File(it) }!!
        mToPath = inputData.getString("toPath")!!
        try {
            AESHelper.aesProgress?.setProgress(mFrom, 0)
            decryptFile()
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
    private fun decryptFile() {
        val fileName: String = AESHelper.decryptFileName(mFrom.nameWithoutExtension)
        val toFile = File(mToPath, fileName)
        linePrint(" decrypting to file  ${toFile.path}}")

        val inputStream: CipherInputStream? =
            applicationContext.contentResolver.openInputStream(Uri.fromFile(mFrom))?.let { AESHelper.getDecipherInputStream(it)}
        if (inputStream != null) {
            val fileOutputStream = FileOutputStream(toFile)
            val totalBytes = mFrom.length()
            val buffer = ByteArray(if (totalBytes > 1024 * 1024) 1024 * 256 else 1024 * 8)
            val pThr = max(1024 * 8, min((totalBytes * 2) / 100, 1024 * 1024))
            var totalBytesRead = 0L
            var lastReported = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytesRead - lastReported >= pThr) {
                    val percent = (totalBytesRead * 100L) / totalBytes
                    AESHelper.aesProgress?.setProgress(mFrom, percent.toInt())
                    lastReported = totalBytesRead
                }
            }
            inputStream.close()
            fileOutputStream.close()
        } else {
            throw FileNotFoundException("$PRINT_TAG File Not found $mFrom")
        }
    }
}

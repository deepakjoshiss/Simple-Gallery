package com.simplemobiletools.gallery.pro.aes

import android.content.Context
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import java.security.NoSuchAlgorithmException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import com.simplemobiletools.gallery.pro.aes.AESHelper
import com.google.android.exoplayer2.upstream.TransferListener
import com.simplemobiletools.gallery.pro.aes.EncryptedFileDataSourceFactory
import java.io.File
import java.io.InputStream
import java.lang.Exception
import javax.crypto.CipherInputStream


object AESHelper {
    private val mFolderPath: String? = null
    private var mSecretKeySpec: SecretKeySpec? = null
    private var mIvParameterSpec: IvParameterSpec? = null
    private lateinit var mCipher: Cipher
    private lateinit var mDataCipher: Cipher
    var aesProgress: AESProgress? = null

    init {
        mIvParameterSpec = AESUtils.createDataIVSpec()
    }

    fun setToken(token: ByteArray) {
        mSecretKeySpec = AESUtils.createDataKeySpec(token)
        try {
            mCipher = Cipher.getInstance(AES_TRANSFORMATION)
            mCipher.init(
                Cipher.DECRYPT_MODE,
                mSecretKeySpec,
                mIvParameterSpec
            )
            mDataCipher = Cipher.getInstance(AES_TRANSFORMATION)
            mCipher.init(
                Cipher.DECRYPT_MODE,
                mSecretKeySpec,
                mIvParameterSpec
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCipherInputStream(inputS: InputStream): CipherInputStream {
        return CipherInputStream(inputS, mCipher)
    }

    fun createDataSourceFactory(listener: TransferListener?): EncryptedFileDataSourceFactory {
        return EncryptedFileDataSourceFactory(mDataCipher, mSecretKeySpec, mIvParameterSpec, listener)
    }


    fun decryptText(arr: ByteArray): String {
        return mCipher.doFinal(arr).decodeToString()
    }

    fun decrypt(arr: ByteArray): ByteArray {
        return mCipher.doFinal(arr)
    }

    fun decryptAlbumData(fileData: AESDirItem): AESDirItem {
        try {
            fileData.displayName = mCipher.doFinal(AESFileUtils.decodeBase64Name(fileData.encodedName)).decodeToString()
        } catch (e: Exception) {
            println(">>>> error in decrypt Album data ${fileData.path}")
            e.printStackTrace()
        }

        println(">>>> got album info ${fileData.displayName}")
        return fileData
    }

    fun decryptImageFileData(context: Context, fileData: AESDirItem): AESDirItem {
        try {
            val fileName = mCipher.doFinal(AESFileUtils.decodeBase64Name(fileData.encodedName)).decodeToString()
            fileData.displayName = fileName

        } catch (e: Exception) {
            println(">>>> error in decrypt image file data ${fileData.path}")
            e.printStackTrace()
        }

        println(">>>> got file info ${fileData.displayName}, ${fileData.duration}")
        return fileData
    }

    fun decryptVideoFileData(context: Context, fileData: AESDirItem): AESDirItem {
        try {
            val fileName = mCipher.doFinal(AESFileUtils.decodeBase64Name(fileData.encodedName)).decodeToString()
            fileData.displayName = fileName
            fileData.mInfoFile?.let {
                val durData = AESFileUtils.decodeFileData(context, it)
                if (durData != null) {
                    fileData.duration = mCipher.doFinal(durData).decodeToString().toLong()
                }
            }

        } catch (e: Exception) {
            println(">>>> error in decrypt video file data ${fileData.path}")
            e.printStackTrace()
        }

        println(">>>> got file info ${fileData.displayName}, ${fileData.duration}")
        return fileData
    }

    @get:Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class
    )

    val encryptionCypher: Cipher
        get() {
            val encryptionCipher = Cipher.getInstance(AES_TRANSFORMATION)
            encryptionCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec)
            return encryptionCipher
        }
}

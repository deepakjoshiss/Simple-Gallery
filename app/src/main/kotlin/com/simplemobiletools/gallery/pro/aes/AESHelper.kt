package com.simplemobiletools.gallery.pro.aes

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
import java.lang.Exception



object AESHelper {
    private val mFolderPath: String? = null
    private var mSecretKeySpec: SecretKeySpec? = null
    private var mIvParameterSpec: IvParameterSpec? = null
    private lateinit var mCipher: Cipher

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createDataSourceFactory(listener: TransferListener?): EncryptedFileDataSourceFactory {
        return EncryptedFileDataSourceFactory(mCipher, mSecretKeySpec, mIvParameterSpec, listener)
    }

    fun decryptText(arr: ByteArray): String {
        return mCipher.doFinal(arr).decodeToString()
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

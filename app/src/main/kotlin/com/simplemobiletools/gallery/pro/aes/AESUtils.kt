package com.simplemobiletools.gallery.pro.aes

import android.util.Log
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

const val PASS_KEY_TYPE = "PBKDF2WithHmacSHA1"
const val PASS_KEY_LENGTH = 256
const val PASS_KEY_ITER = 48
const val PASS_KEY_ALGO = "AES"
const val PASS_KEY_TRANSFORM = "AES/CBC/PKCS7Padding"

const val DATA_IV = "a7c2d599fb131290"
const val AES_TRANSFORMATION = "AES/CTR/NoPadding"

data class AESText(var salt: ByteArray, var iv: ByteArray, var data: ByteArray)

data class AESVault(var salt: ByteArray, var iv: ByteArray, var pass: ByteArray, var vault: ByteArray)

data class AESCipher(var salt: ByteArray, var iv: ByteArray, var cipher: Cipher)

object AESUtils {

    fun createRandomByteArray(size: Int): ByteArray {
        val random = SecureRandom()
        val arr = ByteArray(size)
        random.nextBytes(arr)
        return arr;
    }

    fun createKeySpec(salt: ByteArray, passwordChar: CharArray, longKey: Boolean = false): SecretKeySpec {
        val pbKeySpec = PBEKeySpec(passwordChar, salt, PASS_KEY_ITER, PASS_KEY_LENGTH) //1324 iterations
        val secretKeyFactory = SecretKeyFactory.getInstance(PASS_KEY_TYPE)
        val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
        return SecretKeySpec(keyBytes, PASS_KEY_ALGO)
    }

    fun createPassKeySpec(salt: ByteArray, passwordString: String): SecretKeySpec {
        return createKeySpec(salt, passwordString.toCharArray(), true)
    }

    fun createPassCipher(opmode: Int, key: Key, params: AlgorithmParameterSpec): Cipher {
        val cipher = Cipher.getInstance(PASS_KEY_TRANSFORM)
        cipher.init(opmode, key, params)
        return cipher
    }

    fun createPassEncryptCipher(passwordString: String): AESCipher {
        //Random salt for next step
        val salt = createRandomByteArray(256)
        //PBKDF2 - derive the key from the password, don't use passwords directly
        val keySpec = createPassKeySpec(salt, passwordString)
        // Random iv
        val iv = createRandomByteArray(16)
        val ivSpec = IvParameterSpec(iv)
        val cipher = createPassCipher(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return AESCipher(salt, iv, cipher)
    }

    fun createPassDecryptCipher(salt: ByteArray, iv: ByteArray, passwordString: String): Cipher {
        //regenerate key from password
        val keySpec = createPassKeySpec(salt, passwordString)
        val ivSpec = IvParameterSpec(iv)

        return createPassCipher(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    }

    fun encryptVault(pass: String, folderPath: String, pin: String): AESVault? {
        try {
            val passBytes = pass.encodeToByteArray()
            val pathBytes = folderPath.encodeToByteArray()
            val aesCipher = createPassEncryptCipher(pin)
            val encryptedPass = aesCipher.cipher.doFinal(passBytes)
            val encryptedPath = aesCipher.cipher.doFinal(pathBytes)
            return AESVault(aesCipher.salt, aesCipher.iv, encryptedPass, encryptedPath);
        } catch (e: Exception) {
            Log.e("encryption exception", e.message!!)
        }
        return null
    }

    fun decryptVault(aesData: AESVault, pin: String): Array<ByteArray>? {
        try {
            val cipher = createPassDecryptCipher(aesData.salt, aesData.iv, pin);
            return arrayOf(cipher.doFinal(aesData.pass), cipher.doFinal(aesData.vault))
        } catch (e: java.lang.Exception) {
            Log.e("decryption exception", e.message!!)
        }
        return null;
    }

    fun encryptPass(plainText: String, passwordString: String): AESText? {
        try {
            val plainTextBytes = plainText.encodeToByteArray()
            val aesCipher = createPassEncryptCipher(passwordString)
            val encrypted = aesCipher.cipher.doFinal(plainTextBytes)
            return AESText(aesCipher.salt, aesCipher.iv, encrypted);
        } catch (e: Exception) {
            Log.e("encryption exception", e.message!!)
        }
        return null
    }

    fun decryptPass(aesData: AESText, passwordString: String): ByteArray? {
        var decrypted: ByteArray? = null
        try {
            decrypted = createPassDecryptCipher(aesData.salt, aesData.iv, passwordString).doFinal(aesData.data)
        } catch (e: java.lang.Exception) {
            Log.e("decryption exception", e.message!!)
        }
        return decrypted;
    }

    //Data related

    fun createDataKeySpec(token: ByteArray): SecretKeySpec {
        val salt = DATA_IV.encodeToByteArray()
        val pass = token.decodeToString().toCharArray()
        return createKeySpec(salt, pass, false)
    }

    fun createDataIVSpec(): IvParameterSpec {
        return IvParameterSpec(DATA_IV.encodeToByteArray())
    }
}

package me.han.muffin.client.utils.encryption

import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESUtils(key: String) {
    private var keyBytes = ByteArray(16)

    init {
        for (i in 0 until 16) keyBytes[i] = key.toByteArray()[i]
    }

    @Throws(Exception::class)
    fun encrypt(content: String): String {
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val aesCipher = Cipher.getInstance("AES")
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val doFinal= aesCipher.doFinal(content.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(doFinal)
    }

    @Throws(Exception::class)
    fun encryptTest(content: String): String {
        val secretKeyFactory = SecretKeyFactory.getInstance("AES")
        val aesCipher = Cipher.getInstance("AES")
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKeyFactory.generateSecret(DESKeySpec(ByteArray(8))), IvParameterSpec(ByteArray(8)))
        val doFinal= aesCipher.doFinal(content.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(doFinal)
    }

    @Throws(Exception::class)
    fun decrypt(content: String): String {
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val instance = Cipher.getInstance("AES")
        instance.init(Cipher.DECRYPT_MODE, secretKey)
        return String(instance.doFinal(Base64.getDecoder().decode(content))).trim()
    }

}
package me.han.muffin.client.utils.encryption

import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

/**
 * (1)jdk版本：1.40以上
 * (2)加密背景：檔案加密 加密方法：三重des加密
 * (3)加密解密原理:
 * 	  加密時,對輸入的48位祕鑰,前兩位,中間兩位和後44位,分別求md5值,
 *  先用第一個md5值加密,再用第二個md5值加密,最後用第三個md5值加密,,共三重加密,
 *  解密時,對輸入的byte陣列即加密後的檔案, 先用第三個md5值解密,再用第二個md5值解密,再用第一個md5值解密,這樣返回的就是解密後的檔案
 *  md5值的獲取用 java.security 包裡的類
 *  加密解密用的是 javax.crypto 包裡面的類
 * (4)加密解密過程用法：
 * 在console 輸入祕鑰  AD67EA2F3BE6E5ADD368DFE03120B5DF92A8FD8FEC2F0746
 * 再輸入需要加密的檔名  def.txt(檔案路徑是相對路徑是相對這個的路徑  D:\***\EclipseWorkingspace\WorkspaceLearning\jsp_api)
 * 再輸入en,即可實現加密
 * 在以上路徑下出現加密檔案   en_def.txt,
 * 解密過程方法:輸入祕鑰  AD67EA2F3BE6E5ADD368DFE03120B5DF92A8FD8FEC2F0746
 * 再輸入需要解密的檔名  en_def.txt
 * 再輸入de,即可實現解密
 *
 *
 */
class FileEncryptor {

    /**
     * 加密函式 輸入： 要加密的檔案，密碼（由0-F組成，共48個字元，表示3個16位的密碼）如：
     * AD67EA2F3BE6E5ADD368DFE03120B5DF92A8FD8FEC2F0746 其中：
     * AD67EA2F3BE6E5AD  DES密碼一      D368DFE03120B5DF DES密碼二 92A8FD8FEC2F0746 DES密碼三
     * 輸出：對輸入的檔案加密後，儲存到同一資料夾下增加了 "en+原檔名" 為副檔名的檔案中。
     *
     * param:
     * sKey 是三個md5值的字串拼接, 一共48位
     */
    fun encrypt(fileIn: File, sKey: String) {
        try {
            if (sKey.length == 48) {
                val bytK1 = getKeyByStr(sKey.substring(0, 16))
                val bytK2 = getKeyByStr(sKey.substring(16, 32))
                val bytK3 = getKeyByStr(sKey.substring(32, 48))

                val fis = FileInputStream(fileIn)
                val bytIn = ByteArray(fileIn.length().toInt())
                for (i in 0 until fileIn.length()) bytIn[i.toInt()] = fis.read().toByte()

                // 加密
                val bytOut = encryptByDES(encryptByDES(encryptByDES(bytIn, bytK1), bytK2), bytK3)
                val fos = FileOutputStream(fileIn)
                for (element in bytOut) fos.write(element.toInt())
                fos.flush()
                fos.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encrypt(outputStream: OutputStream, fileIn: File, sKey: String) {
        try {
            if (sKey.length == 48) {
                val bytK1 = getKeyByStr(sKey.substring(0, 16))
                val bytK2 = getKeyByStr(sKey.substring(16, 32))
                val bytK3 = getKeyByStr(sKey.substring(32, 48))

                val fis = FileInputStream(fileIn)
                val bytIn = ByteArray(fileIn.length().toInt())
                for (i in 0 until fileIn.length()) bytIn[i.toInt()] = fis.read().toByte()

                // 加密
                val bytOut = encryptByDES(encryptByDES(encryptByDES(bytIn, bytK1), bytK2), bytK3)

                for (element in bytOut) outputStream.write(element.toInt())
                outputStream.flush()
                outputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解密函式 輸入： 要解密的檔案，密碼（由0-F組成，共48個字元，表示3個16位的密碼）如：
     * AD67EA2F3BE6E5ADD368DFE03120B5DF92A8FD8FEC2F0746 其中：
     * AD67EA2F3BE6E5AD  DES密碼一 D368DFE03120B5DF DES密碼二 92A8FD8FEC2F0746 DES密碼三
     * 輸出：對輸入的檔案解密後，儲存到使用者指定的檔案中。
     */
    fun decrypt(fileIn: File, sKey: String): String? {
        try {
            if (sKey.length == 48) {
                val bytK1 = getKeyByStr(sKey.substring(0, 16))
                val bytK2 = getKeyByStr(sKey.substring(16, 32))
                val bytK3 = getKeyByStr(sKey.substring(32, 48))

                val input = FileInputStream(fileIn)
                val bytIn = ByteArray(fileIn.length().toInt())
                for (i in 0 until fileIn.length()) bytIn[i.toInt()] = input.read().toByte()

                // 解密
                val bytOut = decryptByDES(decryptByDES(decryptByDES(bytIn, bytK3), bytK2), bytK1)

                fileIn.createNewFile()
                val output = FileOutputStream(fileIn)
                for (element in bytOut) output.write(element.toInt())
                output.close()

                var line: String? = null
                val reader = BufferedReader(FileReader(fileIn))
                while (reader.readLine()?.also { line = it } != null);
                encrypt(fileIn, sKey)

                return line
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }


    /**
     * 用DES方法加密輸入的位元組 bytKey需為8位元組長，是加密的密碼
     * param:bytP 檔案  , bytKey 密碼
     * return:
     */
    @Throws(Exception::class)
    private fun encryptByDES(bytP: ByteArray, bytKey: ByteArray): ByteArray {
        val desKS = DESKeySpec(bytKey)
        val skf = SecretKeyFactory.getInstance("DES")
        val sk = skf.generateSecret(desKS)
        val cip = Cipher.getInstance("DES")
        cip.init(Cipher.ENCRYPT_MODE, sk)
        return cip.doFinal(bytP)
    }

    /**
     * 用DES方法解密輸入的位元組 bytKey需為8位元組長，是解密的密碼
     */
    @Throws(Exception::class)
    private fun decryptByDES(bytE: ByteArray, bytKey: ByteArray): ByteArray {
        val desKS = DESKeySpec(bytKey)
        val skf = SecretKeyFactory.getInstance("DES")
        val sk = skf.generateSecret(desKS)
        val cip = Cipher.getInstance("DES")
        cip.init(Cipher.DECRYPT_MODE, sk)
        return cip.doFinal(bytE)
    }

    /**
     * 輸入密碼的字元形式，返回位元組陣列形式。 如輸入字串：AD67EA2F3BE6E5AD
     * 返回位元組陣列：{173,103,234,47,59,230,229,173}
     */
    private fun getKeyByStr(str: String): ByteArray {
        val bRet = ByteArray(str.length / 2)
        for (i in 0 until str.length / 2) {
            val itg = (16 * getChrInt(str[2 * i]) + getChrInt(str[2 * i + 1]))
            bRet[i] = itg.toByte()
        }
        return bRet
    }

    /**
     * 計算一個16進位制字元的10進位制值 輸入：0-F
     */
    private fun getChrInt(chr: Char): Int {
        var iRet = 0
        if (chr == "0"[0]) iRet = 0
        if (chr == "1"[0]) iRet = 1
        if (chr == "2"[0]) iRet = 2
        if (chr == "3"[0]) iRet = 3
        if (chr == "4"[0]) iRet = 4
        if (chr == "5"[0]) iRet = 5
        if (chr == "6"[0]) iRet = 6
        if (chr == "7"[0]) iRet = 7
        if (chr == "8"[0]) iRet = 8
        if (chr == "9"[0]) iRet = 9
        if (chr == "A"[0]) iRet = 10
        if (chr == "B"[0]) iRet = 11
        if (chr == "C"[0]) iRet = 12
        if (chr == "D"[0]) iRet = 13
        if (chr == "E"[0]) iRet = 14
        if (chr == "F"[0]) iRet = 15
        return iRet
    }

    fun md5s(plainText: String): String? {
        var str: String? = null
        try {
            val md = MessageDigest.getInstance("MD5")
            md.update(plainText.toByteArray())
            val b = md.digest()
            var i: Int
            val buf = StringBuffer("")
            for (offset in b.indices) {
                i = b[offset].toInt()
                if (i < 0) i += 256
                if (i < 16) buf.append("0")
                buf.append(Integer.toHexString(i))
            }
            // System.out.println("result: " + buf.toString());// 32位的加密
            // System.out.println("result: " + buf.toString().substring(8,
            // 24));// 16位的加密
            str = buf.toString().substring(8, 24)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return str
    }

}
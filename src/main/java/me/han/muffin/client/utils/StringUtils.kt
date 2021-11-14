package me.han.muffin.client.utils

import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.FileWriter
import java.util.regex.Pattern

object StringUtils {
    const val EMPTY = ""

    fun isEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.isEmpty()
    }

    fun isNotEmpty(cs: CharSequence?): Boolean {
        return !isEmpty(cs)
    }

    fun repeat(str: String?, repeat: Int): String? {
        if (str == null) return null
        if (repeat <= 0) return EMPTY

        val inputLength = str.length
        if (repeat == 1 || inputLength == 0) return str
        if (inputLength == 1 && repeat <= 8192) return repeat(str[0], repeat)

        val outputLength = inputLength * repeat
        when (inputLength) {
            1 -> {
                return repeat(str[0], repeat)
            }
            2 -> {
                val ch0 = str[0]
                val ch1 = str[1]
                val output2 = CharArray(outputLength)
                var i = repeat * 2 - 2
                while (i >= 0) {
                    output2[i] = ch0
                    output2[i + 1] = ch1
                    --i
                    --i
                }
                return String(output2)
            }
        }

        return StringBuilder(outputLength).run {
            for (i in 0 until repeat) append(str)
            toString()
        }
    }

    fun repeat(str: String?, separator: String?, repeat: Int): String? {
        if (str == null || separator == null) return repeat(str, repeat)

        val result = repeat(str + separator, repeat)
        return removeEnd(result, separator)
    }

    fun repeat(ch: Char, repeat: Int): String {
        val buf = CharArray(repeat)
        for (i in repeat - 1 downTo 0) buf[i] = ch

        return buf.toString()
    }

    fun removeEnd(str: String?, remove: String): String? {
        if (isEmpty(str) || isEmpty(remove)) return str
        return if (str!!.endsWith(remove)) str.substring(0, str.length - remove.length) else str
    }

    fun rainbowColour(offset: Long, fade: Float): Color {
        val hue = (System.nanoTime() + offset).toFloat() / 1.0E10F % 1.0F
        val c = Color(Integer.toHexString(Color.HSBtoRGB(hue, 1.0f, 1.0f)).toLong(16).toInt())
        return Color(c.red.toFloat() / 255.0f * fade, c.green.toFloat() / 255.0f * fade, c.blue.toFloat() / 255.0f * fade, c.alpha.toFloat() / 255.0f)
    }

    fun copyToClipboard(s: String?) {
        val selection = StringSelection(s)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }

    fun writeFile(file: String, contents: String): Boolean {
        return try {
            FileWriter(file).buffered().run {
                write(contents)
                close()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun checkIsMandarin(text: String?): Boolean {
        val p = Pattern.compile("[\u4e00-\u9fa5]")
        val m = p.matcher(text)
        return m.find()
    }
}
package me.han.muffin.client.gui.util

import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object GuiUtils {
    private val df = DecimalFormat("#.###")

    fun roundSlider(f: Number): String? = df.format(f)

    fun roundSliderStep(input: Double, step: Double): Double {
        return round(input / step) * step
    }

    fun reCheckSliderRange(value: Double, min: Double, max: Double): Double {
        return min(max(value, min), max)
    }

}
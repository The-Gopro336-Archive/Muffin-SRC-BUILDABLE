package me.han.muffin.client.module.modules.other

import me.han.muffin.client.module.Module
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue

object RenderModeModule: Module("RenderMode", Category.OTHERS, "Allow you to change module render mode.", true, true, false) {

    val autoCrystal = EnumValue(RenderMode.Full, "AutoCrystal")
    val holeFiller = EnumValue(RenderMode.Full, "HoleFiller")
    val blockHighLight = EnumValue(RenderMode.Outline, "BlockHighLight")
    val lineWidth = NumberValue(1.5F, 0.1F, 3.0F, 0.1F, "LineWidth")

    enum class RenderMode {
        Solid, Outline, Full
    }

    init {
        addSettings(autoCrystal, holeFiller, blockHighLight, lineWidth)
    }

}
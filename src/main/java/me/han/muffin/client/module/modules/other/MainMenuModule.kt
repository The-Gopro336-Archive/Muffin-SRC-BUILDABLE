package me.han.muffin.client.module.modules.other

import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value

object MainMenuModule: Module("MainMenu", Category.OTHERS, "Custom main menu for Minecraft", true, true, false) {
    val custom = Value(false, "Custom")
    val shader = EnumValue(Shader.Random, "Shader")
    val FPS = NumberValue(60, 10, 300, 2, "FPS")

    init {
        addSettings(custom, shader, FPS)
    }

    private val SHADERS = arrayListOf("bluehole.fsh", "cube.fsh", "cubicpulse.fsh", "dj.fsh", "dna.fsh", "doom.fsh", "flappybird.fsh", "gas.fsh", "leafy.fsh", "minecraft.fsh", "nebula.fsh", "pinwheel.fsh", "red.fsh", "steam.fsh", "cool.fsh", "flame.fsh")

    enum class Shader {
        Random, BlueHole, Cube, CubicPulse, DJ, DNA, Doom, FlappyBird, Gas, Leafy, Minecraft, Nebula, Red, Pinwheel, Steam, Cool, Flame
    }

    fun getFileNameByMode(): String {
        return when (shader.value) {
            Shader.Random -> SHADERS[RandomUtils.random.nextInt(SHADERS.size)]
            Shader.BlueHole -> SHADERS[0]
            Shader.Cube -> SHADERS[1]
            Shader.CubicPulse -> SHADERS[2]
            Shader.DJ -> SHADERS[3]
            Shader.DNA -> SHADERS[4]
            Shader.Doom -> SHADERS[5]
            Shader.FlappyBird -> SHADERS[6]
            Shader.Gas -> SHADERS[7]
            Shader.Leafy -> SHADERS[8]
            Shader.Minecraft -> SHADERS[9]
            Shader.Nebula -> SHADERS[10]
            Shader.Red -> SHADERS[11]
            Shader.Pinwheel -> SHADERS[12]
            Shader.Steam -> SHADERS[13]
            Shader.Cool -> SHADERS[14]
            Shader.Flame -> SHADERS[15]
            else -> SHADERS[RandomUtils.random.nextInt(SHADERS.size)]
        }
    }

}
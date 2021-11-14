package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.GetSkyColourEvent
import me.han.muffin.client.event.events.render.SetupFogEvent
import me.han.muffin.client.event.events.render.UpdateFogColorEvent
import me.han.muffin.client.event.events.render.UpdateLightMapEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.render.drawBuffer
import me.han.muffin.client.utils.render.pos
import me.han.muffin.client.utils.render.withVertexFormat
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.MobEffects
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.lwjgl.opengl.GL11.GL_QUADS
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color
import java.util.*
import kotlin.math.pow

internal object FullBrightModule: Module("FullBright", Category.RENDER, true, "Turns up brightness to see in the dark.") {

    private val mode = EnumValue(Mode.Gamma, "Mode")
    private val gamma = NumberValue({ mode.value == Mode.Gamma }, 10f, 0f, 100f, 1f, "Gamma")

    private val brightness = NumberValue(1f, 0f, 3f, 0.1f, "Brightness")
    private val flashing = Value(false, "Flash")
    private val barrier = Value(false, "Barrier")

    private val skyColor = Value(false, "SkyColour")
    private val skyRainbow = Value({ skyColor.value }, false, "Sky-Rainbow")
    private val skyRed = NumberValue({ skyColor.value && !skyRainbow.value }, 90, 0, 255, 1, "Sky-Red")
    private val skyGreen = NumberValue({ skyColor.value && !skyRainbow.value }, 0, 0, 255, 1, "Sky-Green")
    private val skyBlue = NumberValue({ skyColor.value && !skyRainbow.value }, 255, 0, 255, 1, "Sky-Blue")

    private val rainbowSpeed = NumberValue({ skyColor.value && skyRainbow.value }, 3, 0, 10, 1, "RainbowSpeed")
    private val rainbowLength = NumberValue({ skyColor.value && skyRainbow.value }, 10.0F, 1.0F, 20.0F, 0.5F, "RainbowLength")
    private val indexedHue = NumberValue({ skyColor.value && skyRainbow.value }, 0.5F, 0.0F, 1.0F, 0.05F, "IndexedHue")

    private val END_SKY_TEXTURES = ResourceLocation("textures/environment/end_sky.png")
    private var world: World? = null

    private enum class Mode {
        Normal, Gamma, Potion
    }

    init {
        addSettings(
            mode, gamma, brightness, flashing, barrier, skyColor, skyRed, skyGreen, skyBlue, skyRainbow, rainbowSpeed, rainbowLength, indexedHue
        )
    }

    private var colour: Color? = null
    var index = 0

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        var red = skyRed.value
        var green = skyGreen.value
        var blue = skyBlue.value

        val primaryHsb = Color.RGBtoHSB(red, green, blue, null)
        val lengthMs = rainbowLength.value.pow(1000.0F)
        val timedHue = System.currentTimeMillis() % lengthMs.toLong() / lengthMs

        if (skyRainbow.value) {
            val hue = timedHue + indexedHue.value * 0.05F * index++
            val color = Color.HSBtoRGB(hue, primaryHsb[1], primaryHsb[2])

            red = color shr 16 and 0xFF
            green = color shr 8 and 0xFF
            blue = color and 0xFF
        }

        colour = Color(red, green, blue)

        if (mode.value == Mode.Gamma) {
            Globals.mc.gameSettings.gammaSetting = gamma.value
        } else if (mode.value == Mode.Potion) {
            Globals.mc.gameSettings.gammaSetting = 1.0F
            Globals.mc.player.addPotionEffect(PotionEffect(MobEffects.NIGHT_VISION, 1215, 0))
        }

    }

    override fun onEnable() {
        if (mode.value == Mode.Normal) updateLightmap(brightness.value, flashing.value)
    }

    override fun onDisable() {
        when (mode.value) {
            Mode.Potion -> {
                Globals.mc.player.removePotionEffect(MobEffects.NIGHT_VISION)
            }
            Mode.Normal -> {
                generateLightBrightnessTable()
            }
            Mode.Gamma -> {
                Globals.mc.gameSettings.gammaSetting = 1.0F
            }
        }
    }

    @Listener
    private fun onUpdateLightmap(event: UpdateLightMapEvent) {
        if (mode.value != Mode.Normal) return
        updateLightmap(brightness.value, flashing.value)
    }

    private fun updateLightmap(brightness: Float, flash: Boolean) {
        if (world != Globals.mc.world) {
            if (Globals.mc.world != null) {
                val randomValue = RandomUtils.nextFloat(0.0F, 1.0F)
                Arrays.fill(Globals.mc.world.provider.lightBrightnessTable, if (flash) randomValue else brightness)
            }
            world = Globals.mc.world
        }
    }

    /**
     * Creates the light to brightness table
     */
    private fun generateLightBrightnessTable() {
        if (Globals.mc.world == null) return

        for (i in 0..15) {
            val f1 = 1.0F - i.toFloat() / 15.0F
            Globals.mc.world.provider.lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * 1.0F + 0.0F
        }
    }

    @Listener
    private fun onUpdateFogColor(event: UpdateFogColorEvent) {
        if (skyColor.value && colour != null && Globals.mc.player != null && Globals.mc.player.ticksExisted > 20) {
            GlStateManager.clearColor(colour?.red!! / 255f, colour?.green!! / 255f, colour?.blue!! / 255f, 0f)
            event.cancel()
        }
    }

    @Listener
    private fun onSetupFog(event: SetupFogEvent) {
        if (skyColor.value && Globals.mc.player != null && Globals.mc.player.ticksExisted > 20) {
            GlStateManager.setFogDensity(0.1F)
            event.cancel()
        }
    }

    @Listener
    private fun onGetSkyColourEvent(event: GetSkyColourEvent) {
        if (fullNullCheck()) return

        if (skyColor.value && colour != null && Globals.mc.player.ticksExisted > 20) {
            event.vec3d = Vec3d(colour!!.red.toDouble(), colour!!.green.toDouble(), colour!!.blue.toDouble())
            event.cancel()
        }
    }

    private fun renderSky() {
        GlStateManager.disableFog()
        GlStateManager.disableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)

        RenderHelper.disableStandardItemLighting()
        GlStateManager.depthMask(false)
        Globals.mc.renderEngine.bindTexture(END_SKY_TEXTURES)

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        for (i in 0 until 6) {
            GlStateManager.pushMatrix()

            if (i == 1) GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f)
            if (i == 2) GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f)
            if (i == 3) GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f)
            if (i == 4) GlStateManager.rotate(90.0f, 0.0f, 0.0f, 1.0f)
            if (i == 5) GlStateManager.rotate(-90.0f, 0.0f, 0.0f, 1.0f)

            GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_TEX_COLOR drawBuffer {
                pos(-100.0, -100.0, -100.0, tex = 0.0 to 0.0, colour = Colour(40, 40, 40))
                pos(-100.0, -100.0, 100.0, tex = 0.0 to 16.0, colour = Colour(40, 40, 40))
                pos(100.0, -100.0, 100.0, tex = 16.0 to 16.0, colour = Colour(40, 40, 40))
                pos(100.0, -100.0, -100.0, tex = 16.0 to 0.0, colour = Colour(40, 40, 40))
            }

            GlStateManager.popMatrix()
        }

        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.enableAlpha()
    }

}
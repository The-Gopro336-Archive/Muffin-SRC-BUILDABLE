package me.han.muffin.client.module.modules.other

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.command.commands.LiveCommand
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.RenderTickEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.event.events.gui.RenderGuiBackgroundEvent
import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.event.events.render.RenderLiquidVisionEvent
import me.han.muffin.client.event.events.render.overlay.RenderPotionEffectsEvent
import me.han.muffin.client.event.events.render.overlay.RenderPotionIconsEvent
import me.han.muffin.client.gui.MuffinGuiScreen
import me.han.muffin.client.gui.font.AWTFontRenderer
import me.han.muffin.client.gui.font.util.Opacity
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.HudManager
import me.han.muffin.client.manager.managers.ModuleManager
import me.han.muffin.client.manager.managers.SpeedManager.speedKmh
import me.han.muffin.client.manager.managers.SpeedManager.speedMps
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.CircularArray
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.extensions.kotlin.interfaces.DisplayEnum
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.network.LagCompensator
import me.han.muffin.client.utils.network.TpsCalculator
import me.han.muffin.client.utils.render.AnimationUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.resources.I18n
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object HudModule: Module("HUD", Category.OTHERS, true, true, "Enable/Disable the HUD.") {

    private val effectHud = EnumValue(EffectHud.Hide, "EffectHud")
    private val liquidVision = Value(true, "LiquidVision")

    private val playerOnline = Value(true, "PlayerOnline")
    private val fps = Value(true, "FPS")
    private val averageFps = Value({ fps.value }, true, "AverageFPS")
    private val ping = Value(true, "Ping")
    private val tps = Value(true, "TPS")
    private val speed = Value(true, "Speed")
    private val showAverageSpeed = Value({ speed.value }, true, "AverageSpeed")
    private val speedUnit = EnumValue({ speed.value }, SpeedUnit.KMH, "SpeedUnit")
    private val potion = Value(true, "Potion")
    private val time = Value(true, "Time")
    private val serverBrand = Value(false, "ServerBrand")

    private val direction = Value(false, "Direction")
    private val coordinates = Value(false, "Coordinates")
    private val netherCoords = Value({ coordinates.value }, false, "NetherCoords")
    private val coordDecimal = NumberValue({ coordinates.value },2, 0, 4, 1, "CoordsDecimal")

    private val cleanGui = Value(true, "CleanGui")
    private val blur = Value(true, "Blur")

    private val activeModule = Value(true, "ActiveModules")
    private val activeModulePos = EnumValue(ActiveModulePos.Up, "AModulePos")
    private val activeModuleOrganize = EnumValue(SortingMode.Length, "AModuleOrganize")
    private val activeModuleContainer = Value(true, "AModuleContainer")

    private var chatAnimationY = 0

    private var rainbowSpeed = 0F
    private var rainbowBrightness = 0F
    private var rainbowWidth = 0.0F

    private var updateTime = 0L
    private var prevFps = 0
    private var currentFps = 0

    private val longFps = CircularArray.create<Int>(10)

    private var prevAvgFps = 0
    private var currentAvgFps = 0

    private val tpsBuffer = CircularArray.create(20, 20f)
    private val newTpsBuffer = CircularArray.create(20, 20f)

    private val hue = Opacity(0)

    private var cachedModules = emptyList<Module>()
    private val moduleAnimationTimer = Timer()
    private val cachedModuleAnimation = ConcurrentHashMap<Module, Int>()

    private val speedList = ArrayDeque<Double>()
    var averageSpeed = "0.0"

    private var onlinePlayers = 0

    private var coordinate = "No Coordinates"

    private val fontManager = Muffin.getInstance().fontManager

    private var cachedEffects = emptyList<PotionEffect>()

    private enum class SortingMode(override val displayName: String, val comparator: Comparator<Module>): DisplayEnum {
        Length("Length", compareByDescending { fontManager.getStringWidth(it.name + if (it.hudInfo == null) "" else " [${it.hudInfo}]") }),
        Alphabet("Alphabet", compareBy { it.name }),
        Category("Category", compareBy { it.category.ordinal })
    }

    enum class ActiveModulePos {
        Up, Down
    }

    enum class EffectHud {
        Keep, Hide, Move
    }

    init {
        addSettings(
            effectHud, liquidVision,
            speed, showAverageSpeed, speedUnit,
            playerOnline, fps, averageFps, ping, tps, potion, time, serverBrand,
            coordinates, netherCoords, coordDecimal, direction,
            cleanGui, blur,
            activeModule, activeModulePos, activeModuleOrganize, activeModuleContainer
        )
    }

    override fun onEnable() {
        rainbowSpeed = ColorControl.INSTANCE.rainbowSpeed.value
        rainbowBrightness = ColorControl.INSTANCE.rainbowBrightness.value
        rainbowWidth = ColorControl.INSTANCE.rainbowWidth.value
    }

    override fun onDisable() {
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE) return
        if (Globals.mc.gameSettings.showDebugInfo || Globals.mc.gameSettings.hideGUI) return
        HudManager.getHudManager().onTicking()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (fullNullCheck() || event.stage != EventStageable.EventStage.PRE) return
        if (Globals.mc.gameSettings.showDebugInfo || Globals.mc.gameSettings.hideGUI) return

        if (showAverageSpeed.value) updateSpeedList()
        if (potion.value) cachedEffects = Globals.mc.player.activePotionEffects.sortedBy { -fontManager.getStringWidth(formatPotionEffectAmplifier(it)) }
        if (playerOnline.value) onlinePlayers = if (Globals.mc.isSingleplayer) -1 else Globals.mc.player.connection.playerInfoMap.size

        if (tps.value) {
            tpsBuffer.add(LagCompensator.tickRate)
            newTpsBuffer.add(TpsCalculator.tickRate)
        }

        if (coordinates.value) updateCoordinates()
        if (activeModule.value) cachedModules = ModuleManager.modules.filter { !it.isDrawn }.sortedWith(activeModuleOrganize.value.comparator)
    }

    @JvmStatic
    fun updateFpsCounter(fpsIn: Int) {
        prevFps = currentFps
        currentFps = fpsIn

        longFps.add(fpsIn)

        prevAvgFps = currentAvgFps
        currentAvgFps = longFps.average().roundToInt()
        updateTime = System.currentTimeMillis()
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        val sr = event.scaledResolution ?: return

        if (fullNullCheck() || Globals.mc.gameSettings.showDebugInfo || Globals.mc.gameSettings.hideGUI) return
        updateChatAnimation(event.partialTicks)

        var infoYUp = 2
        var infoYDown = sr.scaledHeight - 4 - chatAnimationY

        HudManager.getHudManager().onRender(event.partialTicks)
        AWTFontRenderer.assumeNonVolatile = true

        if (potion.value && cachedEffects.isNotEmpty()) {
            for (effect in cachedEffects) {
                val formattedEffect = formatPotionEffectAmplifier(effect)
                if (activeModulePos.value == ActiveModulePos.Down) {
                    fontManager.drawStringWithShadow(formattedEffect, sr.scaledWidth - fontManager.getStringWidth(formattedEffect) - 2F, infoYUp.toFloat(), effect.potion.liquidColor)
                    infoYUp += 10
                } else if (activeModulePos.value == ActiveModulePos.Up) {
                    fontManager.drawStringWithShadow(formattedEffect, sr.scaledWidth - fontManager.getStringWidth(formattedEffect) - 2F, (infoYDown - fontManager.stringHeight).toFloat(), effect.potion.liquidColor)
                    infoYDown -= 10
                }
            }
        }

        if (serverBrand.value) {
            val finalServerBrand = ChatFormatting.RESET.toString() + InfoUtils.getServerBrand()
            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(finalServerBrand, sr.scaledWidth - fontManager.getStringWidth(finalServerBrand) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(finalServerBrand, sr.scaledWidth - fontManager.getStringWidth(finalServerBrand) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (speed.value) {
            val playerSpeed = if (showAverageSpeed.value) averageSpeed else MathUtils.round(if (speedUnit.value == SpeedUnit.KMH) Globals.mc.player.speedKmh else Globals.mc.player.speedMps, 2)
            val speed = formatColour("Speed") + playerSpeed + " " + speedUnit.value.displayName

            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(speed, sr.scaledWidth - fontManager.getStringWidth(speed) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(speed, sr.scaledWidth - fontManager.getStringWidth(speed) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (time.value) {
            val timeString = formatColour("Time") + InfoUtils.time()

            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(timeString, sr.scaledWidth - fontManager.getStringWidth(timeString) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(timeString, sr.scaledWidth - fontManager.getStringWidth(timeString) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (playerOnline.value) {
            val online = ChatFormatting.RESET.toString() + if (onlinePlayers == -1) "SinglePlayer" else "${ChatFormatting.GRAY}Online " + ChatFormatting.RESET + onlinePlayers

            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(online, sr.scaledWidth - fontManager.getStringWidth(online) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(online, sr.scaledWidth - fontManager.getStringWidth(online) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (fps.value) {
            val deltaTime = AnimationUtils.toDeltaTimeFloat(updateTime) / 1000.0F
            val fps = MathUtils.interpolate(currentFps, prevFps, deltaTime).roundToInt()
            val avg = MathUtils.interpolate(currentAvgFps, prevAvgFps, deltaTime).roundToInt()

            var min = 6969
            var max = 0
            for (value in longFps) {
                if (value != 0) min = min(value, min)
                max = max(value, max)
            }

            val placeholder = formatColour("FPS") + fps + if (averageFps.value) getFormatBraces(avg.toString(), true) else ""
            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(placeholder, sr.scaledWidth - fontManager.getStringWidth(placeholder) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(placeholder, sr.scaledWidth - fontManager.getStringWidth(placeholder) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (ping.value) {
            val pingValue = formatColour("Ping") + InfoUtils.ping()
            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(pingValue, sr.scaledWidth - fontManager.getStringWidth(pingValue) - 2, infoYUp)
                infoYUp += 10
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(pingValue, sr.scaledWidth - fontManager.getStringWidth(pingValue) - 2, infoYDown - fontManager.stringHeight)
                infoYDown -= 10
            }
        }

        if (tps.value) {
            val tpsValue = formatColour("TPS") + "%.2f".format(tpsBuffer.average()) + " " + getFormatBraces("%.2f".format(newTpsBuffer.average()), true)

            if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(tpsValue, sr.scaledWidth - fontManager.getStringWidth(tpsValue) - 2, infoYUp)
            } else if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(tpsValue, sr.scaledWidth - fontManager.getStringWidth(tpsValue) - 2, infoYDown - fontManager.stringHeight)
            }
        }

        var directionAndCoordsYPos = sr.scaledHeight - 3

        if (!LiveCommand.isLive && coordinates.value) {
            fontManager.drawStringWithShadow(coordinate, 1, directionAndCoordsYPos - fontManager.stringHeight + 1 - chatAnimationY)
            directionAndCoordsYPos -= 10
        }

        if (direction.value) {
            val direction = InfoUtils.getDirection()
            val directionPlaceholder = ChatFormatting.GRAY.toString() + direction.displayFacing + ChatFormatting.RESET + " [" + direction.displayToward + "] "
            fontManager.drawStringWithShadow(directionPlaceholder, 1, directionAndCoordsYPos - fontManager.stringHeight + 1 - chatAnimationY)
        }

        if (activeModule.value) drawActiveModules(sr)

        AWTFontRenderer.assumeNonVolatile = false
    }

    @Listener
    private fun onHidingPotionEffects(event: RenderPotionIconsEvent) {
        if (effectHud.value == EffectHud.Hide) event.cancel()
    }

    @Listener
    private fun onHidingPotionIcon(event: RenderPotionEffectsEvent) {
        if (effectHud.value == EffectHud.Hide) event.cancel()
    }

    @Listener
    private fun onRenderGuiBackground(event: RenderGuiBackgroundEvent) {
        if (cleanGui.value && Globals.mc.currentScreen !is MuffinGuiScreen) event.cancel()
    }

    @Listener
    private fun onGuiScreenEvent(event: GuiScreenEvent.Displayed) {
        if (fullNullCheck()) return

        if (blur.value && !Globals.mc.entityRenderer.isShaderActive && event.screen != null) {
            Globals.mc.entityRenderer.loadShader(ResourceLocation("shader/blur/blur.json"))
        } else if (Globals.mc.entityRenderer.isShaderActive && event.screen == null) {
            Globals.mc.entityRenderer.stopUseShader()
        }

    }

    @Listener
    private fun onRenderLiquidVision(event: RenderLiquidVisionEvent) {
        if (liquidVision.value) event.cancel()
    }

    private fun drawActiveModules(resolution: ScaledResolution) {
        if (cachedModules.isEmpty()) return

        var cachedOpacity = hue.opacity

        hue.interp(256F, rainbowSpeed - 1.0)
        if (hue.opacity > 255) hue.opacity = 0F

        val yExtraMinus = 0
        var y = if (activeModulePos.value == ActiveModulePos.Down) 2 else if (effectHud.value == EffectHud.Move) -7 + fontManager.stringHeight else 2

        var lastResolutionWidth = resolution.scaledWidth

        for (module in cachedModules) {
            if (cachedOpacity > 255) cachedOpacity = 0.0F

            cachedModuleAnimation.putIfAbsent(module, -8)
            val x = resolution.scaledWidth - cachedModuleAnimation[module]!! - 2

            var lastModule = module
            val info = module.hudInfo
            val text = module.name + if (info == null) "" else " ${ChatFormatting.GRAY}[${ChatFormatting.WHITE}$info${ChatFormatting.GRAY}]"

            if (module.isEnabled) {
                if (cachedModuleAnimation[module]!! < fontManager.getStringWidth(text) && moduleAnimationTimer.passed(10.0)) {
                    cachedModuleAnimation[module] = cachedModuleAnimation[module]!! + fontManager.getStringWidth(text) / 12
                    moduleAnimationTimer.reset()
                }
                if (cachedModuleAnimation[module]!! > fontManager.getStringWidth(text)) {
                    cachedModuleAnimation[module] = cachedModuleAnimation[module]!! - 1
                    lastModule = module
                }
            } else if (cachedModuleAnimation[module]!! > -8 && moduleAnimationTimer.passed(10.0)) {
                cachedModuleAnimation[module] = cachedModuleAnimation[module]!! - fontManager.getStringWidth(text) / 12
                moduleAnimationTimer.reset()
            }

            if (!lastModule.isEnabled && cachedModuleAnimation[module]!! <= -8) continue
            var colour = fontManager.color

            if (ColorControl.INSTANCE.moduleListMode.value == ColorControl.ModuleListMode.RAINBOW) {
                val rainB = Color.getHSBColor(cachedOpacity / 255.0F, rainbowBrightness, 1.0F)
                colour = rainB.rgb
            }

            if (activeModuleContainer.value) {
                if (activeModulePos.value == ActiveModulePos.Up) {
                    // background rect
                    RenderUtils.rectangle(x - 1.9, y - 1.0, resolution.scaledHeight_double, y + 8.85, Colour(8, 8, 8, 80).toHex())

                    // side line of line with colour
                    RenderUtils.rectangle(x - 3.0, y - 1.0, x - 2.0, y + 10.0, colour)

                    // bottom line of rect with colour
                    if (module != cachedModules[0]) {
                        RenderUtils.rectangle(x - 3.0, y - 1.0, lastResolutionWidth - 2.0, y.toDouble(), colour)
                    }

                    // last line but it WIP
                    // TODO: Fix last module not drawing
                    if (module == cachedModules[cachedModules.size - 1]) {
                        RenderUtils.rectangle(x - 2.0, y + 8.5, resolution.scaledWidth_double, y + 9.7, colour)
                    }

                } else if (activeModulePos.value == ActiveModulePos.Down) {
                    ChatManager.sendMessage("Active Module Container Only Work On Top Right.")
                    activeModuleContainer.value = false
                }
            }

            if (activeModulePos.value == ActiveModulePos.Up) {
                fontManager.drawStringWithShadow(text, x.toFloat(), y.toFloat(), colour)
            } else if (activeModulePos.value == ActiveModulePos.Down) {
                fontManager.drawStringWithShadow(text, x.toFloat(), resolution.scaledHeight - 9.0F - y, colour)
            }

            lastResolutionWidth = x
            y += yExtraMinus + fontManager.stringHeight
            cachedOpacity += 20 - rainbowWidth
        }

    }

    private fun formatPotionEffectAmplifier(first: PotionEffect): String {
        var firstEffectName = I18n.format(first.potion.name)
        when (first.amplifier) {
            1 -> firstEffectName += " " + I18n.format("enchantment.level.2")
            2 -> firstEffectName += " " + I18n.format("enchantment.level.3")
            3 -> firstEffectName += " " + I18n.format("enchantment.level.4")
        }
        return firstEffectName + " " + ChatFormatting.GRAY + Potion.getPotionDurationString(first, 1.0f)
    }

    private fun updateChatAnimation(partialTicks: Float) {
        if (Globals.mc.currentScreen is GuiChat) {
            if (chatAnimationY < 12) chatAnimationY += (3 * partialTicks).toInt()
            if (chatAnimationY > 12) chatAnimationY = 12
        } else {
            if (chatAnimationY > 0) chatAnimationY -= (3 * partialTicks).toInt()
            if (chatAnimationY < 0) chatAnimationY = 0
        }
    }

    private fun updateCoordinates() {
        val player = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return

        val inHell = Globals.mc.player.dimension == -1
        val scale = if (!inHell) 0.125 else 8.0

        coordinate =
            "${ChatFormatting.GRAY}XYZ: ${getFormattedCoords(player.positionVector, true)}" +
            if (netherCoords.value) " ${getFormatBraces(getFormattedCoords(player.positionVector.scale(scale), false), false)}" else ""
    }

    private fun getFormatBraces(baseString: String, resetColour: Boolean): String {
        val resetHolder = if (resetColour) ChatFormatting.RESET.toString() else ""
        return "${ChatFormatting.GRAY}[$resetHolder$baseString${ChatFormatting.GRAY}]"
    }

    private fun getFormattedCoords(pos: Vec3d, appendY: Boolean): String {
        val x = roundOrInt(pos.x)
        val y = roundOrInt(pos.y)
        val z = roundOrInt(pos.z)
        return StringBuilder().run {
            append("${ChatFormatting.RESET}$x")
            if (appendY) appendWithComma(y)
            appendWithComma(z)
            toString()
        }
    }

    private fun roundOrInt(input: Double): String {
        return "%.${coordDecimal.value}f".format(input)
    }

    private fun StringBuilder.appendWithComma(string: String) = append(if (length > 0) "${ChatFormatting.GRAY}, ${ChatFormatting.RESET}$string" else "${ChatFormatting.RESET}$string")

    private fun formatColour(info: String): String {
        return "${ChatFormatting.GRAY}$info ${ChatFormatting.RESET}"
    }

    private fun updateSpeedList() {
        val speed = Globals.mc.player.speedMps

        if (speed > 0.0 || Globals.mc.player.ticksExisted % 4 == 0) {
            speedList.add(speed) // Only adding it every 4 ticks if speed is 0
        } else {
            speedList.pollFirst()
        }

        while (speedList.size > 1.0F * 20.0f) speedList.pollFirst()

        var tempAverageSpeed = if (speedList.isEmpty()) 0.0 else speedList.sum() / speedList.size
        tempAverageSpeed *= speedUnit.value.multiplier
        tempAverageSpeed = MathUtils.round(tempAverageSpeed, 2)

        averageSpeed = "%.2f".format(tempAverageSpeed)
    }

    @Suppress("UNUSED")
    private enum class SpeedUnit(override val displayName: String, val multiplier: Double): DisplayEnum {
        MPS("m/s", 1.0),
        KMH("km/h", 3.6),
        MPH("mph", 2.237) // Monkey Americans
    }

}
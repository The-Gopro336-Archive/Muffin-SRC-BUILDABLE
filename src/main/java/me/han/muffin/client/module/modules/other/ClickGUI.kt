package me.han.muffin.client.module.modules.other


import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.command.commands.LiveCommand
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.click.ClickGui
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.value.*
import org.lwjgl.input.Keyboard

/**
 * ticks running
 * @see me.han.muffin.module.modules.other.CombatStatusModule
 */
object ClickGUI: Module("ClickGui", Category.OTHERS,true, false, Keyboard.KEY_Y, "Open the click gui.") {

    private val page = EnumValue(Pages.Panel, "Page")
    val guiWidth = NumberValue(0, 0, 5, 1, "GuiWidth")
    val guiHeight = NumberValue(0, 0, 5, 1, "GuiHeight")
    val panelAnimation = Value({ page.value == Pages.Panel },false, "PanelAnimation")
    val drawParticle = Value(false, "DrawParticle")
    val drawDescription = Value({ page.value == Pages.Panel },true, "DrawDescription")
    val guiBackground = EnumValue(BackgroundMode.Black, "Background")
    val panelBlur = Value({ page.value == Pages.Panel },true, "PanelBlur")
    val panelBlurIntensity = NumberValue({ page.value == Pages.Panel && panelBlur.value }, 8, 0, 20, 1, "BlurIntensity")

    val panelBackground = Value({ page.value == Pages.Panel },false, "PanelBackground")
    private val panelBackgroundRed = NumberValue({ page.value == Pages.Panel && panelBackground.value },90, 0, 255, 1, "PanelRed")
    private val panelBackgroundGreen = NumberValue({ page.value == Pages.Panel && panelBackground.value },90, 0, 255, 1, "PanelGreen")
    private val panelBackgroundBlue = NumberValue({ page.value == Pages.Panel && panelBackground.value },90, 0, 255, 1, "PanelBlue")
    private val panelBackgroundAlpha = NumberValue({ page.value == Pages.Panel && panelBackground.value },95, 0, 255, 1, "PanelAlpha")

    val panelLineThickness = NumberValue({ page.value == Pages.Panel && panelBackground.value },0.5F, 0.0F, 3.0F, 0.1F, "PanelLineThick")

    private val panelTextBackgroundRed = NumberValue({ page.value == Pages.PanelText },255, 0, 255, 1, "PanelTextRed")
    private val panelTextBackgroundGreen = NumberValue({ page.value == Pages.PanelText },255, 0, 255, 1, "PanelTextGreen")
    private val panelTextBackgroundBlue = NumberValue({ page.value == Pages.PanelText },255, 0, 255, 1, "PanelTextBlue")
    private val panelTextBackgroundAlpha = NumberValue({ page.value == Pages.PanelText },255, 0, 255, 1, "PanelTextAlpha")

    val buttonRect = Value({ page.value == Pages.Button },true, "ButtonRect")
    val buttonAnimation = Value({ page.value == Pages.Button && buttonRect.value },true, "ButtonAnimation")

    private val buttonEnabledTextRed = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonEnabledTextRed")
    private val buttonEnabledTextGreen = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonEnabledTextGreen")
    private val buttonEnabledTextBlue = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonEnabledTextBlue")
    private val buttonDisabledTextRed = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonDisabledTextRed")
    private val buttonDisabledTextGreen = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonDisabledTextGreen")
    private val buttonDisabledTextBlue = NumberValue({ page.value == Pages.ButtonText },255, 0, 255, 1, "ButtonDisabledTextBlue")

    val settingAnimation = Value({ page.value == Pages.Setting }, true, "SettingAnimation")
    val settingEnabledRect = Value({ page.value == Pages.Setting },true, "SettingEnabledRect")
    private val settingEnabledTextRed = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingEnabledTextRed")
    private val settingEnabledTextGreen = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingEnabledTextGreen")
    private val settingEnabledTextBlue = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingEnabledTextBlue")
    private val settingEnabledTextAlpha = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingEnabledTextAlpha")

    private val settingDisabledTextRed = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingDisabledTextRed")
    private val settingDisabledTextGreen = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingDisabledTextGreen")
    private val settingDisabledTextBlue = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingDisabledTextBlue")
    private val settingDisabledTextAlpha = NumberValue({ page.value == Pages.SettingText },255, 0, 255, 1, "SettingDisabledTextAlpha")

    init {
        addSettings(
            page,
            guiWidth, guiHeight,
            panelAnimation, drawParticle, drawDescription, guiBackground,
            panelBlur, panelBlurIntensity,
            panelBackground, panelBackgroundRed, panelBackgroundGreen, panelBackgroundBlue, panelBackgroundAlpha,
            panelLineThickness,
            panelTextBackgroundRed, panelTextBackgroundGreen, panelTextBackgroundBlue, panelTextBackgroundAlpha,
            buttonRect, buttonAnimation,
            buttonEnabledTextRed, buttonEnabledTextGreen, buttonEnabledTextBlue,
            buttonDisabledTextRed, buttonDisabledTextGreen, buttonDisabledTextBlue,
            settingAnimation, settingEnabledRect,
            settingEnabledTextRed, settingEnabledTextGreen, settingEnabledTextBlue, settingEnabledTextAlpha,
            settingDisabledTextRed, settingDisabledTextGreen, settingDisabledTextBlue, settingDisabledTextAlpha
        )
    }

    enum class BackgroundMode {
        None, Black, Blur
    }

    enum class Pages {
        Panel, PanelText, Button, ButtonText, Setting, SettingText
    }

    var startCounting = false
    var count = 0
    var ticksRun = 0

    /*
    val getPanelHue get() = Color.HSBtoRGB(
        ColourUtils.toF(panelBackgroundHue.value),
        ColourUtils.toF(panelBackgroundSaturation.value),
        ColourUtils.toF(panelBackgroundBrightness.value))

    fun getPanelHue(alpha: Int): Int {
        return Color.HSBtoRGB(
            ColourUtils.toF(panelBackgroundHue.value),
            ColourUtils.toF(panelBackgroundSaturation.value),
            ColourUtils.toF(alpha))
    }
     */

    val panelBackgroundColour get() =
        Colour(panelBackgroundRed.value, panelBackgroundGreen.value, panelBackgroundBlue.value, panelBackgroundAlpha.value)

    val panelBackgroundTextColour get() =
        Colour(panelTextBackgroundRed.value, panelTextBackgroundGreen.value, panelTextBackgroundBlue.value, panelTextBackgroundAlpha.value)

    val buttonEnabledTextColour get() =
        Colour(buttonEnabledTextRed.value, buttonEnabledTextGreen.value, buttonEnabledTextBlue.value)

    val buttonDisabledTextColour get() =
        Colour(buttonDisabledTextRed.value, buttonDisabledTextGreen.value, buttonDisabledTextBlue.value)

    val settingEnabledTextColour get() =
        Colour(settingEnabledTextRed.value, settingEnabledTextGreen.value, settingEnabledTextBlue.value, settingEnabledTextAlpha.value)

    val settingDisabledTextColour get() =
        Colour(settingDisabledTextRed.value, settingDisabledTextGreen.value, settingDisabledTextBlue.value, settingDisabledTextAlpha.value)

    override fun onEnable() {
        if (fullNullCheck()) return

        if (LiveCommand.isLive) startCounting = true

        if (startCounting) {
            startCounting = false
            count++

            if (ticksRun < 20 && count > 2) {
                Globals.mc.displayGuiScreen(ClickGui.getClickGui())
                disable()
                ticksRun = 0
                count = 0
                return
            }

            ChatManager.sendDeleteMessage("${ChatFormatting.RED} You had clicked $count time", "clicked", ChatIDs.CLICK_GUI_LIVE_COUNT)
            return
        }

        Globals.mc.displayGuiScreen(ClickGui.getClickGui())
        isEnabled = false
    }

}
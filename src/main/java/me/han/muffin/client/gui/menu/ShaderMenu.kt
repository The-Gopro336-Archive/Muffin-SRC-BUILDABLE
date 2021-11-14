package me.han.muffin.client.gui.menu

import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.google.common.util.concurrent.Runnables
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.altmanager.GuiAltList
import me.han.muffin.client.gui.menu.utils.GLSLSandboxShader
import me.han.muffin.client.mixin.ClientLoader
import me.han.muffin.client.module.modules.other.MainMenuModule
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.render.drawBuffer
import me.han.muffin.client.utils.render.pos
import me.han.muffin.client.utils.render.withVertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.I18n
import net.minecraft.client.settings.GameSettings
import net.minecraft.realms.RealmsBridge
import net.minecraft.util.ResourceLocation
import net.minecraft.util.StringUtils
import net.minecraft.util.math.MathHelper
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.WorldServerDemo
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.gui.NotificationModUpdateScreen
import net.minecraftforge.fml.client.GuiModList
import net.minecraftforge.fml.common.FMLCommonHandler
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.GL_QUADS
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GLContext
import java.io.IOException
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class ShaderMenu: GuiMainMenu() {
    private val backgroundShader = try {
        GLSLSandboxShader("/shaders/${MainMenuModule.getFileNameByMode()}")
    } catch (e: IOException) {
        throw IllegalStateException("Failed to load background shader", e)
    }

    val MORE_INFO_TEXT = "Please click " + TextFormatting.UNDERLINE + "here" + TextFormatting.RESET + " for more information."

    private var splashText = "missingno"
    private val minecraftRoll = RandomUtils.random.nextFloat()

    private var lowestButtonY = 0

    private var buttonResetDemo: GuiButton? = null

    private var openGLWarning2Width = 0

    private var openGLWarning1Width = 0
    private var openGLWarningX1 = 0
    private var openGLWarningY1 = 0
    private var openGLWarningX2 = 0
    private var openGLWarningY2 = 0
    private var openGLWarning1 = ""
    private var openGLWarning2 = MORE_INFO_TEXT
    private var openGLWarningLink: String? = null

    /** The Object object utilized as a thread lock when performing non thread-safe operations  */
    private val threadLock = Any()

    private var hasCheckedForRealmsNotification = false
    private var realmsNotification: GuiScreen? = null

    private var modUpdateNotification: NotificationModUpdateScreen? = null

    private var widthCopyright = 0
    private var widthCopyrightRest = 0

    /** Minecraft Realms button.  */
    private var realmsButton: GuiButton? = null

    private val SPLASH_TEXTS = ResourceLocation("texts/splashes.txt")
    private val MINECRAFT_TITLE_TEXTURES = ResourceLocation("textures/gui/title/minecraft.png")
    private val field_194400_H = ResourceLocation("textures/gui/title/edition.png")

    private var modButton: GuiButton? = null

    private var initTime = System.currentTimeMillis()

    init {
        /*
        var iresource: IResource? = null
        try {
            val list = Lists.newArrayList<String>()
            iresource = Globals.mc.resourceManager.getResource(SPLASH_TEXTS)
            val bufferedReader = BufferedReader(InputStreamReader(iresource.inputStream, StandardCharsets.UTF_8))
            var s: String? = null
            while (bufferedReader.readLine()?.also { s = it } != null) {
                s = s?.trim { it <= ' ' }
                if (s!!.isNotEmpty()) {
                    list.add(s)
                }
            }
            if (list.isNotEmpty()) {
                while (true) {
                    this.splashText = list[RandomUtils.random.nextInt(list.size)]
                    if (this.splashText.hashCode() != 125780783) {
                        break
                    }
                }
            }
        } catch (var8: IOException) {
        } finally {
            IOUtils.closeQuietly(iresource)
        }
         */
        splashText = "han is so fucking handsome"

        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported()) {
            this.openGLWarning1 = I18n.format("title.oldgl1")
            this.openGLWarning2 = I18n.format("title.oldgl2")
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game"
        }
    }

    override fun updateScreen() {
        if (!MainMenuModule.custom.value) {
            super.updateScreen()
            return
        }

        if (areRealmsNotificationsEnabled()) {
            realmsNotification?.updateScreen()
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!MainMenuModule.custom.value) {
            super.drawScreen(mouseX, mouseY, partialTicks)
            return
        }

        GlStateManager.enableAlpha()
        GlStateManager.disableCull()

        val i = 274
        val j = width / 2 - 137
        val k = 30

        this.backgroundShader.useShader(this.width, this.height, mouseX.toFloat(), mouseY.toFloat(), (System.currentTimeMillis() - initTime) / 1000F)

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION drawBuffer {
            pos(-1.0, -1.0)
            pos(-1.0, 1.0)
            pos(1.0, 1.0)
            pos(1.0, -1.0)
        }

        // Unbind shader
        GL20.glUseProgram(0)
        // Stuff enabled done by the skybox rendering

        mc.textureManager.bindTexture(MINECRAFT_TITLE_TEXTURES)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        if (minecraftRoll < 1.0E-4) {
            this.drawTexturedModalRect(j + 0, 30, 0, 0, 99, 44)
            this.drawTexturedModalRect(j + 99, 30, 129, 0, 27, 44)
            this.drawTexturedModalRect(j + 99 + 26, 30, 126, 0, 3, 44)
            this.drawTexturedModalRect(j + 99 + 26 + 3, 30, 99, 0, 26, 44)
            this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44)
        } else {
            this.drawTexturedModalRect(j + 0, 30, 0, 0, 155, 44)
            this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44)
        }

        mc.textureManager.bindTexture(field_194400_H)
        drawModalRectWithCustomSizedTexture(j + 88, 67, 0.0f, 0.0f, 98, 14, 128.0f, 16.0f)

        splashText = ForgeHooksClient.renderMainMenu(this, fontRenderer, width, height, splashText)

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2 + 90).toFloat(), 70.0f, 0.0f)
        GlStateManager.rotate(-20.0f, 0.0f, 0.0f, 1.0f)
        var f = 1.8f - abs(MathHelper.sin((Minecraft.getSystemTime() % 1000L).toFloat() / 1000.0f * (Math.PI.toFloat() * 2f)) * 0.1f)
        f = f * 100.0f / (fontRenderer.getStringWidth(splashText) + 32).toFloat()
        GlStateManager.scale(f, f, f)
        drawCenteredString(fontRenderer, splashText, 0, -8, -256)
        GlStateManager.popMatrix()
        var s = "Minecraft 1.12.2"

        s = if (mc.isDemo) {
            "$s Demo"
        } else {
            s + if ("release".equals(mc.versionType, ignoreCase = true)) "" else "/" + mc.versionType
        }

        val brandings = Lists.reverse(FMLCommonHandler.instance().getBrandings(true))
        for (brdline in brandings.indices) {
            val brd = brandings[brdline]
            if (!Strings.isNullOrEmpty(brd)) {
                drawString(fontRenderer, brd, 2, height - (10 + brdline * (fontRenderer.FONT_HEIGHT + 1)), 16777215)
            }
        }

        drawString(fontRenderer, "Copyright Mojang AB. Do not distribute!", widthCopyrightRest, height - 10, -1)

        if (mouseX > widthCopyrightRest && mouseX < widthCopyrightRest + widthCopyright && mouseY > height - 10 && mouseY < height && Mouse.isInsideWindow()) {
            drawRect(widthCopyrightRest, height - 1, widthCopyrightRest + widthCopyright, height, -1)
        }

        if (openGLWarning1.isNotEmpty()) {
            drawRect(openGLWarningX1 - 2, openGLWarningY1 - 2, openGLWarningX2 + 2, openGLWarningY2 - 1, 1428160512)
            drawString(fontRenderer, openGLWarning1, openGLWarningX1, openGLWarningY1, -1)
            drawString(fontRenderer, openGLWarning2, (width - openGLWarning2Width) / 2, buttonList[0].y - 12, -1)
        }

        for (buttons in buttonList.indices) {
            (buttonList[buttons] as GuiButton).drawButton(mc, mouseX, mouseY, partialTicks)
        }
        for (labels in labelList.indices) {
            (labelList[labels] as GuiLabel).drawLabel(mc, mouseX, mouseY)
        }

        if (realmsNotification != null && areRealmsNotificationsEnabled()) {
            realmsNotification?.drawScreen(mouseX, mouseY, partialTicks)
        }

        modUpdateNotification?.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame(): Boolean {
        if (!MainMenuModule.custom.value) return super.doesGuiPauseGame()
        return false
    }

    override fun onGuiClosed() {
        if (!MainMenuModule.custom.value) {
            super.onGuiClosed()
            return
        }

        if (this.realmsNotification != null) {
            this.realmsNotification?.onGuiClosed()
        }
    }

    override fun actionPerformed(button: GuiButton) {
        if (!MainMenuModule.custom.value) {
            super.actionPerformed(button)
            return
        }

        if (button.id == 0) {
            mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
        }

        if (button.id == 5) {
            mc.displayGuiScreen(GuiLanguage(this, mc.gameSettings, mc.languageManager))
        }

        if (button.id == 1) {
            mc.displayGuiScreen(GuiWorldSelection(this))
        }

        if (button.id == 2) {
            mc.displayGuiScreen(GuiMultiplayer(this))
        }

        if (button.id == 14 && realmsButton!!.visible) {
            switchToRealms()
        }

        if (button.id == 4) {
            mc.shutdown()
        }

        if (button.id == 6) {
            mc.displayGuiScreen(GuiModList(this))
        }

        if (button.id == 11) {
            mc.launchIntegratedServer("Demo_World", "Demo_World", WorldServerDemo.DEMO_WORLD_SETTINGS)
        }

        if (button.id == 12) {
            val iSaveFormat = mc.saveLoader
            val worldInfo = iSaveFormat.getWorldInfo("Demo_World")
            if (worldInfo != null) {
                mc.displayGuiScreen(GuiYesNo(this, I18n.format("selectWorld.deleteQuestion"), "'" + worldInfo.worldName + "' " + I18n.format("selectWorld.deleteWarning"), I18n.format("selectWorld.deleteButton"), I18n.format("gui.cancel"), 12))
            }
        }

        if (button.id == 569) {
            mc.displayGuiScreen(GuiAltList(this))
        }

    }

    override fun initGui() {
        if (!MainMenuModule.custom.value) {
            super.initGui()
            return
        }

        this.widthCopyright = fontRenderer.getStringWidth("Copyright Mojang AB. Do not distribute!")
        this.widthCopyrightRest = width - this.widthCopyright - 2
        val calendar = Calendar.getInstance()
        calendar.time = Date()

        if (calendar[2] + 1 == 12 && calendar[5] == 24) {
            this.splashText = "Merry X-mas!"
        } else if (calendar[2] + 1 == 1 && calendar[5] == 1) {
            this.splashText = "Happy new year!"
        } else if (calendar[2] + 1 == 10 && calendar[5] == 31) {
            this.splashText = "OOoooOOOoooo! Spooky!"
        }

        val i = 24
        val j = height / 4 + 48

        if (mc.isDemo) {
            addDemoButtons(j, 24)
        } else {
            addSinglePlayerMultiplayerButtons(j, 24)
        }

        buttonList.add(GuiButton(0, width / 2 - 100, j + 72 + 12, 98, 20, I18n.format("menu.options")))
        buttonList.add(GuiButton(4, width / 2 + 2, j + 72 + 12, 98, 20, I18n.format("menu.quit")))
        buttonList.add(GuiButtonLanguage(5, width / 2 - 124, j + 72 + 12))

        lowestButtonY = 0
        for (guiButton in buttonList) {
            if (guiButton.y <= lowestButtonY) continue
            lowestButtonY = guiButton.y
        }
        buttonList.add(GuiButton(569, this.width / 2 + 104, this.lowestButtonY, 98, 20, "Alt Manager"))

        synchronized(threadLock) {
            this.openGLWarning1Width = fontRenderer.getStringWidth(this.openGLWarning1)
            this.openGLWarning2Width = fontRenderer.getStringWidth(this.openGLWarning2)
            val k = max(this.openGLWarning1Width, this.openGLWarning2Width)
            this.openGLWarningX1 = (width - k) / 2
            this.openGLWarningY1 = buttonList[0].y - 24
            this.openGLWarningX2 = this.openGLWarningX1 + k
            this.openGLWarningY2 = this.openGLWarningY1 + 24
        }

        mc.isConnectedToRealms = false

        if (Globals.mc.gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.hasCheckedForRealmsNotification) {
            val realmsBridge = RealmsBridge()
            this.realmsNotification = realmsBridge.getNotificationScreen(this)
            this.hasCheckedForRealmsNotification = true
        }

        if (realmsNotification != null && areRealmsNotificationsEnabled()) {
            this.realmsNotification?.setGuiSize(width, height)
            this.realmsNotification?.initGui()
        }

        modUpdateNotification = NotificationModUpdateScreen.init(this, modButton)

        initTime = System.currentTimeMillis()
    }

    override fun confirmClicked(result: Boolean, id: Int) {
        if (!MainMenuModule.custom.value) {
            super.confirmClicked(result, id)
            return
        }

        if (result && id == 12) {
            val iSaveFormat = mc.saveLoader
            iSaveFormat.flushCache()
            iSaveFormat.deleteWorldDirectory("Demo_World")
            mc.displayGuiScreen(this)
        } else if (id == 12) {
            mc.displayGuiScreen(this)
        } else if (id == 13) {
            if (result) {
                try {
                    val oClass = Class.forName("java.awt.Desktop")
                    val desktop = oClass.getMethod("getDesktop").invoke(null)
                    oClass.getMethod("browse", URI::class.java).invoke(desktop, URI(this.openGLWarningLink))
                } catch (throwable: Throwable) {
                    ClientLoader.LOGGER.error("Couldn't open link", throwable)
                }
            }
            mc.displayGuiScreen(this)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {

        if (!MainMenuModule.custom.value) {
            super.mouseClicked(mouseX, mouseY, mouseButton)
            return
        }

        synchronized(this.threadLock) {
            if (this.openGLWarning1.isNotEmpty() && !StringUtils.isNullOrEmpty(this.openGLWarningLink) && mouseX >= this.openGLWarningX1 && mouseX <= this.openGLWarningX2 && mouseY >= this.openGLWarningY1 && mouseY <= this.openGLWarningY2) {
                val guiConfirmOpenLink = GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true)
                guiConfirmOpenLink.disableSecurityWarning()
                mc.displayGuiScreen(guiConfirmOpenLink)
            }
        }

        if (mouseX > this.widthCopyrightRest && mouseX < this.widthCopyrightRest + this.widthCopyright && mouseY > height - 10 && mouseY < height) {
            mc.displayGuiScreen(GuiWinGame(false, Runnables.doNothing()))
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
    }

    private fun areRealmsNotificationsEnabled(): Boolean {
        return Globals.mc.gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && realmsNotification != null
    }

    private fun addDemoButtons(posY: Int, height: Int) {
        buttonList.add(GuiButton(11, width / 2 - 100, posY, I18n.format("menu.playdemo")))
        buttonResetDemo = addButton(GuiButton(12, width / 2 - 100, posY + height * 1, I18n.format("menu.resetdemo")))
        val iSaveFormat = mc.saveLoader
        val worldInfo = iSaveFormat.getWorldInfo("Demo_World")
        if (worldInfo == null) {
            buttonResetDemo?.enabled = false
        }
    }

    private fun addSinglePlayerMultiplayerButtons(posY: Int, height: Int) {
        buttonList.add(GuiButton(1, width / 2 - 100, posY, I18n.format("menu.singleplayer")))
        buttonList.add(GuiButton(2, width / 2 - 100, posY + height * 1, I18n.format("menu.multiplayer")))
        realmsButton = addButton(GuiButton(14, width / 2 + 2, posY + height * 2, 98, 20, I18n.format("menu.online").replace("Minecraft", "").trim { it <= ' ' }))
        buttonList.add(GuiButton(6, width / 2 - 100, posY + height * 2, 98, 20, I18n.format("fml.menu.mods")).also { modButton = it })
    }

    private fun switchToRealms() {
        val realmsBridge = RealmsBridge()
        realmsBridge.switchToRealms(this)
    }

//    private fun renderMainMenu(font: FontRenderer, width: Int, height: Int, splashText: String): String {
//        val status = ForgeVersion.getStatus()
//        if (status == ForgeVersion.Status.BETA || status == ForgeVersion.Status.BETA_OUTDATED) {
//            // render a warning at the top of the screen,
//            var line = I18n.format("forge.update.beta.1", TextFormatting.RED, TextFormatting.RESET)
//            drawString(font, line, (width - font.getStringWidth(line)) / 2, 4 + 0 * (font.FONT_HEIGHT + 1), -1)
//            line = I18n.format("forge.update.beta.2")
//            drawString(font, line, (width - font.getStringWidth(line)) / 2, 4 + 1 * (font.FONT_HEIGHT + 1), -1)
//        }
//        var line: String? = null
//        when (status) {
//            ForgeVersion.Status.OUTDATED, ForgeVersion.Status.BETA_OUTDATED -> line =
//                I18n.format("forge.update.newversion", ForgeVersion.getTarget())
//            else -> {
//            }
//        }
//        if (line != null) {
//            // if we have a line, render it in the bottom right, above Mojang's copyright line
//            drawString(font, line, width - font.getStringWidth(line) - 2, height - 2 * (font.FONT_HEIGHT + 1), -1)
//        }
//        return splashText
//    }

//    class NotificationModUpdateScreen(private val modButton: GuiButton) : GuiScreen() {
//        private var showNotification: ForgeVersion.Status? = null
//        private var hasCheckedForUpdates = false
//
//        override fun initGui() {
//            if (!hasCheckedForUpdates) {
//                for (mod in Loader.instance().modList) {
//                    val status = ForgeVersion.getResult(mod).status
//                    if (status == ForgeVersion.Status.OUTDATED || status == ForgeVersion.Status.BETA_OUTDATED) {
//                        showNotification = ForgeVersion.Status.OUTDATED
//                    }
//                }
//                hasCheckedForUpdates = true
//            }
//        }
//
//        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
//            if (showNotification == null || !showNotification!!.shouldDraw() || ForgeModContainer.disableVersionCheck) {
//                return
//            }
//            Minecraft.getMinecraft().textureManager.bindTexture(VERSION_CHECK_ICONS)
//            GlStateManager.color(1f, 1f, 1f, 1f)
//            GlStateManager.pushMatrix()
//            val x = modButton.x
//            val y = modButton.y
//            val w = modButton.width
//            val h = modButton.height
//            drawModalRectWithCustomSizedTexture(
//                x + w - (h / 2 + 4),
//                y + (h / 2 - 4),
//                (showNotification!!.sheetOffset * 8).toFloat(),
//                if (showNotification!!.isAnimated && System.currentTimeMillis() / 800 and 1 == 1L) 8F else 0F,
//                8,
//                8,
//                64f,
//                16f
//            )
//            GlStateManager.popMatrix()
//        }
//
//        companion object {
//            private val VERSION_CHECK_ICONS = ResourceLocation(ForgeVersion.MOD_ID, "textures/gui/version_check_icons.png")
//
//            fun init(guiMainMenu: GuiMainMenu, modButton: GuiButton): NotificationModUpdateScreen {
//                val notificationModUpdateScreen = NotificationModUpdateScreen(modButton)
//                notificationModUpdateScreen.setGuiSize(guiMainMenu.width, guiMainMenu.height)
//                notificationModUpdateScreen.initGui()
//                return notificationModUpdateScreen
//            }
//        }
//    }

}
package me.han.muffin.client.module.modules.render

import com.google.common.collect.Ordering
import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.gui.TabOrderSortedCopyEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.TextureManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.FontsModule
import me.han.muffin.client.utils.color.ChatFormattingToggleable
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.encryption.AESUtils
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.scoreboard.ScorePlayerTeam
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import kotlin.concurrent.thread

/**
 * @author han
 * @see me.han.muffin.client.mixin.mixins.gui.MixinGuiPlayerTabOverlay
 */
internal object BetterTabModule: Module("BetterTab", Category.RENDER, true, "Allow you to customize and optimize the tab.") {
    private val maxSlots = NumberValue(250, 50, 400, 20, "MaxSlots")
    private val maxRows = NumberValue(30, 0, 100, 2, "MaxRows")

    private val customFont = Value(true, "CustomFont")
    private val poolList = Value(true, "PoolList")
    private val sortingMode = EnumValue(SortingMode.Vanilla, "SortingMode")
    private val pingMode = EnumValue(PingMode.Vanilla, "PingMode")
    private val test = Value(false, "Test")

    private val icon = Value(true, "Icon")
    private val friendHighlights = EnumValue(ChatFormattingToggleable.AQUA, "FriendHighlights")
    private val selfHighlights = EnumValue(ChatFormattingToggleable.DARK_AQUA, "SelfHighlights")
    private val enemiesHighlights = EnumValue(ChatFormattingToggleable.WHITE, "EnemiesHighlights")

    private val customBackground = Value(false, "CustomBackground")
    private val backgroundRainbow = Value({ customBackground.value },false, "BackgroundRainbow")
    private val backgroundRed = NumberValue({ customBackground.value && !backgroundRainbow.value },0 , 0, 255, 1, "BackgroundRed")
    private val backgroundGreen = NumberValue({ customBackground.value && !backgroundRainbow.value },0, 0, 255, 1, "BackgroundGreen")
    private val backgroundBlue = NumberValue({ customBackground.value && !backgroundRainbow.value },0, 0, 255, 1, "BackgroundBlue")
    private val backgroundAlpha = NumberValue({ customBackground.value && !backgroundRainbow.value },25, 0, 255, 1, "BackgroundAlpha")

    private val infoColour = Value(false, "InfoColour")

    private val infoAll = Value({ infoColour.value },false, "InfoAll")
    private val infoMuffin = Value({ infoColour.value && !infoAll.value },true, "InfoMuffin")
    private val infoFriend = Value({ infoColour.value && !infoAll.value },false, "InfoFriend")

    private val infoRainbow = Value({ infoColour.value },false, "InfoRainbow")
    private val infoRed = NumberValue({ infoColour.value && !infoRainbow.value },255, 0, 255, 1, "InfoRed")
    private val infoGreen = NumberValue({ infoColour.value && !infoRainbow.value },255, 0, 255, 1, "InfoGreen")
    private val infoBlue = NumberValue({ infoColour.value && !infoRainbow.value },253, 0, 255, 1, "InfoBlue")
    private val infoAlpha = NumberValue({ infoColour.value && !infoRainbow.value },32, 0, 255, 1, "InfoAlpha")

    private var hadDone = false
    private val muffinUsers = hashSetOf<String>().synchronized()

    private val LENGTH_ORDERING = Ordering.from(compareBy<NetworkPlayerInfo> { it.gameProfile.name.length })
    private val ALPHABET_ORDERING = Ordering.from(compareBy<NetworkPlayerInfo> { it.gameProfile.name })
    private val PING_ORDERING = Ordering.from(compareBy<NetworkPlayerInfo> { it.responseTime })
    private val FRIEND_ORDERING = Ordering.from(compareByDescending<NetworkPlayerInfo> { FriendManager.isFriend(it.gameProfile.name) })
    private val MUFFIN_ORDERING = Ordering.from(compareByDescending<NetworkPlayerInfo> { checkIsMuffinUser(it.gameProfile.id.toString()) })

    private val customBackgroundColour get() = Colour(backgroundRed.value, backgroundGreen.value, backgroundBlue.value, backgroundAlpha.value)
    private val customInfoColour get() = Colour(infoRed.value, infoGreen.value, infoBlue.value, infoAlpha.value)

    @JvmStatic val isPoolListOn get() = isEnabled && poolList.value
    @JvmStatic val isSegoeFont get() = FontsModule.font.value == FontsModule.FontType.Segoe

    var infoXAddon = 9

    enum class SortingMode {
        None, Vanilla, Alphabet, Length, Ping, Friend, Muffin
    }

    enum class PingMode {
        Vanilla, Text
    }

    init {
        addSettings(
            maxSlots, maxRows,
            customFont, poolList,
            sortingMode, pingMode, test,
            icon,
            friendHighlights, selfHighlights, enemiesHighlights,
            customBackground, backgroundRainbow, backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha,
            infoColour, infoAll, infoMuffin, infoFriend,
            infoRainbow, infoRed, infoGreen, infoBlue, infoAlpha
        )
    }

    override fun onEnable() {
        if (!hadDone) doDecrypt()
    }

    private fun doDecrypt() {

    }

//    private fun getIcon(name: String?): MipmapTexture? {
//        if (name.isNullOrEmpty()) return null
//        if (!iconMap.containsKey(name)) loadIcon()
//    }
//
//    private fun loadIcon(name: String) {
//        try {
//            val image = ImageIO.read(Muffin.ICON_STREAM)
//            val texture = MipmapTexture(image, GL_RGBA, 3)
//
//            texture.bindTexture()
//            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
//            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
//            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
//            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
//            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f)
//            texture.unbindTexture()
//
//            iconMap[name] = texture
//        } catch (e: IOException) {
//            ClientLoader.LOGGER.warn("Failed to load icon", e)
//        }
//    }

    fun checkIsMuffinUser(uuid: String): Boolean {
        return muffinUsers.contains(uuid)
    }

    @Listener
    private fun onTabOrderSortedCopyEvent(event: TabOrderSortedCopyEvent) {
        val customOrdering = when (sortingMode.value) {
            SortingMode.Ping -> PING_ORDERING
            SortingMode.Alphabet -> ALPHABET_ORDERING
            SortingMode.Length -> LENGTH_ORDERING
            SortingMode.Friend -> FRIEND_ORDERING
            SortingMode.Muffin -> MUFFIN_ORDERING
            SortingMode.Vanilla -> event.ordering
            else -> null
        }

        event.customOrdering = customOrdering?.sortedCopy(event.elements) ?:
        if (sortingMode.value == SortingMode.None) StreamSupport.stream(event.elements.spliterator(), false).collect(Collectors.toList()) else null

    }

    @JvmStatic
    fun subList(list: List<NetworkPlayerInfo>, newList: List<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        if (isDisabled) return newList
        if (test.value) return list
        return list.subList(0, maxSlots.value.coerceAtMost(list.size))
    }

    @JvmStatic
    fun sortingMode(oldOrdering: Ordering<NetworkPlayerInfo>, elements: Iterable<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        if (isDisabled || sortingMode.value == SortingMode.Vanilla) return oldOrdering.sortedCopy(elements)

        return when (sortingMode.value) {
            SortingMode.Ping -> PING_ORDERING.sortedCopy(elements)
            SortingMode.Length -> LENGTH_ORDERING.sortedCopy(elements)
            else -> StreamSupport.stream(elements.spliterator(), false).collect(Collectors.toList())
        }
    }

    @JvmStatic
    fun renderCustomPing(x: Float, y: Float, width: Float, playerInfo: NetworkPlayerInfo): Boolean {
        if (isDisabled || pingMode.value == PingMode.Vanilla) return false

        val ping = playerInfo.responseTime
        val stringWidth = Muffin.getInstance().fontManager.tabCustomFont?.getStringWidth(ping.toString()) ?: Globals.mc.fontRenderer.getStringWidth(ping.toString())

        infoXAddon = 9 + stringWidth

        val pingText = getResponsePing(ping)
        val posX = x + width - 1 - stringWidth
        val colour = -1

        renderMuffinIcon(posX - 12, y - 2, playerInfo, vanilla = false)

        Muffin.getInstance().fontManager.tabCustomFont?.drawStringWithShadow(pingText, posX, y + if (isSegoeFont) 0.85F else 1.5F, colour) ?:
        Globals.mc.fontRenderer.drawStringWithShadow(pingText, posX, y, colour)

        return true
    }

    @JvmStatic
    fun renderMuffinIcon(x: Float, y: Float, playerInfo: NetworkPlayerInfo, vanilla: Boolean = false) {
        if (isDisabled || !icon.value || !checkIsMuffinUser(playerInfo.gameProfile.id.toString())) return

        GlStateUtils.resetColour()
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        // ChatManager.sendMessage("drawing")
        TextureManager.drawMipmapIcon512(x.toDouble(), y.toDouble(), 12F)

        GlStateUtils.blend(false)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        if (vanilla) Globals.mc.textureManager.bindTexture(Gui.ICONS)
    }

    @JvmStatic
    fun getInfoBackgroundColourRedirect(oldValue: Int): Int {
        if (isDisabled || !infoColour.value || !infoAll.value) return oldValue
        if (infoAlpha.value == 0) return -1

        return customInfoColour.toHex() or 0x80000000.toInt()
    }

    @JvmStatic
    fun getInfoBackgroundColourCover(tabList: List<NetworkPlayerInfo>, index: Int): Int {
        if (isDisabled || !infoColour.value) return -1
        if (infoAll.value) return -5

        var colour = 553648126

        if (index < tabList.size) {
            val gameProfile = tabList[index].gameProfile

            val name = gameProfile.name
            val uuid = gameProfile.id.toString()

            if (infoMuffin.value && checkIsMuffinUser(uuid) || infoFriend.value && FriendManager.isFriend(name))
                colour = customInfoColour.toHex() or 0x80000000.toInt()
        }

        return colour
    }

    @JvmStatic
    fun getColourPlayerName(playerInfo: NetworkPlayerInfo): String? {
        if (isDisabled || selfHighlights.value == ChatFormattingToggleable.NONE && friendHighlights.value == ChatFormattingToggleable.NONE) return null

        val gameProfile = playerInfo.gameProfile
        val name = playerInfo.displayName?.formattedText ?: ScorePlayerTeam.formatPlayerName(playerInfo.playerTeam, playerInfo.gameProfile.name) ?: gameProfile.name

        val doesFriendHighlightOn = friendHighlights.value != ChatFormattingToggleable.NONE
        val doesSelfHighlightOn = selfHighlights.value != ChatFormattingToggleable.NONE
        val doesEnemiesHighlightOn = enemiesHighlights.value != ChatFormattingToggleable.NONE

        val isLocalName = name.equals(Globals.mc.player.name)

        return (if (doesFriendHighlightOn && !isLocalName && FriendManager.isFriend(name)) friendHighlights.value.mcFormat
        else if (doesSelfHighlightOn && isLocalName) selfHighlights.value.mcFormat
        else if (doesEnemiesHighlightOn) enemiesHighlights.value.mcFormat
        else ChatFormatting.WHITE).toString() + name
    }

    @JvmStatic
    fun renderCustomBackground(x: Float, y: Float, width: Float, height: Float, colour: Int): Boolean {
        if (isDisabled || !customBackground.value) return false

        if (backgroundAlpha.value > 0) RenderUtils.drawRect(x, y, width, height, customBackgroundColour.toHex())

        return true
    }

    @JvmStatic
    fun getInfoXAddon(oldX: Int): Int {
        return oldX + if (isDisabled || pingMode.value == PingMode.Vanilla) 9 else infoXAddon
    }

    @JvmStatic
    fun getMaxRows(oldRow: Int): Int {
        return if (isDisabled || maxRows.value == 0) oldRow else maxRows.value
    }

    @JvmStatic
    fun getCustomFontRenderer(mcFontRenderer: FontRenderer): Pair<FontRenderer, Boolean> {
        return if (isDisabled || !customFont.value) mcFontRenderer to false else Muffin.getInstance().fontManager.tabCustomFont to true
    }

    @JvmStatic
    fun getCustomFontRendererInstead(mcFontRenderer: FontRenderer): FontRenderer {
        return if (isDisabled || !customFont.value) mcFontRenderer else Muffin.getInstance().fontManager.tabCustomFont ?: mcFontRenderer
    }

    private fun getResponsePing(currentPing: Int): String {
        if (currentPing < 0) return ChatFormatting.BLUE.toString() + currentPing

        val colour = when (currentPing) {
            in 0..150 -> ChatFormatting.GREEN
            in 151..300 -> ChatFormatting.YELLOW
            in 301..600 -> ChatFormatting.GOLD
            in 601..1000 -> ChatFormatting.RED
            else -> ChatFormatting.DARK_RED
        }

        return colour.toString() + currentPing
    }

}
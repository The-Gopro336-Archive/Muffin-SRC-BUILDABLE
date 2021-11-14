package me.han.muffin.client.manager

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.altmanager.Account
import me.han.muffin.client.mixin.ClientLoader
import me.han.muffin.client.utils.render.RenderUtils.drawModalRectWithCustomSizedTexture
import me.han.muffin.client.utils.timer.Timer
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.ImageBufferDownload
import net.minecraft.client.renderer.ThreadDownloadImageData
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.StringUtils
import net.minecraft.util.text.TextFormatting
import org.apache.commons.io.IOUtils
import java.lang.reflect.Type
import java.net.MalformedURLException
import java.net.URL

object AccountManager {

    @JvmField var altList = ArrayList<Account>()
    const val slotHeight = 35

    private val checkTimer = Timer()
    private var authenticatorStatus = HashMap<String, String>()

    private val altAuthServer: URL? = try {
        URL("https://status.mojang.com/check")
    } catch (e: MalformedURLException) {
        ClientLoader.LOGGER.info("Malformed URL found trying while to format Mojang server status checker URL string.")
        null
    }

    private val type = TokenTypeStore().type
    private val gsonFormat = GsonBuilder().registerTypeAdapter(type, Deserializer()).create()

    fun checkStatus() {
        if (checkTimer.passedSeconds(30)) {
            try {
                authenticatorStatus = gsonFormat.fromJson(IOUtils.toString(altAuthServer, "UTF-8"), type)
            } catch (e: Exception) {
                authenticatorStatus["authserver.mojang.com"] = "black"
                authenticatorStatus["session.minecraft.net"] = "black"
                e.printStackTrace()
            }
            checkTimer.reset()
        }
    }

    fun drawString() {
        val authServerStatus =
            if (authenticatorStatus == null) TextFormatting.GRAY.toString() + "Loading..." else
            authenticatorStatus["authserver.mojang.com"]
            ?.replace("green", TextFormatting.GREEN.toString() + "Online")
            ?.replace("yellow", TextFormatting.GOLD.toString() + "Slow")
            ?.replace("red", TextFormatting.DARK_RED.toString() + "Offline")
            ?.replace("black", TextFormatting.DARK_GRAY.toString() + "DNS Failure")
            ?: TextFormatting.GRAY.toString() + "Loading..."

        val sessionServer =
            if (authenticatorStatus == null) TextFormatting.GRAY.toString() + "Loading..." else
                authenticatorStatus["session.minecraft.net"]
                    ?.replace("green", TextFormatting.GREEN.toString() + "Online")
                    ?.replace("yellow", TextFormatting.GOLD.toString() + "Slow")
                    ?.replace("red", TextFormatting.DARK_RED.toString() + "Offline")
                    ?.replace("black", TextFormatting.DARK_GRAY.toString() + "DNS Failure")
                    ?: TextFormatting.GRAY.toString() + "Loading..."

        val finalAuth = StringBuilder().insert(0, TextFormatting.GRAY.toString() + "Authentication Server: ").append(authServerStatus).toString()
        val finalSession = StringBuilder().insert(0, TextFormatting.GRAY.toString() + "Multiplayer Session: ").append(sessionServer).toString()

        Globals.mc.fontRenderer.drawStringWithShadow(finalAuth, 2.0f, 2f, -1)
        Globals.mc.fontRenderer.drawStringWithShadow(finalSession, 2.0f, (2 + Globals.mc.fontRenderer.FONT_HEIGHT).toFloat(), -1)
    }

    private fun downloadImages(rs: ResourceLocation, s: String): ThreadDownloadImageData {
        val textureManager = Globals.mc.textureManager
        var texture = textureManager.getTexture(rs)

        if (texture == null) {
            texture = ThreadDownloadImageData(
                null,
                "https://skins.futureclient.net/MinecraftSkins/%s.png?bypass_secret=SHKzhL8TaNeE5cyJ".format(StringUtils.stripControlCodes(s)),
                DefaultPlayerSkin.getDefaultSkin(EntityPlayer.getOfflineUUID(s)), ImageBufferDownload()
            )
            textureManager.loadTexture(rs, texture)
        }

        return texture as ThreadDownloadImageData
    }

    fun drawImage(name: String, x: Int, y: Int) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        val locationSkin = AbstractClientPlayer.getLocationSkin(name)
        downloadImages(locationSkin, name)
        Globals.mc.textureManager.bindTexture(locationSkin)
        GlStateManager.enableBlend()
        drawModalRectWithCustomSizedTexture(x, y, 32.0f, 32.0f, 32, 32, 256.0f, 256.0f)
        drawModalRectWithCustomSizedTexture(x, y, 160.0f, 32.0f, 32, 32, 256.0f, 256.0f)
        GlStateManager.disableBlend()
    }

    fun makePassChar(regex: String): String {
        return regex.replace("(?s).".toRegex(), "*")
    }

    class TokenTypeStore: TypeToken<Map<String, String>>()

    class Deserializer: JsonDeserializer<Map<String, String>> {

        @Throws(JsonParseException::class)
        override fun deserialize(element: JsonElement, type: Type, jsonContext: JsonDeserializationContext): Map<String, String> {
            return hashMapOf<String, String>().apply {
                for (json in element.asJsonArray) {
                    if (json == null) continue
                    for (elements in json.asJsonObject.entrySet()) {
                        if (elements == null) continue
                        this[elements.key] = elements.value.asString
                    }
                }
            }
        }

    }

}
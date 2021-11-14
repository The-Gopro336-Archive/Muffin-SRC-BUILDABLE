package me.han.muffin.client.module.modules.other

import me.han.muffin.client.Muffin
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.gui.font.MinecraftFontRenderer
import me.han.muffin.client.manager.managers.FontManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.AsyncCachedValue
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.GraphicsEnvironment
import java.util.*

object FontsModule: Module("Fonts", Category.OTHERS, true, true, "Control client fonts.") {
    //var fontType = EnumValue(FontState.Client, "FontType")
    //var clientFont = EnumValue({ fontType.value == FontState.Client }, FontType.ProductSans, "ClientFont")

    val font = EnumValue(FontType.ProductSans, "ClientFont")
    private val fontStyle = EnumValue(MinecraftFontRenderer.FontStyle.Plain, "FontStyle")
    private val fontSize = NumberValue(14, 5, 20, 1, "GuiFontSize")
    private var fontManager: FontManager? = null

    private val DEFAULT_FONT_NAME = FontType.ProductSans.name

    private var lastFontStyle = MinecraftFontRenderer.FontStyle.Plain
    private var lastFontSize = 18

    val sizeFont: Int get() = fontSize.value
    val styleFont: MinecraftFontRenderer.FontStyle get() = fontStyle.value

    init {
        addSettings(font, fontStyle, fontSize)

        font.listeners = object: ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                fontManager?.systemFont = null
                fontManager?.setFont(font.value, fontSize.value)
                fontManager?.setFontStyle(fontStyle.value)
            }
        }

        fontSize.listeners = object: ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                if (fontSize.value != lastFontSize) {
                    lastFontSize = fontSize.value
                    fontManager?.setFont(font.value, fontSize.value)
                }
            }
        }

        fontStyle.listeners = object: ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                if (fontStyle.value != lastFontStyle) {
                    lastFontStyle = fontStyle.value
                    fontManager?.setFontStyle(fontStyle.value)
                }
            }
        }

    }

    enum class FontType {
        Default, Roboto, ProductSans, Segoe
    }

    val availableFonts: Map<String, String> by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        HashMap<String, String>().apply {
            val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            environment.availableFontFamilyNames.forEach {
                this[it.toLowerCase(Locale.ROOT)] = it
            }
            environment.allFonts.forEach {
                this[it.name.toLowerCase(Locale.ROOT)] = it.family
            }
        }
    }

    override fun onEnable() {
        if (fontManager == null) fontManager = Muffin.getInstance().fontManager
        val fontManager = fontManager ?: return

        fontManager.setFont(font.value, fontSize.value)
        fontManager.setFontStyle(fontStyle.value)
    }


    @Listener
    private fun onTicking(event: TickEvent) {
        if (fontManager == null) fontManager = Muffin.getInstance().fontManager
        val fontManager = fontManager ?: return
        fontManager.setColour()
    }

    fun getMatchingFontName(name: String): String? {
        val spaceReplace = name.replace("_", " ")
        return if (spaceReplace.equals(DEFAULT_FONT_NAME, true)) DEFAULT_FONT_NAME
        else availableFonts[spaceReplace.toLowerCase(Locale.ROOT)]
    }

}
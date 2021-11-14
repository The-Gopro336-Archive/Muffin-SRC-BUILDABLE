package me.han.muffin.client.command.commands

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.module.modules.other.FontsModule

object FontsCommand: Command(arrayOf("font", "fonts", "customfont"), Argument("fontname")) {

    override fun dispatch(): String {
        val fontName = getArgument("fontname")?.value?.toLowerCase() ?: return "No such arguments."
        val fontManager = Muffin.getInstance().fontManager ?: return "WTF???"

        var fontCounter = 0
        val builder = StringBuilder()
        val cacheSet = hashSetOf<String>()

        when (fontName) {
            "fonts" -> {
                FontsModule.availableFonts.values.forEach { cacheSet.add(it) }
                var lastColour = ChatManager.textColour

                builder.append("${ChatFormatting.GRAY}Available fonts ${ChatFormatting.GRAY}(${ChatFormatting.GREEN}${cacheSet.size}${ChatFormatting.GRAY})${ChatManager.textColour}: ")

                cacheSet.sortedBy { it }.distinct().forEach {

                    val currentColour = ChatFormatting.values().filter { colour ->
                        val colourString = colour.toString()

                                colourString != lastColour &&
                                colourString != ChatManager.darkTextColour &&
                                colourString != ChatFormatting.OBFUSCATED.toString() &&
                                colourString != ChatFormatting.BOLD.toString() &&
                                colourString != ChatFormatting.STRIKETHROUGH.toString() &&
                                colourString != ChatFormatting.UNDERLINE.toString() &&
                                colourString != ChatFormatting.ITALIC.toString() &&
                                colourString != ChatFormatting.RESET.toString()
                    }.random().toString()

                    fontCounter++

                    builder.append(currentColour + it)
                    lastColour = currentColour

                    if (fontCounter < cacheSet.size) {
                        builder.append("${ChatManager.darkTextColour}, ")
                    } else {
                        builder.append(".")
                    }
                }

                return builder.toString()
            }
            else -> {
                val font = FontsModule.getMatchingFontName(fontName) ?: return "You can use _ to replace spaces / Current font not available."
                fontManager.systemFont = font
                fontManager.setSystemFont(FontsModule.sizeFont)
                fontManager.setFontStyle(FontsModule.styleFont)

                return "Successfully changed the font."
            }
        }

    }

}

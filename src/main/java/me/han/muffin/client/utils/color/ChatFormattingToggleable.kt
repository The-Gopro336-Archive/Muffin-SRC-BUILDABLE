package me.han.muffin.client.utils.color

import com.mojang.realmsclient.gui.ChatFormatting

enum class ChatFormattingToggleable(val mcFormat: ChatFormatting, val colourCode: Char) {
    NONE(ChatFormatting.RESET, 'r'),
    BLACK(ChatFormatting.BLACK,'0'),
    DARK_BLUE(ChatFormatting.DARK_BLUE,'1'),
    DARK_GREEN(ChatFormatting.DARK_GREEN,'2'),
    DARK_AQUA(ChatFormatting.DARK_AQUA,'3'),
    DARK_RED(ChatFormatting.DARK_RED,'4'),
    DARK_PURPLE(ChatFormatting.DARK_PURPLE,'5'),
    GOLD(ChatFormatting.GOLD,'6'),
    GRAY(ChatFormatting.GRAY,'7'),
    DARK_GRAY(ChatFormatting.DARK_GRAY,'8'),
    BLUE(ChatFormatting.BLUE,'9'),
    GREEN(ChatFormatting.GREEN,'a'),
    AQUA(ChatFormatting.AQUA,'b'),
    RED(ChatFormatting.RED,'c'),
    LIGHT_PURPLE(ChatFormatting.LIGHT_PURPLE,'d'),
    YELLOW(ChatFormatting.YELLOW,'e'),
    WHITE(ChatFormatting.WHITE,'f')
}
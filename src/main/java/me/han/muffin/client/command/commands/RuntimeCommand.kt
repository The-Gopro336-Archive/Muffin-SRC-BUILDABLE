package me.han.muffin.client.command.commands

import me.han.muffin.client.Muffin
import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager

object RuntimeCommand: Command(arrayOf("runtime", "time"), Argument("format")) {

    override fun dispatch(): String {
        val formatArgument = getArgument("format")?.value?.toLowerCase() ?: return "Invalid format argument."

        val second = (System.nanoTime() / 1000000L - Muffin.getInstance().startTime) / 1000L
        val minute = second / 60L
        val hour = minute / 60L
        val day = hour / 24L
        val week = day / 7L

        val runtime = when (formatArgument) {
            "second" -> "%s seconds".format(second)
            "minute" -> "%s minutes".format(minute)
            "hour" -> "%s hours".format(hour)
            "day" -> "%s days".format(day)
            "week" -> "%s weeks".format(week)
            else -> "Invalid time format, use second, minute, hour, day, week."
        }

        return "You've been playing for ${ChatManager.textColour}$runtime."
    }

}
package me.han.muffin.client.command.commands

import me.han.muffin.client.command.Argument
import me.han.muffin.client.command.Command
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.utils.network.WebUtils

object QueueLookupCommand: Command(arrayOf("queue"), Argument("normal/prio")) {
    private const val NORMALQ_LINK = "https://2b2t.io/api/queue?last=true"
    private const val PRIOQ_LINK = "https://api.2b2t.dev/prioq"

    override fun dispatch(): String {
        val modeArgument = getArgument("normal/prio")?.value ?: return "Invalid mode arguement."

        if (modeArgument.startsWith("n")) {
            Thread {
                val normalQueue = getNormalQCount()
                val placeholder = if (normalQueue == -1) "Normal queue currently not available." else "There are $normalQueue player are queueing normal queue."
                ChatManager.sendMessage(placeholder)
            }.start()
            return "Searching normal queue..."
        } else if (modeArgument.startsWith("p")) {
            Thread {
                val prioQueue = getPrioQCount()
                val queue = prioQueue.first
                val waitTime = prioQueue.second
                val placeholder = if (queue == -1 || waitTime == -1) "Priority queue currently not available." else "You had to wait $queue players and $waitTime minutes with priority queue."
                ChatManager.sendMessage(placeholder)
            }.start()
            return "Searching priority queue..."
        }

        return "Not supported."
    }

    private fun getNormalQCount(): Int {
        val content = WebUtils.getUrlContents(NORMALQ_LINK)
            .replace("[[", "")
            .replace("]]", "")
            .trim()
            .split(",")

        val queue = content[1].replace("\"", "")
        return if (queue == "null") -1 else queue.toInt()
    }

    private fun getPrioQCount(): Pair<Int, Int> {
        val content = WebUtils.getUrlContents(PRIOQ_LINK)
            .replace("[", "")
            .replace("]", "")
            .trim()
            .split(",")

        val queue = content[1].replace("\"", "")
        val waitTime = content[2].replace("\"", "").replace("m", "")

        return if (queue == "null" || waitTime == "null") -1 to -1 else queue.toInt() to waitTime.toInt()
    }

}
package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.entity.living.EntityUseItemFinishEvent
import me.han.muffin.client.event.events.gui.GuiScreenEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.PlayerConnectEvent
import me.han.muffin.client.manager.managers.BBTTQueueManager
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.client.ChatUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.math.RandomUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.Value
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUpdateSign
import net.minecraft.util.math.BlockPos
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

internal object AnnouncerModule: Module("Announcer", Category.MISC, "Speak to global what are you doing.") {
    private val join = Value(true, "Join")
    private val leave = Value(true, "Leave")
    private val place = Value(true, "Place")
    private val breaking = Value(true, "Break")
    private val food = Value(true, "Food")
    private val blocks = Value(false, "Blocks")
    private val items = Value(false, "Items")
    private val worldTime = Value(true, "WorldTime")
    val queueLogging = Value(true, "QueueLogging")
    private val clientName = Value(false, "ClientName")
    private val clientSideOnly = Value(true, "ClientSideOnly")

    init {
        addSettings(join, leave, place, breaking, food, items, blocks, worldTime, queueLogging, clientName, clientSideOnly)
    }

    private val leaveMsgs = arrayOf("See you later, ", "Catch ya later, ", "See you next time, ", "Farewell, ", "Bye, ", "Good bye, ", "Later, ")
    private val joinMsgs = arrayOf("Good to see you, ", "Greetings, ", "Hello, ", "Howdy, ", "Hey, ", "Good evening, ", "Welcome to SERVERIP1D5A9E, ")

    private val morningMsgs = arrayOf("Good morning!", "Top of the morning to you!", "Good day!", "You survived another night!", "Good morning everyone!", "The sun is rising in the east, hurrah, hurrah!")
    private val noonMsgs = arrayOf("Let's go tanning!", "Let's go to the beach!", "Grab your sunglasses!", "Enjoy the sun outside! It is currently very bright!", "It's the brightest time of the day!")
    private val afternoonMsgs = arrayOf("Good afternoon!", "Let's grab lunch!", "Lunch time, kids!", "Good afternoon everyone!", "IT'S HIGH NOON!")
    private val dinnerMsgs = arrayOf("Happy hour!", "Let's get crunk!", "Enjoy the sunset everyone!")
    private val nightMsgs = arrayOf("Let's get comfy!", "Netflix and chill!", "You survived another day!", "Time to go to bed kids!")
    private val sunsetMsgs = arrayOf("Sunset has now ended! You may eat your lunch now if you are a muslim.")
    private val midNight = arrayOf("It's so dark outside...", "It's the opposite of noon!")
    private val daylightMsgs = arrayOf("Good bye, zombies!", "Monsters are now burning!", "Burn baby, burn!")

    // for breaking blocks
    private var lastBlockBroken: String? = null
    private var blocksBroken = 0
    private var blocksBrokenAtPos: ArrayList<BlockPos>? = ArrayList()

    // for placing
    private var lastBlockPlaced: String? = null
    private var blocksPlaced = 0

    private val messageQueue = ConcurrentLinkedQueue<String>()

    private val minedBlocks = HashMap<String, Int>()
    private val placedBlocks = HashMap<String, Int>()
    private val droppedItems = HashMap<String, Int>()
    private val consumedItems = HashMap<String, Int>()

    private val lastEventReceive: PacketEvent.Receive? = null
    private var lastEventSend: PacketEvent.Send? = null
    private val lastLivingEntityUseFinishEvent: LivingEntityUseItemEvent.Finish? = null
    private val lastGuiScreenDisplayedEvent: GuiScreenEvent.Displayed? = null

    private val lastMsg: String? = null
    private val stringsToChose = ArrayList<String>()
    private var timer = Timer()
    private var oldTimer = java.util.Timer()
    private var timerTask: TimerTask? = null
    private var df = DecimalFormat()

    override fun onEnable() {
        if (fullNullCheck()) return
        oldTimer = java.util.Timer()

        df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.CEILING

        timerTask = object : TimerTask() {
            override fun run() {
                sendMessageCycle()
            }
        }
        oldTimer.schedule(timerTask, 0, 3)
        stringsToChose.clear()

        if (queueLogging.value) BBTTQueueManager.init()
    }

    override fun onDisable() {
        oldTimer.cancel()
        oldTimer.purge()
        messageQueue.clear()

        if (queueLogging.value) BBTTQueueManager.close()
    }

    private fun sendToChat(msg: String) {
        if (clientSideOnly.value) ChatUtils.sendMessage(msg, ChatIDs.ANNOUNCER) else Globals.mc.player.connection.sendPacket(CPacketChatMessage(msg.replace(ChatUtils.SECTIONSIGN, "")));
    }

    private fun composeEventData() {
        val suffix = if (clientName.value) ", thanks to " + Muffin.MODNAME + "!" else "!"
        for (kv in minedBlocks.entries) {
            queueMessage("I mined " + kv.value.toString() + " " + kv.key + suffix)
            minedBlocks.remove(kv.key)
        }
        for (kv in placedBlocks.entries) {
            queueMessage("I placed " + kv.value.toString() + " " + kv.key + suffix)
            placedBlocks.remove(kv.key)
        }
        for (kv in droppedItems.entries) {
            queueMessage("I dropped " + kv.value.toString() + " " + kv.key + suffix)
            droppedItems.remove(kv.key)
        }
        for (kv in consumedItems.entries) {
            queueMessage("I ate " + kv.value.toString() + " " + kv.key + suffix)
            consumedItems.remove(kv.key)
        }
    }

    private fun sendMessageCycle() {
        composeEventData()
        for (message in messageQueue) {
            sendToChat(message)
            messageQueue.remove(message)
            return
        }
    }


    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (!timer.passed(10000.0)) return

        if (lastBlockBroken != null && breaking.value) {
            stringsToChose.add(StringBuilder("I just mined ").append(blocksBroken.toString()).append(" ").append(lastBlockBroken).append("!").toString())
            blocksBroken = 0
            lastBlockBroken = null
            blocksBrokenAtPos = null
        }

        if (lastBlockPlaced != null && place.value) {
            stringsToChose.add(StringBuilder("I just placed ").append(blocksPlaced.toString()).append(" ").append(lastBlockPlaced).append("!").toString())
            lastBlockPlaced = null
            blocksPlaced = 0
        }

        if (stringsToChose.isNotEmpty()) {
            val index = RandomUtils.random.nextInt(stringsToChose.size) // java is dumb and doesn't let me not declare it as an int?
            val rand = stringsToChose[index]
            stringsToChose.remove(rand)
            timer.reset()
            sendToChat(rand)
        }

    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (lastEventSend != null && lastEventSend == event) {
            return
        }

        // Mining and Item Drop
        if ((items.value || blocks.value) && event.packet is CPacketPlayerDigging) {
            if (items.value) {
                if (!(Globals.mc.player.heldItemMainhand.item === Items.AIR) && (event.packet.action == CPacketPlayerDigging.Action.DROP_ITEM || event.packet.action == CPacketPlayerDigging.Action.DROP_ALL_ITEMS)) {
                    val name = Globals.mc.player.inventory.getCurrentItem().displayName
                    if (droppedItems.containsKey(name)) {
                        droppedItems[name] = droppedItems[name]!! + 1
                    } else {
                        droppedItems[name] = 1
                    }
                    lastEventSend = event
                    return
                }
            }
            if (blocks.value) {
                if (event.packet.action == CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                    val name = event.packet.position.block.localizedName
                    if (minedBlocks.containsKey(name)) {
                        minedBlocks[name] = minedBlocks[name]!! + 1
                    } else {
                        minedBlocks[name] = 1
                    }
                    lastEventSend = event
                    return
                }
            }
        } else if (items.value && event.packet is CPacketUpdateSign) {
            val message = if (clientName.value) "I placed a Sign, thanks to " + Muffin.MODNAME + "!" else "I placed a Sign!"
            queueMessage(message)
            lastEventSend = event
            return
        } else if (blocks.value && event.getPacket() is CPacketPlayerTryUseItemOnBlock) {
            val itemStack = Globals.mc.player.inventory.getCurrentItem()
            if (itemStack.isEmpty) {
                lastEventSend = event
                return
            }
            if (itemStack.item is ItemBlock) {
                val name = Globals.mc.player.inventory.getCurrentItem().displayName
                if (placedBlocks.containsKey(name)) {
                    placedBlocks[name] = placedBlocks[name]!! + 1
                } else {
                    placedBlocks[name] = 1
                }
                lastEventSend = event
                return
            }
        }

    }

    @Listener
    private fun onPlayerConnect(event: PlayerConnectEvent.Join) {
        if (!timer.passed(2000.0)) return

        stringsToChose.clear()
        timer.reset()

        if (FriendManager.isFriend(event.username))
            sendToChat(StringBuilder("Your friend ").append(event.username).append(" just joined the server!").toString())
        else sendToChat(
            StringBuilder(
                joinMsgs[RandomUtils.random.nextInt(joinMsgs.size - 1)].replace("SERVERIP1D5A9E", InfoUtils.getServerIP() ?: "127.0.0.1")
            ).append(event.username).toString()
        )
    }

    @Listener
    private fun onPlayerDisconnect(event: PlayerConnectEvent.Leave) {
        if (!timer.passed(2000.0)) return

        stringsToChose.clear()
        timer.reset()
        sendToChat(StringBuilder(leaveMsgs[RandomUtils.random.nextInt(joinMsgs.size - 1)]).append(event.username).toString())
    }

    @Listener
    private fun onEntityUseItemFinish(event: EntityUseItemFinishEvent) {
        if (fullNullCheck()) return

        if (!food.value) return

        val mainHandStackItem = Globals.mc.player.heldItemMainhand.item
        val offHandStackItem = Globals.mc.player.heldItemOffhand.item

        if (event.entity == Globals.mc.player && (mainHandStackItem is ItemFood || offHandStackItem is ItemFood)) {
            var name: String? = null

            if (mainHandStackItem is ItemFood) {
                name = mainHandStackItem.getItemStackDisplayName(Globals.mc.player.heldItemMainhand)
            } else if (offHandStackItem is ItemFood) {
                name = offHandStackItem.getItemStackDisplayName(Globals.mc.player.heldItemOffhand)
            }

            if (name != null) {
                if (consumedItems.containsKey(name)) {
                    consumedItems[name] = consumedItems[name]!! + 1
                } else {
                    consumedItems[name] = 1
                }
            }
        }
    }

    private fun queueMessage(message: String) {
        if (messageQueue.size > 5) {
            return
        }
        messageQueue.add(message)
    }


}
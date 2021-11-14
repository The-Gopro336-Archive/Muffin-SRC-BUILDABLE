package me.han.muffin.client.module.modules.misc

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.mixin.ClientLoader
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mixin.netty.*
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.utils.timer.TimeUnit
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.util.StringUtils
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal object PacketLoggerModule: Module("PacketLogger", Category.MISC, "Log packets.") {
    private val page = EnumValue(Pages.General, "Page")

    private val all = Value({ page.value == Pages.General },false, "All")
    private val incoming = Value({ page.value == Pages.General },false, "Incoming")
    private val outgoing = Value({ page.value == Pages.General },true, "Outgoing")

    private val data = Value({ page.value == Pages.Format },false, "Data")
    private val showType = Value({ page.value == Pages.Format },false, "ShowType")
    private val showTime = Value({ page.value == Pages.Format },false, "ShowTime")

    private val printInChat = Value({ page.value == Pages.Printing },true, "PrintInChat")
    private val saveToFile = Value({ page.value == Pages.Printing },true, "SaveToFile")

    private val showClientTicks = Value({ page.value == Pages.Printing }, true, "ShowClientTicks")

    private val ignoreCancelled = Value({ page.value == Pages.Ignore },false, "IgnoreCancelled")
    private val verboseCancelled = Value({ page.value == Pages.Ignore && !ignoreCancelled.value },false, "VerboseCancelled")

    private val ignoreKeepAlive = Value({ page.value == Pages.Ignore },true, "IgnoreKeepAlive")
    private val ignoreChunkLoading = Value({ page.value == Pages.Ignore },true, "IgnoreChunkLoading")
    private val ignoreChat = Value({ page.value == Pages.Ignore },true, "IgnoreChat")
    private val ignoreParticles = Value({ page.value == Pages.Ignore },true, "IgnoreParticles")
    private val ignoreListHeadFoot = Value({ page.value == Pages.Ignore },true, "IgnoreHeadFoot")
    private val ignoreHeadLook = Value({ page.value == Pages.Ignore}, true, "IgnoreHeadLook")
    private val ignoreTransaction = Value({ page.value == Pages.Ignore}, true, "IgnoreTransaction")
    private val ignoreTimeUpdate = Value({ page.value == Pages.Ignore}, true, "IgnoreTimeUpdate")
    private val ignoreTeaming = Value({ page.value == Pages.Ignore}, true, "IgnoreTeaming")
    private val ignoreEntityMove = Value({ page.value == Pages.Ignore}, true, "IgnoreEntityMove")
    private val ignoreEntityTeleport = Value({ page.value == Pages.Ignore}, true, "IgnoreEntityTeleport")
    private val ignoreEntityEquipment = Value({ page.value == Pages.Ignore}, true, "IgnoreEntityEquipment")
    private val ignoreEntityVelocity = Value({ page.value == Pages.Ignore}, true, "IgnoreEntityVelocity")

    private val importantPosLook = Value({ page.value == Pages.Important }, false, "ImportantPosLook")

    init {
        addSettings(
            page,
            // general
            all, incoming, outgoing,
            // format
            data, showType, showTime,
            // printing
            printInChat, saveToFile, showClientTicks,
            // ignore //
            ignoreCancelled, verboseCancelled, ignoreKeepAlive, ignoreChunkLoading, ignoreChat, ignoreParticles, ignoreListHeadFoot,
            ignoreHeadLook, ignoreTransaction, ignoreTeaming, ignoreTimeUpdate, ignoreEntityMove, ignoreEntityTeleport, ignoreEntityEquipment, ignoreEntityVelocity,
            importantPosLook
        )
    }

    private val directory = "${Muffin.getInstance().directory.absolutePath}/packetlogs"
    private var filename = ""
    private var lines = ArrayList<String>()

    private val fileTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss_SSS")
    private val logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private var start = 0L
    private var last = 0L
    private val writeTimer = TickTimer(TimeUnit.SECONDS)

    private enum class Pages {
        General, Format, Ignore, Important, Printing
    }

    override fun onEnable() {
        start = System.currentTimeMillis()
        filename = "muffinPackets-${fileTimeFormatter.format(LocalTime.now())}.txt"
    }

    override fun onDisable() {
        if (saveToFile.value) write()
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        disable()
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (showClientTicks.value) {
            synchronized(this) {
                lines.add("Tick Pulse - Realtime: ${logTimeFormatter.format(LocalTime.now())} - Runtime: ${System.currentTimeMillis() - start}ms\n")
            }
        }

    //    Globals.mc.connection?.sendPacket(CrashPackets())

        /* Don't let lines get too big, write periodically to the file */
        if (saveToFile.value) if (lines.size >= 100 || writeTimer.tick(15L)) write()
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
    //    if (event.packet is CrashPackets) event.cancel()

        if (!outgoing.value) return
        if (event.packet == null) {
            ChatManager.sendMessage("KEKKEKEKEKEKEKEKEKEKWKWKWEWKE")
            return
        }

        if (event.isCanceled) {
            if (ignoreCancelled.value) return
            else if (verboseCancelled.value) {
                if (event.stage == EventStageable.EventStage.PRE) {
                    addClientPackets(EventStageable.EventStage.PRE, event.packet, true)
                } else if (all.value && event.stage == EventStageable.EventStage.POST) {
                    addClientPackets(EventStageable.EventStage.POST, event.packet, true)
                }
                return
            }
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            addClientPackets(EventStageable.EventStage.PRE, event.packet)
        } else if (all.value && event.stage == EventStageable.EventStage.POST) {
            addClientPackets(EventStageable.EventStage.POST, event.packet)
        }

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (!incoming.value) return

        if (event.isCanceled) {
            if (ignoreCancelled.value) return
            else if (verboseCancelled.value) {
                if (event.stage == EventStageable.EventStage.PRE) {
                    addServerPackets(EventStageable.EventStage.PRE, event.packet, true)
                } else if (all.value && event.stage == EventStageable.EventStage.POST) {
                    addServerPackets(EventStageable.EventStage.POST, event.packet, true)
                }
                return
            }
        }

        if (event.stage == EventStageable.EventStage.PRE) {
            addServerPackets(EventStageable.EventStage.PRE, event.packet)
        } else if (all.value && event.stage == EventStageable.EventStage.POST) {
            addServerPackets(EventStageable.EventStage.POST, event.packet)
        }
    }

    private fun addClientPackets(stage: EventStageable.EventStage, packet: Packet<*>, isCancelled: Boolean = false) {
        when (packet) {
            is CPacketAnimation -> addLine(packet, stage, isCancelled, "hand: ${packet.hand}")
            is CPacketPlayer -> {
                addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "yaw: ${packet.yaw} " + "pitch: ${packet.pitch} " + "onGround: ${packet.isOnGround} " + "moving: ${packet.moving} " + "rotating: ${packet.rotating}")
            }
            is CPacketPlayer.Rotation -> {
                addLine(packet, stage, isCancelled, "yaw: ${packet.yaw} " + "pitch: ${packet.pitch} " + "onGround: ${packet.isOnGround}")
            }
            is CPacketPlayer.Position -> {
                addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "onGround: ${packet.isOnGround}")
            }
            is CPacketPlayer.PositionRotation -> {
                addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "yaw: ${packet.yaw} " + "pitch: ${packet.pitch} " + "onGround: ${packet.isOnGround}")
            }
            is CPacketPlayerDigging -> {
                addLine(packet, stage, isCancelled, "positionX: ${packet.position.x} " + "positionY: ${packet.position.y} " + "positionZ: ${packet.position.z} " + "facing: ${packet.facing} " + "action: ${packet.action} ")
            }
            is CPacketEntityAction -> {
                addLine(packet, stage, isCancelled, "action: ${packet.action} " + "auxData: ${packet.auxData}")
            }
            is CPacketUseEntity -> {
                addLine(packet, stage, isCancelled, "action: ${packet.action} " + "hand: ${packet.hand} " + "hitVecX: ${packet.hitVec.x} " + "hitVecY: ${packet.hitVec.y} " + "hitVecZ: ${packet.hitVec.z}")
            }
            is CPacketPlayerTryUseItem -> {
                addLine(packet, stage, isCancelled, "hand: ${packet.hand}")
            }
            is CPacketPlayerTryUseItemOnBlock -> {
                addLine(packet, stage, isCancelled, "pos: ${packet.pos} " + "direction: ${packet.direction} " + "hand: ${packet.hand} " + "facingX: ${packet.facingX} " + "facingY: ${packet.facingY} " + "facingZ: ${packet.facingZ}")
            }
            is CPacketHeldItemChange -> {
                addLine(packet, stage, isCancelled, "slotId: ${packet.slotId}")
            }
            is CPacketConfirmTeleport -> {
                addLine(packet, stage, isCancelled, "teleportID: ${packet.teleportId}")
            }
            is CPacketChatMessage -> {
                if (!ignoreChat.value) {
                    addLine(packet, stage, isCancelled, "message: ${packet.message}")
                }
            }
            is CPacketKeepAlive -> {
                if (!ignoreKeepAlive.value) {
                    addLine(packet, stage, isCancelled, "key: ${packet.key}")
                }
            }
            else -> addLine(packet, stage, isCancelled)
        }
    }

    private fun addServerPackets(stage: EventStageable.EventStage, packet: Packet<*>, isCancelled: Boolean = false) {
        if (importantPosLook.value) {
            if (packet is SPacketPlayerPosLook) {
                val flags = StringBuilder().run {
                    append("flags: ")
                    for (entry in packet.flags) append("> ${entry.name} ")
                    toString()
                }
                addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "pitch: ${packet.pitch} " + "yaw: ${packet.yaw} " + "teleportID: ${packet.teleportId}" + flags)
            }
            return
        }

        when (packet) {
            is SPacketEntityTeleport -> {
                if (!ignoreEntityTeleport.value)
                    addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "pitch: ${packet.pitch} " + "yaw: ${packet.yaw} " + "entityID: ${packet.entityId}")
            }
            is SPacketEntityMetadata -> {
                val dataEntry = StringBuilder().run {
                    append("dataEntries: ")
                    for (entry in packet.dataManagerEntries) {
                        append("> isDirty: ${entry.isDirty} key: ${entry.key} value: ${entry.value} ")
                    }
                    toString()
                }
                addLine(packet, stage, isCancelled, dataEntry)
            }

            is SPacketUnloadChunk -> if (!ignoreChunkLoading.value) addLine(packet, stage, isCancelled, "x: ${packet.x} " + "z: ${packet.z}")
            is SPacketDestroyEntities -> {
                val entities = StringBuilder().run {
                    append("entityIDs: ")
                    for (entry in packet.entityIDs) append("> $entry ")
                    toString()
                }
                addLine(packet, stage, isCancelled, entities)
            }

            is SPacketPlayerPosLook -> {
                val flags = StringBuilder().run {
                    append("flags: ")
                    for (entry in packet.flags) append("> ${entry.name} ")
                    toString()
                }
                addLine(packet, stage, isCancelled, "x: ${packet.x} " + "y: ${packet.y} " + "z: ${packet.z} " + "pitch: ${packet.pitch} " + "yaw: ${packet.yaw} " + "teleportID: ${packet.teleportId}" + flags)
            }

            is SPacketBlockChange -> addLine(packet, stage, isCancelled, "x: ${packet.blockPosition.x} " + "y: ${packet.blockPosition.y} " + "z: ${packet.blockPosition.z}")

            is SPacketMultiBlockChange -> {
                val changedBlock = StringBuilder().run {
                    append("changedBlocks: ")
                    for (changedBlock in packet.changedBlocks) append("> x: ${changedBlock.pos.x} y: ${changedBlock.pos.y} z: ${changedBlock.pos.z} ")
                    toString()
                }
                addLine(packet, stage, isCancelled, changedBlock)
            }

            is SPacketTimeUpdate -> {
                if (!ignoreTimeUpdate.value) {
                    addLine(packet, stage, isCancelled, "totalWorldTime: ${packet.totalWorldTime} " + "worldTime: ${packet.worldTime}")
                }
            }
            is SPacketChat -> {
                if (!ignoreChat.value) {
                    addLine(packet, stage, isCancelled, "unformattedText: ${packet.chatComponent.unformattedText} " + "type: ${packet.type} " + "isSystem: ${packet.isSystem}")
                }
            }
            is SPacketTeams -> {
                if (!ignoreTeaming.value)
                    addLine(packet, stage, isCancelled, "action: ${packet.action} " + "displayName: ${packet.displayName} " + "color: ${packet.color}")
            }
            is SPacketChunkData -> {
                addLine(packet, stage, isCancelled, "chunkX: ${packet.chunkX} " + "chunkZ: ${packet.chunkZ} " + "extractedSize: ${packet.extractedSize}")
            }
            is SPacketEntityProperties -> {
                addLine(packet, stage, isCancelled, "entityID: ${packet.entityId}")
            }
            is SPacketUpdateTileEntity -> {
                addLine(packet, stage, isCancelled, "posX: ${packet.pos.x} " + "posY: ${packet.pos.y} " + "posZ: ${packet.pos.z}")
            }
            is SPacketSpawnObject -> {
                addLine(packet, stage, isCancelled, "entityID: ${packet.entityID} " + "data: ${packet.data}")
            }
            is SPacketKeepAlive -> {
                if (!ignoreKeepAlive.value) {
                    addLine(packet, stage, isCancelled, "id: ${packet.id}")
                }
            }
            is SPacketParticles -> {
                if (!ignoreParticles.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketPlayerListHeaderFooter -> {
                if (!ignoreListHeadFoot.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntityHeadLook -> {
                if (!ignoreHeadLook.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntity.S15PacketEntityRelMove -> {
                if (!ignoreEntityMove.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntity.S16PacketEntityLook -> {
                if (!ignoreEntityMove.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntity.S17PacketEntityLookMove -> {
                if (!ignoreEntityMove.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntityVelocity -> {
                if (!ignoreEntityVelocity.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketEntityEquipment -> {
                if (!ignoreEntityEquipment.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            is SPacketConfirmTransaction -> {
                if (!ignoreTransaction.value) {
                    addLine(packet, stage, isCancelled)
                }
            }
            else -> addLine(packet, stage, isCancelled)
        }
    }


    private fun addLine(packet: Packet<*>, stage: EventStageable.EventStage, isCancelled: Boolean = false, castedData: String? = null) {
        val timePHolder = if (showTime.value) "Since Start: ${System.currentTimeMillis() - start} | Since Last: ${System.currentTimeMillis() - last}, " else ""

        val packetSimpleName = packet.javaClass.simpleName

        var placeholder = "$timePHolder$packetSimpleName, "

        if (data.value && !packetSimpleName.startsWith("f$")) {
            if (castedData != null) {
                placeholder += castedData
            } else {
                placeholder = getPacketData(packet, placeholder)
            }
        }

        val finalPlaceholder = when (stage) {
            EventStageable.EventStage.PRE -> if (isCancelled) "${ChatFormatting.DARK_AQUA}CancelledPre: $placeholder" else "${ChatFormatting.RED}PRE: $placeholder"
            EventStageable.EventStage.POST -> if (isCancelled) "${ChatFormatting.DARK_PURPLE}CancelledPost: $placeholder" else "${ChatFormatting.GREEN}POST: $placeholder"
        }

        lines.add(finalPlaceholder)
        last = System.currentTimeMillis()

        if (printInChat.value) ChatManager.sendMessage(finalPlaceholder)
    }

    private fun getPacketData(packet: Packet<*>, basePlaceholder: String): String {
        var editablePlaceholder = basePlaceholder

        return try {
            var clazz: Class<*> = packet.javaClass
            while (clazz != Any::class.java) {
                for (field in clazz.declaredFields) {
                    if (field != null) {
                        if (!field.isAccessible) field.isAccessible = true
                        val simpleName = field.type.simpleName
                        editablePlaceholder += StringUtils.stripControlCodes(if (showType.value) simpleName else "" + " " + field.name + ": " + field[packet])
                    }
                }
                clazz = clazz.superclass
            }

            editablePlaceholder
        } catch (e: Exception) {
            e.printStackTrace()
            editablePlaceholder
        }
    }

    private fun write() {
        val lines = synchronized(this) {
            val cache = lines
            lines = ArrayList()
            cache
        }

        Thread {
            try {
                with(File(directory)) {
                    if (!exists()) mkdir()
                }
                FileWriter("$directory/${filename}", true).buffered().use {
                    for (line in lines) it.write(StringUtils.stripControlCodes(line))
                }
            } catch (e: IOException) {
                ClientLoader.LOGGER.error("$chatName Error saving!", e)
            }
        }.start()

    }

    class CrashPackets: Packet<INetHandlerPlayServer> {
        override fun readPacketData(buf: PacketBuffer) {
        }
        override fun writePacketData(buf: PacketBuffer) {
        }
        override fun processPacket(handler: INetHandlerPlayServer) {
        }
    }

}
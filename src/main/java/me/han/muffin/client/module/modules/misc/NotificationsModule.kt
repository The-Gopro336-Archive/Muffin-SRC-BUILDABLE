package me.han.muffin.client.module.modules.misc

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.world.WorldEntityEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.NotificationManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.combat.AutoCrystalModule
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.client.TrayUtils
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.math.Direction.Companion.toDirection
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.network.play.server.SPacketDisconnect
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.network.play.server.SPacketUpdateHealth
import net.minecraft.util.StringUtils
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.Display
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.TrayIcon

internal object NotificationsModule: Module("Notifications", Category.MISC, "Send some notifications.") {
    val totem = Value(true, "TotemPops")
    val resetLeavingVisual = Value({ totem.value }, false, "TPops-ResetVisual")

    val desktopMsg = Value(false, "DesktopNotif")

    private val desktopMsgMode = EnumValue(DesktopMessageMode.Looping, "DesktopNotifMode")

    private val pearl = Value(true, "Pearl")
    private val strength = Value(true, "Strength")
    private val strengthEsp = Value({ strength.value }, false, "StrengthEsp")
    private val visualRange = Value(false, "VisualRange")
    private val leaving = Value({ visualRange.value }, false, "Leaving")
    private val boxDetector = Value(true, "BoxDetector")

    private val queue = Value({ desktopMsg.value }, true, "Queue")
    private val damage = Value({ desktopMsg.value }, true, "Damage")
    private val kick = Value({ desktopMsg.value }, true, "Kick")
    private val pm = Value({ desktopMsg.value }, true, "PM")
    private val name = Value({ desktopMsg.value }, true, "Name")
    private val selfTotem = Value({ desktopMsg.value }, true, "SelfTotem")

    private val armorDetect = Value(false, "Armor")
    private val armorValue = NumberValue({ armorDetect.value }, 35, 1, 100, 1, "ArmorValue")
    private val armorMsgMode = EnumValue({ armorDetect.value }, ArmorMsgMode.Chat, "ArmorMsgMode")
    private val friend = Value({ armorDetect.value }, false, "Friend")

    private var prevPlayer = -1
    var strengthEspPlayer: EntityPlayer? = null

    private val strengthList = HashSet<EntityPlayer>()
    private val fuckedPlayers = HashSet<EntityPlayer>()

    private val armourList = HashMap<EntityPlayer, Int>()

    private var damageOnce = false
    private var totemOnce = false
    private var nameOnce = false
    private var pmOnce = false

    private val healthTimer = Timer()
    private val totemPopTimer = Timer()
    private val queueTimer = Timer()
    private val nameTimer = Timer()
    private val whisperTimer = Timer()
    private val disconnectTimer = Timer()

    init {
        addSettings(
            desktopMsg,
            desktopMsgMode,
            totem,
            resetLeavingVisual,
            pearl,
            queue,
            damage,
            kick,
            name,
            pm,
            selfTotem,
            strength,
            strengthEsp,
            visualRange,
            leaving,
            boxDetector,
            armorDetect,
            armorValue,
            armorMsgMode,
            friend
        )
    }

    private enum class ArmorMsgMode {
        Chat, Notification
    }

    private enum class DesktopMessageMode {
        Once, Looping
    }

    override fun onEnable() {
        if (Muffin.getInstance().trayUtils == null) Muffin.getInstance().trayUtils = TrayUtils()

        damageOnce = false
        totemOnce = false
        nameOnce = false
        pmOnce = false
    }

    override fun onDisable() {
        if (Muffin.getInstance().trayUtils != null) {
            Muffin.getInstance().trayUtils.systemTray?.remove(Muffin.getInstance().trayUtils.trayIcon)
            Muffin.getInstance().trayUtils = null
        }
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (Display.isActive()) {
            damageOnce = false
            totemOnce = false
            nameOnce = false
            pmOnce = false
        }

        if (boxDetector.value) {
            getBoxList()
        }

        if (strength.value) {
            strengthEspPlayer = null

            for (player in Globals.mc.world.playerEntities) {
                if (player == null || player == Globals.mc.player || player == Globals.mc.renderViewEntity || !player.isAlive) continue

                val playerName = player.name
                if (player.isPotionActive(MobEffects.STRENGTH) && !strengthList.contains(player)) {
                    ChatManager.sendDeleteMessage(ChatManager.darkTextColour + playerName + ChatManager.textColour + " has " + ChatFormatting.RED + "(drank)" + ChatManager.darkTextColour + " strength!", playerName, ChatIDs.STRENGTH_NOTIF)
                    strengthList.add(player)
                }

                if (strengthList.contains(player) && !player.isPotionActive(MobEffects.STRENGTH)) {
                    ChatManager.sendDeleteMessage(ChatManager.darkTextColour + playerName + ChatManager.textColour + " has " + ChatFormatting.GREEN + "(ran)" + ChatManager.darkTextColour + " out of strength!", playerName, ChatIDs.STRENGTH_NOTIF)
                    strengthList.remove(player)
                }

                strengthEspPlayer = player
            }
        }

        if (armorDetect.value) {
            for (player in Globals.mc.world.playerEntities) {
                if (!player.isAlive || !FriendManager.isFriend(player.name)) continue

                for (armour in player.inventory.armorInventory) {
                    if (armour != ItemStack.EMPTY) {
                        val durability = EntityUtil.getArmorPct(armour)
                        if (durability < armorValue.value && !armourList.containsKey(player)) {
                            if (player == Globals.mc.player) {
                                if (armorMsgMode.value == ArmorMsgMode.Chat) ChatManager.sendMessage("your ${getArmourName(armour)} on low durability.")
                                else if (armorMsgMode.value == ArmorMsgMode.Notification) NotificationManager.addNotification(NotificationManager.NotificationType.Info, "", "your ${getArmourName(armour)} on low durability.")
                            }
                            if (player != Globals.mc.player && friend.value) {
                                Globals.mc.player.sendChatMessage("/msg ${player.name} your ${getArmourName(armour)} on low durability.")
                            }
                            armourList[player] = player.inventory.armorInventory.indexOf(armour)
                        }
                        if (!armourList.containsKey(player) || armourList[player] != player.inventory.armorInventory.indexOf(armour) || durability <= armorValue.value) {
                            continue
                        }
                        armourList.remove(player)
                    }
                }

                if (!armourList.containsKey(player) || player.inventory.armorInventory[armourList[player]!!] != ItemStack.EMPTY) {
                    continue
                }
                armourList.remove(player)
            }
        }

    }

    private fun getArmourName(stack: ItemStack): String {
        if (stack.item == Items.DIAMOND_HELMET || stack.item == Items.GOLDEN_HELMET || stack.item == Items.IRON_HELMET || stack.item == Items.CHAINMAIL_HELMET || stack.item == Items.LEATHER_HELMET) {
            return "helmet is"
        }
        if (stack.item == Items.DIAMOND_CHESTPLATE || stack.item == Items.GOLDEN_CHESTPLATE || stack.item == Items.IRON_CHESTPLATE || stack.item == Items.CHAINMAIL_CHESTPLATE || stack.item == Items.LEATHER_CHESTPLATE) {
            return "chestplate is"
        }
        if (stack.item == Items.DIAMOND_LEGGINGS || stack.item == Items.GOLDEN_LEGGINGS || stack.item == Items.IRON_LEGGINGS || stack.item == Items.CHAINMAIL_LEGGINGS || stack.item == Items.LEATHER_LEGGINGS) {
            return "leggings are"
        }
        return "boots are"
    }

    @Listener
    private fun onWorldEntityAdded(event: WorldEntityEvent.Add) {
        if (fullNullCheck()) return

        if (!Globals.mc.player.isAlive || event.entity == null) return

        if (visualRange.value) {
            if (event.entity is EntityPlayer && !event.entity.name.equals(Globals.mc.player.name, ignoreCase = true)) {
                val playerName = event.entity.name

                val msg = (if (FriendManager.isFriend(playerName)) ChatFormatting.GREEN else ChatFormatting.RED).toString() +
                    playerName + ChatManager.textColour + " has been spotted so enjoy Muffin."

                if (desktopMsg.value && !Display.isActive()) {
                    Muffin.getInstance().trayUtils.trayIcon?.displayMessage("Player in range", StringUtils.stripControlCodes(msg), TrayIcon.MessageType.NONE)
                }

                ChatManager.sendDeleteMessage(msg, playerName, ChatIDs.VRANGE_NOTIF)
                if (event.entity.entityId == prevPlayer) prevPlayer = -1
            }
        }

        if (pearl.value) {
            val closestPlayer = event.entity.entityWorld.getClosestPlayerToEntity(event.entity, 3.0) ?: return
            if (closestPlayer == Globals.mc.player || closestPlayer == Globals.mc.renderViewEntity) return

            if (event.entity is EntityEnderPearl) {
                val playerName = closestPlayer.name
                val facing = event.entity.horizontalFacing.toDirection()

                val facingPlaceholder = facing?.displayFacing?.toLowerCase() ?: "unknown direction"
                val towardPlaceholder = facing?.displayToward?.toLowerCase() ?: "unsupported"

                ChatManager.sendDeleteMessage("$playerName has thrown an ender pearl toward $facingPlaceholder ($towardPlaceholder).", playerName, ChatIDs.EPEARL_NOTIF)
            }
        }

    }

    @Listener
    private fun onWorldEntityRemove(event: WorldEntityEvent.Remove) {
        if (fullNullCheck()) return

        if (!Globals.mc.player.isAlive || event.entity == null) return

        if (visualRange.value) {
            if (event.entity is EntityPlayer && !event.entity.name.equals(Globals.mc.player.name, ignoreCase = true)) {
                if (prevPlayer != event.entity.entityId) {
                    prevPlayer = event.entity.entityId
                    val playerName = event.entity.name

                    val msg = (if (FriendManager.isFriend(playerName)) ChatFormatting.GREEN else ChatFormatting.RED).toString() +
                        playerName + ChatManager.textColour + " has left."

                    if (leaving.value) {
                        if (desktopMsg.value && !Display.isActive()) {
                            Muffin.getInstance().trayUtils.trayIcon?.displayMessage("Player out of range", StringUtils.stripControlCodes(msg), TrayIcon.MessageType.NONE)
                        }
                        ChatManager.sendDeleteMessage(msg, playerName, ChatIDs.VRANGE_NOTIF)
                    }
                }
            }
        }

    }

    @Listener
    private fun onConnectServer(event: ServerEvent.Connect) {
        if (event.state == ServerEvent.State.FAILED) return

        if (!desktopMsg.value || Display.isActive()) return

        if (queue.value && Globals.mc.currentServerData != null && Globals.mc.currentServerData?.serverIP.equals("2b2t.org", ignoreCase = true) && queueTimer.passed(10000.0)) {
            Muffin.getInstance().trayUtils.trayIcon?.displayMessage("Connected to the server", "You have finished going through the queue.", TrayIcon.MessageType.NONE)
            queueTimer.reset()
        }

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (!desktopMsg.value || Display.isActive()) {
            return
        }

        if (event.packet is SPacketEntityStatus && selfTotem.value) {
            if (event.packet.getEntity(Globals.mc.world) == Globals.mc.player && event.packet.opCode == 35.toByte()) {
                if (!totemOnce && totemPopTimer.passed(10000.0)) {
                    Muffin.getInstance().trayUtils.trayIcon?.displayMessage("Popped a totem", "You just popped a totem.", TrayIcon.MessageType.NONE)
                    if (desktopMsgMode.value == DesktopMessageMode.Once) totemOnce = true
                    totemPopTimer.reset()
                }
            } else {
                totemOnce = false
            }
        }

        if (event.packet is SPacketUpdateHealth && damage.value) {
            if (event.packet.health < Globals.mc.player.health) {
                if (!damageOnce && healthTimer.passed(10000.0)) {
                    Muffin.getInstance().trayUtils.trayIcon?.displayMessage("Damage received", "You've just taken damage.", TrayIcon.MessageType.NONE)
                    if (desktopMsgMode.value == DesktopMessageMode.Once) damageOnce = true
                    healthTimer.reset()
                }
            } else {
                damageOnce = false
            }
        }

        if (event.packet is SPacketChat && (pm.value || name.value)) {
            val formattedText = event.packet.chatComponent.formattedText

            if (name.value && !formattedText.contains("§d") && formattedText.contains(String.format(" %s ", Globals.mc.player.name))) {
                if (!nameOnce && nameTimer.passed(10000.0)) {
                    Muffin.getInstance().trayUtils.trayIcon?.displayMessage(
                        "Name called in chat",
                        "Your name has been written in chat.",
                        TrayIcon.MessageType.NONE
                    )
                    if (desktopMsgMode.value == DesktopMessageMode.Once) nameOnce = true
                    nameTimer.reset()
                }
            } else {
                nameOnce = false
            }

            if (pm.value && formattedText.contains("§d") && formattedText.contains(" whispers: ")) {
                if (!pmOnce && whisperTimer.passed(10000.0)) {
                    Muffin.getInstance().trayUtils.trayIcon?.displayMessage(
                        "Private message received",
                        "You have received a private message.",
                        TrayIcon.MessageType.NONE
                    )
                    if (desktopMsgMode.value == DesktopMessageMode.Once) pmOnce = true
                    whisperTimer.reset()
                }
            } else {
                pmOnce = false
            }
        }

        if (event.packet is SPacketDisconnect && kick.value && disconnectTimer.passed(10000.0)) {
            Muffin.getInstance().trayUtils.trayIcon?.displayMessage(
                "Kicked from the server.",
                "You've just disconnected.",
                TrayIcon.MessageType.NONE
            )
            disconnectTimer.reset()
        }

    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (strength.value && strengthEsp.value && strengthEspPlayer != null) {
            val vector = MathUtils.getInterpolatedRenderPos(strengthEspPlayer!!, event.partialTicks)
            val colour = ColourUtils.getColorByRange(strengthEspPlayer!!)
            val renderBB = AxisAlignedBB(0.0, 0.0, 0.0, strengthEspPlayer!!.width.toDouble(), strengthEspPlayer!!.height.toDouble(), strengthEspPlayer!!.width.toDouble()).offset(vector.x - strengthEspPlayer!!.width / 2, vector.y, vector.z - strengthEspPlayer!!.width / 2)
            RenderUtils.drawBoxESP(renderBB, colour.red, colour.green, colour.blue, 60)
        }

        if (boxDetector.value && fuckedPlayers.isNotEmpty()) {
            for (player in fuckedPlayers) {
                val vec = MathUtils.getInterpolatedRenderPos(player, event.partialTicks)
                val x = vec.x
                val y = vec.y
                val z = vec.z
                val renderBB = AxisAlignedBB(0.0, 0.0, 0.0, player.width.toDouble(), player.height.toDouble() / 2, player.width.toDouble()).offset(x - player.width / 2, y, z - player.width / 2)
                RenderUtils.drawBoxESP(renderBB, ColourUtils.toRGBAClient(65))
                RenderUtils.drawBoxOutlineESP(renderBB, ColourUtils.toRGBAClient(255), 2.0F)
            }
        }

    }

    private fun getBoxList(): HashSet<EntityPlayer> {
        fuckedPlayers.clear()

        for (player in Globals.mc.world.playerEntities) {
            if (EntityUtil.isntValid(player, 8.0) || !checkHasNoBox(player)) continue
            fuckedPlayers.add(player)
        }

        return fuckedPlayers
    }


    private fun checkHasNoBox(player: EntityPlayer): Boolean {
        val pos = player.flooredPosition.add(0.0, -1.0, 0.0)

        if (!pos.isAir) {
            if (canPlace(pos.south()) || canPlace(pos.south().south()) && pos.add(0, 1, 1).block == Blocks.AIR) return true
            if (canPlace(pos.east()) || canPlace(pos.east().east()) && pos.add(1, 1, 0).block == Blocks.AIR) return true

            return if (canPlace(pos.west()) || canPlace(pos.west().west()) && pos.add(-1, 1, 0).block == Blocks.AIR) {
                true
            } else {
                canPlace(pos.north()) || canPlace(pos.north().north()) && pos.add(0, 1, -1).block == Blocks.AIR
            }
        }

        return false
    }

    private fun canPlace(pos: BlockPos, onePointThirteen: Boolean = AutoCrystalModule.onePointThirteen.value): Boolean {
        return CrystalUtils.canPlaceCrystal(pos, true, onePointThirteen)
    }

}
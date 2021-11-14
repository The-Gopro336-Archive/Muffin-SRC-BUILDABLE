package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.player.PlayerSlowEvent
import me.han.muffin.client.event.events.movement.CollideSoulSandEvent
import me.han.muffin.client.event.events.movement.LandOnSlimeEvent
import me.han.muffin.client.event.events.movement.PlayerUpdateMoveStateEvent
import me.han.muffin.client.event.events.movement.WalkOnSlimeEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.player.FreecamModule
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mixin.entity.isInWeb
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemShield
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

// 0.2550
internal object NoSlowModule : Module("NoSlow", Category.MOVEMENT, "No longer slow down when eating, blocking or while keeping the inventory open.") {

    val inventoryWalk = Value(true, "InventoryWalk")
    private val strictInventoryWalk = Value(true, "StrictInvWalk")
    val noSneak = Value(true, "NoSneak")

    private val webMode = EnumValue(WebMode.Motion, "WebMode")
    private val webSpeed = NumberValue({ webMode.value == WebMode.Speed }, 2.0, 0.0, 10.0, 0.1, "WebSpeed")
    private val webTimerSpeed = NumberValue({ webMode.value == WebMode.Speed }, 12.0, 0.0, 30.0, 0.2, "WebTimerSpeed")
    // private val slowerMotion = Value({ webMode.value == WebMode.Speed && webTimerSpeed.value > 0.0 }, true, "SlowerMotion")

    private val items = Value(true, "Item")

    private val soulSand = Value(true, "SoulSand")
    private val slime = Value(true, "Slime")

    private val ncpStrict = Value(true, "NCPStrict")
    private val airStrict = Value(true, "AirStrict")
    private val newStrict = Value(false, "NewStrict")
    private val testStrict = Value(false, "TestStrict")

    var isSneaking = false
    var isEating = false

    private val webTimer = Timer()
    private var isPrevInWeb = false

    private val KEYS_SNEAK = arrayOf(
        Globals.mc.gameSettings.keyBindForward,
        Globals.mc.gameSettings.keyBindLeft,
        Globals.mc.gameSettings.keyBindRight,
        Globals.mc.gameSettings.keyBindBack,
        Globals.mc.gameSettings.keyBindJump,
        Globals.mc.gameSettings.keyBindSneak
    )

    private val KEYS = arrayOf(
        Globals.mc.gameSettings.keyBindForward,
        Globals.mc.gameSettings.keyBindLeft,
        Globals.mc.gameSettings.keyBindRight,
        Globals.mc.gameSettings.keyBindBack,
        Globals.mc.gameSettings.keyBindJump
    )

    private enum class WebMode {
        Off, Normal, Motion, New, Speed
    }

    init {
        addSettings(inventoryWalk, strictInventoryWalk, noSneak, webMode, webSpeed, webTimerSpeed, items, soulSand, slime, ncpStrict, airStrict) //newStrict)
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (event.stage == EventStageable.EventStage.PRE) {

            when (webMode.value) {
                WebMode.Motion -> {
                    if (Globals.mc.player.isInWeb) {
                        Globals.mc.player.motionX *= 0.20000000298023224
                        Globals.mc.player.motionZ *= 0.20000000298023224
                        Globals.mc.player.motionY = 0.0
                        Globals.mc.player.onGround = true
                    }
                    Globals.mc.player.isInWeb = false
                }
                WebMode.Normal -> {
                    Globals.mc.player.isInWeb = false
                    if (Globals.mc.player.ridingEntity != null) Globals.mc.player.ridingEntity?.isInWeb = false
                }
                WebMode.New -> {
                    if (Globals.mc.player.isInWeb) {
                        Globals.mc.player.onGround = false
                        Globals.mc.player.isInWeb = false
                        Globals.mc.player.motionX *= 0.84
                        Globals.mc.player.motionZ *= 0.84
                    }
                }
            }

//            if (items.value && newStrict.value && Globals.mc.player.isHandActive && !Globals.mc.player.isRiding) {
//                // Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(Globals.mc.player.posX, Globals.mc.player.posY + 0.027, Globals.mc.player.posZ, Globals.mc.player.onGround))
//                 Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.027, Globals.mc.player.posZ)
//                // LocalMotionManager.addPacket(this, LocalMotionManager.PlayerPacket(onGround = Globals.mc.player.onGround, pos = Globals.mc.player.positionVector.add(0.0, 0.027, 0.0)))
//                isEating = true
//            }
            

        } else if (event.stage == EventStageable.EventStage.POST) {
            if (items.value && ncpStrict.value && Globals.mc.player.isHandActive && !Globals.mc.player.isRiding) {
                val item = Globals.mc.player.activeItemStack.item
                if ((MovementUtils.isMoving() && item is ItemFood) || item is ItemBow || item is ItemPotion) {
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, Globals.mc.player.flooredPosition, EnumFacing.DOWN))
                }
            }

        }

    }

    var currentPosY = -50.0

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (items.value && !isEating && newStrict.value && Globals.mc.player.isHandActive && !Globals.mc.player.isRiding) {
            Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.027, Globals.mc.player.posZ)

            Globals.mc.player.motionY = 0.0
            event.y = 0.0

            if (!Globals.mc.player.collidedVertically) {
                Globals.mc.player.motionY -= 1E-6
                event.y = Globals.mc.player.motionY
            }

            currentPosY = Globals.mc.player.posY
            isEating = true
        }

        if (isEating) {
            Globals.mc.player.motionY = 0.0
            event.y = 0.0
            if (!Globals.mc.player.collidedVertically) {
                Globals.mc.player.motionY -= 1E-6
                event.y = Globals.mc.player.motionY
            }
            if (!Globals.mc.player.isHandActive || Globals.mc.player.isRiding) {
                isEating = false
                currentPosY = -50.0
            }
        }

        if (webMode.value == WebMode.Speed) {
            if (webTimer.passed(250.0) && Globals.mc.player.isInWeb) {
                if (webTimerSpeed.value > 0.0 && !isPrevInWeb && !MovementUtils.isOnGround(0.08)) {
                    isPrevInWeb = true
                    TimerManager.setTimer(webTimerSpeed.value.toFloat())
                }

                if (Globals.mc.player.onGround) {
                    event.x *= webSpeed.value
                    event.z *= webSpeed.value
                    return
                }

                if (Globals.mc.player.movementInput.sneak) {
                    event.y *= webSpeed.value + 10.0
                }
            }
        }

        if (webTimerSpeed.value > 0.0 && isPrevInWeb && (!Globals.mc.player.isInWeb || Globals.mc.player.onGround || MovementUtils.isOnGround(0.08))) {
            isPrevInWeb = false
            TimerManager.resetTimer()
        }

    }


    @Listener
    private fun onPlayerUpdateMoveStateEvent(event: PlayerUpdateMoveStateEvent) {
        if (fullNullCheck()) return

        if (event.movementInput !is MovementInputFromOptions) return

        if (!isEating && !isSneaking && items.value && !Globals.mc.gameSettings.keyBindSneak.isKeyDown && Globals.mc.player.isHandActive && !Globals.mc.player.isRiding) {
            event.movementInput.moveForward /= 0.2F
            Globals.mc.player.movementInput.moveStrafe /= 0.2F
        }

        if (Globals.mc.currentServerData?.serverIP?.toLowerCase() == "2b2t.org") {
            if (items.value && airStrict.value && !Globals.mc.player.isSneaking && Globals.mc.player.isHandActive && !Globals.mc.player.isRiding) {
                Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                isSneaking = true
            }

            if (isSneaking && items.value && airStrict.value && !Globals.mc.player.isHandActive) {
                Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                isSneaking = false
            }
        }

        if (inventoryWalk.value && strictInventoryWalk.value) {
            if (AutoWalkModule.isEnabled || Globals.mc.currentScreen is GuiChat || Globals.mc.currentScreen is GuiRepair || Globals.mc.currentScreen is GuiEditSign || Globals.mc.currentScreen == null) {
                return
            }

            event.movementInput.moveStrafe = 0.0F
            event.movementInput.moveForward = 0.0F

            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindForward.keyCode))
            if (Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindForward.keyCode)) {
                ++event.movementInput.moveForward
                event.movementInput.forwardKeyDown = true
            } else {
                event.movementInput.forwardKeyDown = false
            }

            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindBack.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindBack.keyCode))
            if (Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindBack.keyCode)) {
                --event.movementInput.moveForward
                event.movementInput.backKeyDown = true
            } else {
                event.movementInput.backKeyDown = false
            }

            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindLeft.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindLeft.keyCode))
            if (Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindLeft.keyCode)) {
                ++event.movementInput.moveStrafe
                event.movementInput.leftKeyDown = true
            } else {
                event.movementInput.leftKeyDown = false
            }

            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindRight.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindRight.keyCode))
            if (Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindRight.keyCode)) {
                --event.movementInput.moveStrafe
                event.movementInput.rightKeyDown = true
            } else {
                event.movementInput.rightKeyDown = false
            }

            KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindJump.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindJump.keyCode))
            event.movementInput.jump = Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindJump.keyCode)

            if (!noSneak.value) {
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindSneak.keyCode))
                event.movementInput.sneak = Keyboard.isKeyDown(Globals.mc.gameSettings.keyBindSneak.keyCode)
            }

        }

        /*
        if (inventoryWalk.value) {
            if (Globals.mc.currentScreen is GuiChat || Globals.mc.currentScreen is GuiEditSign || Globals.mc.currentScreen == null) {
                return
            }

            var keys = arrayOf(
                Globals.mc.gameSettings.keyBindForward, Globals.mc.gameSettings.keyBindLeft, Globals.mc.gameSettings.keyBindRight, Globals.mc.gameSettings.keyBindBack, Globals.mc.gameSettings.keyBindJump, Globals.mc.gameSettings.keyBindSneak
            )

            if (noSneak.value) {
                keys = arrayOf(
                    Globals.mc.gameSettings.keyBindForward, Globals.mc.gameSettings.keyBindLeft, Globals.mc.gameSettings.keyBindRight, Globals.mc.gameSettings.keyBindBack, Globals.mc.gameSettings.keyBindJump
                )
            }

            for (bind in keys) {
                KeyBinding.setKeyBindState(bind.keyCode, Keyboard.isKeyDown(bind.keyCode))
            }
            if (Mouse.isButtonDown(2)) {
                Mouse.setGrabbed(true)
                Globals.mc.inGameHasFocus = true
            } else {
                Mouse.setGrabbed(false)
                Globals.mc.inGameHasFocus = false
            }
        }
         */

    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (Globals.mc.player.isHandActive) {
            if (Globals.mc.player.getHeldItem(Globals.mc.player.activeHand).item is ItemShield) {
                if (Globals.mc.player.movementInput.moveStrafe != 0F || Globals.mc.player.movementInput.moveForward != 0F && Globals.mc.player.itemInUseMaxCount >= 8) {
                    Globals.mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Globals.mc.player.horizontalFacing))
                }
            }
        }

        if (inventoryWalk.value && !strictInventoryWalk.value || FreecamModule.isEnabled) {
            if (Globals.mc.currentScreen is GuiChat || Globals.mc.currentScreen is GuiEditSign || Globals.mc.currentScreen == null) {
                return
            }

            val keys = if (noSneak.value) KEYS else KEYS_SNEAK
            for (bind in keys) {
                KeyBinding.setKeyBindState(bind.keyCode, Keyboard.isKeyDown(bind.keyCode))
            }
        }

    }


    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {

        if (fullNullCheck()) return

        if (ncpStrict.value && event.packet is CPacketClickWindow) {

            if (event.stage == EventStageable.EventStage.PRE) {
                if (Globals.mc.player.isActiveItemStackBlocking) {
                    Globals.mc.playerController.onStoppedUsingItem(Globals.mc.player)
                }

                if (Globals.mc.player.isSneaking) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
                }

                if (Globals.mc.player.isSprinting) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
                }

            } else {
                if (Globals.mc.player.isSneaking) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))
                }

                if (Globals.mc.player.isSprinting) {
                    Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))
                }
            }
        }

    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            webTimer.reset()

            if (webMode.value == WebMode.Speed && webTimerSpeed.value > 0.0 && isPrevInWeb && Globals.mc.player.isInWeb) {
                isPrevInWeb = false
                TimerManager.resetTimer()
            }
        }
    }

    @Listener
    private fun onCollisionWebBB(event: BlockCollisionBoundingBoxEvent) {
        if (fullNullCheck() || webMode.value != WebMode.New) return

        if (event.pos.block == Blocks.WEB) {
            event.cancel()
            event.boundingBox = Block.FULL_BLOCK_AABB.contract(0.0, 0.25, 0.0)
        }
    }

    //@Listener
    //private fun onPlayerCollisionInWeb(event: CollisionInWebEvent) {
        // if (webMode.value == WebMode.Off && webMode.value != WebMode.New) event.cancel()
    //}

    @Listener
    private fun onPlayerSlowActiveHand(event: PlayerSlowEvent.ActiveHand) {
        if (items.value) event.cancel()
    }

    @Listener
    private fun onPlayerSlowAttack(event: PlayerSlowEvent.Attack) {
        if (items.value) event.cancel()
    }

    @Listener
    private fun onWalkOnSlime(event: WalkOnSlimeEvent) {
        if (slime.value) event.cancel()
    }

    @Listener
    private fun onLandOnSlime(event: LandOnSlimeEvent) {
        if (slime.value) event.cancel()
    }

    @Listener
    private fun onWalkBlockSoulSand(event: CollideSoulSandEvent) {
        if (soulSand.value) event.cancel()
    }

}
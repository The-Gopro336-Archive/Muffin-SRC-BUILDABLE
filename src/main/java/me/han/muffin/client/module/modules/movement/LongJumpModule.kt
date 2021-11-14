package me.han.muffin.client.module.modules.movement

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.JumpEvent
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.client.TravelEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.gui.font.util.Translate
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.misc.TimerModule
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

object  LongJumpModule: Module("LongJump", Category.MOVEMENT, "Jump farther than normally.") {

    private val mode = EnumValue(Mode.Normal, "Mode")
    private val disable = Value(true, "Disable")
    private val useTimer = Value(true, "UseTimer")
    private val farther = Value(false, "Farther")
    private val check = Value(true, "Check")

    //  private final Value<Boolean> autoSprint = new Value<>(false, "AutoSprint");
    private val horizontalSpeed = NumberValue(4.25f, 1f, 5f, 0.01f, "HorizontalSpeed")

    private var stage = 1
    private var moveSpeed = 0.1873
    private var timerDelay = 0
    private var lastDist = 0.0
    private var prevGround: Vec3d? = null
    private var ticksNeeded = 0
    private val timer = Timer()

    private var airTicks = 0
    private var groundTicks = 0
    private var isSpeeding = false

    private var disableGroundTicks = 0
    private var onGroundLastTick = false

    var height = Translate(0.0F, 0.0F)
    var speed = Translate(0.0F, 0.0F)

    private enum class Mode {
        Normal, Frog, Test, Test2
    }

    init {
        addSettings(mode, disable, useTimer, farther, check, horizontalSpeed)
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

    override fun onEnable() {
        //stage = if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0)).size > 0 || Globals.mc.player.collidedVertically) 1 else 4
        timer.reset()

        stage = 0
        moveSpeed = 0.0
        ticksNeeded = 0
        lastDist = 0.0

        onGroundLastTick = false
        timerDelay = 0

        height = Translate(0.0F, 0.0F)
        speed = Translate(0.0F, 0.0F)
    }

    override fun onDisable() {
        timer.reset()

        if (TimerModule.isDisabled) TimerManager.resetTimer()
        groundTicks = 0

        lastDist = 0.0

        Globals.mc.player.motionX *= 0.0
        Globals.mc.player.motionZ *= 0.0
        Globals.mc.player.jumpMovementFactor = 0f
    }

    @Listener
    private fun onJump(event: JumpEvent) {
        event.cancel()
    }


    fun updatePosition(x: Double, y: Double, z: Double) {
        Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(x, y, z, Globals.mc.player.onGround))
    }

    private fun getDistance(player: EntityPlayer, distance: Double): Double {
        val boundingBoxes = player.world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, -distance, 0.0))
        if (boundingBoxes.isEmpty()) {
            return 0.0
        }
        var y = 0.0
        for (boundingBox in boundingBoxes) {
            if (boundingBox.maxY <= y) continue
            y = boundingBox.maxY
        }
        return player.posY - y
    }

    @Listener
    private fun onMove(event: MoveEvent) {
        if (fullNullCheck()) return

        if (EntityUtil.isInWater(Globals.mc.player) || EntityUtil.isAboveWater(Globals.mc.player)) return

        if (Globals.mc.player.isOnLadder || Globals.mc.player.isEntityInsideOpaqueBlock) {
            moveSpeed = 0.0
            return
        }

        if (disable.value && timer.passed(4500.0)) {
            disable()
            timer.reset()
        }

        val reset = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0)).size > 0 && Globals.mc.player.onGround

        if (mode.value == Mode.Normal) {

            //   if (!Globals.mc.player.onGround) {
            //       moveSpeed = MovementUtils.getBaseMoveSpeed() / 2
            //   }

            ++timerDelay
            timerDelay %= 5

            if (timerDelay != 0) {
                if (useTimer.value) TimerManager.resetTimer()
            } else if (MovementUtils.isMoving()) {
                if (useTimer.value) TimerManager.setTimer(1.3F)
                event.x = (1.0199999809265137.let { Globals.mc.player.motionX *= it; Globals.mc.player.motionX })
                event.z = (1.0199999809265137.let { Globals.mc.player.motionZ *= it; Globals.mc.player.motionZ })
            }

            val block: Block

            when (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3)) {
                MathUtils.round(0.4, 3) -> {
                    Globals.mc.player.motionY = (0.31).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.71, 3) -> {
                    Globals.mc.player.motionY = (0.05).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.75, 3) -> {
                    event.y = (-0.5).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.55, 3) -> {
                    event.y = (-0.2).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.41, 3) -> {
                    event.y = (0.0).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.943, 3) -> {
                    event.y = (0.0).also { Globals.mc.player.motionY = it }
                }
                MathUtils.round(0.481, 3) -> {
                    if (check.value) {
                        block = BlockPos(Globals.mc.player.posX.toInt(), Globals.mc.player.posY.toInt() - 1, Globals.mc.player.posZ.toInt()).block
                        if (block !is BlockAir) {
                            event.y -= 0.075.also { Globals.mc.player.motionY -= it }
                        }
                    } else {
                        event.y -= 0.075.also { Globals.mc.player.motionY -= it }
                    }
                }
                MathUtils.round(0.40599999999999997, 3) -> {
                    if (check.value) {
                        block = BlockPos(Globals.mc.player.posX.toInt(), Globals.mc.player.posY.toInt() - 1, Globals.mc.player.posZ.toInt()).block
                        if (block !is BlockAir) {
                            event.y = (-0.1).also { Globals.mc.player.motionY = it }
                        }
                    } else {
                        event.y = (-0.1).also { Globals.mc.player.motionY = it }
                    }
                }
                MathUtils.round(0.306, 3) -> {
                    if (!Globals.mc.player.collidedHorizontally && stage != 0) {
                        if (check.value) {
                            block = BlockPos(Globals.mc.player.posX.toInt(), Globals.mc.player.posY.toInt() - 1, Globals.mc.player.posZ.toInt()).block
                            if (block !is BlockAir || Globals.mc.player.moveForward == 0.0F && Globals.mc.player.moveStrafing == 0.0F) {
                                event.y = (-6.0E-6).also { Globals.mc.player.motionY = it }
                            }
                        } else {
                            event.y = (-6.0E-6).also { Globals.mc.player.motionY = it }
                        }
                    }
                }
                MathUtils.round(0.305, 3) -> {
                    stage = 1
                }
                MathUtils.round(0.138, 3) -> {
                    Globals.mc.player.motionY -= 0.08
                    event.y -= 0.09316090325960147
                    Globals.mc.player.posY -= 0.09316090325960147
                }
            }


            if (Globals.mc.player.moveForward == 0.0f && Globals.mc.player.moveStrafing == 0.0f || Globals.mc.player.collidedHorizontally) {
                stage = 1
            }

                if (!MovementUtils.isMoving()) {
                    stage = 0
                    return
                }

                if (ticksNeeded > 0) {
                    if (Globals.mc.player.collidedVertically && Globals.mc.player.onGround) {
                        ticksNeeded--
                    }
                    return
                }

            /*
                        when (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3)) {
                            MathUtils.round(0.41, 3) -> {
                                event.y = (0.0).also { Globals.mc.player.motionY = it }
                            }
                            MathUtils.round(0.943, 3) -> {
                                event.y -= (0.03).also { Globals.mc.player.motionY -= it }
                            }
                        }
             */

            if (stage != 1 || !MovementUtils.isMoving()) {
                if (stage == 2) {
                    ++stage
                    lastDist = 0.0
                    if (MovementUtils.isMoving() && (Globals.mc.player.onGround || Globals.mc.player.capabilities.isFlying)) {
                        val height = MovementUtils.getJumpBoostModifier(0.4025)
                        Globals.mc.player.motionY = height
                        event.y = Globals.mc.player.motionY
                        moveSpeed *= 2.149802
                    }
                } else if (stage == 3) {
                    ++stage
                    var boost = 0.66
                    if (!farther.value) boost = 0.763
                    val difference = boost * (lastDist - MovementUtils.getBaseMoveSpeed())
                    moveSpeed = lastDist - difference
                } else {
                    val list = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                    val list2 = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, -0.4, 0.0))

                    if (!(Globals.mc.player.collidedVertically || list.size <= 0 && list2.size <= 0)) {
                        Globals.mc.player.motionY = -1.0E-4
                        event.y = Globals.mc.player.motionY
                    }

                    if (Globals.mc.player.motionY < 0.1) Globals.mc.player.motionY -= 0.005
                    if (event.y < 0.0) event.y *= 0.5

                    if (abs(event.x) < 0.003) event.x = 0.0
                    if (abs(event.y) < 0.003) event.y = 0.0
                    if (abs(event.z) < 0.003) event.z = 0.0

                    if (Globals.mc.player.motionY < 0.0 && Globals.mc.player.fallDistance <= 0) Globals.mc.player.motionY *= 0.40

                    if (list.size > 0 || Globals.mc.player.collidedVertically) {
                        if (stage == 4) toggle()
                        stage = if (MovementUtils.isMoving()) 1 else 0
                    }

                    moveSpeed = lastDist - lastDist / 159.0
                }
            } else {
                stage = 2
                lastDist = 0.0
                event.x = 0.0.also { Globals.mc.player.motionX = it }
                event.z = 0.0.also { Globals.mc.player.motionZ = it }
                val boost = if (Globals.mc.player.isPotionActive(MobEffects.SPEED)) horizontalSpeed.value else horizontalSpeed.value + 1.1F
                moveSpeed = boost * MovementUtils.getBaseMoveSpeed() - 0.01
            }


            /*
            if (stage != 1 || !MovementUtils.isMoving()) {
                when (stage) {
                    0 -> {
                        ++stage
                        lastDist = 0.0
                    }
                    2 -> {
                        var height = 0.4025
                        if (MovementUtils.isMoving() && Globals.mc.player.onGround) {
                            if (Globals.mc.player.isPotionActive(MobEffects.JUMP_BOOST)) height = MovementUtils.getJumpBoostModifier(height)
                            event.y = height.also { Globals.mc.player.motionY = it }
                            moveSpeed *= 2.1489999294281006
                        }
                    }
                    3 -> {
                        var boost = 0.66
                        if (!farther.value) boost = 0.763
                        val difference = boost * (lastDist - MovementUtils.getBaseMoveSpeed())
                        moveSpeed = lastDist - difference
                    }
                    else -> {
                        val list = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                        val list2 = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, -0.4, 0.0))
                        if (list.size > 0 || Globals.mc.player.collidedVertically) {
                            if (stage == 4) toggle()
                            stage = if (MovementUtils.isMoving()) 1 else 0
                        }
                        if (!(Globals.mc.player.collidedVertically || list.size <= 0 && list2.size <= 0)) {
                            event.y = (-1.0E-4).also { Globals.mc.player.motionY = it }
                        }
                        moveSpeed = lastDist - lastDist / 159.0
                    }
                }
            } else {
                val boost = if (Globals.mc.player.isPotionActive(MobEffects.SPEED)) horizontalSpeed.value else horizontalSpeed.value + 1.1F
                moveSpeed = boost * MovementUtils.getBaseMoveSpeed() - 0.01
            }
             */

            /*
            if (stage == 1 && (Globals.mc.player.moveForward != 0.0f || Globals.mc.player.moveStrafing != 0.0f) && Globals.mc.player.collidedVertically) {
                stage = 2
                lastDist = 0.0
                prevGround = Globals.mc.player.positionVector
                moveSpeed = horizontalSpeed.value * MovementUtils.getBaseMoveSpeed() - 0.05
            } else if (stage == 2) {
                stage = 3
                event.x = 0.0
                event.z = 0.0
                Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY - 0.001, Globals.mc.player.posZ)
                Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.001, Globals.mc.player.posZ)
                //  Globals.mc.player.motionY = 0.399 //0.4025
                event.y = 0.4025.also { Globals.mc.player.motionY = it}
                moveSpeed *= 2.1489999294281006
            } else if (stage == 3) {
                stage = 4
                var boost = 0.66
                if (!farther.value) boost = 0.763
                val difference = boost * (lastDist - MovementUtils.getBaseMoveSpeed())
                moveSpeed = lastDist - difference
            } else if (stage == 4) {
                moveSpeed = lastDist - lastDist / 159.0

                val list = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0))
                val list2 = Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, -0.4, 0.0))

                if (list.size > 0 || Globals.mc.player.collidedVertically) {
                    prevGround = null
                    stage = 1
                }

                if (prevGround != null && prevGround!!.distanceTo(Globals.mc.player.positionVector) > 12.0) {
                    event.x = (0.0).also { Globals.mc.player.motionX = it }
                    event.z = (0.0).also { Globals.mc.player.motionZ = it }
                    return
                }

                if (!(Globals.mc.player.collidedVertically || list.size <= 0 && list2.size <= 0)) {
                    event.y = (-1.0E-4).also { Globals.mc.player.motionY = it }
                }

                if (Globals.mc.player.motionY < 0.1) {
                    Globals.mc.player.motionY -= 0.005
                }

                if (event.y < 0.0) {
                    event.y *= 0.67
                }

                if (Globals.mc.player.motionY < 0.0 && Globals.mc.player.fallDistance <= 0) Globals.mc.player.motionY *= 0.40

            }
 */

            onGroundLastTick = Globals.mc.player.onGround
            moveSpeed = max(moveSpeed, MovementUtils.getBaseMoveSpeed())

//            updatePosition(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)
//            updatePosition(Globals.mc.player.posX + Globals.mc.player.motionX, 0.0, Globals.mc.player.posZ + Globals.mc.player.motionZ)

            MovementUtils.setMoveMotionNew(event, moveSpeed)
        //    ++stage

            if (Globals.mc.player.fallDistance > 1.0) {
                event.x = (0.0).also { Globals.mc.player.motionX = it }
                event.z = (0.0).also { Globals.mc.player.motionZ = it }
            }

        }

        if (mode.value == Mode.Test) {

            if (Globals.mc.player.moveForward <= 0.0F && Globals.mc.player.moveStrafing <= 0.0F) stage = 1

            if (MathUtils.round(Globals.mc.player.posY - Globals.mc.player.posY.toInt().toDouble(), 3) == MathUtils.round(0.943, 3)) {
                event.y -= 0.03.also { Globals.mc.player.motionY -= it }
            }

            if (stage == 1 && (Globals.mc.player.moveForward != 0.0F && Globals.mc.player.moveStrafing != 0.0F)) {
                stage = 2
                moveSpeed = horizontalSpeed.value * MovementUtils.getBaseMoveSpeed() - 0.01
            } else if (stage == 2) {
                var height = 0.424
                if (Globals.mc.player.isPotionActive(MobEffects.JUMP_BOOST)) height = MovementUtils.getJumpBoostModifier(height)
                event.y = height.also { Globals.mc.player.motionY = it }
                stage = 3
                moveSpeed *= 2.149802
            } else if (stage == 3) {
                var boost = 0.66
                if (!farther.value) boost = 0.763
                val difference = boost * (lastDist - MovementUtils.getBaseMoveSpeed())
                moveSpeed = lastDist - difference
            } else {
                if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, Globals.mc.player.entityBoundingBox.offset(0.0, Globals.mc.player.motionY, 0.0)).size > 0 || Globals.mc.player.collidedVertically) {
                    if (stage == 4) toggle()
                    stage = 1
                }
                moveSpeed = lastDist - lastDist / 159.0
            }

            moveSpeed = max(moveSpeed, MovementUtils.getBaseMoveSpeed())
            MovementUtils.setMoveMotionNew(event, moveSpeed)
        }

        if (mode.value == Mode.Test2) {
            if (stage == 1) {
                moveSpeed = 0.0
            } else if (stage == 2) {
                event.y = MovementUtils.getJumpBoostModifier(0.41999998688697815).also { Globals.mc.player.motionY = it }
                moveSpeed = horizontalSpeed.value * MovementUtils.getBaseMoveSpeed() - 0.01
            } else if (stage == 3) {
                moveSpeed = 2.14999 * MovementUtils.getBaseMoveSpeed()
            } else if (stage == 4) {
                moveSpeed *= 1.22
            } else {
                if (stage < 15) {
                    if (Globals.mc.player.motionY < 0) {
                        event.y *= 0.7225.also { Globals.mc.player.motionY *= it }
                    }
                    moveSpeed = lastDist - lastDist / 159.0
                } else {
                    moveSpeed *= .75
                }
            }
            MovementUtils.setMoveMotionOld(event, max(moveSpeed, MovementUtils.getBaseMoveSpeed()))
            stage++
        }


    }

    @Listener
    private fun onTravel(event: TravelEvent) {
        if (fullNullCheck()) return

        if (EntityUtil.isInWater(Globals.mc.player) || EntityUtil.isAboveWater(Globals.mc.player)) return

        if (mode.value == Mode.Frog) {

            if (Keyboard.isKeyDown(50)) {
                updatePosition(0.0, 2.147483647E9, 0.0)
            }

            if (!MovementUtils.isKeyDown()) return

            if (!Globals.mc.player.collidedVertically) {
                airTicks++
                isSpeeding = true
                if (Globals.mc.gameSettings.keyBindSneak.isKeyDown) {
                    Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(0.0, 2.147483647E9, 0.0, false))
                }

                groundTicks = 0
                if (!Globals.mc.player.collidedVertically) {
                    /*
                    if (Globals.mc.player.motionY == -0.07190068807140403) {
                        Globals.mc.player.motionY *= 0.3499999940395355;
                    }
                    if (Globals.mc.player.motionY == -0.10306193759436909) {
                        Globals.mc.player.motionY *= 0.550000011920929;
                    }
                    if (Globals.mc.player.motionY == -0.13395038817442878) {
                        Globals.mc.player.motionY *= 0.6700000166893005;
                    }
                    if (Globals.mc.player.motionY == -0.16635183030382) {
                        Globals.mc.player.motionY *= 0.6899999976158142;
                    }
                    if (Globals.mc.player.motionY == -0.19088711097794803) {
                        Globals.mc.player.motionY *= 0.7099999785423279;
                    }
                    if (Globals.mc.player.motionY == -0.21121925191528862) {
                        Globals.mc.player.motionY *= 0.20000000298023224;
                    }
                    if (Globals.mc.player.motionY == -0.11979897632390576) {
                        Globals.mc.player.motionY *= 0.9300000071525574;
                    }
                    if (Globals.mc.player.motionY == -0.18758479151225355) {
                        Globals.mc.player.motionY *= 0.7200000286102295;
                    }
                    if (Globals.mc.player.motionY == -0.21075983825251726) {
                        Globals.mc.player.motionY *= 0.7599999904632568;
                    }
                     */

                    when (Globals.mc.player.motionY) {
                        -0.07190068807140403 -> Globals.mc.player.motionY *= 0.3499999940395355
                        -0.10306193759436909 -> Globals.mc.player.motionY *= 0.550000011920929
                        -0.13395038817442878 -> Globals.mc.player.motionY *= 0.6700000166893005
                        -0.16635183030382 -> Globals.mc.player.motionY *= 0.6899999976158142
                        -0.19088711097794803 -> Globals.mc.player.motionY *= 0.7099999785423279
                        -0.21121925191528862 -> Globals.mc.player.motionY *= 0.20000000298023224
                        -0.11979897632390576 -> Globals.mc.player.motionY *= 0.9300000071525574
                        -0.18758479151225355 -> Globals.mc.player.motionY *= 0.7200000286102295
                        -0.21075983825251726 -> Globals.mc.player.motionY *= 0.7599999904632568
                    }
                    if (getDistance(Globals.mc.player, 69.0) < 0.5 && !BlockPos(Globals.mc.player.posX, Globals.mc.player.posY - 0.32, Globals.mc.player.posZ).state.isFullCube) {
                        when (Globals.mc.player.motionY) {
                            -0.23537393014173347 -> Globals.mc.player.motionY *= 0.029999999329447746
                            -0.08531999505205401 -> Globals.mc.player.motionY *= -0.5
                            -0.03659320313669756 -> Globals.mc.player.motionY *= -0.10000000149011612
                            -0.07481386749524899 -> Globals.mc.player.motionY *= -0.07000000029802322
                            -0.0732677700939672 -> Globals.mc.player.motionY *= -0.05000000074505806
                            -0.07480988066790395 -> Globals.mc.player.motionY *= -0.03999999910593033
                            -0.0784000015258789 -> Globals.mc.player.motionY *= 0.10000000149011612
                            -0.08608320193943977 -> Globals.mc.player.motionY *= 0.10000000149011612
                            -0.08683615560584318 -> Globals.mc.player.motionY *= 0.05000000074505806
                            -0.08265497329678266 -> Globals.mc.player.motionY *= 0.05000000074505806
                            -0.08245009535659828 -> Globals.mc.player.motionY *= 0.05000000074505806
                            -0.08244005633718426 -> Globals.mc.player.motionY *= -0.08243956442521608
                            -0.08243956442521608 -> Globals.mc.player.motionY *= -0.08244005590677261
                        }
                    } else {
                        if (Globals.mc.player.motionY < -0.2 && Globals.mc.player.motionY > -0.24) {
                            Globals.mc.player.motionY *= 0.7
                        }
                        if (Globals.mc.player.motionY < -0.25 && Globals.mc.player.motionY > -0.32) {
                            Globals.mc.player.motionY *= 0.8
                        }
                        if (Globals.mc.player.motionY < -0.35 && Globals.mc.player.motionY > -0.8) {
                            Globals.mc.player.motionY *= 0.98
                        }
                        if (Globals.mc.player.motionY < -0.8 && Globals.mc.player.motionY > -1.6) {
                            Globals.mc.player.motionY *= 0.99
                        }
                    }
                }

                TimerManager.setTimer(0.85F)
                val speedVals = doubleArrayOf(0.420606, 0.417924, 0.415258, 0.412609, 0.409977, 0.407361, 0.404761, 0.402178, 0.399611, 0.39706, 0.394525, 0.392, 0.3894, 0.38644, 0.383655, 0.381105, 0.37867, 0.37625, 0.37384, 0.37145, 0.369, 0.3666, 0.3642, 0.3618, 0.35945, 0.357, 0.354, 0.351, 0.348, 0.345, 0.342, 0.339, 0.336, 0.333, 0.33, 0.327, 0.324, 0.321, 0.318, 0.315, 0.312, 0.309, 0.307, 0.305, 0.303, 0.3, 0.297, 0.295, 0.293, 0.291, 0.289, 0.287, 0.285, 0.283, 0.281, 0.279, 0.277, 0.275, 0.273, 0.271, 0.269, 0.267, 0.265, 0.263, 0.261, 0.259, 0.257, 0.255, 0.253, 0.251, 0.249, 0.247, 0.245, 0.243, 0.241, 0.239, 0.237)
                if (Globals.mc.gameSettings.keyBindForward.isKeyDown) {
                    try {
                        MovementUtils.setSpeed(speedVals[airTicks - 1] * horizontalSpeed.value)
                        //    Globals.mc.player.motionX = xDir * speedVals[airTicks - 1] * 3.0 * MovementUtils.getBaseMoveSpeed()
                        //    Globals.mc.player.motionZ = zDir * speedVals[airTicks - 1] * 3.0 * MovementUtils.getBaseMoveSpeed()
                    } catch (e: Exception) {
                    }
                } else {
                    Globals.mc.player.motionX = 0.0
                    Globals.mc.player.motionZ = 0.0
                }
            } else {
                TimerManager.resetTimer()
                airTicks = 0
                groundTicks++
                Globals.mc.player.motionX /= 13.0
                Globals.mc.player.motionZ /= 13.0

                if (groundTicks == 1) {
                    updatePosition(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)
                    updatePosition(Globals.mc.player.posX + 0.0624, Globals.mc.player.posY, Globals.mc.player.posZ)
                    updatePosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.419, Globals.mc.player.posZ)
                    updatePosition(Globals.mc.player.posX + 0.0624, Globals.mc.player.posY, Globals.mc.player.posZ)
                    updatePosition(Globals.mc.player.posX, Globals.mc.player.posY + 0.419, Globals.mc.player.posZ)
                }

                if (groundTicks > 2) {
                    groundTicks = 0
                    MovementUtils.setSpeed(0.3)
                    //    Globals.mc.player.motionX = xDir * 0.3
                    //    Globals.mc.player.motionZ = zDir * 0.3
                    Globals.mc.player.motionY = 0.42399999499320984
                }
            }
        }
    }

    private var jumped = false

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        if (event.stage != EventStageable.EventStage.PRE) return

        /*
        if (Globals.mc.player.onGround && Globals.mc.player.collidedVertically) {
            event.location.y += 0.0007435
        }


        if (stage > 1) {
            Globals.mc.player.motionY = 0.0
            if (Globals.mc.player.ticksExisted % 2 == 0) {
                event.location.y += 0.0000023
            } else {
                event.location.y -= 0.0000023
            }
        }
 */

        lastDist = hypot(Globals.mc.player.posX - Globals.mc.player.prevPosX, Globals.mc.player.posZ - Globals.mc.player.prevPosZ)
        if (mode.value == Mode.Test2 && Globals.mc.player.collidedVertically && Globals.mc.player.onGround) {
            if (stage < 2) {
                event.location.y = Globals.mc.player.posY + 1.8995E-35
            }
            if (stage > 2 && MovementUtils.isMoving()) {
                toggle()
            }
        }
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is SPacketPlayerPosLook) {
            TimerManager.resetTimer()

            moveSpeed = MovementUtils.getBaseMoveSpeed()
            lastDist = 0.0
            timerDelay = 0
            groundTicks = 0
            stage = -4

            Globals.mc.player.motionX *= 0.0
            Globals.mc.player.motionZ *= 0.0
            Globals.mc.player.jumpMovementFactor = 0f

            disable()
        }

    }

    fun getMotion(stage: Int): Double {
        var currentStage = stage
        val isMoving = Globals.mc.player.moveStrafing != 0f || Globals.mc.player.moveForward != 0f
        val mot = doubleArrayOf(0.396, -0.122, -0.1, 0.423, 0.35, 0.28, 0.217, 0.15, 0.025, -0.00625, -0.038, -0.0693, -0.102, -0.13, -0.018, -0.1, -0.117, -0.14532, -0.1334, -0.1581, -0.183141, -0.170695, -0.195653, -0.221, -0.209, -0.233, -0.25767, -0.314917, -0.371019, -0.426)
        currentStage--
        return if (currentStage >= 0 && currentStage < mot.size) {
            mot[currentStage]
        } else {
            Globals.mc.player.motionY
        }
    }

}
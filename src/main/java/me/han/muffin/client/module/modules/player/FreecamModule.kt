package me.han.muffin.client.module.modules.player

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.AttackEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.entity.MouseOverEntityEvent
import me.han.muffin.client.event.events.movement.PlayerUpdateMoveStateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.network.ServerEvent
import me.han.muffin.client.event.events.render.ComputeVisibilityEvent
import me.han.muffin.client.event.events.render.OrientCameraPreEvent
import me.han.muffin.client.event.events.render.RenderHandEvent
import me.han.muffin.client.event.events.render.ShouldSetupTerrainEvent
import me.han.muffin.client.event.events.render.entity.GetMouseOverPostEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityEvent
import me.han.muffin.client.event.events.world.SetOpaqueCubeEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils.resetJumpSneak
import me.han.muffin.client.utils.entity.MovementUtils.resetMove
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.threading.onMainThread
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.MoverType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketInput
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketRespawn
import net.minecraft.util.MovementInput
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal object FreecamModule: Module("Freecam", Category.PLAYER, "Ability to take a look from another perspective.") {

    private val interactMode = EnumValue(InteractMode.Camera, "InteractMode")
    private val horizontalSpeed = NumberValue(2.0F, 0.1F, 5.0F, 0.1F, "HorizontalSpeed")
    private val verticalSpeed =  NumberValue(2.0F, 0.1F, 5.0F, 0.1F, "VerticalSpeed")
    private val renderHand = Value(false, "RenderHand")
    private val playerMove = Value(true, "PlayerMove")
    private val rotateCrosshair = Value(true, "RotateCrosshair")
    private val followCamera = Value(false, "FollowCamera")
    private val cancelPackets = Value(true, "CancelPackets")

    private var prevThirdPersonViewSetting = -1
    var cameraGuy: EntityPlayer? = null; private set
    var resetInput = false

    private const val ENTITY_ID = -6958

    enum class InteractMode {
        Camera, Player
    }

    init {
        addSettings(interactMode, horizontalSpeed, verticalSpeed, renderHand, playerMove, rotateCrosshair, followCamera, cancelPackets)
    }

    override fun onEnable() {
        Globals.mc.renderChunksMany = false
    }

    override fun onDisable() {
        if (Globals.mc.player == null) return

        Globals.mc.renderChunksMany = true
        resetCameraGuy()
        resetMovementInput(Globals.mc.player?.movementInput)
    }

    @Listener
    private fun onAttack(event: AttackEvent) {
        if (event.entity == Globals.mc.player) event.cancel()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (event.getPacket() is SPacketRespawn) toggle()
    }

    @Listener
    private fun onMouseOverEntity(event: MouseOverEntityEvent) {
        if (fullNullCheck()) return
        if (interactMode.value == InteractMode.Player || Keyboard.isKeyDown(Keyboard.KEY_LMENU)) event.entity = Globals.mc.player
        else event.entity = cameraGuy ?: Globals.mc.renderViewEntity ?: Globals.mc.player
    }

    @Listener
    private fun onTrace(event: OrientCameraPreEvent) {
        if (fullNullCheck()) return
        event.shouldIgnoreTrace = true
    }

    @Listener
    private fun onGetMouseOverPost(event: GetMouseOverPostEvent) {
        if (fullNullCheck()) return
        val result = event.result
        if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY && result.entityHit == Globals.mc.player) {
            event.result = RayTraceResult(RayTraceResult.Type.MISS, result.hitVec, null, result.hitVec.toBlockPos())
        }
    }

    @Listener
    private fun onDisconnect(event: ServerEvent.Disconnect) {
        prevThirdPersonViewSetting = -1
        disable()
    }

    @Listener
    private fun onShouldSetupTerrain(event: ShouldSetupTerrainEvent) {
        event.cancel()
    }

    @Listener
    private fun onComputeVisibility(event: ComputeVisibilityEvent) {
        event.cancel()
    }

    @Listener
    private fun onSetOpaqueBlock(event: SetOpaqueCubeEvent) {
        event.cancel()
    }

    @Listener
    private fun onRenderHand(event: RenderHandEvent) {
        if (!renderHand.value) event.cancel()
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (fullNullCheck()) return

        if (event.packet is CPacketUseEntity) {
            // Don't interact with self
            if (event.packet.getEntityFromWorld(Globals.mc.world) == Globals.mc.player) event.cancel()
        }

        if (cancelPackets.value) {
            if (event.packet is CPacketPlayer || event.packet is CPacketInput || event.packet is CPacketEntityAction) {
                event.cancel()
            }
        }

    }

    //@Listener
    //private fun onKeyInput(event: KeyPressedEvent) {
    //    if (Globals.mc.world == null || Globals.mc.player == null) return
        // Force it to stay in first person lol
 //       if (event.key == Globals.mc.gameSettings.keyBindTogglePerspective.keyCode) Globals.mc.gameSettings.thirdPersonView = 2
    //}

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.POST) return

        if (fullNullCheck()) return

        if (Globals.mc.player.isDead || Globals.mc.player.health <= 0.0f) {
            if (cameraGuy != null) resetCameraGuy()
            return
        }

        if (cameraGuy == null && Globals.mc.player.ticksExisted > 5) spawnCameraGuy()
    }

    @Listener
    private fun onPlayerUpdateMoveState(event: PlayerUpdateMoveStateEvent) {
        if (event.movementInput !is MovementInputFromOptions) return

        resetMovementInput(event.movementInput)
        if (rotateCrosshair.value) updatePlayerRotation()
        if (playerMove.value) updatePlayerMovement()
        if (followCamera.value) updatePlayerRotationToCamera()
    }

    @Listener
    private fun onRenderEntity(event: RenderEntityEvent) {
        cameraGuy?.let {
            if (event.entity == it) event.cancel()
        }
    }

    private fun resetMovementInput(movementInput: MovementInput?) {
        if (movementInput is MovementInputFromOptions) {
            movementInput.resetMove()
            movementInput.resetJumpSneak()
        }
    }

    private fun spawnCameraGuy() {
        // Create a cloned player
        cameraGuy = FakeCamera(Globals.mc.player).also {
            // Add it to the world
            Globals.mc.world?.addEntityToWorld(ENTITY_ID, it)

            // Set the render view entity to our camera guy
            Globals.mc.renderViewEntity = it

            // Reset player movement input
            resetInput = true
            resetMovementInput(Globals.mc.player?.movementInput)

            // Stores prev third person view setting
            prevThirdPersonViewSetting = Globals.mc.gameSettings.thirdPersonView
            Globals.mc.gameSettings.thirdPersonView = 0
        }
    }

    private fun resetCameraGuy() {
        cameraGuy = null
        onMainThread {
            if (Globals.mc.player == null) return@onMainThread
            Globals.mc.world.removeEntityFromWorld(ENTITY_ID)
            Globals.mc.renderViewEntity = Globals.mc.player
            if (prevThirdPersonViewSetting != -1) Globals.mc.gameSettings.thirdPersonView = prevThirdPersonViewSetting
        }
    }

    private fun updatePlayerRotationToCamera() {
        cameraGuy?.let {
            val rotation = RotationUtils.getRotationToEntityClosest(it)
            Globals.mc.player.rotationYaw = rotation.x
            Globals.mc.player.rotationPitch = rotation.y
        }
    }

    private fun updatePlayerRotation() {
        Globals.mc.objectMouseOver?.let {
            val hitVec = it.hitVec
            if (it.typeOfHit == RayTraceResult.Type.MISS || hitVec == null) return
            val rotation = RotationUtils.getRotationTo(hitVec)
            Globals.mc.player?.apply {
                rotationYaw = rotation.x
                rotationPitch = rotation.y
            }
        }
    }

    private fun updatePlayerMovement() {
        Globals.mc.player?.let { player ->
            cameraGuy?.let {

                if (!Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
                    resetMovementInput(player.movementInput)
                    return
                }

                val forwardCode = Globals.mc.gameSettings.keyBindForward.keyCode to Globals.mc.gameSettings.keyBindBack.keyCode
                val strafeCode = Globals.mc.gameSettings.keyBindLeft.keyCode to Globals.mc.gameSettings.keyBindRight.keyCode

                val jumpCode = Globals.mc.gameSettings.keyBindJump.keyCode
                val sneakCode = Globals.mc.gameSettings.keyBindSneak.keyCode

                val arrowUpDownCode = Keyboard.KEY_UP to Keyboard.KEY_DOWN
                val arrowLeftRightCode = Keyboard.KEY_LEFT to Keyboard.KEY_RIGHT

                val forward = Keyboard.isKeyDown(forwardCode.first) to Keyboard.isKeyDown(forwardCode.second)
                val strafe = Keyboard.isKeyDown(strafeCode.first) to Keyboard.isKeyDown(strafeCode.second)

                val movementInput = calcMovementInput(forward, strafe, false to false)

                player.movementInput?.apply {
                    moveForward = movementInput.first
                    moveStrafe = -movementInput.second

                    forwardKeyDown = forward.first
                    backKeyDown = forward.second
                    leftKeyDown = strafe.first
                    rightKeyDown = strafe.second

                    jump = Keyboard.isKeyDown(jumpCode)
                    sneak = Keyboard.isKeyDown(sneakCode)
                }

                if (Keyboard.isKeyDown(arrowUpDownCode.second)) {
                    player.rotationPitch += 5
                    if (player.rotationPitch > 90F) player.rotationPitch = 90F
                }
                if (Keyboard.isKeyDown(arrowUpDownCode.first)) {
                    player.rotationPitch -= 5
                    if (player.rotationPitch < -90F) player.rotationPitch = -90F
                }
                if (Keyboard.isKeyDown(arrowLeftRightCode.first)) {
                    player.rotationYaw -= 15
                }
                if (Keyboard.isKeyDown(arrowLeftRightCode.second)) {
                    player.rotationYaw += 15
                }

            }
        }
    }

    private class FakeCamera(val player: EntityPlayerSP): EntityOtherPlayerMP(Globals.mc.world, Globals.mc.player.gameProfile) { // Globals.mc.session.profile) {
        // private var isInsideMaterial = false

        init {
            copyLocationAndAnglesFrom(player)
            capabilities.allowFlying = true
            capabilities.isFlying = true
        }

        /*
        override fun isInsideOfMaterial(materialIn: Material): Boolean {
            if (isInsideMaterial) {
                isInsideMaterial = false
                return false
            }

            isInsideMaterial = true
            val isLocalPlayerInside = Globals.mc.player.isInsideOfMaterial(materialIn)
            isInsideMaterial = false

            return isLocalPlayerInside
        }

        override fun getActivePotionMap(): MutableMap<Potion, PotionEffect> {
            return Globals.mc.player.activePotionMap
        }

        override fun getActivePotionEffects(): MutableCollection<PotionEffect> {
            return Globals.mc.player.activePotionEffects
        }

        override fun getFoodStats(): FoodStats {
            return Globals.mc.player.foodStats
        }

        override fun getTotalArmorValue(): Int {
            return Globals.mc.player.totalArmorValue
        }

        override fun isPotionActive(potionIn: Potion): Boolean {
            return Globals.mc.player.isPotionActive(potionIn)
        }
         */

        override fun onLivingUpdate() {
            health = player.health
            absorptionAmount = player.absorptionAmount

            // Update inventory
            inventory.copyInventory(player.inventory)

            // Update yaw head
            updateEntityActionState()

           // resetPositionToBB()

            val horizontalSpeedMultiPlied = horizontalSpeed.value * 10
            val verticalSpeedMultiPlied = verticalSpeed.value * 10

            if (playerMove.value && Keyboard.isKeyDown(Keyboard.KEY_LMENU)) return

            // We have to update movement input from key binds because mc.player.movementInput is used by Baritone
            val forward = Globals.mc.gameSettings.keyBindForward.isKeyDown to Globals.mc.gameSettings.keyBindBack.isKeyDown
            val strafe = Globals.mc.gameSettings.keyBindLeft.isKeyDown to Globals.mc.gameSettings.keyBindRight.isKeyDown
            val vertical = Globals.mc.gameSettings.keyBindJump.isKeyDown to Globals.mc.gameSettings.keyBindSneak.isKeyDown
            val movementInput = calcMovementInput(forward, strafe, vertical)

            moveForward = movementInput.first
            moveStrafing = movementInput.second
            moveVertical = movementInput.third

            // Update sprinting
            isSprinting = Globals.mc.gameSettings.keyBindSprint.isKeyDown

            val yawRad = (rotationYaw - RotationUtils.getRotationFromVec(Vec3d(moveStrafing.toDouble(), 0.0, moveForward.toDouble())).x).toDouble().toRadian()
            val speed = (horizontalSpeedMultiPlied / 20f) * min(abs(moveForward) + abs(moveStrafing), 1f)

            motionX = -sin(yawRad) * speed
            motionY = moveVertical.toDouble() * (verticalSpeedMultiPlied / 20f)
            motionZ = cos(yawRad) * speed

            if (isSprinting) {
                motionX *= 1.5
                motionY *= 1.5
                motionZ *= 1.5
            }

            noClip = true

            move(MoverType.SELF, motionX, motionY, motionZ)
        }

        override fun getEyeHeight() = 1.65f
    }

    /**
     * @param forward <Forward, Backward>
     * @param strafe <Left, Right>
     * @param vertical <Up, Down>
     *
     * @return <Forward, Strafe, Vertical>
     */
    private fun calcMovementInput(forward: Pair<Boolean, Boolean>, strafe: Pair<Boolean, Boolean>, vertical: Pair<Boolean, Boolean>): Triple<Float, Float, Float> {
        // Forward movement input
        val moveForward = if (forward.first xor forward.second) {
            if (forward.first) 1f else -1f
        } else {
            0f
        }

        // Strafe movement input
        val moveStrafing = if (strafe.first xor strafe.second) {
            if (strafe.second) 1f else -1f
        } else {
            0f
        }

        // Vertical movement input
        val moveVertical = if (vertical.first xor vertical.second) {
            if (vertical.first) 1f else -1f
        } else {
            0f
        }

        return Triple(moveForward, moveStrafing, moveVertical)
    }

}
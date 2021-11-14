package me.han.muffin.client.module.modules.hidden

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.client.MoveEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.ComputeVisibilityEvent
import me.han.muffin.client.event.events.render.SetupTerrainEvent
import me.han.muffin.client.event.events.render.ShouldSetupTerrainEvent
import me.han.muffin.client.event.events.world.SetOpaqueCubeEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.value.NumberValue
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object FreecamDupeModule: Module("FreecamDupe", Category.PLAYER, "Freecam allow you to dupe with entity desync.") {
    private val horizontalSpeed = NumberValue(1.5, 0.1, 5.0, 0.1, "HorizontalSpeed")
    private val verticalSpeed = NumberValue(1.0, 0.1, 5.0, 0.1, "VerticalSpeed")

    private var prevRiding = false
    private var prevPos: Vec3d? = null
    private var prevRotation: Vec2f? = null

    private var ridingEntity: Entity? = null
    private var clonedEntity: ClonedPlayer? = null

    init {
        addSettings(horizontalSpeed, verticalSpeed)
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        prevRiding = Globals.mc.player.ridingEntity != null
        if (Globals.mc.player.ridingEntity == null) {
            prevPos = Globals.mc.player.positionVector
        } else {
            ridingEntity = Globals.mc.player.ridingEntity
            Globals.mc.player.dismountRidingEntity()
        }

        prevRotation = Vec2f(Globals.mc.player)
        clonedEntity = ClonedPlayer()
        clonedEntity?.addEntityToWorld()
        Globals.mc.player.noClip = true
    }

    override fun onDisable() {
        if (Globals.mc.player == null || prevPos == null || prevRotation == null) return

        Globals.mc.player.setPositionAndRotation(prevPos!!.x, prevPos!!.y, prevPos!!.z, prevRotation!!.x, prevRotation!!.y)
        Globals.mc.world.removeEntityFromWorld(-69102)
        clonedEntity = null
        prevPos = null
        prevRotation = null

        Globals.mc.player.noClip = false
        Globals.mc.player.setVelocity(0.0, 0.0, 0.0)

        if (prevRiding && ridingEntity != null) {
            Globals.mc.player.startRiding(ridingEntity!!, true)
            ridingEntity = null
        }
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (!Globals.mc.player.onGround) Globals.mc.player.motionY = -0.2

        if (Globals.mc.gameSettings.keyBindJump.isKeyDown) Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY + verticalSpeed.value, Globals.mc.player.posZ)
        if (Globals.mc.player.isSneaking) Globals.mc.player.setPosition(Globals.mc.player.posX, Globals.mc.player.posY - verticalSpeed.value, Globals.mc.player.posZ)

        if (MovementUtils.isMoving()) {
            MovementUtils.setSpeed(horizontalSpeed.value)
        } else {
            Globals.mc.player.motionX = 0.0
            Globals.mc.player.motionZ = 0.0
        }

        Globals.mc.player.onGround = true
        Globals.mc.player.motionY = 0.0
        if (Globals.mc.player.isRiding) Globals.mc.player.ridingEntity?.motionY = 0.0
        Globals.mc.player.noClip = true

        Globals.mc.player.onGround = false
        Globals.mc.player.fallDistance = 0.0F
        clonedEntity?.setupPlayerData()
    }

    @Listener
    private fun onMoving(event: MoveEvent) {
        if (fullNullCheck()) return
        Globals.mc.player.noClip = true
    }

    @Listener
    private fun onPacketSent(event: PacketEvent.Send) {
        if (event.stage != EventStageable.EventStage.PRE) return
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (event.packet is SPacketPlayerPosLook) {
            prevPos = Vec3d(event.packet.x, event.packet.y, event.packet.z)
            prevRotation = Vec2f(event.packet.yaw, event.packet.pitch)
        }
    }

    @Listener
    private fun onSetupTerrain(event: SetupTerrainEvent) {
        event.cancel()
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

    private class ClonedPlayer: EntityOtherPlayerMP(Globals.mc.world, Globals.mc.player.gameProfile) {

        init {
            setupPlayerData()
            teleportDirection = Globals.mc.player.teleportDirection
            copyLocationAndAnglesFrom(Globals.mc.player)
            rotationYawHead = Globals.mc.player.rotationYawHead
        }

        fun setupPlayerData() {
            inventory.copyInventory(Globals.mc.player.inventory)
            foodStats = Globals.mc.player.foodStats
            experienceLevel = Globals.mc.player.experienceLevel
            experienceTotal = Globals.mc.player.experienceTotal
            experience = Globals.mc.player.experience
            xpSeed = Globals.mc.player.xpSeed
            health = Globals.mc.player.health
            score = Globals.mc.player.score
            getDataManager()[PLAYER_MODEL_FLAG] = Globals.mc.player.dataManager.get(PLAYER_MODEL_FLAG)
            absorptionAmount = Globals.mc.player.absorptionAmount
            activePotionMap.putAll(Globals.mc.player.activePotionMap)
        }

        fun addEntityToWorld() {
            Globals.mc.world.addEntityToWorld(-69102, this)
        }
    }

}
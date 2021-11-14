package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.world.block.BlockCollisionBoundingBoxEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AvoidModule: Module("Avoid", Category.MISC, "Avoids interactions with certain things") {
    private val fire = Value(false, "Fire")
    private val cactus = Value(false, "Cactus")
    private val lava = Value(false, "Lava")
    private val webs = Value(false, "Webs")

    private val unloaded = Value(true, "Unloaded")
    private val void = EnumValue(AntiVoidMethod.Off, "Void")
    private val yPosition = NumberValue({ void.value != AntiVoidMethod.Off }, 1.0, 0.0, 5.0, 0.1, "YPos")

    init {
        addSettings(fire, cactus, lava, webs, unloaded, void, yPosition)
    }

    private var lastGroundPosition = BlockPos.ORIGIN
    private var canMotion = false

    private enum class AntiVoidMethod {
        Off, Stay, Motion
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        when (void.value) {
            AntiVoidMethod.Stay -> {
                if (Globals.mc.player.entityBoundingBox.minY <= yPosition.value && Globals.mc.player.fallDistance > 0.0) {
                    val result = Globals.mc.world.rayTraceBlocks(Globals.mc.player.positionVector, Vec3d(Globals.mc.player.posX, 0.0, Globals.mc.player.posZ))
                    if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK) {
                        Globals.mc.player.setVelocity(0.0, 0.0, 0.0)
                        Globals.mc.player.ridingEntity?.setVelocity(0.0, 0.0, 0.0)
                    }
                }
            }

            AntiVoidMethod.Motion -> {
                if (Globals.mc.player.onGround) lastGroundPosition = BlockPos(Globals.mc.player.posX, Globals.mc.player.posY, Globals.mc.player.posZ)
                if (!isBlockUnder()) {
                    if (Globals.mc.player.fallDistance > 0.0F) {
                        if (canMotion) {
                            Globals.mc.player.motionY = 1.5
                            canMotion = false
                        } else {
                            Globals.mc.player.connection.sendPacket(CPacketPlayer.Position(lastGroundPosition.x.toDouble(), lastGroundPosition.y.toDouble(), lastGroundPosition.z.toDouble(), true))
                            Globals.mc.player.fallDistance = 0.0F
                        }
                    } else {
                        canMotion = true
                    }
                } else {
                    canMotion = true
                }
            }

        }

    }

    private fun isBlockUnder(): Boolean {
        val customResult = Globals.mc.world.rayTraceBlocks(Globals.mc.player.positionVector, Vec3d(Globals.mc.player.posX, 0.0, Globals.mc.player.posZ))
        if (Globals.mc.player.entityBoundingBox.minY <= yPosition.value && (customResult == null || customResult.typeOfHit != RayTraceResult.Type.BLOCK)) return false

        var off = 0.0
        while (off < Globals.mc.player.posY + 2.0) {
            val bb = Globals.mc.player.entityBoundingBox.offset(0.0, -off, 0.0)
            if (Globals.mc.world.getCollisionBoxes(Globals.mc.player, bb).isNotEmpty()) return true
            off += 2.0
        }

        return false
    }

    @Listener
    private fun onBlockCollisionBoundingBox(event: BlockCollisionBoundingBoxEvent) {
        if (fullNullCheck()) return
        val block = event.pos.block
        if ((block == Blocks.LAVA && lava.value) || (block == Blocks.WEB && webs.value) || (block == Blocks.FIRE && fire.value) || (block == Blocks.CACTUS && cactus.value) || ((!Globals.mc.world.isBlockLoaded(event.pos, false) || event.pos.y < 0) && unloaded.value)) {
            event.cancel()
            event.boundingBox = Block.FULL_BLOCK_AABB
        }
    }

}
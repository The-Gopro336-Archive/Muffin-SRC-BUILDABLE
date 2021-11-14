package me.han.muffin.client.utils.entity

import com.google.gson.JsonParser
import me.han.muffin.client.core.Globals
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.utils.extensions.kotlin.ceilToInt
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.block.selectedBox
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.item.id
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import net.minecraft.block.*
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.net.URL

object EntityUtil {

    val Entity.prevPosVector get() = Vec3d(this.prevPosX, this.prevPosY, this.prevPosZ)
    val Entity.flooredPosition get() = BlockPos(posX.floorToInt(), posY.floorToInt(), posZ.floorToInt())

    @JvmStatic
    fun fullNullCheck() = Globals.mc.player == null || Globals.mc.world == null

    fun isntValid(entity: Entity?, range: Double): Boolean {
        return entity == null || !entity.isAlive || entity == Globals.mc.renderViewEntity || entity == Globals.mc.player ||
                entity is EntityPlayer && FriendManager.isFriend(entity.getName()) ||
                Globals.mc.player.getDistanceSq(entity) > range.square
    }

    fun isInWater(entity: Entity?): Boolean {
        if (entity == null) return false
        return entity.isInWater || isInBlock(BlockLiquid::class.java, entity)
    }

    fun isInBlock(clazz: Class<out Block>, entity: Entity?): Boolean {
        if (entity == null) return false
        val y = entity.entityBoundingBox.minY.toInt()

        for (x in entity.entityBoundingBox.minX.floorToInt() until entity.entityBoundingBox.maxX.ceilToInt())
            for (z in entity.entityBoundingBox.minZ.floorToInt() until entity.entityBoundingBox.maxZ.ceilToInt()) {
                val pos = BlockPos(x, y, z)
                if (clazz.isInstance(pos.block)) return true
            }

        return false
    }

    fun isInBlockServerSide(entity: Entity?): Boolean {
        if (entity == null) return false

        val serverLocation = LocalMotionManager.serverSidePosition
        val serverBB = AxisAlignedBB(serverLocation.toBlockPos())

        for (x in serverBB.minX.floorToInt() until serverBB.maxX.ceilToInt()) {
            for (y in serverBB.minY.floorToInt() until serverBB.maxY.ceilToInt()) {
                for (z in serverBB.minZ.floorToInt() until serverBB.maxZ.ceilToInt()) {
                    val pos = BlockPos(x, y, z)
                    if (pos.isAir) continue
                    if (pos.block is BlockTallGrass) return false
                    if (serverBB.intersects(pos.selectedBox)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun isAboveWater(entity: Entity?): Boolean {
        return isAboveWater(entity, false)
    }

    fun isAboveBlock(clazz: Class<out Block>, entity: Entity?): Boolean {
        return isAboveBlock(clazz, entity, false)
    }

    fun isAboveWater(entity: Entity?, packet: Boolean): Boolean {
        return isAboveBlock(BlockLiquid::class.java, entity, packet)
    }

    fun isAboveBlock(clazz: Class<out Block>, entity: Entity?, packet: Boolean): Boolean {
        if (entity == null) return false
        // increasing this seems to flag more in NCP but needs to be increased so the player lands on solid water
        return isAboveBlock(clazz, entity, (entity.entityBoundingBox.minY - if (packet) 0.03 else if (entity is EntityPlayer) 0.2 else 0.5).floorToInt())
    }

    private fun isAboveBlock(clazz: Class<out Block>, entity: Entity?, posY: Int): Boolean {
        if (entity == null) return false
        //val y = entity.entityBoundingBox.offset(0.0, -0.01, 0.0).minY.toInt()

        for (x in entity.entityBoundingBox.minX.floorToInt() until entity.entityBoundingBox.maxX.ceilToInt())
            for (z in entity.entityBoundingBox.minZ.floorToInt() until entity.entityBoundingBox.maxZ.ceilToInt()) {
            val pos = BlockPos(x, posY, z)
            if (clazz.isInstance(pos.block)) return true
        }

        return false
    }

    fun findClosestTarget(range: Double): EntityPlayer? {
        var closestTarget: EntityPlayer? = null

        for (target in Globals.mc.world.playerEntities) {
            if (isntValid(target, range)) continue

            if (closestTarget == null) {
                closestTarget = target
                continue
            }

            if (Globals.mc.player.getDistanceSq(target) < Globals.mc.player.getDistanceSq(closestTarget)) {
                closestTarget = target
            }
        }

        return closestTarget
    }

    inline fun findClosestTargetFilter(range: Double, filter: EntityPlayer.() -> Boolean): EntityPlayer? {
        var closestTarget: EntityPlayer? = null

        for (target in Globals.mc.world.playerEntities) {
            if (isntValid(target, range)) continue

            if (filter.invoke(target)) {
                continue
            }

            if (closestTarget == null) {
                closestTarget = target
                continue
            }

            if (Globals.mc.player.getDistanceSq(target) < Globals.mc.player.getDistanceSq(closestTarget)) {
                closestTarget = target
            }
        }

        return closestTarget
    }

    fun findClosestPlayer(range: Double): EntityPlayer? {
        var closestTarget: EntityPlayer? = null
        for (target in Globals.mc.world.playerEntities) {
            if (target == Globals.mc.player) {
                continue
            }
            if (target !is EntityLivingBase) {
                continue
            }
            if (target.health <= 0) {
                continue
            }
            if (Globals.mc.player.getDistance(target) > range) {
                continue
            }
            if (closestTarget == null) {
                closestTarget = target
                continue
            }
            if (Globals.mc.player.getDistance(target) < Globals.mc.player.getDistance(closestTarget)) {
                closestTarget = target
            }
        }

        return closestTarget
    }

    fun isEating() = Globals.mc.player.heldItemMainhand.item is ItemFood && Globals.mc.player.isHandActive && Globals.mc.player.activeItemStack.item is ItemFood

    /**
     * Ray tracing the 8 vertex of the entity bounding box
     *
     * @return [Vec3d] of the visible vertex, null if none
     */
    fun canEntityHitboxBeSeen(entity: Entity): Vec3d? {
        val eyesPos = Globals.mc.player.eyePosition
        val box = entity.entityBoundingBox
        val xArray = arrayOf(box.minX + 0.1, box.maxX - 0.1)
        val yArray = arrayOf(box.minY + 0.1, box.maxY - 0.1)
        val zArray = arrayOf(box.minZ + 0.1, box.maxZ - 0.1)
        for (x in xArray) for (y in yArray) for (z in zArray) {
            val vertex = Vec3d(x, y, z)
            if (Globals.mc.world.rayTraceBlocks(vertex, eyesPos, false, true, false) == null) return vertex
        }
        return null
    }

    fun canEntityFeetBeSeen(entityIn: Entity) =
        Globals.mc.world.rayTraceBlocks(Globals.mc.player.eyePosition, entityIn.positionVector, false, true, false) == null


    fun getDroppedItems(itemId: Int, range: Float): ArrayList<Entity> {
        return arrayListOf<Entity>().apply {
            Globals.mc.world.loadedEntityList
                .filter { it.getDistance(Globals.mc.player) <= range && it is EntityItem && it.item.item.id == itemId }
                .forEach { add(it) }
        }
    }

    fun getDroppedItem(itemId: Int, range: Float) = getDroppedItems(itemId, range).minByOrNull { Globals.mc.player.getDistance(it) }?.positionVector?.toBlockPos()

    fun getArmorPct(stack: ItemStack): Int {
        val dmg = (stack.maxDamage.toFloat() - stack.itemDamage.toFloat()) / stack.maxDamage.toFloat()
        val getPercentage = 1 - dmg
        return 100 - (getPercentage * 100).toInt()
    }

    fun getName(playerInfo: NetworkPlayerInfo): String? {
        if (playerInfo.displayName != null) return playerInfo.displayName?.formattedText ?: ""

        val team = playerInfo.playerTeam
        val name = playerInfo.gameProfile.name

        return team?.formatString(name) ?: name
    }

    /**
     * Gets the MC username tied to a given UUID.
     *
     * @param uuid UUID to get name from.
     * @return The name tied to the UUID.
     */
    @JvmStatic
    fun getNameFromUUID(uuid: String): String? {
        return try {
            val jsonUrl = IOUtils.toString(URL("https://api.mojang.com/user/profiles/" + uuid.replace("-", "") + "/names"))
            val parser = JsonParser()
            parser.parse(jsonUrl).asJsonArray[parser.parse(jsonUrl).asJsonArray.size() - 1].asJsonObject["name"].toString().replace('"', ' ').replace(" ", "")
        } catch (ex: IOException) {
            null
        }
    }

    fun isSafe(entity: Entity, height: Double, floor: Boolean) = getUnsafeBlocks(entity, height, floor).isEmpty()
    fun isSafe(entity: Entity) = isSafe(entity, 0.0, false)

    fun getUnsafeBlocks(entity: Entity, height: Double, floor: Boolean) = getUnsafeBlocksFromVec3d(entity.positionVector, height, floor)

    fun getUnsafeBlocksFromVec3d(pos: Vec3d, height: Double, floor: Boolean): ArrayList<Vec3d> {
        return arrayListOf<Vec3d>().apply {
            getOffsets(height, floor).forEach {
                val block = pos.toBlockPos().add(it.x, it.y, it.z).block
                if (block is BlockAir || block is BlockLiquid || block is BlockTallGrass || block is BlockFire || block is BlockDeadBush || block is BlockSnow) {
                    add(it)
                }
            }
        }
    }

    fun getOffsets(y: Double, floor: Boolean): Array<Vec3d> {
        val offsets = getOffsetList(y, floor)
        val array = arrayOfNulls<Vec3d>(offsets.size)
        return offsets.toArray(array)
    }

    fun getOffsetList(y: Double, floor: Boolean): ArrayList<Vec3d> {
        return arrayListOf<Vec3d>().apply {
            add(Vec3d(-1.0, y, 0.0))
            add(Vec3d(1.0, y, 0.0))
            add(Vec3d(0.0, y, -1.0))
            add(Vec3d(0.0, y, 1.0))
            if (floor) add(Vec3d(0.0, y - 1.0, 0.0))
        }
    }

    fun canSeeBlock(pos: BlockPos) = Globals.mc.world.rayTraceBlocks(Globals.mc.player.eyePosition, pos.toVec3d(), false, true, false) == null
    fun canSeeVec3d(vector: Vec3d) = Globals.mc.world.rayTraceBlocks(Globals.mc.player.eyePosition, vector, false, true, false) == null

}
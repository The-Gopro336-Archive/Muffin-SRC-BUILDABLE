package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.LocalHotbarManager.resetHotbar
import me.han.muffin.client.manager.managers.LocalHotbarManager.serverSideItem
import me.han.muffin.client.manager.managers.LocalHotbarManager.spoofHotbar
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.client.BindUtils
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.block
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.timer.TickTimer
import me.han.muffin.client.value.BindValue
import me.han.muffin.client.value.NumberValue
import net.minecraft.block.Block
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.function.Predicate

internal object CrystalBlocksModule: Module("CrystalBlocks", Category.COMBAT, "Automatically place obsidian when no placeable block around target.") {

    private val manualPlaceBind = BindValue(Keyboard.KEY_NONE, "BindManualPlace")
    private val minDamageInc = NumberValue(2F, 0F, 10F, 0.25F, "MinDamageInc")
    private val range = NumberValue(4F, 0F, 8F, 0.5F,"Range")
    private val delay = NumberValue(20, 0, 50, 5, "Delay")

    init {
        addSettings(manualPlaceBind, minDamageInc, range, delay)
    }

    private val timer = TickTimer()
    private var inactiveTicks = 0
    private var rotationTo: Vec3d? = null
    private var placePacket: CPacketPlayerTryUseItemOnBlock? = null

    private val renderPos = BlockPos.MutableBlockPos(0, -69, 0)

    override fun onDisable() {
        inactiveTicks = 0
        placePacket = null
        resetHotbar()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (renderPos.isNotNull) {
            Muffin.getInstance().blockRenderer.drawFull(renderPos, 50)
        }
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        inactiveTicks++

        val slot = Globals.mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: return
        val target = AutoCrystalHelper.target ?: return

        if (BindUtils.checkIsClicked(manualPlaceBind.value)) prePlace(target)

        placePacket?.let { packet ->
            if (inactiveTicks > 1) {
                if (!isHoldingObby) spoofHotbar(slot.hotbarSlot)
                Globals.mc.player.swingArm(EnumHand.MAIN_HAND)
                Globals.mc.connection?.sendPacket(packet)
                resetHotbar()
                placePacket = null
            }
        }

        if (placePacket == null && AutoCrystalModule.isEnabled && AutoCrystalModule.inactiveTicks > 15) prePlace(target)

        rotationTo?.let { hitVec ->
            val rotation = RotationUtils.getRotationTo(hitVec)
            addMotion { rotate(rotation) }
        }

        if (inactiveTicks > 3) {
            rotationTo = null
            renderPos.setNull()
        }

    }


    private val isHoldingObby get() = isObby(Globals.mc.player.heldItemMainhand) ||
            isObby(Globals.mc.player.serverSideItem)

    private fun isObby(itemStack: ItemStack) = itemStack.item.block == Blocks.OBSIDIAN

    private fun prePlace(entity: EntityLivingBase) {
        if (rotationTo != null || !timer.tick((delay.value * 50.0f).toLong(), false)) return
        val placeInfo = getPlaceInfo(entity)

        if (placeInfo != null) {
            rotationTo = placeInfo.hitVec
            placePacket = CPacketPlayerTryUseItemOnBlock(placeInfo.pos, placeInfo.side, EnumHand.MAIN_HAND, placeInfo.hitVecOffset.x.toFloat(), placeInfo.hitVecOffset.y.toFloat(), placeInfo.hitVecOffset.z.toFloat())

            renderPos.setPos(placeInfo.placedPos)
            inactiveTicks = 0
            timer.reset()
        } else {
            timer.reset((delay.value * -25.0f).toLong())
        }
    }


    private fun getPlaceInfo(entity: EntityLivingBase): PlaceInfo? {
        val cacheMap = TreeMap<Float, BlockPos>(compareByDescending { it })

        val eyePos = Globals.mc.player.eyePosition

        val posList = VectorUtils.getBlockPosInSphere(eyePos, range.value)
        val maxCurrentDamage = AutoCrystalHelper.placeMap.entries
            .filter { eyePos.distanceTo(it.key) <= range.value }
            .map { it.value.targetDamage }
            .maxOrNull() ?: 0.0F

        for (pos in posList) {
            // Placeable check
            if (!pos.isPlaceable()) continue

            // Neighbour blocks check
            if (!pos.hasNeighbour) continue

            // Damage check
            val damage = calcDamage(pos, entity)
            if (!checkDamage(damage.first, damage.second, maxCurrentDamage)) continue

            cacheMap[damage.first] = pos
        }

        for (pos in cacheMap.values) return searchForNeighbour(pos, 1) ?: continue

        return null
    }

    private fun calcDamage(pos: BlockPos, entity: EntityLivingBase): Pair<Float, Float> {
        // Set up a fake obsidian here for proper damage calculation
        val prevState = pos.state
        Globals.mc.world.setBlockState(pos, Blocks.OBSIDIAN.defaultState)

        // Checks damage
        val damage = CrystalUtils.calculateDamage(pos, entity)
        val selfDamage = CrystalUtils.calculateDamage(pos, Globals.mc.player)

        // Revert the block state before return
        Globals.mc.world.setBlockState(pos, prevState)

        return damage to selfDamage
    }

    private fun checkDamage(damage: Float, selfDamage: Float, maxCurrentDamage: Float) =
        selfDamage < AutoCrystalModule.settingMaxSelfDamage && damage > AutoCrystalModule.minDamage && (maxCurrentDamage < AutoCrystalModule.minDamage || damage - maxCurrentDamage >= minDamageInc.value)

}
package me.han.muffin.client.module.modules.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.LocalHotbarManager
import me.han.muffin.client.manager.managers.LocalMotionManager
import me.han.muffin.client.manager.managers.LocalMotionManager.addMotion
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.entity.EntityUtil
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.mc.block.*
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.item.firstBlock
import me.han.muffin.client.utils.extensions.mc.item.firstByStack
import me.han.muffin.client.utils.extensions.mc.item.firstItem
import me.han.muffin.client.utils.extensions.mc.item.hotbarSlots
import me.han.muffin.client.utils.extensions.mc.world.getVisibleSides
import me.han.muffin.client.utils.extensions.mc.world.placeBlock
import me.han.muffin.client.utils.extensions.mc.world.searchForNeighbour
import me.han.muffin.client.utils.math.PlaceInfo
import me.han.muffin.client.utils.math.VectorUtils
import me.han.muffin.client.utils.math.VectorUtils.distanceTo
import me.han.muffin.client.utils.math.VectorUtils.toVec3d
import me.han.muffin.client.utils.math.VectorUtils.toVec3dCenter
import me.han.muffin.client.utils.math.rotation.RotationUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import net.minecraft.block.Block
import net.minecraft.block.BlockPistonBase
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.function.Predicate

/**
 * @author han
 * Created on 17/3/2020
 * TODO: find piston pos base on check can be push to offset direction/pos
 * TODO: do raytrace check in case we can break the crystal
 * TODO: find redstone pos base on check can piston can be power
 * @see net.minecraft.world.World.isSidePowered
 */
internal object CrystalPistonModule: Module("CrystalPiston", Category.COMBAT, "Push the crystal toward enemy by using piston and redstone block.") {
    private val delay = NumberValue(2, 0, 20, 1, "Delay")

    private val range = NumberValue(5.0, 0.1, 10.0, 0.1, "Range")
    private val wallRange = NumberValue(3.0, 0.1, 10.0, 0.1, "WallRange")

    private val breakDelay = NumberValue(2, 0, 10, 1, "BreakDelay")
    private val placeDelay = NumberValue(1, 0, 10, 1, "PlaceDelay")

    private var stage = Stage.Checks

    private var slotsData: Slots? = null
    private var buildStructure: BuildStructure? = null

    private var currentTarget: EntityPlayer? = null

    private val pistonRenderPos = BlockPos.MutableBlockPos(0, -69, 0)
    private val redstoneRenderPos = BlockPos.MutableBlockPos(0, -69, 0)
    private val crystalRenderPos = BlockPos.MutableBlockPos(0, -69, 0)

    private val pistonPos = BlockPos.MutableBlockPos(0, -69, 0)

    private val breakTimer = Timer()
    private val placeTimer = Timer()


    init {
        addSettings(delay, range, wallRange, breakDelay, placeDelay)
    }

    override fun onDisable() {
        stage = Stage.Checks
        pistonRenderPos.setNull()
        redstoneRenderPos.setNull()
        crystalRenderPos.setNull()
        pistonPos.setNull()
    }

    fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
        firstByStack { itemStack ->
            itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
        }

    private fun getSlotsData(): Slots? {
        val hotbarSlots = Globals.mc.player.hotbarSlots

        val crystalSlot = hotbarSlots.firstItem(Items.END_CRYSTAL)?.hotbarSlot ?: return null
        val redstoneSlot = (hotbarSlots.firstBlock(Blocks.REDSTONE_BLOCK) ?: hotbarSlots.firstBlock(Blocks.REDSTONE_TORCH))?.hotbarSlot ?: return null
        val pistonSlot = (hotbarSlots.firstBlock(Blocks.PISTON) ?: hotbarSlots.firstBlock(Blocks.STICKY_PISTON))?.hotbarSlot ?: return null

        return Slots(redstoneSlot, crystalSlot, pistonSlot)
    }

    private fun resetEverything() {
        stage = Stage.Checks
        pistonRenderPos.setNull()
        redstoneRenderPos.setNull()
        crystalRenderPos.setNull()
        pistonPos.setNull()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (event.packet is SPacketSoundEffect && event.packet.category == SoundCategory.BLOCKS && event.packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            val pos = BlockPos(event.packet.x, event.packet.y, event.packet.z)
            if (stage == Stage.BreakCrystal && currentTarget != null && currentTarget!!.getDistanceSq(pos) <= 6.0.square) {
                val crystalList = CrystalUtils.getCrystalList(Vec3d(event.packet.x, event.packet.y, event.packet.z), 6.0)
                for (crystal in crystalList) {
                    crystal.setDead()
                }
                resetEverything()
            }
        }

    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (fullNullCheck()) return

        when (event.stage) {
            EventStageable.EventStage.PRE -> {
                currentTarget = EntityUtil.findClosestTarget(range.value + 2.0)
                // ChatManager.sendMessage(currentTarget?.name ?: "mom")

                when (stage) {
                    Stage.Checks -> {
                        val tempSlotsData = getSlotsData()
                        if (tempSlotsData == null) {
                            ChatManager.sendMessage("Missing required items.")
                            disable()
                            return
                        }

                        slotsData = tempSlotsData
                        val tempStructure = searchStructure()

                        if (tempStructure != null) {
                            if (tempStructure.pistonPos != null) {
                                buildStructure = tempStructure
                                ChatManager.sendMessage("in piston state")
                                stage = Stage.Piston
                            }
                        }

                    }
                }
            }
            EventStageable.EventStage.POST -> {
                val slotData = slotsData ?: return
                val placeStructure = buildStructure ?: return

                when (stage) {
                    Stage.Piston -> {
                        val pistonSlot = slotData.pistonSlot
                        if (pistonSlot == -1) return

                        placeStructure.pistonPos?.let { pistonInfo ->
                            placeStructure.redstonePos?.let { redstoneInfo ->
                                //val middleHitVec = pistonInfo.hitVec.subtract(redstoneInfo.hitVec).add(pistonInfo.hitVec)
                                // RotationUtils.faceVectorWithPositionPacket(middleHitVec)
                                placeBlockInfo(pistonInfo, pistonSlot)
                                pistonPos.setPos(pistonInfo.placedPos)
                                pistonRenderPos.setPos(pistonInfo.placedPos)
                                stage = Stage.PlaceCrystal
                            }
                        }

                    }
                    Stage.PlaceCrystal -> {
                        val crystalSlot = slotData.crystalSlot
                        if (crystalSlot == -1) return

                        placeStructure.crystalPos?.let {
                            // placeCrystal(it.pos, crystalSlot)
                            placeBlockInfo(it, crystalSlot)
                            crystalRenderPos.setPos(it.placedPos)
                            // ChatManager.sendMessage("placing crystal on = ${it.pos}")
                            stage = Stage.Redstone
                        }

                    }
                    Stage.Redstone -> {
                        val redstoneSlot = slotData.redstoneSlot
                        if (redstoneSlot == -1) return

                        placeStructure.redstonePos?.let {
                            placeBlockInfo(it, redstoneSlot)
                            redstoneRenderPos.setPos(it.placedPos)
                            stage = Stage.BreakCrystal
                        }
                    }
                    Stage.BreakCrystal -> {
                        if (currentTarget == null || pistonRenderPos.isNull || !Globals.mc.world.isBlockPowered(pistonRenderPos)) {
                            ChatManager.sendMessage("not valid bruh")
                            return
                        }

                        Globals.mc.world.loadedEntityList.filter {
                            it is EntityEnderCrystal &&
                            currentTarget!!.getDistance(it) <= 3.0 && Globals.mc.player.getDistance(it) <= range.value &&
                            (Globals.mc.player.canEntityBeSeen(it) || !Globals.mc.player.canEntityBeSeen(it) && Globals.mc.player.getDistance(it) <= wallRange.value)
                        }
                        .sortedBy { it?.let { CrystalUtils.calculateDamage(it.positionVector, currentTarget!!) } }
                        .forEach {
                            if (breakTimer.passedTicks(breakDelay.value)) {
                                addMotion { rotate(RotationUtils.getRotationTo(it.positionVector)) }
                                Globals.mc.player.connection.sendPacket(CPacketUseEntity(it))
                                Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

                                stage = Stage.Reset
                                breakTimer.reset()
                            }
                        }

                    }
                    Stage.Reset -> {
                        resetEverything()
                    }
                }

            }
        }
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (pistonRenderPos.isNotNull) {
            RenderUtils.drawBlockFullESP(pistonRenderPos, 200, 0, 0, 35, 1.5F)
        }

        if (redstoneRenderPos.isNotNull) {
            RenderUtils.drawBlockFullESP(redstoneRenderPos, 0, 200, 0, 35, 1.5F)
        }

        if (crystalRenderPos.isNotNull) {
            RenderUtils.drawBlockFullESP(crystalRenderPos, 0, 0, 200, 35, 1.5F)
        }


    }

    private fun placeCrystal(pos: BlockPos, slot: Int) {
        val lastSlot = Globals.mc.player.inventory.currentItem

        val shouldSwap = LocalHotbarManager.serverSideHotbar != slot
        val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(pos.block) || pos.needTileSneak)
        val isSprinting = Globals.mc.player.isSprinting

        if (shouldSwap) InventoryUtils.swapSlot(slot)

        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        // Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5F, 0.5F, 0.5F))
        // Globals.mc.player.connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
        placeBlock(pos, EnumFacing.UP, EnumHand.MAIN_HAND, packet = false, packetRotate = true, swingArm = true, noGhost = true)

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))

        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)
    }

    private fun placeBlockInfo(info: PlaceInfo, slot: Int, rotateTo: Boolean = true) {
        val lastSlot = Globals.mc.player.inventory.currentItem

        val shouldSwap = LocalHotbarManager.serverSideHotbar != slot
        val shouldSneak = !Globals.mc.player.isSneaking && (rightClickableBlock.contains(info.pos.block) || info.pos.needTileSneak)
        val isSprinting = Globals.mc.player.isSprinting

        if (shouldSwap) InventoryUtils.swapSlot(slot)

        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING))
        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING))

        placeBlock(info, EnumHand.MAIN_HAND, packet = false, packetRotate = rotateTo, swingArm = true, noGhost = true)

        if (shouldSneak) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        if (isSprinting) Globals.mc.player.connection.sendPacket(CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING))

        if (shouldSwap) InventoryUtils.swapSlot(lastSlot)
    }

    private fun searchStructure(): BuildStructure? {
        val target = currentTarget ?: return null

        val eyesPos = Globals.mc.player.eyePosition

        val availableBlocks = VectorUtils.getBlockPosInSphere(eyesPos, range.value.toFloat()).sortedBy { target.getDistanceSq(it) - Globals.mc.player.getDistanceSq(it) }
        if (availableBlocks.isEmpty()) return null

        val localFacing = LocalMotionManager.horizontalFacing
        var minEfficient = Float.MIN_VALUE

        var crystalPos: PlaceInfo? = null
        var pistonPos: PlaceInfo? = null
        var redstonePos: PlaceInfo? = null

        for (pos in availableBlocks) {
            // check is target out of range
            if (target.distanceTo(pos) > 8) continue

            // if the pos can't place crystal
            if (!pos.hasNeighbour || !CrystalUtils.canPlaceCrystal(pos, strictDirection = true)) continue

            val crystalHitVec = pos.toVec3dCenter(0.0, 0.5, 0.0)
            if (wallRange.value > 0.0 && !pos.isVisible() && eyesPos.distanceTo(crystalHitVec) > wallRange.value) continue

            val crystalUpPos = pos.up()

            // get best direction base on crystal pos
            val bestDirection = getBestDirection(eyesPos, localFacing, target, crystalUpPos) ?: continue
            //ChatManager.sendMessage(bestDirection.toString())

            val possibleCrystalPosition = searchForCrystalNeighbour(crystalUpPos) ?: continue

            val crystalClickPos = possibleCrystalPosition.pos
            val crystalPlacePos = possibleCrystalPosition.placedPos

            val offsetToPlayer = crystalClickPos.offset(bestDirection)
            val selfDamage = CrystalUtils.calculateDamage(offsetToPlayer, Globals.mc.player)
            val targetDamage = CrystalUtils.calculateDamage(offsetToPlayer, target)

            val efficientDamage = targetDamage - selfDamage
            if (efficientDamage < 2.5) continue

            val oppositeFromCrystal = EnumFacing.HORIZONTALS.firstOrNull { it != bestDirection } ?: continue // bestDirection.opposite

            // TODO: make not only detecting opposite
            val pistonPosition = crystalPlacePos.offset(oppositeFromCrystal) //.offset(crystalPlacePos.closestVisibleSide ?: EnumFacing.UP)
            if (!pistonPosition.isPlaceable()) {
                // ChatManager.sendMessage("not placeable")
                continue
            }

            // ChatManager.sendMessage("crystal place pos = ${crystalPlacePos.toString()}")
            if (!BlockPistonBase.canPush(crystalPlacePos.state, Globals.mc.world, crystalPlacePos, bestDirection, false, bestDirection)) continue

            val possiblePistonPosition = searchForPistonPosition(eyesPos, localFacing, pistonPosition, 3, true, bestDirection) ?: continue

            val pistonClickPos = possiblePistonPosition.pos
            val pistonPlacePos = possiblePistonPosition.placedPos

            // if (eyesPos.distanceTo(possiblePistonPosition.pos.toVec3dCenter()) > 4) continue

            // val lastState = possiblePistonPosition.pos.state
            // Globals.mc.world.setBlockState(possiblePistonPosition.pos, Blocks.PISTON.defaultState)
            //if (localFacing.opposite != possiblePistonPosition.pos.state.getValue(BlockDirectional.FACING)) continue
            // Globals.mc.world.setBlockState(possiblePistonPosition.pos, lastState)

            val redstonePosition = pistonPlacePos.offset(oppositeFromCrystal)//.offset(pistonPlacePos.closestVisibleSide ?: EnumFacing.UP)
            // val lastState = pistonPlacePos.state
            // Globals.mc.world.setBlockState(pistonPlacePos, Blocks.PISTON.defaultState)

            // ChatManager.sendMessage("checking side power = $pistonPlacePos")
            // redstoneRenderPos.setPos(pistonPlacePos)

//            if (isSidePowered(pistonPlacePos, bestDirection)) {
//                //Globals.mc.world.setBlockState(pistonPlacePos, lastState)
//                ChatManager.sendMessage("cant power")
//                continue
//            }

            //Globals.mc.world.setBlockState(pistonPlacePos, lastState)


            val possibleRedstonePosition = searchForRedstonePosition(eyesPos, redstonePosition, 6, true, pistonPosition, bestDirection) ?: continue
            // if (!checkCanRedstoneBePlaced(possibleRedstonePosition.placedPos)) continue

            // ChatManager.sendMessage(bestDirection.toString())

            if (efficientDamage > minEfficient) {
                minEfficient = efficientDamage

                crystalPos = possibleCrystalPosition
                pistonPos = possiblePistonPosition
                redstonePos = possibleRedstonePosition
            }
        }

        return BuildStructure(crystalPos, pistonPos, redstonePos)
    }

    /**
     * @return most efficient damage of offset pos and direction to push
     * TODO: check piston can be push to offset pos base on local direction toward return direction
     * TODO: use facing to iterate piston position
     */
    private fun getBestDirection(eyesPos: Vec3d, localFacing: EnumFacing, target: EntityPlayer, crystalPos: BlockPos): EnumFacing? {
        var bestOffset: BlockPos? = null
        var bestFacing: EnumFacing? = null

        var maxDamage = Float.MAX_VALUE
        var minDamage = Float.MIN_VALUE
        var maxDistance = Float.MAX_VALUE

        // iterate through all directions
        // for (facing in EnumFacing.values().filter { it != EnumFacing.UP && it != EnumFacing.DOWN && it != localFacing }) {
        for (facing in EnumFacing.values()) {
            // offset crystal position
            val offsetPos = crystalPos.offset(facing)
            val crystalCenterUpOffset = offsetPos.toVec3d(0.5, 0.0, 0.5)

            val localDistanceTo = eyesPos.distanceTo(crystalCenterUpOffset)
            if (localDistanceTo > range.value) continue

            // if (!BlockPistonBase.canPush(offsetPos.state, Globals.mc.world, offsetPos, facing, false, facing)) continue

            if (wallRange.value > 0.0 && !offsetPos.isVisible() && localDistanceTo > wallRange.value) continue

            val distanceTo  = target.distanceTo(crystalCenterUpOffset)
            if (distanceTo > 2) continue

            val selfDamage = CrystalUtils.calculateDamage(crystalCenterUpOffset, Globals.mc.player)
            val targetDamage = CrystalUtils.calculateDamage(crystalCenterUpOffset, target)

            val efficientDamage = targetDamage - selfDamage
            if (efficientDamage < 4.0) continue
            // TODO: check mindmg if it works

            // get most efficient damage
            if (efficientDamage > minDamage && maxDistance > distanceTo) {
                minDamage = efficientDamage
                maxDistance = distanceTo.toFloat()
                bestFacing = facing
                bestOffset = offsetPos
            }
        }

        // in case it null
        if (bestOffset == null || bestFacing == null) return null

//        val targetDamage = CrystalUtils.calculateDamage(bestOffset.down(2), target)
//        if (targetDamage < 5) return null

//        ChatManager.sendMessage(bestOffset.down(2).toString())

//        ChatManager.sendMessage("crystalpos = ${crystalPos.toString()}")
//        ChatManager.sendMessage("best offset = ${bestOffset.toString()}")
//        ChatManager.sendMessage("mindamage = ${minDamage.toString()}")
//        ChatManager.sendMessage("maxdamage = ${maxDamage.toString()}")

        // most efficient damage offset & direction to push
        return bestFacing
    }

    // private fun getBestPistonPosition(bestDirection:)

    private fun checkCanRedstoneBePlaced(pistonPos: BlockPos): Boolean {
        for (facing in EnumFacing.values()) {
            val offsetPos = pistonPos.offset(facing)
            if (offsetPos.canBeClicked || !offsetPos.isPlaceable()) continue
            val collisionBB = Blocks.REDSTONE_TORCH.defaultState.getCollisionBoundingBox(Globals.mc.world, pistonPos) ?: return false
            return offsetPos.material.isReplaceable && Globals.mc.world.checkNoEntityCollision(collisionBB.offset(offsetPos))
        }
        return false
    }

    private fun searchForCrystalNeighbour(pos: BlockPos): PlaceInfo? {
        val neighbourIn = searchForNeighbour(pos, 1, range.value.toFloat(), true) ?: return null
        if (!CrystalUtils.canPlaceCrystal(neighbourIn.pos)) return null
        return neighbourIn
    }

    private fun searchForRedstonePosition(eyesPos: Vec3d, pos: BlockPos, attempts: Int, strictDirection: Boolean, offsetPiston: BlockPos, offsetFacing: EnumFacing): PlaceInfo? {
        for (side in EnumFacing.values()) {
            val result = checkRedstonePosition(eyesPos, pos, side, strictDirection, offsetPiston, offsetFacing)
            if (result != null) return result
        }

        if (attempts > 1) {
            for (side in EnumFacing.values()) {
                val newPos = pos.offset(side)
                if (!newPos.isPlaceable()) continue
                return searchForRedstonePosition(eyesPos, newPos, attempts - 1, strictDirection, offsetPiston, offsetFacing) ?: continue
            }
        }

        return null
    }

    private fun checkRedstonePosition(eyesPos: Vec3d, pos: BlockPos, side: EnumFacing, strictDirection: Boolean, pistonPos: BlockPos, offsetFacing: EnumFacing): PlaceInfo? {
        val offsetPos = pos.offset(side)
        val oppositeSide = side.opposite

        val hitVec = offsetPos.getHitVec(oppositeSide)
        val dist = eyesPos.distanceTo(hitVec)

        if (dist > range.value) return null
        if (strictDirection && !offsetPos.getVisibleSides(true).contains(oppositeSide)) return null
        if (offsetPos.state.isReplaceable) return null
        if (!pos.isPlaceable()) return null

        // TODO: check is facing wrong - if wrong then opposite
//        if (!Globals.mc.world.isSidePowered(pistonPos, offsetFacing)) {
//            ChatManager.sendMessage("cant power ")
//        }

        val hitVecOffset = oppositeSide.hitVecOffset
        return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
    }

    private fun searchForPistonPosition(eyesPos: Vec3d, localFacing: EnumFacing, pos: BlockPos, attempts: Int, strictDirection: Boolean, offsetCrystalFacing: EnumFacing): PlaceInfo? {
        for (side in EnumFacing.values()) {
            val result = checkPistonPosition(eyesPos, localFacing, pos, side, strictDirection, offsetCrystalFacing)
            if (result != null) return result
        }

        if (attempts > 1) {
            for (side in EnumFacing.values()) {
                val newPos = pos.offset(side)
                if (!newPos.isPlaceable()) continue
                return searchForPistonPosition(eyesPos, localFacing, newPos, attempts - 1, strictDirection, offsetCrystalFacing) ?: continue
            }
        }

        return null
    }

    // TODO: don forgot meaning between replaceable and placeable
    private fun checkPistonPosition(eyesPos: Vec3d, localFacing: EnumFacing, pos: BlockPos, side: EnumFacing, strictDirection: Boolean, offsetCrystalFacing: EnumFacing): PlaceInfo? {
        val offsetPos = pos.offset(side)
        val oppositeSide = side.opposite

        val hitVec = offsetPos.getHitVec(oppositeSide)
        val dist = eyesPos.distanceTo(hitVec)

        if (dist > range.value) return null
        if (strictDirection && !offsetPos.getVisibleSides(true).contains(oppositeSide)) return null
        if (offsetPos.state.isReplaceable) return null
        if (!pos.isPlaceable()) return null

        // TODO: fix if wrong facing
        //val lastState = pos.state
        //Globals.mc.world.setBlockState(pos, Blocks.PISTON.defaultState)
        //if (localFacing.opposite != pos.state.getValue(BlockDirectional.FACING)) return null
        //Globals.mc.world.setBlockState(pos, lastState)

        // TODO: fix if wrong position and check destroyBlocks parameter
        val offsetCrystalPos = pos.offset(offsetCrystalFacing)
        if (!BlockPistonBase.canPush(offsetCrystalPos.state, Globals.mc.world, offsetCrystalPos, offsetCrystalFacing, true, offsetCrystalFacing)) return null

        val hitVecOffset = oppositeSide.hitVecOffset
        return PlaceInfo(offsetPos, oppositeSide, dist, hitVecOffset, hitVec, pos)
    }

    /**
     * @return pos that can push crystal toward target
     */
//    private fun checkPistonPosition(offsetCrystalPos: BlockPos, offsetCrystalFacing: EnumFacing): BlockPos? {
//        for (facing in EnumFacing.values()) {
//            val offsetPos = offsetCrystalPos.offset(facing)
//            val offsetState = offsetPos.state
//
//            if (offsetState.getValue(BlockDirectional.FACING) != offsetCrystalFacing) continue
//
//        }
//
//        return null
//    }

    private fun isSidePowered(pos: BlockPos, side: EnumFacing): Boolean {
        return getRedstonePower(pos, side) > 0
    }

    private fun getRedstonePower(pos: BlockPos, facing: EnumFacing): Int {
        val state = Blocks.PISTON.defaultState
        return if (state.block.shouldCheckWeakPower(state, Globals.mc.world, pos, facing)) Globals.mc.world.getStrongPower(pos) else state.getWeakPower(Globals.mc.world, pos, facing)
    }

    data class Slots(val redstoneSlot: Int = -1, val crystalSlot: Int = -1, val pistonSlot: Int = -1)
    data class BuildStructure(val crystalPos: PlaceInfo?, val pistonPos: PlaceInfo?, val redstonePos: PlaceInfo?)


    private enum class Stage {
        Checks, Piston, PlaceCrystal, Redstone, BreakCrystal, Reset
    }

}
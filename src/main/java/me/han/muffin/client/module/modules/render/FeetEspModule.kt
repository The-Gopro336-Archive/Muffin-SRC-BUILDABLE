package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.HoleManager.holeInfo
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.player.FreecamModule
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.combat.CrystalUtils
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.extensions.kotlin.square
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.block.blockHardness
import me.han.muffin.client.utils.extensions.mc.block.isAir
import me.han.muffin.client.utils.extensions.mc.block.selectedBox
import me.han.muffin.client.utils.extensions.mc.block.state
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.entity.nearestPlayers
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal object FeetEspModule: Module("FeetESP", Category.RENDER, "Highlight the block that are able to blow crystal.") {
    private val page = EnumValue(Pages.General, "Page")

    private val renderSelf = Value({ page.value == Pages.General },false, "RenderSelf")
    private val renderFriend = Value({ page.value == Pages.General },false, "RenderFriend")
    private val maxTargets = NumberValue({ page.value == Pages.General },2, 1, 20, 1, "MaxTargets")

    private val holeOnly = Value({ page.value == Pages.General },true, "HoleOnly")
    private val frustumCheck = Value({ page.value == Pages.General },true, "FrustumCheck")
    private val distance = NumberValue({ page.value == Pages.General },7.0, 1.0, 16.0, 0.2, "Distance")

    private val renderMode = EnumValue({ page.value == Pages.Render }, RenderMode.Solid, "RenderMode")

    private val selfRed = NumberValue({ page.value == Pages.Render && renderSelf.value },189, 0, 255, 1, "SelfRed")
    private val selfGreen = NumberValue({ page.value == Pages.Render && renderSelf.value },56, 0, 255, 1, "SelfGreen")
    private val selfBlue = NumberValue({ page.value == Pages.Render && renderSelf.value },209, 0, 255, 1, "SelfBlue")
    private val selfAlpha = NumberValue({ page.value == Pages.Render && renderSelf.value },63, 0, 255, 1, "SelfAlpha")

    private val otherRed = NumberValue({ page.value == Pages.Render  },189, 0, 255, 1, "OtherRed")
    private val otherGreen = NumberValue({ page.value == Pages.Render },56, 0, 255, 1, "OtherGreen")
    private val otherBlue = NumberValue({ page.value == Pages.Render },209, 0, 255, 1, "OtherBlue")
    private val otherAlpha = NumberValue({ page.value == Pages.Render },63, 0, 255, 1, "OtherAlpha")

    private val lineWidth = NumberValue({ page.value == Pages.Render && renderMode.value != RenderMode.Solid },1.5F, 0.1F, 5.0F, 0.1F, "LineWidth")
    private val renderHeight = NumberValue({ page.value == Pages.Render },0.5F, 0.0F, 1.0F, 0.01F, "RenderHeight")

    private val renderFeets = hashSetOf<Pair<EntityPlayer, BlockPos>>().synchronized()

    private var updateThread: Thread? = null
    private val updateExecutor = Executors.newCachedThreadPool()

    private enum class Pages {
        General, Render
    }

    private enum class RenderMode {
        Solid, Outline, Full
    }

    init {
        addSettings(
            page,
            renderSelf, renderFriend, maxTargets, holeOnly, frustumCheck, distance,
            renderMode,
            selfRed, selfGreen, selfBlue, selfAlpha,
            otherRed, otherGreen, otherBlue, otherAlpha,
            lineWidth, renderHeight
        )
    }

    @Listener
    private fun onMotionUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted) {
            updateThread = thread(start = false) { doFeetDetect() }
            updateExecutor.execute(updateThread!!)
        }

    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val playerView = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val interpView = MathUtils.interpolateEntity(playerView, RenderUtils.renderPartialTicks)

        val selfColour = Colour(selfRed.value, selfGreen.value, selfBlue.value, selfAlpha.value)
        val otherColour = Colour(otherRed.value, otherGreen.value, otherBlue.value, otherAlpha.value)

        synchronized(renderFeets) {
            renderFeets.forEach { (player, pos) ->
                val renderOffset = pos.selectedBox
                    .grow(RenderUtils.BBGrow)
                    .offset(-interpView.x, -interpView.y, -interpView.z)

                val renderBB = AxisAlignedBB(
                    renderOffset.minX, renderOffset.minY, renderOffset.minZ,
                    renderOffset.maxX, renderOffset.minY + renderHeight.value, renderOffset.maxZ
                )

                val isLocal = player == Globals.mc.player

                when (renderMode.value) {
                    RenderMode.Solid -> RenderUtils.drawBoxESP(renderBB, if (isLocal) selfColour else otherColour)
                    RenderMode.Outline -> RenderUtils.drawBoxOutlineESP(renderBB, if (isLocal) selfColour else otherColour, lineWidth.value)
                    RenderMode.Full -> RenderUtils.drawBoxFullESP(renderBB, if (isLocal) selfColour else otherColour, lineWidth.value)
                }
            }
        }

    }

    private fun doFeetDetect() {
        val tempFeets = hashSetOf<Pair<EntityPlayer, BlockPos>>()

        var targetCounter = 0

        for (player in nearestPlayers) {
            if (!player.isAlive || player == FreecamModule.cameraGuy || Globals.mc.player.getDistanceSq(player) > distance.value.square) continue

            if (!renderSelf.value && player == Globals.mc.player) continue
            if (!renderFriend.value && player != Globals.mc.player && FriendManager.isFriend(player.name)) continue
            if (frustumCheck.value && !RenderUtils.isInViewFrustum(player)) continue

            val flooredPosition = player.flooredPosition

            for (facing in EnumFacing.HORIZONTALS) {
                val offset = flooredPosition.offset(facing)
                if (tempFeets.any { it.second == offset } || offset.isAir || !CrystalUtils.isResistant(offset.state) || offset.blockHardness == -1F) continue

                val offsetPlace = offset.down()
                val secondOffsetPlace = offsetPlace.offset(facing)
                val thirdOffsetPlace = secondOffsetPlace.offset(facing)

                if (!CrystalUtils.canPlaceOn(offsetPlace) &&
                    !CrystalUtils.canPlaceOn(secondOffsetPlace) &&
                    !CrystalUtils.canPlaceOn(thirdOffsetPlace)) {
                    continue
                }

                if (!offset.up().isAir) continue

                val holeInfo = player.holeInfo

                if (holeInfo.isHole) {
                    holeInfo.surroundPos.forEach {
                        if (it.blockHardness != -1.0F && it != flooredPosition.down() && it.up().isAir) {
                            tempFeets.add(player to it)
                        }
                    }
                } else if (!holeOnly.value) {
                    tempFeets.add(player to offset)
                }

            }

            if (++targetCounter > maxTargets.value) break
        }

        synchronized(renderFeets) {
            renderFeets.clear()
            renderFeets.addAll(tempFeets)
        }
    }


}
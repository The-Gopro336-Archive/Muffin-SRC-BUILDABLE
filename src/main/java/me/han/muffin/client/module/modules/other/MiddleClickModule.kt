package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MiddleClickEvent
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.friend.Friend
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.ChatIDs
import me.han.muffin.client.utils.InventoryUtils
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.BindValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Keyboard
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object MiddleClickModule: Module("MiddleClick", Category.OTHERS, true, true, "Do something by middle clicking.") {
    private val friend = Value(true, "Friend")
    private val pearl = BindValue(Keyboard.KEY_NONE, "Pearl")
    private val infiniteFriend = Value(false, "InfiniteFriend")
    //private val test = NumberValue(2.5, 0.0, 10.0, 0.1, "value")

    private val clickCount = NumberValue({ pearl.value != Keyboard.KEY_NONE }, 1, 1, 5, 1, "PearlClickCounter")

    private var clickedCount = 0
    private val clearTimer = Timer()

    init {
        addSettings(friend, pearl, clickCount) //, test)
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return

        if (clickedCount > 0 && clearTimer.passed(5500.0)) {
            clearTimer.reset()
            clickedCount = 0
        }
    }

    @Listener
    private fun onMiddleClick(event: MiddleClickEvent) {
        if (fullNullCheck() || Globals.mc.currentScreen != null) return

        /*
        if (infiniteFriend.value) {
            for (targeted in AutoCrystalHelper.sortedTarget) {
                if (targeted == null || targeted == Globals.mc.player || targeted == Globals.mc.renderViewEntity || !EntityUtil.isEntityAlive(targeted)) continue
                val lookVector = Globals.mc.player.getLook(1.0F).normalize()

                val eyesPos = Globals.mc.player.getPositionEyes(1F)
                val xDiff = targeted.posX - eyesPos.x
                val yDiff = targeted.posY + targeted.getEyeHeight() - eyesPos.y
                val zDiff = targeted.posZ - eyesPos.z

                var vectorDiff = Vec3d(xDiff, yDiff, zDiff)
                val vectorLength = vectorDiff.length()
                vectorDiff = vectorDiff.normalize()

                val dotProduct = lookVector.dotProduct(vectorDiff)
                if (dotProduct > 1.0 - test.value.div(100.0) / vectorLength && Globals.mc.player.canEntityBeSeen(targeted)) {
                    doFriendPlayer(targeted.name)
                    break
                }
            }
            return
        }
         */

        clickedCount++

//        if (friend.value && infiniteFriend.value) {
//            val rayTracePlayer = RayTraceUtils.getRayTraceResult(Vec2f(Globals.mc.player), 256.0F)
//            println(rayTracePlayer.blockPos)
//            println(rayTracePlayer.typeOfHit)
//            if (rayTracePlayer.typeOfHit == RayTraceResult.Type.ENTITY && rayTracePlayer.entityHit != null && rayTracePlayer.entityHit is EntityPlayer) {
//                doFriendPlayer(rayTracePlayer.entityHit.name)
//                clickedCount = 0
//                return
//            }
//        }

        val rayTraceMouse = Globals.mc.objectMouseOver ?: return

        if (friend.value && !infiniteFriend.value) {
            if (rayTraceMouse.typeOfHit == RayTraceResult.Type.ENTITY && rayTraceMouse.entityHit != null && rayTraceMouse.entityHit is EntityPlayer) {
                doFriendPlayer(rayTraceMouse.entityHit.name)
                clickedCount = 0
                return
            }
        }

        if (pearl.value != Keyboard.KEY_NONE && clickedCount >= clickCount.value && rayTraceMouse.typeOfHit == RayTraceResult.Type.MISS) {
            clickedCount = 0
            val lastSlot = Globals.mc.player.inventory.currentItem
            val pearl = InventoryUtils.findItem(Items.ENDER_PEARL)

            if (pearl == -1) {
                ChatManager.sendDeleteMessage("No pearl found on hotbar.", "pearl", ChatIDs.MIDDLE_CLICK_PEARL)
                return
            }

            InventoryUtils.swapSlot(pearl)
            val hand = if (Globals.mc.player.heldItemOffhand.item == Items.ENDER_PEARL) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
            Globals.mc.player.connection.sendPacket(CPacketPlayerTryUseItem(hand))

            InventoryUtils.swapSlot(lastSlot)
        }
    }

    private fun doFriendPlayer(name: String) {
        if (FriendManager.isFriend(name)) {
            val friend = FriendManager.getFriendByAliasOrLabel(name) ?: return
            FriendManager.remove(friend)
            ChatManager.sendDeleteMessage("Removed $name from your friend list", name, ChatIDs.FRIEND)
        } else {
            FriendManager.add(Friend(name, name))
            ChatManager.sendDeleteMessage("Added $name to your friend list", name, ChatIDs.FRIEND)
        }
    }

}
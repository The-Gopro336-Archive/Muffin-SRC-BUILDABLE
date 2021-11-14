package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.render.FovModifierEvent
import me.han.muffin.client.event.events.render.RenderHandEvent
import me.han.muffin.client.event.events.render.item.RenderItemInFirstPersonEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object FovModule: Module("FovModifier", Category.OTHERS, true, "Allow you to control your viewmodel.") {

    private val fov = NumberValue(120F, 10F, 200F, 5F, "Fov")
    private val noBob = Value(false, "NoBob")
    private val noHand = Value(false, "NoHand")

    val handHeight = EnumValue(HandMode.Off, "HandHeight")

    val mainHeight = NumberValue({ handHeight.value == HandMode.Main || handHeight.value == HandMode.Both },1.0F, -1.0F, 1.0F, 0.1F, "MainHeight")
    val offHeight = NumberValue({ handHeight.value == HandMode.Off || handHeight.value == HandMode.Both },0.5F, -1.0F, 1.0F, 0.1F, "OffHeight")

    private val mainWidth = NumberValue({ handHeight.value == HandMode.Main || handHeight.value == HandMode.Both },1.0F, -1.0F, 1.0F, 0.1F, "MainWidth")

    private val offWidth = NumberValue({ handHeight.value == HandMode.Off || handHeight.value == HandMode.Both },0.5F, -1.0F, 1.0F, 0.1F, "OffWidth")

    private val itemFov = Value(false, "ItemFov")
    private val itemFovValue = NumberValue({ itemFov.value },120F, 10F, 200F, 5F, "ItemFov")

 //   private val mainHandSwingAmount: NumberValue<Float> = NumberValue(1F, 0.0F, 10.0F, 0.1F, "MainSwing")
//    private val noHand: EnumValue<HandMode> = EnumValue(HandMode.Off, "NoHand")

    var cachedFov = 120F

    enum class HandMode {
        Off, Main, Offhand, Both
    }

    init {
        addSettings(fov, noBob, noHand, handHeight, mainHeight, offHeight, mainWidth, offWidth, itemFov, itemFovValue)
    }

    override fun onEnable() {
        cachedFov = Globals.mc.gameSettings.fovSetting
    }

    override fun onDisable() {
        Globals.mc.gameSettings.fovSetting = cachedFov
    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (fullNullCheck()) return

/*
        if (handHeight.value == HandMode.Main || handHeight.value == HandMode.Both)
            (Globals.mc.entityRenderer.itemRenderer as IItemRenderer).equippedProgressMainHand = mainHeight.value

        if (handHeight.value == HandMode.Offhand || handHeight.value == HandMode.Both)
            (Globals.mc.entityRenderer.itemRenderer as IItemRenderer).equippedProgressOffHand = offHeight.value

 */

        Globals.mc.gameSettings.fovSetting = fov.value

        if (noBob.value) Globals.mc.player.distanceWalkedModified = 0F
    }

    @Listener
    private fun onFovModifier(event: FovModifierEvent) {
        if (fullNullCheck()) return

        if (itemFov.value) {
            event.cancel()
            event.fov = itemFovValue.value
        }

    }

    /*
    @Listener
    private fun onRenderUpdateEquippedItem(event: RenderUpdateEquippedItemEvent) {
        if (Globals.mc.player == null || Globals.mc.world == null)
            return

        if (handHeight.value == HandMode.Off)
            return

        (Globals.mc.entityRenderer.itemRenderer as IItemRenderer).itemStackMainHand = Globals.mc.player.getHeldItem(EnumHand.MAIN_HAND)
        (Globals.mc.entityRenderer.itemRenderer as IItemRenderer).itemStackOffHand = Globals.mc.player.getHeldItem(EnumHand.OFF_HAND)

    }

     */

    @Listener
    private fun onRenderHand(event: RenderHandEvent) {
        if (noHand.value) event.cancel()
    }


    @Listener
    private fun onRenderItemInFirstPersonMain(event: RenderItemInFirstPersonEvent.MainHand) {
        if (handHeight.value == HandMode.Main || handHeight.value == HandMode.Both) {
            event.cancel()
            event.width -= 0 - mainWidth.value
            event.height -= 0 - mainHeight.value
        }
    }

    @Listener
    private fun onRenderItemInFirstPersonOff(event: RenderItemInFirstPersonEvent.OffHand) {
        if (handHeight.value == HandMode.Offhand || handHeight.value == HandMode.Both) {
            event.cancel()
            event.width -= 0 - offWidth.value
            event.height -= 0 - offHeight.value
        }
    }

    /*
    @Listener
    private fun onRenderHandInFirstPerson(event: RenderHandInFirstPersonEvent) {
        if (handHeight.value == HandMode.Main || handHeight.value == HandMode.Both) {
            event.cancel()
            event.x = offWidth.value
            event.y = offHeight.value
        }
    }
     */


}
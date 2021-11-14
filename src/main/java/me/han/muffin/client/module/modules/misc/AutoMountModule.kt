package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.MotionUpdateEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.passive.*
import net.minecraft.util.EnumHand
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object AutoMountModule: Module("AutoMount", Category.MISC, "Automatically attempts to mount an entity near you") {
    private val boats = Value(true, "Boats")
    private val horses = Value(true, "Horses")
    private val skeletonHorses = Value(true, "SkeletonHorses")
    private val donkeys = Value(true, "Donkeys")
    private val pigs = Value(true, "Pigs")
    private val llamas = Value(true, "Llamas")
    private val range = NumberValue(4, 0, 10, 1, "Range")
    private val delay = NumberValue(1, 0, 10, 1, "Delay")

    private val timer = Timer()

    init {
        addSettings(boats, horses, skeletonHorses, donkeys, pigs, llamas, range, delay)
    }

    @Listener
    private fun onPlayerUpdate(event: MotionUpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (Globals.mc.player.isRiding) return

        if (!timer.passedSeconds(delay.value)) return

        val ridingEntity = Globals.mc.world.loadedEntityList.filter { isValidEntity(it) }.minByOrNull { Globals.mc.player.getDistanceSq(it) } ?: return
        Globals.mc.playerController.interactWithEntity(Globals.mc.player, ridingEntity, EnumHand.MAIN_HAND)

        timer.reset()
    }

    private fun isValidEntity(entity: Entity): Boolean {
        if (entity.getDistance(Globals.mc.player) > range.value) return false
        if (entity is AbstractHorse && entity.isChild) return false

        if (entity is EntityBoat && boats.value) return true
        if (entity is EntitySkeletonHorse && skeletonHorses.value) return true
        if (entity is EntityHorse && horses.value) return true
        if (entity is EntityDonkey && donkeys.value) return true
        if (entity is EntityPig && pigs.value) return entity.saddled
        if (entity is EntityLlama && llamas.value && !entity.isChild) return true

        return false
    }


}
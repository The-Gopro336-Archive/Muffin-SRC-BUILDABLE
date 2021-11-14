package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.render.entity.*
import me.han.muffin.client.event.events.render.item.RenderItemEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object ChamsModule: Module("Chams", Category.RENDER, "Make entities visible through walls.") {

    private val players = Value(true, "Players")
    private val animals = Value(false, "Animals")
    private val mobs = Value(false, "Mobs")
    private val item = Value(true, "Items")
    private val crystal = Value(true, "Crystal")
    private val storage = Value(false, "Storage")
    private val invisibles = Value(true, "Invisibles")

    init {
        addSettings(players, animals, mobs, item, crystal, storage, invisibles)
    }

    private fun doPreChams() {
        glEnable(GL_POLYGON_OFFSET_FILL)
        glDepthRange(0.0, 0.01)
    }

    private fun doPostChams() {
        glDepthRange(0.0, 1.0)
        glDisable(GL_POLYGON_OFFSET_FILL)
    }

    private fun isEntityValid(entity: Entity?): Boolean {
        return entity != null && entity != Globals.mc.player && Globals.mc.renderViewEntity != entity && entity.isAlive
    }

    private fun doesQualify(entity: Entity): Boolean {
        return isEntityValid(entity) &&
                        (players.value && entity is EntityPlayer ||
                        mobs.value && (entity is EntityMob || entity is EntityVillager || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon) ||
                        animals.value && (entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityBat)) &&
                (!entity.isInvisible || invisibles.value)
    }

    private fun doChams(stage: EventStageable.EventStage) {
        if (stage == EventStageable.EventStage.PRE) {
            doPreChams()
        } else if (stage == EventStageable.EventStage.POST) {
            doPostChams()
        }
    }

    @Listener
    private fun onRenderEntity(event: RenderEntityEvent) {
        if (doesQualify(event.entity)) doChams(event.stage)
    }

    @Listener
    private fun onRenderItem(event: RenderItemEvent) {
        if (item.value) doChams(event.stage)
    }

    @Listener
    private fun onRenderEnderCrystal(event: RenderEnderCrystalEvent) {
        if (crystal.value) doChams(event.stage)
    }

    @Listener
    private fun onRenderTileEntityChest(event: RenderTileEntityChestEvent) {
        if (storage.value) doChams(event.stage)
    }

    @Listener
    private fun onRenderTileEntityEnderChest(event: RenderTileEntityEnderChestEvent) {
        if (storage.value) doChams(event.stage)
    }

    @Listener
    private fun onRenderTileEntityShulkerBox(event: RenderTileEntityShulkerBoxEvent) {
        if (storage.value) doChams(event.stage)
    }

}
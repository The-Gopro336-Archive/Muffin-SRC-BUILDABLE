package me.han.muffin.client.module.modules.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.color.DyeColours
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mixin.render.orientCamera
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.MathUtils.convertRange
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color

/**
 * @author han
 * Created by han on 30/6/2020
 */
internal object TracersModule : Module("Tracers", Category.RENDER, "Draw a tracer to an entity.") {

    private val players = Value(true, "Players")
    private val friends = Value(true, "Friends")
    private val invisibles = Value(false, "Invisibles")
    private val monster = Value(false, "Monster")
    private val animals = Value(false, "Animals")
    private val items = Value(false, "Items")
    private val vehicles = Value(false, "Vehicles")
    private val pearls = Value(false, "Pearls")

    private val colorMode = EnumValue(ColorMode.Distance, "ColorMode")
    private val targetMode = EnumValue(TargetMode.Feet, "TargetMode")
    private val designMode = EnumValue(DesignMode.Off, "DesignMode")

    private val thicknessValue: NumberValue<Float> = NumberValue(2F, 1F, 5F, 0.2F, "Thickness")

    private enum class DesignMode {
        Off, Stem, Fill
    }

    private enum class TargetMode {
        Head, Body, Feet
    }

    private enum class ColorMode {
        Client, Distance, Rainbow, DistanceRework
    }

    init {
        addSettings(players, friends, invisibles, monster, animals, items, vehicles, pearls, colorMode, targetMode, designMode, thicknessValue)
    }

    fun render(entity: Entity, partialTicks: Float, distance: Int) {
        RenderUtils.prepareGL3D()

        glLineWidth(thicknessValue.value)

        glLoadIdentity()
        val bobbing = Globals.mc.gameSettings.viewBobbing

        Globals.mc.gameSettings.viewBobbing = false
        Globals.mc.entityRenderer.orientCamera(partialTicks)

        glBegin(GL_LINES)

        val red = Muffin.getInstance().fontManager.publicRed
        val green = Muffin.getInstance().fontManager.publicGreen
        val blue = Muffin.getInstance().fontManager.publicBlue
        var alpha = 150

        if (entity is EntityLivingBase && entity.hurtTime > 0) {
            alpha += 15
        }

        alpha = alpha.coerceIn(0..255)

        var color = when (colorMode.value) {
            ColorMode.Client -> Color(red, green, blue, alpha)
            ColorMode.Distance -> Color(255 - distance, distance, 0, alpha)
            ColorMode.Rainbow -> ColourUtils.rainbow()
            ColorMode.DistanceRework -> Color(red, green, blue, alpha)
        }

        if (colorMode.value == ColorMode.DistanceRework) {
            val r = getRangedColor(entity, Colour(color), alpha = alpha.toFloat()).r
            val g = getRangedColor(entity, Colour(color), alpha = alpha.toFloat()).g
            val b = getRangedColor(entity, Colour(color), alpha = alpha.toFloat()).b
            val a = getRangedColor(entity, Colour(color), alpha = alpha.toFloat()).a
            color = Color(r, g, b, a)
        }

        if (friends.value && entity is EntityPlayer && FriendManager.isFriend(entity.name)) {
            color = Color(85, 255, 255, alpha)
        }

        drawTraces(entity, color, partialTicks)

        glEnd()

        Globals.mc.gameSettings.viewBobbing = bobbing

        RenderUtils.releaseGL3D()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        for (entity in Globals.mc.world.loadedEntityList) {
            if (entity == null) continue

            if (entity == Globals.mc.player) continue

            if (!doesQualifyEntity(entity)) continue
            if (!friends.value && entity is EntityPlayer && FriendManager.isFriend(entity.name)) continue

            var dist = (Globals.mc.player.getDistance(entity) * 2).toInt()
            if (dist > 255) dist = 255

            render(entity, event.partialTicks, dist)
        }

    }


    private fun doesQualifyEntity(entity: Entity): Boolean {
        return Globals.mc.renderViewEntity != entity && entity.isAlive && entity != Globals.mc.player &&
                ((entity is EntityPlayer && players.value) || (entity is EntityItem && items.value) ||
                        (entity is EntityEnderPearl && pearls.value) ||
                        ((entity is EntityMob || entity is EntitySlime || entity is EntityVillager) && monster.value) ||
                        (entity is EntityAnimal && animals.value) ||
                        ((entity is EntityBoat || entity is EntityMinecart || entity is EntityMinecartContainer) && vehicles.value))
                && (!entity.isInvisible || invisibles.value)
    }


    private fun drawTraces(entity: Entity, color: Color, partialTicks: Float) {

        // val interpVec3d : Vec3d = MathUtils.interpolateEntity(entity, partialTicks).add(ActiveRenderInfo.getCameraPosition())
        val vec = MathUtils.getInterpolatedRenderPos(entity, partialTicks)
        val x = vec.x
        val y = vec.y
        val z = vec.z

        val viewEntity = Globals.mc.renderViewEntity ?: Globals.mc.player
        val eyeVector = Vec3d(0.0, 0.0, 1.0)
            .rotatePitch(-(viewEntity.rotationPitch.toRadian()))
            .rotateYaw(-(viewEntity.rotationYaw.toRadian()))

        RenderUtils.glColor(color)

        glVertex3d(eyeVector.x, viewEntity.eyeHeight + eyeVector.y, eyeVector.z)

        if (targetMode.value == TargetMode.Head)
            glVertex3d(x, y + entity.height - 0.18, z)
        else if (targetMode.value == TargetMode.Body)
            glVertex3d(x, y + entity.height / 2, z)
        else if (targetMode.value == TargetMode.Feet)
            glVertex3d(x, y, z)

        glVertex3d(x, y, z)

        when (designMode.value) {
            DesignMode.Stem -> glVertex3d(x, y + entity.height, z)
        }

    }

    private fun getRangedColor(entity: Entity, rgba: Colour, alpha: Float): Colour {
        val distance = Globals.mc.player.getDistance(entity)
        val colorFar = DyeColours.GREEN.color
        colorFar.a = 160
        val r = convertRange(distance, 0f, 55F, rgba.r.toFloat(), colorFar.r.toFloat()).toInt()
        val g = convertRange(distance, 0f, 55F, rgba.g.toFloat(), colorFar.g.toFloat()).toInt()
        val b = convertRange(distance, 0f, 55F, rgba.b.toFloat(), colorFar.b.toFloat()).toInt()
        val a = convertRange(distance, 0f, 55F, alpha, colorFar.a.toFloat()).toInt()
        return Colour(r, g, b, a)
    }

}
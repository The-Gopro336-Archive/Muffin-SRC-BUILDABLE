package me.han.muffin.client.module.modules.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.entity.living.EntityUseItemTickEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.player.FastBowModule
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.math.MathUtils.interpolateEntity
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.*
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object TrajectoriesModule: Module("Trajectories", Category.RENDER, "Draws lines to where trajectories are going to fall") {
    private var velocity = 0.0
    private var gravity = 0.0

    private var prevItemUseCount = 0

    private val items = hashSetOf<Class<*>>(
        ItemBow::class.java,
        ItemSplashPotion::class.java,
        ItemLingeringPotion::class.java,
        ItemExpBottle::class.java,
        ItemEnderPearl::class.java,
        ItemSnowball::class.java,
        ItemEgg::class.java,
        ItemFishingRod::class.java
    )

    @Listener
    private fun onEntityUseItemTickEvent(event: EntityUseItemTickEvent) {
        if (fullNullCheck() || event.entity != Globals.mc.player) return
        prevItemUseCount = Globals.mc.player.itemInUseCount
    }

    fun getInterpolatedCharge() = prevItemUseCount.toDouble() + (Globals.mc.player.itemInUseCount - prevItemUseCount) * RenderUtils.renderPartialTicks

    private fun checkHandItem(class_: Class<*>): Boolean {
        return class_.isInstance(Globals.mc.player.getHeldItem(EnumHand.MAIN_HAND).item) || class_.isInstance(Globals.mc.player.getHeldItem(EnumHand.OFF_HAND).item)
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (!items.any { checkHandItem(it) }) return

        val mainHandItem = Globals.mc.player.heldItemMainhand.item
        val offHandItem = Globals.mc.player.heldItemMainhand.item

        var holdingBow = mainHandItem is ItemBow || offHandItem is ItemBow

        var holdingThrowable = mainHandItem is ItemSplashPotion ||
                offHandItem is ItemSplashPotion ||
                mainHandItem is ItemLingeringPotion ||
                offHandItem is ItemLingeringPotion ||
                mainHandItem is ItemExpBottle ||
                offHandItem is ItemExpBottle

        if (mainHandItem is ItemBow || mainHandItem is ItemSnowball || mainHandItem is ItemEnderPearl || mainHandItem is ItemEgg) {
            holdingThrowable = false
        }

        if (mainHandItem is ItemSplashPotion ||
            mainHandItem is ItemLingeringPotion ||
            mainHandItem is ItemSnowball ||
            mainHandItem is ItemEnderPearl ||
            mainHandItem is ItemEgg ||
            mainHandItem is ItemExpBottle) {
            holdingBow = false
        }

        if (holdingThrowable && (mainHandItem is ItemPotion || offHandItem is ItemPotion)) {
            velocity = 0.5
            gravity = 0.05
        } else if (holdingThrowable &&
            (mainHandItem is ItemExpBottle || offHandItem is ItemExpBottle)) {
            velocity = 0.7
            gravity = 0.07
        } else if (holdingBow) {
            velocity = 1.5
            gravity = 0.05
        } else if (Globals.mc.player.heldItemMainhand.item is ItemFishingRod || Globals.mc.player.heldItemOffhand.item is ItemFishingRod) {
            velocity = 1.5
            gravity = 0.15
        } else {
            velocity = 1.5
            gravity = 0.03
        }

        val player = Globals.mc.player
        val throwingYaw = player.rotationYaw
        val throwingPitch = player.rotationPitch

        val renderPos = interpolateEntity(player, event.partialTicks)

        //     double posX = Globals.mc.player.posX - MathHelper.cos(throwingYaw / 180.0f * 3.1415927F) * 0.16f;
        //     double posY = Globals.mc.player.posY + Globals.mc.player.getEyeHeight() - 0.1000000014901161D;
        //     double posZ = Globals.mc.player.posZ - MathHelper.sin(throwingYaw / 180.0f * 3.1415927F) * 0.16f;
        var posX = renderPos.x - cos(throwingYaw.toRadian()) * 0.16F
        var posY = renderPos.y + player.eyeHeight - 0.10000000149011612
        var posZ = renderPos.z - sin(throwingYaw.toRadian()) * 0.16F

        val holdingBowMultiplier = if (holdingBow) 1.0 else 0.4
        var motionX = (-sin(throwingYaw.toRadian()) * cos(throwingPitch.toRadian()) * holdingBowMultiplier)
        var motionY = (-sin((throwingPitch - (if (holdingThrowable) 20 else 0)).toRadian()) * holdingBowMultiplier)
        var motionZ = (cos(throwingYaw.toRadian()) * cos(throwingPitch.toRadian()) * holdingBowMultiplier)

        if (!Globals.mc.player.onGround && !holdingBow) motionY += Globals.mc.player.motionY

        val itemUseCount = FastBowModule.bowCharge ?: if (Globals.mc.player.isHandActive) getInterpolatedCharge() else 0.0
        val useDuration = (72000 - itemUseCount) / 20.0

        var power = (useDuration.pow(2) + useDuration * 2.0) / 3.0

        if (power < 0.1000000014901161) return
        if (power > 1.0F) power = 1.0

        val distance = sqrt(motionX.pow(2) + motionY.pow(2) + motionZ.pow(2))

        motionX /= distance
        motionY /= distance
        motionZ /= distance

        motionX *= (if (holdingBow) power * 2.0 else 1.0) * velocity
        motionY *= (if (holdingBow) power * 2.0 else 1.0) * velocity
        motionZ *= (if (holdingBow) power * 2.0 else 1.0) * velocity

        GlStateManager.pushMatrix()

        RenderUtils.prepareGL3D()

        GlStateUtils.texture2d(false)
        GlStateUtils.alpha(true)
        GlStateUtils.blend(true)

        glLineWidth(1.5F)
        RenderUtils.glColorClient(255)


        var hasLanded = false
        val hitEntity: Entity? = null
        var landingPosition: RayTraceResult? = null

        glBegin(GL_LINE_STRIP)
        while (!hasLanded && posY > 0.0) {
            val present = Vec3d(posX, posY, posZ)
            val future = Vec3d(posX + motionX, posY + motionY, posZ + motionZ)

            val possibleLandingStrip = Globals.mc.world.rayTraceBlocks(present, future, false, true, false)

            if (possibleLandingStrip != null) {
                if (possibleLandingStrip.typeOfHit != RayTraceResult.Type.MISS) {
                    landingPosition = possibleLandingStrip
                    hasLanded = true
                }
            } else {
                val entityHit = getEntityHit(present, future)
                if (entityHit != null) {
                    landingPosition = RayTraceResult(entityHit)
                    hasLanded = true
                }
            }

            posX += motionX
            posY += motionY
            posZ += motionZ

            val motionAdjustmentHorizontal = 0.9900000095367432 //0.99F;

            motionX *= motionAdjustmentHorizontal
            motionY *= motionAdjustmentHorizontal
            motionZ *= motionAdjustmentHorizontal

            motionY -= gravity
            glVertex3d(posX - RenderUtils.renderPosX, posY - RenderUtils.renderPosY, posZ - RenderUtils.renderPosZ)
        }

        glEnd()
        glPushMatrix()

        glTranslated(posX - RenderUtils.renderPosX, posY - RenderUtils.renderPosY, posZ - RenderUtils.renderPosZ)

        if (landingPosition != null && landingPosition.typeOfHit == RayTraceResult.Type.BLOCK) {

            when (landingPosition.sideHit.index) {
                1 -> glRotatef(180.0f, 1.0f, 0.0f, 0.0f)
                2 -> glRotatef(90.0f, 1.0f, 0.0f, 0.0f)
                3 -> glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
                4 -> glRotatef(-90.0f, 0.0f, 0.0f, 1.0f)
                5 -> glRotatef(90.0f, 0.0f, 0.0f, 1.0f)
            }

            glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)

            RenderUtils.drawBorderedRectReliant(-0.6f, -0.6f, 0.6f, 0.6f, 0.3f, ColourUtils.toRGBAClient(120), ColourUtils.toRGBAClient(120))
        }

        glPopMatrix()

        if (landingPosition != null && landingPosition.typeOfHit == RayTraceResult.Type.ENTITY) {
            glTranslated(-RenderUtils.renderPosX, -RenderUtils.renderPosY, -RenderUtils.renderPosZ)
            // RenderUtils.glColorClient(0.17F)

            val target = landingPosition.entityHit
            val bb = target.entityBoundingBox

            //       final double h = 1.697596633E-314;
            //       final AxisAlignedBB grow = bb.grow(h, 1.0, h);
            val grow = bb.offset(0.0, 0.1, 0.0).expand(0.0, 0.2, 0.0)
            RenderUtils.drawBBESP(grow, Muffin.getInstance().fontManager.publicRed, Muffin.getInstance().fontManager.publicGreen, Muffin.getInstance().fontManager.publicBlue, 44)
           // RenderUtils.drawBoxESP(grow, Colour().clientColour(44))

            glTranslated(RenderUtils.renderPosX, RenderUtils.renderPosY, RenderUtils.renderPosZ)
        }

        GlStateUtils.alpha(false)
        GlStateUtils.blend(false)
        RenderUtils.releaseGL3D()
        GlStateUtils.matrix(false)

    }

    private fun getEntityHit(present: Vec3d, future: Vec3d): Entity? {

        for (entity in getEntities()) {
            if (entity == Globals.mc.player) continue
            if (!entity.canBeCollidedWith()) continue
            //    double d2 = 1.3262473694E-314;
            //     if (entity.getEntityBoundingBox().grow(d2, d2, d2).calculateIntercept(present, future) == null) continue;
            val expander = 0.30000001192092896
            if (entity.entityBoundingBox.expand(expander, expander, expander).calculateIntercept(present, future) == null)
                continue

            return entity
        }
        return null
    }


    private fun getEntities(): ArrayList<EntityLivingBase> {
        val list = ArrayList<EntityLivingBase>()
        for (entity in Globals.mc.world.loadedEntityList) {
            if (entity == Globals.mc.player || entity !is EntityLivingBase) continue
            list.add(entity)
        }
        return list
    }

    /*
    private fun getEntities(bb: AxisAlignedBB): ArrayList<Entity> {
        val list = ArrayList<Entity>()

        val chunkMinX = floor(((bb.minX - 2.0) / 16.0)).toInt()
        val chunkMaxX = floor(((bb.maxX + 2.0) / 16.0)).toInt()
        val chunkMinZ = floor(((bb.minZ - 2.0) / 16.0)).toInt()
        val chunkMaxZ = floor(((bb.maxZ + 2.0) / 16.0)).toInt()

        for (x in chunkMinX..chunkMaxX) {
            for (z in chunkMinZ..chunkMaxZ) {
                if (Globals.mc.world.chunkProvider.getLoadedChunk(x, z) == null) continue
                Globals.mc.world.getChunk(x, z).getEntitiesWithinAABBForEntity(Globals.mc.player, bb, list, null)
            }
        }

        return list
    }
     */

    fun drawRectReliant(x: Float, y: Float, x2: Float, y2: Float, w: Float, firstColour: Int, secondColour: Int) {
        startGL2D()
        RenderUtils.glColor(firstColour)
        quickDraw(x + w, y + w, x2 - w, y2 - w)
        RenderUtils.glColor(secondColour)
        quickDraw(x + w, y, x2 - w, y + w)
        quickDraw(x, y, x + w, y2)
        quickDraw(x2 - w, y, x2, y2)
        quickDraw(x + w, y2 - w, x2 - w, y2)
        endGL2D()
    }

    fun quickDraw(x: Float, y: Float, x2: Float, y2: Float) {
        glBegin(GL_QUADS)
        glVertex2f(x, y2)
        glVertex2f(x2, y2)
        glVertex2f(x2, y)
        glVertex2f(x, y)
        glEnd()
    }

    fun startGL2D() {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(true)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST)
    }

    fun endGL2D() {
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE)
        glHint(GL_POLYGON_SMOOTH_HINT, GL_DONT_CARE)
    }

    fun startGL3D() {
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glPushMatrix()
        glDisable(GL_ALPHA_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glEnable(GL_CULL_FACE)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST)
        glDisable(GL_LIGHTING)
    }

    fun endGL3D() {
        glEnable(GL_LIGHTING)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_ALPHA_TEST)
        glDepthMask(true)
        glCullFace(GL_BACK)
        glPopMatrix()
        glPopAttrib()
    }


}
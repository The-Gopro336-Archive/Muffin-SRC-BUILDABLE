package me.han.muffin.client.module.modules.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.*
import me.han.muffin.client.event.events.render.entity.HurtCamEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityTeamColorEvent
import me.han.muffin.client.event.events.render.item.RenderEnchantedEvent
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.extensions.mc.utils.component1
import me.han.muffin.client.utils.extensions.mc.utils.component2
import me.han.muffin.client.utils.extensions.mc.utils.component3
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.OutlineUtils
import me.han.muffin.client.utils.render.ProjectionUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.render.shader.shaders.GlowShader
import me.han.muffin.client.utils.render.shader.shaders.OutlineShader
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.Gui
import net.minecraft.client.model.ModelPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object EntityESPModule: Module("EntityESP", Category.RENDER, "Highlights entities with different render methods.") {

    private val renderMode = EnumValue(Mode.Outline, "RenderMode")
    private val players = Value(true, "Players")
    private val mobs = Value(false, "Mobs")
    private val animals = Value(false, "Animals")
    private val invisibles = Value(true, "Invisibles")
    private val experience = Value(true, "EXP")
    private val pearl = Value(true, "EnderPearl")
    private val crystal = Value(true, "Crystal")
    private val item = Value(true, "Item")
    private val itemText = Value(false, "ItemText")
    private val itemTextScale = NumberValue({ itemText.value },1.0F, 0.0F, 4.0F, 0.1F, "ItemTextScale")
    val skeletons = Value(true, "Skeletons")

    var alpha = NumberValue(255, 0, 255, 1, "Alpha")

    private val width = NumberValue(0.5f, 0.1f, 5.0f, 0.1f, "Width")

    @JvmField var renderNameTags = true

    private var renderEntity: Entity? = null

    var fancyGraphics = Globals.mc.gameSettings.fancyGraphics
    var gamma = Globals.mc.gameSettings.gammaSetting

    init {
        addSettings(renderMode, players, mobs, animals, invisibles, experience, pearl, crystal, item, itemText, skeletons, width, alpha)
    }

    enum class Mode {
        Box, Full, ShaderGlow, Outline, ShaderOutline, Csgo
    }


    override fun getHudInfo(): String {
        if (renderMode.value == Mode.ShaderGlow || renderMode.value == Mode.ShaderOutline || renderMode.value == Mode.Outline) return "Outline"
        if (renderMode.value == Mode.Box || renderMode.value == Mode.Full) return "Solid"
        return if (renderMode.value == Mode.Csgo) "CS:GO" else renderMode.fixedValue
    }

    private fun getColour(entity: Entity?): Colour {
        var colour = ColourUtils.getClientColour(alpha.value)
        if (entity != null && entity is EntityPlayer && FriendManager.isFriend(entity.getName())) colour = ColourUtils.getFriendColour(alpha.value)
        return colour
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (skeletons.value) {
            if (Globals.mc.renderManager == null || Globals.mc.renderManager.options == null) return

            startEnd(true)
            glEnable(2903)
            glDisable(GL_LINE_SMOOTH)

            entities.keys.removeIf { doesntContain(it) }

            Globals.mc.world.playerEntities.forEach { drawSkeleton(event, it) }

            Gui.drawRect(0, 0, 0, 0, 0)
            startEnd(false)
        }

        if (renderMode.value == Mode.ShaderGlow || renderMode.value == Mode.ShaderOutline) return

        Globals.mc.world.loadedEntityList.forEach {
            val colour = getColour(it)
            val (r, g, b) = colour

            val (posX, posY, posZ) = MathUtils.getInterpolatedRenderPos(it, event.partialTicks)
            var alpha = alpha.value

            if (it is EntityLivingBase && it.hurtTime > 0) alpha += 10
            if (alpha > 255) alpha = 255

            val bb = AxisAlignedBB(0.0, 0.0, 0.0, it.width.toDouble(), it.height.toDouble(), it.width.toDouble())
                .offset(posX - it.width / 2.0F, posY, posZ - it.width / 2.0F)

            if (renderMode.value == Mode.Box && doesQualifyAll(it)) {
                RenderUtils.drawBoxESP(bb, r, g, b, alpha)
            } else if (renderMode.value == Mode.Full && doesQualifyAll(it)) {
                RenderUtils.drawBoxESP(bb, r, g, b, 40)
                RenderUtils.drawBoxOutlineESP(bb, r, g, b, 255, width.value)
            } else if (renderMode.value == Mode.Outline && doesQualifyExpPearlItem(it)) {
                RenderUtils.drawBoxOutlineESP(bb, r, g, b, 230, 1f)
            } else if (renderMode.value == Mode.Csgo && doesQualifyAll(it)) {
                RenderUtils.draw2D(it, posX, posY, posZ, colour, Colour(0, 0, 0, 255))
            }

        }
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        if (item.value && itemText.value) drawItemText(event)

        val shader = when (renderMode.value) {
            Mode.ShaderOutline -> OutlineShader.OUTLINE_SHADER
            Mode.ShaderGlow -> GlowShader.GLOW_SHADER
            else -> null
        } ?: return

        val radius = if (renderMode.value == Mode.ShaderOutline) width.value else if (renderMode.value == Mode.ShaderGlow) 0.5F + width.value else 1F
        val entityMap = HashMap<Pair<Entity, Render<Entity>>, Colour>()

        renderNameTags = false

        try {
            for (entity in Globals.mc.world.loadedEntityList) {
                if (!doesQualifyAll(entity)) continue
                val entityRenderObject = Globals.mc.renderManager.getEntityRenderObject<Entity>(entity) ?: continue
                val colour = getColour(entity)
                entityMap[entity to entityRenderObject] = colour
            }

            entityMap.forEach { (entity, rendering), colour ->
                val (r, g, b, a) = colour
                shader.startDraw(event.partialTicks)

                val vector = MathUtils.getInterpolatedRenderPos(entity, event.partialTicks)
                rendering.doRender(entity, vector.x, vector.y, vector.z, entity.rotationYaw, event.partialTicks)
                renderEntity = entity

                shader.stopDraw(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat(), radius, 1F)
            }

        } catch (ex: Exception) {
            Muffin.LOGGER.info("An error occurred while rendering all entities for shader esp", ex)
        }

        renderNameTags = true
    }


    @Listener
    private fun onRenderOutline(event: OutlineEvent) {
        if (fullNullCheck()) return

        if (renderMode.value != Mode.Outline) return

        OutlineUtils.checkSetupFBO()

        preRenderOutline(event.partialTicks)
        OutlineUtils.renderOne(width.value)
        preRenderOutline(event.partialTicks)
        OutlineUtils.renderTwo()
        postRenderOutline(event.partialTicks)
        OutlineUtils.renderThree()
        OutlineUtils.renderFour()
        postRenderOutline(event.partialTicks)
        OutlineUtils.renderFive()
        GlStateUtils.resetColour()
    }

    private fun drawItemText(event: Render2DEvent) {
        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.depth(false)

        Globals.mc.world.loadedEntityList
            .filter { EntityItem::class.java.isInstance(it) }
            .map { EntityItem::class.java.cast(it) }
            .filter { it.ticksExisted > 1 }
            .forEach {
                GlStateUtils.rescale(Globals.mc.displayWidth.toDouble(), Globals.mc.displayHeight.toDouble())

                val bottomPos = MathUtils.interpolateEntity(it, event.partialTicks)
                val topPos = bottomPos.add(0.0, it.renderBoundingBox.maxY - it.posY, 0.0)

                val top = ProjectionUtils.toScreenPos(topPos)
                val bot = ProjectionUtils.toScreenPos(bottomPos)

                val offX = bot.x - top.x
                val offY = bot.y - top.y

                GlStateUtils.matrix(true)

                glTranslated(top.x - offX / 2.0, bot.y, 0.0)
                glScalef(itemTextScale.value * 2.0f, itemTextScale.value * 2.0f, 1.0f)

                val stack = it.item
                val text = stack.displayName + if (stack.isStackable) " x" + stack.count else ""

                Muffin.getInstance().fontManager.drawStringWithShadow(
                    text, (offX / 2.0 - Muffin.getInstance().fontManager.getStringWidth(text) / 2.0).toFloat(),
                    (-(offY - Muffin.getInstance().fontManager.stringHeight / 2.0)).toInt() - 1.toFloat(), ColourUtils.Colors.WHITE
                )

                GlStateUtils.matrix(false)
                GlStateUtils.rescaleMc()
            }

        GlStateUtils.depth(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.blend(false)
        GlStateUtils.resetColour()
        GlStateUtils.matrix(false)
    }

    private fun preRenderOutline(partialTicks: Float) {
        for (entity in Globals.mc.world.loadedEntityList) {
            if (!doesQualifyPlayerMobAnimal(entity)) continue

            RenderHelper.enableStandardItemLighting()
            val combinedLight = Globals.mc.world.getCombinedLight(entity.position, 0)
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combinedLight % 65536F, combinedLight / 65536F)

            GlStateUtils.resetColour()

            renderNameTags = false

            val entityRenderObject = Globals.mc.renderManager.getEntityRenderObject<Entity>(entity) ?: continue

            val vector = MathUtils.getInterpolatedRenderPos(entity, partialTicks)
            entityRenderObject.doRender(entity, vector.x, vector.y, vector.z, entity.rotationYaw, partialTicks)

            renderEntity = entity
            renderNameTags = true
        }
    }

    private fun postRenderOutline(partialTicks: Float) {
        for (entity in Globals.mc.world.loadedEntityList) {
            if (!doesQualifyPlayerMobAnimal(entity)) continue

            RenderHelper.enableStandardItemLighting()
            val combinedLight = Globals.mc.world.getCombinedLight(entity.position, 0)
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combinedLight % 65536F, combinedLight / 65536F)

            val colour = getColour(entity)
            RenderUtils.glColor(colour)

            renderNameTags = false

            val entityRenderObject = Globals.mc.renderManager.getEntityRenderObject<Entity>(entity) ?: continue

            val vector = MathUtils.getInterpolatedRenderPos(entity, partialTicks)
            entityRenderObject.doRender(entity, vector.x, vector.y, vector.z, entity.rotationYaw, partialTicks)

            renderEntity = entity
            renderNameTags = true
        }
    }

    private fun isEntityValid(entity: Entity?): Boolean {
        return entity != null && entity != Globals.mc.player && Globals.mc.renderViewEntity != entity && entity.isAlive
    }

    private fun doesQualifyAll(entity: Entity?): Boolean {
        return (isEntityValid(entity) &&
                        (players.value && entity is EntityPlayer ||
                        item.value && entity is EntityItem  ||
                        experience.value && (entity is EntityXPOrb || entity is EntityExpBottle) ||
                        pearl.value && entity is EntityEnderPearl ||
                        mobs.value && (entity is EntityMob || entity is EntityVillager || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon) ||
                        animals.value && (entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityBat) ||
                        crystal.value && entity is EntityEnderCrystal) &&
                (!entity.isInvisible || invisibles.value))
    }

    private fun doesQualifyPlayerMobAnimal(entity: Entity?): Boolean {
        return (isEntityValid(entity) &&
                        (players.value && entity is EntityPlayer ||
                        mobs.value && (entity is EntityMob || entity is EntityVillager || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon) ||
                        animals.value && (entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityBat) ||
                        crystal.value && entity is EntityEnderCrystal) &&
                (!entity.isInvisible || invisibles.value))
    }

    private fun doesQualifyExpPearlItem(entity: Entity?): Boolean {
        return isEntityValid(entity) &&
                        (item.value && entity is EntityItem ||
                        experience.value && (entity is EntityXPOrb || entity is EntityExpBottle) ||
                        pearl.value && entity is EntityEnderPearl)
    }

    private val camera: ICamera = Frustum()
    private val entities = HashMap<EntityPlayer, Array<FloatArray>>()

    private fun drawSkeleton(event: Render3DEvent, e: EntityPlayer) {
        val viewEntity = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val vector = MathUtils.interpolateEntity(viewEntity, event.partialTicks)

        val colour = getColour(e)
        val (red, green, blue) = colour

        camera.setPosition(vector.x, vector.y, vector.z)
        val entPos = entities[e]

        if (entPos != null && e.isAlive && camera.isBoundingBoxInFrustum(e.entityBoundingBox) && e != Globals.mc.player && !e.isPlayerSleeping) {
            glPushMatrix()
            glEnable(2848)
            glLineWidth(1.0f)
            RenderUtils.glColor(red, green, blue, 255)

            val (x, y, z) = MathUtils.getInterpolatedRenderPos(e, event.partialTicks)

            glTranslated(x, y, z)
            val xOff = e.prevRenderYawOffset + (e.renderYawOffset - e.prevRenderYawOffset) * event.partialTicks
            glRotatef(-xOff, 0.0f, 1.0f, 0.0f)
            glTranslated(0.0, 0.0, if (e.isSneaking) -0.235 else 0.0)
            val yOff = if (e.isSneaking) 0.6f else 0.75f
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(-0.125, yOff.toDouble(), 0.0)
            if (entPos[3][0] != 0.0f) glRotatef(entPos[3][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
            if (entPos[3][1] != 0.0f) glRotatef(entPos[3][1] * 57.295776f, 0.0f, 1.0f, 0.0f)
            if (entPos[3][2] != 0.0f) glRotatef(entPos[3][2] * 57.295776f, 0.0f, 0.0f, 1.0f)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, -yOff.toDouble(), 0.0)
            glEnd()
            glPopMatrix()
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(0.125, yOff.toDouble(), 0.0)
            if (entPos[4][0] != 0.0f) glRotatef(entPos[4][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
            if (entPos[4][1] != 0.0f) glRotatef(entPos[4][1] * 57.295776f, 0.0f, 1.0f, 0.0f)
            if (entPos[4][2] != 0.0f) glRotatef(entPos[4][2] * 57.295776f, 0.0f, 0.0f, 1.0f)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, -yOff.toDouble(), 0.0)
            glEnd()
            glPopMatrix()
            glTranslated(0.0, 0.0, if (e.isSneaking) 0.25 else 0.0)
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(0.0, if (e.isSneaking) -0.05 else 0.0, if (e.isSneaking) -0.01725 else 0.0)
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(-0.375, yOff + 0.55, 0.0)
            if (entPos[1][0] != 0.0f) glRotatef(entPos[1][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
            if (entPos[1][1] != 0.0f) glRotatef(entPos[1][1] * 57.295776f, 0.0f, 1.0f, 0.0f)
            if (entPos[1][2] != 0.0f) glRotatef(-entPos[1][2] * 57.295776f, 0.0f, 0.0f, 1.0f)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, -0.5, 0.0)
            glEnd()
            glPopMatrix()
            glPushMatrix()
            glTranslated(0.375, yOff + 0.55, 0.0)
            if (entPos[2][0] != 0.0f) glRotatef(entPos[2][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
            if (entPos[2][1] != 0.0f) glRotatef(entPos[2][1] * 57.295776f, 0.0f, 1.0f, 0.0f)
            if (entPos[2][2] != 0.0f) glRotatef(-entPos[2][2] * 57.295776f, 0.0f, 0.0f, 1.0f)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, -0.5, 0.0)
            glEnd()
            glPopMatrix()
            glRotatef(xOff - e.rotationYawHead, 0.0f, 1.0f, 0.0f)
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(0.0, yOff + 0.55, 0.0)
            if (entPos[0][0] != 0.0f) glRotatef(entPos[0][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, 0.3, 0.0)
            glEnd()
            glPopMatrix()
            glPopMatrix()
            glRotatef(if (e.isSneaking) 25.0f else 0.0f, 1.0f, 0.0f, 0.0f)
            glTranslated(0.0, if (e.isSneaking) -0.16175 else 0.0, if (e.isSneaking) -0.48025 else 0.0)
            glPushMatrix()
            glTranslated(0.0, yOff.toDouble(), 0.0)
            glBegin(3)
            glVertex3d(-0.125, 0.0, 0.0)
            glVertex3d(0.125, 0.0, 0.0)
            glEnd()
            glPopMatrix()
            glPushMatrix()
            RenderUtils.glColor(red, green, blue, 255)
            glTranslated(0.0, yOff.toDouble(), 0.0)
            glBegin(3)
            glVertex3d(0.0, 0.0, 0.0)
            glVertex3d(0.0, 0.55, 0.0)
            glEnd()
            glPopMatrix()
            glPushMatrix()
            glTranslated(0.0, yOff + 0.55, 0.0)
            glBegin(3)
            glVertex3d(-0.375, 0.0, 0.0)
            glVertex3d(0.375, 0.0, 0.0)
            glEnd()
            glPopMatrix()
            glPopMatrix()
        }
    }

    private fun startEnd(revert: Boolean) {
        if (revert) {
            GlStateManager.pushMatrix()
            GlStateManager.enableBlend()
            glEnable(2848)
            GlStateManager.disableDepth()
            GlStateManager.disableTexture2D()
            glHint(3154, 4354)
        } else {
            GlStateManager.disableBlend()
            GlStateManager.enableTexture2D()
            glDisable(2848)
            GlStateManager.enableDepth()
            GlStateManager.popMatrix()
        }
        GlStateManager.depthMask(!revert)
    }

    @JvmStatic
    fun addEntity(e: EntityPlayer, model: ModelPlayer) {
        entities[e] = arrayOf(floatArrayOf(model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleY, model.bipedHead.rotateAngleZ), floatArrayOf(model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ), floatArrayOf(model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ), floatArrayOf(model.bipedRightLeg.rotateAngleX, model.bipedRightLeg.rotateAngleY, model.bipedRightLeg.rotateAngleZ), floatArrayOf(model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ))
    }

    private fun doesntContain(player: EntityPlayer): Boolean {
        return !Globals.mc.world.playerEntities.contains(player)
    }

    @Listener
    private fun onRenderEntityColor(event: RenderEntityTeamColorEvent) {
        if (fullNullCheck()) return
        var colour = ColourUtils.getClientColour(255)
        if (!renderNameTags) {
            if (renderEntity != null) colour = getColour(renderEntity!!)
            event.color = colour.getRGB()
            event.cancel()
        }
    }

    @Listener
    private fun onRenderEntityLayer(event: RenderEntityLayerEvent) {
        if (!renderNameTags) event.cancel()
    }

    @Listener
    private fun onRenderEnchanted(event: RenderEnchantedEvent) {
        if (!renderNameTags) event.cancel()
    }

    @Listener
    private fun onHurtCam(event: HurtCamEvent) {
        if (!renderNameTags) event.cancel()
    }

    @Listener
    private fun onRenderLeash(event: RenderLeashEvent) {
        if (!renderNameTags) event.cancel()
    }

}
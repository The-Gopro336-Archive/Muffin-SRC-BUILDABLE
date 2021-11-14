package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.render.RenderArmsEvent
import me.han.muffin.client.event.events.render.entity.RenderCrystalModelEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityModelEvent
import me.han.muffin.client.gui.font.util.Opacity
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.other.ItemRender
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.texture.TextureMap
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
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color

internal object ChamsRewriteModule: Module("CoolChams", Category.RENDER, "Render cool chams to entity.") {

    private val mode = EnumValue(EntityMode.Filled, "EntityMode")
    private val crystalMode = EnumValue(CrystalMode.Filled, "CrystalMode")
    private val renderFill = Value({ mode.value == EntityMode.Dot }, false, "RenderFill")
    private val crystalRenderFill = Value({ crystalMode.value == CrystalMode.Dot }, false, "CrystalRenderFill")
    private val renderEntity = Value(false, "RenderEntity")
    private val renderCrystal = Value(false, "RenderCrystal")

    private val shine = Value(false, "Shine")
    private val shineXQZ = Value(false, "ShineXQZ")

    private val shineFactor = NumberValue({ shine.value || shineXQZ.value },1, 1, 5, 1, "ShineFactor")
    private val shineSpeed = NumberValue({ shine.value || shineXQZ.value },1.0F, 0.0F, 20.0F, 0.1F, "ShineSpeed")
    private val shineScale = NumberValue({ shine.value || shineXQZ.value },1.0F, 0.1F, 20.0F, 0.1F, "ShineScale")

    private val texture = Value(false, "Texture")
    private val lightning = Value(false, "Lighting")

    private val self = Value(true, "Self")
    private val hands = Value(true, "Hand")
    private val players = Value(true, "Players")
    private val mobs = Value(false, "Mobs")
    private val animals = Value(false, "Animals")
    private val invisibles = Value(true, "Invisibles")
    private val crystals = Value(false, "Crystal")

    private val handRainbow = Value({ hands.value }, false, "HandRainbow")
    private val visibleRainbow = Value(false, "VisibleRainbow")
    private val invisibleRainbow = Value(false, "XQZRainbow")

    private val rainbowSpeed = NumberValue({ visibleRainbow.value || invisibleRainbow.value || handRainbow.value }, 3f, 1f, 15f, 0.05f, "RainbowSpeed")
    private val rainbowBrightness = NumberValue({ visibleRainbow.value || invisibleRainbow.value || handRainbow.value }, 0.6f, 0.25f, 1.0f, 0.05f, "RainbowBrightness")
    private val rainbowWidth = NumberValue({ visibleRainbow.value || invisibleRainbow.value || handRainbow.value }, 10f, 1f, 20f, 0.05f, "RainbowWidth")

    private val handRed = NumberValue({ hands.value && !handRainbow.value }, 90, 0, 255, 1, "HandRed")
    private val handGreen = NumberValue({ hands.value && !handRainbow.value }, 0, 0, 255, 1, "HandGreen")
    private val handBlue = NumberValue({ hands.value && !handRainbow.value }, 255, 0, 255, 1, "HandBlue")
    private val handAlpha = NumberValue({ hands.value }, 120, 0, 255, 5, "HandAlpha")

    private val vRed = NumberValue({ !visibleRainbow.value }, 90, 0, 255, 1, "VisibleRed")
    private val vGreen = NumberValue({ !visibleRainbow.value }, 0, 0, 255, 1, "VisibleGreen")
    private val vBlue = NumberValue({ !visibleRainbow.value }, 255, 0, 255, 1, "VisibleBlue")

    private val iRed = NumberValue({ !invisibleRainbow.value }, 90, 0, 255, 1, "XQZRed")
    private val iGreen = NumberValue({ !invisibleRainbow.value }, 0, 0, 255, 1, "XQZGreen")
    private val iBlue = NumberValue({ !invisibleRainbow.value }, 255, 0, 255, 1, "XQZBlue")

    private val lineWidth = NumberValue(1.0F, 0.1F, 5.0F, 0.1F, "LineWidth")
    private val dotSize = NumberValue(3.0F, 0.1F, 8.0F, 0.1F, "DotSize")

    private val visibleAlpha = NumberValue(120, 0, 255, 5, "VisibleAlpha")
    private val xqzAlpha = NumberValue(120, 0, 255, 5, "XQZAlpha")
    private val visibleLineAlpha = NumberValue(255, 0, 255, 5, "VisibleLineAlpha")
    private val xqzLineAlpha = NumberValue(255, 0, 255, 5, "XQZLineAlpha")

    private var h = 0F
    private val hue = Opacity(0)
    private var width = 0f
    private var rainbowColour = Colour()

    private val ENCHANTED_ITEM_GLINT_RES = ResourceLocation("textures/misc/enchanted_item_glint.png")

    init {
        addSettings(
            mode, crystalMode,
            renderFill, crystalRenderFill,
            renderEntity, renderCrystal,
            texture, lightning,
            shine, shineXQZ, shineFactor, shineSpeed, shineScale,
            self, players, mobs, animals, invisibles, crystals,
            //   ignoreArmour,
            hands, handRed, handGreen, handBlue, handAlpha,
            handRainbow, visibleRainbow, invisibleRainbow,
            rainbowSpeed, rainbowBrightness, rainbowWidth,
            vRed, vGreen, vBlue,
            iRed, iGreen, iBlue,
            visibleAlpha, xqzAlpha, visibleLineAlpha, xqzLineAlpha,
            lineWidth, dotSize
        )
    }

    enum class EntityMode {
        Normal, Filled, Cools, Solid, Wireframe, Dot, Full
    }

    enum class CrystalMode {
        Normal, Filled, Line, Dot, Full
    }

    private enum class PolygonMode {
        Wireframe, Solid, Dot
    }

    private fun getHandColour(): Colour {
        return if (handRainbow.value) rainbowColour else Colour(handRed.value, handGreen.value, handBlue.value, handAlpha.value)
    }

    private fun getVisibleColour(entity: Entity): Colour {
        var colour = if (visibleRainbow.value) rainbowColour else Colour(vRed.value, vGreen.value, vBlue.value, visibleAlpha.value)
        if (entity is EntityPlayer && FriendManager.isFriend(entity.getName())) colour = ColourUtils.getFriendColour(visibleAlpha.value)
        return colour
    }

    private fun getInvisibleColour(entity: Entity): Colour {
        var colour = if (invisibleRainbow.value) rainbowColour else Colour(iRed.value, iGreen.value, iBlue.value, xqzAlpha.value)
        if (entity is EntityPlayer && FriendManager.isFriend(entity.getName())) colour = ColourUtils.getFriendColour(xqzAlpha.value)
        return colour
    }

    private fun getVisibleLineColour(entity: Entity): Colour {
        var colour = if (visibleRainbow.value) rainbowColour else Colour(vRed.value, vGreen.value, vBlue.value, visibleLineAlpha.value)
        if (entity is EntityPlayer && FriendManager.isFriend(entity.getName())) colour = ColourUtils.getFriendColour(visibleLineAlpha.value)
        return colour
    }

    private fun getInvisibleLineColour(entity: Entity): Colour {
        var colour = if (invisibleRainbow.value) rainbowColour else Colour(vRed.value, vGreen.value, vBlue.value, xqzLineAlpha.value)
        if (entity is EntityPlayer && FriendManager.isFriend(entity.getName())) colour = ColourUtils.getFriendColour(xqzLineAlpha.value)
        return colour
    }

    private fun isEntityValid(entity: Entity?): Boolean {
        return entity != null && (self.value || entity != Globals.mc.player && Globals.mc.renderViewEntity != entity) && entity.isAlive
    }

    private fun doesQualify(entity: Entity?): Boolean {
        return (isEntityValid(entity) &&
                (players.value && entity is EntityPlayer  ||
                mobs.value && (entity is EntityMob || entity is EntityVillager || entity is EntitySlime || entity is EntityGhast || entity is EntityDragon) ||
                animals.value && (entity is EntityAnimal || entity is EntitySquid || entity is EntityGolem || entity is EntityBat)) &&
                (!entity.isInvisible || invisibles.value))
    }

    @Listener
    private fun onRenderCrystalModel(event: RenderCrystalModelEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (event.entity == null || !crystals.value) return

        val vColour = getVisibleColour(event.entity)
        val iColour = getInvisibleColour(event.entity)

        val lVColour = getVisibleColour(event.entity)
        val lXQZColour = getInvisibleLineColour(event.entity)

        ItemRender.doPreSmallCrystal()

        when (crystalMode.value) {
            CrystalMode.Filled -> doFilledChams(event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderCrystal.value)
            CrystalMode.Line -> doPolygonChams(EntityMode.Wireframe, event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderCrystal.value, crystalRenderFill.value)
            CrystalMode.Dot -> doPolygonChams(EntityMode.Dot, event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderCrystal.value, crystalRenderFill.value)
            CrystalMode.Full -> {
                doFilledChams(event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderCrystal.value)
                doPolygonChams(EntityMode.Wireframe, event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, lVColour, lXQZColour, renderCrystal.value, crystalRenderFill.value)
            }
        }

        ItemRender.doPostSmallCrystal()

        h += 20 - width
        event.cancel()
    }

    @Listener
    private fun onRenderEntityModel(event: RenderEntityModelEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (event.entity == null || !doesQualify(event.entity)) return

        val vColour = getVisibleColour(event.entity)
        val iColour = getInvisibleColour(event.entity)

        val lVColour = getVisibleColour(event.entity)
        val lXQZColour = getInvisibleLineColour(event.entity)

        ItemRender.doPreSmallEntity()

        if (mode.value == EntityMode.Solid || mode.value == EntityMode.Wireframe || mode.value == EntityMode.Dot) {
            doPolygonChams(
                mode.value, event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderEntity.value, renderFill.value
            )
        } else {
            when (mode.value) {
                EntityMode.Filled -> doFilledChams(
                    event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderEntity.value
                )
                EntityMode.Cools -> doCoolChams(
                    event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderEntity.value
                )
                EntityMode.Full -> {
                    doFilledChams(event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, vColour, iColour, renderEntity.value)
                    doPolygonChams(EntityMode.Wireframe, event.modelBase, event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale, lVColour, lXQZColour, renderEntity.value, renderFill.value)
                }
            }
        }

        ItemRender.doPostSmallEntity()

        h += 20 - width
        event.cancel()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        if (visibleRainbow.value || invisibleRainbow.value || handRainbow.value) {
            h = hue.opacity
            val speed = rainbowSpeed.value
            val brightness = rainbowBrightness.value
            hue.interp(256f, speed - 1.toDouble())
            if (hue.opacity > 255) hue.opacity = 0f
            width = rainbowWidth.value
            if (h > 255) h = 0f
            val preRainbow = Color.getHSBColor(h / 255.0f, brightness, 1.0f)
            rainbowColour = Colour(preRainbow.red, preRainbow.green, preRainbow.blue, visibleAlpha.value)
        }

    }

    @Listener
    private fun onRenderHand(event: RenderArmsEvent) {
        if (!hands.value) return
        event.cancel()
    }

    fun doHandChamsPre() {
        GlStateUtils.alpha(false)
        GlStateUtils.texture2d(false)
        if (!lightning.value) GlStateUtils.lighting(false)
        GlStateUtils.blend(true)
        if (ItemRender.isEnabled && ItemRender.handScaling.value) {
            glScaled(ItemRender.handScale.value, ItemRender.handScale.value, ItemRender.handScale.value)
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f)
        RenderUtils.glColor(getHandColour())
    }

    fun doHandChamsPost() {
        if (ItemRender.isEnabled && ItemRender.handScaling.value) {
            glScaled(1.0, 1.0, 1.0)
        }
        GlStateUtils.blend(true)
        if (!lightning.value) GlStateUtils.lighting(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.alpha(true)
        GlStateUtils.resetColour()
        h += 20 - width
    }

    private fun doPolygonChams(entityMode: EntityMode, modelBase: ModelBase, entity: Entity, limbSwing: Float, limbSwingAmount: Float, age: Float, headYaw: Float, headPitch: Float, scale: Float, visibleColour: Colour, invisibleColour: Colour, renderEntity: Boolean, renderFill: Boolean) {
        if (renderEntity) {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        glPushAttrib(GL_ALL_ATTRIB_BITS)

        when (entityMode) {
            EntityMode.Wireframe -> glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
            EntityMode.Solid -> glPolygonMode(GL_FRONT, GL_LINE)
            EntityMode.Dot -> if (renderFill) glPolygonMode(GL_FRONT, GL_POINT) else glPolygonMode(GL_FRONT_AND_BACK, GL_POINT)
        }

        if (!texture.value) GlStateUtils.texture2d(false)

        RenderHelper.enableStandardItemLighting()
        val combinedLight = Globals.mc.world.getCombinedLight(entity.position, 0)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combinedLight % 65536F, combinedLight / 65536F)

        if (!lightning.value) GlStateUtils.lighting(false)

        if (entityMode == EntityMode.Dot) {
            glEnable(GL_POINT_SMOOTH)
            glHint(GL_POINT_SMOOTH_HINT, GL_NICEST)
        } else {
            GlStateUtils.lineSmooth(true)
        }

        GlStateUtils.hintPolygon(true)
        GlStateUtils.smooth(true)

        GlStateUtils.blend(true)

        GlStateUtils.depth(false)
        GlStateUtils.depthMask(false)
        GlStateUtils.colorLock(true)
        RenderUtils.glColor(invisibleColour)

        if (entityMode == EntityMode.Dot) {
            glPointSize(dotSize.value)
        } else {
            glLineWidth(lineWidth.value)
        }

        if (shineXQZ.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            // Render the base model that we draw on top of
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        GlStateUtils.depth(true)
        GlStateUtils.depthMask(true)
        RenderUtils.glColor(visibleColour)

        // Render the base model that we draw on top of
        if (shine.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            // Render the base model that we draw on top of
            modelBase.render(
                entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale
            )
        }

        GlStateUtils.hintPolygon(false)
        GlStateUtils.smooth(false)

        if (entityMode == EntityMode.Dot) {
            glDisable(GL_POINT_SMOOTH)
        } else {
            GlStateUtils.lineSmooth(false)
        }

        GlStateUtils.blend(false)
        if (!texture.value) GlStateUtils.texture2d(true)
        if (!lightning.value) GlStateUtils.lighting(true)
        GlStateUtils.blend(false)
        GlStateUtils.colorLock(false)
        GlStateUtils.resetColour()
        glPopAttrib()
    }


    fun doFilledChams(modelBase: ModelBase, entity: Entity, limbSwing: Float, limbSwingAmount: Float, age: Float, headYaw: Float, headPitch: Float, scale: Float, visibleColour: Colour, invisibleColour: Colour, renderEntity: Boolean) {

        if (renderEntity) {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        val combinedLight = Globals.mc.world.getCombinedLight(entity.position, 0)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (combinedLight % 65536).toFloat(), (combinedLight / 65536).toFloat())

        if (!texture.value) GlStateUtils.texture2d(false)
        if (!lightning.value) GlStateUtils.lighting(false)

        GlStateUtils.blend(true)

        GlStateUtils.alpha(false)

        RenderUtils.glColor(invisibleColour)
        GlStateUtils.colorLock(true)

        GlStateUtils.depth(false)
        GlStateUtils.depthMask(false)

        glEnable(GL_POLYGON_OFFSET_FILL)
        GlStateManager.doPolygonOffset(1.0f, -1000000f)

        if (shineXQZ.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        GlStateManager.doPolygonOffset(1.0f, 1000000f)
        glDisable(GL_POLYGON_OFFSET_FILL)

        GlStateUtils.depthMask(true)
        if (!lightning.value) GlStateUtils.lighting(true)

        GlStateUtils.alpha(true)
        GlStateUtils.blend(false)

        GlStateUtils.resetColour()
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        RenderUtils.glColor(visibleColour)

        if (shine.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            // Render the base model that we draw on top of
            modelBase.render(
                entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale
            )
        }

        if (!texture.value) GlStateUtils.texture2d(true)
        GlStateUtils.blend(false)
        GlStateUtils.colorLock(false)
        GlStateUtils.resetColour()

    }

    private fun doCoolChams(modelBase: ModelBase, entity: Entity, limbSwing: Float, limbSwingAmount: Float, age: Float, headYaw: Float, headPitch: Float, scale: Float, visibleColour: Colour, invisibleColour: Colour, renderEntity: Boolean) {

        if (renderEntity) {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        glPushAttrib(GL_ALL_ATTRIB_BITS)
        GlStateUtils.alpha(false)
        if (!texture.value) GlStateUtils.texture2d(false)
        if (!lightning.value) GlStateUtils.lighting(false)
        GlStateUtils.blend(true)
        GlStateUtils.depthMask(false)

        glLineWidth(lineWidth.value)
        glEnable(GL_STENCIL_TEST)
        glClear(GL_STENCIL_BUFFER_BIT)
        glClearStencil(15)
        glStencilFunc(512, 1, 15)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT, GL_LINE)
        glStencilFunc(512, 0, 15)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT, GL_FILL)

        glStencilFunc(GL_EQUAL, 1, 15)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)

        glPolygonMode(GL_FRONT, GL_LINE)

        GlStateUtils.depth(false)
        GlStateUtils.depthMask(false)
        glEnable(GL_POLYGON_OFFSET_LINE)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f)

        glColor4f(1.0f, 0.0f, 0.0f, 1.0f)
        RenderUtils.glColor(invisibleColour)

        GlStateUtils.colorLock(true)

        if (shineXQZ.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        GlStateUtils.depth(true)
        GlStateUtils.depthMask(true)
        RenderUtils.glColor(visibleColour)

        if (shine.value) {
            renderEnchantEffect(modelBase, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale, invisibleColour)
        } else {
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        }

        GlStateUtils.blend(false)
        GlStateUtils.colorLock(false)
        if (!lightning.value) GlStateUtils.lighting(true)
        if (!texture.value) GlStateUtils.texture2d(true)
        GlStateUtils.alpha(true)
        glPopAttrib()
    }

    private fun renderEnchantEffect(modelBase: ModelBase, entity: Entity, limbSwing: Float, limbSwingAmount: Float, age: Float, headYaw: Float, headPitch: Float, scale: Float, colour: Colour) {
        val renderLiving = Globals.mc.renderManager.getEntityRenderObject<Entity>(entity) ?: return

        val newScale = 0.33333334F * shineScale.value
        val f = entity.ticksExisted + RenderUtils.renderPartialTicks
        Globals.mc.entityRenderer.setupFogColor(true)
        GlStateManager.enableTexture2D()
        renderLiving.bindTexture(ENCHANTED_ITEM_GLINT_RES)
        GlStateManager.depthFunc(GL_EQUAL)
        GlStateManager.color(0.5f, 0.5f, 0.5f, colour.a / 255F)

        for (i in 0 until shineFactor.value) {
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE)
            RenderUtils.glColor(colour)
            //       GlStateManager.color(0.38f, 0.19f, 0.608f, 1.0f)
            GlStateManager.matrixMode(GL_TEXTURE)
            GlStateManager.loadIdentity()
            GlStateManager.scale(newScale, newScale, newScale)
            GlStateManager.rotate(30.0f - i * 60.0f, 0.0f, 0.0f, 1.0f)
            GlStateManager.translate(0.0f, f * (0.001f + i * 0.003f) * shineSpeed.value, 0.0f)
            GlStateManager.matrixMode(GL_MODELVIEW)
            modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
        }

        GlStateManager.matrixMode(GL_TEXTURE)
        GlStateManager.loadIdentity()
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.disableTexture2D()
        GlStateManager.depthFunc(GL_LEQUAL)
        Globals.mc.entityRenderer.setupFogColor(false)
    }

    private fun renderEffect(modelBase: ModelBase, entity: Entity, limbSwing: Float, limbSwingAmount: Float, age: Float, headYaw: Float, headPitch: Float, scale: Float) {
        GlStateManager.depthMask(false)
        GlStateManager.depthFunc(514)
        GlStateManager.disableLighting()
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE)
        Globals.mc.textureManager.bindTexture(ENCHANTED_ITEM_GLINT_RES)
        GlStateManager.matrixMode(5890)
        GlStateManager.pushMatrix()
        GlStateManager.scale(8.0f, 8.0f, 8.0f)
        val f = (Minecraft.getSystemTime() % 3000L).toFloat() / 3000.0f / 8.0f
        GlStateManager.translate(f, 0.0f, 0.0f)
        GlStateManager.rotate(-50.0f, 0.0f, 0.0f, 1.0f)
        modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        GlStateManager.popMatrix()
        GlStateManager.pushMatrix()
        GlStateManager.scale(8.0f, 8.0f, 8.0f)
        val f1 = (Minecraft.getSystemTime() % 4873L).toFloat() / 4873.0f / 8.0f
        GlStateManager.translate(-f1, 0.0f, 0.0f)
        GlStateManager.rotate(10.0f, 0.0f, 0.0f, 1.0f)
        modelBase.render(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(5888)
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableLighting()
        GlStateManager.depthFunc(515)
        GlStateManager.depthMask(true)
        Globals.mc.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
    }



}
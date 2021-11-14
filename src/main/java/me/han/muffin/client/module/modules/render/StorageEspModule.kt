package me.han.muffin.client.module.modules.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.Render2DEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.color.DyeColours
import me.han.muffin.client.utils.color.HueCycler
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.OutlineUtils
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.render.shader.shaders.GlowShader
import me.han.muffin.client.utils.render.shader.shaders.OutlineShader
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.awt.Color

internal object StorageEspModule: Module("StorageESP", Category.RENDER, "Highlights storages with different render methods.") {

    private val page = EnumValue(Pages.General, "Page")

    private val mode = EnumValue({ page.value == Pages.General }, Mode.Outline, "Mode")
    private val alpha = NumberValue(45, 0, 255, 5, "Alpha")
    private val lineWidth = NumberValue(2.0F, 0.1F, 5.0F, 0.1F, "LineWidth")

    private val chest = Value({ page.value == Pages.Filter },true, "Chest")
    private val enderChest = Value({ page.value == Pages.Filter },true, "EnderChest")
    private val furnace = Value({ page.value == Pages.Filter },true, "Furnace")
    private val dispenser = Value({ page.value == Pages.Filter },true, "Dispenser")
    private val hopper = Value({ page.value == Pages.Filter },true, "Hopper")
    private val shulkerBox = Value({ page.value == Pages.Filter },true, "ShulkerBox")
    //private val minecart = Value({ page.value == Pages.Filter },true, "Minecart")

    private val chestColour = EnumValue({ page.value == Pages.Colour }, DyeColours.ORANGE, "Chest-Colour")
    private val enderChestColour = EnumValue({ page.value == Pages.Colour }, DyeColours.PURPLE, "EnderChest-Colour")
    private val furnaceColour = EnumValue({ page.value == Pages.Colour }, DyeColours.LIGHT_GRAY, "Furnace-Colour")
    private val dispenserColour = EnumValue({ page.value == Pages.Colour }, DyeColours.LIGHT_GRAY, "Dispenser-Colour")
    private val hopperColour = EnumValue({ page.value == Pages.Colour }, DyeColours.GRAY, "Hopper-Colour")
    private val shulkerBoxColour = EnumValue({ page.value == Pages.Colour }, DyeColours.MAGENTA, "ShulkerBox-Colour")
    //private val minecartColour = EnumValue({ page.value == Pages.Colour }, DyeColours.GREEN, "Minecart-Colour")

    private var cycler = HueCycler(600)
    val colour get() = Colour().clientColour(alpha.value)

    init {
        addSettings(
            page,
            mode, alpha, lineWidth,
            enderChest, furnace, dispenser, hopper, shulkerBox, //minecart,
            chestColour, enderChestColour, furnaceColour, dispenserColour, hopperColour, shulkerBoxColour, //minecartColour
        )
    }

    private enum class Pages {
        General, Filter, Colour
    }

    enum class Mode {
        Box, Full, ShaderGlow, Outline, ShaderOutline, Wireframe, Solid, Csgo
    }

    private fun getTileEntityColor(tileEntity: TileEntity): Colour? {
        val colour = when (tileEntity) {
            is TileEntityChest -> chestColour.value
            is TileEntityDispenser -> dispenserColour.value
            is TileEntityShulkerBox -> shulkerBoxColour.value
            is TileEntityEnderChest -> enderChestColour.value
            is TileEntityFurnace -> furnaceColour.value
            is TileEntityHopper -> hopperColour.value
            else -> return null
        }.color
        return if (colour == DyeColours.RAINBOW.color) {
            cycler.currentRgb()
        } else colour
    }

    private fun checkTileEntityType(tileEntity: TileEntity) =
        chest.value && tileEntity is TileEntityChest
                || dispenser.value && tileEntity is TileEntityDispenser
                || shulkerBox.value && tileEntity is TileEntityShulkerBox
                || enderChest.value && tileEntity is TileEntityEnderChest
                || furnace.value && tileEntity is TileEntityFurnace
                || hopper.value && tileEntity is TileEntityHopper


    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        val mode = mode.value

        if (mode == Mode.Outline) {
            OutlineUtils.checkSetupFBO()
        }

        val red = colour.r
        val green = colour.g
        val blue = colour.b

        val gamma = Globals.mc.gameSettings.gammaSetting
        Globals.mc.gameSettings.gammaSetting = 100000.0F

        for (tileEntity in Globals.mc.world.loadedTileEntityList) {

            val color = when {
                chest.value && tileEntity is TileEntityChest -> ColourUtils.getClientColor()
                enderChest.value && tileEntity is TileEntityEnderChest -> Color.MAGENTA
                furnace.value && tileEntity is TileEntityFurnace -> Color.BLACK
                dispenser.value && tileEntity is TileEntityDispenser -> Color.BLACK
                hopper.value && tileEntity is TileEntityHopper -> Color.GRAY
                shulkerBox.value && tileEntity is TileEntityShulkerBox -> Color(0x6e, 0x4d, 0x6e).brighter()
                else -> null
            } ?: continue

            val entityPos = tileEntity.pos

            if (tileEntity !is TileEntityChest || tileEntity !is TileEntityEnderChest) {
                RenderUtils.drawBlockESP(entityPos, red, green, blue, alpha.value)
                continue
            }

            when (mode) {
                Mode.Box -> RenderUtils.drawBlockESP(entityPos, red, green, blue, alpha.value)
                Mode.Full -> RenderUtils.drawBlockFullESP(entityPos, red, green, blue, alpha.value, lineWidth.value)
                Mode.Csgo -> RenderUtils.draw2D(entityPos, color.rgb, Color.BLACK.rgb)

                Mode.Outline -> {
                    RenderUtils.glColor(red, green, blue, 255)
                    OutlineUtils.renderOne(lineWidth.value)
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    OutlineUtils.renderTwo()
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    OutlineUtils.renderThree()
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    OutlineUtils.renderFour(red, green, blue, 255)
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    OutlineUtils.renderFive()
                    OutlineUtils.setColor(255, 255, 255, 255)
                }

                Mode.Wireframe -> {
                    glPushMatrix()
                    glPushAttrib(GL_ALL_ATTRIB_BITS)
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                    glDisable(GL_TEXTURE_2D)
                    glDisable(GL_LIGHTING)
                    glDisable(GL_DEPTH_TEST)
                    glEnable(GL_LINE_SMOOTH)
                    glEnable(GL_BLEND)
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    RenderUtils.glColor(red, green, blue, alpha.value)
                    glLineWidth(lineWidth.value)
                    TileEntityRendererDispatcher.instance.render(tileEntity, event.partialTicks, -1)
                    glPopAttrib()
                    glPopMatrix()
                }
            }

        }

        for (entity in Globals.mc.world.loadedEntityList) {
            if (entity is EntityMinecartChest) {

                val vec = MathUtils.interpolateEntity(entity, event.partialTicks)
                val x = vec.x - RenderUtils.renderPosX
                val y = vec.y - RenderUtils.renderPosY
                val z = vec.z - RenderUtils.renderPosZ

                val bb = AxisAlignedBB(
                    0.0, 0.0, 0.0,
                    entity.width.toDouble(), entity.height.toDouble(), entity.width.toDouble()
                ).offset(x - entity.width / 2, y, z - entity.width / 2)

                when (mode) {
                    Mode.Box -> RenderUtils.drawBoxESP(bb, red, green, blue, alpha.value)

                    Mode.Full -> RenderUtils.drawBoxFullESP(bb, red, green, blue, alpha.value, lineWidth.value)

                    Mode.Csgo -> RenderUtils.draw2D(
                        entity.position,
                        ColourUtils.getClientColor().rgb,
                        Color.BLACK.rgb
                    )

                    Mode.Outline -> {
                        val entityShadow = Globals.mc.gameSettings.entityShadows
                        Globals.mc.gameSettings.entityShadows = false
                        RenderUtils.glColor(red, green, blue, 255)
                        OutlineUtils.renderOne(lineWidth.value)
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        OutlineUtils.renderTwo()
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        OutlineUtils.renderThree()
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        OutlineUtils.renderFour(red, green, blue, 255)
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        OutlineUtils.renderFive()
                        OutlineUtils.setColor(255, 255, 255, 255)
                        Globals.mc.gameSettings.entityShadows = entityShadow
                    }

                    Mode.Wireframe -> {
                        val entityShadow = Globals.mc.gameSettings.entityShadows
                        Globals.mc.gameSettings.entityShadows = false
                        glPushMatrix()
                        glPushAttrib(GL_ALL_ATTRIB_BITS)
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                        glDisable(GL_TEXTURE_2D)
                        glDisable(GL_LIGHTING)
                        glDisable(GL_DEPTH_TEST)
                        glEnable(GL_LINE_SMOOTH)
                        glEnable(GL_BLEND)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        RenderUtils.glColor(red, green, blue, alpha.value)
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        RenderUtils.glColor(red, green, blue, alpha.value)
                        glLineWidth(lineWidth.value)
                        Globals.mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
                        glPopAttrib()
                        glPopMatrix()
                        Globals.mc.gameSettings.entityShadows = entityShadow
                    }
                }
            }
        }

        RenderUtils.glColor(Color(255, 255, 255, 255))
        Globals.mc.gameSettings.gammaSetting = gamma
    }

    @Listener
    private fun onRender2D(event: Render2DEvent) {
        if (fullNullCheck()) return

        val mode = mode.value
        val shader = (if (mode == Mode.ShaderOutline) OutlineShader.OUTLINE_SHADER else if (mode == Mode.ShaderGlow) GlowShader.GLOW_SHADER else null) ?: return

        shader.startDraw(event.partialTicks)

        try {
            val renderManager = Globals.mc.renderManager

            for (entity in Globals.mc.world.loadedTileEntityList) {
                if (!(entity is TileEntityChest && chest.value || entity is TileEntityChest && enderChest.value)) continue

                val x = entity.pos.x - RenderUtils.renderPosX
                val y = entity.pos.y - RenderUtils.renderPosY
                val z = entity.pos.z - RenderUtils.renderPosZ

                TileEntityRendererDispatcher.instance.render(entity, x, y, z, event.partialTicks)
            }

            for (entity in Globals.mc.world.loadedEntityList) {
                if (entity !is EntityMinecartChest) continue
                renderManager.renderEntityStatic(entity, event.partialTicks, true)
            }

        } catch (ex: Exception) {
            Muffin.LOGGER.info("An error occurred while rendering all entities for shader esp", ex)
        }

        shader.stopDraw(colour.r.toFloat(), colour.g.toFloat(), colour.b.toFloat(), 255F, lineWidth.value, 1f)
    }

}
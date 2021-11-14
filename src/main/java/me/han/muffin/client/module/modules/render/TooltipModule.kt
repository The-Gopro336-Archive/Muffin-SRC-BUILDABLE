package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.events.render.RenderTooltipEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.world.storage.MapData
import org.lwjgl.opengl.GL11
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

object TooltipModule: Module("Tooltip", Category.RENDER, "Better tootips for shulker box and map.") {
    private val shulker = Value(true, "Shulker")
    private val map = Value(true, "Map")
    private val mapFrame = Value(true, "MapFrame")
    private val mapSize = NumberValue(5.0, 0.0, 10.0, 0.5, "MapSize")

    private val RES_MAP_BACKGROUND = ResourceLocation("textures/map/map_background.png")

    private fun getMapData(stack: ItemStack): MapData? {
        return (stack.item as ItemMap).getMapData(stack, Globals.mc.world)
    }

    init {
        addSettings(shulker, map, mapFrame, mapSize)
    }

    @Listener
    private fun onRenderToolTip(event: RenderTooltipEvent) {
        if (fullNullCheck()) return

        if (shulker.value && event.item.item is ItemShulkerBox) {
            val stack = event.item
            val tagCompound = stack.tagCompound

            if (tagCompound != null && tagCompound.hasKey("BlockEntityTag", 10)) {
                val blockEntityTag = tagCompound.getCompoundTag("BlockEntityTag")

                if (blockEntityTag.hasKey("Items", 9)) {
                    // We'll take over!
                    event.cancel()

                    val nonNullList = NonNullList.withSize(27, ItemStack.EMPTY)
                    ItemStackHelper.loadAllItems(blockEntityTag, nonNullList)

                    GlStateManager.pushMatrix()
                    GlStateManager.enableBlend()
                    GlStateManager.disableRescaleNormal()
                    RenderHelper.disableStandardItemLighting()
                    GlStateManager.disableLighting()
                    GlStateManager.disableDepth()
                    GlStateManager.enableTexture2D()

                    val width = 144.coerceAtLeast(Globals.mc.fontRenderer.getStringWidth(stack.displayName) + 3) //9*16

                    val eventX = event.x
                    val eventY = event.y

                    val x1 = eventX + 12
                    val y1 = eventY - 12
                    val height = 48 + 9 //3*16

                    Globals.mc.renderItem.zLevel = 300.0f
                    RenderUtils.drawGradientRectP(x1 - 3, y1 - 4, x1 + width + 3, y1 - 3, -267386864, -267386864)
                    RenderUtils.drawGradientRectP(x1 - 3, y1 + height + 3, x1 + width + 3, y1 + height + 4, -267386864, -267386864)
                    RenderUtils.drawGradientRectP(x1 - 3, y1 - 3, x1 + width + 3, y1 + height + 3, -267386864, -267386864)
                    RenderUtils.drawGradientRectP(x1 - 4, y1 - 3, x1 - 3, y1 + height + 3, -267386864, -267386864)
                    RenderUtils.drawGradientRectP(x1 + width + 3, y1 - 3, x1 + width + 4, y1 + height + 3, -267386864, -267386864)
                    RenderUtils.drawGradientRectP(x1 - 3, y1 - 3 + 1, x1 - 3 + 1, y1 + height + 3 - 1, 1347420415, 1344798847)
                    RenderUtils.drawGradientRectP(x1 + width + 2, y1 - 3 + 1, x1 + width + 3, y1 + height + 3 - 1, 1347420415, 1344798847)
                    RenderUtils.drawGradientRectP(x1 - 3, y1 - 3, x1 + width + 3, y1 - 3 + 1, 1347420415, 1347420415)
                    RenderUtils.drawGradientRectP(x1 - 3, y1 + height + 2, x1 + width + 3, y1 + height + 3, 1344798847, 1344798847)

                    Globals.mc.fontRenderer.drawStringWithShadow(stack.displayName, eventX + 12.toFloat(), eventY - 12.toFloat(), 0xFFFFFF)

                    GlStateManager.enableBlend()
                    GlStateManager.enableAlpha()
                    GlStateManager.enableTexture2D()
                    GlStateManager.enableLighting()
                    GlStateManager.enableDepth()
                    RenderHelper.enableGUIStandardItemLighting()

                    for (i in nonNullList.indices) {
                        val iX = eventX + i % 9 * 16 + 11
                        val iY = eventY + i / 9 * 16 - 11 + 8
                        val itemStack = nonNullList[i]
                        Globals.mc.renderItem.renderItemAndEffectIntoGUI(itemStack, iX, iY)
                        Globals.mc.renderItem.renderItemOverlayIntoGUI(Globals.mc.fontRenderer, itemStack, iX, iY, null)
                    }

                    RenderHelper.disableStandardItemLighting()
                    Globals.mc.renderItem.zLevel = 0.0f
                    GlStateManager.disableBlend()
                    GlStateManager.enableLighting()
                    GlStateManager.enableDepth()
                    RenderHelper.enableStandardItemLighting()
                    GlStateManager.enableRescaleNormal()
                    GlStateManager.popMatrix()
                }
            }
        }

        if (map.value && event.item.item is ItemMap) {
            val mapData = getMapData(event.item) ?: return

            event.cancel()

            val xl = event.x + 6
            val yl = event.y + 6

            GL11.glPushMatrix()
            GlStateManager.color(
                1f,
                1f,
                1f
            ) // this dumbass shit is needed for some fucking reason???? thanks tiger for figuring this one out

            val tessellator = Tessellator.getInstance()
            val builder = tessellator.buffer

            GlStateManager.translate(xl.toDouble(), yl.toDouble(), 0.0)
            GlStateManager.scale(mapSize.value / 5.0, mapSize.value / 5.0, 0.0)
            RenderHelper.enableGUIStandardItemLighting() // needed to make lighting work inside non inventory containers
            Globals.mc.textureManager.bindTexture(RES_MAP_BACKGROUND)

            if (mapFrame.value) {
                GL11.glDepthRange(0.0, 0.01) // fix drawing under other layers, just draw over everything
                builder.begin(7, DefaultVertexFormats.POSITION_TEX)
                builder.pos(-7.0, 135.0, 0.0).tex(0.0, 1.0).endVertex()
                builder.pos(135.0, 135.0, 0.0).tex(1.0, 1.0).endVertex()
                builder.pos(135.0, -7.0, 0.0).tex(1.0, 0.0).endVertex()
                builder.pos(-7.0, -7.0, 0.0).tex(0.0, 0.0).endVertex()
                tessellator.draw()
                GL11.glDepthRange(0.0, 1.0) // undo changes to rendering order
            }

            /* Draw the map */GlStateManager.disableDepth() // needed to keep it on top of the frame
            GL11.glDepthRange(0.0, 0.01)
            Globals.mc.entityRenderer.mapItemRenderer.renderMap(mapData, !mapFrame.value)
            GL11.glDepthRange(0.0, 1.0)
            GlStateManager.enableDepth() // originally enabled

            RenderHelper.disableStandardItemLighting() // originally disabled
            GL11.glPopMatrix()
        }
    }



}
package me.han.muffin.client.gui.hud.item.component.combat

import me.han.muffin.client.core.Globals
import me.han.muffin.client.gui.hud.item.HudItem
import me.han.muffin.client.utils.extensions.kotlin.floorToInt
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.utils.extensions.mixin.render.damagedBlocks
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.RenderUtils
import net.minecraft.block.Block
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

object HoleOverlayItem: HudItem("HoleOverlay", HudCategory.Combat, 20, 300) {

    init {
        width = 48F
        height = 48F
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        var yaw = 0F
        when (((Globals.mc.player.rotationYaw * 4.0 / 360.0) + 0.5).floorToInt() and 3) {
            1 -> yaw = 90F
            2 -> yaw = -180F
            3 -> yaw = -90F
        }

        val northPos = traceToBlock(partialTicks, yaw)

        val north = getBlock(northPos)
        if (north != Blocks.AIR) {
            val damage = getBlockDamage(northPos)
            if (damage != 0) RenderUtils.drawRect(x + 16F, y.toFloat(), x + 32F, y + 16F, 0x60ff0000)
            drawBlock(north, x + 16F, y.toFloat())
        }

        val southPos = traceToBlock(partialTicks, yaw - 180.0f)
        val south = getBlock(southPos)
        if (south != Blocks.AIR) {
            val damage = getBlockDamage(southPos)
            if (damage != 0) RenderUtils.drawRect(x + 16F, y + 32F, x + 32F, y + 48F, 0x60ff0000)
            drawBlock(south, x + 16F, y + 32F)
        }

        val eastPos = traceToBlock(partialTicks, yaw + 90.0f)

        val east = getBlock(eastPos)
        if (east != Blocks.AIR) {
            val damage = getBlockDamage(eastPos)
            if (damage != 0) RenderUtils.drawRect(x + 32F, y + 16F, x + 48F, y + 32F, 0x60ff0000)
            drawBlock(east, x + 32F, y + 16F)
        }

        val westPos = traceToBlock(partialTicks, yaw - 90.0f)

        val west = getBlock(westPos)
        if (west != Blocks.AIR) {
            val damage = getBlockDamage(westPos)
            if (damage != 0) RenderUtils.drawRect(x.toFloat(), y + 16F, x + 16F, y + 32F, 0x60ff0000)
            drawBlock(west, x.toFloat(), y + 16F)
        }
    }

    private fun getBlockDamage(pos: BlockPos): Int {
        for (destBlockProgress in Globals.mc.renderGlobal.damagedBlocks.values) {
            if (destBlockProgress.position.x == pos.x && destBlockProgress.position.y == pos.y && destBlockProgress.position.z == pos.z) {
                return destBlockProgress.partialBlockDamage
            }
        }
        return 0
    }

    private fun traceToBlock(partialTicks: Float, yaw: Float): BlockPos {
        val pos = MathUtils.interpolateEntity(Globals.mc.player, partialTicks)
        val dir = MathUtils.direction(yaw)
        return BlockPos(pos.x + dir.x, pos.y, pos.z + dir.y)
    }

    private fun getBlock(pos: BlockPos): Block {
        val block = pos.block
        return if (block == Blocks.BEDROCK || block == Blocks.OBSIDIAN) {
            block
        } else {
            Blocks.AIR
        }
    }

    private fun drawBlock(block: Block, x: Float, y: Float) {
        val stack = ItemStack(block)

        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)

        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.translate(x, y, 0f)
        Globals.mc.renderItem.renderItemAndEffectIntoGUI(stack, 0, 0)
        RenderHelper.disableStandardItemLighting()

        GlStateUtils.blend(false)
        GlStateUtils.resetColour()
        GlStateUtils.matrix(false)
    }


}
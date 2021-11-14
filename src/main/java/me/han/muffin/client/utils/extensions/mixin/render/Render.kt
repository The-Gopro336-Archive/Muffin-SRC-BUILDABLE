package me.han.muffin.client.utils.extensions.mixin.render

import me.han.muffin.client.imixin.render.*
import me.han.muffin.client.imixin.render.entity.IEntityRenderer
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

val Frustum.x: Double
    get() = (this as IFrustum).x

val Frustum.y: Double
    get() = (this as IFrustum).y

val Frustum.z: Double
    get() = (this as IFrustum).z

var ItemRenderer.itemStackMainHand: ItemStack
    get() = (this as IItemRenderer).itemStackMainHand
    set(value) {
        (this as IItemRenderer).itemStackMainHand = value
    }

var ItemRenderer.itemStackOffHand: ItemStack
    get() = (this as IItemRenderer).itemStackOffHand
    set(value) {
        (this as IItemRenderer).itemStackOffHand = value
    }

var ItemRenderer.equippedProgressMainHand: Float
    get() = (this as IItemRenderer).equippedProgressMainHand
    set(value) {
        (this as IItemRenderer).equippedProgressMainHand = value
    }

var ItemRenderer.equippedProgressOffHand: Float
    get() = (this as IItemRenderer).equippedProgressOffHand
    set(value) {
        (this as IItemRenderer).equippedProgressOffHand = value
    }

var ItemRenderer.prevEquippedProgressMainHand: Float
    get() = (this as IItemRenderer).prevEquippedProgressMainHand
    set(value) {
        (this as IItemRenderer).prevEquippedProgressMainHand = value
    }

var ItemRenderer.prevEquippedProgressOffHand: Float
    get() = (this as IItemRenderer).prevEquippedProgressOffHand
    set(value) {
        (this as IItemRenderer).prevEquippedProgressOffHand = value
    }

val RenderGlobal.damagedBlocks: Map<Int, DestroyBlockProgress>
    get() = (this as IRenderGlobal).damagedBlocks

val RenderManager.renderPosX: Double
    get() = (this as IRenderManager).renderPosX

val RenderManager.renderPosY: Double
    get() = (this as IRenderManager).renderPosY

val RenderManager.renderPosZ: Double
    get() = (this as IRenderManager).renderPosZ


fun ViewFrustum.getRenderChunk(pos: BlockPos) = (this as IViewFrustum).getRenderChunkVoid(pos)

fun EntityRenderer.orientCamera(partialTicks: Float) = (this as IEntityRenderer).orientCameraVoid(partialTicks)
fun EntityRenderer.setupCameraTransform(partialTicks: Float, pass: Int) = (this as IEntityRenderer).setupCameraTransformVoid(partialTicks, pass)
fun EntityRenderer.renderWorldPass(pass: Int, partialTicks: Float, finishTimeNano: Long) = (this as IEntityRenderer).renderWorldPassVoid(pass, partialTicks, finishTimeNano)
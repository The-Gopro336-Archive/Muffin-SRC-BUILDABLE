package me.han.muffin.client.utils.render

import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.extensions.kotlin.toColour
import me.han.muffin.client.utils.extensions.kotlin.toRadian
import me.han.muffin.client.utils.extensions.mixin.misc.renderPartialTicksPaused
import me.han.muffin.client.utils.extensions.mixin.render.renderPosX
import me.han.muffin.client.utils.extensions.mixin.render.renderPosY
import me.han.muffin.client.utils.extensions.mixin.render.renderPosZ
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.texture.MipmapTexture
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL32
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object RenderUtils {
    private val camera = Frustum()

    val tessellator = Tessellator.getInstance()
    val buffer = tessellator.buffer

    @JvmField var deltaTime = 0
    var BBGrow = 0.0020000000949949026

    val renderPosX: Double get() = Globals.mc.renderManager.renderPosX
    val renderPosY: Double get() = Globals.mc.renderManager.renderPosY
    val renderPosZ: Double get() = Globals.mc.renderManager.renderPosZ

    val renderPosVector: Vec3d get() = Vec3d(this.renderPosX, this.renderPosY, this.renderPosZ)

    val playerViewX: Float get() = Globals.mc.renderManager.playerViewX
    val playerViewY: Float get() = Globals.mc.renderManager.playerViewY

    @JvmStatic
    val camPos: Vec3d get() = MathUtils.interpolateEntity(Globals.mc.player, renderPartialTicks).add(ActiveRenderInfo.getCameraPosition())

    @JvmStatic
    val renderPartialTicks: Float get() {
        return if (Globals.mc.isGamePaused) Globals.mc.renderPartialTicksPaused else Globals.mc.renderPartialTicks
    }

    val viewerPosX: Double get() = Globals.mc.renderManager.viewerPosX
    val viewerPosY: Double get() = Globals.mc.renderManager.viewerPosY
    val viewerPosZ: Double get() = Globals.mc.renderManager.viewerPosZ

    @JvmStatic
    fun enableGL2D() {
        GlStateUtils.blend(true)
        GlStateUtils.alpha(false)
        GlStateUtils.texture2d(false)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.hintPolygon(true)
        GlStateUtils.smooth(true)
        GlStateUtils.cull(false)
    }

    @JvmStatic
    fun disableGL2D() {
        GlStateUtils.cull(true)
        GlStateUtils.smooth(false)
        GlStateUtils.hintPolygon(false)
        GlStateUtils.lineSmooth(false)
        GlStateUtils.texture2d(true)
        GlStateUtils.alpha(true)
        GlStateUtils.blend(false)
    }

    fun prepareGL3D() {
        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)
        GlStateUtils.depth(false)
        glEnable(GL32.GL_DEPTH_CLAMP)
        GlStateUtils.alpha(false)
        GlStateUtils.smooth(true)
        GlStateUtils.cull(false)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.hintPolygon(true)
        GlStateUtils.lighting(false)
        GlStateUtils.texture2d(false)
        GlStateUtils.depthMask(false)
    }

    fun releaseGL3D() {
        GlStateUtils.depthMask(true)
        GlStateUtils.lighting(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.cull(true)
        GlStateUtils.smooth(false)
        GlStateUtils.alpha(true)
        GlStateUtils.lineSmooth(false)
        GlStateUtils.hintPolygon(false)
        glDisable(GL32.GL_DEPTH_CLAMP)
        GlStateUtils.depth(true)
        GlStateUtils.blend(false)
        GlStateUtils.resetColour()
        GlStateUtils.matrix(false)
    }

    fun draw2D(entity: Entity, posX: Double, posY: Double, posZ: Double, colour: Colour, backgroundColour: Colour) {
        Dimension.ThreeD {
            GlStateManager.translate(posX, posY, posZ)
            glNormal3f(0f, 0f, 0f)
            GlStateManager.rotate(-playerViewY, 0f, 1f, 0f)
            GlStateManager.scale(-0.1, -0.1, 0.1)

            Dimension.TwoD {
                GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                    quickDrawRectNoBegin(-7f, 2f, -4f, 3f, colour)
                    quickDrawRectNoBegin(4f, 2f, 7f, 3f, colour)
                    quickDrawRectNoBegin(-7f, 0.5f, -6f, 3f, colour)
                    quickDrawRectNoBegin(6f, 0.5f, 7f, 3f, colour)

                    quickDrawRectNoBegin(-7f, 3f, -4f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(4f, 3f, 7f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, 0.5f, -7f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(7f, 0.5f, 7.3f, 3.3f, backgroundColour)

                    GlStateManager.translate(0.0, 21 + -(entity.entityBoundingBox.maxY - entity.entityBoundingBox.minY) * 12, 0.0)

                    quickDrawRectNoBegin(4f, -20f, 7f, -19f, colour)
                    quickDrawRectNoBegin(-7f, -20f, -4f, -19f, colour)
                    quickDrawRectNoBegin(6f, -20f, 7f, -17.5f, colour)
                    quickDrawRectNoBegin(-7f, -20f, -6f, -17.5f, colour)

                    quickDrawRectNoBegin(7f, -20f, 7.3f, -17.5f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, -20f, -7f, -17.5f, backgroundColour)
                    quickDrawRectNoBegin(4f, -20.3f, 7.3f, -20f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, -20.3f, -4f, -20f, backgroundColour)
                }
            }
        }
    }

    fun draw2D(blockPos: BlockPos, color: Int, backgroundColor: Int) {
        val colour = color.toColour()
        val backgroundColour = backgroundColor.toColour()

        val posX = blockPos.x + 0.5 - renderPosX
        val posY = blockPos.y - renderPosY
        val posZ = blockPos.z + 0.5 - renderPosZ

        Dimension.ThreeD {
            GlStateManager.translate(posX, posY, posZ)
            glNormal3f(0f, 0f, 0f)
            GlStateManager.rotate(-Globals.mc.renderManager.playerViewY, 0f, 1f, 0f)
            GlStateManager.scale(-0.1, -0.1, 0.1)

            Dimension.TwoD {
                GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                    quickDrawRectNoBegin(-7f, 2f, -4f, 3f, colour)
                    quickDrawRectNoBegin(4f, 2f, 7f, 3f, colour)
                    quickDrawRectNoBegin(-7f, 0.5f, -6f, 3f, colour)
                    quickDrawRectNoBegin(6f, 0.5f, 7f, 3f, colour)

                    quickDrawRectNoBegin(-7f, 3f, -4f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(4f, 3f, 7f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, 0.5f, -7f, 3.3f, backgroundColour)
                    quickDrawRectNoBegin(7f, 0.5f, 7.3f, 3.3f, backgroundColour)

                    GlStateManager.translate(0f, 9f, 0f)

                    quickDrawRectNoBegin(4f, -20f, 7f, -19f, colour)
                    quickDrawRectNoBegin(-7f, -20f, -4f, -19f, colour)
                    quickDrawRectNoBegin(6f, -20f, 7f, -17.5f, colour)
                    quickDrawRectNoBegin(-7f, -20f, -6f, -17.5f, colour)

                    quickDrawRectNoBegin(7f, -20f, 7.3f, -17.5f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, -20f, -7f, -17.5f, backgroundColour)
                    quickDrawRectNoBegin(4f, -20.3f, 7.3f, -20f, backgroundColour)
                    quickDrawRectNoBegin(-7.3f, -20.3f, -4f, -20f, backgroundColour)
                }
            }
        }

    }

    fun glBillboard(x: Float, y: Float, z: Float) {
        val scale = 0.016666668f * 1.6f
        GlStateManager.translate(x - renderPosX, y - renderPosY, z - renderPosZ)
        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(-Globals.mc.player.rotationYaw, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(Globals.mc.player.rotationPitch, if (Globals.mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-scale, -scale, scale)
    }

    fun glBillboardDistanceScaled(x: Float, y: Float, z: Float, player: EntityPlayer, scale: Float) {
        glBillboard(x, y, z)
        val distance = player.getDistance(x.toDouble(), y.toDouble(), z.toDouble()).toInt()
        var scaleDistance = distance / 2.0f / (2.0f + (2.0f - scale))
        if (scaleDistance < 1f) scaleDistance = 1f
        GlStateManager.scale(scaleDistance, scaleDistance, scaleDistance)
    }

    fun drawScaledCustomSizeModalRect(x: Float, y: Float, u: Float, v: Float, uWidth: Int, vHeight: Int, width: Int, height: Int, tileWidth: Float, tileHeight: Float) {
        val f = 1.0f / tileWidth
        val f1 = 1.0f / tileHeight

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_TEX drawBuffer {
            pos(x, (y + height), tex = (u * f) to ((v + vHeight) * f1))
            pos((x + width), (y + height), tex = ((u + uWidth) * f) to ((v + vHeight) * f1))
            pos(x + width, y, tex = ((u + uWidth) * f) to (v * f1))
            pos(x, y, tex = (u * f) to (v * f1))
        }

    }

    @JvmStatic
    fun drawModalRectWithCustomSizedTexture(x: Int, y: Int, u: Float, v: Float, width: Int, height: Int, textureWidth: Float, textureHeight: Float) {
        val f = 1.0f / textureWidth
        val f1 = 1.0f / textureHeight

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_TEX drawBuffer {
            pos(x, y + height, tex = u * f to (v + height) * f1)
            pos(x + width, y + height, tex = (u + width) * f to (v + height) * f1)
            pos(x + width, y, tex = (u + width) * f to v * f1)
            pos(x, y, tex = u * f to v * f1)
        }
    }

    @JvmStatic
    fun drawMipMapTexture(texture: MipmapTexture, x: Double, y: Double, size: Float) {
        texture.bindTexture()

        GL_TRIANGLE_STRIP withVertexFormat DefaultVertexFormats.POSITION_TEX drawBuffer {
            pos(x, y + size, tex = 0.0 to 1.0)
            pos(x + size, y + size, tex = 1.0 to 1.0)
            pos(x, y, tex = 0.0 to 0.0)
            pos(x + size, y, tex = 1.0 to 0.0)
        }

    }


    @JvmStatic
    fun drawTexturedRect(x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int, zLevel: Int) {
        val radian = 0.00390625F

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_TEX drawBuffer {
            pos(x, (y + height), zLevel, tex = textureX * radian to (textureY + height) * radian)
            pos((x + width), (y + height), zLevel, tex = (textureX + width * radian) to (textureY + height) * radian)
            pos((x + width), y, zLevel, tex = (textureX + width * radian) to (textureY * radian))
            pos(x, y, zLevel, tex = textureX * radian to (textureY * radian))
        }

    }

    fun drawLine(x: Double, y: Double, x1: Double, y1: Double, width: Float) {
        Dimension.TwoD {
            glLineWidth(width)
            GL_LINES withVertexFormat DefaultVertexFormats.POSITION drawBuffer {
                pos(x, y)
                pos(x1, y1)
            }
        }
    }

    fun drawLine(x: Double, y: Double, x1: Double, y1: Double, width: Float, color: Int) {
        Dimension.TwoD {
            glColor(color)
            drawLine(x, y, x1, y1, width)
        }
    }

    fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        val colour = color.toColour()

        Dimension.TwoD {
            glLineWidth(1.0F)
            GL_POLYGON withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                for (i in 0..360) {
                    val xBuff = MathUtils.round(x + sin(i * Math.PI / 180.0) * radius, 4)
                    val zBuff = MathUtils.round(y + cos(i * Math.PI / 180.0) * radius, 4)
                    pos(xBuff, zBuff, colour = colour)
                }
            }
        }

        GlStateUtils.resetColour()
    }

    @JvmStatic
    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        Dimension.TwoD {
            quickDrawRect(x, y, x2, y2, color)
        }
    }

    fun quickDrawRectNoBegin(x: Float, y: Float, x2: Float, y2: Float, colour: Colour) {
        buffer.pos(x2, y, colour = colour)
        buffer.pos(x, y, colour = colour)
        buffer.pos(x, y2, colour = colour)
        buffer.pos(x2, y2, colour = colour)
    }

    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        val colour = color.toColour()

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            quickDrawRectNoBegin(x, y, x2, y2, colour)
        }
    }


    fun quickDrawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int, color2: Int) {
        val colour = color2.toColour()

        quickDrawRect(x, y, x2, y2, color1)
        glLineWidth(width)

        GL_LINE_LOOP withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            buffer.pos(x2, y, colour = colour)
            buffer.pos(x, y, colour = colour)
            buffer.pos(x, y2, colour = colour)
            buffer.pos(x2, y2, colour = colour)
        }

    }

    fun quickDrawRoundedRect(xParam: Float, yParam: Float, x1Param: Float, y1Param: Float, borderC: Int, insideC: Int) {
        val x = xParam * 2.0F
        val y = yParam * 2.0F
        val x1 = x1Param * 2.0F
        val y1 = y1Param * 2.0F

        glScalef(0.5f, 0.5f, 0.5f)
        quickDrawVLine(x, y + 1, y1 - 2, borderC)
        quickDrawVLine(x1 - 1, y + 1, y1 - 2, borderC)
        quickDrawHLine(x + 2, x1 - 3, y, borderC)
        quickDrawHLine(x + 2, x1 - 3, y1 - 1, borderC)
        quickDrawHLine(x + 1, x + 1, y + 1, borderC)
        quickDrawHLine(x1 - 2, x1 - 2, y + 1, borderC)
        quickDrawHLine(x1 - 2, x1 - 2, y1 - 2, borderC)
        quickDrawHLine(x + 1, x + 1, y1 - 2, borderC)
        quickDrawRect(x + 1, y + 1, x1 - 1, y1 - 1, insideC)
        glScalef(2.0f, 2.0f, 2.0f)
    }

    fun drawRoundedRect(xParam: Float, yParam: Float, x1Param: Float, y1Param: Float, borderC: Int, insideC: Int) {
        val x = xParam * 2.0F
        val y = yParam * 2.0F
        val x1 = x1Param * 2.0F
        val y1 = y1Param * 2.0F

        Dimension.TwoD {
            glScalef(0.5f, 0.5f, 0.5f)
            drawVLine(x, y + 1, y1 - 2, borderC)
            drawVLine(x1 - 1, y + 1, y1 - 2, borderC)
            drawHLine(x + 2, x1 - 3, y, borderC)
            drawHLine(x + 2, x1 - 3, y1 - 1, borderC)
            drawHLine(x + 1, x + 1, y + 1, borderC)
            drawHLine(x1 - 2, x1 - 2, y + 1, borderC)
            drawHLine(x1 - 2, x1 - 2, y1 - 2, borderC)
            drawHLine(x + 1, x + 1, y1 - 2, borderC)
            quickDrawRect(x + 1, y + 1, x1 - 1, y1 - 1, insideC)
            glScalef(2.0f, 2.0f, 2.0f)
        }
    }

    @JvmStatic
    fun drawOutlineRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int) {
        val colour = color.toColour()

        Dimension.TwoD {
            GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                quickDrawRectNoBegin(x, y, x - thickness, h, colour)
                quickDrawRectNoBegin(w + thickness, y, w, h, colour)
                quickDrawRectNoBegin(x, y, w, y - thickness, colour)
                quickDrawRectNoBegin(x, h + thickness, w, h, colour)
            }
        }
    }

    @JvmStatic
    fun drawBorderedRectReliant(x: Float, y: Float, x1: Float, y1: Float, lineWidth: Float, inside: Int, border: Int) {
        val borderColour = border.toColour()

        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(true)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST)

        quickDrawRect(x, y, x1, y1, inside)

        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(lineWidth)

        GL_LINE_STRIP withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            pos(x, y, colour = borderColour)
            pos(x, y1, colour = borderColour)
            pos(x1, y1, colour = borderColour)
            pos(x1, y, colour = borderColour)
            pos(x, y, colour = borderColour)
        }

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDepthMask(false)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE)
        glHint(GL_POLYGON_SMOOTH_HINT, GL_DONT_CARE)
    }

    fun drawGradientBorderedRectReliant(x: Float, y: Float, x1: Float, y1: Float, lineWidth: Float, border: Int, bottom: Int, top: Int) {
        val borderColour = border.toColour()

        Dimension.TwoD {
            drawGradientRect(x, y, x1, y1, top, bottom)
            glEnable(GL_BLEND)
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glLineWidth(lineWidth)

            GL_LINE_STRIP withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                pos(x, y, colour = borderColour)
                pos(x, y1, colour = borderColour)
                pos(x1, y1, colour = borderColour)
                pos(x1, y, colour = borderColour)
                pos(x, y, colour = borderColour)
            }

            glEnable(GL_TEXTURE_2D)
            glDisable(GL_BLEND)
        }
    }

    @JvmStatic
    fun drawGradientRect(left: Float, top: Float, right: Float, bottom: Float, startColor: Int, endColor: Int) {
        val startColour = startColor.toColour()
        val endColour = endColor.toColour()

        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)
        GlStateUtils.depth(false)
        GlStateUtils.texture2d(false)
        GlStateUtils.alpha(false)
        GlStateManager.shadeModel(GL_SMOOTH)

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            pos(right, top, colour = startColour)
            pos(left, top, colour = startColour)
            pos(left, bottom, colour = endColour)
            pos(right, bottom, colour = endColour)
        }

        GlStateManager.shadeModel(GL_FLAT)
        GlStateUtils.alpha(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.depth(true)
        GlStateUtils.blend(false)
        GlStateUtils.matrix(false)
    }

    fun drawGradientRectP(left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int) {
        val startColour = startColor.toColour()
        val endColour = endColor.toColour()

        GlStateUtils.matrix(true)
        GlStateUtils.texture2d(false)
        GlStateUtils.blend(true)
        GlStateUtils.alpha(false)
        GlStateManager.shadeModel(GL_SMOOTH)

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            pos(right, top, 300.0, colour = startColour)
            pos(left, top, 300.0, colour = startColour)
            pos(left, bottom, 300.0, colour = endColour)
            pos(right, bottom, 300.0, colour = endColour)
        }

        GlStateManager.shadeModel(GL_FLAT)
        GlStateUtils.blend(false)
        GlStateUtils.alpha(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.matrix(false)
    }

    fun drawGradientHRect(x: Float, y: Float, x1: Float, y1: Float, topColor: Int, bottomColor: Int) {
        val startColour = topColor.toColour()
        val endColour = bottomColor.toColour()

        Dimension.TwoD {
            glShadeModel(GL_SMOOTH)
            GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
                pos(x, y, colour = startColour)
                pos(x, y1, colour = startColour)
                pos(x1, y1, colour = endColour)
                pos(x1, y, colour = endColour)
            }
            glShadeModel(GL_FLAT)
        }

    }

    @JvmStatic
    fun drawTriangle(x: Double, y: Double, size: Float, theta: Float, color: Int) {
        val colour = color.toColour()

        glTranslated(x, y, 0.0)
        glRotatef(180 + theta, 0f, 0f, 1.0f)

        glColor(color)
        GlStateUtils.blend(true)
        GlStateUtils.texture2d(false)
        GlStateUtils.lineSmooth(true)

        glLineWidth(1.0F)

        GL_TRIANGLE_FAN withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            pos(0.0, 1.0 * size, colour = colour)
            pos(1 * size, -(1.0F * size), colour = colour)
            pos(-(1 * size), -(1.0F * size), colour = colour)
        }

        GlStateUtils.lineSmooth(false)
        GlStateUtils.texture2d(true)
        GlStateUtils.blend(false)

        glRotatef(-180 - theta, 0F, 0F, 1.0F)
        glTranslated(-x, -y, 0.0)
    }

    fun drawTriangle(x: Int, y: Int, type: Int, size: Int, color: Int) {
        glPushMatrix()
        glEnable(3042)
        glDisable(3553)
        glBlendFunc(770, 771)

        glColor(color)

        glEnable(2848)
        glHint(3154, 4354)
        glLineWidth(1.0f)
        glShadeModel(7425)
        when (type) {
            0 -> {
                glBegin(2)
                glVertex2d(x.toDouble(), (y + size).toDouble())
                glVertex2d((x + size).toDouble(), (y - size).toDouble())
                glVertex2d((x - size).toDouble(), (y - size).toDouble())
                glEnd()
                glBegin(4)
                glVertex2d(x.toDouble(), (y + size).toDouble())
                glVertex2d((x + size).toDouble(), (y - size).toDouble())
                glVertex2d((x - size).toDouble(), (y - size).toDouble())
                glEnd()
            }
            1 -> {
                glBegin(2)
                glVertex2d(x.toDouble(), y.toDouble())
                glVertex2d(x.toDouble(), (y + size / 2).toDouble())
                glVertex2d((x + size + size / 2).toDouble(), y.toDouble())
                glEnd()
                glBegin(4)
                glVertex2d(x.toDouble(), y.toDouble())
                glVertex2d(x.toDouble(), (y + size / 2).toDouble())
                glVertex2d((x + size + size / 2).toDouble(), y.toDouble())
                glEnd()
            }
            2 -> {
            }
            3 -> {
                glBegin(2)
                glVertex2d(x.toDouble(), y.toDouble())
                glVertex2d(x + size * 1.25, (y - size / 2).toDouble())
                glVertex2d(x + size * 1.25, (y + size / 2).toDouble())
                glEnd()
                glBegin(4)
                glVertex2d(x + size * 1.25, (y - size / 2).toDouble())
                glVertex2d(x.toDouble(), y.toDouble())
                glVertex2d(x + size * 1.25, (y + size / 2).toDouble())
                glEnd()
            }
        }
        glDisable(2848)
        glEnable(3553)
        glDisable(3042)
        glPopMatrix()
    }

    fun drawStrip(x: Int, y: Int, width: Float, angle: Double, points: Float, radius: Float, color: Int) {
        GlStateUtils.matrix(true)
        glTranslated(x.toDouble(), y.toDouble(), 0.0)
        glColor(color)
        glLineWidth(width)

        if (angle > 0) {
            glBegin(GL_LINE_STRIP)
            var i = 0
            while (i < angle) {
                val a = (i * (angle * Math.PI / points)).toFloat()
                val xc = (cos(a.toDouble()) * radius).toFloat()
                val yc = (sin(a.toDouble()) * radius).toFloat()
                glVertex2f(xc, yc)
                i++
            }
            glEnd()
        }

        if (angle < 0) {
            glBegin(GL_LINE_STRIP)
            var i = 0
            while (i > angle) {
                val a = (i * (angle * Math.PI / points)).toFloat()
                val xc = (cos(a.toDouble()) * -radius).toFloat()
                val yc = (sin(a.toDouble()) * -radius).toFloat()
                glVertex2f(xc, yc)
                i--
            }
            glEnd()
        }

        disableGL2D()
        glDisable(GL_MAP1_VERTEX_3)
        GlStateUtils.matrix(false)
    }

    @JvmStatic
    fun rectangle(leftParam: Double, topParam: Double, rightParam: Double, bottomParam: Double, color: Int) {
        var left = leftParam
        var top = topParam
        var right = rightParam
        var bottom = bottomParam

        if (left < right) {
            val lastLeft = left
            left = right
            right = lastLeft
        }

        if (top < bottom) {
            val lastTop = top
            top = bottom
            bottom = lastTop
        }

        val colour = color.toColour()

        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)
        GlStateUtils.depth(false)
        GlStateUtils.texture2d(false)

        GL_QUADS withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            pos(left, bottom, colour = colour)
            pos(right, bottom, colour = colour)
            pos(right, top, colour = colour)
            pos(left, top, colour = colour)
        }

        GlStateUtils.texture2d(true)
        GlStateUtils.depth(true)
        GlStateUtils.blend(false)
        GlStateUtils.matrix(false)
    }

    fun renderItemOnScreen(x: Int, y: Int, itemStack: ItemStack, sideRender: Boolean, value: Int) {
        GlStateUtils.matrix(true)

        GlStateUtils.blend(true)
        GlStateUtils.texture2d(true)

        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableDepth()
        GlStateManager.translate(0.0f, 0.0f, 700.0F)
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.clear(GL_DEPTH_BUFFER_BIT)

        Globals.mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)

        if (sideRender) {
            Globals.mc.renderItem.renderItemOverlayIntoGUI(Globals.mc.fontRenderer, itemStack, x, y, "")
        } else {
            Globals.mc.renderItem.renderItemOverlays(Globals.mc.fontRenderer, itemStack, x, y)
        }

        RenderHelper.disableStandardItemLighting()
        if (sideRender) Globals.mc.fontRenderer.drawStringWithShadow(value.toString(), (x + 17).toFloat(), (y + 5).toFloat(), -1)

        GlStateManager.disableRescaleNormal()
        GlStateUtils.blend(false)

        GlStateUtils.matrix(false)
    }

    fun prepareScissorBox(sr: ScaledResolution, x: Float, y: Float, width: Float, height: Float) {
        val x2 = x + width
        val y2 = y + height
        val factor = sr.scaleFactor
        glScissor((x * factor).toInt(), ((sr.scaledHeight - y2) * factor).toInt(), ((x2 - x) * factor).toInt(), ((y2 - y) * factor).toInt())
    }

    fun makeScissorBox(x: Float, y: Float, x2: Float, y2: Float) {
        val scaledResolution = ScaledResolution(Globals.mc)
        val factor = scaledResolution.scaleFactor
        glScissor((x * factor).toInt(), ((scaledResolution.scaledHeight - y2) * factor).toInt(), ((x2 - x) * factor).toInt(), ((y2 - y) * factor).toInt())
    }

    fun scissorBox(x: Int, y: Int, xEnd: Int, yEnd: Int) {
        if (Globals.mc.currentScreen == null) return
        val width = xEnd - x
        val height = yEnd - y
        val sr = ScaledResolution(Globals.mc)
        val factor = sr.scaleFactor
        val bottomY = Globals.mc.currentScreen!!.height - yEnd
        glScissor(x * factor, bottomY * factor, width * factor, height * factor)
    }

    fun drawCircle3D(x: Float, y: Float, radius: Float, hex: Int) {
        val colour = hex.toColour()

        GlStateUtils.matrix(true)
        GlStateUtils.blend(true)
        GlStateUtils.texture2d(false)
        GlStateUtils.lineSmooth(true)

        GL_TRIANGLE_FAN withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            for (i in 0..360) {
                val x2 = sin(i.toRadian()) * (radius / 2)
                val y2 = cos(i.toRadian()) * (radius / 2)
                pos(x + radius / 2 + x2, y + radius / 2 + y2, colour = colour)
            }
        }

        GL_LINE_LOOP withVertexFormat DefaultVertexFormats.POSITION_COLOR drawBuffer {
            for (i in 0..360) {
                val x2 = sin(i.toRadian()) * (radius / 2)
                val y2 = cos(i.toRadian()) * (radius / 2)
                pos(x + radius / 2 + x2, y + radius / 2 + y2, colour = colour)
            }
        }

        GlStateUtils.lineSmooth(false)
        GlStateUtils.texture2d(true)

        GlStateUtils.blend(false)
        GlStateUtils.matrix(false)
    }

    fun renderCrosses(box: AxisAlignedBB) {
        GL_LINES withVertexFormat DefaultVertexFormats.POSITION drawBuffer {
            pos(box.maxX, box.maxY, box.maxZ)
            pos(box.maxX, box.minY, box.minZ)
            pos(box.maxX, box.maxY, box.minZ)
            pos(box.minX, box.maxY, box.maxZ)
            pos(box.minX, box.maxY, box.minZ)
            pos(box.maxX, box.minY, box.minZ)
            pos(box.minX, box.minY, box.maxZ)
            pos(box.maxX, box.maxY, box.maxZ)
            pos(box.minX, box.minY, box.maxZ)
            pos(box.minX, box.maxY, box.minZ)
            pos(box.minX, box.minY, box.minZ)
            pos(box.maxX, box.minY, box.maxZ)
        }
    }


    private fun drawBBESPOutline(bb: AxisAlignedBB, red: Int, green: Int, blue: Int, alpha: Int, width: Float) { // prepareGL3D();
        MuffinTessellator.prepareGL()

        MuffinTessellator.begin(GL_LINES)
        MuffinTessellator.drawOutline(bb, Colour(red, green, blue), alpha, GeometryMasks.Quad.ALL, width)
        MuffinTessellator.render()
        MuffinTessellator.releaseGL()
    }

    fun drawBBESP(bb: AxisAlignedBB, red: Int, green: Int, blue: Int, alpha: Int) {
        MuffinTessellator.prepareGL()
        MuffinTessellator.begin(GL_QUADS)
        MuffinTessellator.drawBox(bb, Colour(red, green, blue), alpha, GeometryMasks.Quad.ALL)
        MuffinTessellator.render()
        MuffinTessellator.releaseGL()
    }

    fun drawBBGlowEspBox(bb: AxisAlignedBB, colour: Colour, outlineAlpha: Int, lineWidth: Float, glowHeight: Double, flatOutline: Boolean) {
        MuffinTessellator.prepareGL()

        GlStateUtils.depth(false)
        glShadeModel(GL_SMOOTH)
        GlStateManager.glLineWidth(lineWidth)

        MuffinTessellator.begin(GL_QUADS)
        MuffinTessellator.drawGlowESPFilled(bb, colour, glowHeight)
        MuffinTessellator.render()

        MuffinTessellator.begin(GL_LINES)
        MuffinTessellator.drawGlowESPOutline(bb, colour.apply { a = outlineAlpha }, glowHeight, flatOutline)
        MuffinTessellator.render()

        GlStateUtils.depth(true)
        MuffinTessellator.releaseGL()
    }

    private fun drawBlockPosESP(pos: BlockPos, red: Int, green: Int, blue: Int, alpha: Int) {
        val bb = interpolatePos(pos, 1.0F)

        MuffinTessellator.prepareGL()
        MuffinTessellator.begin(GL_QUADS)
        MuffinTessellator.drawBox(bb, Colour(red, green, blue), alpha, GeometryMasks.Quad.ALL)
        MuffinTessellator.render()
        MuffinTessellator.releaseGL()
    }

    private fun drawBlockPosOutlineESP(pos: BlockPos, red: Int, green: Int, blue: Int, alpha: Int, width: Float) {
        val bb = interpolatePos(pos, 1f)

        MuffinTessellator.prepareGL()
        MuffinTessellator.begin(GL_LINES)
        MuffinTessellator.drawOutline(bb, Colour(red, green, blue), alpha, GeometryMasks.Quad.ALL, width)
        MuffinTessellator.render()
        MuffinTessellator.releaseGL()
    }

    /**
     * Solid Box with BlockPos
     */
    fun drawBlockESP(pos: BlockPos, argb: Int) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF

        drawBlockESP(pos, r, g, b, a)
    }

    fun drawBlockESP(pos: BlockPos, colour: Colour) {
        drawBlockESP(pos, colour.r, colour.g, colour.b, colour.a)
    }

    @JvmStatic
    fun drawBlockESP(pos: BlockPos, red: Int, green: Int, blue: Int, alpha: Int) {
        if (isInViewFrustum(pos)) drawBlockPosESP(pos, red, green, blue, alpha)
    }

    fun drawBlockOutlineESP(pos: BlockPos, argb: Int, width: Float) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF

        drawBlockOutlineESP(pos, r, g, b, a, width)
    }

    @JvmStatic
    fun drawBlockOutlineESP(pos: BlockPos, red: Int, green: Int, blue: Int, alpha: Int, width: Float) {
        if (isInViewFrustum(pos)) {
            drawBlockPosOutlineESP(pos, red, green, blue, alpha, width)
        }
    }

    fun drawBlockOutlineESP(pos: BlockPos, colour: Colour, width: Float) {
        drawBlockOutlineESP(pos, colour.r, colour.g, colour.b, colour.a, width)
    }

    fun drawBlockFullESP(pos: BlockPos, argb: Int, width: Float) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF
        if (isInViewFrustum(pos)) {
            drawBlockPosESP(pos, r, g, b, a)
            drawBlockPosOutlineESP(pos, r, g, b, a, width)
        }
    }

    @JvmStatic
    fun drawBlockFullESP(pos: BlockPos, red: Int, green: Int, blue: Int, alpha: Int, width: Float) {
        if (isInViewFrustum(pos)) {
            drawBlockPosESP(pos, red, green, blue, alpha)
            drawBlockPosOutlineESP(pos, red, green, blue, 255, width)
        }
    }

    fun drawBlockFullESP(pos: BlockPos, colour: Colour, width: Float) {
        if (isInViewFrustum(pos)) {
            drawBlockPosESP(pos, colour.r, colour.g, colour.b, colour.a)
            drawBlockPosOutlineESP(pos, colour.r, colour.g, colour.b, 255, width)
        }
    }

    /**
     * Solid box with AxisAlignedBB
     */
    fun drawBoxESP(bb: AxisAlignedBB, argb: Int) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF

        drawBoxESP(bb, r, g, b, a)
    }

    fun drawBoxESP(bb: AxisAlignedBB, red: Int, green: Int, blue: Int, alpha: Int) {
        if (isInViewFrustum(bb)) drawBBESP(bb, red, green, blue, alpha)
    }

    fun drawBoxESP(bb: AxisAlignedBB, colour: Color) {
        drawBoxESP(bb, colour.red, colour.green, colour.blue, colour.alpha)
    }

    fun drawBoxESP(bb: AxisAlignedBB, colour: Colour) {
        drawBoxESP(bb, colour.r, colour.g, colour.b, colour.a)
    }

    fun drawBoxOutlineESP(bb: AxisAlignedBB, argb: Int, width: Float) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF

        drawBoxOutlineESP(bb, r, g, b, a, width)
    }

    fun drawBoxOutlineESP(bb: AxisAlignedBB, red: Int, green: Int, blue: Int, alpha: Int, width: Float) {
        if (isInViewFrustum(bb)) {
            drawBBESPOutline(bb, red, green, blue, alpha, width)
        }
    }

    fun drawBoxOutlineESP(bb: AxisAlignedBB, colour: Color, width: Float) {
        drawBoxOutlineESP(bb, colour.red, colour.green, colour.blue, colour.alpha, width)
    }

    fun drawBoxOutlineESP(bb: AxisAlignedBB, colour: Colour, width: Float) {
        drawBoxOutlineESP(bb, colour.r, colour.g, colour.b, colour.a, width)
    }

    fun drawBoxFullESP(bb: AxisAlignedBB, argb: Int, width: Float) {
        val a = argb ushr 24 and 0xFF
        val r = argb ushr 16 and 0xFF
        val g = argb ushr 8 and 0xFF
        val b = argb and 0xFF

        if (isInViewFrustum(bb)) {
            drawBBESP(bb, r, g, b, a)
            drawBBESPOutline(bb, r, g, b, a, width)
        }
    }

    fun drawBoxFullESP(bb: AxisAlignedBB, red: Int, green: Int, blue: Int, alpha: Int, width: Float) {
        if (isInViewFrustum(bb)) {
            drawBBESP(bb, red, green, blue, alpha)
            drawBBESPOutline(bb, red, green, blue, 255, width)
        }
    }

    fun drawBoxFullESP(bb: AxisAlignedBB, colour: Colour, width: Float) {
        drawBoxFullESP(bb, colour.r, colour.g, colour.b, colour.a, width)
    }


    fun isInViewFrustum(entity: Entity): Boolean {
        return isBoundingBoxInViewFrustum(entity.entityBoundingBox) || entity.ignoreFrustumCheck
    }

    private fun isBoundingBoxInViewFrustum(bb: AxisAlignedBB): Boolean {
        val current = Globals.mc.renderViewEntity ?: Globals.mc.player

        if (current != null) {
            val vector = MathUtils.interpolateEntity(current, renderPartialTicks)
            camera.setPosition(vector.x, vector.y, vector.z)
        }

        return camera.isBoundingBoxInFrustum(bb)
    }

    fun isInViewFrustum(bb: AxisAlignedBB): Boolean {
        val current = Globals.mc.renderViewEntity ?: Globals.mc.player
        if (current != null) {
            val vector = MathUtils.interpolateEntity(current, renderPartialTicks)
            camera.setPosition(vector.x, vector.y, vector.z)
        }
        return camera.isBoundingBoxInFrustum(AxisAlignedBB(bb.minX + viewerPosX, bb.minY + viewerPosY, bb.minZ + viewerPosZ, bb.maxX + viewerPosX, bb.maxY + viewerPosY, bb.maxZ + viewerPosZ))
    }

    @JvmStatic
    fun isInViewFrustum(pos: BlockPos): Boolean {
        val bb = AxisAlignedBB(pos.x - viewerPosX, pos.y - viewerPosY, pos.z - viewerPosZ, pos.x + 1 - viewerPosX, pos.y + 1 - viewerPosY, pos.z + 1 - viewerPosZ)

        val current = Globals.mc.renderViewEntity ?: Globals.mc.player

        if (current != null) {
            val vector = MathUtils.interpolateEntity(current, renderPartialTicks)
            camera.setPosition(vector.x, vector.y, vector.z)
        }

        return camera.isBoundingBoxInFrustum(AxisAlignedBB(bb.minX + viewerPosX, bb.minY + viewerPosY, bb.minZ + viewerPosZ, bb.maxX + viewerPosX, bb.maxY + viewerPosY, bb.maxZ + viewerPosZ))
    }

    fun glColorClient(alpha: Int) {
        val r = Muffin.getInstance().fontManager.publicRed
        val g = Muffin.getInstance().fontManager.publicGreen
        val b = Muffin.getInstance().fontManager.publicBlue
        glColor(r, g, b, alpha)
    }

    @JvmStatic
    fun glColorClient(alpha: Float) {
        val r = Muffin.getInstance().fontManager.publicRed
        val g = Muffin.getInstance().fontManager.publicGreen
        val b = Muffin.getInstance().fontManager.publicBlue
        glColor(r / 255f, g / 255f, b / 255f, alpha)
    }

    @JvmStatic
    fun glColor(red: Int, green: Int, blue: Int, alpha: Int) {
        glColor4f(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    }

    @JvmStatic
    fun glColor(holder: Colour) {
        glColor(holder.r, holder.g, holder.b, holder.a)
    }

    @JvmStatic
    fun glColor(red: Float, green: Float, blue: Float, alpha: Float) {
        glColor4f(red, green, blue, alpha)
    }

    @JvmStatic
    fun glColor(color: Color?) {
        if (color == null) return
        glColor(color.red, color.green, color.blue, color.alpha)
    }

    @JvmStatic
    fun glColor(hex: Int) {
        val alpha = hex shr 24 and 0xFF
        val red = hex shr 16 and 0xFF
        val green = hex shr 8 and 0xFF
        val blue = hex and 0xFF

        glColor(red, green, blue, alpha)
    }

    fun quickDrawHLine(xIn: Float, widthIn: Float, y: Float, color: Int) {
        var x = xIn
        var width = widthIn

        if (width < x) {
            val i = x
            x = width
            width = i
        }
        quickDrawRect(x, y, width + 1, y + 1, color)
    }

    fun quickDrawVLine(x: Float, yIn: Float, heightIn: Float, color: Int) {
        var y = yIn
        var height = heightIn

        if (height < y) {
            val i = y
            y = height
            height = i
        }
        quickDrawRect(x, y + 1, x + 1, height, color)
    }

    fun drawHLine(xIn: Float, widthIn: Float, y: Float, color: Int) {
        var x = xIn
        var width = widthIn

        if (width < x) {
            val i = x
            x = width
            width = i
        }
        drawRect(x, y, width + 1, y + 1, color)
    }

    fun drawVLine(x: Float, yIn: Float, heightIn: Float, color: Int) {
        var y = yIn
        var height = heightIn

        if (height < y) {
            val i = y
            y = height
            height = i
        }

        drawRect(x, y + 1, x + 1, height, color)
    }

    fun drawHLine(xIn: Float, widthIn: Float, y: Float, colour: Int, color: Int) {
        var x = xIn
        var width = widthIn

        if (width < x) {
            val i = x
            x = width
            width = i
        }

        drawGradientRect(x, y, width + 1, y + 1, colour, color)
    }

    fun polygon(x: Double, y: Double, sideLengthIn: Double, amountOfSides: Int, filled: Boolean, color: Color?) {
        val sideLength = sideLengthIn / 2.0

        Dimension.TwoD {
            if (color != null) glColor(color)
            if (!filled) glLineWidth(1.0F)

            GlStateUtils.lineSmooth(true)

            (if (filled) GL_TRIANGLE_FAN else GL_LINE_STRIP) withVertexFormat DefaultVertexFormats.POSITION drawBuffer {
                for (i in 0..amountOfSides) {
                    val angle = i * (Math.PI * 2) / amountOfSides
                    pos(x + sideLength * cos(angle) + sideLength, y + sideLength * sin(angle) + sideLength)
                }
            }

            GlStateUtils.lineSmooth(false)
        }

    }

    fun polygon(x: Double, y: Double, sideLength: Double, amountOfSides: Int, filled: Boolean) {
        polygon(x, y, sideLength, amountOfSides, filled, null)
    }

    fun polygon(x: Double, y: Double, sideLength: Double, amountOfSides: Int, color: Color?) {
        polygon(x, y, sideLength, amountOfSides, true, color)
    }

    fun polygon(x: Double, y: Double, sideLength: Double, amountOfSides: Int) {
        polygon(x, y, sideLength, amountOfSides, true, null)
    }

    @JvmStatic
    fun circle(x: Double, y: Double, radius: Double, filled: Boolean, color: Color?) {
        polygon(x, y, radius, 360, filled, color)
    }

    fun circle(x: Double, y: Double, radius: Double, filled: Boolean) {
        polygon(x, y, radius, 360, filled)
    }

    @JvmStatic
    fun circle(x: Double, y: Double, radius: Double, color: Color?) {
        polygon(x, y, radius, 360, color)
    }

    fun circle(x: Double, y: Double, radius: Double) {
        polygon(x, y, radius, 360)
    }

    fun interpolatePos(pos: BlockPos, height: Float): AxisAlignedBB {
        return AxisAlignedBB(
            pos.x - viewerPosX,
            pos.y - viewerPosY,
            pos.z - viewerPosZ,
            pos.x - viewerPosX + 1,
            pos.y - viewerPosY + height,
            pos.z - viewerPosZ + 1
        )
    }

    fun interpolateAxis(bb: AxisAlignedBB): AxisAlignedBB {
        return AxisAlignedBB(
            bb.minX - viewerPosX,
            bb.minY - viewerPosY,
            bb.minZ - viewerPosZ,
            bb.maxX - viewerPosX,
            bb.maxY - viewerPosY,
            bb.maxZ - viewerPosZ
        )
    }

}
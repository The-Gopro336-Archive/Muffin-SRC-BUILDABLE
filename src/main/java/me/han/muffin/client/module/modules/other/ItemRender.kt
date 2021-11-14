package me.han.muffin.client.module.modules.other

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.render.RenderCrystalSizeEvent
import me.han.muffin.client.event.events.render.RenderEntitySizeEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.pow

internal object ItemRender: Module("Renderer", Category.OTHERS, true, "Do changes to render stuff.") {

    private val currentHand = EnumValue(Hand.Main, "Hand")
    private val entitySize = Value(false, "EntitySize")

    private val entityScale = NumberValue({ entitySize.value }, 0.5, -2.0, 2.0, 0.01, "EntityScale")
    private val entityTranslate = NumberValue({ entitySize.value }, 1.5, -2.0, 2.0, 0.01, "EntityTranslate")

    private val crystalSize = Value(false, "CrystalSize")
    private val crystalScale = NumberValue({ crystalSize.value }, 0.5, -2.0, 2.0, 0.01, "CrystalScale")
    private val crystalTranslate = NumberValue({ crystalSize.value }, 1.5, -2.0, 2.0, 0.01, "CrystalTranslate")

    val itemsAngle = Value(false, "ItemAngle")

    private val damageColour = Value(false, "DamageColour")
    val damageRed = NumberValue({ damageColour.value },255F, 0F, 255F, 1F, "DamageRed")
    val damageGreen = NumberValue({ damageColour.value },0F, 0F, 255F, 1F, "DamageGreen")
    val damageBlue = NumberValue({ damageColour.value },0F, 0F, 255F, 1F, "DamageBlue")
    val damageAlpha = NumberValue({ damageColour.value },77F, 0F, 255F, 1F, "DamageAlpha")

    private val worldColour = Value(false, "WorldColour")
    private val worldRed = NumberValue({ worldColour.value },221, 0, 255, 1, "WorldRed")
    private val worldGreen = NumberValue({ worldColour.value },192, 0, 255, 1, "WorldGreen")
    private val worldBlue = NumberValue({ worldColour.value },0, 0, 255, 1, "WorldBlue")
    private val worldAlpha = NumberValue({ worldColour.value },77, 0, 255, 1, "WorldAlpha")

    private val viewPort = Value(false, "Viewport")
    val aspectRatioWidth = NumberValue({ viewPort.value }, 1, 0, 10, 1, "AspectRatioWidth")
    val aspectRatioHeight = NumberValue({ viewPort.value }, 1, 0, 10, 1, "AspectRatioHeight")
    val fovPortX = NumberValue({ viewPort.value }, 20, 0, 180, 2, "FOV-X")
    val fovPortY = NumberValue({ viewPort.value }, 20, 0, 180, 2, "FOV-Y")

    private val mainItemView = Value({ currentHand.value == Hand.Main }, true, "MainItemView")
    private val offItemView = Value({ currentHand.value == Hand.Off }, true, "OffItemView")

    private val swingChange = Value(false, "SwingChange")
    private val swingSpeedValue = NumberValue(1F, 0F, 2F, 0.1F, "SwingSpeed")

    private val mainX = NumberValue({ currentHand.value == Hand.Main && mainItemView.value }, 0.0, -1.0, 1.0, 0.01, "MainOffset")
    private val mainY = NumberValue({ currentHand.value == Hand.Main && mainItemView.value }, 0.0, -1.0, 1.0, 0.01, "MainHeight")
    private val mainZ = NumberValue({ currentHand.value == Hand.Main && mainItemView.value }, 0.0, -3.0, 1.0, 0.01, "MainFar")

    private val offX = NumberValue({ currentHand.value == Hand.Off && offItemView.value }, 0.0, -1.0, 1.0, 0.01, "OffOffset")
    private val offY = NumberValue({ currentHand.value == Hand.Off && offItemView.value }, 0.0, -1.0, 1.0, 0.01, "OffHeight")
    private val offZ = NumberValue({ currentHand.value == Hand.Off && offItemView.value }, 0.0, -3.0, 1.0, 0.01, "OffFar")

    private val mainItemX = Value({ currentHand.value == Hand.Main }, false, "MainItemRX")
    private val mainItemY = Value({ currentHand.value == Hand.Main }, false, "MainItemRY")
    private val mainItemZ = Value({ currentHand.value == Hand.Main }, false, "MainItemRZ")
    private val mainItemXValue = NumberValue({ currentHand.value == Hand.Main && mainItemX.value }, 0F, -360F, 360F, 2F, "MainItemRXV")
    private val mainItemYValue = NumberValue({ currentHand.value == Hand.Main && mainItemY.value }, 0F, -360F, 360F, 2F, "MainItemRYV")
    private val mainItemZValue = NumberValue({ currentHand.value == Hand.Main && mainItemZ.value }, 0F, -360F, 360F, 2F, "MainItemRZV")

    private val offItemX = Value({ currentHand.value == Hand.Off }, false, "OffItemRX")
    private val offItemY = Value({ currentHand.value == Hand.Off }, false, "OffItemRY")
    private val offItemZ = Value({ currentHand.value == Hand.Off }, false, "OffItemRZ")
    private val offItemXValue = NumberValue({ currentHand.value == Hand.Off && offItemX.value }, 0F, -360F, 360F, 2F, "OffItemRXV")
    private val offItemYValue = NumberValue({ currentHand.value == Hand.Off && offItemY.value }, 0F, -360F, 360F, 2F, "OffItemRYV")
    private val offItemZValue = NumberValue({ currentHand.value == Hand.Off && offItemZ.value }, 0F, -360F, 360F, 2F, "OffItemRZV")

    private val mainHandX = Value({ currentHand.value == Hand.Main }, false, "MainHandRX")
    private val mainHandY = Value({ currentHand.value == Hand.Main }, false, "MainHandRY")
    private val mainHandZ = Value({ currentHand.value == Hand.Main }, false, "MainHandRZ")
    private val mainHandXValue = NumberValue({ currentHand.value == Hand.Main && mainHandX.value }, 0F, -180F, 180F, 2F, "MainHandRXV")
    private val mainHandYValue = NumberValue({ currentHand.value == Hand.Main && mainHandY.value }, 0F, -180F, 180F, 2F, "MainHandRYV")
    private val mainHandZValue = NumberValue({ currentHand.value == Hand.Main && mainHandZ.value }, 0F, -180F, 180F, 2F, "MainHandRZV")

    private val offHandX = Value({ currentHand.value == Hand.Off }, false, "OffHandRX")
    private val offHandY = Value({ currentHand.value == Hand.Off }, false, "OffHandRY")
    private val offHandZ = Value({ currentHand.value == Hand.Off }, false, "OffHandRZ")
    private val offHandXValue = NumberValue({ currentHand.value == Hand.Off && offHandX.value }, 0F, -180F, 180F, 2F, "OffHandRXV")
    private val offHandYValue = NumberValue({ currentHand.value == Hand.Off && offHandY.value }, 0F, -180F, 180F, 2F, "OffHandRYV")
    private val offHandZValue = NumberValue({ currentHand.value == Hand.Off && offHandZ.value }, 0F, -180F, 180F, 2F, "OffHandRZV")

    private val mainItemScaling = Value({ currentHand.value == Hand.Main }, false, "MainItemScaling")
    private val mainItemScale = NumberValue({ currentHand.value == Hand.Main && mainItemScaling.value }, 0.0F, -1.0, 1.0, 0.01, "MainItemScale")

    private val offItemScaling = Value({ currentHand.value == Hand.Off }, false, "OffItemScaling")
    private val offItemScale = NumberValue({ currentHand.value == Hand.Off && offItemScaling.value }, 0.0F, -1.0, 1.0, 0.01, "OffItemScale")

    val handScaling = Value(false, "HandScaling")
    val handScale = NumberValue({ handScaling.value }, 0.0, -1.0, 1.0, 0.01, "HandScale")

    private enum class Hand {
        Main, Off
    }

    @JvmStatic val isViewPortRatio: Boolean get() = isEnabled && viewPort.value && aspectRatioWidth.value > 0.0
    @JvmStatic val isViewPortFov: Boolean get() = isEnabled && viewPort.value && fovPortX.value > 0.0

    @JvmStatic val isCustomDamageColour: Boolean get() = isEnabled && damageColour.value

    @JvmStatic val isCustomWorldColour: Boolean get() = isEnabled && worldColour.value
    @JvmStatic val customWorldColour: Colour get() = Colour(worldRed.value, worldGreen.value, worldBlue.value, worldAlpha.value)

    init {
        addSettings(
            currentHand,
            damageColour, damageRed, damageGreen, damageBlue, damageAlpha,
            worldColour, worldRed, worldGreen, worldBlue, worldAlpha,
            //viewPort, aspectRatioWidth, aspectRatioHeight, fovPortX, fovPortY,
            itemsAngle,
            entitySize, entityScale, entityTranslate,
            crystalSize, crystalScale, crystalTranslate,
            mainItemView, mainX, mainY, mainZ,
            offItemView, offX, offY, offZ,
            mainItemX, mainItemY, mainItemZ,
            mainItemXValue, mainItemYValue, mainItemZValue,
            offItemX, offItemY, offItemZ,
            offItemXValue, offItemYValue, offItemZValue,
            mainItemScaling, mainItemScale,
            offItemScaling, offItemScale,
            handScaling, handScale,
            mainHandX, mainHandY, mainHandZ,
            mainHandXValue, mainHandYValue, mainHandZValue,
            offHandX, offHandY, offHandZ,
            offHandXValue, offHandYValue, offHandZValue
        )
    }
/*
    @Listener
    private fun onRenderHand(event: RenderHandEvent) {
        if (firstPerson.value && isRealArmsEnabled(Globals.mc.player)) {
            event.cancel()
        }
    }

    @Listener
    private fun onTicking(event: RenderTickEvent) {
        if (fullNullCheck()) return

        if (!firstPerson.value) {
            spawnDelay = 40
            return
        }

        try {
            // Decrement timers
            if (checkEnableModDelay > 0) --checkEnableModDelay
            if (checkEnableRealArmsDelay > 0) --checkEnableRealArmsDelay

            // Check if dummy needs to be spawned
            if (dummy == null) {
                // It does, are we in a respawn waiting interval?
                if (spawnDelay > 0) {
                    --spawnDelay
                } else {
                    attemptDummySpawn(Globals.mc.player)
                }
            } else {
                var needsReset = false

                // Did the player change dimensions on us? If so, reset the dummy.
                if (dummy!!.world.provider.dimension != Globals.mc.player.world.provider.dimension) {
                    needsReset = true
                    MixinLoader.MIXIN_LOGGER.info(this.javaClass.name + ": Respawning dummy because player changed dimension.")
                }

                if (dummy!!.getDistanceSq(Globals.mc.player) > 5) {
                    needsReset = true
                    MixinLoader.MIXIN_LOGGER.info(this.javaClass.name + ": Respawning dummy because player and dummy became separated.")
                }

                if (dummy!!.lastTickUpdated < Globals.mc.player.world.totalWorldTime - 20) {
                    needsReset = true
                    MixinLoader.MIXIN_LOGGER.info(this.javaClass.name + ": Respawning dummy because state became stale. (Is the server lagging?)")
                }

                if (needsReset) {
                    resetDummy()
                }
            }

        } catch (e: Exception) {
            // If anything goes wrong, shut the mod off and write an error to the logs.
            MixinLoader.MIXIN_LOGGER.error(this.javaClass.name + ".onEvent(TickEvent.ClientTickEvent)", e)
        }
    }

    // Handles dummy spawning
    fun attemptDummySpawn(player: EntityPlayer) {
        try {
            if (dummy != null) dummy!!.setDead()
            dummy = EntityPlayerDummy(player.world)
            dummy!!.setPositionAndRotation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch)
            player.world.spawnEntity(dummy)
        } catch (e: Exception) {
            MixinLoader.MIXIN_LOGGER.error(this.javaClass.name + ": failed to spawn PlayerDummy! Will retry. Exception:", e.toString())
            e.printStackTrace()
            resetDummy()
        }
    }

    // Handles killing off defunct dummies and scheduling respawns
    fun resetDummy() {
        if (dummy != null) dummy!!.setDead()
        dummy = null
        spawnDelay = 40
    }

    fun isRealArmsEnabled(player: EntityPlayer?): Boolean {
        if (player == null) return false
        if (checkEnableRealArmsDelay == 0L) {
            checkEnableRealArmsDelay = 1
            lastRealArmsCheckResult = true
        }
        return lastRealArmsCheckResult
    }
 */

    fun doPreSmallEntity() {
        if (isDisabled || !entitySize.value) return

        glPushMatrix()
        glScaled(entityScale.value, entityScale.value, entityScale.value)
        glTranslated(0.0, entityTranslate.value, 0.0)
    }

    fun doPostSmallEntity() {
        if (isDisabled || !entitySize.value) return

        glPopMatrix()
    }

    fun doPreSmallCrystal() {
        if (isDisabled || !crystalSize.value) return

        glPushMatrix()
        glScaled(crystalScale.value, crystalScale.value, crystalScale.value)
        glTranslated(0.0, crystalTranslate.value, 0.0)
    }

    fun doPostSmallCrystal() {
        if (isDisabled || !crystalSize.value) return

        glPopMatrix()
    }

/*
    @Listener
    private fun onRenderHeldItem(event: RenderHeldItemEvent) {
        if (!itemView.value) return
        event.cancel()
        event.x = x.value
        event.y = y.value
        event.z = z.value
    }
 */


    @Listener
    private fun onRender3D(event: Render3DEvent) {
    }

    @Listener
    private fun onRenderEntitySize(event: RenderEntitySizeEvent) {
        if (!entitySize.value) return

        if (event.stage == EventStageable.EventStage.PRE) {
            doPreSmallEntity()
        } else {
            doPostSmallEntity()
        }
    }

    @Listener
    private fun onRenderCrystalSize(event: RenderCrystalSizeEvent) {
        if (!crystalSize.value) return

        if (event.stage == EventStageable.EventStage.PRE) {
            doPreSmallCrystal()
        } else {
            doPostSmallCrystal()
        }

    }

    private fun doEatTranslation(handSide: EnumHandSide) {
        val f = Globals.mc.player.itemInUseCount - RenderUtils.renderPartialTicks + 1.0f
        val f1 = f / Globals.mc.player.activeItemStack.maxItemUseDuration
        /*
        if (f1 < 0.8f) {
            val f2 = abs(cos(f / 4.0f * Math.PI.toFloat()) * 0.1f)
            GlStateManager.translate(0.0f, f2, 0.0f)
        }
         */

        val f3 = 1.0f - f1.toDouble().pow(27.0).toFloat()
        val i = if (handSide == EnumHandSide.RIGHT) 1 + mainX.value.toInt() else -1 - offX.value.toInt()
        GlStateManager.translate(f3 * 0.6f * i.toFloat(), f3 * -0.5f, f3 * 0.0f)

        /*
        GlStateManager.rotate(i.toFloat() * f3 * 90.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(f3 * 10.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(i.toFloat() * f3 * 30.0f, 0.0f, 0.0f, 1.0f)
         */
    }

    fun doItemRender(handSide: EnumHandSide) {
        if (handSide == EnumHandSide.RIGHT) {
            if (mainItemView.value) if (Globals.mc.player.isHandActive && Globals.mc.player.activeHand == EnumHand.MAIN_HAND) doEatTranslation(handSide) else GlStateManager.translate(mainX.value, mainY.value, mainZ.value)
            if (mainItemScaling.value) GlStateManager.scale(mainItemScale.value.toDouble(), mainItemScale.value.toDouble(), mainItemScale.value.toDouble())
            if (mainItemX.value) GlStateManager.rotate(mainItemXValue.value, 1F, 0F, 0F)
            if (mainItemY.value) GlStateManager.rotate(mainItemYValue.value, 0F, 1F, 0F)
            if (mainItemZ.value) GlStateManager.rotate(mainItemZValue.value, 0F, 0F, 1F)
        } else if (handSide == EnumHandSide.LEFT) {
            if (offItemView.value) if (Globals.mc.player.isHandActive && Globals.mc.player.activeHand == EnumHand.OFF_HAND) doEatTranslation(handSide) else GlStateManager.translate(offX.value, offY.value, offZ.value)
            if (offItemScaling.value) GlStateManager.scale(offItemScale.value.toDouble(), offItemScale.value.toDouble(), offItemScale.value.toDouble())
            if (offItemX.value) GlStateManager.rotate(offItemXValue.value, 1F, 0F, 0F)
            if (offItemY.value) GlStateManager.rotate(offItemYValue.value, 0F, 1F, 0F)
            if (offItemZ.value) GlStateManager.rotate(offItemZValue.value, 0F, 0F, 1F)
        }
    }

    fun doHandRender(handSide: EnumHandSide) {
        if (handSide == EnumHandSide.RIGHT) {
            if (mainHandX.value) GlStateManager.rotate(mainHandXValue.value, 1F, 0F, 0F)
            if (mainHandY.value) GlStateManager.rotate(mainHandYValue.value, 0F, 1F, 0F)
            if (mainHandZ.value) GlStateManager.rotate(mainHandZValue.value, 0F, 0F, 1F)
        } else {
            if (offHandX.value) GlStateManager.rotate(offHandXValue.value, 1F, 0F, 0F)
            if (offHandY.value) GlStateManager.rotate(offHandYValue.value, 0F, 1F, 0F)
            if (offHandZ.value) GlStateManager.rotate(offHandZValue.value, 0F, 0F, 1F)
        }
    }

    fun doItemAngle() {
        GlStateManager.rotate(-RenderUtils.playerViewY, 0F, 1F, 0F)
        GlStateManager.rotate(RenderUtils.playerViewX, 1F, 0F, 0F)
    }


}
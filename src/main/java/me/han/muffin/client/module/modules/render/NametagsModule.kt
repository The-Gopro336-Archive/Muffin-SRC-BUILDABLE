package me.han.muffin.client.module.modules.render

import com.mojang.realmsclient.gui.ChatFormatting
import me.han.muffin.client.Muffin
import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.render.Render3DEvent
import me.han.muffin.client.event.events.render.entity.RenderPlayerTagsEvent
import me.han.muffin.client.gui.font.AWTFontRenderer
import me.han.muffin.client.manager.managers.FriendManager
import me.han.muffin.client.manager.managers.HoleManager.holeInfo
import me.han.muffin.client.manager.managers.PotionManager
import me.han.muffin.client.manager.managers.TotemPopManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.InfoUtils
import me.han.muffin.client.utils.block.HoleType
import me.han.muffin.client.utils.color.Colour
import me.han.muffin.client.utils.color.ColourGradient
import me.han.muffin.client.utils.color.ColourUtils
import me.han.muffin.client.utils.extensions.kotlin.ceilToInt
import me.han.muffin.client.utils.extensions.kotlin.synchronized
import me.han.muffin.client.utils.extensions.mc.entity.eyePosition
import me.han.muffin.client.utils.extensions.mc.entity.isAlive
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.render.BufferDSL
import me.han.muffin.client.utils.render.GlStateUtils
import me.han.muffin.client.utils.render.GlStateUtils.alpha
import me.han.muffin.client.utils.render.GlStateUtils.blend
import me.han.muffin.client.utils.render.GlStateUtils.cull
import me.han.muffin.client.utils.render.GlStateUtils.depth
import me.han.muffin.client.utils.render.GlStateUtils.depthMask
import me.han.muffin.client.utils.render.GlStateUtils.matrix
import me.han.muffin.client.utils.render.GlStateUtils.polygon
import me.han.muffin.client.utils.render.GlStateUtils.resetColour
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.roundToInt

internal object NametagsModule: Module("Nametags", Category.RENDER, "Better nametags than vanilla nametags.") {

    private val drawMode = EnumValue(DrawMode.Rect, "DrawMode")
    private val customFont = Value(true, "CustomFont")
    private val effects = Value(false, "Effects")
    private val armour = Value(true, "Armour")
    private val entityID = Value(false, "EntityID")
    private val gameMode = Value(true, "GameMode")
    private val ping = Value(true, "Ping")
    private val health = Value(true, "Health")
    private val totems = Value(true, "Totems")
    private val renderDurability = Value(true, "Durability")
    private val renderEnchants = Value(true, "Enchants")
    private val limitEnchants = Value(false, "LimitEnchants")
    private val frustumCheck = Value(true, "FrustumCheck")

    private val holeColour = Value(false, "HoleColour")
    private val scaling = NumberValue(2.0F, 0.1F, 10.0F, 0.1F, "Scaling")

    private val ENCHANTMENT_REGISTRY = Enchantment.REGISTRY
    private val entityRenderMap = TreeMap<EntityPlayer, PlayerDataStore>(compareBy { Globals.mc.player.eyePosition.distanceTo(it.eyePosition) }).synchronized()

    private var updateThread: Thread? = null
    private val threadPool = Executors.newCachedThreadPool()

    private val fontManager = Muffin.getInstance().fontManager

    private enum class DrawMode {
        Rounded, Rect, BorderRect
    }

    private val pingColorGradient = ColourGradient(
        0f to Colour(101, 101, 101),
        0.1f to Colour(20, 232, 20),
        20f to Colour(20, 232, 20),
        150f to Colour(20, 232, 20),
        300f to Colour(150, 0, 0)
    )

    private val healthColorGradient = ColourGradient(
        0f to Colour(180, 20, 20),
        50f to Colour(240, 220, 20),
        100f to Colour(20, 232, 20)
    )

    init {
        addSettings(
            drawMode, customFont, effects, armour, entityID, gameMode, ping, health, totems, renderDurability, renderEnchants, limitEnchants, frustumCheck, holeColour, scaling
        )
    }

    @Listener
    private fun onRenderPlayerTags(event: RenderPlayerTagsEvent) {
        event.cancel()
    }

    private fun getEnchantmentPair(stack: ItemStack, availableEnchantments: Set<Enchantment>): Array<String> {
        val enchantments = ArrayList<String>()
        if (renderEnchants.value) {
            for (enchantment in availableEnchantments) {
                val colour = if (enchantment == Enchantments.VANISHING_CURSE || enchantment == Enchantments.BINDING_CURSE) ChatFormatting.RED else ChatFormatting.RESET
                enchantments.add(colour.toString() + getEnchantment(enchantment, EnchantmentHelper.getEnchantmentLevel(enchantment, stack)))
            }
        }
        return enchantments.toTypedArray()
    }

    private fun getPerArmourData(armour: ItemStack): Pair<Int, Array<String>> {
        val armourEnchantments = EnchantmentHelper.getEnchantments(armour)
        return Pair(armourEnchantments.size, getEnchantmentPair(armour, armourEnchantments.keys))
    }

    private fun getArmourData(armourInventory: NonNullList<ItemStack>): Array<Triple<ItemStack, Int, Array<String>>> {
        val armourData = ArrayList<Triple<ItemStack, Int, Array<String>>>()

        for (i in 3 downTo 0) {
            val stack = armourInventory[i]
            val enchantmentData = getPerArmourData(stack)
            armourData.add(Triple(stack, enchantmentData.first, enchantmentData.second))
        }

        return armourData.toTypedArray()
    }

    private fun getItemData(stack: ItemStack): OffMainItemEnchantments {
        val stackEnchantments = EnchantmentHelper.getEnchantments(stack)
        return OffMainItemEnchantments(stack, stackEnchantments.size, getEnchantmentPair(stack, stackEnchantments.keys))
    }

    private fun updateNametags() {
        val tempNametag = hashMapOf<EntityPlayer, PlayerDataStore>()

        val camera = Globals.mc.renderViewEntity ?: Globals.mc.player ?: return
        val interpolatedCamera = MathUtils.interpolateEntity(camera, RenderUtils.renderPartialTicks)

        Globals.mc.world.playerEntities
            .filter { it != null && it != camera && it.isAlive && !it.name.startsWith("Body #") && (!frustumCheck.value || RenderUtils.isInViewFrustum(it)) }
            .forEach { player ->
                val vector = MathUtils.getInterpolatedRenderPos(player, RenderUtils.renderPartialTicks)

                val mainHand = player.heldItemMainhand
                val offHand = player.heldItemOffhand

                val armourInventory = player.inventory.armorInventory

                tempNametag[player] =
                    PlayerDataStore(
                        camera, interpolatedCamera, getDisplayNameString(player, TotemPopManager.getTotemPopString(player)),
                        getNametagSize(camera, vector.x, vector.y, vector.z),
                        getBorderColour(player),
                        getItemData(mainHand),
                        getItemData(offHand),
                        getArmourData(armourInventory)
                )
            }

        synchronized(entityRenderMap) {
            entityRenderMap.clear()
            entityRenderMap.putAll(tempNametag)
        }

    }


    private fun updateNametagsThreading() {
        if (updateThread == null || !updateThread!!.isAlive || updateThread!!.isInterrupted) {
            updateThread = thread(start = false) { updateNametags() }
            threadPool.execute(updateThread!!)
        }
    }

    private fun getBorderColour(player: EntityPlayer): Colour {
        val clientColour = ColourUtils.getClientColour(255)
        if (!holeColour.value) return clientColour
        return when (player.holeInfo.type) {
            HoleType.None -> clientColour
            HoleType.Bedrock -> Colour(12, 218, 13)
            HoleType.Obsidian -> Colour(218, 12, 13)
            else -> Colour(205, 132, 23)
        }
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.POST) return
        updateNametagsThreading()
    }

    @Listener
    private fun onRender3D(event: Render3DEvent) {
        if (fullNullCheck()) return

        synchronized(entityRenderMap) {
            for ((player, data) in entityRenderMap) {
                val vector = MathUtils.getInterpolatedRenderPos(player, event.partialTicks)
                data.renderNameTags(player, vector.x, vector.y, vector.z)
            }
        }

    }

    /*
    private float getNametagSize(final EntityLivingBase player) {
        final ScaledResolution scaledRes = new ScaledResolution(Globals.mc);
        final double twoDscale = scaledRes.getScaleFactor() / Math.pow(scaledRes.getScaleFactor(), 0.0 + scaling.getValue());
        return (float)twoDscale + Globals.mc.player.getDistance(player) / 5.6f;
    }
     */
    private fun getNametagSize(entity: Entity, x: Double, y: Double, z: Double): Float {
        val distance = entity.getDistance(x + RenderUtils.viewerPosX, y + RenderUtils.viewerPosY, z + RenderUtils.viewerPosZ)
        var size = (0.0018F + scaling.value.div(1000) * distance).toFloat()
        if (distance <= 8) size = 0.0245F
        return size
    }

    private fun getGMText(player: EntityPlayer): String {
        if (player.isCreative) return "[C]"
        if (player.isSpectator) return "[I]"

        if (!player.isAllowEdit && !player.isSpectator) return "[A]"
        return if (!player.isCreative && !player.isSpectator && player.isAllowEdit) {
            "[S]"
        } else ""
    }

    private fun getDisplayNameString(player: EntityPlayer, totemPops: String): Pair<String, Boolean> {
        val builder = StringBuilder()
        var isFriend = false

        if (effects.value) builder.append("%s%s".format(PotionManager.getTextRadarPotion(player), "§r"))
        var hasColourCode = builder.toString().replace("§.".toRegex(), "").isNotEmpty()

        val friend = FriendManager.getFriendByAliasOrLabel(player.name)
        if (friend != null) isFriend = true

        builder.append(
            "%s%s".format(if (hasColourCode) " " else "", if (isFriend) friend?.alias else player.displayName.formattedText.trim())
        )

        hasColourCode = true

        if (entityID.value) {
            builder.append("%sID: %s".format(if (hasColourCode) " " else "", player.entityId))
            hasColourCode = true
        }

        if (gameMode.value) {
            builder.append("%s%s".format(if (hasColourCode) " " else "", getGMText(player)))
            hasColourCode = true
        }

        if (ping.value) {
            builder.append("%s%sms".format(if (hasColourCode) " " else "", InfoUtils.ping(player)))
            hasColourCode = true
        }

        if (!health.value) return builder.toString() to isFriend

        val playerHealth = ceil(player.health + player.absorptionAmount)

        val colourCode = when {
            playerHealth > 18.0 -> "§a"
            playerHealth > 16.0 -> "§2"
            playerHealth > 12.0 -> "§e"
            playerHealth > 8.0 -> "§6"
            playerHealth > 5.0 -> "§c"
            else -> "§4"
        }

        if (health.value) {
            builder.append("%s%s%s%s".format(if (hasColourCode) " " else "", colourCode, if (playerHealth > 0.0) playerHealth.ceilToInt() else "0", "§r"))
            hasColourCode = true
        }

        if (totems.value) {
            builder.append("%s%s".format(if (hasColourCode) " " else "", totemPops))
        }

        return builder.toString() to isFriend
    }

    /*
    private void renderItem(final ItemStack stack, final int x, final int y, final int betterY) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        RenderHelper.enableStandardItemLighting();
        Globals.mc.getRenderItem().zLevel = -150.0f;
        GlStateManager.disableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.disableCull();
        final int posY = (betterY > 4) ? ((betterY - 4) * 8 / 2) : 0;
        Globals.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y + posY);
        Globals.mc.getRenderItem().renderItemOverlays(Globals.mc.fontRenderer, stack, x, y + posY);
        Globals.mc.getRenderItem().zLevel = 0.0f;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.disableDepth();
        renderEnchantText(stack, x, y - 24);
        GlStateManager.enableDepth();
        GlStateManager.scale(2.0F, 2.0F, 2.0F);
        GlStateManager.popMatrix();
    }
     */

    private fun renderItem(stack: ItemStack, enchantments: Array<String>, x: Int, y: Int, betterY: Int) {
        matrix(true)
        depthMask(true)
        GlStateManager.clear(256)
        RenderHelper.enableStandardItemLighting()
        Globals.mc.renderItem.zLevel = -150.0F

        alpha(false)
        depth(true)
        cull(false)

        GlStateManager.scale(1.0f, 1.0f, 0.01f)
        val addon = if (betterY > 4) (betterY - 4) * 8 / 2 else 0
        Globals.mc.renderItem.renderItemAndEffectIntoGUI(stack, x, y + addon)
        Globals.mc.renderItem.renderItemOverlays(Globals.mc.fontRenderer, stack, x, y + addon)
        Globals.mc.renderItem.zLevel = 0.0F
        GlStateManager.scale(1.0f, 1.0f, 1.0f)

        RenderHelper.disableStandardItemLighting()

        cull(true)
        alpha(true)
        blend(false)
        GlStateManager.scale(0.5, 0.5, 0.5)
        depth(false)
        renderEnchantText(stack, enchantments, x, y - 24)
        depth(true)
        GlStateManager.scale(2.0, 2.0, 2.0)

        matrix(false)
    }

    private fun renderEnchantText(stack: ItemStack, enchantments: Array<String>, x: Int, posY: Int) {
        var y = posY

        if (renderEnchants.value && enchantments.isNotEmpty()) {
            if (limitEnchants.value && enchantments.size > 2) {
                val placeholder = "muffin"
                if (customFont.value) {
                    prepareCustomFont { fontManager.drawStringWithShadow(placeholder, x * 2F, y.toFloat(), -1) }
                } else {
                    Globals.mc.fontRenderer.drawStringWithShadow(placeholder, x * 2F, y.toFloat(), -1)
                }
                y += 8
            } else {
                for (placeholder in enchantments) {
                    if (customFont.value) {
                        prepareCustomFont { fontManager.drawStringWithShadow(placeholder, x * 2F, y.toFloat(), -1) }
                    } else {
                        Globals.mc.fontRenderer.drawStringWithShadow(placeholder, x * 2F, y.toFloat(), -1)
                    }
                    y += 8
                }
            }
        }

        if (stack.item == Items.GOLDEN_APPLE && stack.hasEffect()) {
            if (customFont.value) {
                prepareCustomFont { fontManager.drawStringWithShadow("God", x * 2F, y.toFloat(), -3977919) }
                return
            }
            Globals.mc.fontRenderer.drawStringWithShadow("God", x * 2F, y.toFloat(), -3977919)
        }

    }

    private fun renderStackName(stack: ItemStack, x: Int, y: Int) {
        GlStateManager.scale(0.5f, 0.5f, 0.5f)
        GlStateManager.disableDepth()
        val displayName = stack.displayName

        if (customFont.value) {
            prepareCustomFont { fontManager.drawStringWithShadow(displayName, -fontManager.getStringWidth(displayName) / 2F, y.toFloat(), -1) }
        } else {
            Globals.mc.fontRenderer.drawStringWithShadow(displayName, -Globals.mc.fontRenderer.getStringWidth(displayName) / 2F, y.toFloat(), -1)
        }

        GlStateManager.enableDepth()
        GlStateManager.scale(2.0f, 2.0f, 2.0f)
    }

    private fun getNameColour(player: EntityPlayer, isFriend: Boolean): Int {
        if (isFriend) return if (player.isSneaking) 16733695 else 5636095 // TextFormatting.LIGHT_PURPLE else TextFormatting.AQUA
        if (player.isInvisible) return 16733525 // TextFormatting.RED
        return if (player.isSneaking) 11141290 else 16777215// TextFormatting.DARK_PURPLE else TextFormatting.WHITE
    }

    private fun getEnchantment(enchantment: Enchantment, level: Int): String {
        val resourceLocation = ENCHANTMENT_REGISTRY.getNameForObject(enchantment)
        var subString = resourceLocation?.toString() ?: enchantment.name

        val levels = if (level > 1) 12 else 13

        if (subString.length > levels) subString = subString.substring(10, levels)

        var editedEnchantments = StringBuilder().run {
            insert(0, subString.substring(0, 1).toUpperCase())
            append(subString.substring(1))
            toString()
        }

        if (level > 1) {
            editedEnchantments = StringBuilder().run {
                insert(0, editedEnchantments)
                append(level)
                toString()
            }
        }

        return editedEnchantments
    }

    private fun drawDurability(stack: ItemStack, x: Int, y: Int) {
        val duraPercentage = 100f - (stack.itemDamage.toFloat() / stack.maxDamage.toFloat()) * 100f
        val colour = healthColorGradient.get(duraPercentage).toHex()
        val dmg = duraPercentage.roundToInt().toString()
        GlStateManager.scale(0.5f, 0.5f, 0.5f)
        GlStateManager.disableDepth()

        val durability = StringBuilder().run {
            insert(0, dmg)
            append('%')
            toString()
        }

        if (customFont.value) {
            prepareCustomFont { fontManager.drawStringWithShadow(durability, x * 2F, y.toFloat(), colour) }
        } else {
            Globals.mc.fontRenderer.drawStringWithShadow(durability, x * 2F, y.toFloat(), colour)
        }

        GlStateManager.enableDepth()
        GlStateManager.scale(2.0f, 2.0f, 2.0f)
    }

    private fun getAverage(y: Int): Int {
        var finalY = -26
        if (y > 4) {
            finalY -= (y - 4) * 8
        }
        return finalY
    }

    data class EnchantmentWithName(val enchantment: Enchantment, val displayEnchantment: String)
    data class OffMainItemEnchantments(val item: ItemStack, val enchantmentsSize: Int, val enchantments: Array<String>)
    data class ArmourEnchantments(val armour: ItemStack)

    data class PlayerDataStore(
        val camera: Entity, val interpolatedCamera: Vec3d,
        val data: Pair<String, Boolean>, val scale: Float, val boxColour: Colour,
        val mainHandItem: OffMainItemEnchantments, val offhandItem: OffMainItemEnchantments,
        val armourItems: Array<Triple<ItemStack, Int, Array<String>>> //NonNullList<ItemStack>
        ) {

        fun renderNameTags(player: EntityPlayer, x: Double, y: Double, z: Double) {
            val originalPositionX = camera.posX
            val originalPositionY = camera.posY
            val originalPositionZ = camera.posZ

            camera.posX = interpolatedCamera.x
            camera.posY = interpolatedCamera.y
            camera.posZ = interpolatedCamera.z

            val name = data.first
            val isFriend = data.second

            val tempY = y + if (player.isSneaking) 0.5 else 0.7
            val thirdPerson = Globals.mc.gameSettings.thirdPersonView

            matrix(true)
            RenderHelper.enableStandardItemLighting()
            val combinedLight = Globals.mc.world.getCombinedLight(player.position, 0)
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combinedLight % 65536F, combinedLight / 65536F)

            polygon(true)
            GlStateManager.disableLighting()

            glTranslated(x, tempY + 1.4, z)
            glNormal3f(0.0f, 2.0f, 0.0f)
            glRotatef(-RenderUtils.playerViewY, 0.0f, 1.0F, 0.0f)
            glRotatef(RenderUtils.playerViewX, if (thirdPerson == 2) -1.0F else 1.0F, 0.0F, 0.0F)
            glScalef(-scale, -scale, scale)

            depth(false)
            depthMask(false)
            GlStateUtils.texture2d(false)
            blend(true)

            AWTFontRenderer.assumeNonVolatile = true

            var width = Globals.mc.fontRenderer.getStringWidth(name) / 2
            var height = Globals.mc.fontRenderer.FONT_HEIGHT

            if (customFont.value) {
                width = fontManager.getStringWidth(name) / 2
                height = fontManager.stringHeight
            }

            when (drawMode.value) {
                DrawMode.Rect -> RenderUtils.quickDrawRect(-width - 4F, -height - 1.5f, width + 2F, 2.0f, ColourUtils.toRGBA(24, 24, 24, 80))
                DrawMode.BorderRect -> RenderUtils.quickDrawBorderedRect(-width - 4F, -height - 1.5f, width + 2F, 2.0f, 1.3f, ColourUtils.toRGBA(24, 24, 24, 80), boxColour.toHex())
                DrawMode.Rounded ->  RenderUtils.quickDrawRoundedRect(-width - 4F, -height - 1.5f, width + 2F, 2.0f, boxColour.toHex(), ColourUtils.toRGBA(24, 24, 24, 80))
            }

            GlStateUtils.texture2d(true)

            val colour = getNameColour(player, isFriend)

            if (customFont.value) {
                fontManager.drawStringWithShadow(name, -width.toFloat(), -(height - 1).toFloat(), colour)
            } else {
                Globals.mc.fontRenderer.drawStringWithShadow(name, -width.toFloat(), -(height - 1).toFloat(), colour)
            }

            var xOffset = 0
            var betterPosY = 0
            var canRenderDurability = false

            val mainHand = mainHandItem.item
            val offHand = offhandItem.item

            matrix(true)

            for (playerArmour in armourItems) {
                val stack = playerArmour.first
                if (!stack.isEmpty) {
                    val isRenderDurabilityOn = renderDurability.value

                    xOffset -= 8
                    if (isRenderDurabilityOn) canRenderDurability = true

                    if (armour.value) {
                        val size = playerArmour.second
                        if (size > betterPosY) {
                            betterPosY = size
                        }
                    }
                }
            }

            if (!offHand.isEmpty && (armour.value || renderDurability.value && offHand.isItemStackDamageable)) {
                xOffset -= 8

                if (renderDurability.value && offHand.isItemStackDamageable) {
                    canRenderDurability = true
                }
                if (armour.value) {
                    val size = offhandItem.enchantmentsSize
                    if (size > betterPosY) {
                        betterPosY = size
                    }
                }
            }

            if (!mainHand.isEmpty) {
                if (armour.value) {
                    val size = mainHandItem.enchantmentsSize
                    if (size > betterPosY) {
                        betterPosY = size
                    }
                }

                var addY = getAverage(betterPosY)
                if (armour.value || renderDurability.value && mainHand.isItemStackDamageable) {
                    xOffset -= 8
                }

                if (armour.value) {
                    renderItem(mainHand, mainHandItem.enchantments, xOffset - 2, addY, betterPosY)
                    addY -= 32
                }

                if (renderDurability.value && mainHand.isItemStackDamageable) {
                    drawDurability(mainHand, xOffset - 2, addY)
                    addY -= height
                } else {
                    if (canRenderDurability) {
                        addY -= height
                    }
                }

                renderStackName(mainHand, xOffset, addY - 1)
                if (armour.value || renderDurability.value && mainHand.isItemStackDamageable) {
                    xOffset += 16
                }
            }

            for (playerArmour in armourItems) {
                val stack = playerArmour.first
                if (!stack.isEmpty) {
                    var addY = getAverage(betterPosY)
                    if (armour.value) {
                        renderItem(stack, playerArmour.third, xOffset, addY, betterPosY)
                        addY -= 32
                    }
                    if (renderDurability.value && stack.isItemStackDamageable) {
                        drawDurability(stack, xOffset, addY)
                    }
                    xOffset += 16
                }
            }

            if (!offHand.isEmpty) {
                var addY = getAverage(betterPosY)
                if (armour.value) {
                    renderItem(offHand, offhandItem.enchantments, xOffset, addY, betterPosY)
                    addY -= 32
                }
                if (renderDurability.value && offHand.isItemStackDamageable) {
                    drawDurability(offHand, xOffset, addY)
                }
                xOffset += 16
            }

            matrix(false)

            AWTFontRenderer.assumeNonVolatile = false

            camera.posX = originalPositionX
            camera.posY = originalPositionY
            camera.posZ = originalPositionZ

            GlStateUtils.texture2d(true)
            depthMask(true)
            depth(true)
            blend(false)
            polygon(false)
            resetColour()
            matrix(false)
        }

    }

    @BufferDSL
    private inline fun prepareCustomFont(elements: () -> Unit) {
        blend(true)
        elements()
        blend(false)
    }

}
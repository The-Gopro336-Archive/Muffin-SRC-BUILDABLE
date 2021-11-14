package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.network.PacketEvent
import me.han.muffin.client.event.events.render.RenderArmorLayerEvent
import me.han.muffin.client.event.events.render.RenderSkyEvent
import me.han.muffin.client.event.events.render.RenderTotemAnimationEvent
import me.han.muffin.client.event.events.render.SetupFogEvent
import me.han.muffin.client.event.events.render.entity.HurtCamEvent
import me.han.muffin.client.event.events.render.entity.RenderEntityEvent
import me.han.muffin.client.event.events.render.overlay.RenderGuiBossOverlayEvent
import me.han.muffin.client.event.events.world.RainStrengthEvent
import me.han.muffin.client.event.events.world.RenderSkyLightEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import net.minecraft.block.BlockSnow
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleFirework
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.IAnimals
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.*
import net.minecraft.tileentity.*
import net.minecraftforge.registries.GameData
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object NoRenderModule: Module("NoRender", Category.RENDER, true, "Don't render certain things.") {
    private val page = EnumValue(Pages.Entities, "Page")
    private val cancelPackets = Value(true, "CancelPackets")

    private val items = EnumValue({ page.value == Pages.Entities }, NoItemsMode.Off, "NoItems")
    private val paint = Value({ page.value == Pages.Entities }, false, "Paintings")
    private val animals = Value({ page.value == Pages.Entities }, false, "Animals")
    private val mobs = Value({ page.value == Pages.Entities }, false, "Mobs")
    private val player = Value({ page.value == Pages.Entities }, false, "Players")
    private val sign = Value({ page.value == Pages.Entities }, false, "Signs")
    private val skull = Value({ page.value == Pages.Entities }, false, "Heads")
    private val armorStand = Value({ page.value == Pages.Entities }, false, "ArmorStands")
    private val endPortal = Value({ page.value == Pages.Entities }, false, "EndPortals")
    private val banner = Value({ page.value == Pages.Entities }, false, "Banners")
    private val itemFrame = Value({ page.value == Pages.Entities }, false, "ItemFrames")
    private val xp = Value({ page.value == Pages.Entities }, false, "XP")
    private val crystal = Value({ page.value == Pages.Entities }, false, "Crystals")
    private val firework = Value({ page.value == Pages.Entities }, false, "Firework")

    private val explosions = Value({ page.value == Pages.Others }, true, "Explosions")
    private val falling = Value({ page.value == Pages.Others }, true,"FallingBlocks")
    private val enchantingTable = Value({ page.value == Pages.Others }, true,"EnchantingBooks")
    private val enchantingTableSnow = Value({ page.value == Pages.Others }, false,"EnchantTableSnow")

    private val projectiles = Value({ page.value == Pages.Others }, false,"Projectiles")
    private val lightning = Value({ page.value == Pages.Others }, true,"Lightning")

    val signText = Value({ page.value == Pages.Others }, false, "SignText")
    private val particles = Value({ page.value == Pages.Others }, true, "Particles")
    private val weather = Value({ page.value == Pages.Others }, true, "Weather")

    private val wither = Value({ page.value == Pages.Others }, true, "Wither")
    private val armor = Value({ page.value == Pages.Others }, false, "Armor")
    private val fog = Value({ page.value == Pages.Others }, false, "NoFog")
    private val hurtCam = Value({ page.value == Pages.Others }, true, "HurtCam")
    private val totem = Value({ page.value == Pages.Others }, false, "Totem")
    private val sky = Value({ page.value == Pages.Others }, false, "Sky")
    private val skyLight = Value({ page.value == Pages.Others }, true, "SkyLight")

    init {
        addSettings(
            page, cancelPackets,
            items, paint, animals, mobs, player, sign, skull, armorStand, endPortal, banner, itemFrame, xp, crystal,
            explosions, falling, enchantingTable, enchantingTableSnow, projectiles, lightning, signText, particles, weather,
            wither, armor, fog, hurtCam, totem, sky, skyLight
        )

        settings.forEach {
            it.listeners = object: ValueListeners {
                override fun onValueChange(value: Value<*>?) {
                    if (isDisabled) return
                    updateList()
                }
            }
        }

    }

    private enum class NoItemsMode {
        Off, Hide, Remove
    }

    private enum class Pages {
        Entities, Others
    }

    private val settingMap = mapOf(
        player.value to EntityOtherPlayerMP::class.java,
        paint.value to EntityPainting::class.java,
        sign.value to TileEntitySign::class.java,
        skull.value to TileEntitySkull::class.java,
        armorStand.value to EntityArmorStand::class.java,
        endPortal.value to TileEntityEndPortal::class.java,
        banner.value to TileEntityBanner::class.java,
        itemFrame.value to EntityItemFrame::class.java,
        xp.value to EntityXPOrb::class.java,
        (items.value != NoItemsMode.Off) to EntityItem::class.java,
        crystal.value to EntityEnderCrystal::class.java,
        firework.value to EntityFireworkRocket::class.java,
        falling.value to EntityFallingBlock::class.java,
        enchantingTable.value to TileEntityEnchantmentTable::class.java,
        lightning.value to EntityLightningBolt::class.java
    )

    var entityList = HashSet<Class<out Any>>(); private set

    @Listener
    private fun onRenderEntity(event: RenderEntityEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (items.value == NoItemsMode.Hide && event.entity is EntityItem) event.cancel()

        if (entityList.contains(event.entity.javaClass)
            || animals.value && event.entity is IAnimals && event.entity !is EntityMob
            || mobs.value && event.entity is EntityMob) {
            event.cancel()
        }

    }

    @Listener
    private fun onPlayerUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return
        if (items.value == NoItemsMode.Remove) {
            for (entity in Globals.mc.world.loadedEntityList) {
                if (entity == null || entity !is EntityItem) continue
                entity.setDead()
                Globals.mc.world.removeEntityFromWorld(entity.entityId)
            }
        }
    }

    @Listener
    private fun onRenderSkyLight(event: RenderSkyLightEvent) {
        if (skyLight.value) event.cancel()
    }

    @Listener
    private fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (explosions.value && event.packet is SPacketExplosion ||
            particles.value && event.packet is SPacketParticles ||
            cancelPackets.value
            && (lightning.value && event.packet is SPacketSpawnGlobalEntity ||
                xp.value && event.packet is SPacketSpawnExperienceOrb ||
                paint.value && event.packet is SPacketSpawnPainting)
        ) {
            event.cancel()
            return
        }

        when (event.packet) {
            is SPacketSpawnObject -> {
                event.isCanceled = cancelPackets.value &&
                    when (event.packet.type) {
                        71 -> itemFrame.value
                        78 -> armorStand.value
                        51 -> crystal.value
                        2 -> items.value != NoItemsMode.Off
                        70 -> falling.value
                        76 -> firework.value
                        else -> projectiles.value
                    }
            }
            is SPacketSpawnMob -> {
                if (cancelPackets.value) {
                    val entityClass = GameData.getEntityRegistry().getValue(event.packet.entityType).entityClass
                    if (EntityMob::class.java.isAssignableFrom(entityClass)) {
                        if (mobs.value) event.cancel()
                    } else if (IAnimals::class.java.isAssignableFrom(entityClass)) {
                        if (animals.value) event.cancel()
                    }
                }
            }
        }

    }

    @Listener
    private fun onRenderTotemAnimation(event: RenderTotemAnimationEvent) {
        if (totem.value) event.cancel()
    }

    @Listener
    private fun onRenderSky(event: RenderSkyEvent) {
        if (sky.value) event.cancel()
    }

    @Listener
    private fun onGuiBossOverlay(event: RenderGuiBossOverlayEvent) {
        if (wither.value) event.cancel()
    }

    @Listener
    private fun onRenderArmorLayer(event: RenderArmorLayerEvent) {
        if (armor.value) event.cancel()
    }

    @Listener
    private fun onHurtCam(event: HurtCamEvent) {
        if (hurtCam.value) event.cancel()
    }

    @Listener
    private fun onRainStrength(event: RainStrengthEvent) {
        if (weather.value) event.cancel()
    }

    @Listener
    private fun onSetupFog(event: SetupFogEvent) {
        if (fog.value && Globals.mc.player != null && Globals.mc.player.ticksExisted > 20) {
            event.cancel()
            Globals.mc.entityRenderer.setupFogColor(false)
            GlStateManager.glNormal3f(0.0f, -1.0f, 0.0f)
            RenderUtils.glColor(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateManager.colorMaterial(1028, 4608)
        }
    }

    fun handleParticle(particle: Particle) = particles.value
        || firework.value && (particle is ParticleFirework.Overlay || particle is ParticleFirework.Spark || particle is ParticleFirework.Starter)

    fun tryReplaceEnchantingTable(tileEntity: TileEntity) : Boolean {
        if (!enchantingTableSnow.value || tileEntity !is TileEntityEnchantmentTable) return false
        val blockState = Blocks.SNOW_LAYER.defaultState.withProperty(BlockSnow.LAYERS, 7)
        Globals.mc.world.setBlockState(tileEntity.pos, blockState)
        Globals.mc.world.markTileEntityForRemoval(tileEntity)
        return true
    }

    private fun updateList() {
        entityList = HashSet<Class<out Any>>().apply {
            settingMap.forEach { if (it.key) add(it.value) }
            // needed because there are 2 entities, the gateway and the portal
            if (endPortal.value) add(TileEntityEndGateway::class.java)
        }
    }


}
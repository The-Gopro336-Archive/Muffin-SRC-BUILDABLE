package me.han.muffin.client.module.modules.render

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.TickEvent
import me.han.muffin.client.event.events.render.ComputeVisibilityEvent
import me.han.muffin.client.event.events.render.RenderPutColorMultiplierEvent
import me.han.muffin.client.event.events.render.ShouldSetupTerrainEvent
import me.han.muffin.client.event.events.world.SetOpaqueCubeEvent
import me.han.muffin.client.event.events.world.block.CanRenderInLayerEvent
import me.han.muffin.client.module.Module
import me.han.muffin.client.utils.extensions.mc.block.block
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import me.han.muffin.client.value.ValueListeners
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeModContainer
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener

internal object WallHackModule: Module("WallHack", Category.RENDER, "Makes blocks transparent to see ores or other blocks.") {

    private val mode = EnumValue(Mode.Circuits, "Mode")
    private val opacity = NumberValue(120F, 0f, 255f, 5f, "Opacity")
    private val softReload = Value(true, "SoftReload")
    private val bypass = Value(false, "Bypass")
    
    private val normalBlock = getNormalBlocks()
    private val circuitBlock = getCircuitBlocks()

    private fun getNormalBlocks(): ArrayList<Block> {
        return arrayListOf<Block>().apply {
            add(Blocks.EMERALD_ORE)
            add(Blocks.GOLD_ORE)
            add(Blocks.IRON_ORE)
            add(Blocks.COAL_ORE)
            add(Blocks.LAPIS_ORE)
            add(Blocks.DIAMOND_ORE)
            add(Blocks.REDSTONE_ORE)
            add(Blocks.LIT_REDSTONE_ORE)
            add(Blocks.TNT)
            add(Blocks.EMERALD_ORE)
            add(Blocks.FURNACE)
            add(Blocks.LIT_FURNACE)
            add(Blocks.DIAMOND_BLOCK)
            add(Blocks.IRON_BLOCK)
            add(Blocks.GOLD_BLOCK)
            add(Blocks.EMERALD_BLOCK)
            add(Blocks.QUARTZ_ORE)
            add(Blocks.BEACON)
            add(Blocks.MOB_SPAWNER)
        }
    }

    private fun getCircuitBlocks(): ArrayList<Block> {
        return arrayListOf<Block>().apply {
            add(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
            add(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)
            add(Blocks.STONE_PRESSURE_PLATE)
            add(Blocks.WOODEN_PRESSURE_PLATE)
            add(Blocks.STONE_BUTTON)
            add(Blocks.WOODEN_BUTTON)
            add(Blocks.LEVER)
            add(Blocks.COMMAND_BLOCK)
            add(Blocks.CHAIN_COMMAND_BLOCK)
            add(Blocks.REPEATING_COMMAND_BLOCK)
            add(Blocks.DAYLIGHT_DETECTOR)
            add(Blocks.DAYLIGHT_DETECTOR_INVERTED)
            add(Blocks.DISPENSER)
            add(Blocks.DROPPER)
            add(Blocks.HOPPER)
            add(Blocks.OBSERVER)
            add(Blocks.TRAPDOOR)
            add(Blocks.IRON_TRAPDOOR)
            add(Blocks.REDSTONE_BLOCK)
            add(Blocks.REDSTONE_LAMP)
            add(Blocks.REDSTONE_TORCH)
            add(Blocks.UNLIT_REDSTONE_TORCH)
            add(Blocks.REDSTONE_WIRE)
            add(Blocks.POWERED_REPEATER)
            add(Blocks.UNPOWERED_REPEATER)
            add(Blocks.POWERED_COMPARATOR)
            add(Blocks.UNPOWERED_COMPARATOR)
            add(Blocks.LIT_REDSTONE_LAMP)
            add(Blocks.REDSTONE_ORE)
            add(Blocks.LIT_REDSTONE_ORE)
            add(Blocks.ACACIA_DOOR)
            add(Blocks.DARK_OAK_DOOR)
            add(Blocks.BIRCH_DOOR)
            add(Blocks.JUNGLE_DOOR)
            add(Blocks.OAK_DOOR)
            add(Blocks.SPRUCE_DOOR)
            add(Blocks.DARK_OAK_DOOR)
            add(Blocks.IRON_DOOR)
            add(Blocks.OAK_FENCE)
            add(Blocks.SPRUCE_FENCE)
            add(Blocks.BIRCH_FENCE)
            add(Blocks.JUNGLE_FENCE)
            add(Blocks.DARK_OAK_FENCE)
            add(Blocks.ACACIA_FENCE)
            add(Blocks.OAK_FENCE_GATE)
            add(Blocks.SPRUCE_FENCE_GATE)
            add(Blocks.BIRCH_FENCE_GATE)
            add(Blocks.JUNGLE_FENCE_GATE)
            add(Blocks.DARK_OAK_FENCE_GATE)
            add(Blocks.ACACIA_FENCE_GATE)
            add(Blocks.JUKEBOX)
            add(Blocks.NOTEBLOCK)
            add(Blocks.PISTON)
            add(Blocks.PISTON_EXTENSION)
            add(Blocks.PISTON_HEAD)
            add(Blocks.STICKY_PISTON)
            add(Blocks.TNT)
            add(Blocks.SLIME_BLOCK)
            add(Blocks.TRIPWIRE)
            add(Blocks.TRIPWIRE_HOOK)
            add(Blocks.RAIL)
            add(Blocks.ACTIVATOR_RAIL)
            add(Blocks.DETECTOR_RAIL)
            add(Blocks.GOLDEN_RAIL)
        }
    }

    private var previousForgeLightPipelineEnabled = false
    private var oldAmbience = 0
    private var cachedOpacity = 0f

    private var previousTest = false

    init {
        addSettings(mode, opacity, softReload, bypass)
        opacity.listeners = object : ValueListeners {
            override fun onValueChange(value: Value<*>?) {
                if (isDisabled) return
                reloadWorld()
            }
        }
    }

    private enum class Mode {
        Normal, Circuits
    }

    @Listener
    private fun onTicking(event: TickEvent) {
        if (event.stage != EventStageable.EventStage.PRE || fullNullCheck()) return
        if (cachedOpacity != opacity.value) {
            cachedOpacity = opacity.value
            reloadWorld()
        }
    }

    private fun doBypass() {
        val i = 16
        for (posX in -i until i) {
            for (posY in i downTo -i + 1) {
                for (posZ in -i until i) {
                    val z = Globals.mc.player.posX.toInt() + posX
                    val y = Globals.mc.player.posY.toInt() + posY
                    val x = Globals.mc.player.posZ.toInt() + posZ
                    val blockPos = BlockPos(x, y, z)

                    val block = blockPos.block
                    if (block == Blocks.DIAMOND_ORE) Globals.mc.playerController.clickBlock(blockPos, EnumFacing.DOWN)
                }
            }
        }
    }


    override fun onEnable() {
        if (bypass.value) doBypass()

        previousForgeLightPipelineEnabled = ForgeModContainer.forgeLightPipelineEnabled
        oldAmbience = Globals.mc.gameSettings.ambientOcclusion

        try {
            val class_ = Class.forName("net.minecraftforge.common.ForgeModContainer", true, this.javaClass.classLoader)
            val field2 = class_.getDeclaredField("forgeLightPipelineEnabled")
            val isAccessible = field2.isAccessible
            field2.isAccessible = true
            previousTest = field2.getBoolean(null)
            field2[null] = false
            field2.isAccessible = isAccessible
        } catch (ignored: Exception) {
        }

        cachedOpacity = opacity.value
        ForgeModContainer.forgeLightPipelineEnabled = false

        Globals.mc.renderChunksMany = false
        Globals.mc.gameSettings.gammaSetting = 11F
        Globals.mc.gameSettings.ambientOcclusion = 0

        reloadWorld()
    }

    override fun onDisable() {

        try {
            val class_ = Class.forName("net.minecraftforge.common.ForgeModContainer", true, this.javaClass.classLoader)
            val field = (class_).getDeclaredField("forgeLightPipelineEnabled")
            val isAccessible = field.isAccessible
            field.isAccessible = true
            field[null] = previousTest
            field.isAccessible = isAccessible
        } catch (ignored: Exception) {
        }

        Globals.mc.renderChunksMany = true
        ForgeModContainer.forgeLightPipelineEnabled = true
        Globals.mc.gameSettings.ambientOcclusion = oldAmbience
        Globals.mc.gameSettings.gammaSetting = 1f

        reloadWorld()
    }

    @Listener
    private fun onShouldSetupTerrain(event: ShouldSetupTerrainEvent) {
        event.cancel()
    }

    @Listener
    private fun onComputeVisibility(event: ComputeVisibilityEvent) {
        event.cancel()
    }

    @Listener
    private fun onSetOpaqueBlock(event: SetOpaqueCubeEvent) {
        event.cancel()
    }


    private fun reloadWorld() {
        if (fullNullCheck() || Globals.mc.renderGlobal == null) return

        if (softReload.value) {
            Globals.mc.addScheduledTask {
                val x = Globals.mc.player.posX.toInt()
                val y = Globals.mc.player.posY.toInt()
                val z = Globals.mc.player.posZ.toInt()
                val distance = Globals.mc.gameSettings.renderDistanceChunks * 16
                Globals.mc.renderGlobal.markBlockRangeForRenderUpdate(x - distance, y - distance, z - distance, x + distance, y + distance, z + distance)
            }

        } else {
            Globals.mc.renderGlobal.loadRenderers()
        }
    }


    @Listener
    private fun onPutColourModifier(event: RenderPutColorMultiplierEvent) {
        event.opacity = opacity.value
        event.cancel()
    }

    @Listener
    private fun onCanRenderInLayer(event: CanRenderInLayerEvent) {
        if (!containsBlock(event.block)) event.blockRenderLayer = BlockRenderLayer.TRANSLUCENT
    }

    fun containsBlock(block: Block): Boolean {
        if (mode.value == Mode.Normal) return normalBlock.contains(block)
        return circuitBlock.contains(block)
    }

    fun processShouldSideBeRendered(block: Block, blockState: IBlockState, blockAccess: IBlockAccess, pos: BlockPos, side: EnumFacing, callback: CallbackInfoReturnable<Boolean>) {
        callback.returnValue = containsBlock(block)
    }

    fun processGetLightValue(block: Block, callback: CallbackInfoReturnable<Int>) {
        if (containsBlock(block)) callback.returnValue = 1
    }

    fun processGetAmbientOcclusionLightValue(block: Block, callback: CallbackInfoReturnable<Float>) {
        callback.returnValue = 1F
    }

    override fun getHudInfo(): String {
        return mode.fixedValue
    }

}
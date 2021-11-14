package me.han.muffin.client.module.modules.misc

import me.han.muffin.client.core.Globals
import me.han.muffin.client.event.EventStageable
import me.han.muffin.client.event.events.client.UpdateEvent
import me.han.muffin.client.event.events.world.ChunkEvent
import me.han.muffin.client.imixin.gui.IGuiScreenHorseInventory
import me.han.muffin.client.manager.managers.ChatManager
import me.han.muffin.client.manager.managers.TimerManager
import me.han.muffin.client.module.Module
import me.han.muffin.client.module.modules.exploits.EntityDesyncModule
import me.han.muffin.client.module.modules.hidden.FreecamDupeModule
import me.han.muffin.client.preset.Preset
import me.han.muffin.client.utils.entity.EntityUtil.flooredPosition
import me.han.muffin.client.utils.entity.MovementUtils
import me.han.muffin.client.utils.extensions.mixin.netty.doneLoadingTerrain
import me.han.muffin.client.utils.math.MathUtils
import me.han.muffin.client.utils.math.VectorUtils.toBlockPos
import me.han.muffin.client.utils.math.rotation.Vec2f
import me.han.muffin.client.utils.timer.Timer
import me.han.muffin.client.value.EnumValue
import me.han.muffin.client.value.NumberValue
import me.han.muffin.client.value.Value
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.EntityDonkey
import net.minecraft.entity.passive.EntityLlama
import net.minecraft.entity.passive.EntityMule
import net.minecraft.inventory.ClickType
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import net.minecraft.world.chunk.Chunk
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * @author han
 */
object AutoDupeModule: Module("AutoDupe", Category.MISC, "Automatically dupe items for you.") {
    private val waitForChunk = Value(false, "WaitForChunk")
    private val useTimer = Value(true, "UseTimer")
    private val timerSpeed = NumberValue(10.0F, 0.1F, 50.0F, 0.1F, "TimerSpeed")

    private val travelType = EnumValue(TravelType.Walk, "TravelType")
    private val horizontalSpeed = NumberValue({ travelType.value == TravelType.Walk },1.5, 0.5, 10.0, 0.1, "HorizontalSpeed")
    private val walkDistance = NumberValue(150, 60, 350, 5, "Distance")

    private var startPosition = Vec3d.ZERO

    private var canStart = false
    private var currentPhase = CurrentPhase.EntityDesync

    private var isEntityDesyncOn = false
    private var isFreecamOn = false

    private val loadedChunk = HashMap<Vec3d, Chunk>()
    private var startRotation = Vec2f(0.0F, 0.0F)

    private var currentTimerSpeed = 0.0F

    private var hasOpenedDonkeyInventory = false
    private var totalDuped = 0

    private var hasNearestDonkey = false

    private val eDesyncFreecamTimer = Timer()
    private val freecamWalkTimer = Timer()

    init {
        addSettings(waitForChunk, useTimer, timerSpeed, travelType, horizontalSpeed, walkDistance)

        offsetPresets(object : Preset("Default") {
            override fun onSet() {
                horizontalSpeed.value = 1.5
            }
        })
    }

    private fun isValidRidingEntity(entity: Entity): Boolean {
        return entity is EntityDonkey && !entity.isChild || entity is EntityLlama && !entity.isChild || entity is EntityMule && !entity.isChild
    }

    override fun onEnable() {
        if (fullNullCheck()) return

        if (Globals.mc.player.ridingEntity == null) {
            ChatManager.sendMessage("You must ride before starting the dupe.")
            disable()
            return
        }

        if (!isValidRidingEntity(Globals.mc.player.ridingEntity!!)) {
            ChatManager.sendMessage("You should ride donkey/llama.")
            disable()
            return
        }


        freecamWalkTimer.reset()

        startPosition = Globals.mc.player.positionVector
        startRotation = Vec2f(Globals.mc.player)
        canStart = true

        currentPhase = CurrentPhase.EntityDesync
    }

    override fun onDisable() {
        if (FreecamDupeModule.isEnabled) FreecamDupeModule.disable()
        if (EntityDesyncModule.isEnabled) EntityDesyncModule.disable()

        TimerManager.resetTimer()

        eDesyncFreecamTimer.reset()
        freecamWalkTimer.reset()

        canStart = false

        hasOpenedDonkeyInventory = false
        isEntityDesyncOn = false
        isFreecamOn = false

        hasNearestDonkey = false

        currentTimerSpeed = 0.0F
        totalDuped = 0

        currentPhase = CurrentPhase.EntityDesync
        startRotation = Vec2f(0.0F, 0.0F)
    }

    @Listener
    private fun onMotionUpdate(event: UpdateEvent) {
        if (event.stage != EventStageable.EventStage.PRE) return

        if (Globals.mc.currentScreen != null && Globals.mc.currentScreen !is GuiScreenHorseInventory) {
            ChatManager.sendMessage("Don't open any other screen.")
            disable()
            return
        }

        if (!canStart || startPosition == Vec3d.ZERO) return

        when (currentPhase) {
            CurrentPhase.EntityDesync -> {
                if (!isEntityDesyncOn && EntityDesyncModule.isDisabled) {
                    isEntityDesyncOn = true
                    EntityDesyncModule.enable()
                }
                freecamWalkTimer.reset()
                if (EntityDesyncModule.isEnabled && isEntityDesyncOn) currentPhase = CurrentPhase.Freecam
            }
            CurrentPhase.Freecam -> {
                if (freecamWalkTimer.passed(5000.0)) {
                    if (!isFreecamOn && FreecamDupeModule.isDisabled) {
                        isFreecamOn = true
                        FreecamDupeModule.enable()
                    }
                    if (FreecamDupeModule.isEnabled && isFreecamOn) currentPhase = CurrentPhase.WalkUntilEnd
                }
            }
            CurrentPhase.WalkUntilEnd -> {
                freecamWalkTimer.reset()

                if (EntityDesyncModule.isDisabled || FreecamDupeModule.isDisabled) {
                    ChatManager.sendMessage("Don't fucking turn off entitydesync and freecam.")
                    disable()
                    return
                }

                // KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, false)
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindBack.keyCode, false)
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindLeft.keyCode, false)
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindRight.keyCode, false)

                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindJump.keyCode, false)
                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindSneak.keyCode, false)

                val currentPositionVector = Globals.mc.player.positionVector
                val currentWalkDistanceX = abs(currentPositionVector.x - startPosition.x)
                val currentWalkDistanceZ = abs(currentPositionVector.z - startPosition.z)
                val hypotDist = hypot(currentWalkDistanceX, currentWalkDistanceZ)

                ChatManager.sendMessage("X: $currentWalkDistanceX || Z: $currentWalkDistanceZ || Dist: $hypotDist")

                if (!isCurrentChunkLoaded()) {
                    if (hypotDist <= walkDistance.value) {
                        KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, false)
                        TimerManager.setTimer(timerSpeed.value)
                        return
                    }
                }

                when (travelType.value) {
                    TravelType.Walk -> {
                        Globals.mc.player.rotationYaw = startRotation.x
                        if (!waitForChunk.value || isCurrentChunkLoaded()) {
                            val mouseOver = Globals.mc.objectMouseOver
                            TimerManager.resetTimer()
                            if (hypotDist <= walkDistance.value) {
                                ChatManager.sendMessage("Walking")
                                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, true)
                                //val yawRad = MovementUtils.calcMoveYaw(startRotation.x)
                                //Globals.mc.player.ridingEntity?.motionX = -sin(yawRad) * horizontalSpeed.value
                                //Globals.mc.player.ridingEntity?.motionZ = cos(yawRad) * horizontalSpeed.value
                                return
                            }
                        }
                    }
                    TravelType.HClip -> {
                        val direction = MathUtils.direction(Globals.mc.player.rotationYaw)
                        val entity = (if (Globals.mc.player.ridingEntity != null) Globals.mc.player.ridingEntity else Globals.mc.player) ?: return
                        entity.setPosition(entity.posX + (1.0F * walkDistance.value * direction.x + 0.0F * walkDistance.value * direction.y), entity.posY, entity.posZ + (1.0F * walkDistance.value * direction.y - 0.0F * walkDistance.value * direction.x))
                    }
                }

                KeyBinding.setKeyBindState(Globals.mc.gameSettings.keyBindForward.keyCode, false)
                if (hypotDist >= walkDistance.value) {
                    currentPhase = if (useTimer.value) CurrentPhase.UseTimer else CurrentPhase.WaitForChunk
                }
            }
            CurrentPhase.UseTimer -> {
                if (!useTimer.value) {
                    currentTimerSpeed = 0.0F
                    currentPhase = CurrentPhase.WaitForChunk
                    return
                }
                currentTimerSpeed = timerSpeed.value
                currentPhase = CurrentPhase.WaitForChunk
            }
            CurrentPhase.WaitForChunk -> {
                if (currentTimerSpeed > 0.0F) {
                    TimerManager.setTimer(currentTimerSpeed)
                }
                ChatManager.sendMessage("Waiting for chunk")
                if (isCurrentChunkLoaded()) {
                    currentPhase = CurrentPhase.ToggleOffEDesync
                }
            }
            CurrentPhase.ToggleOffEDesync -> {
                if (!isEntityDesyncOn || EntityDesyncModule.isDisabled) {
                    ChatManager.sendMessage("Should not turn off entity desync bruh.")
                    disable()
                    return
                }

                currentTimerSpeed = 0.0F
                ChatManager.sendMessage("Turned off EntityDesync")
                isEntityDesyncOn = false
                EntityDesyncModule.disable()
                if (EntityDesyncModule.isDisabled) currentPhase = CurrentPhase.TakeItemsOff
            }
            CurrentPhase.TakeItemsOff -> {
                Globals.mc.player.sendHorseInventory()
                if (Globals.mc.currentScreen is GuiScreenHorseInventory) {
                    val horseInventory = Globals.mc.currentScreen as GuiScreenHorseInventory
                    for (i in 0 until (horseInventory as IGuiScreenHorseInventory).horseInventory.sizeInventory) {
                        val stack = horseInventory.horseInventory.getStackInSlot(i)
                        if (stack.isEmpty) continue
                        Globals.mc.playerController.windowClick(horseInventory.inventorySlots.windowId, i, 1, ClickType.THROW, Globals.mc.player)
                        totalDuped++
                    }
                    Globals.mc.player.closeScreen()
                    Globals.mc.displayGuiScreen(null)
                }
                ChatManager.sendMessage("Duped $totalDuped items.")
                totalDuped = 0
                currentPhase = CurrentPhase.MountToOtherDonkey
            }
            CurrentPhase.MountToOtherDonkey -> {
                val nearestEntity = getNearestRideableEntity()
                if (nearestEntity == null) {
                    ChatManager.sendMessage("There are no other donkey for riding.")
                    hasNearestDonkey = false
                    currentPhase = CurrentPhase.TurnOffFreecam
                    return
                }
                hasNearestDonkey = true
                Globals.mc.playerController.interactWithEntity(Globals.mc.player, nearestEntity, EnumHand.MAIN_HAND)
                currentPhase = CurrentPhase.TurnOffFreecam
            }
            CurrentPhase.TurnOffFreecam -> {
                if (!isFreecamOn || FreecamDupeModule.isDisabled) {
                    ChatManager.sendMessage("Should not turn off freecam bruh.")
                    disable()
                    return
                }

                isFreecamOn = false
                FreecamDupeModule.disable()
                if (FreecamDupeModule.isDisabled) currentPhase = CurrentPhase.ResetEverything
            }
            CurrentPhase.ResetEverything -> {
                ChatManager.sendMessage("Starting to dupe again.")
                resetAndRestart()
            }
        }


    }

    private fun resetAndRestart() {
        if (Globals.mc.player.ridingEntity == null) {
            ChatManager.sendMessage("You must ride before starting the dupe.")
            disable()
            return
        }

        if (!isValidRidingEntity(Globals.mc.player.ridingEntity!!)) {
            ChatManager.sendMessage("You should ride donkey/llama.")
            disable()
            return
        }

        val currentVector = Globals.mc.player.flooredPosition
        val flooredStart = startPosition.toBlockPos()

        if (currentVector.x != flooredStart.x || currentVector.y != flooredStart.y || currentVector.z != flooredStart.z) {
            val yawRad = MovementUtils.calcMoveYaw(startRotation.x)
            Globals.mc.player.ridingEntity?.motionX = -sin(yawRad) * 0.28
            Globals.mc.player.ridingEntity?.motionZ = cos(yawRad) * 0.28
            return
        }

        freecamWalkTimer.reset()
        canStart = true

        hasOpenedDonkeyInventory = false
        isEntityDesyncOn = false
        isFreecamOn = false

        hasNearestDonkey = false

        currentTimerSpeed = 0.0F
        totalDuped = 0

        currentPhase = CurrentPhase.EntityDesync
    }

    private fun getNearestRideableEntity(): Entity? {
        return Globals.mc.world.loadedEntityList.filter { isValidMountEntity(it) }.minByOrNull { Globals.mc.player.getDistance(it) }
    }

    private fun isValidMountEntity(entity: Entity): Boolean {
        if (entity == Globals.mc.player.ridingEntity) return false
        if (entity.getDistance(Globals.mc.player) > 3) return false
        if (isValidRidingEntity(entity)) return true
        return false
    }

    private fun isCurrentChunkLoaded(): Boolean {
        return Globals.mc.world.getChunk(Globals.mc.player.flooredPosition).isLoaded && Globals.mc.player.connection.doneLoadingTerrain
    }

    @Listener
    private fun onChunkLoad(event: ChunkEvent) {
        when (event.type) {
            ChunkEvent.ChunkType.LOAD -> {
                loadedChunk[Globals.mc.player.positionVector] = event.chunk
            }
        }
    }

    private enum class CurrentPhase {
        Setup, EntityDesync, Freecam, WalkUntilEnd, UseTimer, WaitForChunk, ToggleOffEDesync, TakeItemsOff, MountToOtherDonkey, TurnOffFreecam, ResetEverything
    }

    private enum class TravelType {
        HClip, Walk
    }

}
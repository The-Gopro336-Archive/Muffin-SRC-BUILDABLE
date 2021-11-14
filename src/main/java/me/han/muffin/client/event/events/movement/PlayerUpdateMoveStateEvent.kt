package me.han.muffin.client.event.events.movement

import net.minecraft.util.MovementInput

data class PlayerUpdateMoveStateEvent(val movementInput: MovementInput)
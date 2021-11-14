package me.han.muffin.client.event.events.client

import net.minecraft.util.math.AxisAlignedBB

data class LandStepEvent(val bb: AxisAlignedBB, var stepHeight: Float)
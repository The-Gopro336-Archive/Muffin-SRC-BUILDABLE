package me.han.muffin.client.event.events.entity

import net.minecraft.util.EnumActionResult
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

data class OnItemUsePassEvent(val cir: CallbackInfoReturnable<EnumActionResult>)
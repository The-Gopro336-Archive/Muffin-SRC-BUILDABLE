package me.han.muffin.client.mixin.mixins.entity.living.player.invoker;

import me.han.muffin.client.imixin.entity.IPlayerControllerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PlayerControllerMP.class)
public abstract class MixinPlayerControllerMPVoid implements IPlayerControllerMP {
    @Override
    @Invoker(value = "syncCurrentPlayItem")
    public abstract void syncCurrentPlayItemVoid();
}

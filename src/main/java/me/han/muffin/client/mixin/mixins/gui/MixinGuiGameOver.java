package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.network.ServerEvent;
import me.han.muffin.client.imixin.gui.IGuiGameOver;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(value = GuiGameOver.class, priority = 10000)
public abstract class MixinGuiGameOver extends GuiScreen implements IGuiGameOver {

    @Nonnull
    @Override
    @Accessor(value = "causeOfDeath")
    public abstract ITextComponent getCauseOfDeath();

    @Inject(method = "confirmClicked", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/client/Minecraft.loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V"))
    private void onPostLoadWorldGameOverAssign(CallbackInfo ci) {
        if (this.mc.getCurrentServerData() != null)
            Muffin.getInstance().getEventManager().dispatchEvent(new ServerEvent.Disconnect(EventStageable.EventStage.POST, false, this.mc.getCurrentServerData()));
    }

}

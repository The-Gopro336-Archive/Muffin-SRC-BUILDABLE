package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.gui.GuiScreenInputEvent;
import me.han.muffin.client.event.events.gui.RenderGuiBackgroundEvent;
import me.han.muffin.client.event.events.render.RenderTooltipEvent;
import me.han.muffin.client.gui.MuffinGuiScreen;
import me.han.muffin.client.module.modules.movement.NoSlowModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow
    public Minecraft mc;

    @Shadow protected FontRenderer fontRenderer;

    @Shadow protected List<GuiButton> buttonList;

    @Shadow public int width;

    @Shadow public int height;

    @Shadow public void updateScreen() {}

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true)
    public void onRenderToolTipPre(ItemStack stack, int x, int y, CallbackInfo ci) {
        RenderTooltipEvent event = new RenderTooltipEvent(stack, x, y);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (event.isCanceled()) ci.cancel();
    }


    @Inject(method = "handleInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleKeyboardInput()V"))
    private void onKeyboardInputGuiPre(CallbackInfo ci) {
        GuiScreenInputEvent.KeyboardInput event = new GuiScreenInputEvent.KeyboardInput(EventStageable.EventStage.PRE);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(
            method = "handleInput",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z",
            shift = At.Shift.BEFORE),
            remap = false
    )
    private void onKeyboardInputGuiPost(CallbackInfo ci) {
        GuiScreenInputEvent.KeyboardInput event = new GuiScreenInputEvent.KeyboardInput(EventStageable.EventStage.POST);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
    }

    @Inject(method = "doesGuiPauseGame", at = @At(value = "RETURN"), cancellable = true)
    private void onGuiPauseGame(CallbackInfoReturnable<Boolean> cir) {
        if (NoSlowModule.INSTANCE.isEnabled() && NoSlowModule.INSTANCE.getInventoryWalk().getValue())
            cir.setReturnValue(false);
    }

    @Inject(method = "drawScreen", at = @At(value = "RETURN"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiEditSign || mc.isIntegratedServerRunning() && mc.currentScreen instanceof GuiIngameMenu)
            return;

        MuffinGuiScreen.updateRotationContainer();
    }

    @Inject(method = "drawWorldBackground", at = @At(value = "HEAD"), cancellable = true)
    private void onDrawWorldBackgroundPre(int tint, CallbackInfo ci) {
        RenderGuiBackgroundEvent event = new RenderGuiBackgroundEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        if (Globals.mc.world != null && event.isCanceled()) {
            ci.cancel();
        }
    }


}
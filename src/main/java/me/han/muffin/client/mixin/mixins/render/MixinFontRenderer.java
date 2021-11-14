package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.render.TextEvent;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FontRenderer.class)
public abstract class MixinFontRenderer {

    @ModifyVariable(method = "renderString", at = @At("HEAD"))
    private String onRenderStringPre(String string) {
        if (string == null || Muffin.getInstance().getEventManager() == null) return string;

        TextEvent event = new TextEvent(string);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.getText();
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"))
    private String onGetStringWidthPre(String string) {
        if (string == null || Muffin.getInstance().getEventManager() == null) return string;

        TextEvent event = new TextEvent(string);
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.getText();
    }


}
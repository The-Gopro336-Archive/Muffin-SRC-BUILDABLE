package me.han.muffin.client.mixin.mixins.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.gui.RemoveChatBoxEvent;
import me.han.muffin.client.module.modules.misc.ChatTweaksModule;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow @Final private List<ChatLine> chatLines;

    @Inject(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0, remap = false), cancellable = true)
    public void onSetChatLineInvokeSize(ITextComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci) {
        ChatTweaksModule.handleSetChatLine(drawnChatLines, chatLines, chatComponent, chatLineId, updateCounter, displayOnly, ci);
    }

    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At(value = "HEAD"))
    private ITextComponent addTimestamp(ITextComponent componentIn) {
        if (ChatTweaksModule.isTimeStampEnabled()) {
            TextComponentString newComponent = new TextComponentString(ChatTweaksModule.getChatTimeStampsFormat());
            newComponent.appendSibling(componentIn);
            return newComponent;
        }
        return componentIn;
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"), index = 4)
    private int drawRectBackgroundClean(int colour) {
        RemoveChatBoxEvent event = new RemoveChatBoxEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.isCanceled() ? 0 : colour;
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"), index = 0)
    private String drawColouredName(String text) {
    //    if (ChatTweaksModule.INSTANCE.isEnabled()) {
            if (ChatTweaksModule.INSTANCE.isEnabled()) {
                if (ChatTweaksModule.INSTANCE.getSelfChatHighlight().getValue() && text.contains(Globals.mc.player.getName())) {
                    String formatted = ChatTweaksModule.INSTANCE.getFormattedChatName(Globals.mc.player.getName());
                    return text.replace(Globals.mc.player.getName(), formatted);
                }
            }
            /*
            if (Muffin.getInstance().getFriendManager().getFriends().stream().anyMatch(friend -> text.contains(friend.getName()))) {
                String formatted = ChatTweaksModule.INSTANCE.getFormattedChatName(Globals.mc.player.getName(), true);
                text.replace(Globals.mc.player.getName(), formatted);
            }
             */
    //        return text;
    //    }
        return text;
    }

/*
    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I")
    )
    private int drawStringWithShadowClean(FontRenderer fontRenderer, String text, float x, float y, int color) {
        if (mc.player != null || mc.world != null)
            if (!StringUtils.checkIsMandarin(text))
                return (int) Muffin.getInstance().getFontManager().drawStringWithShadow(text, x, y, color);
            else
                return fontRenderer.drawStringWithShadow(text, x, y, color);
        else
            return fontRenderer.drawStringWithShadow(text, x, y, color);
    }
 */

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"), cancellable = true, remap = false)
    private void onPrintingChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId, CallbackInfo ci) {
        ci.cancel();
    }

}


package me.han.muffin.client.mixin.mixins.gui;

import com.google.common.collect.Ordering;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.gui.TabOrderSortedCopyEvent;
import me.han.muffin.client.module.modules.render.BetterTabModule;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(value = GuiPlayerTabOverlay.class, priority = Integer.MAX_VALUE)
public abstract class MixinGuiPlayerTabOverlay {
    @Shadow protected abstract void drawPing(int p_175245_1_, int p_175245_2_, int p_175245_3_, NetworkPlayerInfo networkPlayerInfoIn);

    private List<NetworkPlayerInfo> preSubList = CollectionsKt.emptyList();
    private TabOrderSortedCopyEvent sortingEvent;

    @ModifyVariable(method = "renderPlayerlist", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public List<NetworkPlayerInfo> onRenderPlayerListStorePlayerListPre(List<NetworkPlayerInfo> list) {
        preSubList = list;
        return list;
    }

    @ModifyVariable(method = "renderPlayerlist", at = @At(value = "STORE", ordinal = 1), ordinal = 0)
    public List<NetworkPlayerInfo> onRenderPlayerListStorePlayerListPost(List<NetworkPlayerInfo> list) {
        return BetterTabModule.subList(preSubList, list);
    }

    @Redirect(method = "renderPlayerlist" , at = @At(value = "INVOKE", target = "com/google/common/collect/Ordering.sortedCopy(Ljava/lang/Iterable;)Ljava/util/List;", remap = false))
    private List<NetworkPlayerInfo> onRenderPlayerListInvokeSortedCopyPre(Ordering<NetworkPlayerInfo> ordering, Iterable<NetworkPlayerInfo> elements) {
        sortingEvent = new TabOrderSortedCopyEvent(ordering, elements);
        Muffin.getInstance().getEventManager().dispatchEvent(sortingEvent);

        return (sortingEvent != null && sortingEvent.getCustomOrdering() != null) ? sortingEvent.getCustomOrdering() : ordering.sortedCopy(elements);
//        if (sortingEvent != null && (ordering = sortingEvent.getOrdering()) != null) {
//            return ordering.sortedCopy(elements);
//        }
//        return StreamSupport.stream(elements.spliterator(), false).collect(Collectors.toList());
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawPing(IIILnet/minecraft/client/network/NetworkPlayerInfo;)V"))
    private void onRenderPlayerListInvokeDrawPingPre(GuiPlayerTabOverlay guiPlayerTabOverlay, int width, int x, int y, NetworkPlayerInfo networkPlayerInfoIn) {
        boolean draw = BetterTabModule.renderCustomPing(x, y, width, networkPlayerInfoIn);
        if (!draw) drawPing(width, x, y, networkPlayerInfoIn);
    }

    @Inject(method = "renderPlayerlist", at = @At(value = "RETURN"))
    private void onRenderPlayerListPost(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        sortingEvent = null;
    }

//    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0))
//    public int preventTabClamping(int listSize, int theNumber_80) {
//        return BetterTabModule.INSTANCE.getSlots().getValue() > 0 ? listSize : Math.min(listSize, theNumber_80);
//    }

    @ModifyConstant(method = "renderPlayerlist", constant = @Constant(intValue = 20, ordinal = 0))
    public int onModifyMaxRows(int old) {
        return BetterTabModule.getMaxRows(old);
    }

    @ModifyConstant(method = "renderPlayerlist", constant = @Constant(intValue = 9, ordinal = 0))
    public int onChangePlayerBoxWidthIncrease(int old) {
        return BetterTabModule.getInfoXAddon(old);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isIntegratedServerRunning()Z"))
    public boolean onAlwaysRenderPlayerIcons(Minecraft mc) {
        return true;
    }

//    @ModifyArg(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 1), index = 4)
//    private int onDrawInfoBoxColourPreColour2(int oldColour) {
//        return ColourUtils.toRGBAClient(255); //BetterTabModule.getInfoBackgroundColourRedirect(oldColour);
//    }
//
//    @ModifyArg(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 3), index = 4)
//    private int onDrawInfoBoxColourPreColour(int oldColour) {
//        return ColourUtils.toRGBAClient(255); //BetterTabModule.getInfoBackgroundColourRedirect(oldColour);
//    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 0))
    private void onRedirectHeaderRect(int left, int top, int right, int bottom, int color) {
        boolean drawRectResult = BetterTabModule.renderCustomBackground(left, top, right, bottom, color);
        if (!drawRectResult) Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 1))
    private void onRedirectMiddleRect(int left, int top, int right, int bottom, int color) {
        boolean drawRectResult = BetterTabModule.renderCustomBackground(left, top, right, bottom, color);
        if (!drawRectResult) Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 3))
    private void onRedirectFooterRect(int left, int top, int right, int bottom, int color) {
        boolean drawRectResult = BetterTabModule.renderCustomBackground(left, top, right, bottom, color);
        if (!drawRectResult) Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 2))
    private void onDrawInfoBoxColourPreClientRect(int left, int top, int right, int bottom, int color) {
        int colour = BetterTabModule.getInfoBackgroundColourRedirect(color);
        if (colour != -1) RenderUtils.drawRect(left, top, right, bottom, colour);
    }


    @Inject(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V", ordinal = 2), locals = LocalCapture.CAPTURE_FAILHARD)
    public void drawPlayerBoxBackgroundCustom(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn,
                                              CallbackInfo ci,
                                              NetHandlerPlayClient nethandlerplayclient, List<NetworkPlayerInfo> list,
                                              int i, int j, int l3, int i4, int j4, boolean flag, int l, int i1, int j1, int k1, int l1,
                                              List<String> list1, List<String> list2, int k4, int l4, int i5, int j2, int k2) {

        int infoColour = BetterTabModule.getInfoBackgroundColourCover(list, k4);

        if (infoColour != -1 && infoColour != -5) {
            RenderUtils.drawRect(j2, k2, j2 + i1, k2 + 8, infoColour);
        }

//        if (BetterTabModule.INSTANCE.isEnabled() && BetterTabModule.INSTANCE.getCustomColour().getValue()) {
//            int color = 553648126;
//            if (k4 < list.size()) {
//                String uuid = String.valueOf(list.get(k4).getGameProfile().getId());
//                if (BetterTabModule.INSTANCE.checkIsMuffinUser(uuid)) {
//                    color = ColourUtils.toRGBAClient(40) | 0x80000000;
//                }
//            }
//            RenderUtils.drawRect(j2, k2, j2 + i1, k2 + 8, color);
//        }
    }

    /*
    @ModifyConstant(
            method = "renderPlayerlist",
            constant = @Constant(
                    intValue = 553648127
            ))
    public int changePlayerBoxBackgroundColor(int old) {
        return 0;
    }


    @Redirect(
            method = "drawPing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"
            ))
    public void preventExtraPingTextureBind(TextureManager manager, ResourceLocation location) {
    }
     */

    @Inject(method = "getPlayerName", at = @At(value = "HEAD"), cancellable = true)
    public void onGettingPlayerNamePre(NetworkPlayerInfo playerInfo, CallbackInfoReturnable<String> cir) {
        String modifiedName = BetterTabModule.getColourPlayerName(playerInfo);
        if (modifiedName != null) {
            cir.cancel();
            cir.setReturnValue(modifiedName);
        }
    }

    @Inject(method = "drawPing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawTexturedModalRect(IIIIII)V"))
    private void drawIconIfPossible(int p_175245_1_, int p_175245_2_, int p_175245_3_, NetworkPlayerInfo info, CallbackInfo cir) {
        BetterTabModule.renderMuffinIcon(p_175245_2_ + p_175245_1_ - 11 - 11, p_175245_3_ - 2, info, true);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 0))
    private int onRedirectingFontRendererRenderTabStringDrawingHeader(FontRenderer fontRenderer, String text, float x, float y, int color) {
        return BetterTabModule.getCustomFontRendererInstead(fontRenderer).drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 1))
    private int onRedirectingFontRendererRenderTabStringDrawingSpectator(FontRenderer fontRenderer, String text, float x, float y, int color) {
        Pair<FontRenderer, Boolean> renderer = BetterTabModule.getCustomFontRenderer(fontRenderer);
        FontRenderer fontRendererCustom = renderer.getFirst() != null ? renderer.getFirst() : fontRenderer;
        return fontRendererCustom.drawStringWithShadow(text, x, y + ((renderer.getSecond()) ? (BetterTabModule.isSegoeFont() ? 0.88F : 1.53F) : 0.0F), color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 2))
    private int onRedirectingFontRendererRenderTabStringDrawingNormalPlayer(FontRenderer fontRenderer, String text, float x, float y, int color) {
        Pair<FontRenderer, Boolean> renderer = BetterTabModule.getCustomFontRenderer(fontRenderer);
        FontRenderer fontRendererCustom = renderer.getFirst() != null ? renderer.getFirst() : fontRenderer;
        return fontRendererCustom.drawStringWithShadow(text, x, y + ((renderer.getSecond()) ? (BetterTabModule.isSegoeFont() ? 0.88F : 1.53F) : 0.0F), color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 3))
    private int onRedirectingFontRendererRenderTabStringDrawingFooter(FontRenderer fontRenderer, String text, float x, float y, int color) {
        return BetterTabModule.getCustomFontRendererInstead(fontRenderer).drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int onRedirectingFontRendererRenderTabGetStringWidth(FontRenderer fontRenderer, String text) {
        return BetterTabModule.getCustomFontRendererInstead(fontRenderer).getStringWidth(text);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
    private int onRedirectingFontRendererRenderTabGetFontHeight(FontRenderer fontRenderer) {
        return BetterTabModule.getCustomFontRendererInstead(fontRenderer).FONT_HEIGHT;
    }

}

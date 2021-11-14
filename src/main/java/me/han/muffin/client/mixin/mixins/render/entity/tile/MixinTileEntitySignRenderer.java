package me.han.muffin.client.mixin.mixins.render.entity.tile;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.imixin.gui.IGuiEditSign;
import me.han.muffin.client.module.modules.render.NoRenderModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntitySignRenderer.class)
public class MixinTileEntitySignRenderer {

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/tileentity/TileEntitySign;signText:[Lnet/minecraft/util/text/ITextComponent;", opcode = Opcodes.GETFIELD))
    public ITextComponent[] onGetRenderViewEntitySignText(TileEntitySign sign) {
        if (NoRenderModule.INSTANCE.isEnabled() && NoRenderModule.INSTANCE.getSignText().getValue()) {
            GuiScreen screen = Globals.mc.currentScreen;
            if (screen instanceof GuiEditSign && ((IGuiEditSign) screen).getTileSign((GuiEditSign) screen).equals(sign)) {
                return sign.signText;
            }
            return new ITextComponent[] {};
        }
        return sign.signText;
    }

}

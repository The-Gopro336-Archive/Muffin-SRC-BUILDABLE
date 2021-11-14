package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.core.mixin.IPatchedTextureManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nonnull;
import java.util.Map;

@Mixin(TextureManager.class)
public class MixinTextureManager implements IPatchedTextureManager {
    @Shadow @Final private Map<ResourceLocation, ITextureObject> mapTextureObjects;

    @Nonnull
    @Override
    public Map<ResourceLocation, ITextureObject> getTextures() {
        return mapTextureObjects;
    }

}
package me.han.muffin.client.mixin;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.Name(value = "Muffin")
@IFMLLoadingPlugin.MCVersion(value = "1.12.2")
public class ClientLoader implements IFMLLoadingPlugin {
    public static final Logger LOGGER = LogManager.getLogger();
    private static boolean isObfuscatedEnvironment = false;

    public ClientLoader() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.muffin.json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        isObfuscatedEnvironment = (Boolean) data.get("runtimeDeobfuscationEnabled");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
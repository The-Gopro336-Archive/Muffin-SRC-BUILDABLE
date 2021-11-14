package me.han.muffin.client.mixin.mixins.render;

import me.han.muffin.client.module.modules.render.BetterTabModule;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Mixin(value = ThreadDownloadImageData.class, priority = 1001)
public abstract class MixinThreadDownloadImageData {
    private static final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)); // , new ThreadFactoryBuilder().setNameFormat("SkinThread #%d").setDaemon(true).setPriority(Thread.MIN_PRIORITY).build());
    // private static final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max((int) ((double) Runtime.getRuntime().availableProcessors() / 1.5), 1));

    @Redirect(method = "loadTextureFromServer", at = @At(value = "INVOKE", target = "java/lang/Thread.start()V"))
    private void onLoadingTextureFromServerInvokeThreadStart(Thread oldThread) {
        if (BetterTabModule.isPoolListOn()) {
            threadPoolExecutor.execute(oldThread);
            return;
        }
        oldThread.start();
    }

}
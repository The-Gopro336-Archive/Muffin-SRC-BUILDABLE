package me.han.muffin.client.mixin.mixins.world;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.world.ChunkEvent;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Chunk.class)
public abstract class MixinChunk {

    @Inject(method = "onUnload", at = @At(value = "RETURN"))
    private void onUnloadPost(CallbackInfo ci) {
        Muffin.getInstance().getEventManager().dispatchEvent(new ChunkEvent(ChunkEvent.ChunkType.UNLOAD, (Chunk) (Object) this));
    }

//    @ModifyArg(method = "getEntitiesOfTypeWithinAABB", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), index = 0, remap = false)
//    private Object onAsd(Object defaultObject) {
//        if (FreecamModule.INSTANCE.isEnabled()) {
//            System.out.println("UWU INJECTED");
//            return FreecamModule.INSTANCE.getCameraGuy();
//        }
//        else return defaultObject;
//    }

}
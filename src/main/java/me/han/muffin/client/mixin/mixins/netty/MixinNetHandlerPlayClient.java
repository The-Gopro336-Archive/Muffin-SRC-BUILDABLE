package me.han.muffin.client.mixin.mixins.netty;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.core.mixin.IPatchedTextureManager;
import me.han.muffin.client.core.mixin.MixinDispatcher;
import me.han.muffin.client.event.events.entity.player.PlayerDeathEvent;
import me.han.muffin.client.imixin.netty.INetHandlerPlayClient;
import me.han.muffin.client.utils.extensions.mc.entity.EntityKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Map;


@Mixin(value = NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient implements INetHandlerPlayClient {
    @Shadow private Minecraft client;

    @Override
    @Accessor(value = "doneLoadingTerrain")
    public abstract boolean getDoneLoadingTerrain();

    /*
    @Inject(method = "handleCombatEvent", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/network/PacketThreadUtil.checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
    private void handleCombatEvent(SPacketCombatEvent event, CallbackInfo ci) {
        if (this.world == null) return;

        if (event.eventType == SPacketCombatEvent.Event.ENTITY_DIED) {
            Entity died = this.world.getEntityByID(event.playerId);
            Entity killer = this.world.getEntityByID(event.entityId);

            // The entity that died should always be a player, this is just a safety measure.
            // The killer isn't checked because in some cases it can be null.
            if (died instanceof EntityPlayer) Muffin.getInstance().getEventManager().dispatchEvent(new PlayerDeathEvent((EntityPlayer) died, (EntityLivingBase) killer));
        }
    }
     */

    @Inject(method = "handleEntityMetadata", at = @At(value = "RETURN"), cancellable = true)
    private void onHandleEntityMetadataPost(SPacketEntityMetadata packetIn, CallbackInfo ci) {
        if (Globals.mc.world != null) {
            Entity entity = Globals.mc.world.getEntityByID(packetIn.getEntityId());
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (!EntityKt.isAlive(player)) {
                    Muffin.getInstance().getEventManager().dispatchEvent(new PlayerDeathEvent(player));
                }
            }
        }
    }

    @Inject(method = "handleChunkData", at = @At("RETURN"))
    public void onHandleChunkDataPost(SPacketChunkData packetIn, CallbackInfo ci) {
        if (packetIn != null) {
            MixinDispatcher.INSTANCE.onHandleChunkData(packetIn);
        }
    }

    @Inject(method = "handleSpawnObject", at = @At(value = "NEW", target = "net/minecraft/entity/item/EntityItem"))
    public void onHandleSpawnObjectNewItem(SPacketSpawnObject packetIn, CallbackInfo info) {
        MixinDispatcher.INSTANCE.onHandleSpawnObject(packetIn);
    }

    @Inject(method = "handleEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;emitParticleAtEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/EnumParticleTypes;I)V"))
    public void onHandleEntityStatusParticle(SPacketEntityStatus status, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.dispatchTotemPop(status);
    }

    @Inject(method = "handleChat", at = @At(value = "HEAD"))
    private void onHandleChatPre(SPacketChat packet, CallbackInfo ci) {
        MixinDispatcher.INSTANCE.dispatchClientChat(packet);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(ITextComponent reason, CallbackInfo ci) {
        TextureManager renderEngine = this.client.getTextureManager();
        Map<ResourceLocation, ITextureObject> textures = ((IPatchedTextureManager) renderEngine).getTextures();
        Iterator<Map.Entry<ResourceLocation, ITextureObject>> iterator = textures.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, ITextureObject> entry = iterator.next();
            if (entry.getKey().getPath().startsWith("skins/")) {
                iterator.remove();
                ITextureObject texture = entry.getValue();
                if (texture instanceof AbstractTexture) {
                    ((AbstractTexture) texture).deleteGlTexture();
                } else {
                    GlStateManager.deleteTexture(texture.getGlTextureId());
                }
            }
        }
    }

}
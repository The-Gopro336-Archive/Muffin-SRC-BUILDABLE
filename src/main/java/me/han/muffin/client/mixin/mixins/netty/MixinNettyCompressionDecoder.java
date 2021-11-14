package me.han.muffin.client.mixin.mixins.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.network.OversizedProtocolEvent;
import net.minecraft.network.NettyCompressionDecoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.zip.Inflater;

@Mixin(value = NettyCompressionDecoder.class)
public abstract class MixinNettyCompressionDecoder extends ByteToMessageDecoder {
    @Shadow @Final private Inflater inflater;

    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        this.inflater.end();
    }

    @ModifyConstant(method = "decode", constant = @Constant(intValue = 0x200000))
    private int onDecodePackets(int n) {
        OversizedProtocolEvent event = new OversizedProtocolEvent();
        Muffin.getInstance().getEventManager().dispatchEvent(event);
        return event.isCanceled() ? 51200000 : n;
    }

}
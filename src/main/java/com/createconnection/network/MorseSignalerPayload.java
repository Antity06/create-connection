package com.createconnection.network;

import com.createconnection.CreateConnection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MorseSignalerPayload(BlockPos pos, boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MorseSignalerPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("morse_signaler"));

    public static final StreamCodec<ByteBuf, MorseSignalerPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, MorseSignalerPayload::pos,
            ByteBufCodecs.BOOL, MorseSignalerPayload::active,
            MorseSignalerPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

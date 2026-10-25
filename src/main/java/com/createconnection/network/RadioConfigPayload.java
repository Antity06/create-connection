package com.createconnection.network;

import com.createconnection.CreateConnection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RadioConfigPayload(BlockPos pos, float tune, boolean transmit) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RadioConfigPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("radio_config"));

    public static final StreamCodec<ByteBuf, RadioConfigPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, RadioConfigPayload::pos,
            ByteBufCodecs.FLOAT, RadioConfigPayload::tune,
            ByteBufCodecs.BOOL, RadioConfigPayload::transmit,
            RadioConfigPayload::new
        );

    public RadioConfigPayload(BlockPos pos, int frequency, boolean transmit) {
        this(pos, (float) frequency, transmit);
    }

    public int frequency() {
        return Math.round(tune);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

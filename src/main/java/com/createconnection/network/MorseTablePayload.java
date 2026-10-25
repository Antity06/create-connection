package com.createconnection.network;

import com.createconnection.CreateConnection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MorseTablePayload(BlockPos pos, int action, String text) implements CustomPacketPayload {
    public static final int ACTION_PUNCH = 0;
    public static final int ACTION_PLAY_STOP = 1;
    public static final int ACTION_TAKE_CARD = 2;

    public static final CustomPacketPayload.Type<MorseTablePayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("morse_table"));

    public static final StreamCodec<ByteBuf, MorseTablePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, MorseTablePayload::pos,
            ByteBufCodecs.VAR_INT, MorseTablePayload::action,
            ByteBufCodecs.STRING_UTF8, MorseTablePayload::text,
            MorseTablePayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

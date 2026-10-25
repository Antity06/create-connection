package com.createconnection.network;

import com.createconnection.CreateConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PhoneActionPayload(BlockPos pos, int action, String arg) implements CustomPacketPayload {
    public static final int DIAL = 0;
    public static final int ANSWER = 1;
    public static final int HANGUP = 2;
    public static final int SET_NUMBER = 3;
    public static final int TOGGLE_SPEAKER = 4;
    public static final int CLOSE_SCREEN = 5;

    public static final CustomPacketPayload.Type<PhoneActionPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("phone_action"));

    public static final StreamCodec<FriendlyByteBuf, PhoneActionPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PhoneActionPayload::pos,
            ByteBufCodecs.VAR_INT,
            PhoneActionPayload::action,
            ByteBufCodecs.stringUtf8(16),
            PhoneActionPayload::arg,
            PhoneActionPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

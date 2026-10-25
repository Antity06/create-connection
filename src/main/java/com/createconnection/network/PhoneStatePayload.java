package com.createconnection.network;

import com.createconnection.CreateConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PhoneStatePayload(
    BlockPos pos,
    String ownNumber,
    boolean payphone,
    boolean speakerMode,
    boolean powered,
    int statusOrdinal,
    String peerNumber
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhoneStatePayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("phone_state"));

    public static final StreamCodec<FriendlyByteBuf, PhoneStatePayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos);
                buf.writeUtf(p.ownNumber, 16);
                buf.writeBoolean(p.payphone);
                buf.writeBoolean(p.speakerMode);
                buf.writeBoolean(p.powered);
                buf.writeVarInt(p.statusOrdinal);
                buf.writeUtf(p.peerNumber, 16);
            },
            buf -> new PhoneStatePayload(
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readUtf(16),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readUtf(16)
            )
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

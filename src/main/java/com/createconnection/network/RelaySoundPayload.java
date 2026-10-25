package com.createconnection.network;

import com.createconnection.CreateConnection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RelaySoundPayload(
    ResourceLocation soundId,
    int sourceOrdinal,
    double x,
    double y,
    double z,
    float volume,
    float pitch,
    long seed
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RelaySoundPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("relay_sound"));

    public static final StreamCodec<FriendlyByteBuf, RelaySoundPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeResourceLocation(p.soundId);
                buf.writeVarInt(p.sourceOrdinal);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.volume);
                buf.writeFloat(p.pitch);
                buf.writeLong(p.seed);
            },
            buf -> new RelaySoundPayload(
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readLong()
            )
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.createconnection.network;

import com.createconnection.CreateConnection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MusicRelayPayload(
    String sinkKey,
    boolean start,
    ResourceLocation soundId,
    double x,
    double y,
    double z,
    float volume
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MusicRelayPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("music_relay"));

    public static final StreamCodec<FriendlyByteBuf, MusicRelayPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.sinkKey, 128);
                buf.writeBoolean(p.start);
                buf.writeResourceLocation(p.soundId);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.volume);
            },
            buf -> new MusicRelayPayload(
                buf.readUtf(128),
                buf.readBoolean(),
                buf.readResourceLocation(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat()
            )
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

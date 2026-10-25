package com.createconnection.network;

import com.createconnection.CreateConnection;
import com.simibubi.create.content.logistics.BigItemStack;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemoteTickerPayload(List<List<BigItemStack>> categories) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoteTickerPayload> TYPE =
        new CustomPacketPayload.Type<>(CreateConnection.id("remote_ticker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteTickerPayload> STREAM_CODEC =
        StreamCodec.composite(
            BigItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
            RemoteTickerPayload::categories,
            RemoteTickerPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

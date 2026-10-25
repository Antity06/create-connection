package com.createconnection.radio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RadioLink(UUID networkId, String name) {
    public static final Codec<RadioLink> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(RadioLink::networkId),
        Codec.STRING.fieldOf("name").forGetter(RadioLink::name)
    ).apply(i, RadioLink::new));

    public static final StreamCodec<FriendlyByteBuf, RadioLink> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        RadioLink::networkId,
        ByteBufCodecs.STRING_UTF8,
        RadioLink::name,
        RadioLink::new
    );

    public static RadioLink fresh() {
        UUID id = UUID.randomUUID();
        String code = id.toString().substring(0, 4).toUpperCase();
        return new RadioLink(id, "CC-" + code);
    }

    public static boolean sameNetwork(RadioLink a, RadioLink b) {
        return a != null && b != null && a.networkId.equals(b.networkId);
    }
}

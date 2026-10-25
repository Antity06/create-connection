package com.createconnection.registry;

import com.createconnection.CreateConnection;
import com.createconnection.radio.RadioLink;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateConnection.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RadioLink>> RADIO_LINK =
        COMPONENTS.registerComponentType("radio_link", b -> b
            .persistent(RadioLink.CODEC)
            .networkSynchronized(RadioLink.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WALKIE_ACTIVE =
        COMPONENTS.registerComponentType("walkie_active", b -> b
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GlobalPos>>> MIC_SPEAKERS =
        COMPONENTS.registerComponentType("mic_speakers", b -> b
            .persistent(GlobalPos.CODEC.listOf())
            .networkSynchronized(GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MORSE_MESSAGE =
        COMPONENTS.registerComponentType("morse_message", b -> b
            .persistent(Codec.STRING)
            .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PORTABLE_TUNE =
        COMPONENTS.registerComponentType("portable_tune", b -> b
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WALKIE_COLOR =
        COMPONENTS.registerComponentType("walkie_color", b -> b
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private TDataComponents() {}
}

package com.createconnection.registry;

import com.createconnection.CreateConnection;
import com.createconnection.morse.MorseSignalerBlockEntity;
import com.createconnection.morse.MorseTableBlockEntity;
import com.createconnection.phone.TelephoneBlockEntity;
import com.createconnection.radio.MicrophoneStandBlockEntity;
import com.createconnection.radio.RadioAudioBlockEntity;
import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateConnection.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TelephoneBlockEntity>> TELEPHONE =
        BLOCK_ENTITIES.register("telephone", () -> BlockEntityType.Builder.of(
            (pos, state) -> new TelephoneBlockEntity(TBlockEntities.TELEPHONE.get(), pos, state),
            TBlocks.HOME_PHONE.get(), TBlocks.PAYPHONE.get(), TBlocks.SERVICE_PHONE.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioStationBlockEntity>> RADIO_STATION =
        BLOCK_ENTITIES.register("radio_station", () -> BlockEntityType.Builder.of(
            (pos, state) -> new RadioStationBlockEntity(TBlockEntities.RADIO_STATION.get(), pos, state),
            TBlocks.RADIO_STATION.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO =
        BLOCK_ENTITIES.register("radio", () -> BlockEntityType.Builder.of(
            (pos, state) -> new RadioBlockEntity(TBlockEntities.RADIO.get(), pos, state),
            TBlocks.RADIO.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioAudioBlockEntity>> RADIO_AUDIO =
        BLOCK_ENTITIES.register("radio_audio", () -> BlockEntityType.Builder.of(
            (pos, state) -> new RadioAudioBlockEntity(TBlockEntities.RADIO_AUDIO.get(), pos, state),
            TBlocks.RADIO_SPEAKER.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MicrophoneStandBlockEntity>> MICROPHONE_STAND =
        BLOCK_ENTITIES.register("microphone_stand", () -> BlockEntityType.Builder.of(
            (pos, state) -> new MicrophoneStandBlockEntity(TBlockEntities.MICROPHONE_STAND.get(), pos, state),
            TBlocks.MICROPHONE_STAND.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MorseSignalerBlockEntity>> MORSE_SIGNALER =
        BLOCK_ENTITIES.register("morse_signaler", () -> BlockEntityType.Builder.of(
            (pos, state) -> new MorseSignalerBlockEntity(TBlockEntities.MORSE_SIGNALER.get(), pos, state),
            TBlocks.MORSE_SIGNALER.get()
        ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MorseTableBlockEntity>> MORSE_TABLE =
        BLOCK_ENTITIES.register("morse_table", () -> BlockEntityType.Builder.of(
            (pos, state) -> new MorseTableBlockEntity(TBlockEntities.MORSE_TABLE.get(), pos, state),
            TBlocks.MORSE_TABLE.get()
        ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private TBlockEntities() {}
}

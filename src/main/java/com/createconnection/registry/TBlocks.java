package com.createconnection.registry;

import com.createconnection.CreateConnection;
import com.createconnection.morse.MorseSignalerBlock;
import com.createconnection.morse.MorseTableBlock;
import com.createconnection.phone.HomePhoneBlock;
import com.createconnection.phone.PayphoneBlock;
import com.createconnection.phone.service.ServicePhoneBlock;
import com.createconnection.radio.MicrophoneStandBlock;
import com.createconnection.radio.RadioAudioBlock;
import com.createconnection.radio.RadioBlock;
import com.createconnection.radio.RadioStationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(CreateConnection.MOD_ID);

    public static final DeferredBlock<HomePhoneBlock> HOME_PHONE =
        BLOCKS.register("home_phone", () -> new HomePhoneBlock(device()));

    public static final DeferredBlock<PayphoneBlock> PAYPHONE =
        BLOCKS.register("payphone", () -> new PayphoneBlock(device().strength(3.0f, 12.0f)));

    public static final DeferredBlock<ServicePhoneBlock> SERVICE_PHONE =
        BLOCKS.register("service_phone", () -> new ServicePhoneBlock(device()));

    public static final DeferredBlock<RadioStationBlock> RADIO_STATION =
        BLOCKS.register("radio_station", () -> new RadioStationBlock(device()));

    public static final DeferredBlock<RadioBlock> RADIO =
        BLOCKS.register("radio", () -> new RadioBlock(device()));

    public static final DeferredBlock<RadioAudioBlock> RADIO_SPEAKER =
        BLOCKS.register("radio_speaker", () -> new RadioAudioBlock(device()));

    public static final DeferredBlock<MicrophoneStandBlock> MICROPHONE_STAND =
        BLOCKS.register("microphone_stand", () -> new MicrophoneStandBlock(device()));

    public static final DeferredBlock<MorseSignalerBlock> MORSE_SIGNALER =
        BLOCKS.register("morse_signaler", () -> new MorseSignalerBlock(device()));

    public static final DeferredBlock<MorseTableBlock> MORSE_TABLE =
        BLOCKS.register("morse_table", () -> new MorseTableBlock(device().strength(2.0f, 8.0f)));

    private static BlockBehaviour.Properties device() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5f, 6.0f)
            .sound(SoundType.COPPER)
            .noOcclusion();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private TBlocks() {}
}

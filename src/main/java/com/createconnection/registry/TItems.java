package com.createconnection.registry;

import com.createconnection.CreateConnection;
import com.createconnection.morse.PunchCardItem;
import com.createconnection.radio.HandMicrophoneItem;
import com.createconnection.radio.MegaphoneItem;
import com.createconnection.radio.PortableStationItem;
import com.createconnection.radio.RadioHeadsetItem;
import com.createconnection.radio.WalkieTalkieItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CreateConnection.MOD_ID);

    public static final DeferredItem<?> HOME_PHONE = ITEMS.registerSimpleBlockItem(TBlocks.HOME_PHONE);
    public static final DeferredItem<?> PAYPHONE = ITEMS.registerSimpleBlockItem(TBlocks.PAYPHONE);
    public static final DeferredItem<?> SERVICE_PHONE = ITEMS.registerSimpleBlockItem(TBlocks.SERVICE_PHONE);
    public static final DeferredItem<?> RADIO_STATION = ITEMS.registerSimpleBlockItem(TBlocks.RADIO_STATION);
    public static final DeferredItem<?> RADIO = ITEMS.registerSimpleBlockItem(TBlocks.RADIO);
    public static final DeferredItem<?> RADIO_SPEAKER = ITEMS.registerSimpleBlockItem(TBlocks.RADIO_SPEAKER);
    public static final DeferredItem<?> MICROPHONE_STAND = ITEMS.registerSimpleBlockItem(TBlocks.MICROPHONE_STAND);
    public static final DeferredItem<?> MORSE_SIGNALER = ITEMS.registerSimpleBlockItem(TBlocks.MORSE_SIGNALER);
    public static final DeferredItem<?> MORSE_TABLE = ITEMS.registerSimpleBlockItem(TBlocks.MORSE_TABLE);

    public static final DeferredItem<WalkieTalkieItem> WALKIE_TALKIE =
        ITEMS.register("walkie_talkie", () -> new WalkieTalkieItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<RadioHeadsetItem> RADIO_HEADSET =
        ITEMS.register("radio_headset", () -> new RadioHeadsetItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PortableStationItem> PORTABLE_STATION =
        ITEMS.register("portable_station", () -> new PortableStationItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<HandMicrophoneItem> HAND_MICROPHONE =
        ITEMS.register("hand_microphone", () -> new HandMicrophoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<MegaphoneItem> MEGAPHONE =
        ITEMS.register("megaphone", () -> new MegaphoneItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PunchCardItem> PUNCH_CARD =
        ITEMS.register("punch_card", () -> new PunchCardItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> SPEAKER = ITEMS.registerSimpleItem("speaker");
    public static final DeferredItem<Item> MICROPHONE = ITEMS.registerSimpleItem("microphone");

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private TItems() {}
}

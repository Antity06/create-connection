package com.createconnection.client.ponder;

import com.createconnection.CreateConnection;
import com.createconnection.registry.TBlocks;
import com.createconnection.registry.TItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public class ConnectionPonderPlugin implements PonderPlugin {
    public static final ResourceLocation TAG_PHONES = CreateConnection.id("phones");
    public static final ResourceLocation TAG_RADIO = CreateConnection.id("radio");

    @Override
    public String getModId() {
        return CreateConnection.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemLike> h = helper.withKeyFunction(i -> BuiltInRegistries.ITEM.getKey(i.asItem()));

        h.forComponents(TBlocks.HOME_PHONE.get())
            .addStoryBoard("phone_line", ConnectionScenes::phoneLine, TAG_PHONES)
            .addStoryBoard("phone_bell", ConnectionScenes::phoneBell, TAG_PHONES);

        h.forComponents(TBlocks.PAYPHONE.get())
            .addStoryBoard("payphone", ConnectionScenes::payphone, TAG_PHONES)
            .addStoryBoard("phone_line", ConnectionScenes::phoneLine);

        h.forComponents(TBlocks.SERVICE_PHONE.get())
            .addStoryBoard("service_phone", ConnectionScenes::servicePhone, TAG_PHONES)
            .addStoryBoard("phone_line", ConnectionScenes::phoneLine);

        h.forComponents(TBlocks.RADIO_STATION.get())
            .addStoryBoard("radio_station", ConnectionScenes::radioStation, TAG_RADIO);

        h.forComponents(TBlocks.RADIO.get())
            .addStoryBoard("radio", ConnectionScenes::radio, TAG_RADIO)
            .addStoryBoard("radio_station", ConnectionScenes::radioStation);

        h.forComponents(TItems.WALKIE_TALKIE.get(), TItems.RADIO_HEADSET.get())
            .addStoryBoard("walkie_headset", ConnectionScenes::walkieAndHeadset, TAG_RADIO)
            .addStoryBoard("radio_station", ConnectionScenes::radioStation);

        h.forComponents(
            TBlocks.RADIO_SPEAKER.get(),
            TBlocks.RADIO_MICROPHONE.get(),
            TBlocks.MICROPHONE_STAND.get(),
            TItems.MEGAPHONE.get(),
            TItems.HAND_MICROPHONE.get()
        ).addStoryBoard("radio_audio", ConnectionScenes::radioAudio, TAG_RADIO);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG_PHONES)
            .addToIndex()
            .item(TBlocks.HOME_PHONE.get(), true, false)
            .title("Телефонная связь")
            .description("Проводные телефоны, питаемые от электричества Power Grid")
            .register();

        helper.registerTag(TAG_RADIO)
            .addToIndex()
            .item(TBlocks.RADIO.get(), true, false)
            .title("Радиосвязь")
            .description("Радиостанции, рации и громкоговорители на базе Create: Radars")
            .register();

        PonderTagRegistrationHelper<ItemLike> byItem = helper.withKeyFunction(i -> BuiltInRegistries.ITEM.getKey(i.asItem()));
        byItem.addToTag(TAG_PHONES)
            .add(TBlocks.HOME_PHONE.get())
            .add(TBlocks.PAYPHONE.get())
            .add(TBlocks.SERVICE_PHONE.get());

        byItem.addToTag(TAG_RADIO)
            .add(TBlocks.RADIO_STATION.get())
            .add(TBlocks.RADIO.get())
            .add(TBlocks.RADIO_SPEAKER.get())
            .add(TBlocks.RADIO_MICROPHONE.get())
            .add(TBlocks.MICROPHONE_STAND.get())
            .add(TItems.WALKIE_TALKIE.get())
            .add(TItems.RADIO_HEADSET.get())
            .add(TItems.MEGAPHONE.get())
            .add(TItems.HAND_MICROPHONE.get());
    }
}

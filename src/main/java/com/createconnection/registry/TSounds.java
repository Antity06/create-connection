package com.createconnection.registry;

import com.createconnection.CreateConnection;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, CreateConnection.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_STATIC = register("radio.static");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHONE_CHIRP = register("phone.chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHONE_BELL = register("phone.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORSE_TONE = register("morse.tone");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CreateConnection.id(name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private TSounds() {}
}

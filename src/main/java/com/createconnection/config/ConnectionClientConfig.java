package com.createconnection.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConnectionClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue PHONE_VOLUME;
    public static final ModConfigSpec.IntValue RADIO_VOLUME;

    public static float volume(boolean radio) {
        int pct = radio ? RADIO_VOLUME.get() : PHONE_VOLUME.get();
        return (float) Math.max(0, Math.min(200, pct)) / 100.0f;
    }

    private ConnectionClientConfig() {}

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("volume");
        PHONE_VOLUME = b.comment("Громкость звуков из телефона (%)")
            .defineInRange("phoneVolume", 100, 0, 200);
        RADIO_VOLUME = b.comment("Громкость звуков из рации и радиодинамиков (%)")
            .defineInRange("radioVolume", 100, 0, 200);
        b.pop();
        SPEC = b.build();
    }
}

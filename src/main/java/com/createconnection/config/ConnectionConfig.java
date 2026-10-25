package com.createconnection.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConnectionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PHONE_MIN_VOLTAGE;
    public static final ModConfigSpec.DoubleValue PHONE_MAX_VOLTAGE;
    public static final ModConfigSpec.DoubleValue PHONE_RESISTANCE;
    public static final ModConfigSpec.IntValue PHONE_RING_SECONDS;
    public static final ModConfigSpec.DoubleValue PHONE_MAX_USE_DISTANCE;
    public static final ModConfigSpec.IntValue PHONE_MAX_TRACE_NODES;
    public static final ModConfigSpec.IntValue PAYPHONE_COST;
    public static final ModConfigSpec.DoubleValue TRADE_MAX_VILLAGER_DISTANCE;
    public static final ModConfigSpec.DoubleValue RADIO_RANGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue RADIO_REQUIRE_RADAR_RUNNING;
    public static final ModConfigSpec.IntValue RADIO_UNLINKED_RANGE;
    public static final ModConfigSpec.IntValue RADIO_NETWORK_RANGE;
    public static final ModConfigSpec.IntValue RADIO_DATA_LINK_RANGE;
    public static final ModConfigSpec.IntValue MEGAPHONE_RANGE;
    public static final ModConfigSpec.IntValue MEGAPHONE_SPEAKER_RANGE;
    public static final ModConfigSpec.BooleanValue RELAY_GAME_SOUNDS;
    public static final ModConfigSpec.DoubleValue RELAY_VOLUME;
    public static final ModConfigSpec.BooleanValue RELAY_MUSIC;

    private ConnectionConfig() {}

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("phone");
        PHONE_MIN_VOLTAGE = b.comment("Минимальное напряжение (В) для работы телефона")
            .defineInRange("minVoltage", 12.0, 1.0, 1000.0);
        PHONE_MAX_VOLTAGE = b.comment("Максимальное напряжение (В) до перегорания телефона")
            .defineInRange("maxVoltage", 240.0, 10.0, 10000.0);
        PHONE_RESISTANCE = b.comment("Сопротивление телефона (Ом) в цепи Power Grid")
            .defineInRange("resistance", 600.0, 10.0, 100000.0);
        PHONE_RING_SECONDS = b.comment("Длительность дозвона (сек) до автоотбоя")
            .defineInRange("ringTimeoutSeconds", 30, 5, 300);
        PHONE_MAX_USE_DISTANCE = b.comment("Максимальная дистанция от телефона без громкой связи")
            .defineInRange("maxUseDistance", 6.0, 2.0, 32.0);
        PHONE_MAX_TRACE_NODES = b.comment("Лимит узлов при трассировке телефонной линии")
            .defineInRange("maxTraceNodes", 1024, 32, 16384);
        PAYPHONE_COST = b.comment("Стоимость звонка с таксофона (в медных самородках / жетонах)")
            .defineInRange("payphoneCost", 1, 0, 64);
        TRADE_MAX_VILLAGER_DISTANCE = b.comment("Радиус поиска жителя вокруг торгового телефона")
            .defineInRange("tradeMaxVillagerDistance", 16.0, 2.0, 64.0);
        b.pop();

        b.push("radio");
        RADIO_RANGE_MULTIPLIER = b.comment("Множитель радиуса от радарной тарелки Create: Radars")
            .defineInRange("rangeMultiplier", 1.0, 0.1, 10.0);
        RADIO_REQUIRE_RADAR_RUNNING = b.comment("Требовать вращение радарной тарелки для работы сети")
            .define("requireRadarRunning", true);
        RADIO_UNLINKED_RANGE = b.comment("Дальность обычного радио и раций без станции (в блоках)")
            .defineInRange("unlinkedRange", 30, 4, 512);
        RADIO_NETWORK_RANGE = b.comment("Базовый радиус радиосети с контроллером (в блоках)")
            .defineInRange("networkRange", 256, 16, 4096);
        RADIO_DATA_LINK_RANGE = b.comment("Максимальная дистанция привязки Data Link (в блоках)")
            .defineInRange("dataLinkRange", 64, 4, 256);
        MEGAPHONE_RANGE = b.comment("Дальность слышимости рупора в руках (в блоках)")
            .defineInRange("megaphoneRange", 64, 16, 256);
        MEGAPHONE_SPEAKER_RANGE = b.comment("Дальность слышимости радиодинамика с установленным рупором (в блоках)")
            .defineInRange("megaphoneSpeakerRange", 64, 16, 256);
        b.pop();

        b.push("audio");
        RELAY_GAME_SOUNDS = b.comment("Передавать звуки мира через телефоны и радио")
            .define("relayGameSounds", true);
        RELAY_VOLUME = b.comment("Множитель громкости ретранслируемых звуков")
            .defineInRange("relayVolume", 0.85, 0.0, 2.0);
        RELAY_MUSIC = b.comment("Ретранслировать пластинки из проигрывателей")
            .define("relayMusic", true);
        b.pop();
        SPEC = b.build();
    }
}

package com.createconnection.compat.voicechat;

import com.createconnection.CreateConnection;
import com.createconnection.audio.AudioRouter;
import com.createconnection.phone.TelephoneBlockEntity;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@ForgeVoicechatPlugin
public class ConnectionVoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi api;
    public static final String CATEGORY_PHONE = "connection_phone";
    public static final String CATEGORY_RADIO = "connection_radio";

    @Override
    public String getPluginId() {
        return CreateConnection.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration reg) {
        reg.registerEvent(VoicechatServerStartedEvent.class, e -> {
            api = e.getVoicechat();
            AudioRouter.voiceDistance = api.getVoiceChatDistance();
            api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(CATEGORY_PHONE)
                .setName("Телефон")
                .setNameTranslationKey("create_connection.voice.phone")
                .setDescription("Голос собеседника по телефону")
                .setDescriptionTranslationKey("create_connection.voice.phone.desc")
                .build());
            api.registerVolumeCategory(api.volumeCategoryBuilder()
                .setId(CATEGORY_RADIO)
                .setName("Рация")
                .setNameTranslationKey("create_connection.voice.radio")
                .setDescription("Голоса из раций и радиодинамиков")
                .setDescriptionTranslationKey("create_connection.voice.radio.desc")
                .build());
            CreateConnection.LOGGER.info("Simple Voice Chat найден: голос по телефону и рации включён");
        });
        reg.registerEvent(VoicechatServerStoppedEvent.class, e -> api = null);
        reg.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi serverApi = api;
        VoicechatConnection senderConn = event.getSenderConnection();
        if (serverApi == null || senderConn == null) return;
        UUID senderId = senderConn.getPlayer().getUuid();
        MicrophonePacket packet = event.getPacket();

        AudioRouter.Sink mega = AudioRouter.megaphoneSink(senderId);
        if (mega != null) {
            event.cancel();
            EntitySoundPacket pkt = packet.entitySoundPacketBuilder()
                .entityId(mega.entityId())
                .distance(mega.distance())
                .whispering(false)
                .build();
            for (UUID listenerId : mega.listeners()) {
                if (!listenerId.equals(senderId)) {
                    VoicechatConnection conn = serverApi.getConnectionOf(listenerId);
                    if (conn != null) {
                        serverApi.sendEntitySoundPacketTo(conn, pkt);
                    }
                }
            }
        }

        List<AudioRouter.Sink> sinks = AudioRouter.voiceSinksForSpeaker(senderId);
        if (sinks.isEmpty()) return;

        for (AudioRouter.Sink sink : sinks) {
            UUID chId = UUID.nameUUIDFromBytes((senderId + ":" + sink.key()).getBytes(StandardCharsets.UTF_8));
            String cat = AudioRouter.isPhoneSink(sink) ? CATEGORY_PHONE : CATEGORY_RADIO;
            switch (sink.kind()) {
                case STATIC -> {
                    StaticSoundPacket pkt = packet.staticSoundPacketBuilder()
                        .channelId(chId)
                        .category(cat)
                        .build();
                    for (UUID listenerId : sink.listeners()) {
                        if (!listenerId.equals(senderId)) {
                            VoicechatConnection conn = serverApi.getConnectionOf(listenerId);
                            if (conn != null) {
                                serverApi.sendStaticSoundPacketTo(conn, pkt);
                            }
                        }
                    }
                }
                case ENTITY -> {
                    EntitySoundPacket pkt = packet.entitySoundPacketBuilder()
                        .entityId(sink.entityId())
                        .distance(sink.distance())
                        .whispering(false)
                        .channelId(chId)
                        .category(cat)
                        .build();
                    for (UUID listenerId : sink.listeners()) {
                        if (!listenerId.equals(senderId)) {
                            VoicechatConnection conn = serverApi.getConnectionOf(listenerId);
                            if (conn != null) {
                                serverApi.sendEntitySoundPacketTo(conn, pkt);
                            }
                        }
                    }
                }
                case LOCATIONAL -> {
                    LocationalSoundPacket pkt = packet.locationalSoundPacketBuilder()
                        .position(serverApi.createPosition(sink.pos().x, sink.pos().y, sink.pos().z))
                        .distance(sink.distance())
                        .channelId(chId)
                        .category(cat)
                        .build();
                    for (UUID listenerId : sink.listeners()) {
                        if (!listenerId.equals(senderId)) {
                            VoicechatConnection conn = serverApi.getConnectionOf(listenerId);
                            if (conn != null) {
                                serverApi.sendLocationalSoundPacketTo(conn, pkt);
                            }
                        }
                    }
                }
            }
        }
    }
}

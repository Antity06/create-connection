package com.createconnection.audio;

import com.createconnection.CreateConnection;
import com.createconnection.config.ConnectionConfig;
import com.createconnection.network.RelaySoundPayload;
import com.createconnection.phone.TelephoneBlockEntity;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SoundRelay {
    private static final ThreadLocal<Boolean> RELAYING = ThreadLocal.withInitial(() -> false);
    private static final Set<SoundSource> IGNORED_SOURCES = Set.of(
        SoundSource.MUSIC,
        SoundSource.AMBIENT,
        SoundSource.UI
    );

    private SoundRelay() {}

    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getLevel() instanceof ServerLevel sl && event.getEntity() != null) {
            relay(sl, event.getEntity().position(), event.getSound(), event.getSource(), event.getNewVolume(), event.getNewPitch());
        }
    }

    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            relay(sl, event.getPosition(), event.getSound(), event.getSource(), event.getNewVolume(), event.getNewPitch());
        }
    }

    private static void relay(
        ServerLevel srcLevel,
        Vec3 soundPos,
        Holder<SoundEvent> soundHolder,
        SoundSource source,
        float volume,
        float pitch
    ) {
        if (RELAYING.get() || soundHolder == null || volume <= 0.02f || IGNORED_SOURCES.contains(source)) {
            return;
        }
        SoundEvent sound = soundHolder.value();
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        if (soundId == null) {
            soundId = sound.getLocation();
        }
        String path = soundId.getPath();
        if (soundId.getNamespace().equals(CreateConnection.MOD_ID)) {
            if (!path.equals("morse_tone")) {
                return;
            }
        }
        if (path.contains("step") || path.contains("breath") || path.startsWith("ambient.")) {
            return;
        }

        double maxRadius = ConnectionConfig.SOUND_CAPTURE_RADIUS.get();
        List<AudioRouter.Source> sources = AudioRouter.captureSources(srcLevel, soundPos, maxRadius);
        if (sources.isEmpty()) {
            return;
        }

        long seed = srcLevel.getRandom().nextLong();
        RELAYING.set(true);
        try {
            for (AudioRouter.Source src : sources) {
                double dist = src.pos().distanceTo(soundPos);
                if (dist > src.captureRadius()) {
                    continue;
                }
                float falloff = (float) (1.0 - (dist / src.captureRadius()) * 0.7);
                float baseVol = Math.min(2.0f, volume * falloff);
                for (AudioRouter.Sink sink : src.sinks()) {
                    float outVol = Math.min(2.5f, baseVol * sink.volumeScale());
                    if (outVol <= 0.03f) {
                        continue;
                    }
                    RelaySoundPayload pkt = new RelaySoundPayload(
                        soundId,
                        source.ordinal(),
                        sink.pos().x,
                        sink.pos().y,
                        sink.pos().z,
                        outVol,
                        pitch * 0.96f,
                        seed
                    );
                    if (sink.privateTarget() != null) {
                        PacketDistributor.sendToPlayer(sink.privateTarget(), pkt);
                    } else {
                        BlockPos bp = BlockPos.containing(sink.pos());
                        PacketDistributor.sendToPlayersTrackingChunk(sink.level(), new ChunkPos(bp), pkt);
                        SoundReactions.onRelayedSound(sink.level(), sink.pos(), soundHolder);
                    }
                    if (sink.phoneAtSink() != null) {
                        BlockPos bp = BlockPos.containing(sink.pos());
                        if (sink.level().getBlockEntity(bp) instanceof TelephoneBlockEntity phone) {
                            phone.notifySoundActivity();
                        }
                    }
                }
            }
        } finally {
            RELAYING.set(false);
        }
    }
}

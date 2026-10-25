package com.createconnection.audio;

import com.createconnection.config.ConnectionConfig;
import com.createconnection.network.MusicRelayPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MusicRelay {
    private static final int SCAN_TICKS = 20;
    private static final int MAX_SCAN_RADIUS = 8;
    private static final Map<String, Playing> playing = new HashMap<>();
    private static int age = 0;

    private MusicRelay() {}

    public static void clear() {
        playing.clear();
    }

    public static void tick(MinecraftServer server) {
        if (++age < SCAN_TICKS) {
            return;
        }
        age = 0;
        if (!ConnectionConfig.RELAY_MUSIC.get()) {
            stopAll(server);
            return;
        }

        Set<String> activeKeys = new HashSet<>();
        for (AudioRouter.Source src : AudioRouter.sources()) {
            if (src.sinks().isEmpty()) continue;
            ServerLevel level = server.getLevel(src.dimension());
            if (level == null) continue;
            int r = (int) Math.min((double) MAX_SCAN_RADIUS, Math.ceil(src.soundRadius()));
            BlockPos center = BlockPos.containing(src.pos());
            for (BlockPos bp : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                BlockEntity be = level.getBlockEntity(bp);
                if (!(be instanceof JukeboxBlockEntity jukebox)) continue;
                JukeboxSongPlayer sp = jukebox.getSongPlayer();
                if (!sp.isPlaying()) continue;
                Object songObj = sp.getSong();
                if (songObj instanceof Holder<?> h) {
                    songObj = h.value();
                }
                if (!(songObj instanceof JukeboxSong song)) continue;
                ResourceLocation soundId = song.soundEvent().value().getLocation();
                for (AudioRouter.Sink sink : src.sinks()) {
                    String streamKey = level.dimension().location() + ":" + bp.asLong() + "->" + sink.key();
                    activeKeys.add(streamKey);
                    if (playing.containsKey(streamKey)) continue;
                    int kindOrd = switch (sink.kind()) {
                        case STATIC -> 0;
                        case ENTITY -> 1;
                        case LOCATIONAL -> 2;
                    };
                    float vol = sink.kind() == AudioRouter.Kind.LOCATIONAL ? 1.0f : 0.5f;
                    MusicRelayPayload pkt = new MusicRelayPayload(
                        streamKey,
                        true,
                        soundId,
                        kindOrd,
                        sink.pos().x,
                        sink.pos().y,
                        sink.pos().z,
                        sink.entityId(),
                        vol,
                        !AudioRouter.isPhoneSink(sink)
                    );
                    send(server, sink.listeners(), pkt);
                    playing.put(streamKey, new Playing(soundId, sink.listeners()));
                }
            }
        }

        Iterator<Map.Entry<String, Playing>> it = playing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Playing> e = it.next();
            if (!activeKeys.contains(e.getKey())) {
                send(server, e.getValue().listeners(), stopPayload(e.getKey(), e.getValue().sound()));
                it.remove();
            }
        }
    }

    private static void stopAll(MinecraftServer server) {
        playing.forEach((k, p) -> send(server, p.listeners(), stopPayload(k, p.sound())));
        playing.clear();
    }

    private static MusicRelayPayload stopPayload(String key, ResourceLocation sound) {
        return new MusicRelayPayload(key, false, sound, 0, 0.0, 0.0, 0.0, -1, 0.0f, false);
    }

    private static void send(MinecraftServer server, List<UUID> listeners, MusicRelayPayload pkt) {
        for (UUID id : listeners) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    record Playing(ResourceLocation sound, List<UUID> listeners) {}
}

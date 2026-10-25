package com.createconnection.audio;

import com.createconnection.config.ConnectionConfig;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.TelephoneBlockEntity;
import com.createconnection.radio.HandMicrophoneItem;
import com.createconnection.radio.MegaphoneItem;
import com.createconnection.radio.MicrophoneStandBlockEntity;
import com.createconnection.radio.PortableStationItem;
import com.createconnection.radio.RadioAudioBlock;
import com.createconnection.radio.RadioAudioBlockEntity;
import com.createconnection.radio.RadioBlockEntity;
import com.createconnection.radio.RadioHeadsetItem;
import com.createconnection.radio.RadioLink;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.radio.RadioStationBlockEntity;
import com.createconnection.radio.WalkieTalkieItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AudioRouter {
    public enum Kind {
        LOCATIONAL,
        ENTITY,
        STATIC
    }

    public record Sink(
        Kind kind,
        ResourceKey<Level> dimension,
        Vec3 pos,
        UUID player,
        int entityId,
        float distance,
        String key,
        List<UUID> listeners
    ) {}

    public record Source(
        ResourceKey<Level> dimension,
        Vec3 pos,
        double soundRadius,
        UUID voiceOnly,
        double voiceRadius,
        List<Sink> sinks,
        String key
    ) {}

    record Receiver(
        ServerPlayer player,
        RadioLink link,
        boolean headset
    ) {}

    private static final int REBUILD_TICKS = 4;
    private static volatile List<Source> sources = List.of();
    private static volatile Map<UUID, List<Sink>> voiceRoutes = Map.of();
    private static int age;
    private static volatile Map<UUID, List<UUID>> megaphones = Map.of();
    public static volatile double voiceDistance = 48.0;

    public static boolean isPhoneSink(Sink sink) {
        return sink.key().startsWith("phone-");
    }

    private AudioRouter() {}

    public static void clear() {
        sources = List.of();
        voiceRoutes = Map.of();
        megaphones = Map.of();
    }

    public static List<Source> sources() {
        return sources;
    }

    public static List<Sink> voiceRoute(UUID speaker) {
        return voiceRoutes.getOrDefault(speaker, List.of());
    }

    public static void tick(MinecraftServer server) {
        if (++age < REBUILD_TICKS) {
            return;
        }
        age = 0;
        List<Source> built = new ArrayList<>();
        buildPhoneSources(server, built);
        buildRadioSources(server, built);
        sources = List.copyOf(built);
        buildMegaphones(server);
        voiceRoutes = buildVoiceRoutes(server, built);
    }

    private static void buildPhoneSources(MinecraftServer server, List<Source> out) {
        for (CallManager.Call call : CallManager.get().activeCalls()) {
            if (!call.active) continue;
            addPhoneSide(server, call, call.caller, out);
            addPhoneSide(server, call, call.callee, out);
        }
    }

    private static void addPhoneSide(MinecraftServer server, CallManager.Call call, GlobalPos from, List<Source> out) {
        GlobalPos to = call.other(from);
        ServerLevel fromLevel = server.getLevel(from.dimension());
        ServerLevel toLevel = server.getLevel(to.dimension());
        if (fromLevel == null || toLevel == null) return;
        TelephoneBlockEntity fromPhone = CallManager.phoneAt(fromLevel, from.pos());
        TelephoneBlockEntity toPhone = CallManager.phoneAt(toLevel, to.pos());
        if (fromPhone == null || toPhone == null) return;

        List<Sink> sinks = new ArrayList<>(1);
        Vec3 toCenter = to.pos().getCenter();
        if (toPhone.isSpeakerMode()) {
            sinks.add(new Sink(
                Kind.LOCATIONAL,
                to.dimension(),
                toCenter,
                null,
                -1,
                12.0f,
                "phone-" + to,
                playersNear(toLevel, toCenter, 12.0)
            ));
        } else {
            UUID user = call.userAt(to);
            ServerPlayer sp = user == null ? null : server.getPlayerList().getPlayer(user);
            if (sp != null) {
                sinks.add(new Sink(
                    Kind.STATIC,
                    sp.level().dimension(),
                    sp.position(),
                    user,
                    sp.getId(),
                    0.0f,
                    "phone-ear-" + user,
                    List.of(user)
                ));
            }
        }

        Vec3 fromCenter = from.pos().getCenter();
        if (fromPhone.isSpeakerMode()) {
            out.add(new Source(from.dimension(), fromCenter, 6.0, null, 6.0, sinks, "phone-" + from));
        } else {
            out.add(new Source(from.dimension(), fromCenter, 3.0, call.userAt(from), 0.0, sinks, "phone-" + from));
        }
    }

    private static void buildRadioSources(MinecraftServer server, List<Source> out) {
        RadioRegistry.refreshPortables(server);
        List<Receiver> receivers = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            for (ItemStack walkie : findAllWalkies(p)) {
                receivers.add(new Receiver(p, WalkieTalkieItem.getLink(walkie), false));
            }
            ItemStack headset = RadioHeadsetItem.worn(p);
            if (headset != null) {
                receivers.add(new Receiver(p, RadioHeadsetItem.getLink(headset), true));
            }
            ItemStack chestPortable = PortableStationItem.worn(p);
            if (!chestPortable.isEmpty()) {
                receivers.add(new Receiver(p, PortableStationItem.getOrCreateLink(chestPortable), true));
            } else {
                ItemStack heldPortable = findHeldActivePortable(p);
                if (!heldPortable.isEmpty()) {
                    receivers.add(new Receiver(p, PortableStationItem.getOrCreateLink(heldPortable), false));
                }
            }
        }

        // 1. Transmitting Walkie-Talkies
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!WalkieTalkieItem.isTransmitting(p)) continue;
            RadioLink link = WalkieTalkieItem.getLink(WalkieTalkieItem.activeWalkie(p));
            List<Sink> sinks = radioSinks(link, p.level(), p.position(), receivers, p.getUUID());
            out.add(new Source(
                p.level().dimension(),
                p.position(),
                4.0,
                p.getUUID(),
                0.0,
                sinks,
                "walkie-" + p.getUUID()
            ));
        }

        // 2. Worn Radio Headsets
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ItemStack headset = RadioHeadsetItem.worn(p);
            if (headset == null) continue;
            RadioLink link = RadioHeadsetItem.getLink(headset);
            List<Sink> sinks = radioSinks(link, p.level(), p.position(), receivers, p.getUUID());
            out.add(new Source(
                p.level().dimension(),
                p.position(),
                2.0,
                p.getUUID(),
                0.0,
                sinks,
                "headset-" + p.getUUID()
            ));
        }

        // 3. Worn or Active Portable Radio Stations
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ItemStack portable = PortableStationItem.worn(p);
            boolean active = !portable.isEmpty() && PortableStationItem.isTransmitMode(portable);
            if (!active) {
                portable = findHeldActivePortable(p);
                active = !portable.isEmpty();
            }
            if (!active) continue;
            RadioLink link = PortableStationItem.getOrCreateLink(portable);
            List<Sink> sinks = radioSinks(link, p.level(), p.position(), receivers, p.getUUID());
            out.add(new Source(
                p.level().dimension(),
                p.position(),
                8.0,
                p.getUUID(),
                0.0,
                sinks,
                "portable-" + p.getUUID()
            ));
        }

        // 4. Hand Microphones
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Source s = handMicSource(p);
            if (s != null) {
                out.add(s);
            }
        }

        // 5. Stationary Radio Stations
        for (RadioStationBlockEntity st : RadioRegistry.stations()) {
            if (st.isRemoved() || st.getLevel() == null || RadioRegistry.stationRange(st) <= 0.0) continue;
            Vec3 center = st.getBlockPos().getCenter();
            List<Sink> sinks = radioSinks(st.getLink(), st.getLevel(), center, receivers, null);
            out.add(new Source(
                st.getLevel().dimension(),
                center,
                8.0,
                null,
                8.0,
                sinks,
                "station-" + st.getBlockPos()
            ));
        }

        // 6. Microphone Stands
        for (MicrophoneStandBlockEntity stand : RadioRegistry.stands()) {
            Source s = standSource(stand);
            if (s != null) {
                out.add(s);
            }
        }

        // 7. Transmitting Radio Blocks
        for (RadioBlockEntity tx : RadioRegistry.radios()) {
            if (tx.isRemoved() || !(tx.getLevel() instanceof ServerLevel sl) || !tx.isOnAir()) continue;
            List<Sink> sinks = new ArrayList<>();
            for (RadioBlockEntity rx : RadioRegistry.radios()) {
                if (rx.isRemoved() || rx.isTransmitter() || !RadioRegistry.radioReach(tx, rx)) continue;
                Vec3 rpos = rx.getBlockPos().getCenter();
                sinks.add(new Sink(
                    Kind.LOCATIONAL,
                    sl.dimension(),
                    rpos,
                    null,
                    -1,
                    16.0f,
                    "radio-" + rx.getBlockPos(),
                    playersNear((ServerLevel) rx.getLevel(), rpos, 16.0)
                ));
            }
            for (RadioStationBlockEntity st : RadioRegistry.stations()) {
                if (RadioRegistry.stationReach(st, tx)) {
                    sinks.addAll(radioSinks(st.getLink(), st.getLevel(), st.getBlockPos().getCenter(), receivers, null));
                }
            }
            for (RadioRegistry.PortableCarrier pc : RadioRegistry.portables()) {
                if (RadioRegistry.portableReach(pc, tx)) {
                    sinks.addAll(radioSinks(pc.link(), pc.level(), pc.pos(), receivers, null));
                }
            }
            Vec3 txPos = tx.getBlockPos().getCenter();
            out.add(new Source(
                sl.dimension(),
                txPos,
                8.0,
                null,
                8.0,
                sinks,
                "radio-" + tx.getBlockPos()
            ));
        }
    }

    public static Source handMicSource(ServerPlayer player) {
        ItemStack mic = HandMicrophoneItem.speaking(player);
        if (mic == null || player.isSpectator()) {
            return null;
        }
        return new Source(
            player.level().dimension(),
            player.position(),
            3.0,
            player.getUUID(),
            0.0,
            micSinks(player.server, mic),
            "mic-" + player.getUUID()
        );
    }

    public static Source standSource(MicrophoneStandBlockEntity stand) {
        if (stand.isRemoved() || !(stand.getLevel() instanceof ServerLevel sl) || !stand.isTransmitting()) {
            return null;
        }
        Vec3 pos = stand.getBlockPos().getCenter();
        return new Source(
            sl.dimension(),
            pos,
            8.0,
            null,
            8.0,
            micSinks(sl.getServer(), stand.getMic()),
            "stand-" + stand.getBlockPos()
        );
    }

    private static List<Sink> micSinks(MinecraftServer server, ItemStack mic) {
        List<Sink> sinks = new ArrayList<>();
        for (GlobalPos gp : HandMicrophoneItem.speakers(mic)) {
            ServerLevel level = server.getLevel(gp.dimension());
            if (level == null || !level.isLoaded(gp.pos())) continue;
            if (!(level.getBlockEntity(gp.pos()) instanceof RadioAudioBlockEntity speaker) || !speaker.isPlaying()) continue;
            Vec3 center = gp.pos().getCenter();
            float range = speakerRange(speaker);
            sinks.add(new Sink(
                Kind.LOCATIONAL,
                gp.dimension(),
                center,
                null,
                -1,
                range,
                "speaker-" + gp.pos(),
                playersNear(level, center, (double) range)
            ));
        }
        return sinks;
    }

    private static List<Sink> radioSinks(RadioLink link, Level level, Vec3 pos, List<Receiver> receivers, UUID sender) {
        List<Sink> sinks = new ArrayList<>();
        for (Receiver r : receivers) {
            if (r.player().getUUID().equals(sender)) continue;
            if (!RadioRegistry.canReach(link, level, pos, r.link(), r.player().level(), r.player().position())) continue;
            if (r.headset()) {
                sinks.add(new Sink(
                    Kind.STATIC,
                    r.player().level().dimension(),
                    r.player().position(),
                    r.player().getUUID(),
                    r.player().getId(),
                    0.0f,
                    "headset-" + r.player().getUUID(),
                    List.of(r.player().getUUID())
                ));
            } else {
                if (link != null) {
                    sinks.add(new Sink(
                        Kind.STATIC,
                        r.player().level().dimension(),
                        r.player().position(),
                        r.player().getUUID(),
                        r.player().getId(),
                        0.0f,
                        "walkie-" + r.player().getUUID(),
                        List.of(r.player().getUUID())
                    ));
                } else {
                    sinks.add(new Sink(
                        Kind.ENTITY,
                        r.player().level().dimension(),
                        r.player().position(),
                        r.player().getUUID(),
                        r.player().getId(),
                        8.0f,
                        "walkie-" + r.player().getUUID(),
                        playersNear((ServerLevel) r.player().level(), r.player().position(), 8.0)
                    ));
                }
            }
        }
        if (link != null && RadioRegistry.covers(link, level, pos)) {
            for (RadioStationBlockEntity st : RadioRegistry.stations()) {
                if (st.isRemoved() || !link.networkId().equals(st.getNetworkId())) continue;
                for (RadioBlockEntity r : RadioRegistry.radios()) {
                    if (r.isRemoved() || r.isTransmitter() || !RadioRegistry.stationReach(st, r)) continue;
                    Vec3 rpos = r.getBlockPos().getCenter();
                    sinks.add(new Sink(
                        Kind.LOCATIONAL,
                        r.getLevel().dimension(),
                        rpos,
                        null,
                        -1,
                        16.0f,
                        "radio-" + r.getBlockPos(),
                        playersNear((ServerLevel) r.getLevel(), rpos, 16.0)
                    ));
                }
            }
            for (RadioRegistry.PortableCarrier pc : RadioRegistry.portables()) {
                if (!link.networkId().equals(pc.link().networkId())) continue;
                for (RadioBlockEntity r : RadioRegistry.radios()) {
                    if (r.isRemoved() || r.isTransmitter() || !RadioRegistry.portableReach(pc, r)) continue;
                    Vec3 rpos = r.getBlockPos().getCenter();
                    sinks.add(new Sink(
                        Kind.LOCATIONAL,
                        r.getLevel().dimension(),
                        rpos,
                        null,
                        -1,
                        16.0f,
                        "radio-" + r.getBlockPos(),
                        playersNear((ServerLevel) r.getLevel(), rpos, 16.0)
                    ));
                }
            }
        }
        return sinks;
    }

    private static List<ItemStack> findAllWalkies(ServerPlayer player) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof WalkieTalkieItem) {
                out.add(s);
            }
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof WalkieTalkieItem && !out.contains(off)) {
            out.add(off);
        }
        return out;
    }

    private static ItemStack findHeldActivePortable(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (PortableStationItem.isActive(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static Map<UUID, List<Sink>> buildVoiceRoutes(MinecraftServer server, List<Source> srcList) {
        if (srcList.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Sink>> routes = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Map<String, Sink> playerSinks = new LinkedHashMap<>();
            for (Source src : srcList) {
                boolean heard;
                if (src.voiceOnly() != null) {
                    heard = src.voiceOnly().equals(p.getUUID());
                } else {
                    heard = p.level().dimension() == src.dimension()
                        && p.position().distanceToSqr(src.pos()) <= src.voiceRadius() * src.voiceRadius();
                }
                if (!heard) continue;
                for (Sink sink : src.sinks()) {
                    if (p.getUUID().equals(sink.player())) continue;
                    playerSinks.putIfAbsent(sink.key(), sink);
                }
            }
            if (!playerSinks.isEmpty()) {
                routes.put(p.getUUID(), List.copyOf(playerSinks.values()));
            }
        }
        return Map.copyOf(routes);
    }

    private static float speakerRange(RadioAudioBlockEntity speaker) {
        BlockState state = speaker.getBlockState();
        if (state.hasProperty(RadioAudioBlock.MEGAPHONE) && state.getValue(RadioAudioBlock.MEGAPHONE)) {
            return 24.0f + (float) ConnectionConfig.MEGAPHONE_SPEAKER_RANGE.get().intValue();
        }
        return 24.0f;
    }

    public static double megaphoneRange() {
        return voiceDistance + (double) ConnectionConfig.MEGAPHONE_RANGE.get().intValue();
    }

    public static List<UUID> megaphoneListeners(UUID speaker) {
        return megaphones.getOrDefault(speaker, List.of());
    }

    private static void buildMegaphones(MinecraftServer server) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        double r = megaphoneRange();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (MegaphoneItem.isShouting(p)) {
                map.put(p.getUUID(), playersNear(p.serverLevel(), p.position(), r));
            }
        }
        megaphones = Map.copyOf(map);
    }

    public static List<UUID> playersNear(ServerLevel level, Vec3 pos, double radius) {
        List<UUID> list = new ArrayList<>();
        double r2 = radius * radius;
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(pos) <= r2) {
                list.add(p.getUUID());
            }
        }
        return List.copyOf(list);
    }

    public static List<Source> sourcesHearing(ResourceKey<Level> dimension, Vec3 pos) {
        List<Source> result = null;
        for (Source src : sources) {
            if (src.dimension() != dimension || src.sinks().isEmpty()) continue;
            if (src.pos().distanceToSqr(pos) > src.soundRadius() * src.soundRadius()) continue;
            if (result == null) {
                result = new ArrayList<>(2);
            }
            result.add(src);
        }
        return result == null ? List.of() : result;
    }
}

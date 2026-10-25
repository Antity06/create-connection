package com.createconnection.radio;

import com.createconnection.config.ConnectionConfig;
import com.createconnection.morse.MorseSignalerBlockEntity;
import com.createconnection.morse.MorseTableBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RadioRegistry {
    public record PortableCarrier(
        UUID holderId,
        Level level,
        Vec3 pos,
        RadioLink link,
        float tune,
        boolean transmitMode,
        boolean activelySpeaking,
        int morseSignal
    ) {}

    private static final Set<RadioStationBlockEntity> STATIONS = ConcurrentHashMap.newKeySet();
    private static final Set<RadioBlockEntity> RADIOS = ConcurrentHashMap.newKeySet();
    private static final Set<RadioAudioBlockEntity> AUDIO = ConcurrentHashMap.newKeySet();
    private static final Set<MicrophoneStandBlockEntity> STANDS = ConcurrentHashMap.newKeySet();
    private static final Set<MorseSignalerBlockEntity> SIGNALERS = ConcurrentHashMap.newKeySet();
    private static final Set<MorseTableBlockEntity> MORSE_TABLES = ConcurrentHashMap.newKeySet();
    private static volatile List<PortableCarrier> PORTABLES = List.of();

    public static final float TUNE_TOLERANCE = 0.35f;
    public static final double STATION_FALLBACK_RANGE = 64.0;

    private RadioRegistry() {}

    public static void add(RadioStationBlockEntity be) { STATIONS.add(be); }
    public static void remove(RadioStationBlockEntity be) { STATIONS.remove(be); }

    public static void add(RadioBlockEntity be) { RADIOS.add(be); }
    public static void remove(RadioBlockEntity be) { RADIOS.remove(be); }
    public static Set<RadioBlockEntity> radios() { return Collections.unmodifiableSet(RADIOS); }

    public static void add(RadioAudioBlockEntity be) { AUDIO.add(be); }
    public static void remove(RadioAudioBlockEntity be) { AUDIO.remove(be); }
    public static Set<RadioAudioBlockEntity> audioBlocks() { return Collections.unmodifiableSet(AUDIO); }

    public static void addStand(MicrophoneStandBlockEntity be) { STANDS.add(be); }
    public static void removeStand(MicrophoneStandBlockEntity be) { STANDS.remove(be); }
    public static Set<MicrophoneStandBlockEntity> stands() { return Collections.unmodifiableSet(STANDS); }

    public static void addSignaler(MorseSignalerBlockEntity be) { SIGNALERS.add(be); }
    public static void removeSignaler(MorseSignalerBlockEntity be) { SIGNALERS.remove(be); }

    public static void addMorseTable(MorseTableBlockEntity be) { MORSE_TABLES.add(be); }
    public static void removeMorseTable(MorseTableBlockEntity be) { MORSE_TABLES.remove(be); }

    public static Set<RadioStationBlockEntity> stations() { return Collections.unmodifiableSet(STATIONS); }
    public static List<PortableCarrier> portables() { return PORTABLES; }

    public static void clear() {
        STATIONS.clear();
        RADIOS.clear();
        AUDIO.clear();
        STANDS.clear();
        SIGNALERS.clear();
        MORSE_TABLES.clear();
        PORTABLES = List.of();
    }

    public static void refreshPortables(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            PORTABLES = List.of();
            return;
        }
        List<PortableCarrier> list = new ArrayList<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            Set<UUID> seenNetworks = new HashSet<>();
            ItemStack chest = sp.getItemBySlot(EquipmentSlot.CHEST);
            if (chest != null && chest.getItem() instanceof PortableStationItem) {
                addCarrier(list, seenNetworks, sp, chest, true);
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack h = sp.getItemInHand(hand);
                if (h != null && h.getItem() instanceof PortableStationItem) {
                    addCarrier(list, seenNetworks, sp, h, false);
                }
            }
            Inventory inv = sp.getInventory();
            if (inv != null) {
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s != null && s.getItem() instanceof PortableStationItem) {
                        addCarrier(list, seenNetworks, sp, s, false);
                    }
                }
            }
        }
        PORTABLES = List.copyOf(list);
    }

    private static void addCarrier(List<PortableCarrier> list, Set<UUID> seen, ServerPlayer sp, ItemStack stack, boolean worn) {
        RadioLink link = PortableStationItem.getOrCreateLink(stack);
        if (link == null || !seen.add(link.networkId())) {
            return;
        }
        float tune = PortableStationItem.getTune(stack);
        boolean txMode = PortableStationItem.isTransmitMode(stack);
        boolean speaking = (worn && txMode) || PortableStationItem.isActive(stack);
        int morse = getMorseSignalNear(sp.level(), sp.blockPosition(), 8.0);
        list.add(new PortableCarrier(sp.getUUID(), sp.level(), sp.position(), link, tune, txMode, speaking, morse));
    }

    public static int getMorseSignalNear(Level level, BlockPos pos, double maxDist) {
        double maxDistSqr = maxDist * maxDist;
        for (MorseSignalerBlockEntity sig : SIGNALERS) {
            if (!sig.isRemoved() && sig.getLevel() == level && sig.isTransmitting() && sig.getBlockPos().distSqr(pos) <= maxDistSqr) {
                return 15;
            }
        }
        for (MorseTableBlockEntity tbl : MORSE_TABLES) {
            if (!tbl.isRemoved() && tbl.getLevel() == level && tbl.isToneOn() && tbl.getBlockPos().distSqr(pos) <= maxDistSqr) {
                return 15;
            }
        }
        return 0;
    }

    public static void notifyMorsePulseNear(Level level, BlockPos pos, double maxDist) {
        double maxDistSqr = maxDist * maxDist;
        for (RadioBlockEntity radio : RADIOS) {
            if (!radio.isRemoved() && radio.getLevel() == level && radio.isTransmitter() && radio.getBlockPos().distSqr(pos) <= maxDistSqr) {
                radio.notifyMorsePulse();
            }
        }
    }

    public static boolean tunesMatch(float a, float b) {
        return Math.abs(a - b) <= TUNE_TOLERANCE;
    }

    public static double stationRange(RadioStationBlockEntity st) {
        double r = st.getEffectiveRange();
        return r > 0.0 ? r : STATION_FALLBACK_RANGE;
    }

    public static boolean covers(RadioLink link, Level level, Vec3 pos) {
        if (link == null || level == null || pos == null) {
            return false;
        }
        for (RadioStationBlockEntity st : STATIONS) {
            if (st.isRemoved() || st.getLevel() != level || !link.networkId().equals(st.getNetworkId())) {
                continue;
            }
            double r = stationRange(st);
            if (r > 0.0 && st.getBlockPos().getCenter().distanceToSqr(pos) <= r * r) {
                return true;
            }
        }
        double pr2 = PortableStationItem.RANGE * PortableStationItem.RANGE;
        for (PortableCarrier pc : PORTABLES) {
            if (pc.level() == level && link.networkId().equals(pc.link().networkId())) {
                if (pc.pos().distanceToSqr(pos) <= pr2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canReach(RadioLink linkA, Level levelA, Vec3 posA, RadioLink linkB, Level levelB, Vec3 posB) {
        if (linkA == null && linkB == null) {
            if (levelA != levelB) {
                return false;
            }
            double r = (double) ConnectionConfig.RADIO_UNLINKED_RANGE.get();
            return posA.distanceToSqr(posB) <= r * r;
        }
        // Walkie-talkies connected to a station (portable or stationary) cannot be intercepted by other walkie-talkies:
        // both endpoints must belong to the exact same station network.
        if (!RadioLink.sameNetwork(linkA, linkB)) {
            return false;
        }
        return covers(linkA, levelA, posA) && covers(linkB, levelB, posB);
    }

    public static boolean radioReach(RadioBlockEntity a, RadioBlockEntity b) {
        if (a == b || a.getLevel() != b.getLevel() || !tunesMatch(a.getTune(), b.getTune())) {
            return false;
        }
        double r = (double) Math.max(a.getRange(), b.getRange());
        return a.getBlockPos().distSqr(b.getBlockPos()) <= r * r;
    }

    public static boolean stationReach(RadioStationBlockEntity st, RadioBlockEntity r) {
        if (st.isRemoved() || st.getLevel() != r.getLevel() || !tunesMatch(st.getTune(), r.getTune())) {
            return false;
        }
        double range = stationRange(st);
        return st.getBlockPos().distSqr(r.getBlockPos()) <= range * range;
    }

    public static boolean portableReach(PortableCarrier pc, RadioBlockEntity r) {
        if (pc == null || r.isRemoved() || pc.level() != r.getLevel() || !tunesMatch(pc.tune(), r.getTune())) {
            return false;
        }
        double range = PortableStationItem.RANGE;
        return pc.pos().distanceToSqr(r.getBlockPos().getCenter()) <= range * range;
    }

    private static Set<UUID> activeLinkedNetworks(MinecraftServer server) {
        Set<UUID> active = new HashSet<>();
        if (server == null || server.getPlayerList() == null) return active;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            ItemStack w = WalkieTalkieItem.activeWalkie(sp);
            if (w != null) {
                RadioLink l = WalkieTalkieItem.getLink(w);
                if (l != null && covers(l, sp.level(), sp.position())) {
                    active.add(l.networkId());
                }
            }
            ItemStack h = RadioHeadsetItem.worn(sp);
            if (h != null) {
                RadioLink l = RadioHeadsetItem.getLink(h);
                if (l != null && covers(l, sp.level(), sp.position())) {
                    active.add(l.networkId());
                }
            }
        }
        for (PortableCarrier pc : PORTABLES) {
            if (pc.activelySpeaking()) {
                active.add(pc.link().networkId());
            }
        }
        return active;
    }

    public static void tickRadios(MinecraftServer server) {
        refreshPortables(server);
        Set<UUID> activeNets = activeLinkedNetworks(server);
        for (RadioBlockEntity rx : RADIOS) {
            if (rx.isRemoved() || rx.getLevel() == null || rx.isTransmitter()) {
                continue;
            }
            int maxSignal = 0;
            for (RadioBlockEntity tx : RADIOS) {
                if (tx.isRemoved() || !tx.isOnAir() || tx.getInputSignal() <= maxSignal) {
                    continue;
                }
                if (radioReach(tx, rx)) {
                    maxSignal = tx.getInputSignal();
                }
            }
            String stationName = "";
            for (RadioStationBlockEntity st : STATIONS) {
                if (!stationReach(st, rx)) {
                    continue;
                }
                if (stationName.isEmpty()) {
                    stationName = st.getLink().name();
                }
                int sig = st.getInputSignal();
                if (sig == 0 && activeNets.contains(st.getNetworkId())) {
                    sig = 10;
                }
                maxSignal = Math.max(maxSignal, sig);
            }
            for (PortableCarrier pc : PORTABLES) {
                if (!portableReach(pc, rx)) {
                    continue;
                }
                if (stationName.isEmpty()) {
                    stationName = pc.link().name();
                }
                int sig = pc.morseSignal();
                if (sig == 0 && (pc.activelySpeaking() || activeNets.contains(pc.link().networkId()))) {
                    sig = 10;
                }
                maxSignal = Math.max(maxSignal, sig);
            }
            rx.setStation(stationName);
            rx.setOutputSignal(maxSignal);
        }
    }
}

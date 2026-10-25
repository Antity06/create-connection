package com.createconnection.phone;

import com.createconnection.network.PhoneStatePayload;
import com.createconnection.phone.service.PhoneService;
import com.createconnection.registry.TSounds;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CallManager {
    private static final CallManager INSTANCE = new CallManager();
    public static final Pattern VALID_NUMBER = Pattern.compile("^\d{1,5}$");

    private final Map<GlobalPos, Call> callsByPhone = new HashMap<>();
    private final Set<Call> activeCalls = new HashSet<>();
    private final Map<String, PhoneService> services = new HashMap<>();
    private final Map<GlobalPos, ActiveServiceCall> serviceCalls = new HashMap<>();
    private final Map<GlobalPos, Set<UUID>> screenViewers = new HashMap<>();

    private CallManager() {}

    public static CallManager get() {
        return INSTANCE;
    }

    public void reset() {
        this.callsByPhone.clear();
        this.activeCalls.clear();
        this.serviceCalls.clear();
        this.screenViewers.clear();
    }

    public void registerService(PhoneService service) {
        this.services.put(service.number(), service);
    }

    public Collection<PhoneService> services() {
        return Collections.unmodifiableCollection(this.services.values());
    }

    public Set<Call> activeCalls() {
        return Collections.unmodifiableSet(this.activeCalls);
    }

    public Call callAt(GlobalPos pos) {
        return this.callsByPhone.get(pos);
    }

    public Status statusOf(GlobalPos pos) {
        if (this.serviceCalls.containsKey(pos)) {
            return Status.ACTIVE;
        }
        Call c = this.callsByPhone.get(pos);
        if (c == null) {
            return Status.IDLE;
        }
        if (c.answered) {
            return Status.ACTIVE;
        }
        return c.caller.equals(pos) ? Status.DIALING_OUT : Status.RINGING_IN;
    }

    public String peerNumberOf(GlobalPos pos) {
        ActiveServiceCall sc = this.serviceCalls.get(pos);
        if (sc != null) {
            return sc.service.number();
        }
        Call c = this.callsByPhone.get(pos);
        if (c == null) {
            return "";
        }
        return c.caller.equals(pos) ? c.calleeNumber : c.callerNumber;
    }

    public void onPlayerInteract(ServerPlayer player, TelephoneBlockEntity phone) {
        GlobalPos pos = phone.globalPos();
        if (statusOf(pos) == Status.RINGING_IN) {
            answer(player, phone);
            return;
        }
        this.screenViewers.computeIfAbsent(pos, k -> new HashSet<>()).add(player.getUUID());
        sendStateTo(player, phone);
    }

    public void closeScreen(ServerPlayer player, TelephoneBlockEntity phone) {
        Set<UUID> set = this.screenViewers.get(phone.globalPos());
        if (set != null) {
            set.remove(player.getUUID());
        }
    }

    public void setNumber(ServerPlayer player, TelephoneBlockEntity phone, String raw) {
        if (phone.isPayphone()) return;
        String num = raw == null ? "" : raw.trim();
        if (!num.isEmpty() && (!VALID_NUMBER.matcher(num).matches() || this.services.containsKey(num))) {
            player.displayClientMessage(
                Component.translatable("create_connection.msg.invalid_number").withStyle(ChatFormatting.RED),
                true
            );
            return;
        }
        PhoneSavedData data = PhoneSavedData.get((ServerLevel) phone.getLevel());
        if (!data.assignNumber(num, phone.globalPos())) {
            player.displayClientMessage(
                Component.translatable("create_connection.msg.number_taken", num).withStyle(ChatFormatting.RED),
                true
            );
            return;
        }
        phone.setNumberInternal(num);
        player.displayClientMessage(
            Component.translatable("create_connection.msg.number_set", num.isEmpty() ? "—" : num).withStyle(ChatFormatting.GREEN),
            true
        );
        broadcastState((ServerLevel) phone.getLevel(), phone.globalPos());
    }

    public void toggleSpeaker(ServerPlayer player, TelephoneBlockEntity phone) {
        phone.setSpeakerMode(!phone.isSpeakerMode());
        Call c = this.callsByPhone.get(phone.globalPos());
        if (c != null) {
            if (c.caller.equals(phone.globalPos())) {
                c.callerPlayer = player.getUUID();
            } else {
                c.calleePlayer = player.getUUID();
            }
        }
        broadcastState((ServerLevel) phone.getLevel(), phone.globalPos());
    }

    public Component dial(ServerPlayer player, TelephoneBlockEntity fromPhone, String rawTarget) {
        ServerLevel level = (ServerLevel) fromPhone.getLevel();
        GlobalPos fromPos = fromPhone.globalPos();
        if (statusOf(fromPos) != Status.IDLE) {
            return notify(player, Component.translatable("create_connection.msg.already_in_call").withStyle(ChatFormatting.RED));
        }
        if (!fromPhone.isPowered()) {
            return notify(player, Component.translatable("create_connection.msg.no_power").withStyle(ChatFormatting.RED));
        }
        String target = rawTarget == null ? "" : rawTarget.trim();
        if (!VALID_NUMBER.matcher(target).matches()) {
            return notify(player, Component.translatable("create_connection.msg.invalid_number").withStyle(ChatFormatting.RED));
        }
        if (fromPhone.isPayphone() && !consumeCopperCoin(player)) {
            return notify(player, Component.translatable("create_connection.msg.need_coin").withStyle(ChatFormatting.GOLD));
        }

        PhoneService svc = this.services.get(target);
        if (svc != null) {
            fromPhone.setBlockFlags(false, true);
            ActiveServiceCall sc = new ActiveServiceCall(fromPos, player.getUUID(), svc);
            this.serviceCalls.put(fromPos, sc);
            svc.onConnected(level, fromPhone, player);
            broadcastState(level, fromPos);
            return Component.translatable("create_connection.msg.service_connected", target).withStyle(ChatFormatting.GREEN);
        }

        PhoneSavedData data = PhoneSavedData.get(level);
        GlobalPos targetPos = data.findByNumber(target);
        if (targetPos == null || targetPos.equals(fromPos)) {
            playTone(level, fromPos, false);
            return notify(player, Component.translatable("create_connection.msg.unreachable", target).withStyle(ChatFormatting.RED));
        }

        ServerLevel targetLevel = level.getServer().getLevel(targetPos.dimension());
        if (targetLevel != null && targetLevel == level && targetLevel.isLoaded(targetPos.pos())) {
            if (!(targetLevel.getBlockEntity(targetPos.pos()) instanceof TelephoneBlockEntity toPhone) || !toPhone.isPowered()) {
                return notify(player, Component.translatable("create_connection.msg.target_unpowered").withStyle(ChatFormatting.RED));
            }
            if (!PhoneLineTracer.connected(level, fromPos.pos(), targetPos.pos())) {
                return notify(player, Component.translatable("create_connection.msg.no_line").withStyle(ChatFormatting.RED));
            }
            if (statusOf(targetPos) != Status.IDLE) {
                playTone(level, fromPos, true);
                return notify(player, Component.translatable("create_connection.msg.busy", target).withStyle(ChatFormatting.YELLOW));
            }

            Call call = new Call(fromPos, targetPos, fromPhone.getNumber(), target, player.getUUID());
            this.callsByPhone.put(fromPos, call);
            this.callsByPhone.put(targetPos, call);
            this.activeCalls.add(call);

            fromPhone.setBlockFlags(false, true);
            toPhone.setBlockFlags(true, false);

            broadcastState(level, fromPos);
            broadcastState(targetLevel, targetPos);
            return notify(player, Component.translatable("create_connection.msg.calling", target).withStyle(ChatFormatting.YELLOW));
        }
        return notify(player, Component.translatable("create_connection.msg.unreachable", target).withStyle(ChatFormatting.RED));
    }

    public void answer(ServerPlayer player, TelephoneBlockEntity phone) {
        GlobalPos pos = phone.globalPos();
        Call call = this.callsByPhone.get(pos);
        if (call != null && !call.answered && call.callee.equals(pos)) {
            call.answered = true;
            call.calleePlayer = player.getUUID();
            call.ticksSinceStart = 0;

            ServerLevel level = (ServerLevel) phone.getLevel();
            phone.setBlockFlags(false, true);
            level.playSound(null, pos.pos(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6f, 1.4f);

            broadcastState(level, call.caller);
            broadcastState(level, call.callee);
            player.displayClientMessage(
                Component.translatable(
                    "create_connection.msg.connected",
                    call.callerNumber.isEmpty() ? Component.translatable("create_connection.unknown_caller") : call.callerNumber
                ).withStyle(ChatFormatting.GREEN),
                true
            );
        }
    }

    public void hangup(TelephoneBlockEntity phone) {
        if (phone.getLevel() instanceof ServerLevel sl) {
            endCallAt(sl.getServer(), phone.globalPos(), true);
        }
    }

    public void onPhoneRemoved(ServerLevel level, TelephoneBlockEntity phone) {
        GlobalPos gp = phone.globalPos();
        endCallAt(level.getServer(), gp, false);
        PhoneSavedData.get(level).removeAt(gp);
        this.screenViewers.remove(gp);
    }

    public void endCallAt(MinecraftServer server, GlobalPos pos, boolean playClick) {
        ActiveServiceCall sc = this.serviceCalls.remove(pos);
        if (sc != null) {
            resetPhoneBlock(server, pos, playClick);
            return;
        }
        Call call = this.callsByPhone.remove(pos);
        if (call != null) {
            this.activeCalls.remove(call);
            GlobalPos other = call.caller.equals(pos) ? call.callee : call.caller;
            this.callsByPhone.remove(other);
            resetPhoneBlock(server, call.caller, playClick);
            resetPhoneBlock(server, call.callee, playClick);
        }
    }

    private void resetPhoneBlock(MinecraftServer server, GlobalPos gp, boolean playClick) {
        ServerLevel level = server.getLevel(gp.dimension());
        if (level != null) {
            if (level.isLoaded(gp.pos()) && level.getBlockEntity(gp.pos()) instanceof TelephoneBlockEntity phone) {
                phone.setBlockFlags(false, false);
                if (playClick) {
                    level.playSound(null, gp.pos(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.3f);
                }
            }
            broadcastState(level, gp);
        }
    }

    public void tick(MinecraftServer server) {
        Iterator<Map.Entry<GlobalPos, ActiveServiceCall>> sit = this.serviceCalls.entrySet().iterator();
        while (sit.hasNext()) {
            ActiveServiceCall sc = sit.next().getValue();
            sc.ticks++;
            ServerLevel level = server.getLevel(sc.phone.dimension());
            ServerPlayer sp = server.getPlayerList().getPlayer(sc.player);
            if (level == null || !level.isLoaded(sc.phone.pos())
                || !(level.getBlockEntity(sc.phone.pos()) instanceof TelephoneBlockEntity phone)
                || !phone.isPowered()
                || (sp != null && sp.isShiftKeyDown() && sp.position().distanceToSqr(sc.phone.pos().getCenter()) < 36.0)
                || !sc.service.tick(level, phone, sp, sc.ticks)) {
                sit.remove();
                resetPhoneBlock(server, sc.phone, true);
            }
        }

        for (Call call : Set.copyOf(this.activeCalls)) {
            call.ticksSinceStart++;
            ServerLevel level = server.getLevel(call.caller.dimension());
            if (level == null) {
                endCallAt(server, call.caller, false);
            } else {
                TelephoneBlockEntity callerBe = level.isLoaded(call.caller.pos()) && level.getBlockEntity(call.caller.pos()) instanceof TelephoneBlockEntity p ? p : null;
                TelephoneBlockEntity calleeBe = level.isLoaded(call.callee.pos()) && level.getBlockEntity(call.callee.pos()) instanceof TelephoneBlockEntity p ? p : null;
                if (callerBe == null || calleeBe == null || !callerBe.isPowered() || !calleeBe.isPowered()) {
                    endCallAt(server, call.caller, true);
                } else if (call.ticksSinceStart % 40 == 0 && !PhoneLineTracer.connected(level, call.caller.pos(), call.callee.pos())) {
                    endCallAt(server, call.caller, true);
                } else if (!call.answered) {
                    if (call.ticksSinceStart > 600) {
                        endCallAt(server, call.caller, true);
                    } else if (call.ticksSinceStart % 25 == 1) {
                        level.playSound(null, call.callee.pos(), TSounds.PHONE_RING.get(), SoundSource.BLOCKS, 1.4f, 1.0f);
                        level.playSound(null, call.caller.pos(), TSounds.DIAL_TONE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                    }
                } else if (shouldSneakHangup(server, call.callerPlayer, call.caller, callerBe)
                    || shouldSneakHangup(server, call.calleePlayer, call.callee, calleeBe)) {
                    endCallAt(server, call.caller, true);
                }
            }
        }
    }

    private boolean shouldSneakHangup(MinecraftServer server, UUID playerId, GlobalPos phonePos, TelephoneBlockEntity phone) {
        if (playerId == null) return false;
        ServerPlayer sp = server.getPlayerList().getPlayer(playerId);
        if (sp == null || sp.level().dimension() != phonePos.dimension()) {
            return !phone.isSpeakerMode();
        }
        double dist2 = sp.position().distanceToSqr(phonePos.pos().getCenter());
        if (!phone.isSpeakerMode() && dist2 > 64.0) {
            return true;
        }
        return sp.isShiftKeyDown() && dist2 <= 36.0;
    }

    private boolean consumeCopperCoin(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Item nugget = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "copper_nugget"));
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && (st.is(nugget) || st.is(Items.COPPER_INGOT))) {
                st.shrink(1);
                player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.7f, 1.5f);
                return true;
            }
        }
        return false;
    }

    private void playTone(ServerLevel level, GlobalPos pos, boolean busy) {
        level.playSound(null, pos.pos(), TSounds.BUSY_TONE.get(), SoundSource.BLOCKS, 0.7f, busy ? 1.0f : 0.8f);
    }

    private Component notify(ServerPlayer player, Component msg) {
        if (player != null) {
            player.displayClientMessage(msg, true);
        }
        return msg;
    }

    public void broadcastState(ServerLevel level, GlobalPos pos) {
        if (!level.isLoaded(pos.pos())) return;
        if (!(level.getBlockEntity(pos.pos()) instanceof TelephoneBlockEntity phone)) return;
        Set<UUID> viewers = this.screenViewers.get(pos);
        if (viewers != null && !viewers.isEmpty()) {
            for (UUID id : Set.copyOf(viewers)) {
                ServerPlayer sp = level.getServer().getPlayerList().getPlayer(id);
                if (sp != null) {
                    sendStateTo(sp, phone);
                }
            }
        }
    }

    public void sendStateTo(ServerPlayer player, TelephoneBlockEntity phone) {
        GlobalPos gp = phone.globalPos();
        PhoneStatePayload payload = new PhoneStatePayload(
            phone.getBlockPos(),
            phone.getNumber(),
            phone.isPayphone(),
            phone.isSpeakerMode(),
            phone.isPowered(),
            statusOf(gp).ordinal(),
            peerNumberOf(gp)
        );
        PacketDistributor.sendToPlayer(player, payload);
    }

    public enum Status {
        IDLE,
        DIALING_OUT,
        RINGING_IN,
        ACTIVE
    }

    public static final class Call {
        public final GlobalPos caller;
        public final GlobalPos callee;
        public final String callerNumber;
        public final String calleeNumber;
        public UUID callerPlayer;
        public UUID calleePlayer;
        public boolean answered;
        public int ticksSinceStart;

        Call(GlobalPos caller, GlobalPos callee, String callerNumber, String calleeNumber, UUID callerPlayer) {
            this.caller = caller;
            this.callee = callee;
            this.callerNumber = callerNumber;
            this.calleeNumber = calleeNumber;
            this.callerPlayer = callerPlayer;
        }
    }

    private static final class ActiveServiceCall {
        final GlobalPos phone;
        final UUID player;
        final PhoneService service;
        int ticks;

        ActiveServiceCall(GlobalPos phone, UUID player, PhoneService service) {
            this.phone = phone;
            this.player = player;
            this.service = service;
        }
    }
}

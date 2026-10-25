package com.createconnection.radio;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RadioStationBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ITunable {
    private RadioLink link;
    private final ControllerLink controller = new ControllerLink();
    private int frequency;
    private float tune;
    private int inputSignal;
    private boolean connected;
    private int range;

    public RadioStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (link == null) {
            link = RadioLink.fresh();
            setChanged();
        }
        if (level != null && !level.isClientSide) {
            RadioRegistry.add(this);
            refresh();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RadioRegistry.remove(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        int sig = Math.max(level.getBestNeighborSignal(worldPosition), RadioRegistry.getMorseSignalNear(level, worldPosition, 8.0));
        if (sig != inputSignal) {
            inputSignal = sig;
            sendData();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level != null && !level.isClientSide) {
            refresh();
        }
    }

    private void refresh() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        int r = controller.refresh(sl, worldPosition);
        boolean conn = r >= 0;
        int clampedRange = Math.max(0, r);
        if (conn != connected || clampedRange != range) {
            connected = conn;
            range = clampedRange;
            setChanged();
            sendData();
        }
        boolean online = getEffectiveRange() > 0.0;
        BlockState state = getBlockState();
        if (state.hasProperty(RadioStationBlock.ONLINE) && state.getValue(RadioStationBlock.ONLINE) != online) {
            level.setBlockAndUpdate(worldPosition, state.setValue(RadioStationBlock.ONLINE, online));
        }
    }

    public void connect(BlockPos a, BlockPos b) {
        controller.connect(a, b);
        refresh();
        setChanged();
        sendData();
    }

    public boolean isConnected() {
        return connected;
    }

    public double getEffectiveRange() {
        return connected ? (double) range : 0.0;
    }

    public boolean covers(BlockPos pos) {
        double r = getEffectiveRange();
        return r > 0.0 && worldPosition.distSqr(pos) <= r * r;
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    @Override
    public float getTune() {
        return tune;
    }

    public int getInputSignal() {
        return inputSignal;
    }

    @Override
    public void configure(int freq, boolean transmit) {
        configureTune((float) freq, transmit);
    }

    @Override
    public void configureTune(float newTune, boolean transmit) {
        float clamped = Mth.clamp(newTune, 0.0f, 15.0f);
        clamped = Math.round(clamped * 100.0f) / 100.0f;
        int newFreq = Mth.clamp(Math.round(clamped), 0, 15);
        if (Math.abs(clamped - this.tune) <= 0.001f && newFreq == this.frequency) {
            return;
        }
        this.tune = clamped;
        this.frequency = newFreq;
        setChanged();
        sendData();
    }

    public RadioLink getLink() {
        if (link == null) {
            link = RadioLink.fresh();
        }
        return link;
    }

    public UUID getNetworkId() {
        return link == null ? null : link.networkId();
    }

    public Component describe() {
        RadioLink l = getLink();
        if (!connected) {
            return Component.translatable("create_connection.msg.station_no_controller", l.name()).withStyle(ChatFormatting.RED);
        }
        if (getEffectiveRange() <= 0.0) {
            return Component.translatable("create_connection.msg.station_radar_idle", l.name()).withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("create_connection.msg.station_online", l.name(), (int) getEffectiveRange()).withStyle(ChatFormatting.GREEN);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (link != null) {
            tag.putUUID("Network", link.networkId());
            tag.putString("NetworkName", link.name());
        }
        tag.putInt("Frequency", frequency);
        tag.putFloat("Tune", tune);
        tag.putInt("Input", inputSignal);
        controller.write(tag);
        if (clientPacket) {
            tag.putBoolean("Connected", connected);
            tag.putInt("Range", range);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.hasUUID("Network")) {
            link = new RadioLink(tag.getUUID("Network"), tag.getString("NetworkName"));
        }
        frequency = Mth.clamp(tag.getInt("Frequency"), 0, 15);
        if (tag.contains("Tune")) {
            tune = Mth.clamp(tag.getFloat("Tune"), 0.0f, 15.0f);
        } else {
            tune = (float) frequency;
        }
        inputSignal = tag.getInt("Input");
        controller.read(tag);
        if (clientPacket) {
            connected = tag.getBoolean("Connected");
            range = tag.getInt("Range");
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.station")));
        tooltip.add(Component.literal(" ").append(Component.literal(getLink().name()).withStyle(ChatFormatting.AQUA)));
        String freqStr = Math.abs(tune - Math.round(tune)) < 0.05f
            ? String.format(Locale.ROOT, "%02d", Math.round(tune))
            : String.format(Locale.ROOT, "%04.1f", tune);
        tooltip.add(Component.literal(" ").append(Component.translatable("create_connection.goggles.frequency", freqStr).withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal(" ").append(describe()));
        return true;
    }
}

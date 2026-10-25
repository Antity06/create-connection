package com.createconnection.radio;

import com.createconnection.config.ConnectionConfig;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RadioBlockEntity extends SmartBlockEntity implements ITunable {
    private int frequency;
    private float tune;
    private int inputSignal;
    private int outputSignal;
    private int morseCarrierTicks;
    private String station = "";

    public RadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public boolean isTransmitter() {
        return getBlockState().getValue(RadioBlock.TRANSMIT);
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    @Override
    public float getTune() {
        return tune;
    }

    public boolean isOnAir() {
        return isTransmitter() && (inputSignal > 0 || morseCarrierTicks > 0);
    }

    public void notifyMorsePulse() {
        this.morseCarrierTicks = 20;
    }

    public int getInputSignal() {
        return inputSignal;
    }

    public int getOutputSignal() {
        return isTransmitter() ? 0 : outputSignal;
    }

    public int getSignalLevel() {
        return isTransmitter() ? inputSignal : outputSignal;
    }

    public int getRange() {
        return unlinkedRange();
    }

    public String getStation() {
        return station;
    }

    void setStation(String s) {
        if (!s.equals(station)) {
            station = s;
            sendData();
        }
    }

    private static int unlinkedRange() {
        try {
            return ConnectionConfig.RADIO_UNLINKED_RANGE.get();
        } catch (IllegalStateException e) {
            return 30;
        }
    }

    @Override
    public void configure(int freq, boolean transmit) {
        configureTune((float) freq, transmit);
    }

    @Override
    public void configureTune(float newTune, boolean transmit) {
        if (level == null || level.isClientSide) {
            return;
        }
        float clamped = Mth.clamp(newTune, 0.0f, 15.0f);
        clamped = Math.round(clamped * 100.0f) / 100.0f;
        int newFreq = Mth.clamp(Math.round(clamped), 0, 15);
        boolean changed = (Math.abs(clamped - this.tune) > 0.001f) || (newFreq != this.frequency);
        this.tune = clamped;
        this.frequency = newFreq;

        BlockState state = getBlockState();
        if (state.getValue(RadioBlock.TRANSMIT) != transmit) {
            inputSignal = 0;
            outputSignal = 0;
            level.setBlock(worldPosition, state.setValue(RadioBlock.TRANSMIT, transmit).setValue(RadioBlock.ACTIVE, false), 3);
            level.updateNeighborsAt(worldPosition, state.getBlock());
            changed = true;
        }
        if (changed) {
            setChanged();
            sendData();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RadioRegistry.add(this);
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
        if (morseCarrierTicks > 0) {
            morseCarrierTicks--;
        }
        if (isTransmitter()) {
            int sig = Math.max(level.getBestNeighborSignal(worldPosition), RadioRegistry.getMorseSignalNear(level, worldPosition, 8.0));
            if (sig != inputSignal) {
                inputSignal = sig;
                setActive(sig > 0);
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                sendData();
            }
        }
    }

    void setOutputSignal(int sig) {
        if (sig == outputSignal || level == null) {
            return;
        }
        outputSignal = sig;
        setChanged();
        setActive(sig > 0);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        sendData();
    }

    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(RadioBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(RadioBlock.ACTIVE, active), 3);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Frequency", frequency);
        tag.putFloat("Tune", tune);
        tag.putInt("Input", inputSignal);
        tag.putInt("Output", outputSignal);
        if (clientPacket) {
            tag.putString("Station", station);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        frequency = Mth.clamp(tag.getInt("Frequency"), 0, 15);
        if (tag.contains("Tune")) {
            tune = Mth.clamp(tag.getFloat("Tune"), 0.0f, 15.0f);
        } else {
            tune = (float) frequency;
        }
        inputSignal = tag.getInt("Input");
        outputSignal = tag.getInt("Output");
        if (clientPacket) {
            station = tag.getString("Station");
        }
    }
}

package com.createconnection.morse;

import com.createconnection.radio.RadioRegistry;
import com.createconnection.registry.TSounds;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MorseSignalerBlockEntity extends SmartBlockEntity {
    private boolean transmitting;
    private UUID activePlayer;
    private int timeoutTicks;
    private int toneTick;

    public MorseSignalerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            RadioRegistry.addSignaler(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RadioRegistry.removeSignaler(this);
    }

    public boolean isTransmitting() {
        return transmitting;
    }

    public void setTransmitting(ServerPlayer player, boolean active) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (active) {
            this.activePlayer = player.getUUID();
            this.timeoutTicks = 8;
            RadioRegistry.notifyMorsePulseNear(level, worldPosition, 8.0);
            if (!this.transmitting) {
                this.transmitting = true;
                this.toneTick = 0;
                updateBlockActive(true);
                playMorseTone();
            }
        } else if (this.transmitting && (this.activePlayer == null || this.activePlayer.equals(player.getUUID()))) {
            stopTransmitting();
        }
    }

    private void stopTransmitting() {
        this.transmitting = false;
        this.activePlayer = null;
        this.timeoutTicks = 0;
        updateBlockActive(false);
    }

    private void updateBlockActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(MorseSignalerBlock.ACTIVE) && state.getValue(MorseSignalerBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(MorseSignalerBlock.ACTIVE, active), 3);
            level.updateNeighborsAt(worldPosition, state.getBlock());
        }
    }

    private void playMorseTone() {
        if (level == null || level.isClientSide) return;
        RadioRegistry.notifyMorsePulseNear(level, worldPosition, 8.0);
        level.playSound(null, worldPosition, TSounds.MORSE_TONE.get(), SoundSource.BLOCKS, 0.85f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (transmitting) {
            timeoutTicks--;
            if (timeoutTicks <= 0) {
                stopTransmitting();
                return;
            }
            toneTick++;
            if (toneTick % 2 == 0) {
                playMorseTone();
            }
        }
    }
}

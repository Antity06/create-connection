package com.createconnection.radio;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RadioAudioBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private int frequency = 0;

    public RadioAudioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (this.level != null && !this.level.isClientSide) {
            RadioRegistry.add(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RadioRegistry.remove(this);
    }

    public boolean isSpeaker() {
        return getBlockState().getBlock() instanceof RadioAudioBlock b && b.isSpeaker();
    }

    public boolean isActive() {
        return getBlockState().hasProperty(RadioAudioBlock.POWERED) && getBlockState().getValue(RadioAudioBlock.POWERED);
    }

    public boolean hasMegaphone() {
        return isSpeaker() && getBlockState().getValue(RadioAudioBlock.MEGAPHONE);
    }

    public float getVolumeMultiplier() {
        return hasMegaphone() ? 2.0f : 1.0f;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setFrequency(int f) {
        this.frequency = Math.max(0, Math.min(15, f));
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Freq", this.frequency);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.frequency = tag.getInt("Freq");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = isSpeaker() ? "create_connection.goggles.speaker" : "create_connection.goggles.microphone";
        tooltip.add(Component.literal("    ").append(Component.translatable(key).withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.literal("    ").append(Component.translatable(
            "create_connection.goggles.frequency",
            String.format("%02d", this.frequency)
        ).withStyle(ChatFormatting.AQUA)));
        if (hasMegaphone()) {
            tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.megaphone_boost")
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        return true;
    }
}

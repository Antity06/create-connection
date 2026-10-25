package com.createconnection.radio;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MicrophoneStandBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private ItemStack mic = ItemStack.EMPTY;

    public MicrophoneStandBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void initialize() {
        super.initialize();
        if (this.level != null && !this.level.isClientSide) {
            RadioRegistry.addStand(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        RadioRegistry.removeStand(this);
    }

    public boolean hasMic() {
        return !this.mic.isEmpty() && getBlockState().getValue(MicrophoneStandBlock.HAS_MIC);
    }

    public ItemStack getMic() {
        return this.mic;
    }

    public List<GlobalPos> linkedSpeakers() {
        return this.mic.isEmpty() ? List.of() : HandMicrophoneItem.speakers(this.mic);
    }

    public void setMic(ItemStack stack) {
        this.mic = stack == null ? ItemStack.EMPTY : stack.copyWithCount(1);
        notifyUpdate();
    }

    public ItemStack takeMic() {
        ItemStack out = this.mic;
        this.mic = ItemStack.EMPTY;
        notifyUpdate();
        return out;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!this.mic.isEmpty()) {
            tag.put("Mic", this.mic.save(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.mic = tag.contains("Mic")
            ? ItemStack.parseOptional(registries, tag.getCompound("Mic"))
            : ItemStack.EMPTY;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!hasMic()) {
            tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.stand_empty")
                .withStyle(ChatFormatting.GRAY)));
            return true;
        }
        int n = linkedSpeakers().size();
        tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.stand_active")
            .withStyle(ChatFormatting.GREEN)));
        if (n > 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.tooltip.hand_mic_count", n)
                .withStyle(ChatFormatting.AQUA)));
        }
        return true;
    }
}

package com.createconnection.phone;

import com.createconnection.config.ConnectionConfig;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.Role;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class TelephoneBlockEntity extends SmartBlockEntity implements IElectricEntity, IHaveGoggleInformation {
    private static final TerminalBoundingBox[] TERMINALS = {
        new TerminalBoundingBox(Role.NONE, 2.0, 1.0, 12.0, 5.0, 4.0, 15.0),
        new TerminalBoundingBox(Role.NONE, 11.0, 1.0, 12.0, 14.0, 4.0, 15.0)
    };

    private ElectricBehaviour electricBehaviour;
    private SwitchedWire loadWire;
    private String number = "";
    private boolean speakerMode = false;
    private int soundActivityTicks = 0;

    public TelephoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        this.electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(this.electricBehaviour);
    }

    @Override
    public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
        builder.setTerminalCount(2);
        float r = (float) (double) ConnectionConfig.PHONE_RESISTANCE.get();
        this.loadWire = builder.connectSwitch(r, builder.terminalNode(0), builder.terminalNode(1), true);
    }

    @Override
    public TerminalBoundingBox[] getTerminals() {
        return TERMINALS;
    }

    public float getVoltage() {
        if (this.loadWire == null) {
            return 0.0f;
        }
        return Math.abs((float) this.loadWire.potentialDifference());
    }

    public boolean isPowered() {
        return (double) getVoltage() >= ConnectionConfig.PHONE_MIN_VOLTAGE.get();
    }

    public boolean isPayphone() {
        return getBlockState().getBlock() instanceof AbstractPhoneBlock b && b.isPayphone();
    }

    public String getNumber() {
        return isPayphone() ? "" : this.number;
    }

    public void setNumberInternal(String num) {
        this.number = num == null ? "" : num;
        notifyUpdate();
    }

    public boolean isSpeakerMode() {
        return this.speakerMode;
    }

    public void setSpeakerMode(boolean speakerMode) {
        this.speakerMode = speakerMode;
        notifyUpdate();
    }

    public GlobalPos globalPos() {
        return GlobalPos.of(this.level.dimension(), this.worldPosition);
    }

    public void notifySoundActivity() {
        this.soundActivityTicks = 12;
        if (this.level != null) {
            this.level.updateNeighborsAt(this.worldPosition, getBlockState().getBlock());
        }
    }

    public int getComparatorOutput() {
        if (getBlockState().getValue(AbstractPhoneBlock.RINGING)) {
            return 15;
        }
        if (this.soundActivityTicks > 0) {
            return 10;
        }
        return getBlockState().getValue(AbstractPhoneBlock.OFF_HOOK) ? 5 : 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level != null && !this.level.isClientSide && this.soundActivityTicks > 0) {
            this.soundActivityTicks--;
            if (this.soundActivityTicks == 0) {
                this.level.updateNeighborsAt(this.worldPosition, getBlockState().getBlock());
            }
        }
    }

    public void setBlockFlags(boolean ringing, boolean offHook) {
        if (this.level == null) return;
        BlockState st = getBlockState();
        if (!(st.getBlock() instanceof AbstractPhoneBlock)) return;
        if (st.getValue(AbstractPhoneBlock.RINGING) != ringing || st.getValue(AbstractPhoneBlock.OFF_HOOK) != offHook) {
            BlockState next = st
                .setValue(AbstractPhoneBlock.RINGING, ringing)
                .setValue(AbstractPhoneBlock.OFF_HOOK, offHook);
            this.level.setBlock(this.worldPosition, next, 3);
            this.level.updateNeighborsAt(this.worldPosition, st.getBlock());
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("PhoneNumber", this.number);
        tag.putBoolean("SpeakerMode", this.speakerMode);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.number = tag.getString("PhoneNumber");
        this.speakerMode = tag.getBoolean("SpeakerMode");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.phone_header")
            .withStyle(ChatFormatting.GOLD)));
        if (isPayphone()) {
            tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.payphone")
                .withStyle(ChatFormatting.YELLOW)));
        } else {
            String shown = this.number.isEmpty() ? "—" : this.number;
            tooltip.add(Component.literal("    ").append(Component.translatable("create_connection.goggles.number", shown)
                .withStyle(ChatFormatting.AQUA)));
        }
        boolean pwr = isPowered();
        tooltip.add(Component.literal("    ").append(Component.translatable(
            pwr ? "create_connection.goggles.powered" : "create_connection.goggles.unpowered",
            String.format("%.1f", getVoltage())
        ).withStyle(pwr ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return true;
    }
}

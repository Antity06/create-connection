package com.createconnection.radio;

import com.createconnection.config.ConnectionConfig;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.radar.behavior.IRadar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ControllerLink {
    public static final ResourceLocation DATA_LINK = ResourceLocation.fromNamespaceAndPath("create_radar", "data_link");
    public static final ResourceLocation NETWORK_CONTROLLER = ResourceLocation.fromNamespaceAndPath("create_radar", "network_filterer");

    private BlockPos controllerPos;
    private BlockPos dataLinkPos;

    public void connect(BlockPos controllerPos, BlockPos dataLinkPos) {
        this.controllerPos = controllerPos;
        this.dataLinkPos = dataLinkPos;
    }

    public BlockPos controllerPos() {
        return this.controllerPos;
    }

    public void clear() {
        this.controllerPos = null;
        this.dataLinkPos = null;
    }

    public int refresh(ServerLevel level, BlockPos ownerPos) {
        if (this.controllerPos == null || this.dataLinkPos == null) {
            return -1;
        }
        if (!level.isLoaded(this.controllerPos) || !level.isLoaded(this.dataLinkPos)) {
            return -1;
        }
        if (!isDataLinkOn(level, this.dataLinkPos, ownerPos) || !isController(level.getBlockState(this.controllerPos))) {
            clear();
            return -1;
        }
        return networkRange(level, this.controllerPos);
    }

    public static boolean isDataLink(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(DATA_LINK);
    }

    public static boolean isController(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(NETWORK_CONTROLLER);
    }

    public static BlockPos attachedTo(BlockGetter level, BlockPos dataLinkPos) {
        BlockState state = level.getBlockState(dataLinkPos);
        if (!isDataLink(state) || !state.hasProperty(BlockStateProperties.FACING)) {
            return null;
        }
        return dataLinkPos.relative(state.getValue(BlockStateProperties.FACING).getOpposite());
    }

    private static boolean isDataLinkOn(BlockGetter level, BlockPos dataLinkPos, BlockPos expectedTarget) {
        return expectedTarget.equals(attachedTo(level, dataLinkPos));
    }

    private static int networkRange(ServerLevel level, BlockPos controllerPos) {
        int base = ConnectionConfig.RADIO_NETWORK_RANGE.get();
        try {
            NetworkData.Group group = NetworkData.get(level).getGroup(level.dimension(), controllerPos);
            if (group == null || group.radarPos == null || !level.isLoaded(group.radarPos)) {
                return base;
            }
            if (level.getBlockEntity(group.radarPos) instanceof IRadar radar) {
                if (ConnectionConfig.RADIO_REQUIRE_RADAR_RUNNING.get() && !radar.isRunning()) {
                    return 0;
                }
                return (int) Math.round((double) radar.getRange() * ConnectionConfig.RADIO_RANGE_MULTIPLIER.get());
            }
        } catch (Throwable ignored) {
        }
        return base;
    }

    public void write(CompoundTag tag) {
        if (this.controllerPos != null) {
            tag.put("Controller", NbtUtils.writeBlockPos(this.controllerPos));
        }
        if (this.dataLinkPos != null) {
            tag.put("DataLink", NbtUtils.writeBlockPos(this.dataLinkPos));
        }
    }

    public void read(CompoundTag tag) {
        this.controllerPos = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
        this.dataLinkPos = NbtUtils.readBlockPos(tag, "DataLink").orElse(null);
    }
}

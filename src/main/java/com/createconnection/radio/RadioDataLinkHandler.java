package com.createconnection.radio;

import com.createconnection.config.ConnectionConfig;
import com.createconnection.registry.TDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class RadioDataLinkHandler {
    private static final String TAG_CONTROLLER = "SelectedFiltererPos";

    private RadioDataLinkHandler() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (linkDevice(event)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ControllerLink.DATA_LINK)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof RadioStationBlock)) {
            return;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || !custom.copyTag().contains(TAG_CONTROLLER)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) {
            return;
        }
        Player player = event.getEntity();
        BlockPos controllerPos = NbtUtils.readBlockPos(custom.copyTag(), TAG_CONTROLLER).orElse(null);
        stack.remove(DataComponents.CUSTOM_DATA);
        if (controllerPos == null || !level.isLoaded(controllerPos) || !ControllerLink.isController(level.getBlockState(controllerPos))) {
            player.displayClientMessage(Component.translatable("create_connection.msg.radio_no_controller").withStyle(ChatFormatting.RED), true);
            return;
        }
        double maxDist = (double) ConnectionConfig.RADIO_DATA_LINK_RANGE.get();
        if (controllerPos.distSqr(pos) > maxDist * maxDist) {
            player.displayClientMessage(Component.translatable("create_connection.msg.radio_too_far").withStyle(ChatFormatting.RED), true);
            return;
        }
        Direction face = event.getFace();
        if (face == null) {
            return;
        }
        BlockPos linkPos = pos.relative(face);
        if (!level.getBlockState(linkPos).canBeReplaced()) {
            player.displayClientMessage(Component.translatable("create_connection.msg.radio_no_space").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockState linkState = BuiltInRegistries.BLOCK.get(ControllerLink.DATA_LINK).defaultBlockState();
        if (linkState.hasProperty(BlockStateProperties.FACING)) {
            linkState = linkState.setValue(BlockStateProperties.FACING, face);
        }
        level.setBlock(linkPos, linkState, 3);
        SoundType st = linkState.getSoundType(level, linkPos, player);
        level.playSound(null, linkPos, st.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 0.8f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (level.getBlockEntity(pos) instanceof RadioStationBlockEntity station) {
            station.connect(controllerPos, linkPos);
        }
        player.displayClientMessage(Component.translatable("create_connection.msg.station_connected").withStyle(ChatFormatting.GREEN), true);
    }

    private static boolean linkDevice(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof WalkieTalkieItem) && !(stack.getItem() instanceof RadioHeadsetItem)) {
            return false;
        }
        if (!event.getEntity().isShiftKeyDown()) {
            return false;
        }
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        RadioStationBlockEntity station = null;
        if (level.getBlockEntity(clicked) instanceof RadioStationBlockEntity directStation) {
            station = directStation;
        } else {
            BlockPos attached = ControllerLink.attachedTo(level, clicked);
            if (attached != null && level.getBlockEntity(attached) instanceof RadioStationBlockEntity attachedStation) {
                station = attachedStation;
            }
        }
        if (station == null) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) {
            return true;
        }
        stack.set(TDataComponents.RADIO_LINK.get(), station.getLink());
        level.playSound(null, clicked, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, 1.4f);
        event.getEntity().displayClientMessage(
            Component.translatable(
                stack.getItem() instanceof RadioHeadsetItem ? "create_connection.msg.headset_linked" : "create_connection.msg.walkie_linked",
                station.getLink().name()
            ),
            true
        );
        return true;
    }
}

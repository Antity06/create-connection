package com.createconnection.radio;

import com.createconnection.registry.TDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class HandMicrophoneItem extends Item {
    public static final int MAX_SPEAKERS = 16;

    public HandMicrophoneItem(Item.Properties props) {
        super(props);
    }

    public static List<GlobalPos> speakers(ItemStack stack) {
        return stack.getOrDefault(TDataComponents.LINKED_SPEAKERS.get(), List.of());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        if (level.getBlockState(pos).getBlock() instanceof RadioAudioBlock block && block.isSpeaker()) {
            GlobalPos gp = GlobalPos.of(level.dimension(), pos.immutable());
            List<GlobalPos> cur = new ArrayList<>(speakers(stack));
            boolean removed = cur.remove(gp);
            if (!removed) {
                if (cur.size() >= MAX_SPEAKERS) {
                    cur.remove(0);
                }
                cur.add(gp);
            }
            if (!level.isClientSide) {
                stack.set(TDataComponents.LINKED_SPEAKERS.get(), List.copyOf(cur));
                level.playSound(
                    null,
                    pos,
                    removed ? SoundEvents.STONE_BUTTON_CLICK_OFF : SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.8f,
                    removed ? 0.9f : 1.3f
                );
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable(
                            removed ? "create_connection.msg.hand_mic_unlinked" : "create_connection.msg.hand_mic_linked",
                            cur.size()
                        ).withStyle(removed ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
                        true
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player != null && player.isShiftKeyDown() && !speakers(stack).isEmpty()) {
            if (!level.isClientSide) {
                stack.remove(TDataComponents.LINKED_SPEAKERS.get());
                player.displayClientMessage(
                    Component.translatable("create_connection.msg.hand_mic_cleared").withStyle(ChatFormatting.GRAY),
                    true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !speakers(stack).isEmpty()) {
            if (!level.isClientSide) {
                stack.remove(TDataComponents.LINKED_SPEAKERS.get());
                player.displayClientMessage(
                    Component.translatable("create_connection.msg.hand_mic_cleared").withStyle(ChatFormatting.GRAY),
                    true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public static boolean isHoldingToSpeak(Player player) {
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof HandMicrophoneItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        List<GlobalPos> list = speakers(stack);
        if (list.isEmpty()) {
            tooltip.add(Component.translatable("create_connection.tooltip.hand_mic_empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("create_connection.tooltip.hand_mic_count", list.size()).withStyle(ChatFormatting.AQUA));
        }
    }
}

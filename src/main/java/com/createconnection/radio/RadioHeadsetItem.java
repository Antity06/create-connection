package com.createconnection.radio;

import com.createconnection.registry.TDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class RadioHeadsetItem extends Item implements Equipable {
    public RadioHeadsetItem(Item.Properties properties) {
        super(properties);
    }

    public static RadioLink getLink(ItemStack stack) {
        return stack == null ? null : stack.get(TDataComponents.RADIO_LINK.get());
    }

    public static ItemStack worn(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return (head.getItem() instanceof RadioHeadsetItem && getLink(head) != null) ? head : null;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && other.getItem() instanceof PortableStationItem) {
            RadioLink link = PortableStationItem.getOrCreateLink(other);
            stack.set(TDataComponents.RADIO_LINK.get(), link);
            player.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.4f);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();
        if (action == ClickAction.SECONDARY && target != null && target.getItem() instanceof PortableStationItem) {
            RadioLink link = PortableStationItem.getOrCreateLink(target);
            stack.set(TDataComponents.RADIO_LINK.get(), link);
            player.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.4f);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown() && target instanceof Player other) {
            ItemStack portable = PortableStationItem.worn(other);
            if (!portable.isEmpty()) {
                if (!player.level().isClientSide) {
                    RadioLink link = PortableStationItem.getOrCreateLink(portable);
                    stack.set(TDataComponents.RADIO_LINK.get(), link);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, 1.4f);
                    player.displayClientMessage(Component.translatable("create_connection.msg.headset_linked", link.name()), true);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (getLink(stack) != null) {
                    stack.remove(TDataComponents.RADIO_LINK.get());
                    player.displayClientMessage(Component.translatable("create_connection.msg.headset_unlinked"), true);
                } else {
                    InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    ItemStack otherStack = player.getItemInHand(otherHand);
                    ItemStack portable = PortableStationItem.worn(player);
                    if (portable.isEmpty() && otherStack.getItem() instanceof PortableStationItem) {
                        portable = otherStack;
                    }
                    if (!portable.isEmpty()) {
                        RadioLink link = PortableStationItem.getOrCreateLink(portable);
                        stack.set(TDataComponents.RADIO_LINK.get(), link);
                        level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, 1.4f);
                        player.displayClientMessage(Component.translatable("create_connection.msg.headset_linked", link.name()), true);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RadioLink link = getLink(stack);
        tooltip.add(
            link == null
                ? Component.translatable("create_connection.tooltip.headset_unlinked").withStyle(ChatFormatting.GRAY)
                : Component.translatable("create_connection.tooltip.walkie_linked", link.name()).withStyle(ChatFormatting.AQUA)
        );
    }
}

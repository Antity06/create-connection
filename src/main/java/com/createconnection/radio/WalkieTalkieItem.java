package com.createconnection.radio;

import com.createconnection.config.ConnectionConfig;
import com.createconnection.registry.TDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class WalkieTalkieItem extends Item {
    public WalkieTalkieItem(Item.Properties properties) {
        super(properties);
    }

    public static RadioLink getLink(ItemStack stack) {
        return stack == null ? null : stack.get(TDataComponents.RADIO_LINK.get());
    }

    public static int getColorId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        Integer c = stack.get(TDataComponents.WALKIE_COLOR.get());
        return c != null && c >= 0 && c < 16 ? c : -1;
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        if (stack == null || stack.isEmpty()) return;
        if (color == null) {
            stack.remove(TDataComponents.WALKIE_COLOR.get());
        } else {
            stack.set(TDataComponents.WALKIE_COLOR.get(), color.getId());
        }
    }

    public static int getTintColor(ItemStack stack) {
        int id = getColorId(stack);
        if (id >= 0 && id < 16) {
            return DyeColor.byId(id).getTextureDiffuseColor() | 0xFF000000;
        }
        return 0xFFD89E68;
    }

    public static boolean isActive(ItemStack stack) {
        return stack != null
            && stack.getItem() instanceof WalkieTalkieItem
            && Boolean.TRUE.equals(stack.get(TDataComponents.WALKIE_ACTIVE.get()));
    }

    public static ItemStack activeWalkie(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (isActive(s)) {
                return s;
            }
        }
        return null;
    }

    public static boolean isTransmitting(Player player) {
        return activeWalkie(player) != null;
    }

    public static void toggle(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WalkieTalkieItem)) {
            return;
        }
        boolean next = !isActive(stack);
        setActive(stack, next);
        player.level().playSound(
            null,
            player.blockPosition(),
            next ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundSource.PLAYERS,
            0.5f,
            next ? 1.6f : 1.3f
        );
        player.displayClientMessage(
            Component.translatable(next ? "create_connection.msg.walkie_on" : "create_connection.msg.walkie_off")
                .withStyle(next ? ChatFormatting.RED : ChatFormatting.GRAY),
            true
        );
    }

    public static void setActive(ItemStack stack, boolean active) {
        if (active) {
            stack.set(TDataComponents.WALKIE_ACTIVE.get(), true);
        } else {
            stack.remove(TDataComponents.WALKIE_ACTIVE.get());
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && isActive(stack) && entity instanceof Player p) {
            if (!isSelected && p.getOffhandItem() != stack) {
                setActive(stack, false);
            }
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        if (other.getItem() instanceof DyeItem dye) {
            int newId = dye.getDyeColor().getId();
            if (getColorId(stack) != newId) {
                setColor(stack, dye.getDyeColor());
                if (!player.getAbilities().instabuild) {
                    other.shrink(1);
                }
                player.playSound(SoundEvents.DYE_USE, 1.0f, 1.0f);
                return true;
            }
        }
        if (other.getItem() instanceof PortableStationItem) {
            RadioLink link = PortableStationItem.getOrCreateLink(other);
            stack.set(TDataComponents.RADIO_LINK.get(), link);
            player.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.4f);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack target = slot.getItem();
        if (target == null || target.isEmpty()) {
            return false;
        }
        if (target.getItem() instanceof DyeItem dye) {
            int newId = dye.getDyeColor().getId();
            if (getColorId(stack) != newId) {
                setColor(stack, dye.getDyeColor());
                if (!player.getAbilities().instabuild) {
                    target.shrink(1);
                }
                player.playSound(SoundEvents.DYE_USE, 1.0f, 1.0f);
                return true;
            }
        }
        if (target.getItem() instanceof PortableStationItem) {
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
                    player.displayClientMessage(Component.translatable("create_connection.msg.walkie_linked", link.name()), true);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        if (otherStack.getItem() instanceof DyeItem dye && getColorId(stack) != dye.getDyeColor().getId()) {
            if (!level.isClientSide) {
                setColor(stack, dye.getDyeColor());
                if (!player.getAbilities().instabuild) {
                    otherStack.shrink(1);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (getLink(stack) != null) {
                    stack.remove(TDataComponents.RADIO_LINK.get());
                    player.displayClientMessage(
                        Component.translatable("create_connection.msg.walkie_unlinked", ConnectionConfig.RADIO_UNLINKED_RANGE.get()),
                        true
                    );
                } else {
                    ItemStack portable = PortableStationItem.worn(player);
                    if (portable.isEmpty() && otherStack.getItem() instanceof PortableStationItem) {
                        portable = otherStack;
                    }
                    if (!portable.isEmpty()) {
                        RadioLink link = PortableStationItem.getOrCreateLink(portable);
                        stack.set(TDataComponents.RADIO_LINK.get(), link);
                        level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, 1.4f);
                        player.displayClientMessage(Component.translatable("create_connection.msg.walkie_linked", link.name()), true);
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            toggle(sp, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RadioLink link = getLink(stack);
        if (link == null) {
            tooltip.add(Component.translatable("create_connection.tooltip.walkie_open").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("create_connection.tooltip.walkie_linked", link.name()).withStyle(ChatFormatting.AQUA));
        }
        int colorId = getColorId(stack);
        if (colorId >= 0 && colorId < 16) {
            DyeColor dc = DyeColor.byId(colorId);
            tooltip.add(Component.translatable("color.minecraft." + dc.getName()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        if (isActive(stack)) {
            tooltip.add(Component.translatable("create_connection.tooltip.walkie_transmitting").withStyle(ChatFormatting.RED));
        }
    }
}

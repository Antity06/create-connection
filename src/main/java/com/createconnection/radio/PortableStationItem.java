package com.createconnection.radio;

import com.createconnection.client.ClientHooks;
import com.createconnection.registry.TDataComponents;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PortableStationItem extends Item implements Equipable {
    public static final double RANGE = 64.0;
    private static final int DEFAULT_ENCODED = 0;

    public PortableStationItem(Item.Properties props) {
        super(props);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    public static RadioLink getLink(ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? null : stack.get(TDataComponents.RADIO_LINK.get());
    }

    public static RadioLink getOrCreateLink(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        RadioLink link = stack.get(TDataComponents.RADIO_LINK.get());
        if (link == null) {
            link = RadioLink.fresh();
            stack.set(TDataComponents.RADIO_LINK.get(), link);
        }
        return link;
    }

    public static float getTune(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0f;
        }
        Integer v = stack.get(TDataComponents.PORTABLE_TUNE.get());
        int raw = v != null ? v : DEFAULT_ENCODED;
        int mag = Math.abs(raw) % 10000;
        return Mth.clamp((float) mag / 100.0f, 0.0f, 15.0f);
    }

    public static int getFrequency(ItemStack stack) {
        return Math.round(getTune(stack));
    }

    public static boolean isTransmitMode(ItemStack stack) {
        return isActive(stack);
    }

    public static void configureTune(ItemStack stack, float tune, boolean transmit) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PortableStationItem)) {
            return;
        }
        float clamped = Mth.clamp(tune, 0.0f, 15.0f);
        int mag = Math.round(clamped * 100.0f);
        int encoded = (transmit ? 10000 : 0) + mag;
        stack.set(TDataComponents.PORTABLE_TUNE.get(), encoded);
        if (transmit) {
            stack.set(TDataComponents.WALKIE_ACTIVE.get(), true);
        } else {
            stack.remove(TDataComponents.WALKIE_ACTIVE.get());
        }
        getOrCreateLink(stack);
    }

    public static boolean isActive(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PortableStationItem)) {
            return false;
        }
        if (Boolean.TRUE.equals(stack.get(TDataComponents.WALKIE_ACTIVE.get()))) {
            return true;
        }
        Integer v = stack.get(TDataComponents.PORTABLE_TUNE.get());
        return v != null && v >= 10000;
    }

    public static void setActive(ItemStack stack, boolean active) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PortableStationItem)) {
            return;
        }
        float tune = getTune(stack);
        int mag = Math.round(Mth.clamp(tune, 0.0f, 15.0f) * 100.0f);
        int encoded = (active ? 10000 : 0) + mag;
        stack.set(TDataComponents.PORTABLE_TUNE.get(), encoded);
        if (active) {
            stack.set(TDataComponents.WALKIE_ACTIVE.get(), true);
        } else {
            stack.remove(TDataComponents.WALKIE_ACTIVE.get());
        }
    }

    public static ItemStack worn(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest != null && chest.getItem() instanceof PortableStationItem) {
            return chest;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findInPlayer(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack worn = worn(player);
        if (!worn.isEmpty()) {
            return worn;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack st = player.getItemInHand(hand);
            if (st != null && st.getItem() instanceof PortableStationItem) {
                return st;
            }
        }
        Inventory inv = player.getInventory();
        if (inv != null) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (st != null && st.getItem() instanceof PortableStationItem) {
                    return st;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isPlayerTransmitting(Player player) {
        if (player == null) {
            return false;
        }
        ItemStack worn = worn(player);
        if (!worn.isEmpty() && isActive(worn)) {
            return true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack st = player.getItemInHand(hand);
            if (isActive(st)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            getOrCreateLink(stack);
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(
        ItemStack stack,
        ItemStack other,
        Slot slot,
        ClickAction action,
        Player player,
        SlotAccess access
    ) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        RadioLink link = getOrCreateLink(stack);
        if (other.isEmpty()) {
            if (player.level().isClientSide) {
                ClientHooks.openPortableStation(stack);
            }
            return true;
        }
        if (other.getItem() instanceof WalkieTalkieItem || other.getItem() instanceof RadioHeadsetItem) {
            other.set(TDataComponents.RADIO_LINK.get(), link);
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
        if (target != null && (target.getItem() instanceof WalkieTalkieItem || target.getItem() instanceof RadioHeadsetItem)) {
            RadioLink link = getOrCreateLink(stack);
            target.set(TDataComponents.RADIO_LINK.get(), link);
            player.playSound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.4f);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        getOrCreateLink(stack);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                ClientHooks.openPortableStation(stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            boolean next = !isActive(stack);
            setActive(stack, next);
            level.playSound(
                null,
                sp.blockPosition(),
                next ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
                SoundSource.PLAYERS,
                0.5f,
                next ? 1.6f : 1.3f
            );
            sp.displayClientMessage(
                Component.translatable(next ? "create_connection.msg.walkie_on" : "create_connection.msg.walkie_off")
                    .withStyle(next ? ChatFormatting.RED : ChatFormatting.GRAY),
                true
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext ctx,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        RadioLink link = getLink(stack);
        String netName = link != null ? link.name() : "—";
        float tune = getTune(stack);
        String freqStr = Math.abs(tune - (float) Math.round(tune)) < 0.05f
            ? String.format(Locale.ROOT, "%02d", Math.round(tune))
            : String.format(Locale.ROOT, "%04.1f", tune);
        tooltip.add(Component.translatable("create_connection.tooltip.portable_station.network", netName, 64)
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("create_connection.goggles.frequency", freqStr)
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("create_connection.tooltip.portable_station.tune_hint")
            .withStyle(ChatFormatting.GRAY));
        if (isActive(stack)) {
            tooltip.add(Component.translatable("create_connection.tooltip.walkie_transmitting")
                .withStyle(ChatFormatting.RED));
        }
    }
}

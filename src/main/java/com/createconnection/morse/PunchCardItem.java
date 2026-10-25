package com.createconnection.morse;

import com.createconnection.registry.TDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class PunchCardItem extends Item {
    public PunchCardItem(Item.Properties properties) {
        super(properties);
    }

    public static String getMessage(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String msg = stack.get(TDataComponents.MORSE_MESSAGE.get());
        return msg != null ? msg : "";
    }

    public static void setMessage(ItemStack stack, String message) {
        if (message == null || message.isBlank()) {
            stack.remove(TDataComponents.MORSE_MESSAGE.get());
        } else {
            stack.set(TDataComponents.MORSE_MESSAGE.get(), message.trim());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String msg = getMessage(stack);
        if (msg.isEmpty()) {
            tooltip.add(Component.translatable("create_connection.tooltip.punch_card.empty").withStyle(ChatFormatting.GRAY));
        } else {
            String morse = MorseCode.formatPretty(MorseCode.encode(msg));
            tooltip.add(Component.translatable("create_connection.tooltip.punch_card.message", msg).withStyle(ChatFormatting.GOLD));
            if (!morse.isEmpty()) {
                tooltip.add(Component.translatable("create_connection.tooltip.punch_card.morse", morse).withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}

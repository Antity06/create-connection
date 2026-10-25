package com.createconnection.client;

import com.createconnection.morse.MorseSignalerBlockEntity;
import com.createconnection.morse.MorseTableBlockEntity;
import com.createconnection.network.MorseSignalerPayload;
import com.createconnection.radio.ITunable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientHooks {
    private static BlockPos activeSignalerPos = null;
    private static boolean waitInitialRelease = false;
    private static boolean lastKeyDown = false;
    private static int holdTicks = 0;
    private static ItemStack pendingPortableScreen = ItemStack.EMPTY;

    private ClientHooks() {}

    public static void openRadio(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof ITunable) {
            mc.setScreen(new RadioScreen(pos));
        }
    }

    public static void openPortableStation(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            pendingPortableScreen = stack;
        }
    }

    public static void openMorseTable(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof MorseTableBlockEntity) {
            mc.setScreen(new MorseTableScreen(pos));
        }
    }

    public static void toggleMorseSignaler(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (activeSignalerPos != null && activeSignalerPos.equals(pos)) {
            exitSignalerMode(mc, true);
        } else {
            if (activeSignalerPos != null) {
                PacketDistributor.sendToServer(new MorseSignalerPayload(activeSignalerPos, false));
            }
            activeSignalerPos = pos.immutable();
            waitInitialRelease = true;
            lastKeyDown = false;
            holdTicks = 0;
            mc.player.displayClientMessage(
                Component.translatable("create_connection.msg.signaler_mode_on").withStyle(ChatFormatting.GOLD),
                true
            );
        }
    }

    public static boolean isInSignalerMode() {
        return activeSignalerPos != null;
    }

    private static void exitSignalerMode(Minecraft mc, boolean notify) {
        if (activeSignalerPos != null) {
            PacketDistributor.sendToServer(new MorseSignalerPayload(activeSignalerPos, false));
            activeSignalerPos = null;
        }
        waitInitialRelease = false;
        lastKeyDown = false;
        holdTicks = 0;
        if (notify && mc.player != null) {
            mc.player.displayClientMessage(
                Component.translatable("create_connection.msg.signaler_mode_off").withStyle(ChatFormatting.GRAY),
                true
            );
        }
    }

    public static void onInteractionTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (activeSignalerPos == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            exitSignalerMode(mc, true);
            return;
        }
        if (event.isUseItem()) {
            if (mc.player != null && mc.player.isShiftKeyDown()) {
                event.setCanceled(true);
                event.setSwingHand(false);
                exitSignalerMode(mc, true);
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (pendingPortableScreen != null && !pendingPortableScreen.isEmpty()) {
            ItemStack st = pendingPortableScreen;
            pendingPortableScreen = ItemStack.EMPTY;
            mc.setScreen(new RadioScreen(st));
            return;
        }
        if (activeSignalerPos == null) return;
        if (mc.player == null || mc.level == null || mc.screen != null) {
            exitSignalerMode(mc, false);
            return;
        }
        if (!(mc.level.getBlockEntity(activeSignalerPos) instanceof MorseSignalerBlockEntity)
            || mc.player.position().distanceToSqr(activeSignalerPos.getCenter()) > 64.0) {
            exitSignalerMode(mc, true);
            return;
        }

        boolean useDown = mc.options.keyUse.isDown();
        if (waitInitialRelease) {
            if (!useDown) {
                waitInitialRelease = false;
            } else {
                return;
            }
        }

        if (useDown) {
            if (!lastKeyDown || (holdTicks % 2 == 0)) {
                PacketDistributor.sendToServer(new MorseSignalerPayload(activeSignalerPos, true));
            }
            holdTicks++;
            lastKeyDown = true;
            mc.player.displayClientMessage(
                Component.translatable("create_connection.msg.signaler_transmitting").withStyle(ChatFormatting.YELLOW),
                true
            );
        } else if (lastKeyDown) {
            PacketDistributor.sendToServer(new MorseSignalerPayload(activeSignalerPos, false));
            lastKeyDown = false;
            holdTicks = 0;
            mc.player.displayClientMessage(
                Component.translatable("create_connection.msg.signaler_mode_on").withStyle(ChatFormatting.GOLD),
                true
            );
        }
    }
}

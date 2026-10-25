package com.createconnection.mixin;

import com.createconnection.client.ClientRemoteStock;
import com.createconnection.compat.create.RemoteTickerBridge;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StockKeeperRequestMenu.class, remap = false)
public class StockKeeperRequestMenuMixin {
    @Inject(
        method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lcom/simibubi/create/content/logistics/stockTicker/StockTickerBlockEntity;)V",
        at = @At("TAIL")
    )
    private void createConnection$onOpenServer(MenuType<?> type, int id, Inventory inv, StockTickerBlockEntity holder, CallbackInfo ci) {
        if (inv.player instanceof ServerPlayer sp && holder != null) {
            StockTickerBlockEntity master = RemoteTickerBridge.syncSlaveToMaster(holder);
            if (master != null) {
                RemoteTickerBridge.startSession(sp, holder, master);
            }
        }
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void createConnection$allowRemoteRadioAccess(Player player, CallbackInfoReturnable<Boolean> cir) {
        StockTickerBlockEntity be = (StockTickerBlockEntity) ((StockKeeperRequestMenu) (Object) this).contentHolder;
        if (be == null || be.isRemoved()) {
            return;
        }
        if (player instanceof ServerPlayer sp && RemoteTickerBridge.isRemoteSession(sp.getUUID())) {
            cir.setReturnValue(true);
            return;
        }
        if (player.level().isClientSide && player.position().distanceTo(be.getBlockPos().getCenter()) > 12.0) {
            ClientRemoteStock.clientMenuIsRemote = true;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"), remap = true)
    private void createConnection$onClose(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            RemoteTickerBridge.endSession(sp.getUUID());
        } else if (player.level().isClientSide) {
            ClientRemoteStock.clear();
        }
    }
}

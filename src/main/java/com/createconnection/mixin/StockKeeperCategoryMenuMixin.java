package com.createconnection.mixin;

import com.createconnection.compat.create.RemoteTickerBridge;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StockKeeperCategoryMenu.class, remap = false)
public class StockKeeperCategoryMenuMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void createConnection$allowRemoteRadioAccess(Player player, CallbackInfoReturnable<Boolean> cir) {
        StockTickerBlockEntity be = (StockTickerBlockEntity) ((StockKeeperCategoryMenu) (Object) this).contentHolder;
        if (be != null && !be.isRemoved() && RemoteTickerBridge.findAdjacentTicker(be.getLevel(), be.getBlockPos()) != null) {
            cir.setReturnValue(true);
        }
    }
}

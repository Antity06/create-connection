package com.createconnection.mixin;

import com.createconnection.compat.create.RemoteTickerBridge;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityMixin {
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void createConnection$syncRemoteStockTicker(CallbackInfo ci) {
        if ((Object) this instanceof StockTickerBlockEntity st) {
            RemoteTickerBridge.syncSlaveToMaster(st);
        }
    }
}

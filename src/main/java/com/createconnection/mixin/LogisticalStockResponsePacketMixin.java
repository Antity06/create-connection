package com.createconnection.mixin;

import com.createconnection.client.ClientRemoteStock;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LogisticalStockResponsePacket.class, remap = false)
public class LogisticalStockResponsePacketMixin {
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private void createConnection$ignoreForRemoteMenu(CallbackInfo ci) {
        if (ClientRemoteStock.clientMenuIsRemote) {
            ci.cancel();
        }
    }
}

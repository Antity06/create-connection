package com.createconnection.mixin;

import com.createconnection.client.ClientRemoteStock;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperRequestScreenMixin {
    @Shadow
    public List<List<BigItemStack>> itemsToOrder;

    @Shadow
    private void updateSearchNextTick() {}

    private List<List<BigItemStack>> createConnection$lastApplied = List.of();

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void createConnection$applyRemoteCategories(CallbackInfo ci) {
        if (!ClientRemoteStock.clientMenuIsRemote) {
            return;
        }
        List<List<BigItemStack>> remote = ClientRemoteStock.categories();
        if (!remote.isEmpty() && remote != this.createConnection$lastApplied) {
            this.createConnection$lastApplied = remote;
            this.itemsToOrder = new ArrayList<>(remote.size());
            for (List<BigItemStack> group : remote) {
                List<BigItemStack> copy = new ArrayList<>(group.size());
                for (BigItemStack bis : group) {
                    copy.add(new BigItemStack(bis.stack.copy(), bis.count));
                }
                this.itemsToOrder.add(copy);
            }
            this.updateSearchNextTick();
        }
    }
}

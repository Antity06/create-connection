package com.createconnection.mixin;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerOrderPacket;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntityConfigurationPacket.class, remap = false)
public abstract class BlockEntityConfigurationPacketMixin {
    @Shadow
    @Final
    protected BlockPos pos;

    @Shadow
    protected abstract void applySettings(ServerPlayer player, SyncedBlockEntity be);

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private void createConnection$allowRemoteStockTickerOrder(IPayloadContext context, CallbackInfo ci) {
        if (!((Object) this instanceof StockTickerOrderPacket orderPkt)) {
            return;
        }
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        if (level == null || !level.isLoaded(this.pos)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(this.pos);
        if (be instanceof StockTickerBlockEntity ticker) {
            try {
                Field orderF = StockTickerOrderPacket.class.getDeclaredField("order");
                Field addrF = StockTickerOrderPacket.class.getDeclaredField("address");
                orderF.setAccessible(true);
                addrF.setAccessible(true);
                PackageOrderWithCrafts order = (PackageOrderWithCrafts) orderF.get(orderPkt);
                String address = (String) addrF.get(orderPkt);
                if (com.createconnection.compat.create.RemoteTickerBridge.broadcastOrderIfSlave(ticker, order, address)) {
                    ticker.previouslyUsedAddress = address;
                    ticker.notifyUpdate();
                    ci.cancel();
                    return;
                }
            } catch (Exception ignored) {
            }
            this.applySettings(player, ticker);
            ticker.notifyUpdate();
            ci.cancel();
        }
    }
}

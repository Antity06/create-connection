package com.createconnection.compat.create;

import com.createconnection.network.RemoteTickerPayload;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RemoteStockKeeper {
    private static final Map<UUID, GlobalPos> SESSIONS = new ConcurrentHashMap<>();

    private RemoteStockKeeper() {}

    public static StockTickerBlockEntity findTicker(ServerLevel level, BlockPos phonePos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = phonePos.relative(dir);
            if (!level.isLoaded(adj)) continue;
            if (level.getBlockEntity(adj) instanceof BlazeBurnerBlockEntity) {
                StockTickerBlockEntity st = BlazeBurnerBlockEntity.getStockTicker(level, adj);
                if (st != null) return st;
            }
            Block b = level.getBlockState(adj).getBlock();
            if (b instanceof SeatBlock) {
                for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, new AABB(adj))) {
                    for (Entity passenger : seat.getPassengers()) {
                        BlockPos tickerPos = StockTickerInteractionHandler.getStockTickerPosition(passenger);
                        if (tickerPos != null && level.getBlockEntity(tickerPos) instanceof StockTickerBlockEntity st) {
                            return st;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean open(ServerPlayer player, StockTickerBlockEntity ticker) {
        ServerLevel level = (ServerLevel) ticker.getLevel();
        if (level == null) return false;
        if (player.level() != level) {
            player.displayClientMessage(
                Component.translatable("create_connection.msg.other_dimension").withStyle(ChatFormatting.RED),
                true
            );
            return false;
        }
        if (!ticker.behaviour.mayInteract(player)) {
            player.displayClientMessage(
                Component.translatable("create.stock_keeper.locked").withStyle(ChatFormatting.RED),
                true
            );
            return false;
        }
        BlockPos pos = ticker.getBlockPos();
        SESSIONS.put(player.getUUID(), GlobalPos.of(level.dimension(), pos));
        PacketDistributor.sendToPlayer(
            player,
            new RemoteTickerPayload(pos, Block.getId(ticker.getBlockState()), ticker.getUpdateTag(level.registryAccess()))
        );
        boolean canLock = ticker.behaviour.mayAdministrate(player) && Create.LOGISTICS.isLockable(ticker.behaviour.freqId);
        boolean isLocked = Create.LOGISTICS.isLocked(ticker.behaviour.freqId);
        Objects.requireNonNull(ticker);
        player.openMenu(ticker.new RequestMenuProvider(), buf -> {
            buf.writeBoolean(canLock);
            buf.writeBoolean(isLocked);
            buf.writeBlockPos(pos);
        });
        ticker.getRecentSummary().divideAndSendTo(player, pos);
        return true;
    }

    public static boolean allows(ServerPlayer player, BlockPos pos) {
        GlobalPos gp = SESSIONS.get(player.getUUID());
        return gp != null
            && gp.pos().equals(pos)
            && gp.dimension() == player.level().dimension()
            && (player.containerMenu instanceof StockKeeperRequestMenu || player.containerMenu instanceof StockKeeperCategoryMenu);
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        SESSIONS.keySet().removeIf(uuid -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            return sp == null || (!(sp.containerMenu instanceof StockKeeperRequestMenu) && !(sp.containerMenu instanceof StockKeeperCategoryMenu));
        });
    }

    public static void clear() {
        SESSIONS.clear();
    }
}

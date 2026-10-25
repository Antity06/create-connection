package com.createconnection.phone.service;

import com.createconnection.compat.create.RemoteStockKeeper;
import com.createconnection.config.ConnectionConfig;
import com.createconnection.phone.AbstractPhoneBlock;
import com.createconnection.phone.PhoneKind;
import com.createconnection.phone.TelephoneBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;

public class ServicePhoneBlock extends AbstractPhoneBlock implements IPhoneServiceBlock {
    public ServicePhoneBlock(BlockBehaviour.Properties props) {
        super(props, Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0), backTerminals(3.0, 0.0));
    }

    @Override
    public PhoneKind kind() {
        return PhoneKind.SERVICE;
    }

    public static Villager findBoundVillager(ServerLevel level, BlockPos phonePos) {
        double r = ConnectionConfig.TRADE_MAX_VILLAGER_DISTANCE.get();
        return level.getEntitiesOfClass(Villager.class, new AABB(phonePos).inflate(r), v -> v.isAlive() && !v.isBaby())
            .stream()
            .filter(v -> jobSiteNear(v, level, phonePos))
            .min(Comparator.comparingDouble(v -> v.distanceToSqr(phonePos.getCenter())))
            .orElse(null);
    }

    private static boolean jobSiteNear(Villager villager, ServerLevel level, BlockPos phonePos) {
        Optional<GlobalPos> mem = villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);
        if (mem == null || mem.isEmpty() || mem.get().dimension() != level.dimension()) {
            return false;
        }
        BlockPos jp = mem.get().pos();
        return !jp.equals(phonePos)
            && Math.abs(jp.getX() - phonePos.getX()) <= 1
            && Math.abs(jp.getY() - phonePos.getY()) <= 1
            && Math.abs(jp.getZ() - phonePos.getZ()) <= 1;
    }

    @Override
    public Component onCall(ServerPlayer caller, TelephoneBlockEntity callerPhone, TelephoneBlockEntity servicePhone) {
        ServerLevel level = (ServerLevel) servicePhone.getLevel();
        Villager v = findBoundVillager(level, servicePhone.getBlockPos());
        if (v != null) {
            if (!canAnswer(v)) {
                return Component.translatable("create_connection.msg.unreachable");
            }
            v.setTradingPlayer(caller);
            v.openTradingScreen(caller, v.getDisplayName(), v.getVillagerData().getLevel());
            return null;
        }
        StockTickerBlockEntity ticker = RemoteStockKeeper.findTicker(level, servicePhone.getBlockPos());
        if (ticker != null) {
            return RemoteStockKeeper.open(caller, ticker)
                ? null
                : Component.translatable("create_connection.msg.unreachable");
        }
        return Component.translatable("create_connection.msg.unreachable");
    }

    public static boolean canAnswer(Villager v) {
        return v.isAlive()
            && !v.isBaby()
            && !v.isSleeping()
            && !v.isTrading()
            && v.getVillagerData().getProfession() != VillagerProfession.NONE
            && v.getVillagerData().getProfession() != VillagerProfession.NITWIT
            && !v.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY)
            && !v.getOffers().isEmpty();
    }
}

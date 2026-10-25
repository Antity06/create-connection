package com.createconnection.phone;

import com.createconnection.config.ConnectionConfig;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.JunctionWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

public final class PhoneLineTracer {
    private static final Map<PairKey, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long TTL_TICKS = 40L;

    private PhoneLineTracer() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static boolean connected(ServerLevel level, BlockPos a, BlockPos b) {
        if (a.equals(b)) {
            return true;
        }
        long now = level.getGameTime();
        PairKey key = PairKey.of(level.dimension(), a, b);
        CacheEntry cached = CACHE.get(key);
        if (cached != null && now - cached.tick <= TTL_TICKS) {
            return cached.connected;
        }
        boolean result = traceWireGraph(level, a, b);
        CACHE.put(key, new CacheEntry(result, now));
        return result;
    }

    private static boolean traceWireGraph(ServerLevel level, BlockPos start, BlockPos goal) {
        Map<Node, Set<Node>> adj = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof WireEntity wire && !wire.isRemoved()) {
                Node n1 = toNode(wire.getEndpoint1());
                Node n2 = toNode(wire.getEndpoint2());
                if (n1 != null && n2 != null && !n1.equals(n2)) {
                    adj.computeIfAbsent(n1, k -> new HashSet<>()).add(n2);
                    adj.computeIfAbsent(n2, k -> new HashSet<>()).add(n1);
                }
            }
        }

        Node startNode = new BlockNode(start);
        Node goalNode = new BlockNode(goal);
        if (!adj.containsKey(startNode) || !adj.containsKey(goalNode)) {
            return false;
        }

        int maxHops = ConnectionConfig.PHONE_MAX_WIRE_HOPS.get();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Map<Node, Integer> dist = new HashMap<>();
        queue.add(startNode);
        dist.put(startNode, 0);

        while (!queue.isEmpty()) {
            Node cur = queue.poll();
            int d = dist.get(cur);
            if (cur.equals(goalNode)) {
                return true;
            }
            if (d < maxHops) {
                for (Node nxt : adj.getOrDefault(cur, Set.of())) {
                    if (!dist.containsKey(nxt)) {
                        dist.put(nxt, d + 1);
                        queue.add(nxt);
                    }
                }
            }
        }
        return false;
    }

    private static Node toNode(IWireEndpoint ep) {
        if (ep instanceof BlockWireEndpoint bep) {
            return new BlockNode(bep.getPos());
        }
        if (ep instanceof JunctionWireEndpoint jep) {
            return new JunctionNode(jep);
        }
        return null;
    }

    private sealed interface Node permits BlockNode, JunctionNode {}
    private record BlockNode(BlockPos pos) implements Node {}
    private record JunctionNode(Object id) implements Node {}

    private record CacheEntry(boolean connected, long tick) {}
    private record PairKey(ResourceKey<Level> dim, long minPos, long maxPos) {
        static PairKey of(ResourceKey<Level> dim, BlockPos a, BlockPos b) {
            long la = a.asLong();
            long lb = b.asLong();
            return new PairKey(dim, Math.min(la, lb), Math.max(la, lb));
        }
    }
}

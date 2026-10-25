package com.createconnection.audio;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SoundReactions {
    private SoundReactions() {}

    public static void onRelayedSound(ServerLevel level, Vec3 pos, Holder<SoundEvent> soundHolder) {
        level.gameEvent(null, GameEvent.INSTRUMENT_PLAY, pos);
        ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(soundHolder.value());
        if (id == null) {
            id = soundHolder.value().getLocation();
        }
        String path = id.getPath();
        BlockPos bp = BlockPos.containing(pos);

        if (path.contains("bell.block") || path.equals("block.bell.use")) {
            AABB box = new AABB(bp).inflate(32.0);
            for (Villager v : level.getEntitiesOfClass(Villager.class, box)) {
                Brain<Villager> brain = v.getBrain();
                brain.setMemory(MemoryModuleType.HEARD_BELL_TIME, level.getGameTime());
            }
        }

        if (path.startsWith("music_disc.")) {
            AABB box = new AABB(bp).inflate(16.0);
            for (Allay allay : level.getEntitiesOfClass(Allay.class, box)) {
                allay.getBrain().setMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION, GlobalPos.of(level.dimension(), bp));
                allay.getBrain().setMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, 600);
            }
        }

        if (path.contains("raid.horn")) {
            AABB box = new AABB(bp).inflate(32.0);
            for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, box)) {
                golem.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.0);
            }
        }

        if (path.contains("warden.") || path.contains("sculk_shrieker")) {
            AABB box = new AABB(bp).inflate(24.0);
            for (Warden w : level.getEntitiesOfClass(Warden.class, box)) {
                w.setAttackTarget(null);
                w.getBrain().setMemoryWithExpiry(MemoryModuleType.DISTURBANCE_LOCATION, bp, 100L);
            }
        }

        if (path.contains(".ambient") || path.contains(".hurt") || path.contains(".Growl") || path.contains("wolf.bark") || path.contains("cat.hiss")) {
            reactMobsToCreatureSound(level, pos, bp, path);
        }
    }

    private static void reactMobsToCreatureSound(ServerLevel level, Vec3 pos, BlockPos bp, String path) {
        AABB box = new AABB(bp).inflate(16.0);
        if (path.contains("creeper.primed") || path.contains("ghast.scream") || path.contains("warden")) {
            for (Villager v : level.getEntitiesOfClass(Villager.class, box)) {
                Vec3 away = v.position().subtract(pos).normalize().scale(10.0).add(v.position());
                v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(away, 0.75f, 2));
            }
        }

        if (path.contains("wolf.")) {
            for (Monster m : level.getEntitiesOfClass(Monster.class, box, e -> e.getType().toShortString().contains("skeleton"))) {
                Vec3 away = m.position().subtract(pos).normalize().scale(8.0).add(m.position());
                m.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            }
        }

        if (path.contains("cat.")) {
            for (Monster m : level.getEntitiesOfClass(Monster.class, box, e -> e.getType().toShortString().contains("creeper"))) {
                Vec3 away = m.position().subtract(pos).normalize().scale(8.0).add(m.position());
                m.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            }
        }

        if (path.contains("zombie.") || path.contains("skeleton.") || path.contains("spider.") || path.contains("pillager.")) {
            List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(bp).inflate(20.0));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if ((e instanceof TamableAnimal ta && ta.isTame()) || e instanceof IronGolem) {
                    if (!monsters.isEmpty() && e instanceof NeutralMob nm && nm.getTarget() == null) {
                        nm.setTarget(monsters.get(0));
                    } else if (e instanceof IronGolem ig) {
                        ig.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.0);
                    }
                }
            }
        }
    }
}

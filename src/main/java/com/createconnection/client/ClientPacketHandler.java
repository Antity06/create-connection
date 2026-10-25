package com.createconnection.client;

import com.createconnection.config.ConnectionClientConfig;
import com.createconnection.network.MusicRelayPayload;
import com.createconnection.network.PhoneStatePayload;
import com.createconnection.network.RelaySoundPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public final class ClientPacketHandler {
    private static final Map<String, SoundInstance> playingMusic = new HashMap<>();

    private ClientPacketHandler() {}

    public static void onPhoneState(PhoneStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PhoneScreen ps && ps.getPos().equals(payload.pos())) {
            ps.update(payload);
        } else {
            mc.setScreen(new PhoneScreen(payload));
        }
    }

    public static void onRelaySound(RelaySoundPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(payload.sound());
        if (sound == null) {
            sound = SoundEvent.createVariableRangeEvent(payload.sound());
        }
        float vol = payload.volume() * ConnectionClientConfig.volume(payload.radio());
        if (vol <= 0.001f) return;

        double x = payload.x();
        double y = payload.y();
        double z = payload.z();
        SoundInstance.Attenuation att = SoundInstance.Attenuation.LINEAR;
        boolean relative = false;

        if (payload.kind() == 0) {
            x = 0.0;
            y = 0.0;
            z = 0.0;
            att = SoundInstance.Attenuation.NONE;
            relative = true;
        } else if (payload.kind() == 1) {
            Entity e = mc.level.getEntity(payload.entityId());
            if (e == null) return;
            x = e.getX();
            y = e.getEyeY();
            z = e.getZ();
        }

        SimpleSoundInstance inst = new SimpleSoundInstance(
            sound.getLocation(),
            SoundSource.MASTER,
            vol,
            payload.pitch(),
            RandomSource.create(),
            false,
            0,
            att,
            x,
            y,
            z,
            relative
        );
        mc.getSoundManager().play(inst);
    }

    public static void onMusicRelay(MusicRelayPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        SoundInstance old = playingMusic.remove(payload.streamKey());
        if (old != null) {
            mc.getSoundManager().stop(old);
        }
        if (!payload.start()) return;

        float vol = payload.volume() * ConnectionClientConfig.volume(payload.radio());
        if (vol <= 0.001f) return;

        SoundInstance inst;
        if (payload.kind() == 0) {
            inst = new SimpleSoundInstance(
                payload.sound(),
                SoundSource.RECORDS,
                vol,
                1.0f,
                RandomSource.create(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
            );
        } else if (payload.kind() == 1) {
            Entity e = mc.level.getEntity(payload.entityId());
            if (e == null) return;
            inst = new FollowingMusicInstance(payload.sound(), e, vol);
        } else {
            inst = new SimpleSoundInstance(
                payload.sound(),
                SoundSource.RECORDS,
                vol,
                1.0f,
                RandomSource.create(),
                false,
                0,
                SoundInstance.Attenuation.LINEAR,
                payload.x(),
                payload.y(),
                payload.z(),
                false
            );
        }
        playingMusic.put(payload.streamKey(), inst);
        mc.getSoundManager().play(inst);
    }

    private static final class FollowingMusicInstance extends AbstractTickableSoundInstance {
        private final Entity entity;

        FollowingMusicInstance(net.minecraft.resources.ResourceLocation sound, Entity entity, float volume) {
            super(SoundEvent.createVariableRangeEvent(sound), SoundSource.RECORDS, RandomSource.create());
            this.entity = entity;
            this.volume = volume;
            this.pitch = 1.0f;
            this.x = entity.getX();
            this.y = entity.getEyeY();
            this.z = entity.getZ();
        }

        @Override
        public void tick() {
            if (this.entity.isRemoved()) {
                this.stop();
            } else {
                this.x = this.entity.getX();
                this.y = this.entity.getEyeY();
                this.z = this.entity.getZ();
            }
        }
    }
}

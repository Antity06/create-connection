package com.createconnection.mixin;

import com.createconnection.compat.radar.RadarDishHelper;
import com.happysg.radar.block.radar.behavior.RadarScanningBlockBehavior;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import java.util.HashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RadarScanningBlockBehavior.class, remap = false)
public class RadarScanningBlockBehaviorMixin {
    @Shadow
    private boolean running;

    @Shadow
    private Vec3 scanPos;

    @Shadow
    private double range;

    @Shadow
    @Final
    private HashMap<String, RadarTrack> scannedTracks;

    @Inject(method = "tick", at = @At("TAIL"))
    private void createConnection$injectRadioTracks(CallbackInfo ci) {
        if (!this.running || this.scanPos == null) {
            return;
        }
        SmartBlockEntity be = ((RadarScanningBlockBehavior) (Object) this).blockEntity;
        if (be == null) {
            return;
        }
        Level level = be.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        this.scannedTracks.keySet().removeIf(k -> k.startsWith("cc_radio_") || k.startsWith("cc_station_"));
        for (RadarTrack t : RadarDishHelper.collectRadioTracks(level, this.scanPos, this.range)) {
            this.scannedTracks.put(t.id(), t);
        }
    }
}

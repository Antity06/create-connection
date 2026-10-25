package com.createconnection.mixin;

import com.createconnection.compat.radar.RadarDishHelper;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.track.RadarTrack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DetectionConfig.class, remap = false)
public class DetectionConfigMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void createConnection$alwaysAllowRadios(RadarTrack track, CallbackInfoReturnable<Boolean> cir) {
        String t = track.entityType();
        if (RadarDishHelper.RADIO_TX_TYPE.equals(t) || RadarDishHelper.RADIO_RX_TYPE.equals(t)) {
            cir.setReturnValue(true);
        }
    }
}

package com.createconnection.mixin;

import com.createconnection.compat.radar.RadarDishHelper;
import com.happysg.radar.block.radar.track.RadarTrack;
import net.createmod.catnip.theme.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RadarTrack.class, remap = false)
public abstract class RadarTrackColorMixin {
    @Shadow
    public abstract String entityType();

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private void createConnection$colorRadios(CallbackInfoReturnable<Color> cir) {
        String t = this.entityType();
        if (RadarDishHelper.RADIO_TX_TYPE.equals(t)) {
            cir.setReturnValue(new Color(0xFF3B30));
        } else if (RadarDishHelper.RADIO_RX_TYPE.equals(t)) {
            cir.setReturnValue(new Color(0xFF8C1A));
        }
    }
}

package com.createconnection.network;

import com.createconnection.client.ClientPacketHandler;
import com.createconnection.client.ClientRemoteStock;
import com.createconnection.morse.MorseSignalerBlockEntity;
import com.createconnection.morse.MorseTableBlockEntity;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.TelephoneBlockEntity;
import com.createconnection.radio.ITunable;
import com.createconnection.radio.PortableStationItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ConnectionNetwork {
    private static final String VERSION = "1";

    private ConnectionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(VERSION);
        reg.playToServer(PhoneActionPayload.TYPE, PhoneActionPayload.STREAM_CODEC, ConnectionNetwork::onPhoneAction);
        reg.playToServer(RadioConfigPayload.TYPE, RadioConfigPayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                if (payload.pos().getY() == -9999) {
                    ItemStack st = PortableStationItem.findInPlayer(sp);
                    if (!st.isEmpty()) {
                        PortableStationItem.configureTune(st, payload.tune(), payload.transmit());
                    }
                    return;
                }
                Level level = sp.level();
                if (!level.isLoaded(payload.pos()) || sp.position().distanceToSqr(payload.pos().getCenter()) > 64.0) return;
                if (level.getBlockEntity(payload.pos()) instanceof ITunable tunable) {
                    tunable.configureTune(payload.tune(), payload.transmit());
                }
            });
        });
        reg.playToServer(MorseSignalerPayload.TYPE, MorseSignalerPayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                Level level = sp.level();
                if (!level.isLoaded(payload.pos()) || sp.position().distanceToSqr(payload.pos().getCenter()) > 81.0) return;
                if (level.getBlockEntity(payload.pos()) instanceof MorseSignalerBlockEntity signaler) {
                    signaler.setTransmitting(sp, payload.active());
                }
            });
        });
        reg.playToServer(MorseTablePayload.TYPE, MorseTablePayload.STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer sp)) return;
                Level level = sp.level();
                if (!level.isLoaded(payload.pos()) || sp.position().distanceToSqr(payload.pos().getCenter()) > 64.0) return;
                if (level.getBlockEntity(payload.pos()) instanceof MorseTableBlockEntity table) {
                    table.handleAction(sp, payload.action(), payload.text());
                }
            });
        });
        reg.playToClient(PhoneStatePayload.TYPE, PhoneStatePayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> ClientPacketHandler.onPhoneState(payload)));
        reg.playToClient(RelaySoundPayload.TYPE, RelaySoundPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> ClientPacketHandler.onRelaySound(payload)));
        reg.playToClient(RemoteTickerPayload.TYPE, RemoteTickerPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> ClientRemoteStock.onPayload(payload)));
        reg.playToClient(MusicRelayPayload.TYPE, MusicRelayPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> ClientPacketHandler.onMusicRelay(payload)));
    }

    private static void onPhoneAction(PhoneActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            ServerLevel level = (ServerLevel) sp.level();
            if (!level.isLoaded(payload.pos()) || sp.position().distanceToSqr(payload.pos().getCenter()) > 64.0) return;
            if (!(level.getBlockEntity(payload.pos()) instanceof TelephoneBlockEntity phone)) return;
            CallManager cm = CallManager.get();
            switch (payload.action()) {
                case 0 -> cm.dial(sp, phone, payload.arg());
                case 1 -> cm.answer(sp, phone);
                case 2 -> cm.hangup(phone);
                case 3 -> cm.setNumber(sp, phone, payload.arg());
                case 4 -> cm.toggleSpeaker(sp, phone);
                case 5 -> cm.closeScreen(sp, phone);
            }
        });
    }
}

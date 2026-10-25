package com.createconnection;

import com.createconnection.audio.MusicRelay;
import com.createconnection.audio.SoundRelay;
import com.createconnection.compat.create.RemoteStockKeeper;
import com.createconnection.config.ConnectionConfig;
import com.createconnection.gametest.ConnectionGameTests;
import com.createconnection.network.ConnectionNetwork;
import com.createconnection.phone.CallManager;
import com.createconnection.phone.PhoneLineTracer;
import com.createconnection.radio.RadioDataLinkHandler;
import com.createconnection.radio.RadioRegistry;
import com.createconnection.registry.TBlockEntities;
import com.createconnection.registry.TBlocks;
import com.createconnection.registry.TCreativeTab;
import com.createconnection.registry.TDataComponents;
import com.createconnection.registry.TItems;
import com.createconnection.registry.TSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(CreateConnection.MOD_ID)
public class CreateConnection {
    public static final String MOD_ID = "create_connection";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateConnection(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ConnectionConfig.SPEC);
        TBlocks.BLOCKS.register(modBus);
        TItems.ITEMS.register(modBus);
        TBlockEntities.BLOCK_ENTITIES.register(modBus);
        TSounds.SOUNDS.register(modBus);
        TDataComponents.COMPONENTS.register(modBus);
        TCreativeTab.TABS.register(modBus);

        modBus.addListener(ConnectionNetwork::register);
        modBus.addListener((RegisterGameTestsEvent e) -> e.register(ConnectionGameTests.class));

        NeoForge.EVENT_BUS.addListener(CreateConnection::onServerTick);
        NeoForge.EVENT_BUS.addListener(CreateConnection::onServerStopped);
        NeoForge.EVENT_BUS.addListener(PhoneLineTracer::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(PhoneLineTracer::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(RadioDataLinkHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(SoundRelay::onSoundAtPosition);
        NeoForge.EVENT_BUS.addListener(SoundRelay::onSoundAtEntity);
        NeoForge.EVENT_BUS.addListener(SoundRelay::onVanillaGameEvent);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> DevCommands.register(e.getDispatcher()));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        CallManager.get().tick(event.getServer());
        RadioRegistry.tickRadios(event.getServer());
        RemoteStockKeeper.tick(event.getServer());
        MusicRelay.tick(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        CallManager.get().clear();
        PhoneLineTracer.invalidate();
        RadioRegistry.clear();
        RemoteStockKeeper.clear();
        MusicRelay.clear();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

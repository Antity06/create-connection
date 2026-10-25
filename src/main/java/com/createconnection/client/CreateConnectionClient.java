package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.client.ponder.ConnectionPonderPlugin;
import com.createconnection.config.ConnectionClientConfig;
import com.createconnection.radio.PortableStationItem;
import com.createconnection.radio.WalkieTalkieItem;
import com.createconnection.registry.TBlockEntities;
import com.createconnection.registry.TItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(value = CreateConnection.MOD_ID, dist = Dist.CLIENT)
public class CreateConnectionClient {
    public CreateConnectionClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ConnectionClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(CreateConnectionClient::clientSetup);
        PonderIndex.addPlugin(new ConnectionPonderPlugin());
        modBus.addListener(PhoneColors::blocks);
        modBus.addListener(PhoneColors::items);
        modBus.addListener(PhoneBellRenderer::registerModels);
        modBus.addListener(PortableStationLayer::registerModels);
        modBus.addListener(PortableStationLayer::addLayers);
        modBus.addListener(CreateConnectionClient::clientExtensions);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
            e.registerBlockEntityRenderer(TBlockEntities.TELEPHONE.get(), PhoneBellRenderer::new));

        NeoForge.EVENT_BUS.addListener(ClientHooks::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientHooks::onInteractionTriggered);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredHolder<Item, ? extends Item> entry : TItems.ITEMS.getEntries()) {
                Item item = entry.get();
                if (item == TItems.SPEAKER.get() || item == TItems.MICROPHONE.get()) {
                    continue;
                }
                TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
            }
        });
        event.enqueueWork(() -> {
            ItemProperties.register(
                TItems.WALKIE_TALKIE.get(),
                CreateConnection.id("active"),
                (stack, level, entity, seed) -> WalkieTalkieItem.isActive(stack) ? 1.0f : 0.0f
            );
            ItemProperties.register(
                TItems.PORTABLE_STATION.get(),
                CreateConnection.id("active"),
                (stack, level, entity, seed) -> PortableStationItem.isActive(stack) ? 1.0f : 0.0f
            );
        });
    }

    private static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
                if (!player.isUsingItem() || player.getUseItem() != stack) {
                    return false;
                }
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                float t = Math.min(1.0f, ((float) player.getTicksUsingItem() + partialTick) / 4.0f);
                pose.translate((float) side * (0.56f - 0.26f * t), -0.52f + 0.16f * t, -0.72f + 0.1f * t);
                pose.mulPose(Axis.YP.rotationDegrees((float) side * -30.0f * t));
                return true;
            }
        }, TItems.MEGAPHONE.get());

        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
                if (!player.isUsingItem() || player.getUseItem() != stack) {
                    return false;
                }
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                float t = Math.min(1.0f, ((float) player.getTicksUsingItem() + partialTick) / 5.0f);
                t = t * t * (3.0f - 2.0f * t);
                pose.translate((float) side * (0.56f - 0.4f * t), -0.52f + 0.05f * t, -0.72f + 0.05f * t);
                pose.mulPose(Axis.YP.rotationDegrees((float) side * 15.0f * t));
                pose.mulPose(Axis.XP.rotationDegrees(40.0f * t));
                return true;
            }
        }, TItems.HAND_MICROPHONE.get());
    }
}

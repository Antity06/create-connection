package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.radio.PortableStationItem;
import com.createconnection.registry.TBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

public class PortableStationLayer extends RenderLayer<Entity, EntityModel<Entity>> {
    public static final ModelResourceLocation BACKPACK_MODEL =
        ModelResourceLocation.standalone(CreateConnection.id("block/portable_station_backpack"));
    public static final ModelResourceLocation BACKPACK_MODEL_ON =
        ModelResourceLocation.standalone(CreateConnection.id("block/portable_station_backpack_on"));

    public PortableStationLayer(RenderLayerParent<Entity, EntityModel<Entity>> renderer) {
        super(renderer);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BACKPACK_MODEL);
        event.register(BACKPACK_MODEL_ON);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attachToRenderer(Object rendererObj, Set<Object> attached) {
        if (!(rendererObj instanceof RenderLayerParent parent) || !attached.add(rendererObj)) {
            return;
        }
        try {
            boolean done = false;
            for (Method m : rendererObj.getClass().getMethods()) {
                if (m.getName().equals("addLayer") && m.getParameterCount() == 1) {
                    m.invoke(rendererObj, new PortableStationLayer(parent));
                    done = true;
                    break;
                }
            }
            if (!done) {
                Class<?> c = rendererObj.getClass();
                while (c != null && !done) {
                    for (Method m : c.getDeclaredMethods()) {
                        if (m.getName().equals("addLayer") && m.getParameterCount() == 1) {
                            m.setAccessible(true);
                            m.invoke(rendererObj, new PortableStationLayer(parent));
                            done = true;
                            break;
                        }
                    }
                    c = c.getSuperclass();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        Set<Object> attached = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            Method getSkins = event.getClass().getMethod("getSkins");
            Iterable<?> skins = (Iterable<?>) getSkins.invoke(event);
            Method getSkin = null;
            for (Method m : event.getClass().getMethods()) {
                if (m.getName().equals("getSkin") && m.getParameterCount() == 1) {
                    getSkin = m;
                    break;
                }
            }
            if (getSkin != null && skins != null) {
                for (Object skin : skins) {
                    Object r = getSkin.invoke(event, skin);
                    attachToRenderer(r, attached);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Method getRenderer = null;
            for (Method m : event.getClass().getMethods()) {
                if (m.getName().equals("getRenderer") && m.getParameterCount() == 1) {
                    getRenderer = m;
                    break;
                }
            }
            if (getRenderer != null) {
                try {
                    Object armorStandRenderer = getRenderer.invoke(event, EntityType.ARMOR_STAND);
                    attachToRenderer(armorStandRenderer, attached);
                } catch (Throwable ignored) {
                }
                try {
                    Method getEntityTypes = event.getClass().getMethod("getEntityTypes");
                    Iterable<?> types = (Iterable<?>) getEntityTypes.invoke(event);
                    if (types != null) {
                        for (Object type : types) {
                            Object r = getRenderer.invoke(event, type);
                            if (r instanceof RenderLayerParent parent && parent.getModel() instanceof HumanoidModel) {
                                attachToRenderer(r, attached);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> c = event.getClass();
            while (c != null && c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object mapObj = f.get(event);
                        if (mapObj instanceof Map<?, ?> map) {
                            for (Object r : map.values()) {
                                if (r instanceof RenderLayerParent parent && parent.getModel() instanceof HumanoidModel) {
                                    attachToRenderer(r, attached);
                                }
                            }
                        }
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void render(
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        if (chest == null || !(chest.getItem() instanceof PortableStationItem)) {
            return;
        }
        boolean active = PortableStationItem.isActive(chest);
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(active ? BACKPACK_MODEL_ON : BACKPACK_MODEL);
        if (model == null) {
            return;
        }
        pose.pushPose();
        EntityModel<Entity> parentModel = getParentModel();
        if (parentModel != null && parentModel.young) {
            pose.translate(0.0f, 0.75f, 0.0f);
            pose.scale(0.5f, 0.5f, 0.5f);
        }
        if (parentModel instanceof HumanoidModel<?> humanoid && humanoid.body != null) {
            humanoid.body.translateAndRotate(pose);
        }
        pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        pose.translate(-0.5f, -0.75f, -0.5f);
        mc.getBlockRenderer().getModelRenderer().renderModel(
            pose.last(),
            buffer.getBuffer(RenderType.cutout()),
            TBlocks.RADIO_STATION.get().defaultBlockState(),
            model,
            1.0f,
            1.0f,
            1.0f,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.cutout()
        );
        pose.popPose();
    }
}

package com.createconnection.client;

import com.createconnection.CreateConnection;
import com.createconnection.phone.AbstractPhoneBlock;
import com.createconnection.phone.TelephoneBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

public class PhoneBellRenderer implements BlockEntityRenderer<TelephoneBlockEntity> {
    public static final ModelResourceLocation CLAPPER_MODEL =
        ModelResourceLocation.standalone(CreateConnection.id("block/phone_clapper"));

    public PhoneBellRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(CLAPPER_MODEL);
    }

    @Override
    public void render(
        TelephoneBlockEntity be,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(AbstractPhoneBlock.RINGING) || !state.getValue(AbstractPhoneBlock.RINGING)) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float time = (float) level.getGameTime() + partialTick;
        float swing = Mth.sin(time * 1.9f) * 18.0f;
        Direction facing = state.getValue(AbstractPhoneBlock.FACING);

        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0.0, 0.5625, -0.1875);
        pose.mulPose(Axis.ZP.rotationDegrees(swing));
        pose.translate(-0.5, -0.5625, 0.1875);

        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(CLAPPER_MODEL);
        mc.getBlockRenderer().getModelRenderer().renderModel(
            pose.last(),
            buffer.getBuffer(RenderType.cutout()),
            state,
            model,
            1.0f,
            1.0f,
            1.0f,
            packedLight,
            packedOverlay,
            ModelData.EMPTY,
            RenderType.cutout()
        );
        pose.popPose();
    }
}

package com.createconnection.phone;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HomePhoneBlock extends AbstractPhoneBlock {
    public static final MapCodec<HomePhoneBlock> CODEC = simpleCodec(HomePhoneBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0),
        Block.box(2.0, 6.0, 5.0, 14.0, 9.0, 11.0)
    );

    public HomePhoneBlock(BlockBehaviour.Properties props) {
        super(props, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}

package com.createconnection.radio;

import com.createconnection.registry.TBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MicrophoneStandBlock extends HorizontalDirectionalBlock implements IBE<MicrophoneStandBlockEntity> {
    public static final BooleanProperty HAS_MIC = BooleanProperty.create("has_mic");

    private static final VoxelShape STAND = Shapes.or(
        Block.box(7.0, 0.0, 7.0, 9.0, 10.5, 9.0),
        Block.box(6.0, 10.5, 6.0, 10.0, 14.0, 10.0)
    );
    private static final VoxelShape WITH_MIC = Shapes.or(
        STAND,
        Block.box(6.0, 13.5, 6.0, 10.0, 24.0, 10.0)
    );

    public MicrophoneStandBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_MIC, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(MicrophoneStandBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HAS_MIC);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (!(stack.getItem() instanceof HandMicrophoneItem) || state.getValue(HAS_MIC)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MicrophoneStandBlockEntity be) {
            be.setMic(stack.split(1));
            level.setBlock(pos, state.setValue(HAS_MIC, true), 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!state.getValue(HAS_MIC)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MicrophoneStandBlockEntity be) {
            ItemStack mic = be.takeMic();
            level.setBlock(pos, state.setValue(HAS_MIC, false), 3);
            if (!mic.isEmpty() && !player.addItem(mic)) {
                popResource(level, pos, mic);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MicrophoneStandBlockEntity be) {
            ItemStack mic = be.takeMic();
            if (!mic.isEmpty()) {
                popResource(level, pos, mic);
            }
        }
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HAS_MIC) ? WITH_MIC : STAND;
    }

    @Override
    public Class<MicrophoneStandBlockEntity> getBlockEntityClass() {
        return MicrophoneStandBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MicrophoneStandBlockEntity> getBlockEntityType() {
        return TBlockEntities.MICROPHONE_STAND.get();
    }
}

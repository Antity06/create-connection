package com.createconnection.radio;

import com.createconnection.registry.TBlockEntities;
import com.createconnection.registry.TItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RadioAudioBlock extends HorizontalDirectionalBlock implements IBE<RadioAudioBlockEntity>, IWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty MEGAPHONE = BooleanProperty.create("megaphone");
    public static final MapCodec<RadioAudioBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        propertiesCodec(),
        Codec.BOOL.fieldOf("speaker").forGetter(b -> b.speaker)
    ).apply(i, RadioAudioBlock::new));

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);
    private final boolean speaker;

    public RadioAudioBlock(BlockBehaviour.Properties props, boolean speaker) {
        super(props);
        this.speaker = speaker;
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(MEGAPHONE, false));
    }

    public boolean isSpeaker() {
        return this.speaker;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, MEGAPHONE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean pow = ctx.getLevel().hasNeighborSignal(ctx.getClickedPos());
        return defaultBlockState()
            .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
            .setValue(POWERED, pow);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean pow = level.hasNeighborSignal(pos);
            if (pow != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, pow), 3);
            }
        }
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
        if (this.speaker && stack.getItem() instanceof HandMicrophoneItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (this.speaker && stack.is(TItems.MEGAPHONE.get()) && !state.getValue(MEGAPHONE)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(MEGAPHONE, true), 3);
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.4f, 1.5f);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (this.speaker && player.isShiftKeyDown() && state.getValue(MEGAPHONE)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(MEGAPHONE, false), 3);
                ItemStack drop = new ItemStack(TItems.MEGAPHONE.get());
                if (!player.addItem(drop)) {
                    popResource(level, pos, drop);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof RadioAudioBlockEntity be) {
            int next = (be.getFrequency() + (player.isShiftKeyDown() ? 15 : 1)) & 15;
            if (!level.isClientSide) {
                be.setFrequency(next);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 0.9f + (float) next * 0.04f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && state.getValue(MEGAPHONE) && !level.isClientSide) {
            popResource(level, pos, new ItemStack(TItems.MEGAPHONE.get()));
        }
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public Class<RadioAudioBlockEntity> getBlockEntityClass() {
        return RadioAudioBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RadioAudioBlockEntity> getBlockEntityType() {
        return TBlockEntities.RADIO_AUDIO.get();
    }
}

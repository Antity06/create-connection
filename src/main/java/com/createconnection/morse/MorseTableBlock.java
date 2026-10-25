package com.createconnection.morse;

import com.createconnection.client.ClientHooks;
import com.createconnection.registry.TBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class MorseTableBlock extends HorizontalDirectionalBlock implements IBE<MorseTableBlockEntity> {
    public static final MapCodec<MorseTableBlock> CODEC = simpleCodec(MorseTableBlock::new);
    public static final BooleanProperty HAS_CARD = BooleanProperty.create("has_card");
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1.0, 0.0, 1.0, 15.0, 10.0, 15.0),
        Block.box(0.0, 10.0, 0.0, 16.0, 14.0, 16.0)
    );

    public MorseTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_CARD, false)
            .setValue(PLAYING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_CARD, PLAYING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MorseTableBlockEntity table) {
            if (stack.is(Items.PAPER) && !table.hasPaper() && table.getCard().isEmpty()) {
                if (!level.isClientSide) {
                    stack.shrink(1);
                    table.setPaper(true);
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.9f, 1.1f);
                } else {
                    table.setPaperClientPreview();
                    ClientHooks.openMorseTable(pos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stack.getItem() instanceof PunchCardItem && table.getCard().isEmpty() && !table.hasPaper()) {
                if (!level.isClientSide) {
                    table.setCard(stack.split(1));
                    level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.9f, 1.0f);
                } else {
                    table.setCardClientPreview(stack.copyWithCount(1));
                    ClientHooks.openMorseTable(pos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MorseTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (!table.hasPaper() && table.getCard().isEmpty()) {
            player.displayClientMessage(
                Component.translatable("create_connection.gui.morse_table.status_empty").withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (table.isCardRecorded()) {
                    table.togglePlay();
                } else {
                    player.displayClientMessage(
                        Component.translatable("create_connection.gui.morse_table.status_empty").withStyle(ChatFormatting.RED),
                        true
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            ClientHooks.openMorseTable(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof MorseTableBlockEntity table && table.isToneOn()) {
            return 15;
        }
        return state.getValue(PLAYING) ? 2 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MorseTableBlockEntity table) {
            ItemStack dropped = table.takeItemForDrop();
            if (!dropped.isEmpty()) {
                popResource(level, pos, dropped);
            }
        }
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public Class<MorseTableBlockEntity> getBlockEntityClass() {
        return MorseTableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MorseTableBlockEntity> getBlockEntityType() {
        return TBlockEntities.MORSE_TABLE.get();
    }
}

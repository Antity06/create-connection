package com.createconnection.phone;

import com.createconnection.registry.TBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractPhoneBlock extends HorizontalDirectionalBlock implements IBE<TelephoneBlockEntity>, IWrenchable {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    public static final BooleanProperty OFF_HOOK = BooleanProperty.create("off_hook");
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);

    private final boolean payphone;

    protected AbstractPhoneBlock(BlockBehaviour.Properties props, boolean payphone) {
        super(props);
        this.payphone = payphone;
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(RINGING, false)
            .setValue(OFF_HOOK, false)
            .setValue(COLOR, 0));
    }

    public boolean isPayphone() {
        return this.payphone;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RINGING, OFF_HOOK, COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int col = 0;
        BlockItemStateProperties saved = ctx.getItemInHand().get(DataComponents.BLOCK_STATE);
        if (saved != null) {
            Integer c = saved.get(COLOR);
            if (c != null) {
                col = c;
            }
        }
        return defaultBlockState()
            .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
            .setValue(COLOR, col);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = new ItemStack(this);
        int col = state.getValue(COLOR);
        if (col != 0) {
            drop.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(COLOR, col));
        }
        return Collections.singletonList(drop);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        int col = state.getValue(COLOR);
        if (col != 0) {
            stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(COLOR, col));
        }
        return stack;
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
        if (stack.getItem() instanceof DyeItem dye) {
            int target = dye.getDyeColor().getId() + 1;
            if (state.getValue(COLOR) != target) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(COLOR, target), 3);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (stack.is(Items.SPONGE) || stack.is(Items.WET_SPONGE)) {
            if (state.getValue(COLOR) != 0) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(COLOR, 0), 3);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof TelephoneBlockEntity phone) {
            CallManager.get().onPlayerInteract(sp, phone);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof TelephoneBlockEntity phone) {
                CallManager.get().onPhoneRemoved(sl, phone);
            }
            IBE.onRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(RINGING)) {
            return 15;
        }
        if (level.getBlockEntity(pos) instanceof TelephoneBlockEntity phone) {
            return phone.getComparatorOutput();
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return state.getValue(RINGING) ? 15 : 0;
    }

    @Override
    public Class<TelephoneBlockEntity> getBlockEntityClass() {
        return TelephoneBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TelephoneBlockEntity> getBlockEntityType() {
        return TBlockEntities.TELEPHONE.get();
    }
}

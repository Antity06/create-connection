package com.createconnection.client;

import com.createconnection.phone.AbstractPhoneBlock;
import com.createconnection.radio.WalkieTalkieItem;
import com.createconnection.registry.TBlocks;
import com.createconnection.registry.TItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class PhoneColors {
    private static final int BAKELITE = -12239044;
    private static final int BRASS = -15022;

    private PhoneColors() {}

    static int color(Block block, int c, int tintIndex) {
        int dyed = c > 0 ? (DyeColor.byId(c - 1).getTextureDiffuseColor() | 0xFF000000) : 0;
        if (block == TBlocks.PAYPHONE.get()) {
            if (tintIndex == 1) {
                return dyed != 0 ? dyed : BRASS;
            }
            return BAKELITE;
        }
        if (tintIndex == 0) {
            return dyed != 0 ? dyed : BAKELITE;
        }
        return -1;
    }

    static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, level, pos, tintIndex) -> color(state.getBlock(), colorOf(state), tintIndex),
            TBlocks.HOME_PHONE.get(),
            TBlocks.PAYPHONE.get()
        );
    }

    static void items(RegisterColorHandlersEvent.Item event) {
        event.register(
            (stack, tintIndex) -> color(Block.byItem(stack.getItem()), colorOf(stack), tintIndex),
            TBlocks.HOME_PHONE.get(),
            TBlocks.PAYPHONE.get()
        );
        event.register(
            (stack, tintIndex) -> tintIndex == 0 ? WalkieTalkieItem.getTintColor(stack) : -1,
            TItems.WALKIE_TALKIE.get()
        );
    }

    private static int colorOf(BlockState state) {
        return state.hasProperty(AbstractPhoneBlock.COLOR) ? state.getValue(AbstractPhoneBlock.COLOR) : 0;
    }

    private static int colorOf(ItemStack stack) {
        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Integer c = props.get(AbstractPhoneBlock.COLOR);
        return c == null ? 0 : c;
    }
}

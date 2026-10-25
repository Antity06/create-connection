package com.createconnection.registry;

import com.createconnection.CreateConnection;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateConnection.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_connection"))
            .icon(() -> new ItemStack(TBlocks.HOME_PHONE.get()))
            .displayItems((params, out) -> {
                for (DeferredHolder<Item, ? extends Item> item : TItems.ITEMS.getEntries()) {
                    out.accept((ItemLike) item.get());
                }
            })
            .build()
    );

    private TCreativeTab() {}
}

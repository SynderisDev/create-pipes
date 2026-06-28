package com.synderisdev.createpipes.registry;

import com.synderisdev.createpipes.CreatePipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreatePipes.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_pipes"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> ModItems.STEAM_ITEM_PIPE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.STEAM_ITEM_PIPE.get());
                        output.accept(ModItems.NETWORK_CONTROL_TOOL.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}

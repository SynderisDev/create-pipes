package com.synderisdev.createpipes.registry;

import com.synderisdev.createpipes.CreatePipes;
import com.synderisdev.createpipes.content.tool.NetworkControlToolItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePipes.MOD_ID);

    public static final DeferredItem<?> STEAM_ITEM_PIPE = ITEMS.registerSimpleBlockItem("steam_item_pipe", ModBlocks.STEAM_ITEM_PIPE);
    public static final DeferredItem<NetworkControlToolItem> NETWORK_CONTROL_TOOL = ITEMS.registerItem(
            "network_control_tool",
            NetworkControlToolItem::new,
            new Item.Properties().stacksTo(1));

    private ModItems() {
    }
}

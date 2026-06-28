package com.synderisdev.createpipes.content.pipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

final class ItemTransferHelper {
    private ItemTransferHelper() {
    }

    static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    static int insertableAmount(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = insert(handler, stack, true);
        return stack.getCount() - remaining.getCount();
    }
}

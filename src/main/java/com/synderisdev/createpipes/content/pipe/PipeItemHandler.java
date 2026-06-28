package com.synderisdev.createpipes.content.pipe;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PipeItemHandler implements IItemHandler {
    private final SteamItemPipeBlockEntity pipe;
    @Nullable
    private final Direction side;

    PipeItemHandler(SteamItemPipeBlockEntity pipe, @Nullable Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }
        if (side != null && !PipeModeRules.acceptsExternalInsertion(pipe.getMode(side))) {
            return stack;
        }
        return pipe.routeExternalInsert(stack, side, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == 0;
    }
}

package com.synderisdev.createpipes.content.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

final class AdjacentItemHandlers {
    private AdjacentItemHandlers() {
    }

    @Nullable
    static IItemHandler get(ServerLevel level, BlockPos inventoryPos, Direction accessSide) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, inventoryPos, accessSide);
        if (handler != null) {
            return handler;
        }

        BlockEntity blockEntity = level.getBlockEntity(inventoryPos);
        if (blockEntity instanceof WorldlyContainer worldlyContainer) {
            return new SidedInvWrapper(worldlyContainer, accessSide);
        }
        if (blockEntity instanceof Container container) {
            return new InvWrapper(container);
        }
        return null;
    }
}

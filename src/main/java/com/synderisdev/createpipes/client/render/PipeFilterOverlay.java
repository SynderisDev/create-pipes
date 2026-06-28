package com.synderisdev.createpipes.client.render;

import com.simibubi.create.CreateClient;
import com.synderisdev.createpipes.content.pipe.PipeConnectionMode;
import com.synderisdev.createpipes.content.pipe.PipeTargeting;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlock;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PipeFilterOverlay {
    private static final TagKey<Item> WRENCHES =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    @Nullable
    private static HoveredFilterSlot hoveredSlot;

    private PipeFilterOverlay() {
    }

    public static void tick(ClientTickEvent.Post event) {
        hoveredSlot = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isShiftKeyDown()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof SteamItemPipeBlock)
                || !(minecraft.level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
            return;
        }

        Vec3 localHit = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        Direction side = PipeTargeting.sideFromHit(localHit.x, localHit.y, localHit.z, hitResult.getDirection());
        if (!state.getValue(SteamItemPipeBlock.propertyFor(side))) {
            return;
        }
        Direction displayFace = PipeFilterSlotTransform.displayFaceForHit(side, hitResult.getDirection());

        ItemStack heldStack = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldStack.is(WRENCHES)) {
            return;
        }
        ItemStack filter = pipe.getFilterItem(side);
        if (!shouldShowSlot(pipe, side, heldStack, filter)) {
            return;
        }

        hoveredSlot = new HoveredFilterSlot(pos, side, displayFace);

        List<MutableComponent> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("tooltip.create_pipes.pipe.filter"));
        tooltip.add(Component.translatable(filter.isEmpty()
                ? "tooltip.create_pipes.pipe.filter_click_to_set"
                : "tooltip.create_pipes.pipe.filter_click_to_replace"));
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tooltip);
    }

    private static boolean shouldShowSlot(SteamItemPipeBlockEntity pipe, Direction side, ItemStack heldStack,
                                          ItemStack filter) {
        if (!pipe.isFilterableEndpoint(side)) {
            return false;
        }
        if (!filter.isEmpty() || !heldStack.isEmpty()) {
            return true;
        }
        PipeConnectionMode mode = pipe.getMode(side);
        return mode == PipeConnectionMode.PULL || mode == PipeConnectionMode.PUSH;
    }

    public static boolean isHovering(BlockPos pos) {
        return hoveredSlot != null && hoveredSlot.pos.equals(pos);
    }

    public static boolean isHovered(BlockPos pos, Direction side, Direction displayFace) {
        return hoveredSlot != null
                && hoveredSlot.pos.equals(pos)
                && hoveredSlot.side == side
                && hoveredSlot.displayFace == displayFace;
    }

    private record HoveredFilterSlot(BlockPos pos, Direction side, Direction displayFace) {
    }
}

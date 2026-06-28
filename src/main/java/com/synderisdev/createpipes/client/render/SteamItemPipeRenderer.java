package com.synderisdev.createpipes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.synderisdev.createpipes.config.CreatePipesClientConfig;
import com.synderisdev.createpipes.content.pipe.PipeConnectionMode;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class SteamItemPipeRenderer implements BlockEntityRenderer<SteamItemPipeBlockEntity> {
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final float PULL_R = 0.1F;
    private static final float PULL_G = 0.55F;
    private static final float PULL_B = 1.0F;

    private static final float PUSH_R = 1.0F;
    private static final float PUSH_G = 0.45F;
    private static final float PUSH_B = 0.05F;

    private static final float ALPHA = 0.9F;
    private static final float FILTER_CORNER_R = 1.0F;
    private static final float FILTER_CORNER_G = 1.0F;
    private static final float FILTER_CORNER_B = 1.0F;
    private static final float FILTER_CORNER_ALPHA = 0.8F;

    public SteamItemPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SteamItemPipeBlockEntity pipe, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int markerMask = pipe.getRenderMarkerMask();
        int filterMask = pipe.getRenderFilterMask();
        boolean renderMarkers = CreatePipesClientConfig.SHOW_PIPE_MODE_MARKERS.get() && markerMask != 0;
        boolean renderHoveredFilterSlot = PipeFilterOverlay.isHovering(pipe.getBlockPos());
        if (!renderMarkers && !renderHoveredFilterSlot && filterMask == 0) {
            return;
        }

        if (renderMarkers) {
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugFilledBox());
            for (Direction direction : DIRECTIONS) {
                if ((markerMask & (1 << direction.ordinal())) == 0) {
                    continue;
                }

                PipeConnectionMode mode = pipe.getMode(direction);
                if (mode == PipeConnectionMode.PULL) {
                    renderMarker(poseStack, buffer, direction, PULL_R, PULL_G, PULL_B);
                } else if (mode == PipeConnectionMode.PUSH) {
                    renderMarker(poseStack, buffer, direction, PUSH_R, PUSH_G, PUSH_B);
                }
            }
        }

        if (renderHoveredFilterSlot) {
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugFilledBox());
            for (Direction direction : DIRECTIONS) {
                for (Direction displayFace : PipeFilterSlotTransform.displayFacesFor(direction)) {
                    if (PipeFilterOverlay.isHovered(pipe.getBlockPos(), direction, displayFace)) {
                        renderFilterSlot(pipe, poseStack, buffer, direction, displayFace);
                    }
                }
            }
        }

        for (Direction direction : DIRECTIONS) {
            if ((filterMask & (1 << direction.ordinal())) != 0) {
                for (Direction displayFace : PipeFilterSlotTransform.displayFacesFor(direction)) {
                    renderFilter(pipe, poseStack, bufferSource, packedLight, packedOverlay, direction, displayFace);
                }
            }
        }
    }

    @Override
    public boolean shouldRender(SteamItemPipeBlockEntity pipe, Vec3 cameraPos) {
        boolean hasMarkers = CreatePipesClientConfig.SHOW_PIPE_MODE_MARKERS.get() && pipe.getRenderMarkerMask() != 0;
        boolean hasHoveredFilterSlot = PipeFilterOverlay.isHovering(pipe.getBlockPos());
        boolean hasFilters = pipe.getRenderFilterMask() != 0;
        if (!hasMarkers && !hasHoveredFilterSlot && !hasFilters) {
            return false;
        }

        double maxDistance = CreatePipesClientConfig.PIPE_MODE_MARKER_RENDER_DISTANCE.get();
        double dx = pipe.getBlockPos().getX() + 0.5D - cameraPos.x;
        double dy = pipe.getBlockPos().getY() + 0.5D - cameraPos.y;
        double dz = pipe.getBlockPos().getZ() + 0.5D - cameraPos.z;
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    @Override
    public int getViewDistance() {
        return CreatePipesClientConfig.PIPE_MODE_MARKER_RENDER_DISTANCE.get();
    }

    private void renderFilterSlot(SteamItemPipeBlockEntity pipe, PoseStack poseStack, VertexConsumer buffer,
                                  Direction direction, Direction displayFace) {
        poseStack.pushPose();
        PipeFilterSlotTransform.forSideAndFace(direction, displayFace)
                .transform(pipe.getLevel(), pipe.getBlockPos(), pipe.getBlockState(), poseStack);
        renderFilterSlotCorners(poseStack, buffer);
        poseStack.popPose();
    }

    private static void renderFilterSlotCorners(PoseStack poseStack, VertexConsumer buffer) {
        double half = 0.39D;
        double length = 0.15D;
        double thickness = 0.045D;
        double minZ = -0.295D;
        double maxZ = -0.255D;

        renderCorner(poseStack, buffer, -half, half - thickness, -half + length, half, -half, half - length, -half + thickness, half, minZ, maxZ);
        renderCorner(poseStack, buffer, half - length, half - thickness, half, half, half - thickness, half - length, half, half, minZ, maxZ);
        renderCorner(poseStack, buffer, -half, -half, -half + length, -half + thickness, -half, -half, -half + thickness, -half + length, minZ, maxZ);
        renderCorner(poseStack, buffer, half - length, -half, half, -half + thickness, half - thickness, -half, half, -half + length, minZ, maxZ);
    }

    private static void renderCorner(PoseStack poseStack, VertexConsumer buffer,
                                     double minHorizontalX, double minHorizontalY,
                                     double maxHorizontalX, double maxHorizontalY,
                                     double minVerticalX, double minVerticalY,
                                     double maxVerticalX, double maxVerticalY,
                                     double minZ, double maxZ) {
        box(poseStack, buffer, minHorizontalX, minHorizontalY, minZ, maxHorizontalX, maxHorizontalY, maxZ,
                FILTER_CORNER_R, FILTER_CORNER_G, FILTER_CORNER_B, FILTER_CORNER_ALPHA);
        box(poseStack, buffer, minVerticalX, minVerticalY, minZ, maxVerticalX, maxVerticalY, maxZ,
                FILTER_CORNER_R, FILTER_CORNER_G, FILTER_CORNER_B, FILTER_CORNER_ALPHA);
    }

    private void renderFilter(SteamItemPipeBlockEntity pipe, PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay, Direction direction, Direction displayFace) {
        ItemStack filter = pipe.getFilterItem(direction);
        if (filter.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        PipeFilterSlotTransform.forSideAndFace(direction, displayFace)
                .transform(pipe.getLevel(), pipe.getBlockPos(), pipe.getBlockState(), poseStack);
        ValueBoxRenderer.renderItemIntoValueBox(filter, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMarker(PoseStack poseStack, VertexConsumer buffer, Direction direction,
                                     float red, float green, float blue) {
        double min = 5.0D / 16.0D;
        double max = 11.0D / 16.0D;
        double bandMin = 1.75D / 16.0D;
        double bandMax = 4.25D / 16.0D;

        switch (direction) {
            case NORTH -> box(poseStack, buffer, min, min, bandMin, max, max, bandMax, red, green, blue);
            case SOUTH -> box(poseStack, buffer, min, min, 1.0D - bandMax, max, max, 1.0D - bandMin, red, green, blue);
            case WEST -> box(poseStack, buffer, bandMin, min, min, bandMax, max, max, red, green, blue);
            case EAST -> box(poseStack, buffer, 1.0D - bandMax, min, min, 1.0D - bandMin, max, max, red, green, blue);
            case DOWN -> box(poseStack, buffer, min, bandMin, min, max, bandMax, max, red, green, blue);
            case UP -> box(poseStack, buffer, min, 1.0D - bandMax, min, max, 1.0D - bandMin, max, red, green, blue);
        }
    }

    private static void box(PoseStack poseStack, VertexConsumer buffer,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ,
                            float red, float green, float blue) {
        box(poseStack, buffer, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, ALPHA);
    }

    private static void box(PoseStack poseStack, VertexConsumer buffer,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ,
                            float red, float green, float blue, float alpha) {
        LevelRenderer.addChainedFilledBoxVertices(poseStack, buffer, minX, minY, minZ, maxX, maxY, maxZ,
                red, green, blue, alpha);
    }
}

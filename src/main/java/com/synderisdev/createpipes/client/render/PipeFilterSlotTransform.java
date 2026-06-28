package com.synderisdev.createpipes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.synderisdev.createpipes.content.pipe.PipeTargeting;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.Map;

public final class PipeFilterSlotTransform extends ValueBoxTransform {
    private static final Direction[] X_AXIS_FACES = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
    private static final Direction[] Y_AXIS_FACES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final Direction[] Z_AXIS_FACES = {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
    private static final Map<Direction, Map<Direction, PipeFilterSlotTransform>> TRANSFORMS = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.values()) {
            Map<Direction, PipeFilterSlotTransform> byFace = new EnumMap<>(Direction.class);
            for (Direction displayFace : displayFacesFor(direction)) {
                byFace.put(displayFace, new PipeFilterSlotTransform(direction, displayFace));
            }
            TRANSFORMS.put(direction, byFace);
        }
    }

    private final Direction direction;
    private final Direction displayFace;

    private PipeFilterSlotTransform(Direction direction, Direction displayFace) {
        this.direction = direction;
        this.displayFace = displayFace;
    }

    public static PipeFilterSlotTransform forSide(Direction direction) {
        return forSideAndFace(direction, defaultDisplayFace(direction));
    }

    public static PipeFilterSlotTransform forSideAndFace(Direction direction, Direction displayFace) {
        Map<Direction, PipeFilterSlotTransform> byFace = TRANSFORMS.get(direction);
        PipeFilterSlotTransform transform = byFace.get(displayFace);
        return transform == null ? byFace.get(defaultDisplayFace(direction)) : transform;
    }

    public static Direction[] displayFacesFor(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> X_AXIS_FACES;
            case Y -> Y_AXIS_FACES;
            case Z -> Z_AXIS_FACES;
        };
    }

    public static Direction displayFaceForHit(Direction direction, Direction hitFace) {
        return hitFace.getAxis() == direction.getAxis() ? defaultDisplayFace(direction) : hitFace;
    }

    public Direction direction() {
        return direction;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        double armOffset = 0.265D;
        double surfaceOffset = 0.195D;
        return new Vec3(
                0.5D + direction.getStepX() * armOffset + displayFace.getStepX() * surfaceOffset,
                0.5D + direction.getStepY() * armOffset + displayFace.getStepY() * surfaceOffset,
                0.5D + direction.getStepZ() * armOffset + displayFace.getStepZ() * surfaceOffset);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        poseStack.mulPose(rotationFor(displayFace, direction));
    }

    @Override
    public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof SteamItemPipeBlock && state.getValue(SteamItemPipeBlock.propertyFor(direction));
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        return shouldRender(level, pos, state)
                && PipeTargeting.sideFromHit(localHit.x, localHit.y, localHit.z, direction) == direction;
    }

    @Override
    public float getScale() {
        return 0.42F;
    }

    private static Direction defaultDisplayFace(Direction direction) {
        return direction.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
    }

    private static Quaternionf rotationFor(Direction normalDirection, Direction upDirection) {
        Vec3i normal = normalDirection.getNormal();
        Vec3i up = upDirection.getNormal();

        int rightX = normal.getY() * up.getZ() - normal.getZ() * up.getY();
        int rightY = normal.getZ() * up.getX() - normal.getX() * up.getZ();
        int rightZ = normal.getX() * up.getY() - normal.getY() * up.getX();

        Matrix3f matrix = new Matrix3f(
                rightX, rightY, rightZ,
                up.getX(), up.getY(), up.getZ(),
                -normal.getX(), -normal.getY(), -normal.getZ());
        return new Quaternionf().setFromNormalized(matrix);
    }
}

package com.synderisdev.createpipes.content.intake;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.synderisdev.createpipes.config.CreatePipesConfig;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlock;
import com.synderisdev.createpipes.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteamIntakeBlockEntity extends BlockEntity {
    private int boilerRefreshCooldown;
    private double lastSteamProduction;

    public SteamIntakeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.STEAM_INTAKE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamIntakeBlockEntity intake) {
        intake.boilerRefreshCooldown--;
        if (intake.boilerRefreshCooldown <= 0) {
            intake.boilerRefreshCooldown = 20;
            intake.notifyBoilerChanged();
            intake.lastSteamProduction = intake.calculateSteamProduction();
            intake.setChanged();
        }
    }

    public boolean canConnectToPipe(Direction pipeSide) {
        Direction facing = getBlockState().getValue(SteamIntakeBlock.FACING);
        return pipeSide != facing;
    }

    public double getSteamProductionPerTick() {
        if (!CreatePipesConfig.REQUIRE_STEAM.get()) {
            return CreatePipesConfig.NETWORK_STEAM_CAPACITY.get();
        }
        if (level == null || level.isClientSide) {
            return 0;
        }
        lastSteamProduction = calculateSteamProduction();
        return lastSteamProduction;
    }

    public double getLastSteamProduction() {
        return lastSteamProduction;
    }

    public void notifyBoilerChanged() {
        FluidTankBlockEntity controller = getAttachedTankController();
        if (controller != null) {
            controller.updateBoilerState();
        }
    }

    public void refreshNeighboringPipes() {
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            SteamItemPipeBlock.refreshConnections(level, worldPosition.relative(direction));
        }
    }

    private double calculateSteamProduction() {
        FluidTankBlockEntity controller = getAttachedTankController();
        if (controller == null) {
            return 0;
        }

        controller.boiler.updateTemperature(controller);
        int size = controller.getTotalTankSize();
        int sizeLevel = controller.boiler.getMaxHeatLevelForBoilerSize(size);
        int waterLevel = controller.boiler.getMaxHeatLevelForWaterSupply();
        int heatLevel = controller.boiler.getTheoreticalHeatLevel();
        int activeLevel = Math.min(heatLevel, Math.min(sizeLevel, waterLevel));
        if (activeLevel <= 0 && controller.boiler.isPassive(size)) {
            activeLevel = 1;
        }
        return activeLevel * CreatePipesConfig.STEAM_UNITS_PER_BOILER_LEVEL_PER_TICK.get();
    }

    @Nullable
    private FluidTankBlockEntity getAttachedTankController() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(SteamIntakeBlock.FACING);
        BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(facing));
        if (blockEntity instanceof FluidTankBlockEntity tank) {
            return tank.getControllerBE();
        }
        return null;
    }

    public static int countAttachedIntakes(FluidTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null) {
            return 0;
        }

        int count = 0;
        BlockPos controllerPos = controller.getBlockPos();
        for (int y = 0; y < controller.getHeight(); y++) {
            for (int x = 0; x < controller.getWidth(); x++) {
                for (int z = 0; z < controller.getWidth(); z++) {
                    BlockPos tankPos = controllerPos.offset(x, y, z);
                    for (Direction direction : Direction.values()) {
                        BlockPos intakePos = tankPos.relative(direction);
                        if (level.getBlockEntity(intakePos) instanceof SteamIntakeBlockEntity intake
                                && intake.getBlockState().getValue(SteamIntakeBlock.FACING) == direction.getOpposite()) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}

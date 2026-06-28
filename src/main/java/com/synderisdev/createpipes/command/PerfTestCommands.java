package com.synderisdev.createpipes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.synderisdev.createpipes.content.pipe.PipeConnectionMode;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlock;
import com.synderisdev.createpipes.content.pipe.SteamItemPipeBlockEntity;
import com.synderisdev.createpipes.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PerfTestCommands {
    private static final int DEFAULT_LANES = 8;
    private static final int DEFAULT_LENGTH = 24;
    private static final int DEFAULT_STACKS = 27;
    private static final int LANE_SPACING = 3;

    private PerfTestCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createpipes")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("perf")
                        .then(Commands.literal("setup")
                                .executes(context -> setup(context.getSource(), DEFAULT_LANES, DEFAULT_LENGTH, DEFAULT_STACKS))
                                .then(Commands.argument("lanes", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> setup(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "lanes"), DEFAULT_LENGTH, DEFAULT_STACKS))
                                        .then(Commands.argument("length", IntegerArgumentType.integer(1, 256))
                                                .executes(context -> setup(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "lanes"),
                                                        IntegerArgumentType.getInteger(context, "length"),
                                                        DEFAULT_STACKS))
                                                .then(Commands.argument("stacks", IntegerArgumentType.integer(0, 54))
                                                        .executes(context -> setup(context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "lanes"),
                                                                IntegerArgumentType.getInteger(context, "length"),
                                                                IntegerArgumentType.getInteger(context, "stacks")))))))
                        .then(Commands.literal("clear")
                                .executes(context -> clear(context.getSource(), DEFAULT_LANES, DEFAULT_LENGTH))
                                .then(Commands.argument("lanes", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> clear(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "lanes"), DEFAULT_LENGTH))
                                        .then(Commands.argument("length", IntegerArgumentType.integer(1, 256))
                                                .executes(context -> clear(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "lanes"),
                                                        IntegerArgumentType.getInteger(context, "length"))))))
                        .then(Commands.literal("refill")
                                .executes(context -> refill(context.getSource(), DEFAULT_LANES, DEFAULT_LENGTH, DEFAULT_STACKS))
                                .then(Commands.argument("lanes", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> refill(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "lanes"), DEFAULT_LENGTH, DEFAULT_STACKS))
                                        .then(Commands.argument("length", IntegerArgumentType.integer(1, 256))
                                                .executes(context -> refill(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "lanes"),
                                                        IntegerArgumentType.getInteger(context, "length"),
                                                        DEFAULT_STACKS))
                                                .then(Commands.argument("stacks", IntegerArgumentType.integer(0, 54))
                                                        .executes(context -> refill(context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "lanes"),
                                                                IntegerArgumentType.getInteger(context, "length"),
                                                                IntegerArgumentType.getInteger(context, "stacks")))))))));
    }

    private static int setup(CommandSourceStack source, int lanes, int length, int stacks) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos origin = player.blockPosition().relative(forward, 3);

        for (int lane = 0; lane < lanes; lane++) {
            BlockPos laneOrigin = origin.relative(right, lane * LANE_SPACING);
            buildLane(level, laneOrigin, forward, length, stacks);
        }

        source.sendSuccess(() -> Component.literal("Built Create: Pipes perf setup: " + lanes + " lanes, "
                + length + " pipes per lane, " + stacks + " source stacks per lane."), true);
        return lanes * length;
    }

    private static int clear(CommandSourceStack source, int lanes, int length) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos origin = player.blockPosition().relative(forward, 3);

        for (int lane = 0; lane < lanes; lane++) {
            BlockPos laneOrigin = origin.relative(right, lane * LANE_SPACING);
            clearLane(level, laneOrigin, forward, length);
        }

        source.sendSuccess(() -> Component.literal("Cleared Create: Pipes perf setup footprint: " + lanes
                + " lanes, " + length + " pipes per lane."), true);
        return lanes * length;
    }

    private static int refill(CommandSourceStack source, int lanes, int length, int stacks) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos origin = player.blockPosition().relative(forward, 3);

        for (int lane = 0; lane < lanes; lane++) {
            BlockPos laneOrigin = origin.relative(right, lane * LANE_SPACING);
            refillLane(level, laneOrigin, forward, length, stacks);
        }

        source.sendSuccess(() -> Component.literal("Refilled Create: Pipes perf setup: " + lanes + " lanes, "
                + stacks + " source stacks per lane."), true);
        return lanes * stacks;
    }

    private static void buildLane(ServerLevel level, BlockPos sourcePos, Direction forward, int length, int stacks) {
        BlockPos destinationPos = sourcePos.relative(forward, length + 1);
        level.setBlock(sourcePos, barrelState(forward), Block.UPDATE_ALL);
        level.setBlock(destinationPos, barrelState(forward.getOpposite()), Block.UPDATE_ALL);

        for (int i = 1; i <= length; i++) {
            BlockPos pipePos = sourcePos.relative(forward, i);
            level.setBlock(pipePos, ModBlocks.STEAM_ITEM_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        }

        for (int i = 1; i <= length; i++) {
            SteamItemPipeBlock.refreshConnections(level, sourcePos.relative(forward, i));
        }

        if (level.getBlockEntity(sourcePos.relative(forward, 1)) instanceof SteamItemPipeBlockEntity firstPipe) {
            firstPipe.setMode(forward.getOpposite(), PipeConnectionMode.PULL);
        }
        if (level.getBlockEntity(sourcePos.relative(forward, length)) instanceof SteamItemPipeBlockEntity lastPipe) {
            lastPipe.setMode(forward, PipeConnectionMode.PUSH);
        }

        refillLane(level, sourcePos, forward, length, stacks);
    }

    private static void clearLane(ServerLevel level, BlockPos sourcePos, Direction forward, int length) {
        for (int i = 0; i <= length + 1; i++) {
            level.setBlock(sourcePos.relative(forward, i), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void refillLane(ServerLevel level, BlockPos sourcePos, Direction forward, int length, int stacks) {
        clearContainer(level, sourcePos);
        clearContainer(level, sourcePos.relative(forward, length + 1));

        if (level.getBlockEntity(sourcePos) instanceof Container container) {
            int slots = Math.min(container.getContainerSize(), stacks);
            for (int slot = 0; slot < slots; slot++) {
                container.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            container.setChanged();
        }
    }

    private static void clearContainer(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                container.setItem(slot, ItemStack.EMPTY);
            }
            container.setChanged();
        }
    }

    private static BlockState barrelState(Direction facing) {
        return Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, facing);
    }
}

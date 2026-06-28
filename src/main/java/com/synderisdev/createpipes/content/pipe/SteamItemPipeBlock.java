package com.synderisdev.createpipes.content.pipe;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.synderisdev.createpipes.content.tool.NetworkControlToolItem;
import com.synderisdev.createpipes.registry.ModBlockEntityTypes;
import com.synderisdev.createpipes.registry.ModBlocks;
import com.synderisdev.createpipes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class SteamItemPipeBlock extends Block implements EntityBlock, IWrenchable {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty OFFLINE = BooleanProperty.create("offline");

    private static final Map<Direction, BooleanProperty> CONNECTION_PROPERTIES = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        CONNECTION_PROPERTIES.put(Direction.NORTH, NORTH);
        CONNECTION_PROPERTIES.put(Direction.EAST, EAST);
        CONNECTION_PROPERTIES.put(Direction.SOUTH, SOUTH);
        CONNECTION_PROPERTIES.put(Direction.WEST, WEST);
        CONNECTION_PROPERTIES.put(Direction.UP, UP);
        CONNECTION_PROPERTIES.put(Direction.DOWN, DOWN);

        ARMS.put(Direction.NORTH, box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.UP, box(5, 11, 5, 11, 16, 11));
        ARMS.put(Direction.DOWN, box(5, 0, 5, 11, 5, 11));
    }

    public SteamItemPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(OFFLINE, false));
    }

    public static BooleanProperty propertyFor(Direction direction) {
        return CONNECTION_PROPERTIES.get(direction);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, OFFLINE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(propertyFor(direction))) {
                shape = Shapes.or(shape, ARMS.get(direction));
            }
        }
        return shape;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos facingPos) {
        return state.setValue(propertyFor(facing), canConnect(level, currentPos, facing));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.is(oldState.getBlock())) {
            SteamItemPipeBlockEntity.invalidateNetworkCache(level);
            refreshConnections(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        SteamItemPipeBlockEntity.invalidateNetworkCache(level);
        refreshConnections(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe) {
                pipe.dropFilterItems();
            }
            SteamItemPipeBlockEntity.invalidateNetworkCache(level);
            for (Direction direction : Direction.values()) {
                refreshConnections(level, pos.relative(direction));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.mayBuild()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModItems.NETWORK_CONTROL_TOOL.get())) {
            InteractionResult result = NetworkControlToolItem.toggleNetwork(level, pos, player);
            return result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")))) {
            return setFilterFromHeldItem(stack, level, pos, player, hitResult);
        }
        if (player.isShiftKeyDown()) {
            InteractionResult result = onSneakWrenched(state, new UseOnContext(player, hand, hitResult));
            return result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = cycleMode(state, new UseOnContext(player, hand, hitResult));
        return result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }

        Direction side = getTargetedSide(pos, hitResult.getLocation(), hitResult.getDirection());
        if (level.isClientSide) {
            return pipe.isFilterableEndpoint(side) && !pipe.getFilterItem(side).isEmpty()
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        if (!pipe.clearFilter(side, player)) {
            return InteractionResult.PASS;
        }

        displayFilterMessage(player, "message.create_pipes.filter_cleared", side, ItemStack.EMPTY);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.25F, 0.9F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return cycleMode(state, context);
    }

    private InteractionResult cycleMode(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
            return InteractionResult.FAIL;
        }

        Direction side = getTargetedSide(context);
        if (!level.isClientSide) {
            PipeConnectionMode next = pipe.getMode(side).next();
            pipe.setMode(side, next);
            pipe.resetTransferCooldown();
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.create_pipes.mode_changed",
                        net.minecraft.network.chat.Component.translatable("direction.create_pipes." + side.getSerializedName()),
                        net.minecraft.network.chat.Component.translatable("mode.create_pipes." + next.serializedName())
                ), true);
            }
            IWrenchable.playRotateSound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static ItemInteractionResult setFilterFromHeldItem(ItemStack stack, Level level, BlockPos pos, Player player,
                                                               BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction side = getTargetedSide(pos, hitResult.getLocation(), hitResult.getDirection());
        if (!pipe.isFilterableEndpoint(side)) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.create_pipes.filter_no_inventory"), true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (!pipe.setFilterFromHeldItem(side, player, stack)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            displayFilterMessage(player, "message.create_pipes.filter_set", side, pipe.getFilterItem(side));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25F, 0.1F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private static void displayFilterMessage(Player player, String key, Direction side, ItemStack filter) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                key,
                net.minecraft.network.chat.Component.translatable("direction.create_pipes." + side.getSerializedName()),
                filter.isEmpty() ? net.minecraft.network.chat.Component.translatable("tooltip.create_pipes.pipe.no_filter") : filter.getHoverName()
        ), true);
    }

    private static Direction getTargetedSide(UseOnContext context) {
        return getTargetedSide(context.getClickedPos(), context.getClickLocation(), context.getClickedFace());
    }

    private static Direction getTargetedSide(BlockPos pos, Vec3 hit, Direction fallback) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        return PipeTargeting.sideFromHit(x, y, z, fallback);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamItemPipeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (state.getValue(OFFLINE)) {
            return null;
        }
        if (type == ModBlockEntityTypes.STEAM_ITEM_PIPE.get()) {
            return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    SteamItemPipeBlockEntity.serverTick(tickerLevel, tickerPos, tickerState, (SteamItemPipeBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state
                .setValue(propertyFor(rotation.rotate(Direction.NORTH)), state.getValue(NORTH))
                .setValue(propertyFor(rotation.rotate(Direction.SOUTH)), state.getValue(SOUTH))
                .setValue(propertyFor(rotation.rotate(Direction.EAST)), state.getValue(EAST))
                .setValue(propertyFor(rotation.rotate(Direction.WEST)), state.getValue(WEST));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(Direction.NORTH));
    }

    public static void refreshConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.STEAM_ITEM_PIPE.get())) {
            return;
        }
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(propertyFor(direction), canConnect(level, pos, direction));
        }
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe) {
            pipe.clearFiltersWithoutInventories();
        }
    }

    private static boolean canConnect(LevelAccessor level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
            return false;
        }
        if (!PipeModeRules.connects(pipe.getMode(direction))) {
            return false;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.is(ModBlocks.STEAM_ITEM_PIPE.get())) {
            if (level.getBlockEntity(neighborPos) instanceof SteamItemPipeBlockEntity other) {
                return PipeModeRules.connects(other.getMode(direction.getOpposite()));
            }
            return true;
        }
        if (neighborState.is(ModBlocks.STEAM_INTAKE.get())) {
            return true;
        }
        if (level instanceof ServerLevel serverLevel) {
            return AdjacentItemHandlers.get(serverLevel, neighborPos, direction.getOpposite()) != null;
        }
        return false;
    }
}

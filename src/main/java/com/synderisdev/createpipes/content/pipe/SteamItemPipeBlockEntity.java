package com.synderisdev.createpipes.content.pipe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.synderisdev.createpipes.config.CreatePipesConfig;
import com.synderisdev.createpipes.content.intake.SteamIntakeBlockEntity;
import com.synderisdev.createpipes.registry.ModBlockEntityTypes;
import com.synderisdev.createpipes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

public class SteamItemPipeBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final Map<ServerLevel, NetworkTickCache> NETWORK_CACHES = new WeakHashMap<>();
    private static final Direction[] DIRECTIONS = Direction.values();

    private final EnumMap<Direction, PipeConnectionMode> modes = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, FilterItemStack> filters = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, PipeItemHandler> sideHandlers = new EnumMap<>(Direction.class);
    private final PipeItemHandler nullSideHandler = new PipeItemHandler(this, null);
    private int transferCooldown;
    private double steamStored;
    private int lastMovedItems;
    private boolean lastBlocked;
    private boolean networkDisabled;
    private int renderMarkerMask = -1;
    @Nullable
    private BlockState renderMarkerState;
    private int renderFilterMask = -1;
    @Nullable
    private BlockState renderFilterState;

    public SteamItemPipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.STEAM_ITEM_PIPE.get(), pos, blockState);
        for (Direction direction : DIRECTIONS) {
            modes.put(direction, PipeConnectionMode.NORMAL);
            filters.put(direction, FilterItemStack.empty());
            sideHandlers.put(direction, new PipeItemHandler(this, direction));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamItemPipeBlockEntity pipe) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (pipe.networkDisabled) {
            pipe.syncOfflineBlockState(true);
            return;
        }

        NetworkView network = getNetwork(serverLevel, pos);
        if (!network.root.equals(pos)) {
            return;
        }

        pipe.fillSteamBuffer(network);
        pipe.transferCooldown--;
        if (pipe.transferCooldown > 0) {
            return;
        }
        pipe.transferCooldown = CreatePipesConfig.EXTRACT_INTERVAL_TICKS.get();
        pipe.lastMovedItems = pipe.processPulls(network);
        pipe.lastBlocked = pipe.lastMovedItems == 0 && network.hasPullSide(serverLevel);
        pipe.setChanged();
    }

    public PipeConnectionMode getMode(Direction direction) {
        return modes.getOrDefault(direction, PipeConnectionMode.NORMAL);
    }

    public void setMode(Direction direction, PipeConnectionMode mode) {
        modes.put(direction, mode);
        invalidateRenderMarkerMask();
        invalidateRenderFilterMask();
        setChanged();
        if (level != null) {
            BlockState oldState = getBlockState();
            SteamItemPipeBlock.refreshConnections(level, worldPosition);
            SteamItemPipeBlock.refreshConnections(level, worldPosition.relative(direction));
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            if (!level.isClientSide) {
                invalidateNetworkCache(level);
                level.sendBlockUpdated(worldPosition, oldState, getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        if (side != null && !PipeModeRules.acceptsExternalInsertion(getMode(side))) {
            return null;
        }
        return side == null ? nullSideHandler : sideHandlers.get(side);
    }

    public boolean isNetworkDisabled() {
        return networkDisabled;
    }

    public boolean toggleNetworkDisabled(ServerLevel serverLevel) {
        boolean disabled = !networkDisabled;
        NetworkView network = getNetwork(serverLevel, worldPosition);
        network.setDisabled(serverLevel, disabled);
        return disabled;
    }

    private void setNetworkDisabled(boolean disabled) {
        if (networkDisabled == disabled) {
            return;
        }

        networkDisabled = disabled;
        lastMovedItems = 0;
        lastBlocked = false;
        transferCooldown = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            syncOfflineBlockState(disabled);
        }
    }

    private void syncOfflineBlockState(boolean disabled) {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof SteamItemPipeBlock) || state.getValue(SteamItemPipeBlock.OFFLINE) == disabled) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(SteamItemPipeBlock.OFFLINE, disabled), Block.UPDATE_ALL);
    }

    public ItemStack getFilterItem(Direction direction) {
        return filters.getOrDefault(direction, FilterItemStack.empty()).item();
    }

    public boolean isFilterableEndpoint(Direction direction) {
        if (level == null) {
            return false;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof SteamItemPipeBlock)
                || !state.getValue(SteamItemPipeBlock.propertyFor(direction))) {
            return false;
        }

        BlockPos inventoryPos = worldPosition.relative(direction);
        BlockState inventoryState = level.getBlockState(inventoryPos);
        if (inventoryState.is(ModBlocks.STEAM_ITEM_PIPE.get()) || inventoryState.is(ModBlocks.STEAM_INTAKE.get())) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel) {
            return AdjacentItemHandlers.get(serverLevel, inventoryPos, direction.getOpposite()) != null;
        }
        return true;
    }

    public boolean setFilter(Direction direction, ItemStack stack) {
        ItemStack filterStack = stack.copy();
        if (!filterStack.isEmpty()) {
            if (!isFilterableEndpoint(direction)) {
                return false;
            }
            filterStack.setCount(1);
        }

        filters.put(direction, FilterItemStack.of(filterStack));
        invalidateRenderFilterMask();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    public boolean setFilterFromHeldItem(Direction direction, Player player, ItemStack heldStack) {
        if (heldStack.isEmpty()) {
            return clearFilter(direction, player);
        }
        if (!isFilterableEndpoint(direction)) {
            return false;
        }

        ItemStack previous = getFilterItem(direction).copy();
        ItemStack filterStack = heldStack.copy();
        filterStack.setCount(1);
        setFilter(direction, filterStack);

        if (!player.isCreative() && filterStack.getItem() instanceof FilterItem) {
            heldStack.shrink(1);
        }
        returnStoredFilterToPlayer(player, previous);
        return true;
    }

    public void clearFiltersWithoutInventories() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (Direction direction : DIRECTIONS) {
            ItemStack filter = getFilterItem(direction);
            if (filter.isEmpty() || isFilterableEndpoint(direction)) {
                continue;
            }
            if (filter.getItem() instanceof FilterItem) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D, filter.copy());
            }
            filters.put(direction, FilterItemStack.empty());
            changed = true;
        }
        if (changed) {
            invalidateRenderFilterMask();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public boolean clearFilter(Direction direction, @Nullable Player player) {
        ItemStack previous = getFilterItem(direction).copy();
        if (previous.isEmpty()) {
            return false;
        }

        setFilter(direction, ItemStack.EMPTY);
        if (player != null) {
            returnStoredFilterToPlayer(player, previous);
        }
        return true;
    }

    public void dropFilterItems() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (Direction direction : DIRECTIONS) {
            ItemStack filter = getFilterItem(direction);
            if (filter.getItem() instanceof FilterItem) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D, filter.copy());
            }
        }
    }

    boolean matchesFilter(Direction direction, ItemStack stack) {
        FilterItemStack filter = filters.getOrDefault(direction, FilterItemStack.empty());
        return level == null || filter.test(level, stack);
    }

    public int getRenderMarkerMask() {
        BlockState state = getBlockState();
        if (renderMarkerMask >= 0 && renderMarkerState == state) {
            return renderMarkerMask;
        }

        int mask = 0;
        if (state.getBlock() instanceof SteamItemPipeBlock) {
            for (Direction direction : DIRECTIONS) {
                if (!state.getValue(SteamItemPipeBlock.propertyFor(direction))) {
                    continue;
                }
                PipeConnectionMode mode = getMode(direction);
                if (mode == PipeConnectionMode.PULL || mode == PipeConnectionMode.PUSH) {
                    mask |= 1 << direction.ordinal();
                }
            }
        }

        renderMarkerState = state;
        renderMarkerMask = mask;
        return mask;
    }

    public int getRenderFilterMask() {
        BlockState state = getBlockState();
        if (renderFilterMask >= 0 && renderFilterState == state) {
            return renderFilterMask;
        }

        int mask = 0;
        if (state.getBlock() instanceof SteamItemPipeBlock) {
            for (Direction direction : DIRECTIONS) {
                if (!state.getValue(SteamItemPipeBlock.propertyFor(direction))) {
                    continue;
                }
                if (isFilterableEndpoint(direction) && !getFilterItem(direction).isEmpty()) {
                    mask |= 1 << direction.ordinal();
                }
            }
        }

        renderFilterState = state;
        renderFilterMask = mask;
        return mask;
    }

    private void invalidateRenderMarkerMask() {
        renderMarkerMask = -1;
        renderMarkerState = null;
    }

    private void invalidateRenderFilterMask() {
        renderFilterMask = -1;
        renderFilterState = null;
    }

    public void resetTransferCooldown() {
        transferCooldown = 0;
        setChanged();
    }

    public ItemStack routeExternalInsert(ItemStack stack, @Nullable Direction fromSide, boolean simulate) {
        if (level == null || level.isClientSide || stack.isEmpty()) {
            return stack;
        }
        if (networkDisabled) {
            return stack;
        }
        NetworkView network = getNetwork((ServerLevel) level, worldPosition);
        SteamItemPipeBlockEntity root = network.rootEntity((ServerLevel) level);
        if (root == null) {
            return stack;
        }
        if (fromSide != null && !matchesFilter(fromSide, stack)) {
            return stack;
        }

        int requested = Math.min(stack.getCount(), root.maxMovableWithSteam(stack.getCount()));
        if (requested <= 0) {
            return stack;
        }

        ItemStack candidate = copyWithCount(stack, requested);
        BlockPos excludedInventory = fromSide == null ? null : worldPosition.relative(fromSide);
        Destination destination = network.findDestination((ServerLevel) level, worldPosition, excludedInventory, candidate);
        if (destination == null) {
            return stack;
        }

        int insertable = ItemTransferHelper.insertableAmount(destination.handler, candidate);
        if (insertable <= 0) {
            return stack;
        }

        int toMove = Math.min(insertable, requested);
        if (!simulate) {
            ItemStack toInsert = copyWithCount(stack, toMove);
            ItemStack remaining = ItemTransferHelper.insert(destination.handler, toInsert, false);
            int moved = toMove - remaining.getCount();
            if (moved > 0) {
                root.consumeSteamForItems(moved);
                root.lastMovedItems += moved;
                root.lastBlocked = false;
                root.setChanged();
            }
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(toMove);
        return remainder;
    }

    private void fillSteamBuffer(NetworkView network) {
        int capacity = CreatePipesConfig.NETWORK_STEAM_CAPACITY.get();
        if (!CreatePipesConfig.REQUIRE_STEAM.get()) {
            steamStored = capacity;
            return;
        }
        double generated = network.steamProduction((ServerLevel) level);
        steamStored = Math.min(capacity, steamStored + generated);
    }

    private int processPulls(NetworkView network) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }

        int moved = 0;
        List<BlockedStack> blockedStacksThisPulse = new ArrayList<>();
        List<CachedDestination> destinationCache = new ArrayList<>();
        for (Source source : network.sources(serverLevel)) {
            IItemHandler sourceHandler = source.handler;
            for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                int maxBySteam = maxMovableWithSteam(CreatePipesConfig.ITEMS_PER_EXTRACT.get());
                if (maxBySteam <= 0) {
                    return moved;
                }

                ItemStack simulatedExtract = sourceHandler.extractItem(slot, maxBySteam, true);
                if (simulatedExtract.isEmpty()) {
                    continue;
                }
                if (!source.matchesFilter(serverLevel, simulatedExtract)) {
                    continue;
                }
                if (isBlockedStack(blockedStacksThisPulse, source.pipePos, source.inventoryPos, simulatedExtract)) {
                    continue;
                }

                Destination destination = findCachedDestination(destinationCache, source.pipePos, source.inventoryPos, simulatedExtract);
                if (destination == null) {
                    destination = network.findDestination(serverLevel, source.pipePos, source.inventoryPos, simulatedExtract);
                    if (destination != null) {
                        destinationCache.add(new CachedDestination(source.pipePos, source.inventoryPos,
                                copyWithCount(simulatedExtract, 1), destination));
                    }
                }
                if (destination == null) {
                    blockedStacksThisPulse.add(new BlockedStack(source.pipePos, source.inventoryPos, copyWithCount(simulatedExtract, 1)));
                    continue;
                }

                int insertable = ItemTransferHelper.insertableAmount(destination.handler, simulatedExtract);
                if (insertable <= 0) {
                    blockedStacksThisPulse.add(new BlockedStack(source.pipePos, source.inventoryPos, copyWithCount(simulatedExtract, 1)));
                    continue;
                }

                int toExtract = Math.min(insertable, simulatedExtract.getCount());
                ItemStack extracted = sourceHandler.extractItem(slot, toExtract, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                ItemStack notInserted = ItemTransferHelper.insert(destination.handler, extracted, false);
                int inserted = extracted.getCount() - notInserted.getCount();
                if (inserted > 0) {
                    consumeSteamForItems(inserted);
                    moved += inserted;
                }
                if (!notInserted.isEmpty()) {
                    ItemStack notReturned = ItemTransferHelper.insert(sourceHandler, notInserted, false);
                    if (!notReturned.isEmpty()) {
                        Containers.dropItemStack(level, source.inventoryPos.getX() + 0.5D, source.inventoryPos.getY() + 0.5D,
                                source.inventoryPos.getZ() + 0.5D, notReturned);
                    }
                }
            }
        }
        return moved;
    }

    @Nullable
    private static Destination findCachedDestination(List<CachedDestination> cache, BlockPos sourcePipe,
                                                     BlockPos excludedInventory, ItemStack stack) {
        for (CachedDestination cached : cache) {
            if (!cached.sourcePipe.equals(sourcePipe) || !cached.excludedInventory.equals(excludedInventory)) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(cached.stack, stack)) {
                continue;
            }
            if (ItemTransferHelper.insertableAmount(cached.destination.handler, stack) > 0) {
                return cached.destination;
            }
        }
        return null;
    }

    private static boolean isBlockedStack(List<BlockedStack> blockedStacks, BlockPos sourcePipe,
                                          BlockPos excludedInventory, ItemStack stack) {
        for (BlockedStack blockedStack : blockedStacks) {
            if (!blockedStack.sourcePipe.equals(sourcePipe) || !blockedStack.excludedInventory.equals(excludedInventory)) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(blockedStack.stack, stack)) {
                return true;
            }
        }
        return false;
    }

    private int maxMovableWithSteam(int requested) {
        if (!CreatePipesConfig.REQUIRE_STEAM.get()) {
            return requested;
        }
        int bySteam = (int) Math.floor(steamStored / CreatePipesConfig.STEAM_UNITS_PER_ITEM.get());
        return Math.min(requested, bySteam);
    }

    private void consumeSteamForItems(int moved) {
        if (!CreatePipesConfig.REQUIRE_STEAM.get()) {
            return;
        }
        steamStored = Math.max(0, steamStored - moved * CreatePipesConfig.STEAM_UNITS_PER_ITEM.get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag modesTag = new CompoundTag();
        CompoundTag filtersTag = new CompoundTag();
        for (Direction direction : DIRECTIONS) {
            modesTag.putString(direction.getSerializedName(), getMode(direction).serializedName());
            filtersTag.put(direction.getSerializedName(),
                    filters.getOrDefault(direction, FilterItemStack.empty()).serializeNBT(registries));
        }
        tag.put("Modes", modesTag);
        tag.put("Filters", filtersTag);
        tag.putDouble("Steam", steamStored);
        tag.putInt("TransferCooldown", transferCooldown);
        tag.putBoolean("NetworkDisabled", networkDisabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Modes")) {
            CompoundTag modesTag = tag.getCompound("Modes");
            for (Direction direction : DIRECTIONS) {
                modes.put(direction, PipeConnectionMode.byName(modesTag.getString(direction.getSerializedName())));
            }
        }
        for (Direction direction : DIRECTIONS) {
            filters.put(direction, FilterItemStack.empty());
        }
        if (tag.contains("Filters")) {
            CompoundTag filtersTag = tag.getCompound("Filters");
            for (Direction direction : DIRECTIONS) {
                filters.put(direction, FilterItemStack.of(registries, filtersTag.getCompound(direction.getSerializedName())));
            }
        }
        steamStored = tag.getDouble("Steam");
        transferCooldown = tag.getInt("TransferCooldown");
        networkDisabled = tag.getBoolean("NetworkDisabled");
        invalidateRenderMarkerMask();
        invalidateRenderFilterMask();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("tooltip.create_pipes.pipe.status").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(networkDisabled
                ? "tooltip.create_pipes.pipe.network_disabled"
                : "tooltip.create_pipes.pipe.network_enabled").withStyle(networkDisabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        if (CreatePipesConfig.REQUIRE_STEAM.get()) {
            tooltip.add(Component.translatable("tooltip.create_pipes.pipe.steam",
                    String.format("%.1f", steamStored),
                    CreatePipesConfig.NETWORK_STEAM_CAPACITY.get()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.create_pipes.pipe.steam_disabled").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.create_pipes.pipe.moved", lastMovedItems).withStyle(ChatFormatting.DARK_GREEN));
        if (lastBlocked) {
            tooltip.add(Component.translatable("tooltip.create_pipes.pipe.blocked").withStyle(ChatFormatting.RED));
        }
        if (isPlayerSneaking) {
            for (Direction direction : DIRECTIONS) {
                ItemStack filter = getFilterItem(direction);
                if (!isFilterableEndpoint(direction)) {
                    filter = ItemStack.EMPTY;
                }
                tooltip.add(Component.translatable("tooltip.create_pipes.pipe.side",
                        Component.translatable("direction.create_pipes." + direction.getSerializedName()),
                        Component.translatable("mode.create_pipes." + getMode(direction).serializedName()),
                        filter.isEmpty() ? Component.translatable("tooltip.create_pipes.pipe.no_filter") : filter.getHoverName()
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return true;
    }

    private static void returnStoredFilterToPlayer(Player player, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof FilterItem && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static NetworkView getNetwork(ServerLevel level, BlockPos pos) {
        NetworkTickCache cache = NETWORK_CACHES.computeIfAbsent(level, ignored -> new NetworkTickCache());
        return cache.get(level, pos);
    }

    public static void invalidateNetworkCache(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            NetworkTickCache cache = NETWORK_CACHES.get(serverLevel);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private record Source(BlockPos pipePos, Direction side, BlockPos inventoryPos, IItemHandler handler) {
        boolean matchesFilter(ServerLevel level, ItemStack stack) {
            BlockEntity blockEntity = level.getBlockEntity(pipePos);
            return !(blockEntity instanceof SteamItemPipeBlockEntity pipe) || pipe.matchesFilter(side, stack);
        }
    }

    private record Destination(BlockPos pipePos, Direction side, BlockPos inventoryPos, IItemHandler handler) {
    }

    private record CachedDestination(BlockPos sourcePipe, BlockPos excludedInventory, ItemStack stack,
                                     Destination destination) {
    }

    private record BlockedStack(BlockPos sourcePipe, BlockPos excludedInventory, ItemStack stack) {
    }

    private static final class NetworkTickCache {
        private final Map<BlockPos, NetworkView> networksByPipe = new HashMap<>();

        NetworkView get(ServerLevel level, BlockPos pos) {
            NetworkView cached = networksByPipe.get(pos);
            if (cached != null) {
                return cached;
            }

            NetworkView collected = NetworkView.collect(level, pos);
            for (BlockPos pipePos : collected.pipes) {
                networksByPipe.put(pipePos, collected);
            }
            return collected;
        }

        void clear() {
            networksByPipe.clear();
        }
    }

    private static final class NetworkView {
        private final Set<BlockPos> pipes;
        private final BlockPos root;
        @Nullable
        private List<Source> sources;
        private final Map<BlockPos, List<BlockPos>> pipesByDistance = new HashMap<>();

        private NetworkView(Set<BlockPos> pipes, BlockPos root) {
            this.pipes = pipes;
            this.root = root;
        }

        static NetworkView collect(ServerLevel level, BlockPos start) {
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            visited.add(start);
            queue.add(start);

            while (!queue.isEmpty()) {
                BlockPos current = queue.remove();
                for (Direction direction : DIRECTIONS) {
                    if (!canTravel(level, current, direction)) {
                        continue;
                    }
                    BlockPos next = current.relative(direction);
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }

            BlockPos root = visited.stream()
                    .min(Comparator.comparingLong(BlockPos::asLong))
                    .orElse(start);
            return new NetworkView(visited, root);
        }

        @Nullable
        SteamItemPipeBlockEntity rootEntity(ServerLevel level) {
            BlockEntity blockEntity = level.getBlockEntity(root);
            return blockEntity instanceof SteamItemPipeBlockEntity pipe ? pipe : null;
        }

        List<Source> sources(ServerLevel level) {
            if (sources != null) {
                return sources;
            }

            List<Source> sources = new ArrayList<>();
            for (BlockPos pipePos : pipes) {
                if (!(level.getBlockEntity(pipePos) instanceof SteamItemPipeBlockEntity pipe)) {
                    continue;
                }
                for (Direction direction : DIRECTIONS) {
                    if (!PipeModeRules.activelyExtracts(pipe.getMode(direction))) {
                        continue;
                    }
                    BlockPos inventoryPos = pipePos.relative(direction);
                    if (isPipeOrIntake(level, inventoryPos)) {
                        continue;
                    }
                    IItemHandler handler = AdjacentItemHandlers.get(level, inventoryPos, direction.getOpposite());
                    if (handler != null) {
                        sources.add(new Source(pipePos, direction, inventoryPos, handler));
                    }
                }
            }
            this.sources = sources;
            return sources;
        }

        boolean hasPullSide(ServerLevel level) {
            return !sources(level).isEmpty();
        }

        void setDisabled(ServerLevel level, boolean disabled) {
            for (BlockPos pipePos : pipes) {
                if (level.getBlockEntity(pipePos) instanceof SteamItemPipeBlockEntity pipe) {
                    pipe.setNetworkDisabled(disabled);
                }
            }
        }

        @Nullable
        Destination findDestination(ServerLevel level, BlockPos sourcePipe, @Nullable BlockPos excludedInventory, ItemStack stack) {
            for (BlockPos pipePos : pipesByDistance(level, sourcePipe)) {
                Destination destination = firstDestinationAt(level, pipePos, excludedInventory, stack);
                if (destination != null) {
                    return destination;
                }
            }
            return null;
        }

        private List<BlockPos> pipesByDistance(ServerLevel level, BlockPos sourcePipe) {
            return pipesByDistance.computeIfAbsent(sourcePipe, key -> {
                Map<BlockPos, Integer> distances = distancesFrom(level, key);
                List<BlockPos> sorted = new ArrayList<>(pipes);
                sorted.sort(Comparator.comparingInt(pos -> distances.getOrDefault(pos, Integer.MAX_VALUE)));
                return sorted;
            });
        }

        @Nullable
        private Destination firstDestinationAt(ServerLevel level, BlockPos pipePos, @Nullable BlockPos excludedInventory, ItemStack stack) {
            if (!(level.getBlockEntity(pipePos) instanceof SteamItemPipeBlockEntity pipe)) {
                return null;
            }
            for (Direction direction : DIRECTIONS) {
                PipeConnectionMode mode = pipe.getMode(direction);
                if (!PipeModeRules.acceptsActiveInsertion(mode)) {
                    continue;
                }
                BlockPos inventoryPos = pipePos.relative(direction);
                if (inventoryPos.equals(excludedInventory) || isPipeOrIntake(level, inventoryPos)) {
                    continue;
                }
                if (!pipe.matchesFilter(direction, stack)) {
                    continue;
                }
                IItemHandler handler = AdjacentItemHandlers.get(level, inventoryPos, direction.getOpposite());
                if (handler != null && ItemTransferHelper.insertableAmount(handler, stack) > 0) {
                    return new Destination(pipePos, direction, inventoryPos, handler);
                }
            }
            return null;
        }

        double steamProduction(ServerLevel level) {
            Set<BlockPos> intakes = new HashSet<>();
            double steam = 0;
            for (BlockPos pipePos : pipes) {
                if (!(level.getBlockEntity(pipePos) instanceof SteamItemPipeBlockEntity pipe)) {
                    continue;
                }
                for (Direction direction : DIRECTIONS) {
                    if (!PipeModeRules.connects(pipe.getMode(direction))) {
                        continue;
                    }
                    BlockPos intakePos = pipePos.relative(direction);
                    if (!intakes.add(intakePos)) {
                        continue;
                    }
                    if (level.getBlockEntity(intakePos) instanceof SteamIntakeBlockEntity intake
                            && intake.canConnectToPipe(direction.getOpposite())) {
                        steam += intake.getSteamProductionPerTick();
                    }
                }
            }
            return steam;
        }

        private Map<BlockPos, Integer> distancesFrom(ServerLevel level, BlockPos start) {
            Map<BlockPos, Integer> distances = new HashMap<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            distances.put(start, 0);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos current = queue.remove();
                int nextDistance = distances.get(current) + 1;
                for (Direction direction : DIRECTIONS) {
                    if (!canTravel(level, current, direction)) {
                        continue;
                    }
                    BlockPos next = current.relative(direction);
                    if (!pipes.contains(next) || distances.containsKey(next)) {
                        continue;
                    }
                    distances.put(next, nextDistance);
                    queue.add(next);
                }
            }
            return distances;
        }

        private static boolean canTravel(ServerLevel level, BlockPos pos, Direction direction) {
            if (!(level.getBlockEntity(pos) instanceof SteamItemPipeBlockEntity pipe)) {
                return false;
            }
            if (!PipeModeRules.connects(pipe.getMode(direction))) {
                return false;
            }
            BlockPos neighbor = pos.relative(direction);
            if (!(level.getBlockEntity(neighbor) instanceof SteamItemPipeBlockEntity other)) {
                return false;
            }
            return PipeModeRules.connects(other.getMode(direction.getOpposite()));
        }

        private static boolean isPipeOrIntake(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(ModBlocks.STEAM_ITEM_PIPE.get()) || state.is(ModBlocks.STEAM_INTAKE.get());
        }
    }
}

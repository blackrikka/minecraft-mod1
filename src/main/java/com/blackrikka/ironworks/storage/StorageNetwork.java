package com.blackrikka.ironworks.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The storage providers reachable from one node, found by a breadth-first search over network nodes
 * (network cables, server racks, storage ports). Server side only.
 * <p>
 * Two nodes are linked when both are nodes and each cable on the pair has the face toward the other connected.
 * Racks and ports link to any adjacent node directly. Only loaded chunks are searched, and at most
 * {@link #MAX_NODES} nodes are visited.
 */
public final class StorageNetwork {
    public static final int MAX_NODES = 256;

    /** A network with no nodes. */
    public static final StorageNetwork EMPTY = new StorageNetwork(List.of(), 0);

    private final List<IStorageProvider> providers;
    private final int nodeCount;

    private StorageNetwork(List<IStorageProvider> providers, int nodeCount) {
        this.providers = providers;
        this.nodeCount = nodeCount;
    }

    /** The network that contains {@code start}, or {@link #EMPTY} if {@code start} is not a network node. */
    public static StorageNetwork collect(Level level, BlockPos start) {
        if (!level.hasChunkAt(start) || !NetworkCableBlock.isNetworkNode(level.getBlockState(start))) {
            return EMPTY;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<IStorageProvider> providers = new ArrayList<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack) {
                providers.add(rack);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || visited.size() >= MAX_NODES || !level.hasChunkAt(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (!NetworkCableBlock.isNetworkNode(nextState)
                        || !isOpen(state, direction)
                        || !isOpen(nextState, direction.getOpposite())) {
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }
        return new StorageNetwork(List.copyOf(providers), visited.size());
    }

    /** A cable face is open only if it is connected; racks and ports are open on every face. */
    private static boolean isOpen(BlockState state, Direction direction) {
        return !(state.getBlock() instanceof NetworkCableBlock) || NetworkCableBlock.isConnected(state, direction);
    }

    public List<IStorageProvider> providers() {
        return providers;
    }

    /** Number of nodes (cables, racks, ports) in the network. */
    public int nodeCount() {
        return nodeCount;
    }

    /** True if at least one provider is online. */
    public boolean isOnline() {
        for (IStorageProvider provider : providers) {
            if (provider.isOnline()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts into the network. Racks that already hold the kind get the items first, so one kind is not spread over
     * several racks while another rack has room for it.
     *
     * @return the amount accepted
     */
    public long insert(ItemStack template, long count, boolean simulate) {
        if (template.isEmpty() || count <= 0) {
            return 0;
        }
        ItemKey key = new ItemKey(template);
        long left = count;
        for (IStorageProvider provider : providers) {
            if (left <= 0) {
                break;
            }
            if (provider.isOnline() && provider.snapshot().containsKey(key)) {
                left -= provider.insert(template, left, simulate);
            }
        }
        for (IStorageProvider provider : providers) {
            if (left <= 0) {
                break;
            }
            if (provider.isOnline() && !provider.snapshot().containsKey(key)) {
                left -= provider.insert(template, left, simulate);
            }
        }
        return count - left;
    }

    /** Removes from the network, provider by provider. Returns the amount removed. */
    public long extract(ItemStack template, long count, boolean simulate) {
        if (template.isEmpty() || count <= 0) {
            return 0;
        }
        long left = count;
        for (IStorageProvider provider : providers) {
            if (left <= 0) {
                break;
            }
            left -= provider.extract(template, left, simulate);
        }
        return count - left;
    }

    /** Totals per kind over all providers. */
    public Map<ItemKey, Long> snapshot() {
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        for (IStorageProvider provider : providers) {
            provider.snapshot().forEach((key, count) -> totals.merge(key, count, Long::sum));
        }
        return totals;
    }
}

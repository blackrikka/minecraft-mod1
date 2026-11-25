package com.blackrikka.ironworks.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Shared logic of the belt blocks (conveyor, splitter, sorter). Each block buffers exactly one item.
 * <p>
 * An item enters through {@link #acceptFrom} from any face except the front, or is pulled every
 * {@value #PULL_INTERVAL} ticks from an {@link ItemEntity} lying on the block or from the block behind it.
 * After {@link #MOVE_TICKS} ticks the item is handed to {@link #deliver}; if the target refuses it, the item
 * stays in the buffer and the block waits.
 * <p>
 * Progress is not stored per tick. The buffer records the game time it was entered ({@code enteredTick}), so the
 * client can animate the item from the synced value without a sync every tick.
 */
public abstract class AbstractBeltBlockEntity extends BlockEntity {
    /** Ticks an item needs to cross one block. */
    public static final int MOVE_TICKS = 8;
    /** Ticks between pull attempts while the buffer is empty. */
    private static final int PULL_INTERVAL = 8;

    private ItemStack item = ItemStack.EMPTY;
    private long enteredTick;
    private int pullTimer;
    /** Cursor for blocks that alternate between several outputs (splitter, sorter). */
    protected int roundRobin;

    private final IItemHandler anySideView = new BeltItemView(null);
    private final IItemHandler[] sideViews = new IItemHandler[Direction.values().length];

    protected AbstractBeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction direction : Direction.values()) {
            sideViews[direction.ordinal()] = new BeltItemView(direction);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractBeltBlockEntity be) {
        be.tick(level, pos, state);
    }

    /**
     * Hands {@code stack} (one item) to the block's output(s). Called once the item has crossed the block.
     *
     * @return true if the item was accepted; false keeps it in the buffer for the next tick
     */
    protected abstract boolean deliver(Level level, BlockPos pos, Direction facing, ItemStack stack);

    /**
     * Offers one item to the neighbour in {@code direction}. A belt neighbour takes it into its buffer; any other
     * block takes it through {@code Capabilities.ItemHandler.BLOCK}.
     */
    protected final boolean offer(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        BlockPos target = pos.relative(direction);
        // The face of the target that touches this block.
        Direction side = direction.getOpposite();
        if (level.getBlockEntity(target) instanceof AbstractBeltBlockEntity belt) {
            return belt.acceptFrom(side, stack);
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, side);
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack.copy(), false).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts one item entering through {@code side} (a face of this block, null if unknown).
     * Rejects when the buffer is full or the item comes through the front face.
     */
    public boolean acceptFrom(@Nullable Direction side, ItemStack stack) {
        Level level = getLevel();
        if (level == null || level.isClientSide || stack.isEmpty() || !canAcceptFrom(side)) {
            return false;
        }
        startItem(level, stack);
        return true;
    }

    private boolean canAcceptFrom(@Nullable Direction side) {
        return item.isEmpty() && (side == null || side != getFacing());
    }

    /** The item handler view used by neighbours. {@code side} is the face of this block they touch. */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == null ? anySideView : sideViews[side.ordinal()];
    }

    /** Item in the buffer (empty if none). Clients read it for rendering; do not modify the returned stack. */
    public ItemStack getItem() {
        return item;
    }

    /** Game time when the buffered item entered this block. */
    public long getEnteredTick() {
        return enteredTick;
    }

    /** Horizontal direction the items flow toward. */
    public Direction getFacing() {
        return getBlockState().getValue(AbstractBeltBlock.FACING);
    }

    /** Drops the buffered item; called when the block is broken. */
    public void dropContents(Level level, BlockPos pos) {
        if (!item.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, item);
        }
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (item.isEmpty()) {
            pullTimer++;
            if (pullTimer >= PULL_INTERVAL) {
                pullTimer = 0;
                pull(level, pos, state.getValue(AbstractBeltBlock.FACING));
            }
            return;
        }
        if (level.getGameTime() - enteredTick < MOVE_TICKS) {
            return;
        }
        if (deliver(level, pos, state.getValue(AbstractBeltBlock.FACING), item.copy())) {
            item = ItemStack.EMPTY;
            // Pull again on the next tick so a line of belts keeps moving at one item per MOVE_TICKS.
            pullTimer = PULL_INTERVAL - 1;
            syncToClients();
        }
    }

    /** Takes one item from a dropped ItemEntity on this block, or from the block behind (never from a belt). */
    private void pull(Level level, BlockPos pos, Direction facing) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos))) {
            ItemStack stack = entity.getItem();
            if (!entity.isAlive() || stack.isEmpty()) {
                continue;
            }
            ItemStack taken = stack.copyWithCount(1);
            if (stack.getCount() <= 1) {
                entity.discard();
            } else {
                entity.setItem(stack.copyWithCount(stack.getCount() - 1));
            }
            startItem(level, taken);
            return;
        }

        BlockPos behind = pos.relative(facing.getOpposite());
        // Belts push their own items forward, so they are never a pull source.
        if (level.getBlockEntity(behind) instanceof AbstractBeltBlockEntity) {
            return;
        }
        // The source's face that touches this block is the one facing us, i.e. `facing`.
        IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, behind, facing);
        if (source == null) {
            return;
        }
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack taken = source.extractItem(slot, 1, false);
            if (!taken.isEmpty()) {
                startItem(level, taken);
                return;
            }
        }
    }

    private void startItem(Level level, ItemStack stack) {
        item = stack.copyWithCount(1);
        enteredTick = level.getGameTime();
        pullTimer = 0;
        syncToClients();
    }

    /** Sends the buffer to clients. Only called when the buffer changes, not every tick. */
    private void syncToClients() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) {
            tag.put("item", item.saveOptional(registries));
        }
        tag.putLong("enteredTick", enteredTick);
        tag.putInt("roundRobin", roundRobin);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = ItemStack.parseOptional(registries, tag.getCompound("item"));
        enteredTick = tag.getLong("enteredTick");
        roundRobin = tag.getInt("roundRobin");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /**
     * Item view for neighbours: accepts one item through every face except the front, never extracts.
     * {@code side} is the face of this block that the neighbour touches (null when unknown).
     */
    private final class BeltItemView implements IItemHandler {
        @Nullable
        private final Direction side;

        private BeltItemView(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? item.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !canAcceptFrom(side)) {
                return stack;
            }
            if (!simulate && !acceptFrom(side, stack)) {
                return stack;
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }
    }
}

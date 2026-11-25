package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.energy.ModEnergyStorage;
import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Server rack: holds {@value #SLOTS} drives and stores items for the network it is part of.
 * <p>
 * Power: the buffer receives FE from cables only (receive-only view). Each server tick it pays
 * {@code 1 + 2 x drives} FE. The rack is online while the buffer covers that cost; otherwise it refuses inserts,
 * extracts and snapshots. The {@code online} block state mirrors this.
 * <p>
 * Insert order: drives that already hold the kind first, then drives with a free kind slot. Drive contents are
 * stored in the drive item itself, so removing a drive from the rack keeps its items.
 */
public class ServerRackBlockEntity extends BlockEntity implements MenuProvider, IStorageProvider {
    public static final int SLOTS = 8;
    public static final int CAPACITY = 5_000;
    public static final int RECEIVE_PER_TICK = 128;
    public static final int BASE_COST_PER_TICK = 1;
    public static final int DRIVE_COST_PER_TICK = 2;
    /** Values exposed through {@link #getDataAccess()}: energy, max energy, online (1 or 0). */
    public static final int DATA_COUNT = 3;

    private final ItemStackHandler drives = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isDrive(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, RECEIVE_PER_TICK, 0, this::setChanged);

    /** Result of the last server tick. */
    private boolean powered;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> powered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Values are only written by the server; the client never changes them.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_RACK.get(), pos, state);
    }

    /** Drive slot filter, shared with the client-side menu. */
    public static boolean isDrive(ItemStack stack) {
        return DriveItem.isDrive(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ServerRackBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        int cost = BASE_COST_PER_TICK + DRIVE_COST_PER_TICK * countDrives();
        boolean nowPowered = energy.getEnergyStored() >= cost;
        if (nowPowered) {
            energy.setEnergy(energy.getEnergyStored() - cost);
            setChanged();
        }
        powered = nowPowered;
        if (state.getValue(ServerRackBlock.ONLINE) != nowPowered) {
            // setBlock with UPDATE_ALL also sends the block update to clients.
            level.setBlock(pos, state.setValue(ServerRackBlock.ONLINE, nowPowered), Block.UPDATE_ALL);
        }
    }

    private int countDrives() {
        int count = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!drives.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isOnline() {
        return powered && !isRemoved();
    }

    @Override
    public long insert(ItemStack template, long count, boolean simulate) {
        if (template.isEmpty() || count <= 0 || !isOnline()) {
            return 0;
        }
        ItemKey key = new ItemKey(template);
        long left = count;
        // Drives that already hold the kind first, so one kind is not split over several drives.
        for (int slot = 0; slot < SLOTS && left > 0; slot++) {
            ItemStack drive = drives.getStackInSlot(slot);
            if (!drive.isEmpty() && DriveItem.holds(drive, key)) {
                left -= DriveItem.insert(drive, template, left, simulate);
            }
        }
        // Then drives with a free kind slot.
        for (int slot = 0; slot < SLOTS && left > 0; slot++) {
            ItemStack drive = drives.getStackInSlot(slot);
            if (!drive.isEmpty() && !DriveItem.holds(drive, key) && DriveItem.hasFreeKind(drive)) {
                left -= DriveItem.insert(drive, template, left, simulate);
            }
        }
        long accepted = count - left;
        if (accepted > 0 && !simulate) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extract(ItemStack template, long count, boolean simulate) {
        if (template.isEmpty() || count <= 0 || !isOnline()) {
            return 0;
        }
        ItemKey key = new ItemKey(template);
        long left = count;
        for (int slot = 0; slot < SLOTS && left > 0; slot++) {
            ItemStack drive = drives.getStackInSlot(slot);
            if (!drive.isEmpty()) {
                left -= DriveItem.extract(drive, key, left, simulate);
            }
        }
        long taken = count - left;
        if (taken > 0 && !simulate) {
            setChanged();
        }
        return taken;
    }

    @Override
    public Map<ItemKey, Long> snapshot() {
        Map<ItemKey, Long> totals = new LinkedHashMap<>();
        if (!isOnline()) {
            return totals;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack drive = drives.getStackInSlot(slot);
            if (!drive.isEmpty()) {
                DriveItem.addTotals(drive, totals);
            }
        }
        return totals;
    }

    /** Receive-only energy view for cables. */
    public ModEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getDrives() {
        return drives;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    /** Drops the drives (their contents stay on the items); called when the block is broken. */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = drives.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("drives", drives.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        drives.deserializeNBT(registries, tag.getCompound("drives"));
        energy.setEnergy(tag.getInt("energy"));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ServerRackMenu(ModMenus.SERVER_RACK_MENU.get(), containerId, playerInventory, drives,
                dataAccess, getBlockState().getBlock(), ContainerLevelAccess.create(level, worldPosition));
    }
}

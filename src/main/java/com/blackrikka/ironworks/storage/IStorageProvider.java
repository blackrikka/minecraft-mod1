package com.blackrikka.ironworks.storage;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * A block that stores items for a storage network (implemented by the server rack).
 * Counts are {@code long}: several drives can hold more than an int.
 */
public interface IStorageProvider {
    /**
     * Stores up to {@code count} items of {@code template}'s kind.
     *
     * @return the amount accepted (not more than {@code count}); nothing is changed when {@code simulate} is true
     */
    long insert(ItemStack template, long count, boolean simulate);

    /**
     * Removes up to {@code count} items of {@code template}'s kind.
     *
     * @return the amount removed; nothing is changed when {@code simulate} is true
     */
    long extract(ItemStack template, long count, boolean simulate);

    /** Amount stored per kind. Empty while offline. */
    Map<ItemKey, Long> snapshot();

    /** False while the provider has no power or has been removed. */
    boolean isOnline();
}

package com.blackrikka.ironworks.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Identity of one storage kind: the item plus its data components (count ignored).
 * Two stacks with the same item and components are the same kind.
 */
public final class ItemKey {
    private final ItemStack stack;

    /** {@code template} must not be empty. The stored copy has count 1. */
    public ItemKey(ItemStack template) {
        this.stack = template.copyWithCount(1);
    }

    /** A single-item copy of this kind. */
    public ItemStack stack() {
        return stack.copy();
    }

    /** True if {@code other} is the same kind (count is ignored). */
    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameComponents(stack, other);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ItemKey other && matches(other.stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}

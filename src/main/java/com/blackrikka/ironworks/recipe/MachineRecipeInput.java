package com.blackrikka.ironworks.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of a machine's input slots, used for recipe lookup.
 * Stacks are copied so that recipe matching never touches the live inventory.
 */
public final class MachineRecipeInput implements RecipeInput {
    private final List<ItemStack> items;

    public MachineRecipeInput(List<ItemStack> items) {
        this.items = List.copyOf(items);
    }

    /** Copies slots {@code [0, inputSlots)} of the handler. */
    public static MachineRecipeInput of(IItemHandler handler, int inputSlots) {
        List<ItemStack> copies = new ArrayList<>(inputSlots);
        for (int i = 0; i < inputSlots; i++) {
            copies.add(handler.getStackInSlot(i).copy());
        }
        return new MachineRecipeInput(copies);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}

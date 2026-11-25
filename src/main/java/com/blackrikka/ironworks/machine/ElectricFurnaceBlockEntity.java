package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Electric furnace: one input, one output, 20 FE/tick, fixed 100 ticks per item.
 * Uses the vanilla {@link RecipeType#SMELTING} recipes, so any smelting recipe (mod or vanilla) works.
 */
public class ElectricFurnaceBlockEntity extends MachineBlockEntity {
    public static final int TIME = 100;
    public static final int ENERGY_PER_TICK = 20;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state, 1);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        ItemStack input = getInventory().getStackInSlot(0);
        if (input.isEmpty()) {
            return null;
        }
        Optional<RecipeHolder<SmeltingRecipe>> holder =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        if (holder.isEmpty()) {
            return null;
        }
        ItemStack result = holder.get().value().getResultItem(level.registryAccess());
        return new Craft(result, TIME, ENERGY_PER_TICK, new int[] {0}, new int[] {1});
    }

    @Override
    protected MenuType<?> menuType() {
        return ModMenus.ELECTRIC_FURNACE_MENU.get();
    }
}

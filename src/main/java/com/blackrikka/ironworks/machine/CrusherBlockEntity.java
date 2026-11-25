package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.recipe.MachineRecipe;
import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModMenus;
import com.blackrikka.ironworks.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Crusher: one input slot, one output slot, 20 FE/tick. Recipes come from {@code ironworks:crushing}. */
public class CrusherBlockEntity extends MachineBlockEntity {
    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state, 1);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.CRUSHING.get())
                .stream()
                .anyMatch(holder -> holder.value().acceptsItem(stack));
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        return findMachineCraft(level, ModRecipeTypes.CRUSHING.get());
    }

    @Override
    protected MenuType<?> menuType() {
        return ModMenus.CRUSHER_MENU.get();
    }
}

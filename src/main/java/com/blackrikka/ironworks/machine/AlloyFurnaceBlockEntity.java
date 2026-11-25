package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModMenus;
import com.blackrikka.ironworks.registry.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Alloy furnace: two input slots, one output slot, 30 FE/tick. Recipes come from {@code ironworks:alloying}. */
public class AlloyFurnaceBlockEntity extends MachineBlockEntity {
    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_FURNACE.get(), pos, state, 2);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return level != null && acceptsRecipeInput(level, ModRecipeTypes.ALLOYING.get(), stack);
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        return findMachineCraft(level, ModRecipeTypes.ALLOYING.get());
    }

    @Override
    protected MenuType<?> menuType() {
        return ModMenus.ALLOY_FURNACE_MENU.get();
    }
}

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

/**
 * Circuit assembler: four input slots (2x2 in the GUI), one output slot. Base 40 FE/tick; each recipe sets its own
 * energy per tick. Recipes come from {@code ironworks:circuit_assembly}.
 */
public class CircuitAssemblerBlockEntity extends MachineBlockEntity {
    public CircuitAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CIRCUIT_ASSEMBLER.get(), pos, state, 4);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return level != null && acceptsRecipeInput(level, ModRecipeTypes.CIRCUIT_ASSEMBLY.get(), stack);
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        return findMachineCraft(level, ModRecipeTypes.CIRCUIT_ASSEMBLY.get());
    }

    @Override
    protected MenuType<?> menuType() {
        return ModMenus.CIRCUIT_ASSEMBLER_MENU.get();
    }
}

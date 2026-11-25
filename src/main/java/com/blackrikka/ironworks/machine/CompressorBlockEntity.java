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

/** Compressor: one input slot, one output slot, 30 FE/tick. Recipes come from {@code ironworks:compressing}. */
public class CompressorBlockEntity extends MachineBlockEntity {
    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR.get(), pos, state, 1);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return level != null && acceptsRecipeInput(level, ModRecipeTypes.COMPRESSING.get(), stack);
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        return findMachineCraft(level, ModRecipeTypes.COMPRESSING.get());
    }

    @Override
    protected MenuType<?> menuType() {
        return ModMenus.COMPRESSOR_MENU.get();
    }
}

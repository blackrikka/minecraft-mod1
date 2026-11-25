package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Alloy furnace: two inputs, one output, 30 FE/tick. Recipes come from ironworks:alloying. */
public class AlloyFurnaceBlock extends MachineBlock {
    public AlloyFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AlloyFurnaceBlock> codec() {
        return simpleCodec(AlloyFurnaceBlock::new);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> machineType() {
        return ModBlockEntities.ALLOY_FURNACE.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlloyFurnaceBlockEntity(pos, state);
    }
}

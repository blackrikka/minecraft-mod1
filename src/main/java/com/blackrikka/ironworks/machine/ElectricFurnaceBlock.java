package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Electric furnace: smelts with vanilla smelting recipes, powered by FE instead of fuel. */
public class ElectricFurnaceBlock extends MachineBlock {
    public ElectricFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ElectricFurnaceBlock> codec() {
        return simpleCodec(ElectricFurnaceBlock::new);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> machineType() {
        return ModBlockEntities.ELECTRIC_FURNACE.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }
}

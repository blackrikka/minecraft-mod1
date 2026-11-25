package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Circuit assembler: four inputs (2x2), one output, 40 FE/tick base. Recipes come from ironworks:circuit_assembly. */
public class CircuitAssemblerBlock extends MachineBlock {
    public CircuitAssemblerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CircuitAssemblerBlock> codec() {
        return simpleCodec(CircuitAssemblerBlock::new);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> machineType() {
        return ModBlockEntities.CIRCUIT_ASSEMBLER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircuitAssemblerBlockEntity(pos, state);
    }
}

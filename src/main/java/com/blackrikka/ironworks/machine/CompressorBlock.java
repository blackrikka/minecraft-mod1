package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Compressor: one input, one output, 30 FE/tick. Recipes come from ironworks:compressing. */
public class CompressorBlock extends MachineBlock {
    public CompressorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CompressorBlock> codec() {
        return simpleCodec(CompressorBlock::new);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> machineType() {
        return ModBlockEntities.COMPRESSOR.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressorBlockEntity(pos, state);
    }
}

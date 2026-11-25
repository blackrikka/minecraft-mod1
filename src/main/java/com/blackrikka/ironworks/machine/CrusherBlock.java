package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Crusher: grinds ores into dust using {@code ironworks:crushing} recipes. */
public class CrusherBlock extends MachineBlock {
    public CrusherBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CrusherBlock> codec() {
        return simpleCodec(CrusherBlock::new);
    }

    @Override
    protected BlockEntityType<? extends MachineBlockEntity> machineType() {
        return ModBlockEntities.CRUSHER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }
}

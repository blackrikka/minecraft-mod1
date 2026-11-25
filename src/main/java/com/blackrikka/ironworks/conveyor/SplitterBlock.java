package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Splitter: sends each item to the front, left or right in turn, skipping outputs that refuse it. */
public class SplitterBlock extends AbstractBeltBlock {
    public SplitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SplitterBlock> codec() {
        return simpleCodec(SplitterBlock::new);
    }

    @Override
    protected BlockEntityType<? extends AbstractBeltBlockEntity> beltType() {
        return ModBlockEntities.SPLITTER.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SplitterBlockEntity(pos, state);
    }
}

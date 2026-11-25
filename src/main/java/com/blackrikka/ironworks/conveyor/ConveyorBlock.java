package com.blackrikka.ironworks.conveyor;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Conveyor: moves one item per {@value AbstractBeltBlockEntity#MOVE_TICKS} ticks straight ahead. */
public class ConveyorBlock extends AbstractBeltBlock {
    public ConveyorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ConveyorBlock> codec() {
        return simpleCodec(ConveyorBlock::new);
    }

    @Override
    protected BlockEntityType<? extends AbstractBeltBlockEntity> beltType() {
        return ModBlockEntities.CONVEYOR.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }
}

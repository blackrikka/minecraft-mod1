package com.blackrikka.ironworks.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Development-only FE sink. Accepts any amount of FE and never gives it back.
 * Right-clicking shows the total amount received so far in chat.
 */
public class CreativeBatteryBlock extends BaseEntityBlock {
    public CreativeBatteryBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CreativeBatteryBlock> codec() {
        return simpleCodec(CreativeBatteryBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeBatteryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CreativeBatteryBlockEntity be) {
            player.displayClientMessage(
                    Component.translatable("message.ironworks.creative_battery.total",
                            String.format(Locale.ROOT, "%,d", be.getTotalReceived())),
                    false);
        }
        return InteractionResult.SUCCESS;
    }
}

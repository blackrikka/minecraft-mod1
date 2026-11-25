package com.blackrikka.ironworks.conveyor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Client-only renderer for the belt buffers. Draws the buffered item on top of the plate, sliding from the back
 * edge to the front edge over {@link AbstractBeltBlockEntity#MOVE_TICKS} ticks. Registered in
 * {@code client.ClientModEvents} for the conveyor, splitter and sorter.
 */
public class ConveyorRenderer implements BlockEntityRenderer<AbstractBeltBlockEntity> {
    /** Height of the top of the plate (4/16) plus a small offset so the item does not z-fight with it. */
    private static final double ITEM_HEIGHT = 0.26D;
    private static final float ITEM_SCALE = 0.4F;

    public ConveyorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AbstractBeltBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = be.getItem();
        Level level = be.getLevel();
        if (stack.isEmpty() || level == null) {
            return;
        }
        float progress = Mth.clamp(
                (level.getGameTime() - be.getEnteredTick() + partialTick) / (float) AbstractBeltBlockEntity.MOVE_TICKS,
                0.0F, 1.0F);
        Direction facing = be.getFacing();
        double along = progress - 0.5D;

        poseStack.pushPose();
        // The block origin is the render origin, so the block centre is at (0.5, _, 0.5).
        poseStack.translate(0.5D + facing.getStepX() * along, ITEM_HEIGHT, 0.5D + facing.getStepZ() * along);
        // Lay the flat item sprite on the plate.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, level,
                (int) be.getBlockPos().asLong());
        poseStack.popPose();
    }
}

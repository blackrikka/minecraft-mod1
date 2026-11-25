package com.blackrikka.ironworks.machine;

import com.blackrikka.ironworks.energy.EnergyTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Untextured GUI drawn with {@link GuiGraphics#fill}: panel, fuel slot, flame gauge and energy bar.
 * Hovering the energy bar shows the stored amount.
 */
public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {
    private static final int ENERGY_X = 152;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_W = 12;
    private static final int ENERGY_H = 54;

    private static final int FLAME_X = 80;
    private static final int FLAME_Y = 56;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 14;

    private static final int COLOR_BORDER = 0xFF373737;
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_GAUGE_BACK = 0xFF555555;
    private static final int COLOR_FLAME = 0xFFFF8A1F;
    private static final int COLOR_BAR_BACK = 0xFF2B2B2B;
    private static final int COLOR_ENERGY = 0xFF30D050;

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    EnergyTooltip.format(menu.getEnergy(), menu.getMaxEnergy()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel with a 1px border.
        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, COLOR_PANEL);

        // Fuel slot (slot origin is 80,35; the 18x18 background starts one pixel earlier).
        drawSlot(graphics, x + 79, y + 34);

        // Player inventory and hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + 141);
        }

        drawFlame(graphics, x + FLAME_X, y + FLAME_Y);
        drawEnergyBar(graphics, x + ENERGY_X, y + ENERGY_Y);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, COLOR_SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, COLOR_SLOT);
    }

    /** Flame gauge: fills from the bottom as the current fuel item burns down. */
    private void drawFlame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + FLAME_W, y + FLAME_H, COLOR_GAUGE_BACK);
        int burn = menu.getBurnTime();
        int total = menu.getBurnTotal();
        if (burn > 0 && total > 0) {
            int h = Math.min(FLAME_H, burn * FLAME_H / total);
            graphics.fill(x, y + FLAME_H - h, x + FLAME_W, y + FLAME_H, COLOR_FLAME);
        }
    }

    /** Energy bar: fills from the bottom according to stored / max. */
    private void drawEnergyBar(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + ENERGY_W + 1, y + ENERGY_H + 1, COLOR_BORDER);
        graphics.fill(x, y, x + ENERGY_W, y + ENERGY_H, COLOR_BAR_BACK);
        int max = menu.getMaxEnergy();
        if (max > 0) {
            int h = (int) ((long) menu.getEnergy() * ENERGY_H / max);
            graphics.fill(x, y + ENERGY_H - h, x + ENERGY_W, y + ENERGY_H, COLOR_ENERGY);
        }
    }
}

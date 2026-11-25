package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.energy.EnergyTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Untextured server rack GUI (same look as {@code MachineScreen}): 8 drive slots, player inventory, energy bar,
 * and an online/offline label at the left. Hovering the energy bar shows the stored amount.
 */
public class ServerRackScreen extends AbstractContainerScreen<ServerRackMenu> {
    private static final int ENERGY_X = 152;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_W = 12;
    private static final int ENERGY_H = 54;

    private static final int COLOR_BORDER = 0xFF373737;
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_BAR_BACK = 0xFF2B2B2B;
    private static final int COLOR_ENERGY = 0xFF30D050;
    private static final int COLOR_ONLINE = 0xFF55FF55;
    private static final int COLOR_OFFLINE = 0xFFFF5555;

    public ServerRackScreen(ServerRackMenu menu, Inventory inventory, Component title) {
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.isOnline()) {
            graphics.drawString(this.font, Component.translatable("gui.ironworks.server_rack.online"),
                    8, 28, COLOR_ONLINE, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.ironworks.server_rack.offline"),
                    8, 28, COLOR_OFFLINE, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel with a 1px border.
        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, COLOR_PANEL);

        // Drive slots (the slot origin is one pixel inside the 18x18 background).
        for (int i = 0; i < 8; i++) {
            drawSlot(graphics, x + ServerRackMenu.SLOT_X + (i % ServerRackMenu.COLUMNS) * 18 - 1,
                    y + ServerRackMenu.SLOT_Y + (i / ServerRackMenu.COLUMNS) * 18 - 1);
        }

        // Player inventory and hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + ServerRackMenu.PLAYER_INVENTORY_Y - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + ServerRackMenu.HOTBAR_Y - 1);
        }

        drawEnergyBar(graphics, x + ENERGY_X, y + ENERGY_Y);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, COLOR_SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, COLOR_SLOT);
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

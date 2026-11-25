package com.blackrikka.ironworks.energy;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Formats FE amounts for tooltips, e.g. {@code 1,234 / 10,000 FE}. */
public final class EnergyTooltip {
    private EnergyTooltip() {
    }

    public static Component format(int stored, int max) {
        return Component.literal(String.format(Locale.ROOT, "%,d / %,d FE", stored, max));
    }
}

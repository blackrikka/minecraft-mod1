package com.blackrikka.ironworks.storage;

import com.blackrikka.ironworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Storage drive. Stacks to 1; its contents live in the {@code drive_data} component, so they stay with the item
 * when the drive is removed from a rack. Static helpers below work on the drive stack itself.
 */
public class DriveItem extends Item {
    /** Storage tiers: total items and maximum number of kinds. */
    public enum Tier {
        K1(1_024, 16),
        K4(4_096, 32),
        K16(16_384, 63);

        private final long capacity;
        private final int maxKinds;

        Tier(long capacity, int maxKinds) {
            this.capacity = capacity;
            this.maxKinds = maxKinds;
        }

        public long capacity() {
            return capacity;
        }

        public int maxKinds() {
            return maxKinds;
        }
    }

    private static final int TOOLTIP_ENTRIES = 3;

    private final Tier tier;

    public DriveItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    /** Contents of {@code drive}, or {@link DriveData#EMPTY}. */
    public static DriveData dataOf(ItemStack drive) {
        return drive.getOrDefault(ModDataComponents.DRIVE_DATA.get(), DriveData.EMPTY);
    }

    /** True if {@code drive} is a drive item. */
    public static boolean isDrive(ItemStack stack) {
        return stack.getItem() instanceof DriveItem;
    }

    /** True if {@code drive} already holds {@code key}. */
    public static boolean holds(ItemStack drive, ItemKey key) {
        return dataOf(drive).countOf(key) > 0;
    }

    /** True if {@code drive} has room for one more kind and at least one more item. */
    public static boolean hasFreeKind(ItemStack drive) {
        if (!(drive.getItem() instanceof DriveItem item)) {
            return false;
        }
        DriveData data = dataOf(drive);
        return data.entries().size() < item.tier.maxKinds && data.used() < item.tier.capacity;
    }

    /**
     * Stores up to {@code amount} of {@code template}'s kind. A kind that is not yet in the drive needs a free kind
     * slot. Returns the amount accepted, which is limited by the free capacity.
     */
    public static long insert(ItemStack drive, ItemStack template, long amount, boolean simulate) {
        if (!(drive.getItem() instanceof DriveItem item) || amount <= 0 || template.isEmpty()) {
            return 0;
        }
        DriveData data = dataOf(drive);
        ItemKey key = new ItemKey(template);
        if (data.countOf(key) == 0 && data.entries().size() >= item.tier.maxKinds) {
            return 0;
        }
        long accepted = Math.min(amount, item.tier.capacity - data.used());
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            drive.set(ModDataComponents.DRIVE_DATA.get(), data.plus(template, accepted));
        }
        return accepted;
    }

    /** Removes up to {@code amount} of {@code key} from {@code drive}. Returns the amount removed. */
    public static long extract(ItemStack drive, ItemKey key, long amount, boolean simulate) {
        if (amount <= 0 || !(drive.getItem() instanceof DriveItem)) {
            return 0;
        }
        DriveData data = dataOf(drive);
        long taken = Math.min(amount, data.countOf(key));
        if (taken <= 0) {
            return 0;
        }
        if (!simulate) {
            drive.set(ModDataComponents.DRIVE_DATA.get(), data.minus(key, taken));
        }
        return taken;
    }

    /** Adds the drive's contents to {@code totals}. */
    public static void addTotals(ItemStack drive, Map<ItemKey, Long> totals) {
        for (DriveData.Entry entry : dataOf(drive).entries()) {
            totals.merge(new ItemKey(entry.template()), entry.count(), Long::sum);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DriveData data = dataOf(stack);
        tooltip.add(Component.translatable("tooltip.ironworks.drive.usage",
                        format(data.used()), format(tier.capacity()), data.entries().size(), tier.maxKinds())
                .withStyle(ChatFormatting.GRAY));

        List<DriveData.Entry> largest = data.entries().stream()
                .sorted(Comparator.comparingLong((DriveData.Entry entry) -> entry.count()).reversed())
                .toList();
        for (int i = 0; i < Math.min(TOOLTIP_ENTRIES, largest.size()); i++) {
            DriveData.Entry entry = largest.get(i);
            tooltip.add(Component.translatable("tooltip.ironworks.drive.entry",
                            entry.template().getHoverName(), format(entry.count()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (largest.size() > TOOLTIP_ENTRIES) {
            tooltip.add(Component.translatable("tooltip.ironworks.drive.more", largest.size() - TOOLTIP_ENTRIES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}

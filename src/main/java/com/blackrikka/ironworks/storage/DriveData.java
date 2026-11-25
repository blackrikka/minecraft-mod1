package com.blackrikka.ironworks.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contents of one drive: one {@link Entry} per item kind, in insertion order. Immutable; changes produce a new record
 * that is written back into the drive stack's {@code drive_data} component.
 */
public record DriveData(List<Entry> entries) {
    public static final DriveData EMPTY = new DriveData(List.of());

    public DriveData {
        entries = List.copyOf(entries);
    }

    /** One item kind. {@code template} is a single item carrying the kind's components; {@code count} is the amount stored. */
    public record Entry(ItemStack template, long count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("template").forGetter(Entry::template),
                Codec.LONG.fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::template,
                ByteBufCodecs.VAR_LONG, Entry::count,
                Entry::new);

        /** ItemStack has identity equality, so entries compare by item and components instead. */
        @Override
        public boolean equals(Object o) {
            return o instanceof Entry other
                    && count == other.count
                    && ItemStack.isSameItemSameComponents(template, other.template);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ItemStack.hashItemAndComponents(template), count);
        }
    }

    public static final Codec<DriveData> CODEC = Entry.CODEC.listOf().xmap(DriveData::new, DriveData::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, DriveData> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, Entry, List<Entry>>collection(ArrayList::new, Entry.STREAM_CODEC)
                    .map(DriveData::new, DriveData::entries);

    /** Total amount stored, summed over all kinds. */
    public long used() {
        long total = 0;
        for (Entry entry : entries) {
            total += entry.count();
        }
        return total;
    }

    /** Amount stored of {@code key}, 0 if the kind is absent. */
    public long countOf(ItemKey key) {
        for (Entry entry : entries) {
            if (key.matches(entry.template())) {
                return entry.count();
            }
        }
        return 0;
    }

    /** A copy with {@code amount} more of {@code template}'s kind. A new kind is appended at the end. */
    public DriveData plus(ItemStack template, long amount) {
        ItemKey key = new ItemKey(template);
        List<Entry> next = new ArrayList<>(entries);
        for (int i = 0; i < next.size(); i++) {
            Entry entry = next.get(i);
            if (key.matches(entry.template())) {
                next.set(i, new Entry(entry.template(), entry.count() + amount));
                return new DriveData(next);
            }
        }
        next.add(new Entry(template.copyWithCount(1), amount));
        return new DriveData(next);
    }

    /** A copy with {@code amount} less of {@code key}. The kind is removed when its count reaches 0. */
    public DriveData minus(ItemKey key, long amount) {
        List<Entry> next = new ArrayList<>(entries);
        for (int i = 0; i < next.size(); i++) {
            Entry entry = next.get(i);
            if (key.matches(entry.template())) {
                long left = entry.count() - amount;
                if (left > 0) {
                    next.set(i, new Entry(entry.template(), left));
                } else {
                    next.remove(i);
                }
                return new DriveData(next);
            }
        }
        return this;
    }
}

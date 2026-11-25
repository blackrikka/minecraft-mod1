package com.blackrikka.ironworks.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared recipe for Ironworks machines (crushing, alloying, ...). The concrete recipe type and serializer
 * are supplied by {@link MachineRecipeSerializer}, so each machine type only differs in its registration.
 * <p>
 * Matching is order-independent: every entry must be satisfied by a distinct input slot holding at least
 * {@code count} items of the entry's ingredient, and every non-empty input slot must be used by an entry.
 */
public class MachineRecipe implements Recipe<MachineRecipeInput> {
    /** One required input: {@code count} items matching {@code ingredient}. */
    public record Entry(Ingredient ingredient, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Entry::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Entry::ingredient,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new);
    }

    private final RecipeType<MachineRecipe> type;
    private final RecipeSerializer<MachineRecipe> serializer;
    private final List<Entry> entries;
    private final ItemStack result;
    private final int time;
    private final int energyPerTick;

    public MachineRecipe(RecipeType<MachineRecipe> type, RecipeSerializer<MachineRecipe> serializer,
                         List<Entry> entries, ItemStack result, int time, int energyPerTick) {
        this.type = type;
        this.serializer = serializer;
        this.entries = List.copyOf(entries);
        this.result = result.copy();
        this.time = time;
        this.energyPerTick = energyPerTick;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    /** Result stack; callers must copy before handing it out. */
    public ItemStack getResult() {
        return result;
    }

    /** Ticks needed to finish one craft. */
    public int getTime() {
        return time;
    }

    /** FE consumed per tick of progress. */
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    /** True if {@code stack} is an ingredient of any entry (used to validate hopper and player inserts). */
    public boolean acceptsItem(ItemStack stack) {
        for (Entry entry : entries) {
            if (entry.ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the input slot used by each entry, in entry order. Returns {@code null} when the recipe does not
     * match: too few/many filled slots, a slot too small for its entry, or an unused non-empty slot.
     */
    public int @Nullable [] findSlots(MachineRecipeInput input) {
        int slots = input.size();
        if (entries.size() > slots) {
            return null;
        }
        int[] assigned = new int[entries.size()];
        boolean[] used = new boolean[slots];
        if (!assign(0, input, assigned, used)) {
            return null;
        }
        for (int slot = 0; slot < slots; slot++) {
            if (!input.getItem(slot).isEmpty() && !used[slot]) {
                return null;
            }
        }
        return assigned;
    }

    private boolean assign(int entryIndex, MachineRecipeInput input, int[] assigned, boolean[] used) {
        if (entryIndex == entries.size()) {
            return true;
        }
        Entry entry = entries.get(entryIndex);
        for (int slot = 0; slot < input.size(); slot++) {
            if (used[slot]) {
                continue;
            }
            ItemStack stack = input.getItem(slot);
            if (stack.getCount() < entry.count() || !entry.ingredient().test(stack)) {
                continue;
            }
            used[slot] = true;
            assigned[entryIndex] = slot;
            if (assign(entryIndex + 1, input, assigned, used)) {
                return true;
            }
            used[slot] = false;
        }
        return false;
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        return findSlots(input) != null;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Entry entry : entries) {
            list.add(entry.ingredient());
        }
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }
}

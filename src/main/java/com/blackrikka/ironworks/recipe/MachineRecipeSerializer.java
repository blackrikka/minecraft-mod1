package com.blackrikka.ironworks.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;

/**
 * JSON and network serializer for {@link MachineRecipe}. One instance is registered per machine recipe type;
 * the supplier provides that type so that decoded recipes report it from {@code getType()}.
 *
 * <pre>
 * {
 *   "type": "ironworks:crushing",
 *   "ingredients": [ { "ingredient": { "item": "minecraft:iron_ore" }, "count": 1 } ],
 *   "result": { "id": "ironworks:iron_dust", "count": 2 },
 *   "time": 100,
 *   "energy_per_tick": 20
 * }
 * </pre>
 */
public final class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {
    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineRecipeSerializer(Supplier<RecipeType<MachineRecipe>> type) {
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.nonEmptyList(MachineRecipe.Entry.CODEC.listOf())
                        .fieldOf("ingredients").forGetter(MachineRecipe::getEntries),
                ItemStack.CODEC.fieldOf("result").forGetter(MachineRecipe::getResult),
                ExtraCodecs.POSITIVE_INT.fieldOf("time").forGetter(MachineRecipe::getTime),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("energy_per_tick").forGetter(MachineRecipe::getEnergyPerTick)
        ).apply(instance, (entries, result, time, energy) ->
                new MachineRecipe(type.get(), this, entries, result, time, energy)));

        this.streamCodec = StreamCodec.composite(
                MachineRecipe.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::getEntries,
                ItemStack.STREAM_CODEC, MachineRecipe::getResult,
                ByteBufCodecs.VAR_INT, MachineRecipe::getTime,
                ByteBufCodecs.VAR_INT, MachineRecipe::getEnergyPerTick,
                (entries, result, time, energy) -> new MachineRecipe(type.get(), this, entries, result, time, energy));
    }

    @Override
    public MapCodec<MachineRecipe> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
        return streamCodec;
    }
}

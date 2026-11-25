package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.recipe.MachineRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Machine recipe types. Add a new machine recipe by calling {@link #machineType(String)} and registering a
 * matching serializer in {@link ModRecipeSerializers}.
 */
public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Ironworks.MOD_ID);

    /** Crusher: ore to dust. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> CRUSHING = machineType("crushing");

    /** M1e: alloy furnace (two inputs to one alloy). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> ALLOYING = machineType("alloying");

    /** M1e: compressor (one input to plate, foil or wafer). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> COMPRESSING = machineType("compressing");

    /** M1e: circuit assembler (four inputs to a circuit). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> CIRCUIT_ASSEMBLY =
            machineType("circuit_assembly");

    private static DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> machineType(String name) {
        return RECIPE_TYPES.register(name,
                () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Ironworks.MOD_ID, name)));
    }
}

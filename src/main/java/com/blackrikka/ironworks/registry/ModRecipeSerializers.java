package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.recipe.MachineRecipe;
import com.blackrikka.ironworks.recipe.MachineRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Ironworks.MOD_ID);

    // M1d: crusher recipes (ironworks:crushing).
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> CRUSHING =
            machineSerializer("crushing", ModRecipeTypes.CRUSHING);

    // M1e: alloying, compressing, circuit_assembly recipes (same MachineRecipe class, separate types).
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> ALLOYING =
            machineSerializer("alloying", ModRecipeTypes.ALLOYING);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> COMPRESSING =
            machineSerializer("compressing", ModRecipeTypes.COMPRESSING);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> CIRCUIT_ASSEMBLY =
            machineSerializer("circuit_assembly", ModRecipeTypes.CIRCUIT_ASSEMBLY);

    private static DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>> machineSerializer(
            String name, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> type) {
        return RECIPE_SERIALIZERS.register(name, () -> new MachineRecipeSerializer(type::get));
    }
}

package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.machine.CombustionGeneratorMenu;
import com.blackrikka.ironworks.machine.MachineMenu;
import com.blackrikka.ironworks.storage.ServerRackMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Ironworks.MOD_ID);

    // M1b: combustion generator GUI.
    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR_MENU =
            MENUS.register("combustion_generator",
                    () -> IMenuTypeExtension.create(CombustionGeneratorMenu::new));

    // M1d: shared machine menu, one menu type per machine (the client needs the input slot count).
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CRUSHER_MENU =
            MENUS.register("crusher",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> MachineMenu.fromNetwork(
                            ModMenus.CRUSHER_MENU.get(), id, inventory, buf, ModBlocks.CRUSHER.get(), 1)));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> MachineMenu.fromNetwork(
                            ModMenus.ELECTRIC_FURNACE_MENU.get(), id, inventory, buf, ModBlocks.ELECTRIC_FURNACE.get(), 1)));

    // M1e: alloy furnace (2 inputs), compressor (1 input), circuit assembler (4 inputs).
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> ALLOY_FURNACE_MENU =
            MENUS.register("alloy_furnace",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> MachineMenu.fromNetwork(
                            ModMenus.ALLOY_FURNACE_MENU.get(), id, inventory, buf, ModBlocks.ALLOY_FURNACE.get(), 2)));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> MachineMenu.fromNetwork(
                            ModMenus.COMPRESSOR_MENU.get(), id, inventory, buf, ModBlocks.COMPRESSOR.get(), 1)));

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> CIRCUIT_ASSEMBLER_MENU =
            MENUS.register("circuit_assembler",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> MachineMenu.fromNetwork(
                            ModMenus.CIRCUIT_ASSEMBLER_MENU.get(), id, inventory, buf, ModBlocks.CIRCUIT_ASSEMBLER.get(), 4)));

    // M3a: server rack drive GUI.
    public static final DeferredHolder<MenuType<?>, MenuType<ServerRackMenu>> SERVER_RACK_MENU =
            MENUS.register("server_rack",
                    () -> IMenuTypeExtension.create((id, inventory, buf) -> ServerRackMenu.fromNetwork(
                            ModMenus.SERVER_RACK_MENU.get(), id, inventory, buf)));
}

package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.machine.CombustionGeneratorMenu;
import com.blackrikka.ironworks.machine.MachineMenu;
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
}

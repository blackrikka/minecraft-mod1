package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ironworks.MOD_ID);

    public static final Supplier<CreativeModeTab> IRONWORKS_TAB = CREATIVE_TABS.register("ironworks_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ironworks"))
                    .icon(() -> ModItems.TEST_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEST_BLOCK_ITEM.get());
                        output.accept(ModItems.RUBBER_LOG_ITEM.get());
                        output.accept(ModItems.RUBBER_PLANKS_ITEM.get());
                        output.accept(ModItems.RUBBER_LEAVES_ITEM.get());
                        output.accept(ModItems.RUBBER_SAPLING_ITEM.get());
                        output.accept(ModItems.RUBBER_SAP.get());
                        output.accept(ModItems.RUBBER.get());
                        output.accept(ModItems.COMBUSTION_GENERATOR_ITEM.get());
                        output.accept(ModItems.CREATIVE_BATTERY_ITEM.get());
                        output.accept(ModItems.COPPER_CABLE_ITEM.get());
                        output.accept(ModItems.TIN_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_TIN_ORE_ITEM.get());
                        output.accept(ModItems.RAW_TIN.get());
                        output.accept(ModItems.TIN_INGOT.get());
                        output.accept(ModItems.IRON_DUST.get());
                        output.accept(ModItems.COPPER_DUST.get());
                        output.accept(ModItems.GOLD_DUST.get());
                        output.accept(ModItems.TIN_DUST.get());
                        output.accept(ModItems.BRONZE_INGOT.get());
                        output.accept(ModItems.MACHINE_CASING_ITEM.get());
                        output.accept(ModItems.CRUSHER_ITEM.get());
                        output.accept(ModItems.ELECTRIC_FURNACE_ITEM.get());
                        output.accept(ModItems.SILICON_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_SILICON_ORE_ITEM.get());
                        output.accept(ModItems.ALLOY_FURNACE_ITEM.get());
                        output.accept(ModItems.COMPRESSOR_ITEM.get());
                        output.accept(ModItems.CIRCUIT_ASSEMBLER_ITEM.get());
                        output.accept(ModItems.STEEL_INGOT.get());
                        output.accept(ModItems.IRON_PLATE.get());
                        output.accept(ModItems.COPPER_PLATE.get());
                        output.accept(ModItems.STEEL_PLATE.get());
                        output.accept(ModItems.GOLD_FOIL.get());
                        output.accept(ModItems.PLASTIC_SHEET.get());
                        output.accept(ModItems.SILICON.get());
                        output.accept(ModItems.SILICON_WAFER.get());
                        output.accept(ModItems.BASIC_CIRCUIT.get());
                        output.accept(ModItems.ADVANCED_CIRCUIT.get());
                        output.accept(ModItems.RESISTOR.get());
                        output.accept(ModItems.DIODE.get());
                        output.accept(ModItems.TRANSISTOR.get());
                        output.accept(ModItems.IC_CHIP_BASIC.get());
                        output.accept(ModItems.IC_CHIP_ADVANCED.get());
                        output.accept(ModItems.CONVEYOR_ITEM.get());
                        output.accept(ModItems.SPLITTER_ITEM.get());
                        output.accept(ModItems.SORTER_ITEM.get());
                        output.accept(ModItems.SERVER_RACK_ITEM.get());
                        output.accept(ModItems.NETWORK_CABLE_ITEM.get());
                        output.accept(ModItems.STORAGE_PORT_ITEM.get());
                        output.accept(ModItems.DRIVE_1K.get());
                        output.accept(ModItems.DRIVE_4K.get());
                        output.accept(ModItems.DRIVE_16K.get());
                    })
                    .build()
    );
}

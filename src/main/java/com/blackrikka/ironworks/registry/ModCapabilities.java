package com.blackrikka.ironworks.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Block capability registration (mod event bus). */
public class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // M1b: combustion generator. Energy is extract-only (the storage has maxReceive = 0).
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage());
        // Hoppers and conveyors may insert fuel only and cannot extract.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getFuelHandler());

        // M1b: development battery accepts FE without limit.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CREATIVE_BATTERY.get(),
                (be, side) -> be.getEnergySink());

        // M1c: copper cable accepts FE from every side; extraction is internal to the cable (view returns 0).
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COPPER_CABLE.get(),
                (be, side) -> be.getExternalEnergy());

        // M1d: machines accept FE (receive-only buffer) and expose inputs for insertion, output for extraction.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CRUSHER.get(),
                (be, side) -> be.getItemView());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getItemView());

        // M1e: alloy furnace (2 inputs), compressor (1 input), circuit assembler (4 inputs).
        // Energy is receive-only; hoppers and conveyors insert into input slots and extract from the output slot.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ALLOY_FURNACE.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ALLOY_FURNACE.get(),
                (be, side) -> be.getItemView());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPRESSOR.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMPRESSOR.get(),
                (be, side) -> be.getItemView());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CIRCUIT_ASSEMBLER.get(),
                (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CIRCUIT_ASSEMBLER.get(),
                (be, side) -> be.getItemView());

        // M2: belts accept one item through every face except the front and never give items out
        // (their own pulls read the block behind them directly).
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.CONVEYOR.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SPLITTER.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SORTER.get(),
                (be, side) -> be.getItemHandler(side));
    }
}

package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ironworks.MOD_ID);

    public static final DeferredItem<BlockItem> TEST_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TEST_BLOCK);

    // M1a: rubber tree.
    public static final DeferredItem<BlockItem> RUBBER_LOG_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_LOG);
    public static final DeferredItem<BlockItem> RUBBER_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_PLANKS);
    public static final DeferredItem<BlockItem> RUBBER_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_LEAVES);
    public static final DeferredItem<BlockItem> RUBBER_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_SAPLING);
    public static final DeferredItem<Item> RUBBER_SAP = ITEMS.registerSimpleItem("rubber_sap");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");

    // M1b: combustion generator and development battery.
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> CREATIVE_BATTERY_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_BATTERY);

    // M1c: copper cable.
    public static final DeferredItem<BlockItem> COPPER_CABLE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COPPER_CABLE);

    // M1d: tin, dusts, alloy, and machine block items.
    public static final DeferredItem<BlockItem> TIN_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_TIN_ORE);
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> TIN_DUST = ITEMS.registerSimpleItem("tin_dust");
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");
    public static final DeferredItem<BlockItem> MACHINE_CASING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MACHINE_CASING);
    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRUSHER);
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_FURNACE);

    // M1e: silicon ore, machines, and processed materials.
    public static final DeferredItem<BlockItem> SILICON_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SILICON_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_SILICON_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_SILICON_ORE);
    public static final DeferredItem<BlockItem> ALLOY_FURNACE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ALLOY_FURNACE);
    public static final DeferredItem<BlockItem> COMPRESSOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COMPRESSOR);
    public static final DeferredItem<BlockItem> CIRCUIT_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CIRCUIT_ASSEMBLER);
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.registerSimpleItem("copper_plate");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");
    public static final DeferredItem<Item> GOLD_FOIL = ITEMS.registerSimpleItem("gold_foil");
    public static final DeferredItem<Item> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer");
    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.registerSimpleItem("basic_circuit");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");

    // M2: belts.
    public static final DeferredItem<BlockItem> CONVEYOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CONVEYOR);
    public static final DeferredItem<BlockItem> SPLITTER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SPLITTER);
    public static final DeferredItem<BlockItem> SORTER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SORTER);
}

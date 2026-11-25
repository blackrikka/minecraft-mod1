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
}

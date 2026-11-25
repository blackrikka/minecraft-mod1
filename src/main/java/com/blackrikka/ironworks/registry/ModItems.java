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
}

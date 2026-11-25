package com.blackrikka.ironworks;

import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModBlocks;
import com.blackrikka.ironworks.registry.ModCapabilities;
import com.blackrikka.ironworks.registry.ModCreativeTabs;
import com.blackrikka.ironworks.registry.ModItems;
import com.blackrikka.ironworks.registry.ModMenus;
import com.blackrikka.ironworks.registry.ModRecipeSerializers;
import com.blackrikka.ironworks.registry.ModRecipeTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Ironworks.MOD_ID)
public class Ironworks {
    public static final String MOD_ID = "ironworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ironworks(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);

        LOGGER.info("Ironworks loaded");
    }
}

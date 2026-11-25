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
                    })
                    .build()
    );
}

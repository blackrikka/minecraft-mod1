package com.blackrikka.ironworks.client;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.conveyor.ConveyorRenderer;
import com.blackrikka.ironworks.machine.CombustionGeneratorScreen;
import com.blackrikka.ironworks.machine.MachineScreen;
import com.blackrikka.ironworks.registry.ModBlockEntities;
import com.blackrikka.ironworks.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Ironworks.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR_MENU.get(), CombustionGeneratorScreen::new);
        event.register(ModMenus.CRUSHER_MENU.get(), MachineScreen::new);
        event.register(ModMenus.ELECTRIC_FURNACE_MENU.get(), MachineScreen::new);
        event.register(ModMenus.ALLOY_FURNACE_MENU.get(), MachineScreen::new);
        event.register(ModMenus.COMPRESSOR_MENU.get(), MachineScreen::new);
        event.register(ModMenus.CIRCUIT_ASSEMBLER_MENU.get(), MachineScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONVEYOR.get(), ConveyorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SPLITTER.get(), ConveyorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SORTER.get(), ConveyorRenderer::new);
    }
}

package com.blackrikka.ironworks.client;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.machine.CombustionGeneratorScreen;
import com.blackrikka.ironworks.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Ironworks.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR_MENU.get(), CombustionGeneratorScreen::new);
    }
}

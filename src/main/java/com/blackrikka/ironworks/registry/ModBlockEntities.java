package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.energy.CableBlockEntity;
import com.blackrikka.ironworks.machine.CombustionGeneratorBlockEntity;
import com.blackrikka.ironworks.machine.CreativeBatteryBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ironworks.MOD_ID);

    // M1b: combustion generator.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITY_TYPES.register("combustion_generator",
                    () -> BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new,
                            ModBlocks.COMBUSTION_GENERATOR.get()).build(null));

    // M1b: development-only creative battery.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeBatteryBlockEntity>> CREATIVE_BATTERY =
            BLOCK_ENTITY_TYPES.register("creative_battery",
                    () -> BlockEntityType.Builder.of(CreativeBatteryBlockEntity::new,
                            ModBlocks.CREATIVE_BATTERY.get()).build(null));

    // M1c: copper cable buffer.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> COPPER_CABLE =
            BLOCK_ENTITY_TYPES.register("copper_cable",
                    () -> BlockEntityType.Builder.of(CableBlockEntity::new,
                            ModBlocks.COPPER_CABLE.get()).build(null));
}

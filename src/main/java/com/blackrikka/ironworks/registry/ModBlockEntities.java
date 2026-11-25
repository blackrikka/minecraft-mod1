package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.conveyor.ConveyorBlockEntity;
import com.blackrikka.ironworks.conveyor.SorterBlockEntity;
import com.blackrikka.ironworks.conveyor.SplitterBlockEntity;
import com.blackrikka.ironworks.energy.CableBlockEntity;
import com.blackrikka.ironworks.machine.AlloyFurnaceBlockEntity;
import com.blackrikka.ironworks.machine.CircuitAssemblerBlockEntity;
import com.blackrikka.ironworks.machine.CombustionGeneratorBlockEntity;
import com.blackrikka.ironworks.machine.CompressorBlockEntity;
import com.blackrikka.ironworks.machine.CrusherBlockEntity;
import com.blackrikka.ironworks.machine.CreativeBatteryBlockEntity;
import com.blackrikka.ironworks.machine.ElectricFurnaceBlockEntity;
import com.blackrikka.ironworks.storage.ServerRackBlockEntity;
import com.blackrikka.ironworks.storage.StoragePortBlockEntity;
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

    // M1d: crusher.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITY_TYPES.register("crusher",
                    () -> BlockEntityType.Builder.of(CrusherBlockEntity::new,
                            ModBlocks.CRUSHER.get()).build(null));

    // M1d: electric furnace.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITY_TYPES.register("electric_furnace",
                    () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new,
                            ModBlocks.ELECTRIC_FURNACE.get()).build(null));

    // M1e: alloy furnace, compressor, circuit assembler.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITY_TYPES.register("alloy_furnace",
                    () -> BlockEntityType.Builder.of(AlloyFurnaceBlockEntity::new,
                            ModBlocks.ALLOY_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
            BLOCK_ENTITY_TYPES.register("compressor",
                    () -> BlockEntityType.Builder.of(CompressorBlockEntity::new,
                            ModBlocks.COMPRESSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CircuitAssemblerBlockEntity>> CIRCUIT_ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("circuit_assembler",
                    () -> BlockEntityType.Builder.of(CircuitAssemblerBlockEntity::new,
                            ModBlocks.CIRCUIT_ASSEMBLER.get()).build(null));

    // M2: belt buffers.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
            BLOCK_ENTITY_TYPES.register("conveyor",
                    () -> BlockEntityType.Builder.of(ConveyorBlockEntity::new,
                            ModBlocks.CONVEYOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SplitterBlockEntity>> SPLITTER =
            BLOCK_ENTITY_TYPES.register("splitter",
                    () -> BlockEntityType.Builder.of(SplitterBlockEntity::new,
                            ModBlocks.SPLITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SorterBlockEntity>> SORTER =
            BLOCK_ENTITY_TYPES.register("sorter",
                    () -> BlockEntityType.Builder.of(SorterBlockEntity::new,
                            ModBlocks.SORTER.get()).build(null));

    // M3a: server rack (drive slots and energy) and storage port (network entry, no buffer).
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK =
            BLOCK_ENTITY_TYPES.register("server_rack",
                    () -> BlockEntityType.Builder.of(ServerRackBlockEntity::new,
                            ModBlocks.SERVER_RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoragePortBlockEntity>> STORAGE_PORT =
            BLOCK_ENTITY_TYPES.register("storage_port",
                    () -> BlockEntityType.Builder.of(StoragePortBlockEntity::new,
                            ModBlocks.STORAGE_PORT.get()).build(null));
}

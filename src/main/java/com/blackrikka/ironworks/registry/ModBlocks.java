package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.block.RubberLogBlock;
import com.blackrikka.ironworks.energy.CableBlock;
import com.blackrikka.ironworks.machine.AlloyFurnaceBlock;
import com.blackrikka.ironworks.machine.CircuitAssemblerBlock;
import com.blackrikka.ironworks.machine.CompressorBlock;
import com.blackrikka.ironworks.machine.CombustionGeneratorBlock;
import com.blackrikka.ironworks.machine.CreativeBatteryBlock;
import com.blackrikka.ironworks.machine.CrusherBlock;
import com.blackrikka.ironworks.machine.ElectricFurnaceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ironworks.MOD_ID);

    // Test block with iron-like properties (M0 skeleton check).
    public static final DeferredBlock<Block> TEST_BLOCK = BLOCKS.registerSimpleBlock(
            "test_block",
            BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
    );

    // M1a: rubber tree.
    /** Configured feature key for the rubber tree (data/ironworks/worldgen/configured_feature/rubber_tree.json). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE_FEATURE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Ironworks.MOD_ID, "rubber_tree"));

    /** Sapling growth uses the same tree as worldgen. */
    public static final TreeGrower RUBBER_TREE_GROWER = new TreeGrower(
            "ironworks_rubber",
            Optional.empty(),
            Optional.of(RUBBER_TREE_FEATURE),
            Optional.empty());

    public static final DeferredBlock<Block> RUBBER_LOG = BLOCKS.register(
            "rubber_log",
            () -> new RubberLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final DeferredBlock<Block> RUBBER_PLANKS = BLOCKS.registerSimpleBlock(
            "rubber_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    public static final DeferredBlock<Block> RUBBER_LEAVES = BLOCKS.register(
            "rubber_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> RUBBER_SAPLING = BLOCKS.register(
            "rubber_sapling",
            () -> new SaplingBlock(RUBBER_TREE_GROWER, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    // M1b: combustion generator (lit = light level 13 while generating).
    public static final DeferredBlock<Block> COMBUSTION_GENERATOR = BLOCKS.register(
            "combustion_generator",
            () -> new CombustionGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(CombustionGeneratorBlock.LIT) ? 13 : 0)));

    // M1b: development-only FE sink.
    public static final DeferredBlock<Block> CREATIVE_BATTERY = BLOCKS.register(
            "creative_battery",
            () -> new CreativeBatteryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    // M1c: copper cable (6-face connection, multipart model).
    public static final DeferredBlock<Block> COPPER_CABLE = BLOCKS.register(
            "copper_cable",
            () -> new CableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // M1d: tin ore (overworld and deepslate variants).
    public static final DeferredBlock<Block> TIN_ORE = BLOCKS.registerSimpleBlock(
            "tin_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_tin_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // M1d: machine casing, the base of every machine recipe.
    public static final DeferredBlock<Block> MACHINE_CASING = BLOCKS.registerSimpleBlock(
            "machine_casing",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // M1d: crusher (ore -> dust) and electric furnace (FE smelting). lit = light level 13 while working.
    public static final DeferredBlock<Block> CRUSHER = BLOCKS.register(
            "crusher",
            () -> new CrusherBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CrusherBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<Block> ELECTRIC_FURNACE = BLOCKS.register(
            "electric_furnace",
            () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 13 : 0)));

    // M1e: silicon ore (stone and deepslate variants), same drop rules as tin ore.
    public static final DeferredBlock<Block> SILICON_ORE = BLOCKS.registerSimpleBlock(
            "silicon_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<Block> DEEPSLATE_SILICON_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_silicon_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // M1e: alloy furnace, compressor, circuit assembler (lit = light level 13 while working).
    public static final DeferredBlock<Block> ALLOY_FURNACE = BLOCKS.register(
            "alloy_furnace",
            () -> new AlloyFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(AlloyFurnaceBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<Block> COMPRESSOR = BLOCKS.register(
            "compressor",
            () -> new CompressorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CompressorBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<Block> CIRCUIT_ASSEMBLER = BLOCKS.register(
            "circuit_assembler",
            () -> new CircuitAssemblerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CircuitAssemblerBlock.LIT) ? 13 : 0)));
}

package com.blackrikka.ironworks.registry;

import com.blackrikka.ironworks.Ironworks;
import com.blackrikka.ironworks.block.RubberLogBlock;
import com.blackrikka.ironworks.energy.CableBlock;
import com.blackrikka.ironworks.machine.CombustionGeneratorBlock;
import com.blackrikka.ironworks.machine.CreativeBatteryBlock;
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
}

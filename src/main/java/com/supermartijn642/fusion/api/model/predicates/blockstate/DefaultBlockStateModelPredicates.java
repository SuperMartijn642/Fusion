package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.predicates.blockstate.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;

/**
 * Used to create instances of the default {@link BlockStateModelPredicate}s provided by Fusion.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public final class DefaultBlockStateModelPredicates {

    /**
     * Predicate that always evaluates to true.
     */
    public static BlockStateModelPredicate always(){
        return TrueBlockStateModelPredicate.INSTANCE;
    }

    /**
     * Predicate that always evaluates to false.
     */
    public static BlockStateModelPredicate never(){
        return FalseBlockStateModelPredicate.INSTANCE;
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static BlockStateModelPredicate and(BlockStateModelPredicate... predicates){
        return AndBlockStateModelPredicate.create(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static BlockStateModelPredicate or(BlockStateModelPredicate... predicates){
        return OrBlockStateModelPredicate.create(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static BlockStateModelPredicate not(BlockStateModelPredicate predicate){
        return NotBlockStateModelPredicate.create(predicate);
    }

    /**
     * Predicate that evaluates whether the block is between the given minimum and maximum heights.
     * @param minHeight height that the block needs to be at or above
     * @param maxHeight height that the block needs to be at or below
     */
    public static BlockStateModelPredicate altitude(int minHeight, int maxHeight){
        return AltitudeBlockStateModelPredicate.create(minHeight, maxHeight);
    }

    /**
     * Creates a predicate that evaluates whether the block is in the given biomes.
     */
    public static BlockStateModelPredicate biome(RegistryKey<Biome>... biomes){
        return BiomeBlockStatePredicate.create(biomes);
    }

    /**
     * Creates a predicate that evaluates whether the block is in the given biomes.
     */
    public static BlockStateModelPredicate biome(ResourceLocation... biomes){
        return BiomeBlockStatePredicate.create(biomes);
    }

    /**
     * Creates a predicate that evaluates whether the block is in one of the given dimensions.
     */
    public static BlockStateModelPredicate dimension(RegistryKey<World>... dimensions){
        return DimensionBlockStateModelPredicate.create(dimensions);
    }

    /**
     * Creates a predicate that evaluates whether the block is in one of the given dimensions.
     */
    public static BlockStateModelPredicate dimension(ResourceLocation... dimensions){
        return DimensionBlockStateModelPredicate.create(dimensions);
    }

    /**
     * Creates a predicate that evaluate whether the block at one of the given offset matches one of the given blocks.
     * @param blocks  blocks that should be matched
     * @param offsets offsets to check
     */
    public static BlockStateModelPredicate matchBlock(List<Block> blocks, List<BlockPos> offsets){
        return MatchBlockBlockStatePredicate.create(blocks, offsets);
    }

    /**
     * Creates a predicate that evaluate whether the block at the given offset matches one of the given blocks.
     * @param x      x-offset
     * @param y      y-offset
     * @param z      z-offset
     * @param blocks blocks that should be matched
     */
    public static BlockStateModelPredicate matchBlock(int x, int y, int z, Block... blocks){
        return MatchBlockBlockStatePredicate.create(x, y, z, blocks);
    }

    /**
     * Creates a predicate that evaluate whether the block at one of the given offsets matches one of the given blocks and matches the given properties.
     * @param blocks     blocks that should be matched
     * @param properties property value pairs that should be matched
     * @param offsets    offsets to check
     */
    public static BlockStateModelPredicate matchState(List<Block> blocks, List<BlockPos> offsets, Pair<Property<?>,?>... properties){
        return MatchStateBlockStatePredicate.create(blocks, offsets, properties);
    }

    /**
     * Creates a predicate that evaluate whether the block at one of the given offsets is the same as the given state.
     * @param state   state that should be matched
     * @param offsets offsets to check
     */
    public static BlockStateModelPredicate matchState(BlockState state, List<BlockPos> offsets){
        return MatchStateBlockStatePredicate.create(state, offsets);
    }
}

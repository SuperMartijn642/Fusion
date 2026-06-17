package com.supermartijn642.fusion.api.texture.types.connecting.predicates;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.types.connecting.predicates.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;

import java.util.List;

/**
 * Used to create instances of the default {@link ConnectionPredicate}s provided by Fusion.
 * <p>
 * Created 28/04/2023 by SuperMartijn642
 */
public final class DefaultConnectionPredicates {

    /**
     * Predicate that always evaluates to true.
     */
    public static ConnectionPredicate always(){
        return TrueConnectionPredicate.INSTANCE;
    }

    /**
     * Predicate that always evaluates to false.
     */
    public static ConnectionPredicate never(){
        return FalseConnectionPredicate.INSTANCE;
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static ConnectionPredicate and(ConnectionPredicate... predicates){
        return AndConnectionPredicate.create(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ConnectionPredicate or(ConnectionPredicate... predicates){
        return OrConnectionPredicate.create(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ConnectionPredicate not(ConnectionPredicate predicate){
        return NotConnectionPredicate.create(predicate);
    }

    /**
     * Creates a predicate that is true only for the given directions.
     */
    public static ConnectionPredicate isDirection(ConnectionDirection... directions){
        return IsDirectionConnectionPredicate.create(directions);
    }

    /**
     * Creates a predicate that is satisfied if the block in the connection direction is visible.
     */
    public static ConnectionPredicate isFaceVisible(){
        return IsFaceVisibleConnectionPredicate.INSTANCE;
    }

    /**
     * Creates a predicate that is satisfied if the block in the connection direction is the same as the block of the model itself.
     */
    public static ConnectionPredicate isSameBlock(){
        return IsSameBlockConnectionPredicate.INSTANCE;
    }

    /**
     * Creates a predicate that is satisfied if the block state in the connection direction is the same as the block state of the model itself.
     */
    public static ConnectionPredicate isSameState(){
        return IsSameStateConnectionPredicate.INSTANCE;
    }

    /**
     * Creates a predicate that is satisfied if the block in the connection direction matches one of the provided block.
     * @param blocks blocks that should be matched
     */
    public static ConnectionPredicate matchBlock(Block... blocks){
        return MatchBlockConnectionPredicate.create(blocks);
    }

    /**
     * Creates a predicate that is satisfied if the state in the connection direction matches one of the provided block and matches the provided properties.
     * @param blocks     blocks that should be matched
     * @param properties property value pairs that should be matched
     */
    public static ConnectionPredicate matchState(List<Block> blocks, Pair<Property<?>,?>... properties){
        return MatchStateConnectionPredicate.create(blocks, properties);
    }

    /**
     * Creates a predicate that is satisfied if the state in the connection direction is the same as the provided state.
     * @param state state that should be matched
     */
    public static ConnectionPredicate matchState(BlockState state){
        return MatchStateConnectionPredicate.create(state);
    }

    /**
     * Creates a predicate that is satisfied if the block in front of the block in the connection direction matches one of the provided block.
     * @param blocks blocks that should be matched
     */
    public static ConnectionPredicate matchBlockInFront(Block... blocks){
        return MatchBlockInFrontConnectionPredicate.create(blocks);
    }

    /**
     * Creates a predicate that is satisfied if the state in front of the state in the connection direction matches one of the provided block and matches the provided properties.
     * @param blocks     blocks that should be matched
     * @param properties property value pairs that should be matched
     */
    public static ConnectionPredicate matchStateInFront(List<Block> blocks, Pair<Property<?>,?>... properties){
        return MatchStateInFrontConnectionPredicate.create(blocks, properties);
    }

    /**
     * Creates a predicate that is satisfied if the state in front of the state in the connection direction is the same as the provided state.
     * @param state state that should be matched
     */
    public static ConnectionPredicate matchStateInFront(BlockState state){
        return MatchStateInFrontConnectionPredicate.create(state);
    }
}

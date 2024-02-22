package com.supermartijn642.fusion.api.predicate;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.predicate.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Arrays;

/**
 * Used to create instances of the default {@link ConnectionPredicate}s provided by Fusion.
 * <p>
 * Created 28/04/2023 by SuperMartijn642
 */
public final class DefaultConnectionPredicates {

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates which need to be satisfied
     */
    public static ConnectionPredicate and(ConnectionPredicate... predicates){
        return new AndConnectionPredicate(Arrays.asList(predicates));
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ConnectionPredicate or(ConnectionPredicate... predicates){
        return new OrConnectionPredicate(Arrays.asList(predicates));
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ConnectionPredicate not(ConnectionPredicate predicate){
        return new NotConnectionPredicate(predicate);
    }

    /**
     * Creates a predicate which is satisfied if the block in the connection direction is visible.
     */
    public static ConnectionPredicate isFaceVisible(){
        return new IsSameBlockConnectionPredicate();
    }

    /**
     * Creates a predicate which is satisfied if the block in the connection direction is the same as the block of the model itself.
     */
    public static ConnectionPredicate isSameBlock(){
        return new IsSameBlockConnectionPredicate();
    }

    /**
     * Creates a predicate which is satisfied if the block state in the connection direction is the same as the block state of the model itself.
     */
    public static ConnectionPredicate isSameState(){
        return new IsSameStateConnectionPredicate();
    }

    /**
     * Creates a predicate which is satisfied if the block in the connection direction is the same as the provided block.
     * @param block block which should be matched
     */
    public static ConnectionPredicate matchBlock(Block block){
        return new MatchBlockConnectionPredicate(block);
    }

    /**
     * Creates a predicate which is satisfied if the state in the connection direction is the same as the provided block and matches the provided properties.
     * @param block      block which should be matched
     * @param properties property value pairs which should be matched
     */
    public static ConnectionPredicate matchState(Block block, Pair<Property<?>,?>... properties){
        return new MatchStateConnectionPredicate(block, properties);
    }

    /**
     * Creates a predicate which is satisfied if the state in the connection direction is the same as the provided state.
     * @param state state which should be matched
     */
    public static ConnectionPredicate matchState(BlockState state){
        //noinspection unchecked
        return matchState(state.getBlock(), (Pair<Property<?>,?>[])state.getProperties().stream().map(p -> Pair.of(p, state.getValue(p))).toArray());
    }
}

package com.supermartijn642.fusion.api.predicate;

import com.supermartijn642.fusion.predicate.*;
import net.minecraft.block.Block;

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
}

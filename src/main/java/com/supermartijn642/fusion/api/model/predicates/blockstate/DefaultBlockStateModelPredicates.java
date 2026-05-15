package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.supermartijn642.fusion.model.predicates.blockstate.*;

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
}

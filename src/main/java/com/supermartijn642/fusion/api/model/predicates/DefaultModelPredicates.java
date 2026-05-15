package com.supermartijn642.fusion.api.model.predicates;

import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.predicates.*;

/**
 * Used to create instances of the default {@link ModelPredicate}s provided by Fusion.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public final class DefaultModelPredicates {

    /**
     * Creates a generic predicate from a block state model predicate.
     * When possible, the block state is obtained from the item stack.
     */
    static ModelPredicate blockStateWrapper(BlockStateModelPredicate predicate){
        return BlockStateWrapperModelPredicate.create(predicate);
    }

    /**
     * Creates a generic predicate from an item model predicate.
     * When possible, the item is obtained from the block state.
     */
    static ModelPredicate itemWrapper(ItemModelPredicate predicate){
        return ItemWrapperModelPredicate.create(predicate);
    }

    /**
     * Predicate that returns the result of the given predicates for block states and items separately.
     */
    static ModelPredicate blockStateAndItem(BlockStateModelPredicate blockStatePredicate, ItemModelPredicate itemPredicate){
        return BlockAndItemModelPredicate.create(blockStatePredicate, itemPredicate);
    }

    /**
     * Predicate that always evaluates to true.
     */
    public static ModelPredicate always(){
        return TrueModelPredicate.INSTANCE;
    }

    /**
     * Predicate that always evaluates to false.
     */
    public static ModelPredicate never(){
        return FalseModelPredicate.INSTANCE;
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static ModelPredicate and(ModelPredicate... predicates){
        return AndModelPredicate.create(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ModelPredicate or(ModelPredicate... predicates){
        return OrModelPredicate.create(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ModelPredicate not(ModelPredicate predicate){
        return NotModelPredicate.create(predicate);
    }
}

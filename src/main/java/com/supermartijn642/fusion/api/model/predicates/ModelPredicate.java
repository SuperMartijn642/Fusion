package com.supermartijn642.fusion.api.model.predicates;

import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.predicates.ModelPredicateImpl;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelPredicate {

    /**
     * Creates a generic predicate from a block state model predicate.
     * When possible, the block state is obtained from the item stack.
     */
    static ModelPredicate of(BlockStateModelPredicate predicate){
        return ModelPredicateImpl.of(predicate);
    }

    /**
     * Creates a generic predicate from an item model predicate.
     * When possible, the item is obtained from the block state.
     */
    static ModelPredicate of(ItemModelPredicate predicate){
        return ModelPredicateImpl.of(predicate);
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates which need to be satisfied
     */
    static ModelPredicate and(ModelPredicate... predicates){
        return ModelPredicateImpl.and(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    static ModelPredicate or(ModelPredicate... predicates){
        return ModelPredicateImpl.or(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    static ModelPredicate not(ModelPredicate predicate){
        return ModelPredicateImpl.not(predicate);
    }

    boolean testForBlock(@Nullable IEnviromentBlockReader level, @Nullable BlockPos pos, @Nullable BlockState state);

    boolean testForItem(ItemStack stack);
}

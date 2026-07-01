package com.supermartijn642.fusion.api.model.predicates;

import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public interface ModelPredicate {

    /**
     * Creates a generic predicate from a block state model predicate.
     * When possible, the block state is obtained from the item stack.
     */
    static ModelPredicate of(BlockStateModelPredicate predicate){
        return DefaultModelPredicates.blockStateWrapper(predicate);
    }

    /**
     * Creates a generic predicate from an item model predicate.
     * When possible, the item is obtained from the block state.
     */
    static ModelPredicate of(ItemModelPredicate predicate){
        return DefaultModelPredicates.itemWrapper(predicate);
    }

    boolean testForBlockState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state);

    boolean testForItem(ItemStack stack);

    /**
     * Applies the given model transform to the predicate.
     */
    default ModelPredicate applyTransform(ModelTransform transform){
        return this;
    }

    /**
     * Simplifies the predicate. May be used to simplify user properties.
     * For example, an and-predicate may flatten nested and-predicates or an empty or-predicate may return a false-predicate.
     */
    default ModelPredicate simplify(){
        return this;
    }

    /**
     * Serializer for this predicate.
     */
    Serializer<? extends ModelPredicate> getSerializer();

    /**
     * Checks whether this predicate is {@link DefaultModelPredicates#always()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysTrue(){
        return this == DefaultModelPredicates.always();
    }

    /**
     * Checks whether this predicate is {@link DefaultModelPredicates#never()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysFalse(){
        return this == DefaultModelPredicates.never();
    }

    /**
     * Adds a requirement to this predicate.
     */
    @ApiStatus.NonExtendable
    default ModelPredicate and(ModelPredicate... predicates){
        ModelPredicate[] allPredicates = new ModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultModelPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    @ApiStatus.NonExtendable
    default ModelPredicate or(ModelPredicate... predicates){
        ModelPredicate[] allPredicates = new ModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultModelPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this resource condition.
     */
    @ApiStatus.NonExtendable
    default ModelPredicate negate(){
        return DefaultModelPredicates.not(this);
    }
}

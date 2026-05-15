package com.supermartijn642.fusion.api.model.predicates.item;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * A predicate for item models.
 * <p>
 * Created 20/09/2024 by SuperMartijn642
 */
public interface ItemModelPredicate {

    boolean test(ItemStack stack);

    /**
     * Simplifies the predicate. May be used to simplify user properties.
     * For example, an and-predicate may flatten nested and-predicates or an empty or-predicate may return a false-predicate.
     */
    default ItemModelPredicate simplify(){
        return this;
    }

    /**
     * Serializer for this predicate.
     */
    Serializer<? extends ItemModelPredicate> getSerializer();

    /**
     * Checks whether this predicate is {@link DefaultItemModelPredicates#always()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysTrue(){
        return this == DefaultItemModelPredicates.always();
    }

    /**
     * Checks whether this predicate is {@link DefaultItemModelPredicates#never()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysFalse(){
        return this == DefaultItemModelPredicates.never();
    }

    /**
     * Adds a requirement to this predicate.
     */
    @ApiStatus.NonExtendable
    default ItemModelPredicate and(ItemModelPredicate... predicates){
        ItemModelPredicate[] allPredicates = new ItemModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemModelPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    @ApiStatus.NonExtendable
    default ItemModelPredicate or(ItemModelPredicate... predicates){
        ItemModelPredicate[] allPredicates = new ItemModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemModelPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this resource condition.
     */
    @ApiStatus.NonExtendable
    default ItemModelPredicate negate(){
        return DefaultItemModelPredicates.not(this);
    }
}

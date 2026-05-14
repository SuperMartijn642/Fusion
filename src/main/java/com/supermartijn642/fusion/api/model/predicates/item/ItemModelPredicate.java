package com.supermartijn642.fusion.api.model.predicates.item;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * A predicate for item models.
 * <p>
 * Created 20/09/2024 by SuperMartijn642
 */
public interface ItemModelPredicate {

    boolean test(ItemStack stack);

    /**
     * @return the serializer for this predicate
     */
    Serializer<? extends ItemModelPredicate> getSerializer();

    /**
     * Adds a requirement to this predicate.
     */
    default ItemModelPredicate and(ItemModelPredicate... predicates){
        ItemModelPredicate[] allPredicates = new ItemModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    default ItemModelPredicate or(ItemModelPredicate... predicates){
        ItemModelPredicate[] allPredicates = new ItemModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this predicate.
     */
    default ItemModelPredicate negate(){
        return DefaultItemPredicates.not(this);
    }
}

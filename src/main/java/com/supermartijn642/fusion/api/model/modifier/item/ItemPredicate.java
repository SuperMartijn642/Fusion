package com.supermartijn642.fusion.api.model.modifier.item;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * A predicate which takes an item stack.
 * <p>
 * Created 20/09/2024 by SuperMartijn642
 */
public interface ItemPredicate {

    boolean test(ItemStack stack);

    /**
     * @return the serializer for this predicate
     */
    Serializer<? extends ItemPredicate> getSerializer();

    /**
     * Adds a requirement to this predicate.
     */
    default ItemPredicate and(ItemPredicate... predicates){
        ItemPredicate[] allPredicates = new ItemPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    default ItemPredicate or(ItemPredicate... predicates){
        ItemPredicate[] allPredicates = new ItemPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultItemPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this predicate.
     */
    default ItemPredicate negate(){
        return DefaultItemPredicates.not(this);
    }
}

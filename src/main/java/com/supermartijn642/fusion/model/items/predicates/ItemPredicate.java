package com.supermartijn642.fusion.model.items.predicates;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public interface ItemPredicate {

    boolean test(ItemStack stack);

    /**
     * @return the serializer for this predicate
     */
    Serializer<? extends ItemPredicate> getSerializer();
}

package com.supermartijn642.fusion.api.model.predicates.item;

import com.supermartijn642.fusion.model.predicates.item.*;
import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Used to create instances of the default {@link ItemModelPredicate}s provided by Fusion.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public final class DefaultItemModelPredicates {

    /**
     * Predicate that always evaluates to true.
     */
    public static ItemModelPredicate always(){
        return TrueItemModelPredicate.INSTANCE;
    }

    /**
     * Predicate that always evaluates to false.
     */
    public static ItemModelPredicate never(){
        return FalseItemModelPredicate.INSTANCE;
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static ItemModelPredicate and(ItemModelPredicate... predicates){
        return AndItemModelPredicate.create(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ItemModelPredicate or(ItemModelPredicate... predicates){
        return OrItemModelPredicate.create(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ItemModelPredicate not(ItemModelPredicate predicate){
        return NotItemModelPredicate.create(predicate);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is equal to the given count.
     */
    public static ItemModelPredicate count(int count){
        return CountItemModelPredicate.create(count);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is between the given minimum and maximum counts (both inclusive).
     */
    public static ItemModelPredicate count(int min, int max){
        return CountItemModelPredicate.create(min, max);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is greater than or equal to the given minimum and the number of items in the stack as percentage of the maximum stack size is less than the given maximum value.
     */
    public static ItemModelPredicate count(int min, float maxPercentage){
        return CountItemModelPredicate.create(min, maxPercentage);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is less than or equal to the given maximum and the number of items in the stack as percentage of the maximum stack size is greater than the given minimum value.
     */
    public static ItemModelPredicate count(float minPercentage, int max){
        return CountItemModelPredicate.create(minPercentage, max);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack as percentage of the maximum stack size is between the given minimum and maximum values.
     */
    public static ItemModelPredicate count(float minPercentage, float maxPercentage){
        return CountItemModelPredicate.create(minPercentage, maxPercentage);
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is between the given minimum and maximum values (both inclusive).
     */
    public static ItemModelPredicate durability(int min, int max){
        return DurabilityItemModelPredicate.create(min, max);
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is greater than or equal to the given minimum and the remaining durability of the item as percentage of the total durability is less than the given maximum value.
     */
    public static ItemModelPredicate durability(int min, float maxPercentage){
        return DurabilityItemModelPredicate.create(min, maxPercentage);
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is less than or equal to the given maximum and the remaining durability of the item as percentage of the total durability is greater than the given minimum value.
     */
    public static ItemModelPredicate durability(float minPercentage, int max){
        return DurabilityItemModelPredicate.create(minPercentage, max);
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item as percentage of the total durability is between the given minimum and maximum values.
     */
    public static ItemModelPredicate durability(float minPercentage, float maxPercentage){
        return DurabilityItemModelPredicate.create(minPercentage, maxPercentage);
    }

    /**
     * Creates a predicate which is satisfied when the item has the given enchantment.
     */
    public static ItemModelPredicate enchantment(Enchantment enchantment){
        return EnchantmentItemModelPredicate.create(enchantment);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is equal to the given level.
     */
    public static ItemModelPredicate enchantment(Enchantment enchantment, int level){
        return EnchantmentItemModelPredicate.create(enchantment, level, level);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is between the given minimum and maximum levels (both inclusive).
     */
    public static ItemModelPredicate enchantment(Enchantment enchantment, int minLevel, int maxLevel){
        return EnchantmentItemModelPredicate.create(enchantment, minLevel, maxLevel);
    }

    /**
     * Creates a predicate which is satisfied if the item is the given kind of potion.
     */
    public static ItemModelPredicate potion(Potion potion){
        return PotionItemModelPredicate.create(potion);
    }

    /**
     * Creates a predicate which is satisfied if the item is the given kind of potion.
     */
    public static ItemModelPredicate potion(Holder<Potion> potion){
        return PotionItemModelPredicate.create(potion);
    }
}

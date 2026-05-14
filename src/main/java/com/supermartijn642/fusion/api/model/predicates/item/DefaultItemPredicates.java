package com.supermartijn642.fusion.api.model.predicates.item;

import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.predicates.item.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;

import java.util.Arrays;

/**
 * Used to create instances of the default {@link ItemModelPredicate}s provided by Fusion.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public final class DefaultItemPredicates {

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static ItemModelPredicate and(ItemModelPredicate... predicates){
        return new AndItemModelPredicate(Arrays.asList(predicates));
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ItemModelPredicate or(ItemModelPredicate... predicates){
        return new OrItemModelPredicate(Arrays.asList(predicates));
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ItemModelPredicate not(ItemModelPredicate predicate){
        return new NotItemModelPredicate(predicate);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is equal to the given count.
     */
    public static ItemModelPredicate count(int count){
        return count(count, count);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is between the given minimum and maximum counts (both inclusive).
     */
    public static ItemModelPredicate count(int min, int max){
        return new CountItemModelPredicate(Either.left(min), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is greater than or equal to the given minimum and the number of items in the stack as percentage of the maximum stack size is less than the given maximum value.
     */
    public static ItemModelPredicate count(int min, float maxPercentage){
        return new CountItemModelPredicate(Either.left(min), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is less than or equal to the given maximum and the number of items in the stack as percentage of the maximum stack size is greater than the given minimum value.
     */
    public static ItemModelPredicate count(float minPercentage, int max){
        return new CountItemModelPredicate(Either.right(minPercentage), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack as percentage of the maximum stack size is between the given minimum and maximum values.
     */
    public static ItemModelPredicate count(float minPercentage, float maxPercentage){
        return new CountItemModelPredicate(Either.right(minPercentage), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is between the given minimum and maximum values (both inclusive).
     */
    public static ItemModelPredicate durability(int min, int max){
        return new DurabilityItemModelPredicate(Either.left(min), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is greater than or equal to the given minimum and the remaining durability of the item as percentage of the total durability is less than the given maximum value.
     */
    public static ItemModelPredicate durability(int min, float maxPercentage){
        return new DurabilityItemModelPredicate(Either.left(min), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is less than or equal to the given maximum and the remaining durability of the item as percentage of the total durability is greater than the given minimum value.
     */
    public static ItemModelPredicate durability(float minPercentage, int max){
        return new DurabilityItemModelPredicate(Either.right(minPercentage), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item as percentage of the total durability is between the given minimum and maximum values.
     */
    public static ItemModelPredicate durability(float minPercentage, float maxPercentage){
        return new DurabilityItemModelPredicate(Either.right(minPercentage), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the item has the given enchantment.
     */
    public static ItemModelPredicate enchantment(ResourceLocation enchantment){
        return enchantment(enchantment, 1, 255);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is equal to the given level.
     */
    public static ItemModelPredicate enchantment(ResourceLocation enchantment, int level){
        return enchantment(enchantment, level, level);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is between the given minimum and maximum levels (both inclusive).
     */
    public static ItemModelPredicate enchantment(ResourceLocation enchantment, int minLevel, int maxLevel){
        return new EnchantmentItemModelPredicate(enchantment, minLevel, maxLevel);
    }

    /**
     * Creates a predicate which is satisfied if the item is the given kind of potion.
     */
    public static ItemModelPredicate potion(Potion potion){
        return potion(BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    /**
     * Creates a predicate which is satisfied if the item is the given kind of potion.
     */
    public static ItemModelPredicate potion(Holder<Potion> potion){
        return new PotionItemModelPredicate(potion);
    }
}

package com.supermartijn642.fusion.api.model.modifier.item;

import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;

/**
 * Used to create instances of the default {@link ItemPredicate}s provided by Fusion.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public final class DefaultItemPredicates {

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates which need to be satisfied
     */
    public static ItemPredicate and(ItemPredicate... predicates){
        return new AndItemPredicate(Arrays.asList(predicates));
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static ItemPredicate or(ItemPredicate... predicates){
        return new OrItemPredicate(Arrays.asList(predicates));
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static ItemPredicate not(ItemPredicate predicate){
        return new NotItemPredicate(predicate);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is equal to the given count.
     */
    public static ItemPredicate count(int count){
        return count(count, count);
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is between the given minimum and maximum counts (both inclusive).
     */
    public static ItemPredicate count(int min, int max){
        return new CountItemPredicate(Either.left(min), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is greater than or equal to the given minimum and the number of items in the stack as percentage of the maximum stack size is less than the given maximum value.
     */
    public static ItemPredicate count(int min, float maxPercentage){
        return new CountItemPredicate(Either.left(min), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack is less than or equal to the given maximum and the number of items in the stack as percentage of the maximum stack size is greater than the given minimum value.
     */
    public static ItemPredicate count(float minPercentage, int max){
        return new CountItemPredicate(Either.right(minPercentage), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the number of items in the stack as percentage of the maximum stack size is between the given minimum and maximum values.
     */
    public static ItemPredicate count(float minPercentage, float maxPercentage){
        return new CountItemPredicate(Either.right(minPercentage), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is between the given minimum and maximum values (both inclusive).
     */
    public static ItemPredicate durability(int min, int max){
        return new DurabilityItemPredicate(Either.left(min), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is greater than or equal to the given minimum and the remaining durability of the item as percentage of the total durability is less than the given maximum value.
     */
    public static ItemPredicate durability(int min, float maxPercentage){
        return new DurabilityItemPredicate(Either.left(min), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item is less than or equal to the given maximum and the remaining durability of the item as percentage of the total durability is greater than the given minimum value.
     */
    public static ItemPredicate durability(float minPercentage, int max){
        return new DurabilityItemPredicate(Either.right(minPercentage), Either.left(max));
    }

    /**
     * Creates a predicate which is satisfied when the remaining durability of the item as percentage of the total durability is between the given minimum and maximum values.
     */
    public static ItemPredicate durability(float minPercentage, float maxPercentage){
        return new DurabilityItemPredicate(Either.right(minPercentage), Either.right(maxPercentage));
    }

    /**
     * Creates a predicate which is satisfied when the item has the given enchantment.
     */
    public static ItemPredicate enchantment(ResourceLocation enchantment){
        return enchantment(enchantment, 1, 255);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is equal to the given level.
     */
    public static ItemPredicate enchantment(ResourceLocation enchantment, int level){
        return enchantment(enchantment, level, level);
    }

    /**
     * Creates a predicate which is satisfied when the item's level of the given enchantment is between the given minimum and maximum levels (both inclusive).
     */
    public static ItemPredicate enchantment(ResourceLocation enchantment, int minLevel, int maxLevel){
        return new EnchantmentItemPredicate(Enchantment.getEnchantmentID(ForgeRegistries.ENCHANTMENTS.getValue(enchantment)), minLevel, maxLevel);
    }

    /**
     * Creates a predicate which is satisfied if the item is the given kind of potion.
     */
    public static ItemPredicate potion(Potion potion){
        return potion(potion);
    }
}

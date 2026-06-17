package com.supermartijn642.fusion.entity.model.predicates;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Used to create instances of the default {@link EntityModelPredicate}s provided by Fusion.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public final class DefaultEntityModelPredicates {

    /**
     * Predicate that always evaluates to true.
     */
    public static EntityModelPredicate always(){
        return TrueEntityModelPredicate.INSTANCE;
    }

    /**
     * Predicate that always evaluates to false.
     */
    public static EntityModelPredicate never(){
        return FalseEntityModelPredicate.INSTANCE;
    }

    /**
     * Combines the given predicates such that all predicates should be satisfied.
     * @param predicates predicates that need to be satisfied
     */
    public static EntityModelPredicate and(EntityModelPredicate... predicates){
        return AndEntityModelPredicate.create(predicates);
    }

    /**
     * Combines the given predicates such that at least one predicate should be satisfied.
     * @param predicates predicates of which any must be satisfied
     */
    public static EntityModelPredicate or(EntityModelPredicate... predicates){
        return OrEntityModelPredicate.create(predicates);
    }

    /**
     * Inverts the given predicate.
     * @param predicate predicate of which the inverse will be taken
     */
    public static EntityModelPredicate not(EntityModelPredicate predicate){
        return NotEntityModelPredicate.create(predicate);
    }

    /**
     * Predicate that evaluates whether an entity is between the given minimum and maximum heights.
     * @param minHeight height that the entity needs to be above
     * @param maxHeight height that the entity needs to be below
     */
    public static EntityModelPredicate altitude(int minHeight, int maxHeight){
        return AltitudeEntityModelPredicate.create(minHeight, maxHeight);
    }

    /**
     * Predicate that evaluates whether an entity is a baby.
     */
    public static EntityModelPredicate isBaby(){
        return BabyEntityModelPredicate.INSTANCE;
    }

    /**
     * Creates a predicate that evaluates whether an entity is in the given biomes.
     */
    public static EntityModelPredicate biome(ResourceKey<Biome>... biomes){
        return BiomeEntityModelPredicate.create(biomes);
    }

    /**
     * Creates a predicate that evaluates whether an entity is in the given biomes.
     */
    public static EntityModelPredicate biome(Identifier... biomes){
        return BiomeEntityModelPredicate.create(biomes);
    }

    /**
     * Creates a predicate that evaluates whether an entity is in one of the given dimensions.
     */
    public static EntityModelPredicate dimension(ResourceKey<Level>... dimensions){
        return DimensionEntityModelPredicate.create(dimensions);
    }

    /**
     * Creates a predicate that evaluates whether an entity is in one of the given dimensions.
     */
    public static EntityModelPredicate dimension(Identifier... dimensions){
        return DimensionEntityModelPredicate.create(dimensions);
    }
}

package com.supermartijn642.fusion.entity.model.predicates;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public interface EntityModelPredicate {

    boolean test(Entity entity);

    /**
     * Simplifies the predicate. May be used to simplify user properties.
     * For example, an and-predicate may flatten nested and-predicates or an empty or-predicate may return a false-predicate.
     */
    default EntityModelPredicate simplify(){
        return this;
    }

    /**
     * Serializer for this predicate.
     */
    Serializer<? extends EntityModelPredicate> getSerializer();


    /**
     * Checks whether this predicate is {@link DefaultEntityModelPredicates#always()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysTrue(){
        return this == DefaultEntityModelPredicates.always();
    }

    /**
     * Checks whether this predicate is {@link DefaultEntityModelPredicates#never()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysFalse(){
        return this == DefaultEntityModelPredicates.never();
    }

    /**
     * Adds a requirement to this predicate.
     */
    @ApiStatus.NonExtendable
    default EntityModelPredicate and(EntityModelPredicate... predicates){
        EntityModelPredicate[] allPredicates = new EntityModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultEntityModelPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    @ApiStatus.NonExtendable
    default EntityModelPredicate or(EntityModelPredicate... predicates){
        EntityModelPredicate[] allPredicates = new EntityModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultEntityModelPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this resource condition.
     */
    @ApiStatus.NonExtendable
    default EntityModelPredicate negate(){
        return DefaultEntityModelPredicates.not(this);
    }
}

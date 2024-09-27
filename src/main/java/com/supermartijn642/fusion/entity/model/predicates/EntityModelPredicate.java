package com.supermartijn642.fusion.entity.model.predicates;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public interface EntityModelPredicate {

    boolean test(Entity entity);

    /**
     * @return the serializer for this predicate
     */
    Serializer<? extends EntityModelPredicate> getSerializer();
}

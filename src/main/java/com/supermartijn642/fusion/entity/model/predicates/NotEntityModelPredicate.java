package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class NotEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(EntityModelPredicate predicate){
        return new NotEntityModelPredicate(predicate);
    }

    public static final Serializer<NotEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public NotEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            EntityModelPredicate predicate = EntityModelPredicateRegistryImpl.deserializePredicate(json.getAsJsonObject("predicate"));
            return new NotEntityModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotEntityModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicate", EntityModelPredicateRegistryImpl.serializePredicate(value.predicate));
            return json;
        }
    };

    private final EntityModelPredicate predicate;

    private NotEntityModelPredicate(EntityModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(Entity entity){
        return !this.predicate.test(entity);
    }

    @Override
    public EntityModelPredicate simplify(){
        EntityModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultEntityModelPredicates.never();
        if(simplified.alwaysFalse())
            return DefaultEntityModelPredicates.always();
        return new NotEntityModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof NotEntityModelPredicate that)) return false;

        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}

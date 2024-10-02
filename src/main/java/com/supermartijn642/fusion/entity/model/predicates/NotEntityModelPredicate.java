package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class NotEntityModelPredicate implements EntityModelPredicate {

    public static final Serializer<NotEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public NotEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            EntityModelPredicate predicate = EntityModelPredicateRegistry.deserializeEntityModelPredicate(json.getAsJsonObject("predicate"));
            return new NotEntityModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotEntityModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicates", EntityModelPredicateRegistry.serializeEntityModelPredicate(value.predicate));
            return json;
        }
    };

    private final EntityModelPredicate predicate;

    public NotEntityModelPredicate(EntityModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(Entity entity){
        return !this.predicate.test(entity);
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

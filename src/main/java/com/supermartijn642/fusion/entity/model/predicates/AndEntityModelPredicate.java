package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class AndEntityModelPredicate implements EntityModelPredicate {

    public static final Serializer<AndEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public AndEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            List<EntityModelPredicate> predicates = new ArrayList<>();
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                EntityModelPredicate predicate = EntityModelPredicateRegistry.deserializeEntityModelPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndEntityModelPredicate(predicates);
        }

        @Override
        public JsonObject serialize(AndEntityModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(EntityModelPredicate predicate : value.predicates)
                predicatesJson.add(EntityModelPredicateRegistry.serializeEntityModelPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<EntityModelPredicate> predicates;

    public AndEntityModelPredicate(List<EntityModelPredicate> predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(Entity entity){
        for(EntityModelPredicate predicate : this.predicates){
            if(!predicate.test(entity))
                return false;
        }
        return true;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

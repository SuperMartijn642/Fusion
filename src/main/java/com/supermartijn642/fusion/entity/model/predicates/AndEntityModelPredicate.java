package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class AndEntityModelPredicate implements EntityModelPredicate {

    public static AndEntityModelPredicate create(EntityModelPredicate... predicates){
        return new AndEntityModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<AndEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public AndEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<EntityModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                EntityModelPredicate predicate = EntityModelPredicateRegistryImpl.deserializePredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndEntityModelPredicate(predicates.toArray(new EntityModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(AndEntityModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(EntityModelPredicate predicate : value.predicates)
                predicatesJson.add(EntityModelPredicateRegistryImpl.serializePredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final EntityModelPredicate[] predicates;

    private AndEntityModelPredicate(EntityModelPredicate[] predicates){
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
    public EntityModelPredicate simplify(){
        List<EntityModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(EntityModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysFalse())
                return DefaultEntityModelPredicates.never();
            if(predicate instanceof AndEntityModelPredicate)
                flattened.addAll(Arrays.asList(((AndEntityModelPredicate)predicate).predicates));
            else if(!predicate.alwaysTrue())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultEntityModelPredicates.always();
        return new AndEntityModelPredicate(flattened.toArray(new EntityModelPredicate[0]));
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof AndEntityModelPredicate that)) return false;

        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

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
public class OrEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(EntityModelPredicate... predicates){
        return new OrEntityModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<OrEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public OrEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<EntityModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                EntityModelPredicate predicate = EntityModelPredicateRegistryImpl.deserializePredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrEntityModelPredicate(predicates.toArray(new EntityModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(OrEntityModelPredicate value){
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

    private OrEntityModelPredicate(EntityModelPredicate[] predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(Entity entity){
        for(EntityModelPredicate predicate : this.predicates){
            if(predicate.test(entity))
                return true;
        }
        return false;
    }

    @Override
    public EntityModelPredicate simplify(){
        List<EntityModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(EntityModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysTrue())
                return DefaultEntityModelPredicates.always();
            if(predicate instanceof OrEntityModelPredicate)
                flattened.addAll(Arrays.asList(((OrEntityModelPredicate)predicate).predicates));
            else if(!predicate.alwaysFalse())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultEntityModelPredicates.never();
        return new OrEntityModelPredicate(flattened.toArray(new EntityModelPredicate[0]));
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrEntityModelPredicate that)) return false;

        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

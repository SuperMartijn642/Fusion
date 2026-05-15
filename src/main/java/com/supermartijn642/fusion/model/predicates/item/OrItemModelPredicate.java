package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class OrItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(ItemModelPredicate... predicates){
        return new OrItemModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<OrItemModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public OrItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<ItemModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ItemModelPredicate predicate = ItemModelPredicateRegistryImpl.deserializePredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrItemModelPredicate(predicates.toArray(new ItemModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(OrItemModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ItemModelPredicate predicate : value.predicates)
                predicatesJson.add(ItemModelPredicateRegistryImpl.serializePredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final ItemModelPredicate[] predicates;

    private OrItemModelPredicate(ItemModelPredicate[] predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(ItemStack stack){
        for(ItemModelPredicate predicate : this.predicates){
            if(predicate.test(stack))
                return true;
        }
        return false;
    }

    @Override
    public ItemModelPredicate simplify(){
        List<ItemModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(ItemModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysTrue())
                return DefaultItemModelPredicates.always();
            if(predicate instanceof OrItemModelPredicate)
                flattened.addAll(Arrays.asList(((OrItemModelPredicate)predicate).predicates));
            else if(!predicate.alwaysFalse())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultItemModelPredicates.never();
        return new OrItemModelPredicate(flattened.toArray(new ItemModelPredicate[0]));
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrItemModelPredicate that)) return false;

        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

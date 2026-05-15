package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class AndItemModelPredicate implements ItemModelPredicate {

    public static AndItemModelPredicate create(ItemModelPredicate... predicates){
        return new AndItemModelPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<AndItemModelPredicate> SERIALIZER = new Serializer<AndItemModelPredicate>() {
        @Override
        public AndItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<ItemModelPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ItemModelPredicate predicate = ItemModelPredicateRegistryImpl.deserializePredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndItemModelPredicate(predicates.toArray(new ItemModelPredicate[0]));
        }

        @Override
        public JsonObject serialize(AndItemModelPredicate value){
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

    private AndItemModelPredicate(ItemModelPredicate[] predicates){
        this.predicates = predicates;
    }

    public List<ItemModelPredicate> getPredicates(){
        return Collections.unmodifiableList(Arrays.asList(this.predicates));
    }

    @Override
    public boolean test(ItemStack stack){
        for(ItemModelPredicate predicate : this.predicates){
            if(!predicate.test(stack))
                return false;
        }
        return true;
    }

    @Override
    public ItemModelPredicate simplify(){
        List<ItemModelPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(ItemModelPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysFalse())
                return DefaultItemModelPredicates.never();
            if(predicate instanceof AndItemModelPredicate)
                flattened.addAll(Arrays.asList(((AndItemModelPredicate)predicate).predicates));
            else if(!predicate.alwaysTrue())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultItemModelPredicates.always();
        return new AndItemModelPredicate(flattened.toArray(new ItemModelPredicate[0]));
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof AndItemModelPredicate)) return false;

        AndItemModelPredicate that = (AndItemModelPredicate)o;
        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

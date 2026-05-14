package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class AndItemModelPredicate implements ItemModelPredicate {

    public static final Serializer<AndItemModelPredicate> SERIALIZER = new Serializer<AndItemModelPredicate>() {
        @Override
        public AndItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            List<ItemModelPredicate> predicates = new ArrayList<>();
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ItemModelPredicate predicate = ItemPredicateRegistry.deserializeItemPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndItemModelPredicate(predicates);
        }

        @Override
        public JsonObject serialize(AndItemModelPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ItemModelPredicate predicate : value.predicates)
                predicatesJson.add(ItemPredicateRegistry.serializeItemPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<ItemModelPredicate> predicates;

    public AndItemModelPredicate(List<ItemModelPredicate> predicates){
        this.predicates = predicates;
    }

    public List<ItemModelPredicate> getPredicates(){
        return Collections.unmodifiableList(this.predicates);
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
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

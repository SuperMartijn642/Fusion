package com.supermartijn642.fusion.model.modifiers.item.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.modifier.item.ItemPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class OrItemPredicate implements ItemPredicate {

    public static final Serializer<OrItemPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public OrItemPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            List<ItemPredicate> predicates = new ArrayList<>();
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ItemPredicate predicate = ItemPredicateRegistry.deserializeItemPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrItemPredicate(predicates);
        }

        @Override
        public JsonObject serialize(OrItemPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ItemPredicate predicate : value.predicates)
                predicatesJson.add(ItemPredicateRegistry.serializeItemPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<ItemPredicate> predicates;

    public OrItemPredicate(List<ItemPredicate> predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean test(ItemStack stack){
        for(ItemPredicate predicate : this.predicates){
            if(predicate.test(stack))
                return true;
        }
        return false;
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}

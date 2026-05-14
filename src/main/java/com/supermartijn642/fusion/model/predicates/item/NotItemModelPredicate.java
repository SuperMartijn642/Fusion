package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class NotItemModelPredicate implements ItemModelPredicate {

    public static final Serializer<NotItemModelPredicate> SERIALIZER = new Serializer<NotItemModelPredicate>() {
        @Override
        public NotItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            ItemModelPredicate predicate = ItemPredicateRegistry.deserializeItemPredicate(json.getAsJsonObject("predicate"));
            return new NotItemModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicates", ItemPredicateRegistry.serializeItemPredicate(value.predicate));
            return json;
        }
    };

    private final ItemModelPredicate predicate;

    public NotItemModelPredicate(ItemModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(ItemStack stack){
        return !this.predicate.test(stack);
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.model.modifiers.item.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class NotItemPredicate implements ItemPredicate {

    public static final Serializer<NotItemPredicate> SERIALIZER = new Serializer<NotItemPredicate>() {
        @Override
        public NotItemPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            ItemPredicate predicate = ItemPredicateRegistry.deserializeItemPredicate(json.getAsJsonObject("predicate"));
            return new NotItemPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotItemPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicates", ItemPredicateRegistry.serializeItemPredicate(value.predicate));
            return json;
        }
    };

    private final ItemPredicate predicate;

    public NotItemPredicate(ItemPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(ItemStack stack){
        return !this.predicate.test(stack);
    }

    @Override
    public Serializer<? extends ItemPredicate> getSerializer(){
        return SERIALIZER;
    }
}

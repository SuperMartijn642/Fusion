package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class NotItemModelPredicate implements ItemModelPredicate {

    public static ItemModelPredicate create(ItemModelPredicate predicate){
        return new NotItemModelPredicate(predicate);
    }

    public static final Serializer<NotItemModelPredicate> SERIALIZER = new Serializer<NotItemModelPredicate>() {
        @Override
        public NotItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            ItemModelPredicate predicate = FusionItemModelPredicateRegistry.deserializeItemModelPredicate(json.getAsJsonObject("predicate"));
            return new NotItemModelPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotItemModelPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicate", FusionItemModelPredicateRegistry.serializeItemModelPredicate(value.predicate));
            return json;
        }
    };

    private final ItemModelPredicate predicate;

    private NotItemModelPredicate(ItemModelPredicate predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean test(ItemStack stack){
        return !this.predicate.test(stack);
    }

    @Override
    public ItemModelPredicate simplify(){
        ItemModelPredicate simplified = this.predicate.simplify();
        if(simplified.alwaysTrue())
            return DefaultItemModelPredicates.never();
        if(simplified.alwaysFalse())
            return DefaultItemModelPredicates.always();
        return new NotItemModelPredicate(simplified);
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof NotItemModelPredicate)) return false;

        NotItemModelPredicate that = (NotItemModelPredicate)o;
        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}

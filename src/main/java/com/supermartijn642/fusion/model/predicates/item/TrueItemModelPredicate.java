package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.item.ItemStack;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class TrueItemModelPredicate implements ItemModelPredicate {

    public static final TrueItemModelPredicate INSTANCE = new TrueItemModelPredicate();
    public static final Serializer<TrueItemModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public TrueItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(TrueItemModelPredicate value){
            return null;
        }
    };

    private TrueItemModelPredicate(){
    }

    @Override
    public boolean test(ItemStack stack){
        return true;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

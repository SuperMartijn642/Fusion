package com.supermartijn642.fusion.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.item.ItemStack;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class FalseItemModelPredicate implements ItemModelPredicate {

    public static final FalseItemModelPredicate INSTANCE = new FalseItemModelPredicate();
    public static final Serializer<FalseItemModelPredicate> SERIALIZER = new Serializer<FalseItemModelPredicate>() {
        @Override
        public FalseItemModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(FalseItemModelPredicate value){
            return null;
        }
    };

    private FalseItemModelPredicate(){
    }

    @Override
    public boolean test(ItemStack stack){
        return false;
    }

    @Override
    public Serializer<? extends ItemModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class FalseEntityModelPredicate implements EntityModelPredicate {

    public static final FalseEntityModelPredicate INSTANCE = new FalseEntityModelPredicate();
    public static final Serializer<FalseEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public FalseEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(FalseEntityModelPredicate value){
            return new JsonObject();
        }
    };

    @Override
    public boolean test(Entity entity){
        return false;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class TrueEntityModelPredicate implements EntityModelPredicate {

    public static final TrueEntityModelPredicate INSTANCE = new TrueEntityModelPredicate();
    public static final Serializer<TrueEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public TrueEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(TrueEntityModelPredicate value){
            return new JsonObject();
        }
    };

    @Override
    public boolean test(Entity entity){
        return true;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class BabyEntityModelPredicate implements EntityModelPredicate {

    public static final BabyEntityModelPredicate INSTANCE = new BabyEntityModelPredicate();
    public static final Serializer<BabyEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public BabyEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(BabyEntityModelPredicate value){
            return new JsonObject();
        }
    };

    private BabyEntityModelPredicate(){
    }

    @Override
    public boolean test(Entity entity){
        return entity instanceof AgeableMob && ((AgeableMob)entity).isBaby();
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

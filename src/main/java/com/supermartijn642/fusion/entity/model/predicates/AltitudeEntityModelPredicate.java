package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.world.entity.Entity;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class AltitudeEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(int minHeight, int maxHeight){
        if(minHeight > maxHeight)
            throw new IllegalArgumentException("Minimum height must be less than or equal to maximum height!");
        return new AltitudeEntityModelPredicate(minHeight, maxHeight);
    }

    public static final Serializer<AltitudeEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public AltitudeEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("min_height") && !json.has("max_height"))
                throw new JsonParseException("Altitude-predicate must have at least one of 'min_height' or 'max_height'!");

            // Minimum height
            int min = Integer.MIN_VALUE;
            if(json.has("min_height")){
                if(!json.get("min_height").isJsonPrimitive() || !json.getAsJsonPrimitive("min_height").isNumber())
                    throw new JsonParseException("Property 'min_height' must be a number!");
                min = json.getAsJsonPrimitive("min_height").getAsInt();
            }

            // Maximum height
            int max = Integer.MAX_VALUE;
            if(json.has("max_height")){
                if(!json.get("max_height").isJsonPrimitive() || !json.getAsJsonPrimitive("max_height").isNumber())
                    throw new JsonParseException("Property 'max_height' must be a number!");
                max = json.getAsJsonPrimitive("max_height").getAsInt();
            }

            // Validate min <= max
            if(min > max)
                throw new JsonParseException("Minimum height must be less than or equal to maximum height!");
            return new AltitudeEntityModelPredicate(min, max);
        }

        @Override
        public JsonObject serialize(AltitudeEntityModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.min != Integer.MIN_VALUE)
                json.addProperty("min_height", value.min);
            if(value.max != Integer.MAX_VALUE)
                json.addProperty("max_height", value.max);
            return json;
        }
    };

    private final int min, max;

    private AltitudeEntityModelPredicate(int min, int max){
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(Entity entity){
        int height = entity.blockPosition().getY();
        return this.min <= height && this.max >= height;
    }

    @Override
    public EntityModelPredicate simplify(){
        return this.min == Integer.MIN_VALUE && this.max == Integer.MAX_VALUE ?
            DefaultEntityModelPredicates.always() : this;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

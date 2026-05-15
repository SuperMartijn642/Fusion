package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import org.jetbrains.annotations.Nullable;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class AltitudeBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(int minHeight, int maxHeight){
        if(minHeight > maxHeight)
            throw new IllegalArgumentException("Minimum height must be less than or equal to maximum height!");
        return new AltitudeBlockStateModelPredicate(minHeight, maxHeight);
    }

    public static final Serializer<AltitudeBlockStateModelPredicate> SERIALIZER = new Serializer<AltitudeBlockStateModelPredicate>() {
        @Override
        public AltitudeBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
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
            return new AltitudeBlockStateModelPredicate(min, max);
        }

        @Override
        public JsonObject serialize(AltitudeBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.min != Integer.MIN_VALUE)
                json.addProperty("min_height", value.min);
            if(value.max != Integer.MAX_VALUE)
                json.addProperty("max_height", value.max);
            return json;
        }
    };

    private final int min, max;

    private AltitudeBlockStateModelPredicate(int min, int max){
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(@Nullable IEnviromentBlockReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(pos == null)
            return false;
        int height = pos.getY();
        return this.min <= height && this.max >= height;
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.min == Integer.MIN_VALUE && this.max == Integer.MAX_VALUE ?
            DefaultBlockStateModelPredicates.always() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

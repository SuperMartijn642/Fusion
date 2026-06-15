package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceKey<Level> dimension){
        return new DimensionBlockStateModelPredicate(dimension.identifier());
    }

    public static BlockStateModelPredicate create(Identifier dimension){
        Objects.requireNonNull(dimension);
        return new DimensionBlockStateModelPredicate(dimension);
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public DimensionBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                // Ignore on this version
            }

            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                throw new JsonParseException("Dimension-predicate must have string property 'dimension'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                throw new JsonParseException("Dimension must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
            return new DimensionBlockStateModelPredicate(Identifier.parse(json.get("dimension").getAsString()));
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension.toString());
            return json;
        }
    };

    private final Identifier dimension;

    private DimensionBlockStateModelPredicate(Identifier dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return level instanceof Level && ((Level)level).dimension().identifier().equals(this.dimension);
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

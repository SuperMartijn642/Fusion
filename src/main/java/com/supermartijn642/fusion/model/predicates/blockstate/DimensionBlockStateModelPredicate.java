package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceLocation dimension){
        Objects.requireNonNull(dimension);
        return new DimensionBlockStateModelPredicate(dimension);
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<DimensionBlockStateModelPredicate>() {
        @Override
        public DimensionBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                throw new JsonParseException("Dimension-predicate must have string property 'dimension'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                throw new JsonParseException("Dimension must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
            return new DimensionBlockStateModelPredicate(new ResourceLocation(json.get("dimension").getAsString()));
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension.toString());
            return json;
        }
    };

    private final ResourceLocation dimension;

    private DimensionBlockStateModelPredicate(ResourceLocation dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return level instanceof World && ((World)level).getDimension().getType().getRegistryName().equals(this.dimension);
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

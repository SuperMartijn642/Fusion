package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.jetbrains.annotations.Nullable;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(int dimension){
        if(!DimensionManager.isDimensionRegistered(dimension))
            throw new JsonParseException("No dimension for id '" + dimension + "'!");
        return new DimensionBlockStateModelPredicate(dimension);
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<DimensionBlockStateModelPredicate>() {
        @Override
        public DimensionBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isNumber())
                throw new JsonParseException("Dimension-predicate must have integer property 'dimension'!");
            int dimension = json.get("dimension").getAsInt();
            if(!DimensionManager.isDimensionRegistered(dimension)){
                if(ignoreMissing)
                    return new DimensionBlockStateModelPredicate(null);
                throw new JsonParseException("No dimension for id '" + dimension + "'!");
            }
            return new DimensionBlockStateModelPredicate(dimension);
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension);
            return json;
        }
    };

    private final Integer dimension;

    private DimensionBlockStateModelPredicate(Integer dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        if(this.dimension == null)
            return false;
        return level instanceof World && ((World)level).provider.getDimension() == this.dimension;
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.dimension == null ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
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
            if(!json.has("dimension") || !json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isNumber())
                throw new JsonParseException("Dimension-predicate must have integer property 'dimension'!");
            int dimension = json.get("dimension").getAsInt();
            if(!DimensionManager.isDimensionRegistered(dimension))
                throw new JsonParseException("No dimension for id '" + dimension + "'!");
            return new DimensionBlockStateModelPredicate(dimension);
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("dimension", value.dimension);
            return json;
        }
    };

    private final int dimension;

    private DimensionBlockStateModelPredicate(int dimension){
        this.dimension = dimension;
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        return level instanceof World && ((World)level).provider.getDimension() == this.dimension;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

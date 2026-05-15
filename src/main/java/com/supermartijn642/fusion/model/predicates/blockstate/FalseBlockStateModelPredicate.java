package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class FalseBlockStateModelPredicate implements BlockStateModelPredicate {

    public static final FalseBlockStateModelPredicate INSTANCE = new FalseBlockStateModelPredicate();
    public static final Serializer<FalseBlockStateModelPredicate> SERIALIZER = new Serializer<FalseBlockStateModelPredicate>() {
        @Override
        public FalseBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(FalseBlockStateModelPredicate value){
            return null;
        }
    };

    private FalseBlockStateModelPredicate(){
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        return false;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

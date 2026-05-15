package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class TrueBlockStateModelPredicate implements BlockStateModelPredicate {

    public static final TrueBlockStateModelPredicate INSTANCE = new TrueBlockStateModelPredicate();
    public static final Serializer<TrueBlockStateModelPredicate> SERIALIZER = new Serializer<TrueBlockStateModelPredicate>() {
        @Override
        public TrueBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(TrueBlockStateModelPredicate value){
            return null;
        }
    };

    private TrueBlockStateModelPredicate(){
    }

    @Override
    public boolean test(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return true;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class TrueModelPredicate implements ModelPredicate {

    public static final TrueModelPredicate INSTANCE = new TrueModelPredicate();
    public static final Serializer<TrueModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public TrueModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(TrueModelPredicate value){
            return null;
        }
    };

    private TrueModelPredicate(){
    }

    @Override
    public boolean testForBlockState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return true;
    }

    @Override
    public boolean testForItem(ItemStack stack){
        return true;
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

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
public class FalseModelPredicate implements ModelPredicate {

    public static final FalseModelPredicate INSTANCE = new FalseModelPredicate();
    public static final Serializer<FalseModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public FalseModelPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(FalseModelPredicate value){
            return null;
        }
    };

    private FalseModelPredicate(){
    }

    @Override
    public boolean testForBlockState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return false;
    }

    @Override
    public boolean testForItem(ItemStack stack){
        return false;
    }

    @Override
    public Serializer<? extends ModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

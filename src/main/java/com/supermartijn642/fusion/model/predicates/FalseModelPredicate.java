package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class FalseModelPredicate implements ModelPredicate {

    public static final FalseModelPredicate INSTANCE = new FalseModelPredicate();
    public static final Serializer<FalseModelPredicate> SERIALIZER = new Serializer<FalseModelPredicate>() {
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
    public boolean testForBlockState(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
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

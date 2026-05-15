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
public class TrueModelPredicate implements ModelPredicate {

    public static final TrueModelPredicate INSTANCE = new TrueModelPredicate();
    public static final Serializer<TrueModelPredicate> SERIALIZER = new Serializer<TrueModelPredicate>() {
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
    public boolean testForBlockState(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
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

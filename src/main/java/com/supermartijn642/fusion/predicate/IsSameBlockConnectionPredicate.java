package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsSameBlockConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<IsSameBlockConnectionPredicate> SERIALIZER = new Serializer<IsSameBlockConnectionPredicate>() {
        @Override
        public IsSameBlockConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return new IsSameBlockConnectionPredicate();
        }

        @Override
        public JsonObject serialize(IsSameBlockConnectionPredicate value){
            return null;
        }
    };

    @Override
    public boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return ownState != null && ownState.getBlock() == otherState.getBlock();
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

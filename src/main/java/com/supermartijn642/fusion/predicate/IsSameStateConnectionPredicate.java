package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsSameStateConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<IsSameStateConnectionPredicate> SERIALIZER = new Serializer<IsSameStateConnectionPredicate>() {
        @Override
        public IsSameStateConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return new IsSameStateConnectionPredicate();
        }

        @Override
        public JsonObject serialize(IsSameStateConnectionPredicate value){
            return null;
        }
    };

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return ownState == otherState;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.model.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsSameStateConnectionPredicate implements ConnectionPredicate {

    public static final IsSameStateConnectionPredicate INSTANCE = new IsSameStateConnectionPredicate();
    public static final Serializer<IsSameStateConnectionPredicate> SERIALIZER = new Serializer<IsSameStateConnectionPredicate>() {
        @Override
        public IsSameStateConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(IsSameStateConnectionPredicate value){
            return null;
        }
    };

    private IsSameStateConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return ownState == otherState;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

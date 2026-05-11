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
public class IsSameBlockConnectionPredicate implements ConnectionPredicate {

    public static final IsSameBlockConnectionPredicate INSTANCE = new IsSameBlockConnectionPredicate();
    public static final Serializer<IsSameBlockConnectionPredicate> SERIALIZER = new Serializer<IsSameBlockConnectionPredicate>() {
        @Override
        public IsSameBlockConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(IsSameBlockConnectionPredicate value){
            return null;
        }
    };

    private IsSameBlockConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return ownState != null && ownState.getBlock() == otherState.getBlock();
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

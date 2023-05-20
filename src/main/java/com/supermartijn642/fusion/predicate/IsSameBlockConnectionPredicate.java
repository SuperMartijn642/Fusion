package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IsSameBlockConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<IsSameBlockConnectionPredicate> SERIALIZER = new Serializer<>() {
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
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return ownState != null && ownState.getBlock() == otherState.getBlock();
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

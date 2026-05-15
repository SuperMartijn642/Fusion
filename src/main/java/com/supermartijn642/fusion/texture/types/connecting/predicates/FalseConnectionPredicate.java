package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class FalseConnectionPredicate implements ConnectionPredicate {

    public static final FalseConnectionPredicate INSTANCE = new FalseConnectionPredicate();
    public static final Serializer<FalseConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public FalseConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(FalseConnectionPredicate value){
            return null;
        }
    };

    private FalseConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return false;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

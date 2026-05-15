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
public class TrueConnectionPredicate implements ConnectionPredicate {

    public static final TrueConnectionPredicate INSTANCE = new TrueConnectionPredicate();
    public static final Serializer<TrueConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public TrueConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            return INSTANCE;
        }

        @Override
        public JsonObject serialize(TrueConnectionPredicate value){
            return null;
        }
    };

    private TrueConnectionPredicate(){
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return true;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

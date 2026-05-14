package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class NotConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<NotConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public NotConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicate") || !json.get("predicate").isJsonObject())
                throw new JsonParseException("Not-predicate must have object property 'predicate'!");
            // Deserialize the predicate
            ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(json.getAsJsonObject("predicate"));
            return new NotConnectionPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicate", FusionConnectionPredicateRegistry.serializeConnectionPredicate(value.predicate));
            return json;
        }
    };

    private final ConnectionPredicate predicate;

    public <T extends ConnectionPredicate> NotConnectionPredicate(T predicate){
        this.predicate = predicate;
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return !this.predicate.shouldConnect(side, ownState, otherState, blockInFront, direction);
    }

    @Override
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return !this.predicate.shouldConnect(level, pos, side, ownState, otherState, blockInFront, direction);
    }

    @Override
    public boolean isSensitive(){
        return this.predicate.isSensitive();
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof NotConnectionPredicate that)) return false;

        return this.predicate.equals(that.predicate);
    }

    @Override
    public int hashCode(){
        return this.predicate.hashCode();
    }
}

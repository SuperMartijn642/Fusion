package com.supermartijn642.fusion.model.types.connecting.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class OrConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<OrConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public OrConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            List<ConnectionPredicate> predicates = new ArrayList<>();
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrConnectionPredicate(predicates);
        }

        @Override
        public JsonObject serialize(OrConnectionPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ConnectionPredicate predicate : value.predicates)
                predicatesJson.add(FusionConnectionPredicateRegistry.serializeConnectionPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<ConnectionPredicate> predicates;
    private final boolean isSensitive;

    public OrConnectionPredicate(List<ConnectionPredicate> predicates){
        this.predicates = predicates;
        this.isSensitive = predicates.stream().anyMatch(ConnectionPredicate::isSensitive);
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        for(ConnectionPredicate predicate : this.predicates){
            if(predicate.shouldConnect(side, ownState, otherState, blockInFront, direction))
                return true;
        }
        return false;
    }

    @Override
    public boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        for(ConnectionPredicate predicate : this.predicates){
            if(predicate.shouldConnect(level, pos, side, ownState, otherState, blockInFront, direction))
                return true;
        }
        return false;
    }

    @Override
    public boolean isSensitive(){
        return this.isSensitive;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrConnectionPredicate that)) return false;

        return this.predicates.equals(that.predicates);
    }

    @Override
    public int hashCode(){
        return this.predicates.hashCode();
    }
}

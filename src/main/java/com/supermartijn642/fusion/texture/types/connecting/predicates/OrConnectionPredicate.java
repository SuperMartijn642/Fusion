package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class OrConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(ConnectionPredicate... predicates){
        return new OrConnectionPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<OrConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public OrConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("Or-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<ConnectionPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new OrConnectionPredicate(predicates.toArray(new ConnectionPredicate[0]));
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

    private final ConnectionPredicate[] predicates;
    private final boolean isSensitive;

    private OrConnectionPredicate(ConnectionPredicate[] predicates){
        this.predicates = predicates;
        this.isSensitive = Arrays.stream(predicates).anyMatch(ConnectionPredicate::isSensitive);
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
    public ConnectionPredicate simplify(){
        List<ConnectionPredicate> flattened = new ArrayList<>(this.predicates.length);
        for(ConnectionPredicate predicate : this.predicates){
            predicate = predicate.simplify();
            if(predicate.alwaysTrue())
                return DefaultConnectionPredicates.always();
            if(predicate instanceof OrConnectionPredicate)
                flattened.addAll(Arrays.asList(((OrConnectionPredicate)predicate).predicates));
            else if(!predicate.alwaysFalse())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultConnectionPredicates.never();
        return new OrConnectionPredicate(flattened.toArray(new ConnectionPredicate[0]));
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof OrConnectionPredicate that)) return false;

        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

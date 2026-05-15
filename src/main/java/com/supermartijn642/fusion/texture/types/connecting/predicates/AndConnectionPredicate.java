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
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class AndConnectionPredicate implements ConnectionPredicate {

    public static AndConnectionPredicate create(ConnectionPredicate... predicates){
        return new AndConnectionPredicate(Arrays.copyOf(predicates, predicates.length));
    }

    public static final Serializer<AndConnectionPredicate> SERIALIZER = new Serializer<AndConnectionPredicate>() {
        @Override
        public AndConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            List<ConnectionPredicate> predicates = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndConnectionPredicate(predicates.toArray(new ConnectionPredicate[0]));
        }

        @Override
        public JsonObject serialize(AndConnectionPredicate value){
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

    private AndConnectionPredicate(ConnectionPredicate[] predicates){
        this.predicates = predicates;
        this.isSensitive = Arrays.stream(predicates).anyMatch(ConnectionPredicate::isSensitive);
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        for(ConnectionPredicate predicate : this.predicates){
            if(!predicate.shouldConnect(side, ownState, otherState, blockInFront, direction))
                return false;
        }
        return true;
    }

    @Override
    public boolean shouldConnect(IBlockReader level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        for(ConnectionPredicate predicate : this.predicates){
            if(!predicate.shouldConnect(level, pos, side, ownState, otherState, blockInFront, direction))
                return false;
        }
        return true;
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
            if(predicate.alwaysFalse())
                return DefaultConnectionPredicates.never();
            if(predicate instanceof AndConnectionPredicate)
                flattened.addAll(Arrays.asList(((AndConnectionPredicate)predicate).predicates));
            else if(!predicate.alwaysTrue())
                flattened.add(predicate);
        }
        if(flattened.isEmpty())
            return DefaultConnectionPredicates.always();
        return new AndConnectionPredicate(flattened.toArray(new ConnectionPredicate[0]));
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof AndConnectionPredicate)) return false;

        AndConnectionPredicate that = (AndConnectionPredicate)o;
        return Arrays.equals(this.predicates, that.predicates);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.predicates);
    }
}

package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class AndConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<AndConnectionPredicate> SERIALIZER = new Serializer<AndConnectionPredicate>() {
        @Override
        public AndConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("predicates") || !json.get("predicates").isJsonArray())
                throw new JsonParseException("And-predicate must have array property 'predicates'!");
            List<ConnectionPredicate> predicates = new ArrayList<>();
            // Deserialize all the predicates from the 'predicates' array
            JsonArray array = json.getAsJsonArray("predicates");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'predicates' must only contain objects!");
                ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
            return new AndConnectionPredicate(predicates);
        }

        @Override
        public JsonObject serialize(AndConnectionPredicate value){
            JsonObject json = new JsonObject();
            // Create an array with all the serialized predicates
            JsonArray predicatesJson = new JsonArray();
            for(ConnectionPredicate predicate : value.predicates)
                predicatesJson.add(FusionPredicateRegistry.serializeConnectionPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<ConnectionPredicate> predicates;
    private final boolean isSensitive;

    public AndConnectionPredicate(List<ConnectionPredicate> predicates){
        this.predicates = predicates;
        this.isSensitive = predicates.stream().anyMatch(ConnectionPredicate::isSensitive);
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return this.predicates.stream().allMatch(predicate -> predicate.shouldConnect(side, ownState, otherState, blockInFront, direction));
    }

    @Override
    public boolean shouldConnect(IBlockReader level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return this.predicates.stream().allMatch(predicate -> predicate.shouldConnect(level, pos, side, ownState, otherState, blockInFront, direction));
    }

    @Override
    public boolean isSensitive(){
        return this.isSensitive;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

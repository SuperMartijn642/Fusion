package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
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
                ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
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
                predicatesJson.add(FusionPredicateRegistry.serializeConnectionPredicate(predicate));
            json.add("predicates", predicatesJson);
            return json;
        }
    };

    private final List<ConnectionPredicate> predicates;

    public OrConnectionPredicate(List<ConnectionPredicate> predicates){
        this.predicates = predicates;
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return this.predicates.stream().anyMatch(predicate -> predicate.shouldConnect(side, ownState, otherState, blockInFront, direction));
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

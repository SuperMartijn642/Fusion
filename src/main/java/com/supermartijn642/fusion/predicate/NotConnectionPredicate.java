package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
            JsonArray array = json.getAsJsonArray("predicates");
            ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(json.getAsJsonObject("predicate"));
            return new NotConnectionPredicate(predicate);
        }

        @Override
        public JsonObject serialize(NotConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.add("predicates", FusionPredicateRegistry.serializeConnectionPredicate(value.predicate));
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
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

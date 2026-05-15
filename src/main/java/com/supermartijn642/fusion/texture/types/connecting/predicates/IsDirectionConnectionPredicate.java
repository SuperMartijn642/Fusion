package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Created 12/10/2024 by SuperMartijn642
 */
public class IsDirectionConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(ConnectionDirection... directions){
        return new IsDirectionConnectionPredicate(directions);
    }

    public static final Serializer<IsDirectionConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public IsDirectionConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("direction") && !json.get("directions").isJsonArray())
                throw new JsonParseException("Is-direction-predicate must have at least one of 'direction' or 'directions'!");
            Set<ConnectionDirection> directions = new HashSet<>();
            if(json.has("direction"))
                readDirections("direction", json.get("direction"), directions);
            if(json.has("directions"))
                readDirections("directions", json.get("directions"), directions);
            if(directions.isEmpty())
                throw new JsonParseException("Is-direction-predicate must contain at least one valid direction!");
            return new IsDirectionConnectionPredicate(directions.toArray(ConnectionDirection[]::new));
        }

        private static void readDirections(String propertyName, JsonElement json, Set<ConnectionDirection> output){
            if(json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()){
                ConnectionDirection direction;
                try{
                    direction = ConnectionDirection.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
                }catch(IllegalArgumentException e){
                    throw new JsonParseException("Property '" + propertyName + "' has unknown direction '" + json.getAsString() + "'!");
                }
                if(!output.add(direction))
                    throw new JsonParseException("Duplicate direction '" + direction + "'!");
            }else if(json.isJsonArray()){
                for(JsonElement element : json.getAsJsonArray()){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Property '" + propertyName + "' must only contain strings!");
                    ConnectionDirection direction;
                    try{
                        direction = ConnectionDirection.valueOf(element.getAsString().toUpperCase(Locale.ROOT));
                    }catch(IllegalArgumentException e){
                        throw new JsonParseException("Property '" + propertyName + "' has unknown direction '" + element.getAsString() + "'!");
                    }
                    if(!output.add(direction))
                        throw new JsonParseException("Duplicate direction '" + direction + "'!");
                }
            }else
                throw new JsonParseException("Property '" + propertyName + "' must be a string or an array of strings!");
        }

        @Override
        public JsonObject serialize(IsDirectionConnectionPredicate value){
            JsonObject json = new JsonObject();
            // Create a list of all the directions
            List<String> directions = Arrays.stream(ConnectionDirection.values())
                .filter(d -> value.directions[d.ordinal()])
                .map(d -> d.name().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
            if(directions.size() == 1)
                json.addProperty("direction", directions.get(0));
            else{
                JsonArray directionsArray = new JsonArray();
                directions.forEach(directionsArray::add);
                json.add("directions", directionsArray);
            }
            return json;
        }
    };

    private final boolean[] directions = new boolean[ConnectionDirection.values().length];

    private IsDirectionConnectionPredicate(ConnectionDirection... directions){
        for(ConnectionDirection direction : directions)
            this.directions[direction.ordinal()] = true;
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return this.directions[direction.ordinal()];
    }

    @Override
    public ConnectionPredicate simplify(){
        for(boolean direction : this.directions){
            if(direction)
                return this;
        }
        return DefaultConnectionPredicates.never();
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof IsDirectionConnectionPredicate that)) return false;

        return Arrays.equals(this.directions, that.directions);
    }

    @Override
    public int hashCode(){
        return Arrays.hashCode(this.directions);
    }
}

package com.supermartijn642.fusion.model.types.connecting.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 22/02/2024 by SuperMartijn642
 */
public class MatchStateConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<MatchStateConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchStateConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match state predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = ResourceLocation.parse(json.get("block").getAsString());
            Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(identifier);
            if(optional.isEmpty())
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            Block block = optional.get();

            List<Pair<Property<?>,Set<?>>> properties = new ArrayList<>();
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match block predicate must have object property 'properties'!");
            if(json.getAsJsonObject("properties").isEmpty())
                throw new JsonParseException("At least one property must be specified for match state predicate!");
            for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
                // Parse the property
                Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
                if(property == null)
                    throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
                // Parse the values
                ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
                if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                    Optional<?> value = property.getValue(entry.getValue().getAsString());
                    if(value.isEmpty())
                        throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }else if(entry.getValue().isJsonArray()){
                    if(entry.getValue().getAsJsonArray().isEmpty())
                        throw new JsonParseException("Valid values for property '" + property.getName() + "' cannot be empty!");
                    for(JsonElement element : entry.getValue().getAsJsonArray()){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                        Optional<?> value = property.getValue(element.getAsString());
                        if(value.isEmpty())
                            throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                        builder.add(value.get());
                    }
                }else
                    throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                properties.add(Pair.of(property, builder.build()));
            }
            //noinspection unchecked
            properties = Arrays.asList(properties.toArray(Pair[]::new));

            return new MatchStateConnectionPredicate(block, properties);
        }

        @Override
        public JsonObject serialize(MatchStateConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(value.block).toString());
            JsonObject properties = new JsonObject();
            value.properties.stream()
                .map(p -> p.mapRight(values -> {
                    JsonArray array = new JsonArray(values.size());
                    //noinspection rawtypes,unchecked
                    values.stream().map(v -> ((Property)p.left()).getName((Comparable)v)).sorted().forEach(array::add);
                    return array;
                }))
                .map(p -> p.mapLeft(Property::getName))
                .sorted(Comparator.comparing(Pair::left))
                .forEach(pair -> properties.add(pair.left(), pair.right()));
            json.add("properties", properties);
            return json;
        }
    };

    private final Block block;
    private final List<Pair<Property<?>,Set<?>>> properties;
    private boolean compareStates = false;
    private Set<BlockState> states = null;

    public MatchStateConnectionPredicate(Block block, List<Pair<Property<?>,Set<?>>> properties){
        this.block = block;
        this.properties = properties;
        this.computeStates();
    }

    @SafeVarargs
    public MatchStateConnectionPredicate(Block block, Pair<Property<?>,?>... propertyPair){
        this.block = block;
        Map<Property<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<Property<?>,?> pair : propertyPair){
            Property<?> property = pair.left();
            if(!block.getStateDefinition().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + BuiltInRegistries.BLOCK.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<Property<?>,Set<?>>[] properties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<Property<?>,List<Object>> entry : propertyMap.entrySet())
            properties[index++] = Pair.of(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        this.properties = Arrays.asList(properties);
        this.computeStates();
    }

    private <T extends Comparable<T>> void computeStates(){
        // Compute the number of states matching this predicate
        Set<Property<?>> unrestrictedProperties = new HashSet<>(this.block.getStateDefinition().getProperties());
        int validStates = 1;
        for(Pair<Property<?>,Set<?>> pair : this.properties){
            validStates *= pair.right().size();
            unrestrictedProperties.remove(pair.left());
        }
        for(Property<?> property : unrestrictedProperties)
            validStates *= property.getPossibleValues().size();

        // If less than 64 states match, store and compare states directly
        if(validStates > 64)
            return;
        Stream<BlockState> states = Stream.of(this.block.getStateDefinition().any());
        for(Pair<Property<?>,Set<?>> pair : this.properties){
            Property<?> property = pair.left();
            Set<?> values = pair.right();
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> values.stream().map(value -> state.setValue((Property)property, (T)value)));
        }
        for(Property<?> property : unrestrictedProperties)
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> property.getAllValues().map(value -> state.setValue((Property)property, (T)value.value())));
        this.compareStates = true;
        this.states = states.collect(Collectors.toUnmodifiableSet());
        // Sanity check
        if(this.states.size() != validStates)
            throw new AssertionError("Got two different numbers of valid states: " + validStates + " and " + this.states.size() + "!");
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        if(this.compareStates)
            return this.states.contains(otherState);
        if(otherState.getBlock() != this.block)
            return false;
        for(Pair<Property<?>,Set<?>> property : this.properties){
            if(!property.right().contains(otherState.getValue(property.left())))
                return false;
        }
        return true;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof MatchStateConnectionPredicate that)) return false;

        return this.block.equals(that.block) && this.properties.equals(that.properties);
    }

    @Override
    public int hashCode(){
        int result = this.block.hashCode();
        result = 31 * result + this.properties.hashCode();
        return result;
    }
}

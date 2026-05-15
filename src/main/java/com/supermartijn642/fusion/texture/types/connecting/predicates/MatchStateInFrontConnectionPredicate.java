package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created 22/02/2024 by SuperMartijn642
 */
public class MatchStateInFrontConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(Block block, Pair<IProperty<?>,?>... properties){
        Map<IProperty<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<IProperty<?>,?> pair : properties){
            IProperty<?> property = pair.left();
            if(!block.getStateDefinition().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + Registry.BLOCK.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<IProperty<?>,Set<?>>[] flattenedProperties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<IProperty<?>,List<Object>> entry : propertyMap.entrySet())
            properties[index++] = Pair.of(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        return new MatchStateInFrontConnectionPredicate(block, flattenedProperties);
    }

    public static ConnectionPredicate create(BlockState state){
        //noinspection unchecked
        return new MatchStateInFrontConnectionPredicate(state.getBlock(), state.getProperties().stream().map(p -> Pair.of(p, ImmutableSet.of(state.getValue(p)))).toArray(Pair[]::new));
    }

    public static final Serializer<MatchStateInFrontConnectionPredicate> SERIALIZER = new Serializer<MatchStateInFrontConnectionPredicate>() {
        @Override
        public MatchStateInFrontConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match state predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
            if(!Registry.BLOCK.containsKey(identifier))
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            Block block = Registry.BLOCK.get(identifier);

            List<Pair<IProperty<?>,Set<?>>> properties = new ArrayList<>();
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match block predicate must have object property 'properties'!");
            if(json.getAsJsonObject("properties").size() == 0)
                throw new JsonParseException("At least one property must be specified for match state predicate!");
            for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
                // Parse the property
                IProperty<?> property = block.getStateDefinition().getProperty(entry.getKey());
                if(property == null)
                    throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
                // Parse the values
                ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
                if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                    Optional<?> value = property.getValue(entry.getValue().getAsString());
                    if(!value.isPresent())
                        throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }else if(entry.getValue().isJsonArray()){
                    if(entry.getValue().getAsJsonArray().size() == 0)
                        throw new JsonParseException("Valid values for property '" + property.getName() + "' cannot be empty!");
                    for(JsonElement element : entry.getValue().getAsJsonArray()){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                        Optional<?> value = property.getValue(element.getAsString());
                        if(!value.isPresent())
                            throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                        builder.add(value.get());
                    }
                }else
                    throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                properties.add(Pair.of(property, builder.build()));
            }
            //noinspection unchecked
            return new MatchStateInFrontConnectionPredicate(block, properties.toArray(new Pair[0]));
        }

        @Override
        public JsonObject serialize(MatchStateInFrontConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", Registry.BLOCK.getKey(value.block).toString());
            JsonObject properties = new JsonObject();
            Arrays.stream(value.properties)
                .map(p -> p.mapRight(values -> {
                    JsonArray array = new JsonArray();
                    //noinspection rawtypes,unchecked
                    values.stream().map(v -> ((IProperty)p.left()).getName((Comparable)v)).sorted().forEach(array::add);
                    return array;
                }))
                .map(p -> p.mapLeft(IProperty::getName))
                .sorted(Comparator.comparing(Pair::left))
                .forEach(pair -> properties.add(pair.left(), pair.right()));
            json.add("properties", properties);
            return json;
        }
    };

    private final Block block;
    private final Pair<IProperty<?>,Set<?>>[] properties;
    private boolean compareStates = false;
    private Set<BlockState> states = null;

    private MatchStateInFrontConnectionPredicate(Block block, Pair<IProperty<?>,Set<?>>[] properties){
        this.block = block;
        this.properties = properties;
        this.computeStates();
    }

    private <T extends Comparable<T>> void computeStates(){
        // Compute the number of states matching this predicate
        Set<IProperty<?>> unrestrictedProperties = new HashSet<>(this.block.getStateDefinition().getProperties());
        int validStates = 1;
        for(Pair<IProperty<?>,Set<?>> pair : this.properties){
            validStates *= pair.right().size();
            unrestrictedProperties.remove(pair.left());
        }
        for(IProperty<?> property : unrestrictedProperties)
            validStates *= property.getPossibleValues().size();

        // If less than 64 states match, store and compare states directly
        if(validStates > 64)
            return;
        Stream<BlockState> states = Stream.of(this.block.getStateDefinition().any());
        for(Pair<IProperty<?>,Set<?>> pair : this.properties){
            IProperty<?> property = pair.left();
            Set<?> values = pair.right();
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> values.stream().map(value -> state.setValue((IProperty)property, (T)value)));
        }
        for(IProperty<?> property : unrestrictedProperties)
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> property.getPossibleValues().stream().map(value -> state.setValue((IProperty)property, (T)value)));
        this.compareStates = true;
        //noinspection UnstableApiUsage
        this.states = states.collect(ImmutableSet.toImmutableSet());
        // Sanity check
        if(this.states.size() != validStates)
            throw new AssertionError("Got two different numbers of valid states: " + validStates + " and " + this.states.size() + "!");
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        if(this.compareStates)
            return this.states.contains(blockInFront);
        if(blockInFront.getBlock() != this.block)
            return false;
        for(Pair<IProperty<?>,Set<?>> property : this.properties){
            if(!property.right().contains(blockInFront.getValue(property.left())))
                return false;
        }
        return true;
    }

    @Override
    public ConnectionPredicate simplify(){
        List<Pair<IProperty<?>,Set<?>>> simplifiedProperties = new ArrayList<>(this.properties.length);
        for(Pair<IProperty<?>,Set<?>> pair : this.properties){
            Set<?> allowedValues = pair.right();
            if(allowedValues.isEmpty())
                return DefaultConnectionPredicates.never();
            IProperty<?> property = pair.left();
            if(property.getPossibleValues().size() != allowedValues.size())
                simplifiedProperties.add(pair);
        }
        if(simplifiedProperties.isEmpty())
            return DefaultConnectionPredicates.always();
        //noinspection unchecked
        return new MatchStateInFrontConnectionPredicate(this.block, simplifiedProperties.toArray(new Pair[0]));
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof MatchStateInFrontConnectionPredicate)) return false;

        MatchStateInFrontConnectionPredicate that = (MatchStateInFrontConnectionPredicate)o;
        return this.block.equals(that.block) && Arrays.equals(this.properties, that.properties);
    }

    @Override
    public int hashCode(){
        int result = this.block.hashCode();
        result = 31 * result + Arrays.hashCode(this.properties);
        return result;
    }
}

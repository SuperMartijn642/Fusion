package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 17/06/2026 by SuperMartijn642
 */
public class BlockStateMatcher {

    public static BlockStateMatcher create(Block block, Pair<Property<?>,?>... properties){
        Map<Property<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<Property<?>,?> pair : properties){
            Property<?> property = pair.left();
            if(!block.getStateDefinition().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + Registry.BLOCK.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "' in block '" + block + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<Property<?>,Set<?>>[] flattenedProperties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<Property<?>,List<Object>> entry : propertyMap.entrySet())
            flattenedProperties[index++] = Pair.of(entry.getKey(), Set.copyOf(entry.getValue()));
        return new BlockStateMatcher(block, flattenedProperties);
    }

    public static BlockStateMatcher parseProperties(Block block, JsonObject propertiesJson){
        ResourceLocation identifier = Registry.BLOCK.getKey(block);
        List<Pair<Property<?>,Set<?>>> properties = new ArrayList<>();
        for(Map.Entry<String,JsonElement> entry : propertiesJson.entrySet()){
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
        return new BlockStateMatcher(block, properties.toArray(new Pair[0]));
    }

    public static JsonObject serializeProperties(BlockStateMatcher matcher){
        JsonObject json = new JsonObject();
        Arrays.stream(matcher.properties)
            .map(p -> p.mapRight(values -> {
                JsonArray array = new JsonArray(values.size());
                //noinspection rawtypes,unchecked
                values.stream().map(v -> ((Property)p.left()).getName((Comparable)v)).sorted().forEach(array::add);
                return array;
            }))
            .map(p -> p.mapLeft(Property::getName))
            .sorted(Comparator.comparing(Pair::left))
            .forEach(pair -> json.add(pair.left(), pair.right()));
        return json;
    }

    private final Block block;
    private final Pair<Property<?>,Set<?>>[] properties;
    private final boolean compareStates;
    private final Set<BlockState> states;
    private Integer hashCode;

    private BlockStateMatcher(Block block, Pair<Property<?>,Set<?>>[] properties){
        this.block = block;
        this.properties = properties;
        this.states = computeStates(block, properties);
        this.compareStates = this.states != null;
    }

    public Block getBlock(){
        return this.block;
    }

    public Pair<Property<?>,Set<?>>[] getProperties(){
        return this.properties;
    }

    public boolean matches(BlockState state){
        if(this.compareStates)
            return this.states.contains(state);
        if(state.getBlock() != this.block)
            return false;
        for(Pair<Property<?>,Set<?>> property : this.properties){
            if(!property.right().contains(state.getValue(property.left())))
                return false;
        }
        return true;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BlockStateMatcher that)) return false;

        return this.block.equals(that.block) && Arrays.equals(this.properties, that.properties);
    }

    @Override
    public int hashCode(){
        if(this.hashCode == null){
            int hashCode = this.block.hashCode();
            hashCode = 31 * hashCode + Arrays.hashCode(this.properties);
            this.hashCode = hashCode;
        }
        return this.hashCode;
    }

    private static <T extends Comparable<T>> Set<BlockState> computeStates(Block block, Pair<Property<?>,Set<?>>[] properties){
        // Compute the number of states matching this predicate
        Set<Property<?>> unrestrictedProperties = new HashSet<>(block.getStateDefinition().getProperties());
        int validStates = 1;
        for(Pair<Property<?>,Set<?>> pair : properties){
            validStates *= pair.right().size();
            unrestrictedProperties.remove(pair.left());
        }
        for(Property<?> property : unrestrictedProperties)
            validStates *= property.getPossibleValues().size();

        // If less than 64 states match, store and compare states directly
        if(validStates > 64)
            return null;
        Stream<BlockState> states = Stream.of(block.getStateDefinition().any());
        for(Pair<Property<?>,Set<?>> pair : properties){
            Property<?> property = pair.left();
            Set<?> values = pair.right();
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> values.stream().map(value -> state.setValue((Property)property, (T)value)));
        }
        for(Property<?> property : unrestrictedProperties)
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> property.getAllValues().map(value -> state.setValue((Property)property, (T)value.value())));
        Set<BlockState> resolvedStates = states.collect(Collectors.toUnmodifiableSet());
        // Sanity check
        if(resolvedStates.size() != validStates)
            throw new AssertionError("Got two different numbers of valid states: " + validStates + " and " + resolvedStates.size() + "!");
        return resolvedStates;
    }
}

package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 17/06/2026 by SuperMartijn642
 */
public class BlockStateMatcher {

    public static BlockStateMatcher create(Block block, Pair<IProperty<?>,?>... properties){
        Map<IProperty<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<IProperty<?>,?> pair : properties){
            IProperty<?> property = pair.left();
            if(!block.getBlockState().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + ForgeRegistries.BLOCKS.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getAllowedValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "' in block '" + block + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<IProperty<?>,Set<?>>[] flattenedProperties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<IProperty<?>,List<Object>> entry : propertyMap.entrySet())
            properties[index++] = Pair.of(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        return new BlockStateMatcher(block, flattenedProperties);
    }

    public static BlockStateMatcher parseProperties(Block block, JsonObject propertiesJson){
        ResourceLocation identifier = ForgeRegistries.BLOCKS.getKey(block);
        List<Pair<IProperty<?>,Set<?>>> properties = new ArrayList<>();
        for(Map.Entry<String,JsonElement> entry : propertiesJson.entrySet()){
            // Parse the property
            IProperty<?> property = block.getBlockState().getProperty(entry.getKey());
            if(property == null)
                throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
            // Parse the values
            ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
            if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                Optional<?> value = property.parseValue(entry.getValue().getAsString()).toJavaUtil();
                if(!value.isPresent())
                    throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                builder.add(value.get());
            }else if(entry.getValue().isJsonArray()){
                if(entry.getValue().getAsJsonArray().size() == 0)
                    throw new JsonParseException("Valid values for property '" + property.getName() + "' cannot be empty!");
                for(JsonElement element : entry.getValue().getAsJsonArray()){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                    Optional<?> value = property.parseValue(element.getAsString()).toJavaUtil();
                    if(!value.isPresent())
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
                JsonArray array = new JsonArray();
                //noinspection rawtypes,unchecked
                values.stream().map(v -> ((IProperty)p.left()).getName((Comparable)v)).sorted().forEach(array::add);
                return array;
            }))
            .map(p -> p.mapLeft(IProperty::getName))
            .sorted(Comparator.comparing(Pair::left))
            .forEach(pair -> json.add(pair.left(), pair.right()));
        return json;
    }

    private final Block block;
    private final Pair<IProperty<?>,Set<?>>[] properties;
    private final boolean compareStates;
    private final Set<IBlockState> states;
    private Integer hashCode;

    private BlockStateMatcher(Block block, Pair<IProperty<?>,Set<?>>[] properties){
        this.block = block;
        this.properties = properties;
        this.states = computeStates(block, properties);
        this.compareStates = this.states != null;
    }

    public Block getBlock(){
        return this.block;
    }

    public Pair<IProperty<?>,Set<?>>[] getProperties(){
        return this.properties;
    }

    public boolean matches(IBlockState state){
        if(this.compareStates)
            return this.states.contains(state);
        if(state.getBlock() != this.block)
            return false;
        for(Pair<IProperty<?>,Set<?>> property : this.properties){
            if(!property.right().contains(state.getValue(property.left())))
                return false;
        }
        return true;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BlockStateMatcher)) return false;

        BlockStateMatcher that = (BlockStateMatcher)o;
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

    private static <T extends Comparable<T>> Set<IBlockState> computeStates(Block block, Pair<IProperty<?>,Set<?>>[] properties){
        // Compute the number of states matching this predicate
        Set<IProperty<?>> unrestrictedProperties = new HashSet<>(block.getBlockState().getProperties());
        int validStates = 1;
        for(Pair<IProperty<?>,Set<?>> pair : properties){
            validStates *= pair.right().size();
            unrestrictedProperties.remove(pair.left());
        }
        for(IProperty<?> property : unrestrictedProperties)
            validStates *= property.getAllowedValues().size();

        // If less than 64 states match, store and compare states directly
        if(validStates > 64)
            return null;
        Stream<IBlockState> states = Stream.of(block.getBlockState().getBaseState());
        for(Pair<IProperty<?>,Set<?>> pair : properties){
            IProperty<?> property = pair.left();
            Set<?> values = pair.right();
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> values.stream().map(value -> state.withProperty((IProperty)property, (T)value)));
        }
        for(IProperty<?> property : unrestrictedProperties)
            //noinspection rawtypes,unchecked
            states = states.flatMap(state -> property.getAllowedValues().stream().map(value -> state.withProperty((IProperty)property, (T)value)));
        Set<IBlockState> resolvedStates = states.collect(Collectors.toSet());
        // Sanity check
        if(resolvedStates.size() != validStates)
            throw new AssertionError("Got two different numbers of valid states: " + validStates + " and " + resolvedStates.size() + "!");
        return resolvedStates;
    }
}

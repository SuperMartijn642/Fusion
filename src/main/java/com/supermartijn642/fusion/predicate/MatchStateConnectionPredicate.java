package com.supermartijn642.fusion.predicate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 22/02/2024 by SuperMartijn642
 */
public class MatchStateConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<MatchStateConnectionPredicate> SERIALIZER = new Serializer<MatchStateConnectionPredicate>() {
        @Override
        public MatchStateConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match state predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
            if(!ForgeRegistries.BLOCKS.containsKey(identifier))
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            Block block = ForgeRegistries.BLOCKS.getValue(identifier);

            List<Pair<IProperty<?>,Set<?>>> properties = new ArrayList<>();
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match block predicate must have string property 'block'!");
            for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
                // Parse the property
                IProperty<?> property = block.getBlockState().getProperty(entry.getKey());
                if(property == null)
                    throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
                // Parse the values
                ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
                if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                    Optional<?> value = property.parseValue(entry.getValue().getAsString());
                    if(!value.isPresent())
                        throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }else if(entry.getValue().isJsonArray()){
                    for(JsonElement element : entry.getValue().getAsJsonArray()){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                        Optional<?> value = property.parseValue(element.getAsString());
                        if(!value.isPresent())
                            throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                        builder.add(value.get());
                    }
                }else
                    throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                properties.add(Pair.of(property, builder.build()));
            }
            //noinspection unchecked,DataFlowIssue
            properties = Arrays.asList((Pair<IProperty<?>,Set<?>>[])properties.toArray());

            return new MatchStateConnectionPredicate(block, properties);
        }

        @Override
        public JsonObject serialize(MatchStateConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", ForgeRegistries.BLOCKS.getKey(value.block).toString());
            JsonObject properties = new JsonObject();
            value.properties.stream()
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
    private final List<Pair<IProperty<?>,Set<?>>> properties;
    private boolean compareStates = false;
    private Set<IBlockState> states = null;

    public MatchStateConnectionPredicate(Block block, List<Pair<IProperty<?>,Set<?>>> properties){
        this.block = block;
        this.properties = properties;
        this.computeStates();
    }

    @SafeVarargs
    public MatchStateConnectionPredicate(Block block, Pair<IProperty<?>,?>... propertyPair){
        this.block = block;
        Map<IProperty<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<IProperty<?>,?> pair : propertyPair){
            IProperty<?> property = pair.left();
            if(!block.getBlockState().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + ForgeRegistries.BLOCKS.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getAllowedValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<IProperty<?>,Set<?>>[] properties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<IProperty<?>,List<Object>> entry : propertyMap.entrySet())
            properties[index++] = Pair.of(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
        this.properties = Arrays.asList(properties);
        this.computeStates();
    }

    private <T extends Comparable<T>> void computeStates(){
        // Compute the number of states matching this predicate
        int validStates = 1;
        for(Pair<IProperty<?>,Set<?>> pair : this.properties)
            validStates *= pair.right().size();

        // If less than 64 states match, store and compare states directly
        if(validStates > 64)
            return;
        Collection<IBlockState> states = Collections.singleton(this.block.getBlockState().getBaseState());
        for(Pair<IProperty<?>,Set<?>> pair : this.properties){
            IProperty<?> property = pair.left();
            Set<?> values = pair.right();
            //noinspection rawtypes,unchecked,RedundantCast
            states = (Collection<IBlockState>)states.stream() // (Collection<BlockState>) is needed or the compiler won't accept it
                .flatMap(state -> values.stream().map(value -> state.withProperty((IProperty)property, (T)value)))
                .collect(Collectors.toList());
        }
        this.compareStates = true;
        this.states = ImmutableSet.copyOf(states);
    }

    @Override
    public boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        if(this.compareStates)
            return this.states.contains(otherState);
        if(otherState.getBlock() != this.block)
            return false;
        for(Pair<IProperty<?>,Set<?>> property : this.properties){
            if(!property.right().contains(otherState.getValue(property.left())))
                return false;
        }
        return true;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

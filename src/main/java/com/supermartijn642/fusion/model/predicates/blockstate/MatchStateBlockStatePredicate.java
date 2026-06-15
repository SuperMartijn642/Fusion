package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.texture.types.connecting.predicates.MatchStateConnectionPredicate;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class MatchStateBlockStatePredicate implements BlockStateModelPredicate {

    private static final int MAX_OFFSET = MatchBlockBlockStatePredicate.MAX_OFFSET;

    public static BlockStateModelPredicate create(Block block, int x, int y, int z, Pair<Property<?>,?>... properties){
        Objects.requireNonNull(block);
        if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
            throw new IllegalArgumentException("Offset must be between -1 and 1 for each axis, not (" + x + ", " + y + ", " + z + ")!");
        Map<Property<?>,List<Object>> propertyMap = new HashMap<>();
        for(Pair<Property<?>,?> pair : properties){
            Property<?> property = pair.left();
            if(!block.getStateDefinition().getProperties().contains(property))
                throw new IllegalArgumentException("Property '" + property.getName() + "' is not a property of block '" + BuiltInRegistries.BLOCK.getKey(block) + "'!");
            Object value = pair.right();
            if(!property.getPossibleValues().contains(value))
                throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName() + "'!");
            propertyMap.computeIfAbsent(property, p -> new ArrayList<>()).add(value);
        }
        //noinspection unchecked
        Pair<Property<?>,Set<?>>[] flattenedProperties = new Pair[propertyMap.size()];
        int index = 0;
        for(Map.Entry<Property<?>,List<Object>> entry : propertyMap.entrySet())
            properties[index++] = Pair.of(entry.getKey(), Set.copyOf(entry.getValue()));
        return new MatchStateBlockStatePredicate(block, flattenedProperties, x, y, z);
    }

    public static BlockStateModelPredicate create(BlockState state, int x, int y, int z){
        if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
            throw new IllegalArgumentException("Offset must be between -1 and 1 for each axis, not (" + x + ", " + y + ", " + z + ")!");
        //noinspection unchecked
        return new MatchStateBlockStatePredicate(
            state.getBlock(),
            state.getProperties().stream().map(p -> Pair.of(p, Set.of(state.getValue(p)))).toArray(Pair[]::new),
            x, y, z
        );
    }

    public static final Serializer<MatchStateBlockStatePredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchStateBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match block predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
            Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(identifier);
            if(optional.isEmpty()){
                if(ignoreMissing)
                    //noinspection unchecked
                    return new MatchStateBlockStatePredicate(null, new Pair[0], 0, 0, 0);
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            }
            Block block = optional.get();

            List<Pair<Property<?>,Set<?>>> properties = new ArrayList<>();
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match block predicate must have object property 'properties'!");
            if(json.getAsJsonObject("properties").size() == 0)
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

            int x = 0, y = 0, z = 0;
            if(json.has("offset")){
                if(!json.get("offset").isJsonArray())
                    throw new JsonParseException("Property 'offset' must be an array of 3 numbers!");
                JsonArray offset = json.getAsJsonArray("offset");
                if(offset.size() != 3
                    || !offset.get(0).isJsonPrimitive() || !offset.get(0).getAsJsonPrimitive().isNumber()
                    || !offset.get(1).isJsonPrimitive() || !offset.get(1).getAsJsonPrimitive().isNumber()
                    || !offset.get(2).isJsonPrimitive() || !offset.get(2).getAsJsonPrimitive().isNumber())
                    throw new JsonParseException("Property 'offset' must be an array of 3 numbers!");
                x = offset.get(0).getAsInt();
                y = offset.get(1).getAsInt();
                z = offset.get(2).getAsInt();
                if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
                    throw new JsonParseException("Offset must be between " + -MAX_OFFSET + " and " + MAX_OFFSET + " for each axis, not (" + x + ", " + y + ", " + z + ")!");
            }
            //noinspection unchecked
            return new MatchStateBlockStatePredicate(block, properties.toArray(Pair[]::new), x, y, z);
        }

        @Override
        public JsonObject serialize(MatchStateBlockStatePredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(value.block).toString());
            if(value.x != 0 || value.y != 0 || value.z != 0){
                JsonArray offset = new JsonArray(3);
                offset.add(value.x);
                offset.add(value.y);
                offset.add(value.z);
                json.add("offset", offset);
            }
            JsonObject properties = new JsonObject();
            Arrays.stream(value.properties)
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
    private final Pair<Property<?>,Set<?>>[] properties;
    private final boolean compareStates;
    private final Set<BlockState> states;
    private final int x, y, z;
    private final boolean hasOffset;

    private MatchStateBlockStatePredicate(Block block, Pair<Property<?>,Set<?>>[] properties, int x, int y, int z){
        this.block = block;
        this.properties = properties;
        this.states = block == null ? null : MatchStateConnectionPredicate.computeStates(block, properties);
        this.compareStates = this.states != null;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasOffset = x == 0 || y == 0 || z == 0;
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(this.block == null)
            return false;
        if(this.hasOffset || state == null){
            if(level == null || pos == null)
                return false;
            if(this.hasOffset)
                pos = pos.offset(this.x, this.y, this.z);
            state = level.getBlockState(pos);
        }
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
    public BlockStateModelPredicate simplify(){
        return this.block == null ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof MatchStateBlockStatePredicate that)) return false;

        return this.x == that.x && this.y == that.y && this.z == that.z && Objects.equals(this.block, that.block) && Arrays.equals(this.properties, that.properties);
    }

    @Override
    public int hashCode(){
        int result = Objects.hashCode(this.block);
        result = 31 * result + Arrays.hashCode(this.properties);
        result = 31 * result + this.x;
        result = 31 * result + this.y;
        result = 31 * result + this.z;
        return result;
    }
}

package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.BlockStateMatcher;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class MatchStateBlockStatePredicate implements BlockStateModelPredicate {

    private static final int MAX_OFFSET = MatchBlockBlockStatePredicate.MAX_OFFSET;

    public static BlockStateModelPredicate create(Collection<Block> blocks, List<BlockPos> offsets, Pair<Property<?>,?>... properties){
        blocks = new HashSet<>(blocks);
        Map<Block,BlockStateMatcher> matchers = new HashMap<>(blocks.size());
        for(Block block : blocks)
            matchers.put(block, BlockStateMatcher.create(block, properties));
        for(BlockPos offset : offsets){
            if(offset.getX() < -MAX_OFFSET || offset.getX() > MAX_OFFSET
                || offset.getY() < -MAX_OFFSET || offset.getY() > MAX_OFFSET
                || offset.getZ() < -MAX_OFFSET || offset.getZ() > MAX_OFFSET)
                throw new JsonParseException("Offset must be between " + -MAX_OFFSET + " and " + MAX_OFFSET + " for each axis, not " + offset + "!");
        }
        return new MatchStateBlockStatePredicate(matchers, offsets);
    }

    public static BlockStateModelPredicate create(BlockState state, List<BlockPos> offsets){
        //noinspection unchecked
        Pair<Property<?>,?>[] properties = state.getProperties().stream().map(p -> Pair.of(p, state.getValue(p))).toArray(Pair[]::new);
        return create(List.of(state.getBlock()), offsets, properties);
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

            // Blocks
            if(!json.has("block") && !json.has("blocks"))
                throw new JsonParseException("Match state predicate must have either property 'block' or 'blocks'!");
            if(json.has("block") && json.has("blocks"))
                throw new JsonParseException("Match state predicate must have either property 'block' or 'blocks', not both!");
            List<Block> blocks;
            if(json.has("block")){
                if(!json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                    throw new JsonParseException("Property 'block' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                    throw new JsonParseException("Property 'block' must be a valid identifier, '" + json.get("block").getAsString() + "'!");
                ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
                Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(identifier);
                if(block.isEmpty()){
                    if(!ignoreMissing)
                        throw new JsonParseException("Unknown block '" + identifier + "'!");
                    blocks = List.of();
                }else
                    blocks = List.of(block.get());
            }else{
                if(!json.get("blocks").isJsonArray())
                    throw new JsonParseException("Property 'blocks' must be an array!");
                JsonArray array = json.getAsJsonArray("blocks");
                blocks = new ArrayList<>(array.size());
                for(JsonElement element : array){
                    try{
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Entry must be a strings!");
                        if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                            throw new JsonParseException("Entry must be a valid identifier, '" + element.getAsString() + "'!");
                        ResourceLocation identifier = new ResourceLocation(element.getAsString());
                        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(identifier);
                        if(block.isEmpty()){
                            if(!ignoreMissing)
                                throw new JsonParseException("Unknown block '" + identifier + "'!");
                            blocks = List.of();
                        }else
                            blocks = List.of(block.get());
                    }catch(JsonParseException e){
                        throw new JsonParseException("Failed to parse 'blocks' entry", e);
                    }
                }
            }

            // Parse properties
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match state predicate must have object property 'properties'!");
            if(json.getAsJsonObject("properties").size() == 0)
                throw new JsonParseException("At least one property must be specified for match state predicate!");
            JsonObject properties = json.getAsJsonObject("properties");
            Map<Block,BlockStateMatcher> matchers = new HashMap<>(blocks.size());
            for(Block block : blocks)
                matchers.put(block, BlockStateMatcher.parseProperties(block, properties));

            // Offsets
            if(json.has("offset") && json.has("offsets"))
                throw new JsonParseException("Match state predicate can have either property 'offset' or 'offsets', not both!");
            Set<BlockPos> offsets;
            if(json.has("offset")){
                if(!json.get("offset").isJsonArray())
                    throw new JsonParseException("Property 'offset' must be an array of 3 numbers!");
                JsonArray offset = json.getAsJsonArray("offset");
                if(offset.size() != 3
                    || !offset.get(0).isJsonPrimitive() || !offset.get(0).getAsJsonPrimitive().isNumber()
                    || !offset.get(1).isJsonPrimitive() || !offset.get(1).getAsJsonPrimitive().isNumber()
                    || !offset.get(2).isJsonPrimitive() || !offset.get(2).getAsJsonPrimitive().isNumber())
                    throw new JsonParseException("Property 'offset' must be an array of 3 numbers!");
                int x = offset.get(0).getAsInt();
                int y = offset.get(1).getAsInt();
                int z = offset.get(2).getAsInt();
                if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
                    throw new JsonParseException("Offset must be between " + -MAX_OFFSET + " and " + MAX_OFFSET + " for each axis, not (" + x + ", " + y + ", " + z + ")!");
                offsets = Set.of(new BlockPos(x, y, z));
            }else if(json.has("offsets")){
                if(!json.get("offsets").isJsonArray())
                    throw new JsonParseException("Property 'offsets' must be an array!");
                JsonArray array = json.getAsJsonArray("offsets");
                offsets = new HashSet<>(array.size());
                for(JsonElement element : array){
                    try{
                        if(!element.isJsonArray())
                            throw new JsonParseException("Entry must be an array of 3 numbers!");
                        JsonArray offset = json.getAsJsonArray("offset");
                        if(offset.size() != 3
                            || !offset.get(0).isJsonPrimitive() || !offset.get(0).getAsJsonPrimitive().isNumber()
                            || !offset.get(1).isJsonPrimitive() || !offset.get(1).getAsJsonPrimitive().isNumber()
                            || !offset.get(2).isJsonPrimitive() || !offset.get(2).getAsJsonPrimitive().isNumber())
                            throw new JsonParseException("Entry must be an array of 3 numbers!");
                        int x = offset.get(0).getAsInt();
                        int y = offset.get(1).getAsInt();
                        int z = offset.get(2).getAsInt();
                        if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
                            throw new JsonParseException("Offset must be between " + -MAX_OFFSET + " and " + MAX_OFFSET + " for each axis, not (" + x + ", " + y + ", " + z + ")!");
                        offsets.add(new BlockPos(x, y, z));
                    }catch(JsonParseException e){
                        throw new JsonParseException("Failed to parse 'offsets' entry", e);
                    }
                }
            }else
                offsets = Set.of(BlockPos.ZERO);
            return new MatchStateBlockStatePredicate(matchers, offsets);
        }

        @Override
        public JsonObject serialize(MatchStateBlockStatePredicate value){
            JsonObject json = new JsonObject();
            // Blocks
            if(value.blocks.size() == 1)
                json.addProperty("block", BuiltInRegistries.BLOCK.getKey(value.blocks.keySet().iterator().next()).toString());
            else{
                JsonArray blocks = new JsonArray(value.blocks.size());
                value.blocks.keySet().stream()
                    .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                    .sorted()
                    .forEach(blocks::add);
                json.add("blocks", blocks);
            }
            // Properties
            if(value.blocks.isEmpty())
                json.add("properties", new JsonArray());
            else
                json.add("properties", BlockStateMatcher.serializeProperties(value.blocks.values().iterator().next()));
            // Offsets
            if(!value.checkCenter || !value.offsets.isEmpty()){
                if(value.offsets.size() == 1 && !value.checkCenter){
                    BlockPos pos = value.offsets.iterator().next();
                    if(!pos.equals(BlockPos.ZERO)){
                        JsonArray offset = new JsonArray(3);
                        offset.add(pos.getX());
                        offset.add(pos.getY());
                        offset.add(pos.getZ());
                        json.add("offset", offset);
                    }
                }else{
                    JsonArray offsets = new JsonArray();
                    Stream.concat(
                            value.checkCenter ? Stream.of(BlockPos.ZERO) : Stream.empty(),
                            value.offsets.stream()
                        ).sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ))
                        .forEach(pos -> {
                            JsonArray offset = new JsonArray(3);
                            offset.add(pos.getX());
                            offset.add(pos.getY());
                            offset.add(pos.getZ());
                            offsets.add(offset);
                        });
                    json.add("offsets", offsets);
                }
            }
            return json;
        }
    };

    private final Map<Block,BlockStateMatcher> blocks;
    private final boolean containsAir;
    private final Set<BlockPos> offsets;
    private final boolean checkCenter;
    private final BlockPos.MutableBlockPos dummyBlockPos = new BlockPos.MutableBlockPos();

    private MatchStateBlockStatePredicate(Map<Block,BlockStateMatcher> blocks, Collection<BlockPos> offsets){
        this.blocks = Map.copyOf(blocks);
        this.containsAir = blocks.keySet().stream().anyMatch(b -> b.defaultBlockState().isAir());
        this.offsets = offsets.stream().filter(o -> !o.equals(BlockPos.ZERO)).collect(Collectors.toSet());
        this.checkCenter = offsets.stream().anyMatch(BlockPos.ZERO::equals);
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(this.checkCenter){
            checkCenter:
            {
                if(state == null){
                    if(level == null || pos == null){
                        if(this.containsAir)
                            return true;
                        break checkCenter;
                    }
                    state = level.getBlockState(pos);
                }
                if(state.isAir()){
                    if(this.containsAir)
                        return true;
                }else{
                    BlockStateMatcher matcher = this.blocks.get(state.getBlock());
                    if(matcher != null && matcher.matches(state))
                        return true;
                }
            }
        }

        if(this.offsets.isEmpty())
            return false;
        if(level == null || pos == null)
            return this.containsAir;
        for(BlockPos offset : this.offsets){
            this.dummyBlockPos.set(pos).move(offset);
            state = level.getBlockState(this.dummyBlockPos);
            if(state.isAir()){
                if(this.containsAir)
                    return true;
            }else{
                BlockStateMatcher matcher = this.blocks.get(state.getBlock());
                if(matcher != null && matcher.matches(state))
                    return true;
            }
        }
        return false;
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.blocks.isEmpty() || (this.offsets.isEmpty() && !this.checkCenter) ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof MatchStateBlockStatePredicate that)) return false;

        if(this.checkCenter != that.checkCenter) return false;
        if(!this.offsets.equals(that.offsets)) return false;
        if(this.blocks.isEmpty())
            return that.blocks.isEmpty();
        if(!this.blocks.keySet().equals(that.blocks.keySet())) return false;
        Block block = this.blocks.keySet().iterator().next();
        return this.blocks.get(block).equals(that.blocks.get(block));
    }

    @Override
    public int hashCode(){
        int result = this.blocks.hashCode();
        result = 31 * result + this.offsets.hashCode();
        result = 31 * result + Boolean.hashCode(this.checkCenter);
        return result;
    }
}

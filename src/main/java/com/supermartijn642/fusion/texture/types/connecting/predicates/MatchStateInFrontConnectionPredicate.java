package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.BlockStateMatcher;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 22/02/2024 by SuperMartijn642
 */
public class MatchStateInFrontConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(Collection<Block> blocks, Pair<Property<?>,?>... properties){
        blocks = new HashSet<>(blocks);
        Map<Block,BlockStateMatcher> matchers = new HashMap<>(blocks.size());
        for(Block block : blocks)
            matchers.put(block, BlockStateMatcher.create(block, properties));
        return new MatchStateInFrontConnectionPredicate(matchers);
    }

    public static ConnectionPredicate create(BlockState state){
        //noinspection unchecked
        Pair<Property<?>,?>[] properties = state.getProperties().stream().map(p -> Pair.of(p, state.getValue(p))).toArray(Pair[]::new);
        return create(List.of(state.getBlock()), properties);
    }

    public static final Serializer<MatchStateInFrontConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchStateInFrontConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            // Parse blocks
            if(!json.has("block") && !json.has("blocks"))
                throw new JsonParseException("Match state in front predicate must have either property 'block' or 'blocks'!");
            if(json.has("block") && json.has("blocks"))
                throw new JsonParseException("Match state in front predicate must have either property 'block' or 'blocks', not both!");
            List<Block> blocks;
            if(json.has("block")){
                if(!json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                    throw new JsonParseException("Property 'block' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                    throw new JsonParseException("Property 'block' must be a valid identifier, '" + json.get("block").getAsString() + "'!");
                ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
                Optional<Block> block = Registry.BLOCK.getOptional(identifier);
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
                        Optional<Block> block = Registry.BLOCK.getOptional(identifier);
                        if(block.isEmpty()){
                            if(!ignoreMissing)
                                throw new JsonParseException("Unknown block '" + identifier + "'!");
                        }else
                            blocks.add(block.get());
                    }catch(JsonParseException e){
                        throw new JsonParseException("Failed to parse 'blocks' entry", e);
                    }
                }
            }

            // Parse properties
            if(!json.has("properties") || !json.get("properties").isJsonObject())
                throw new JsonParseException("Match state in front predicate must have object property 'properties'!");
            if(json.getAsJsonObject("properties").size() == 0)
                throw new JsonParseException("At least one property must be specified for match state predicate!");
            JsonObject properties = json.getAsJsonObject("properties");
            Map<Block,BlockStateMatcher> matchers = new HashMap<>(blocks.size());
            for(Block block : blocks)
                matchers.put(block, BlockStateMatcher.parseProperties(block, properties));
            return new MatchStateInFrontConnectionPredicate(matchers);
        }

        @Override
        public JsonObject serialize(MatchStateInFrontConnectionPredicate value){
            JsonObject json = new JsonObject();
            if(value.blocks.size() == 1)
                json.addProperty("block", Registry.BLOCK.getKey(value.blocks.keySet().iterator().next()).toString());
            else{
                JsonArray blocks = new JsonArray(value.blocks.size());
                value.blocks.keySet().stream()
                    .map(b -> Registry.BLOCK.getKey(b).toString())
                    .sorted()
                    .forEach(blocks::add);
                json.add("blocks", blocks);
            }
            if(value.blocks.isEmpty())
                json.add("properties", new JsonArray());
            else
                json.add("properties", BlockStateMatcher.serializeProperties(value.blocks.values().iterator().next()));
            return json;
        }
    };

    private final Map<Block,BlockStateMatcher> blocks;
    private final boolean containsAir;

    private MatchStateInFrontConnectionPredicate(Map<Block,BlockStateMatcher> blocks){
        this.blocks = Map.copyOf(blocks);
        this.containsAir = blocks.keySet().stream().anyMatch(b -> b.defaultBlockState().isAir());
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        if(blockInFront.isAir())
            return this.containsAir;
        BlockStateMatcher matcher = this.blocks.get(blockInFront.getBlock());
        return matcher != null && matcher.matches(blockInFront);
    }

    @Override
    public ConnectionPredicate simplify(){
        return this.blocks.isEmpty() ? DefaultConnectionPredicates.never() : this;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof MatchStateInFrontConnectionPredicate that)) return false;

        if(this.blocks.isEmpty())
            return that.blocks.isEmpty();
        if(!this.blocks.keySet().equals(that.blocks.keySet())) return false;
        Block block = this.blocks.keySet().iterator().next();
        return this.blocks.get(block).equals(that.blocks.get(block));
    }

    @Override
    public int hashCode(){
        return this.blocks.hashCode();
    }
}

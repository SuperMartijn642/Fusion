package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class MatchBlockConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(Block... blocks){
        Objects.requireNonNull(blocks);
        return new MatchBlockConnectionPredicate(ImmutableList.copyOf(blocks));
    }

    public static final Serializer<MatchBlockConnectionPredicate> SERIALIZER = new Serializer<MatchBlockConnectionPredicate>() {
        @Override
        public MatchBlockConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("block") && !json.has("blocks"))
                throw new JsonParseException("Match block predicate must have either property 'block' or 'blocks'!");
            if(json.has("block") && json.has("blocks"))
                throw new JsonParseException("Match block predicate must have either property 'block' or 'blocks', not both!");
            List<Block> blocks;
            if(json.has("block")){
                if(!json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                    throw new JsonParseException("Property 'block' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                    throw new JsonParseException("Property 'block' must be a valid identifier, '" + json.get("block").getAsString() + "'!");
                ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
                if(!ForgeRegistries.BLOCKS.containsKey(identifier)){
                    if(!ignoreMissing)
                        throw new JsonParseException("Unknown block '" + identifier + "'!");
                    blocks = Collections.emptyList();
                }else
                    blocks = ImmutableList.of(ForgeRegistries.BLOCKS.getValue(identifier));
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
                        if(!ForgeRegistries.BLOCKS.containsKey(identifier)){
                            if(!ignoreMissing)
                                throw new JsonParseException("Unknown block '" + identifier + "'!");
                        }else
                            blocks.add(ForgeRegistries.BLOCKS.getValue(identifier));
                    }catch(JsonParseException e){
                        throw new JsonParseException("Failed to parse 'blocks' entry", e);
                    }
                }
            }
            return new MatchBlockConnectionPredicate(blocks);
        }

        @Override
        public JsonObject serialize(MatchBlockConnectionPredicate value){
            JsonObject json = new JsonObject();
            if(value.blocks.size() == 1)
                json.addProperty("block", ForgeRegistries.BLOCKS.getKey(value.blocks.iterator().next()).toString());
            else{
                JsonArray blocks = new JsonArray();
                value.blocks.stream()
                    .map(b -> ForgeRegistries.BLOCKS.getKey(b).toString())
                    .sorted()
                    .forEach(blocks::add);
                json.add("blocks", blocks);
            }
            return json;
        }
    };

    private final Set<Block> blocks;

    private MatchBlockConnectionPredicate(Collection<Block> blocks){
        this.blocks = ImmutableSet.copyOf(blocks);
    }

    @Override
    public boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return this.blocks.contains(otherState.getBlock());
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
        if(!(o instanceof MatchBlockConnectionPredicate)) return false;

        MatchBlockConnectionPredicate that = (MatchBlockConnectionPredicate)o;
        return this.blocks.equals(that.blocks);
    }

    @Override
    public int hashCode(){
        return this.blocks.hashCode();
    }
}

package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class MatchBlockConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(Block block){
        Objects.requireNonNull(block);
        return new MatchBlockConnectionPredicate(block);
    }

    public static final Serializer<MatchBlockConnectionPredicate> SERIALIZER = new Serializer<MatchBlockConnectionPredicate>() {
        @Override
        public MatchBlockConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match block predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
            if(!ForgeRegistries.BLOCKS.containsKey(identifier))
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            Block block = ForgeRegistries.BLOCKS.getValue(identifier);
            return new MatchBlockConnectionPredicate(block);
        }

        @Override
        public JsonObject serialize(MatchBlockConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", ForgeRegistries.BLOCKS.getKey(value.block).toString());
            return json;
        }
    };

    private final Block block;

    private MatchBlockConnectionPredicate(Block block){
        this.block = block;
    }

    @Override
    public boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return otherState.getBlock() == this.block;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof MatchBlockConnectionPredicate)) return false;

        MatchBlockConnectionPredicate that = (MatchBlockConnectionPredicate)o;
        return this.block.equals(that.block);
    }

    @Override
    public int hashCode(){
        return this.block.hashCode();
    }
}

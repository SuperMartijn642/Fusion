package com.supermartijn642.fusion.texture.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Created 12/10/2024 by SuperMartijn642
 */
public class MatchBlockInFrontConnectionPredicate implements ConnectionPredicate {

    public static ConnectionPredicate create(Block block){
        Objects.requireNonNull(block);
        return new MatchBlockInFrontConnectionPredicate(block);
    }

    public static final Serializer<MatchBlockInFrontConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchBlockInFrontConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match block predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
            Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(identifier);
            if(block.isEmpty())
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            return new MatchBlockInFrontConnectionPredicate(block.get());
        }

        @Override
        public JsonObject serialize(MatchBlockInFrontConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(value.block).toString());
            return json;
        }
    };

    private final Block block;

    private MatchBlockInFrontConnectionPredicate(Block block){
        this.block = block;
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return blockInFront.getBlock() == this.block;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof MatchBlockInFrontConnectionPredicate that)) return false;

        return this.block.equals(that.block);
    }

    @Override
    public int hashCode(){
        return this.block.hashCode();
    }
}

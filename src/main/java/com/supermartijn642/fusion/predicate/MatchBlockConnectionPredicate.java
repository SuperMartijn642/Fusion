package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionDirection;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class MatchBlockConnectionPredicate implements ConnectionPredicate {

    public static final Serializer<MatchBlockConnectionPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public MatchBlockConnectionPredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
                throw new JsonParseException("Match block predicate must have string property 'block'!");
            if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
                throw new JsonParseException("Property 'block' must be a valid identifier!");
            ResourceLocation identifier = ResourceLocation.parse(json.get("block").getAsString());
            if(!BuiltInRegistries.BLOCK.containsKey(identifier))
                throw new JsonParseException("Unknown block '" + identifier + "'!");
            Block block = BuiltInRegistries.BLOCK.get(identifier);
            return new MatchBlockConnectionPredicate(block);
        }

        @Override
        public JsonObject serialize(MatchBlockConnectionPredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", BuiltInRegistries.BLOCK.getKey(value.block).toString());
            return json;
        }
    };

    private final Block block;

    public MatchBlockConnectionPredicate(Block block){
        this.block = block;
    }

    @Override
    public boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        return otherState.getBlock() == this.block;
    }

    @Override
    public Serializer<? extends ConnectionPredicate> getSerializer(){
        return SERIALIZER;
    }
}

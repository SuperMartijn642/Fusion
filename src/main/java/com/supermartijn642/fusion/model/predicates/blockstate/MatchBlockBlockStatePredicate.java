package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IBlockDisplayReader;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class MatchBlockBlockStatePredicate implements BlockStateModelPredicate {

    static final int MAX_OFFSET = 1; // More than 1 and the model might not get rebuild if the offset block changes

    public static BlockStateModelPredicate create(Block block, int x, int y, int z){
        Objects.requireNonNull(block);
        if(x < -MAX_OFFSET || x > MAX_OFFSET || y < -MAX_OFFSET || y > MAX_OFFSET || z < -MAX_OFFSET || z > MAX_OFFSET)
            throw new IllegalArgumentException("Offset must be between -1 and 1 for each axis, not (" + x + ", " + y + ", " + z + ")!");
        return new MatchBlockBlockStatePredicate(block, x, y, z);
    }

    public static final Serializer<MatchBlockBlockStatePredicate> SERIALIZER = new Serializer<MatchBlockBlockStatePredicate>() {
        @Override
        public MatchBlockBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
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
            Optional<Block> block = Registry.BLOCK.getOptional(identifier);
            if(!block.isPresent()){
                if(ignoreMissing)
                    return new MatchBlockBlockStatePredicate(null, 0, 0, 0);
                throw new JsonParseException("Unknown block '" + identifier + "'!");
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
            return new MatchBlockBlockStatePredicate(block.get(), x, y, z);
        }

        @Override
        public JsonObject serialize(MatchBlockBlockStatePredicate value){
            JsonObject json = new JsonObject();
            json.addProperty("block", Registry.BLOCK.getKey(value.block).toString());
            if(value.x != 0 || value.y != 0 || value.z != 0){
                JsonArray offset = new JsonArray();
                offset.add(value.x);
                offset.add(value.y);
                offset.add(value.z);
                json.add("offset", offset);
            }
            return json;
        }
    };

    private final Block block;
    private final boolean isAir;
    private final int x, y, z;
    private final boolean hasOffset;

    private MatchBlockBlockStatePredicate(Block block, int x, int y, int z){
        this.block = block;
        this.isAir = block != null && block.defaultBlockState().isAir();
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasOffset = x == 0 || y == 0 || z == 0;
    }

    @Override
    public boolean test(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(this.block == null)
            return false;
        if(this.hasOffset || state == null){
            if(level == null || pos == null)
                return this.isAir;
            if(this.hasOffset)
                pos = pos.offset(this.x, this.y, this.z);
            state = level.getBlockState(pos);
        }
        return state.getBlock() == this.block;
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
        if(!(o instanceof MatchBlockBlockStatePredicate)) return false;

        MatchBlockBlockStatePredicate that = (MatchBlockBlockStatePredicate)o;
        return this.x == that.x && this.y == that.y && this.z == that.z && Objects.equals(this.block, that.block);
    }

    @Override
    public int hashCode(){
        int result = Objects.hashCode(this.block);
        result = 31 * result + this.x;
        result = 31 * result + this.y;
        result = 31 * result + this.z;
        return result;
    }
}

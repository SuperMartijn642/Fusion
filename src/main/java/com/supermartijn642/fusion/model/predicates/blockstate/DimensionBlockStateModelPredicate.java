package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(int... dimensions){
        for(int dimension : dimensions){
            if(!DimensionManager.isDimensionRegistered(dimension))
                throw new JsonParseException("No dimension for id '" + dimension + "'!");
        }
        return new DimensionBlockStateModelPredicate(dimensions);
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<DimensionBlockStateModelPredicate>() {
        @Override
        public DimensionBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("dimension") && !json.has("dimensions"))
                throw new JsonParseException("Dimension predicate must have either property 'dimension' or 'dimensions'!");
            if(json.has("dimension") && json.has("dimensions"))
                throw new JsonParseException("Dimension predicate must have either property 'dimension' or 'dimensions', not both!");
            Set<Integer> dimensions;
            if(json.has("dimension")){
                if(!json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isNumber())
                    throw new JsonParseException("Property 'dimension' must be an integer!");
                int dimension = json.get("dimension").getAsInt();
                if(!DimensionManager.isDimensionRegistered(dimension)){
                    if(!ignoreMissing)
                        throw new JsonParseException("No dimension for id '" + dimension + "'!");
                    dimensions = Collections.emptySet();
                }else
                    dimensions = ImmutableSet.of(dimension);
            }else{
                if(!json.get("dimensions").isJsonArray())
                    throw new JsonParseException("Property 'dimensions' must be an array!");
                JsonArray array = json.getAsJsonArray("dimensions");
                dimensions = new HashSet<>(array.size());
                for(JsonElement element : array){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Array property 'dimensions' must only contain strings!");
                    int dimension = element.getAsInt();
                    if(!DimensionManager.isDimensionRegistered(dimension)){
                        if(!ignoreMissing)
                            throw new JsonParseException("No dimension for id '" + dimension + "'!");
                    }else
                        dimensions.add(dimension);
                }
            }
            return new DimensionBlockStateModelPredicate(dimensions.stream().mapToInt(Integer::intValue).toArray());
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.dimensions.size() == 1)
                json.addProperty("dimension", value.dimensions.iterator().next().toString());
            else{
                JsonArray biomes = new JsonArray();
                value.dimensions.stream()
                    .sorted()
                    .forEach(biomes::add);
                json.add("dimensions", biomes);
            }
            return json;
        }
    };

    private final IntSet dimensions;

    private DimensionBlockStateModelPredicate(int[] dimensions){
        this.dimensions = new IntArraySet(dimensions);
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        return level instanceof World && this.dimensions.contains(((World)level).provider.getDimension());
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.dimensions.isEmpty() ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof DimensionBlockStateModelPredicate)) return false;

        DimensionBlockStateModelPredicate that = (DimensionBlockStateModelPredicate)o;
        return this.dimensions.equals(that.dimensions);
    }

    @Override
    public int hashCode(){
        return this.dimensions.hashCode();
    }
}

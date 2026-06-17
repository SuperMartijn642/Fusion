package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceLocation... dimensions){
        return new DimensionBlockStateModelPredicate(Arrays.asList(dimensions));
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<DimensionBlockStateModelPredicate>() {
        @Override
        public DimensionBlockStateModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                // Ignore on this version
            }

            if(!json.has("dimension") && !json.has("dimensions"))
                throw new JsonParseException("Dimension predicate must have either property 'dimension' or 'dimensions'!");
            if(json.has("dimension") && json.has("dimensions"))
                throw new JsonParseException("Dimension predicate must have either property 'dimension' or 'dimensions', not both!");
            Set<ResourceLocation> dimensions;
            if(json.has("dimensions")){
                if(!json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                    throw new JsonParseException("Property 'dimension' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                    throw new JsonParseException("Property 'dimension' must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
                dimensions = ImmutableSet.of(new ResourceLocation(json.get("dimension").getAsString()));
            }else{
                if(!json.get("dimensions").isJsonArray())
                    throw new JsonParseException("Property 'dimensions' must be an array!");
                JsonArray array = json.getAsJsonArray("dimensions");
                dimensions = new HashSet<>(array.size());
                for(JsonElement element : array){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Array property 'dimensions' must only contain strings!");
                    if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                        throw new JsonParseException("Dimension entries must be a valid identifier, not '" + element.getAsString() + "'!");
                    dimensions.add(new ResourceLocation(element.getAsString()));
                }
            }
            return new DimensionBlockStateModelPredicate(dimensions);
        }

        @Override
        public JsonObject serialize(DimensionBlockStateModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.dimensions.size() == 1)
                json.addProperty("dimension", value.dimensions.iterator().next().toString());
            else{
                JsonArray biomes = new JsonArray();
                value.dimensions.stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .forEach(biomes::add);
                json.add("dimensions", biomes);
            }
            return json;
        }
    };

    private final Set<ResourceLocation> dimensions;

    private DimensionBlockStateModelPredicate(Collection<ResourceLocation> dimensions){
        this.dimensions = ImmutableSet.copyOf(dimensions);
    }

    @Override
    public boolean test(@Nullable IEnviromentBlockReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        return level instanceof World && this.dimensions.contains(((World)level).getDimension().getType().getRegistryName());
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

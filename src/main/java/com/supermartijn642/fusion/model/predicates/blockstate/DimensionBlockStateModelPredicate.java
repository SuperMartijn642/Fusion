package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class DimensionBlockStateModelPredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceKey<Level>... dimensions){
        return new DimensionBlockStateModelPredicate(Arrays.stream(dimensions).map(ResourceKey::identifier).collect(Collectors.toSet()));
    }

    public static BlockStateModelPredicate create(Identifier... dimensions){
        return new DimensionBlockStateModelPredicate(Arrays.asList(dimensions));
    }

    public static final Serializer<DimensionBlockStateModelPredicate> SERIALIZER = new Serializer<>() {
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
            Set<Identifier> dimensions;
            if(json.has("dimension")){
                if(!json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                    throw new JsonParseException("Property 'dimension' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                    throw new JsonParseException("Property 'dimension' must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
                dimensions = Set.of(Identifier.parse(json.get("dimension").getAsString()));
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
                    dimensions.add(Identifier.parse(element.getAsString()));
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
                JsonArray biomes = new JsonArray(value.dimensions.size());
                value.dimensions.stream()
                    .map(Identifier::toString)
                    .sorted()
                    .forEach(biomes::add);
                json.add("dimensions", biomes);
            }
            return json;
        }
    };

    private final Set<Identifier> dimensions;

    private DimensionBlockStateModelPredicate(Collection<Identifier> dimensions){
        this.dimensions = Set.copyOf(dimensions);
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        return level instanceof Level && this.dimensions.contains(((Level)level).dimension().identifier());
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
        if(!(o instanceof DimensionBlockStateModelPredicate that)) return false;

        return this.dimensions.equals(that.dimensions);
    }

    @Override
    public int hashCode(){
        return this.dimensions.hashCode();
    }
}

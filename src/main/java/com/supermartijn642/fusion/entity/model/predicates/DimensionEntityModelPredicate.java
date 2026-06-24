package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class DimensionEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(ResourceKey<Level>... dimensions){
        return new DimensionEntityModelPredicate(Arrays.stream(dimensions).map(ResourceKey::location).collect(Collectors.toSet()));
    }

    public static EntityModelPredicate create(ResourceLocation... dimensions){
        return new DimensionEntityModelPredicate(Arrays.asList(dimensions));
    }

    public static final Serializer<DimensionEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public DimensionEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
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
            if(json.has("dimension")){
                if(!json.get("dimension").isJsonPrimitive() || !json.getAsJsonPrimitive("dimension").isString())
                    throw new JsonParseException("Property 'dimension' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("dimension").getAsString()))
                    throw new JsonParseException("Property 'dimension' must be a valid identifier, not '" + json.get("dimension").getAsString() + "'!");
                dimensions = Set.of(new ResourceLocation(json.get("dimension").getAsString()));
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
            return new DimensionEntityModelPredicate(dimensions);
        }

        @Override
        public JsonObject serialize(DimensionEntityModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.dimensions.size() == 1)
                json.addProperty("dimension", value.dimensions.iterator().next().toString());
            else{
                JsonArray biomes = new JsonArray(value.dimensions.size());
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

    private DimensionEntityModelPredicate(Collection<ResourceLocation> dimensions){
        this.dimensions = Set.copyOf(dimensions);
    }

    @Override
    public boolean test(Entity entity){
        Level level = entity.level();
        return level != null && this.dimensions.contains(level.dimension().location());
    }

    @Override
    public EntityModelPredicate simplify(){
        return this.dimensions.isEmpty() ? DefaultEntityModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof DimensionEntityModelPredicate that)) return false;

        return this.dimensions.equals(that.dimensions);
    }

    @Override
    public int hashCode(){
        return this.dimensions.hashCode();
    }
}

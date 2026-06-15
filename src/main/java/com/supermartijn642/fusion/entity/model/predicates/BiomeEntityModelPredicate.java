package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class BiomeEntityModelPredicate implements EntityModelPredicate {

    public static EntityModelPredicate create(ResourceKey<Biome>... biomes){
        return create(Arrays.stream(biomes).map(ResourceKey::location).toArray(ResourceLocation[]::new));
    }

    public static EntityModelPredicate create(ResourceLocation... biomes){
        return new BiomeEntityModelPredicate(Set.of(biomes));
    }

    public static final Serializer<BiomeEntityModelPredicate> SERIALIZER = new Serializer<>() {
        @Override
        public BiomeEntityModelPredicate deserialize(JsonObject json) throws JsonParseException{
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                // Ignore on this version
            }

            if(!json.has("biomes") || !json.get("biomes").isJsonArray())
                throw new JsonParseException("Biome-predicate must have array property 'biomes'!");
            Set<ResourceLocation> biomes = new HashSet<>();
            for(JsonElement element : json.getAsJsonArray("biomes")){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'biomes' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Biome entries must be a valid identifier, not '" + element.getAsString() + "'!");
                biomes.add(ResourceLocation.parse(element.getAsString()));
            }
            return new BiomeEntityModelPredicate(biomes);
        }

        @Override
        public JsonObject serialize(BiomeEntityModelPredicate value){
            JsonObject json = new JsonObject();
            JsonArray biomes = new JsonArray(value.biomes.size());
            value.biomes.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(biomes::add);
            json.add("biomes", biomes);
            return json;
        }
    };

    private final Set<ResourceLocation> biomes;

    private BiomeEntityModelPredicate(Set<ResourceLocation> biomes){
        this.biomes = Set.copyOf(biomes);
    }

    @Override
    public boolean test(Entity entity){
        Level level = entity.level();
        if(level == null)
            return false;
        Holder<Biome> biome = level.getBiome(entity.blockPosition());
        //noinspection OptionalGetWithoutIsPresent
        return biome != null && biome.isBound() && this.biomes.contains(biome.unwrapKey().get().location());
    }

    @Override
    public EntityModelPredicate simplify(){
        return this.biomes.isEmpty() ? DefaultEntityModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

package com.supermartijn642.fusion.entity.model.predicates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
        return create(Arrays.stream(biomes).map(ResourceKey::identifier).toArray(Identifier[]::new));
    }

    public static EntityModelPredicate create(Identifier... biomes){
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

            if(!json.has("biome") && !json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes'!");
            if(json.has("biome") && json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes', not both!");
            Set<Identifier> biomes;
            if(json.has("biome")){
                if(!json.get("biome").isJsonPrimitive() || !json.getAsJsonPrimitive("biome").isString())
                    throw new JsonParseException("Property 'biome' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("biome").getAsString()))
                    throw new JsonParseException("Property 'biome' must be a valid identifier, not '" + json.get("biome").getAsString() + "'!");
                biomes = Set.of(Identifier.parse(json.get("biome").getAsString()));
            }else{
                if(!json.get("biomes").isJsonArray())
                    throw new JsonParseException("Property 'biomes' must be an array!");
                JsonArray array = json.getAsJsonArray("biomes");
                biomes = new HashSet<>(array.size());
                for(JsonElement element : array){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Array property 'biomes' must only contain strings!");
                    if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                        throw new JsonParseException("Biome entries must be a valid identifier, not '" + element.getAsString() + "'!");
                    biomes.add(Identifier.parse(element.getAsString()));
                }
            }
            return new BiomeEntityModelPredicate(biomes);
        }

        @Override
        public JsonObject serialize(BiomeEntityModelPredicate value){
            JsonObject json = new JsonObject();
            if(value.biomes.size() == 1)
                json.addProperty("biome", value.biomes.iterator().next().toString());
            else{
                JsonArray biomes = new JsonArray(value.biomes.size());
                value.biomes.stream()
                    .map(Identifier::toString)
                    .sorted()
                    .forEach(biomes::add);
                json.add("biomes", biomes);
            }
            return json;
        }
    };

    private final Set<Identifier> biomes;

    private BiomeEntityModelPredicate(Set<Identifier> biomes){
        this.biomes = Set.copyOf(biomes);
    }

    @Override
    public boolean test(Entity entity){
        Level level = entity.level();
        if(level == null)
            return false;
        Holder<Biome> biome = level.getBiome(entity.blockPosition());
        //noinspection OptionalGetWithoutIsPresent
        return biome != null && biome.isBound() && this.biomes.contains(biome.unwrapKey().get().identifier());
    }

    @Override
    public EntityModelPredicate simplify(){
        return this.biomes.isEmpty() ? DefaultEntityModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends EntityModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BiomeEntityModelPredicate that)) return false;

        return this.biomes.equals(that.biomes);
    }

    @Override
    public int hashCode(){
        return this.biomes.hashCode();
    }
}

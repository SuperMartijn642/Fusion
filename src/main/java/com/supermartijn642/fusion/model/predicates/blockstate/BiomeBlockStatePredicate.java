package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class BiomeBlockStatePredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceKey<Biome>... biomes){
        return create(Arrays.stream(biomes).map(ResourceKey::location).toArray(ResourceLocation[]::new));
    }

    public static BlockStateModelPredicate create(ResourceLocation... biomes){
        return new BiomeBlockStatePredicate(Set.of(biomes));
    }

    public static final Serializer<BiomeBlockStatePredicate> SERIALIZER = new Serializer<>() {
        @Override
        public BiomeBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                // Ignore on this version
            }

            if(!json.has("biome") && !json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes'!");
            if(json.has("biome") && json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes', not both!");
            Set<ResourceLocation> biomes;
            if(json.has("biome")){
                if(!json.get("biome").isJsonPrimitive() || !json.getAsJsonPrimitive("biome").isString())
                    throw new JsonParseException("Property 'biome' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("biome").getAsString()))
                    throw new JsonParseException("Property 'biome' must be a valid identifier, not '" + json.get("biome").getAsString() + "'!");
                biomes = Set.of(new ResourceLocation(json.get("biome").getAsString()));
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
                    biomes.add(new ResourceLocation(element.getAsString()));
                }
            }
            return new BiomeBlockStatePredicate(biomes);
        }

        @Override
        public JsonObject serialize(BiomeBlockStatePredicate value){
            JsonObject json = new JsonObject();
            if(value.biomes.size() == 1)
                json.addProperty("biome", value.biomes.iterator().next().toString());
            else{
                JsonArray biomes = new JsonArray();
                value.biomes.stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .forEach(biomes::add);
                json.add("biomes", biomes);
            }
            return json;
        }
    };

    private final Set<ResourceLocation> biomes;

    private BiomeBlockStatePredicate(Set<ResourceLocation> biomes){
        this.biomes = Set.copyOf(biomes);
    }

    @Override
    public boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(pos == null || !(level instanceof LevelReader))
            return false;
        Biome biome = ((LevelReader)level).getBiome(pos);
        return this.biomes.contains(BuiltinRegistries.BIOME.getKey(biome));
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.biomes.isEmpty() ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BiomeBlockStatePredicate that)) return false;

        return this.biomes.equals(that.biomes);
    }

    @Override
    public int hashCode(){
        return this.biomes.hashCode();
    }
}

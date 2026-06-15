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
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ILightReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class BiomeBlockStatePredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceLocation... biomes){
        Set<Biome> set = new HashSet<>();
        for(ResourceLocation biome : biomes){
            Optional<Biome> optional = Registry.BIOME.getOptional(biome);
            if(!optional.isPresent())
                throw new IllegalArgumentException("Unknown biome '" + biome + "'!");
            set.add(optional.get());
        }
        return new BiomeBlockStatePredicate(set);
    }

    public static BlockStateModelPredicate create(Biome... biomes){
        return new BiomeBlockStatePredicate(ImmutableSet.copyOf(biomes));
    }

    public static final Serializer<BiomeBlockStatePredicate> SERIALIZER = new Serializer<BiomeBlockStatePredicate>() {
        @Override
        public BiomeBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                // Ignore on this version
            }

            if(!json.has("biomes") || !json.get("biomes").isJsonArray())
                throw new JsonParseException("Biome-predicate must have array property 'biomes'!");
            Set<Biome> biomes = new HashSet<>();
            for(JsonElement element : json.getAsJsonArray("biomes")){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'biomes' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Biome entries must be a valid identifier, not '" + element.getAsString() + "'!");
                Optional<Biome> biome = Registry.BIOME.getOptional(new ResourceLocation(element.getAsString()));
                if(!biome.isPresent())
                    throw new JsonParseException("Unknown biome '" + element.getAsString() + "'!");
                biomes.add(biome.get());
            }
            return new BiomeBlockStatePredicate(biomes);
        }

        @Override
        public JsonObject serialize(BiomeBlockStatePredicate value){
            JsonObject json = new JsonObject();
            JsonArray biomes = new JsonArray();
            value.biomes.stream()
                .map(Registry.BIOME::getKey)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(biomes::add);
            json.add("biomes", biomes);
            return json;
        }
    };

    private final Set<Biome> biomes;

    private BiomeBlockStatePredicate(Set<Biome> biomes){
        this.biomes = ImmutableSet.copyOf(biomes);
    }

    @Override
    public boolean test(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(pos == null || !(level instanceof IWorldReader))
            return false;
        Biome biome = ((IWorldReader)level).getBiome(pos);
        return this.biomes.contains(biome);
    }

    @Override
    public BlockStateModelPredicate simplify(){
        return this.biomes.isEmpty() ? DefaultBlockStateModelPredicates.never() : this;
    }

    @Override
    public Serializer<? extends BlockStateModelPredicate> getSerializer(){
        return SERIALIZER;
    }
}

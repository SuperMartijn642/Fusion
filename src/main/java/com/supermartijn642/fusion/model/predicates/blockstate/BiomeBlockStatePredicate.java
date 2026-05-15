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
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class BiomeBlockStatePredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(RegistryKey<Biome>... biomes){
        return create(Arrays.stream(biomes).map(RegistryKey::location).toArray(ResourceLocation[]::new));
    }

    public static BlockStateModelPredicate create(ResourceLocation... biomes){
        return new BiomeBlockStatePredicate(ImmutableSet.copyOf(biomes));
    }

    public static final Serializer<BiomeBlockStatePredicate> SERIALIZER = new Serializer<BiomeBlockStatePredicate>() {
        @Override
        public BiomeBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
            if(!json.has("biomes") || !json.get("biomes").isJsonArray())
                throw new JsonParseException("Biome-predicate must have array property 'biomes'!");
            Set<ResourceLocation> biomes = new HashSet<>();
            for(JsonElement element : json.getAsJsonArray("biomes")){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'biomes' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Biome entries must be a valid identifier, not '" + element.getAsString() + "'!");
                biomes.add(new ResourceLocation(element.getAsString()));
            }
            return new BiomeBlockStatePredicate(biomes);
        }

        @Override
        public JsonObject serialize(BiomeBlockStatePredicate value){
            JsonObject json = new JsonObject();
            JsonArray biomes = new JsonArray();
            value.biomes.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(biomes::add);
            json.add("biomes", biomes);
            return json;
        }
    };

    private final Set<ResourceLocation> biomes;

    private BiomeBlockStatePredicate(Set<ResourceLocation> biomes){
        this.biomes = ImmutableSet.copyOf(biomes);
    }

    @Override
    public boolean test(@Nullable IBlockDisplayReader level, @Nullable BlockPos pos, @Nullable BlockState state){
        if(pos == null || !(level instanceof IWorldReader))
            return false;
        Biome biome = ((IWorldReader)level).getBiome(pos);
        return this.biomes.contains(WorldGenRegistries.BIOME.getKey(biome));
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

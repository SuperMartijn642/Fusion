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
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 15/05/2026 by SuperMartijn642
 */
public class BiomeBlockStatePredicate implements BlockStateModelPredicate {

    public static BlockStateModelPredicate create(ResourceLocation... biomes){
        Set<Biome> set = new HashSet<>();
        for(ResourceLocation biome : biomes){
            if(!ForgeRegistries.BIOMES.containsKey(biome))
                throw new IllegalArgumentException("Unknown biome '" + biome + "'!");
            set.add(ForgeRegistries.BIOMES.getValue(biome));
        }
        return new BiomeBlockStatePredicate(set);
    }

    public static BlockStateModelPredicate create(Biome... biomes){
        return new BiomeBlockStatePredicate(ImmutableSet.copyOf(biomes));
    }

    public static final Serializer<BiomeBlockStatePredicate> SERIALIZER = new Serializer<BiomeBlockStatePredicate>() {
        @Override
        public BiomeBlockStatePredicate deserialize(JsonObject json) throws JsonParseException{
            boolean ignoreMissing = false;
            if(json.has("ignore_missing")){
                if(!json.get("ignore_missing").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing").isBoolean())
                    throw new JsonParseException("Property 'ignore_missing' must be a boolean!");
                ignoreMissing = json.get("ignore_missing").getAsBoolean();
            }

            if(!json.has("biome") && !json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes'!");
            if(json.has("biome") && json.has("biomes"))
                throw new JsonParseException("Biome predicate must have either property 'biome' or 'biomes', not both!");
            Set<Biome> biomes;
            if(json.has("biome")){
                if(!json.get("biome").isJsonPrimitive() || !json.getAsJsonPrimitive("biome").isString())
                    throw new JsonParseException("Property 'biome' must be a string!");
                if(!IdentifierUtil.isValidIdentifier(json.get("biome").getAsString()))
                    throw new JsonParseException("Property 'biome' must be a valid identifier, not '" + json.get("biome").getAsString() + "'!");
                ResourceLocation identifier = new ResourceLocation(json.get("biome").getAsString());
                if(!ForgeRegistries.BLOCKS.containsKey(identifier)){
                    if(!ignoreMissing)
                        throw new JsonParseException("Unknown biome '" + json.get("biome").getAsString() + "'!");
                    biomes = Collections.emptySet();
                }else
                    biomes = ImmutableSet.of(ForgeRegistries.BIOMES.getValue(identifier));
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
                    ResourceLocation identifier = new ResourceLocation(element.getAsString());
                    if(!ForgeRegistries.BLOCKS.containsKey(identifier)){
                        if(!ignoreMissing)
                            throw new JsonParseException("Unknown biome '" + json.get("biome").getAsString() + "'!");
                    }else
                        biomes = ImmutableSet.of(ForgeRegistries.BIOMES.getValue(identifier));
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
                    .map(ForgeRegistries.BIOMES::getKey)
                    .map(ResourceLocation::toString)
                    .sorted()
                    .forEach(biomes::add);
                json.add("biomes", biomes);
            }
            return json;
        }
    };

    private final Set<Biome> biomes;

    private BiomeBlockStatePredicate(Set<Biome> biomes){
        this.biomes = ImmutableSet.copyOf(biomes);
    }

    @Override
    public boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state){
        if(level == null || pos == null)
            return false;
        Biome biome = level.getBiome(pos);
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

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof BiomeBlockStatePredicate)) return false;

        BiomeBlockStatePredicate that = (BiomeBlockStatePredicate)o;
        return this.biomes.equals(that.biomes);
    }

    @Override
    public int hashCode(){
        return this.biomes.hashCode();
    }
}

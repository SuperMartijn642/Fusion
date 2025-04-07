package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.core.generator.ResourceType;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Allows generating block model modifier files.
 * Users must extend the class and overwrite {@link FusionBlockModelModifierProvider#generate()}.
 * Users may use {@link FusionBlockModelModifierProvider#modifier(ResourceLocation)} to obtain a builder for the given location.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public abstract class FusionBlockModelModifierProvider extends ResourceGenerator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Map<ResourceLocation,ModifierBuilder> modifiers = new HashMap<>();

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionBlockModelModifierProvider(String modid, ResourceCache cache){
        super(modid, cache);
    }

    @Override
    public final void save(){
        for(Map.Entry<ResourceLocation,ModifierBuilder> entry : this.modifiers.entrySet()){
            ResourceLocation location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getResourcePath().endsWith(".json") ? "" : ".json";
            this.cache.saveResource(ResourceType.ASSET, GSON.toJson(json).getBytes(StandardCharsets.UTF_8), location.getResourceDomain(), "fusion/model_modifiers/blocks", location.getResourcePath(), extension);
        }
    }

    private JsonObject toJson(ModifierBuilder modifier){
        JsonObject json = new JsonObject();
        // Targets
        if(modifier.targets.isEmpty())
            throw new IllegalArgumentException("Modifier '" + modifier.location + "' must have at least one target!");
        JsonArray targets = new JsonArray();
        modifier.targets.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                ResourceLocation block = entry.getKey();
                Map<IProperty<?>,Set<Object>> properties = entry.getValue();
                if(properties.isEmpty()){
                    targets.add(block.toString());
                    return;
                }
                JsonObject object = new JsonObject();
                object.addProperty("block", block.toString());
                JsonObject propertiesJson = new JsonObject();
                properties.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getName()))
                    .forEach(e -> {
                        //noinspection rawtypes
                        IProperty property = e.getKey();
                        JsonArray values = new JsonArray();
                        //noinspection unchecked,rawtypes
                        e.getValue().stream()
                            .map(v -> property.getName((Comparable)v))
                            .sorted()
                            .forEach(values::add);
                        propertiesJson.add(property.getName(), values);
                    });
                object.add("properties", propertiesJson);
                targets.add(object);
            });
        json.add("targets", targets);
        // Append models
        if(!modifier.appendModels.isEmpty() || !modifier.paneCullingFix){
            JsonArray appendModels = new JsonArray();
            modifier.appendModels.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(appendModels::add);
            json.add("append", appendModels);
        }
        // Pane culling fix
        if(modifier.paneCullingFix)
            json.addProperty("pane_culling_fix", true);
        return json;
    }

    /**
     * Configures block model modifiers which should be generated through {@link #modifier(ResourceLocation)}.
     */
    public abstract void generate();

    /**
     * Creates or gets the block model modifier builder for the given location.
     */
    public final ModifierBuilder modifier(ResourceLocation location){
        return this.modifiers.computeIfAbsent(location, ModifierBuilder::new);
    }

    @Override
    public String getName(){
        return "Fusion Block Model Modifier Provider: " + this.modName;
    }

    public static final class ModifierBuilder {
        private final ResourceLocation location;
        private final Map<ResourceLocation,Map<IProperty<?>,Set<Object>>> targets = new HashMap<>();
        private final Set<ResourceLocation> appendModels = new HashSet<>();
        private boolean paneCullingFix = false;

        private ModifierBuilder(ResourceLocation location){
            this.location = location;
        }

        /**
         * Adds the given block identifier to the targets for this modifier.
         */
        public ModifierBuilder target(ResourceLocation block){
            if(!this.targets.containsKey(block))
                this.targets.put(block, new HashMap<>());
            return this;
        }

        /**
         * Adds the given block to the targets for this modifier.
         */
        public ModifierBuilder target(Block block){
            return this.target(ForgeRegistries.BLOCKS.getKey(block));
        }

        /**
         * Adds the given block with the given properties to the targets for this modifier.
         */
        public ModifierBuilder target(Block block, Map<IProperty<?>,Set<?>> properties){
            ResourceLocation identifier = ForgeRegistries.BLOCKS.getKey(block);
            Map<IProperty<?>,Set<Object>> map = this.targets.computeIfAbsent(identifier, o -> new HashMap<>());
            for(Map.Entry<IProperty<?>,Set<?>> entry : properties.entrySet()){
                IProperty<?> property = entry.getKey();
                Set<Object> values = map.computeIfAbsent(property, o -> new HashSet<>());
                for(Object value : entry.getValue()){
                    if(!property.getAllowedValues().contains(value))
                        throw new IllegalStateException("Value '" + value + "' is not a valid value for property '" + property.getName() + "'!");
                    values.add(value);
                }
            }
            return this;
        }

        /**
         * Adds the given block with the given property to the targets for this modifier.
         */
        public <T extends Comparable<T>> ModifierBuilder target(Block block, IProperty<T> property, T... values){
            return this.target(block, new HashMap<>(Collections.singletonMap(property, new HashSet<>(Collections.singleton(values)))));
        }

        /**
         * Adds the given block state to the targets for this modifier.
         */
        public ModifierBuilder target(IBlockState state){
            return this.target(
                state.getBlock(),
                state.getPropertyKeys().stream()
                    .sorted(Comparator.comparing(IProperty::getName))
                    .map(property -> Pair.of(property, new HashSet<>(Collections.singleton(state.getValue(property)))))
                    .collect(Collectors.toMap(
                        Pair::left, Pair::right,
                        (a, b) -> {throw new AssertionError();},
                        HashMap::new
                    ))
            );
        }

        /**
         * Adds the given model to be appended to any targeted block's model.
         */
        public ModifierBuilder appendModel(ResourceLocation location){
            this.appendModels.add(location);
            return this;
        }

        /**
         * Enables the pane culling fix for any targeted blocks.
         */
        public ModifierBuilder paneCullingFix(boolean enabled){
            this.paneCullingFix = enabled;
            return this;
        }
    }
}

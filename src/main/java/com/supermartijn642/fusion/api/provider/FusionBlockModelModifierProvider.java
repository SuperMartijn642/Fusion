package com.supermartijn642.fusion.api.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Allows generating block model modifier files.
 * Users must extend the class and overwrite {@link FusionBlockModelModifierProvider#generate()}.
 * Users may use {@link FusionBlockModelModifierProvider#modifier(ResourceLocation)} to obtain a builder for the given location.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public abstract class FusionBlockModelModifierProvider implements DataProvider {

    private final Map<ResourceLocation,ModifierBuilder> modifiers = new HashMap<>();
    private final String modName;
    private final DataGenerator generator;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionBlockModelModifierProvider(String modid, DataGenerator generator){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.generator = generator;
    }

    @Override
    public final void run(CachedOutput cache) throws IOException{
        this.generate();

        Path output = this.generator.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModifierBuilder> entry : this.modifiers.entrySet()){
            ResourceLocation location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getPath().endsWith(".json") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/blocks", location.getPath() + extension);
            DataProvider.saveStable(cache, json, output.resolve(path));
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
                Map<Property<?>,Set<Object>> properties = entry.getValue();
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
                        Property property = e.getKey();
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
            json.addProperty("pane_culling_fix", "true");
        return json;
    }

    /**
     * Configures block model modifiers which should be generated through {@link #modifier(ResourceLocation)}.
     */
    protected abstract void generate();

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
        private final Map<ResourceLocation,Map<Property<?>,Set<Object>>> targets = new HashMap<>();
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
            return this.target(Registry.BLOCK.getKey(block));
        }

        /**
         * Adds the given block with the given properties to the targets for this modifier.
         */
        public ModifierBuilder target(Block block, Map<Property<?>,Set<?>> properties){
            ResourceLocation identifier = Registry.BLOCK.getKey(block);
            Map<Property<?>,Set<Object>> map = this.targets.computeIfAbsent(identifier, o -> new HashMap<>());
            for(Map.Entry<Property<?>,Set<?>> entry : properties.entrySet()){
                Property<?> property = entry.getKey();
                Set<Object> values = map.computeIfAbsent(property, o -> new HashSet<>());
                for(Object value : entry.getValue()){
                    if(!property.getPossibleValues().contains(value))
                        throw new IllegalStateException("Value '" + value + "' is not a valid value for property '" + property.getName() + "'!");
                    values.add(value);
                }
            }
            return this;
        }

        /**
         * Adds the given block with the given property to the targets for this modifier.
         */
        public <T extends Comparable<T>> ModifierBuilder target(Block block, Property<T> property, T... values){
            return this.target(block, new HashMap<>(Map.of(property, new HashSet<>(Set.of(values)))));
        }

        /**
         * Adds the given block state to the targets for this modifier.
         */
        public ModifierBuilder target(BlockState state){
            return this.target(
                state.getBlock(),
                state.getProperties().stream()
                    .sorted(Comparator.comparing(Property::getName))
                    .map(property -> Pair.of(property, Set.of(state.getValue(property))))
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

package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

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

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

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
    public final void run(HashCache cache) throws IOException{
        this.generate();

        Path output = this.generator.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModifierBuilder> entry : this.modifiers.entrySet()){
            ResourceLocation location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getPath().endsWith(".json") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/blocks", location.getPath() + extension);
            DataProvider.save(GSON, cache, json, output.resolve(path));
        }
    }

    private JsonObject toJson(ModifierBuilder modifier){
        JsonObject json = new JsonObject();
        // Ignore missing targets
        if(modifier.ignoreMissingTargets)
            json.addProperty("ignore_missing_targets", true);
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
        // Priority
        if(modifier.priority != BlockModelModifierReloadListener.DEFAULT_PRIORITY)
            json.addProperty("priority", modifier.priority);
        // Default model overrides
        if(!modifier.defaultModelOverrides.isEmpty())
            json.add("default_model_overrides", serializeModelEntries(modifier.defaultModelOverrides));
        // Append models
        if(!modifier.appendModels.isEmpty()){
            JsonArray allSeries = new JsonArray(modifier.appendModels.size());
            for(List<ModelEntry> series : modifier.appendModels)
                allSeries.add(serializeModelEntries(series));
            json.add("append_models", allSeries);
        }
        // Pane culling fix
        if(modifier.paneCullingFix)
            json.addProperty("pane_culling_fix", true);
        return json;
    }

    private static JsonElement serializeModelEntries(List<ModelEntry> entries){
        if(entries.size() == 1)
            return serializeModelEntry(entries.get(0));
        JsonArray array = new JsonArray(entries.size());
        for(ModelEntry entry : entries)
            array.add(serializeModelEntry(entry));
        return array;
    }

    private static JsonElement serializeModelEntry(ModelEntry entry){
        if(entry.conditions.isEmpty() && entry.showBreakingOverlay == null)
            return new JsonPrimitive(entry.model.toString());
        JsonObject json = new JsonObject();
        json.addProperty("model", entry.model.toString());
        if(entry.showBreakingOverlay != null)
            json.addProperty("show_breaking_overlay", entry.showBreakingOverlay);
        if(entry.conditions.size() == 1)
            json.add("conditions", FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(entry.conditions.get(0)));
        else if(!entry.conditions.isEmpty()){
            JsonArray conditions = new JsonArray(entry.conditions.size());
            for(BlockStateModelPredicate condition : entry.conditions)
                conditions.add(FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(condition));
            json.add("conditions", conditions);
        }
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
        private boolean ignoreMissingTargets = false;
        private int priority = BlockModelModifierReloadListener.DEFAULT_PRIORITY;
        private final List<ModelEntry> defaultModelOverrides = new ArrayList<>();
        private final List<List<ModelEntry>> appendModels = new ArrayList<>();
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
         * Whether missing target entries should be ignored.
         * Useful for modded blocks which may not always be present.
         */
        public ModifierBuilder ignoreMissingTargets(boolean ignore){
            this.ignoreMissingTargets = ignore;
            return this;
        }

        /**
         * Sets the priority for this modifier.
         * Modifiers with a lower priority value are applied first.
         * The default priority is 100.
         */
        public ModifierBuilder priority(int priority){
            this.priority = priority;
            return this;
        }

        /**
         * Adds a default model override entry.
         * The first override whose condition is met gets used instead of the default model for the block.
         * @see ModelEntry#of(ResourceLocation)
         */
        public ModifierBuilder defaultModelOverride(ModelEntry entry){
            this.defaultModelOverrides.add(entry);
            return this;
        }

        /**
         * Adds an append model entry. The entry gets applied when its condition is met.
         * @see ModelEntry#of(ResourceLocation)
         */
        public ModifierBuilder appendModel(ModelEntry entry){
            this.appendModels.add(List.of(entry));
            return this;
        }

        /**
         * Adds an append model series. The first model in the series whose condition is met is applied.
         * @see ModelEntry#of(ResourceLocation)
         */
        public ModifierBuilder appendModelSeries(ModelEntry... entries){
            this.appendModels.add(List.of(entries));
            return this;
        }

        /**
         * Adds an append model series. The first model in the series whose condition is met is applied.
         * @see ModelEntry#of(ResourceLocation)
         */
        public ModifierBuilder appendModelSeries(List<ModelEntry> entries){
            this.appendModels.add(List.copyOf(entries));
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

    public static final class ModelEntry {

        public static ModelEntry of(ResourceLocation location){
            return new ModelEntry(location);
        }

        private final ResourceLocation model;
        private final List<BlockStateModelPredicate> conditions = new ArrayList<>();
        private Boolean showBreakingOverlay;

        private ModelEntry(ResourceLocation model){
            this.model = model;
        }

        /**
         * Adds the given conditions to this model entry.
         * @see DefaultBlockStateModelPredicates
         */
        public ModelEntry conditions(BlockStateModelPredicate... predicates){
            this.conditions.addAll(Arrays.asList(predicates));
            return this;
        }

        /**
         * Whether to show the breaking overlay on this model entry.
         */
        public ModelEntry showBreakingOverlay(@Nullable Boolean show){
            this.showBreakingOverlay = show;
            return this;
        }
    }
}

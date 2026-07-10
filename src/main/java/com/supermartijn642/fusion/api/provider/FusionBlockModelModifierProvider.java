package com.supermartijn642.fusion.api.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Allows generating block model modifier files.
 * Users must extend the class and overwrite {@link FusionBlockModelModifierProvider#generate()}.
 * Users may use {@link FusionBlockModelModifierProvider#modifier(Identifier)} to obtain a builder for the given location.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public abstract class FusionBlockModelModifierProvider implements DataProvider {

    private final Map<Identifier,ModifierBuilder> modifiers = new HashMap<>();
    private final String modName;
    private final PackOutput output;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionBlockModelModifierProvider(String modid, PackOutput output){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.output = output;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache){
        this.generate();

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path output = this.output.getOutputFolder();
        for(Map.Entry<Identifier,ModifierBuilder> entry : this.modifiers.entrySet()){
            Identifier location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getPath().endsWith(".json") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/blocks", location.getPath() + extension);
            tasks.add(DataProvider.saveStable(cache, json, output.resolve(path)));
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
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
                Identifier block = entry.getKey();
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
        if(entry.conditions.isEmpty() && entry.showBreakingOverlay == null && entry.randomOffset == null)
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
        if(entry.randomOffset != null)
            json.add("random_offset", entry.randomOffset.serialize());
        return json;
    }

    /**
     * Configures block model modifiers which should be generated through {@link #modifier(Identifier)}.
     */
    protected abstract void generate();

    /**
     * Creates or gets the block model modifier builder for the given location.
     */
    public final ModifierBuilder modifier(Identifier location){
        return this.modifiers.computeIfAbsent(location, ModifierBuilder::new);
    }

    @Override
    public String getName(){
        return "Fusion Block Model Modifier Provider: " + this.modName;
    }

    public static final class ModifierBuilder {
        private final Identifier location;
        private final Map<Identifier,Map<Property<?>,Set<Object>>> targets = new HashMap<>();
        private boolean ignoreMissingTargets = false;
        private int priority = BlockModelModifierReloadListener.DEFAULT_PRIORITY;
        private final List<ModelEntry> defaultModelOverrides = new ArrayList<>();
        private final List<List<ModelEntry>> appendModels = new ArrayList<>();
        private boolean paneCullingFix = false;

        private ModifierBuilder(Identifier location){
            this.location = location;
        }

        /**
         * Adds the given block identifier to the targets for this modifier.
         */
        public ModifierBuilder target(Identifier block){
            if(!this.targets.containsKey(block))
                this.targets.put(block, new HashMap<>());
            return this;
        }

        /**
         * Adds the given block to the targets for this modifier.
         */
        public ModifierBuilder target(Block block){
            return this.target(BuiltInRegistries.BLOCK.getKey(block));
        }

        /**
         * Adds the given block with the given properties to the targets for this modifier.
         */
        public ModifierBuilder target(Block block, Map<Property<?>,Set<?>> properties){
            Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
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
         * @see ModelEntry#of(Identifier)
         */
        public ModifierBuilder defaultModelOverride(ModelEntry entry){
            this.defaultModelOverrides.add(entry);
            return this;
        }

        /**
         * Adds an append model entry. The entry gets applied when its condition is met.
         * @see ModelEntry#of(Identifier)
         */
        public ModifierBuilder appendModel(ModelEntry entry){
            this.appendModels.add(List.of(entry));
            return this;
        }

        /**
         * Adds an append model series. The first model in the series whose condition is met is applied.
         * @see ModelEntry#of(Identifier)
         */
        public ModifierBuilder appendModelSeries(ModelEntry... entries){
            this.appendModels.add(List.of(entries));
            return this;
        }

        /**
         * Adds an append model series. The first model in the series whose condition is met is applied.
         * @see ModelEntry#of(Identifier)
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

        public static ModelEntry of(Identifier location){
            return new ModelEntry(location);
        }

        private final Identifier model;
        private final List<BlockStateModelPredicate> conditions = new ArrayList<>();
        private Boolean showBreakingOverlay;
        private RandomOffset randomOffset = null;

        private ModelEntry(Identifier model){
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

        /**
         * Sets no random offset for this model entry.
         */
        public ModelEntry noRandomOffset(){
            this.randomOffset = null;
            return this;
        }

        /**
         * Sets the random offset applied to this model entry to match the block.
         */
        public ModelEntry matchBlockRandomOffset(){
            this.randomOffset = () -> new JsonPrimitive("match_block");
            return this;
        }

        /**
         * Sets the random offset applied to this model entry to the given ranges.
         */
        public ModelEntry randomXYZOffset(@Nullable Long seed, float minXOffset, float maxXOffset, float minYOffset, float maxYOffset, float minZOffset, float maxZOffset){
            if(minXOffset > maxXOffset || minYOffset > maxYOffset || minZOffset > maxZOffset)
                throw new IllegalArgumentException("Min offset must be less than max offset!");
            if(minXOffset == 0 && maxXOffset == 0 && minYOffset == 0 && maxYOffset == 0 && minZOffset == 0 && maxZOffset == 0)
                return this.noRandomOffset();
            this.randomOffset = () -> {
                JsonObject json = new JsonObject();
                json.addProperty("type", "xyz");
                if(seed != null)
                    json.addProperty("seed", seed);
                if(minXOffset != 0 || maxXOffset != 0){
                    JsonArray arr = new JsonArray();
                    arr.add(minXOffset);
                    arr.add(maxXOffset);
                    json.add("x", arr);
                }
                if(minYOffset != 0 || maxYOffset != 0){
                    JsonArray arr = new JsonArray();
                    arr.add(minYOffset);
                    arr.add(maxYOffset);
                    json.add("y", arr);
                }
                if(minZOffset != 0 || maxZOffset != 0){
                    JsonArray arr = new JsonArray();
                    arr.add(minZOffset);
                    arr.add(maxZOffset);
                    json.add("z", arr);
                }
                return json;
            };
            return this;
        }
    }

    private interface RandomOffset {
        JsonElement serialize();
    }
}

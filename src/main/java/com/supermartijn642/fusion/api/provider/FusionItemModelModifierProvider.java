package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Allows generating item model modifier files.
 * Users must extend the class and overwrite {@link FusionItemModelModifierProvider#generate()}.
 * Users may use {@link FusionItemModelModifierProvider#modifier(ResourceLocation)} to obtain a builder for the given location.
 * <p>
 * Created 07/04/2025 by SuperMartijn642
 */
public abstract class FusionItemModelModifierProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Map<ResourceLocation,ModifierBuilder> modifiers = new HashMap<>();
    private final String modName;
    private final DataGenerator generator;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionItemModelModifierProvider(String modid, DataGenerator generator){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.generator = generator;
    }

    @Override
    public final void run(HashCache cache) throws IOException{
        this.generate();

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path output = this.generator.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModifierBuilder> entry : this.modifiers.entrySet()){
            ResourceLocation location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getPath().endsWith(".json") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/items", location.getPath() + extension);
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
        modifier.targets.stream()
            .sorted()
            .map(ResourceLocation::toString)
            .forEach(targets::add);
        json.add("targets", targets);
        // Priority
        if(modifier.priority != ItemModelModifierReloadListener.DEFAULT_PRIORITY)
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
        if(entry.conditions.isEmpty())
            return new JsonPrimitive(entry.model.toString());
        JsonObject json = new JsonObject();
        json.addProperty("model", entry.model.toString());
        if(entry.conditions.size() == 1)
            json.add("conditions", FusionItemModelPredicateRegistry.serializeItemModelPredicate(entry.conditions.get(0)));
        else if(!entry.conditions.isEmpty()){
            JsonArray conditions = new JsonArray(entry.conditions.size());
            for(ItemModelPredicate condition : entry.conditions)
                conditions.add(FusionItemModelPredicateRegistry.serializeItemModelPredicate(condition));
            json.add("conditions", conditions);
        }
        return json;
    }

    /**
     * Configures item model modifiers which should be generated through {@link #modifier(ResourceLocation)}.
     */
    protected abstract void generate();

    /**
     * Creates or gets the item model modifier builder for the given location.
     */
    public final ModifierBuilder modifier(ResourceLocation location){
        return this.modifiers.computeIfAbsent(location, ModifierBuilder::new);
    }

    @Override
    public String getName(){
        return "Fusion Item Model Modifier Provider: " + this.modName;
    }

    public static final class ModifierBuilder {
        private final ResourceLocation location;
        private final Set<ResourceLocation> targets = new HashSet<>();
        private boolean ignoreMissingTargets = false;
        private int priority = ItemModelModifierReloadListener.DEFAULT_PRIORITY;
        private final List<ModelEntry> defaultModelOverrides = new ArrayList<>();
        private final List<List<ModelEntry>> appendModels = new ArrayList<>();

        private ModifierBuilder(ResourceLocation location){
            this.location = location;
        }

        /**
         * Add the given item identifier to the targets for this modifier.
         */
        public ModifierBuilder target(ResourceLocation item){
            this.targets.add(item);
            return this;
        }

        /**
         * Add the given item to the targets for this modifier.
         */
        public ModifierBuilder target(Item item){
            return this.target(Registry.ITEM.getKey(item));
        }

        /**
         * Add the given block's corresponding item to the targets for this modifier.
         */
        public ModifierBuilder target(Block block){
            Item item = Item.byBlock(block);
            if(item == null || (block != Blocks.AIR && item == Items.AIR))
                throw new IllegalArgumentException("Block '" + block + "' does not have an item!");
            return this.target(item);
        }

        /**
         * Whether missing target entries should be ignored.
         * Useful for modded items which may not always be present.
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
         * The first override whose condition is met gets used instead of the default model for the item.
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
    }

    public static final class ModelEntry {

        public static ModelEntry of(ResourceLocation location){
            return new ModelEntry(location);
        }

        private final ResourceLocation model;
        private final List<ItemModelPredicate> conditions = new ArrayList<>();

        private ModelEntry(ResourceLocation model){
            this.model = model;
        }

        /**
         * Adds the given conditions to this model entry.
         * @see ItemModelPredicate
         */
        public ModelEntry conditions(ItemModelPredicate... predicates){
            this.conditions.addAll(Arrays.asList(predicates));
            return this;
        }
    }
}

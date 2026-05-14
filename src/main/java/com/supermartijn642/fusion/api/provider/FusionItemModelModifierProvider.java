package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.predicates.item.AndItemModelPredicate;
import com.supermartijn642.fusion.model.predicates.item.ItemPredicateRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
        this.modName = FabricLoader.getInstance().getModContainer(modid).map(ModContainer::getMetadata).map(ModMetadata::getName).orElse(modid);
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
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/items", location.getPath() + extension);
            DataProvider.save(GSON, cache, json, output.resolve(path));
        }
    }

    private JsonObject toJson(ModifierBuilder modifier){
        JsonObject json = new JsonObject();
        // Targets
        if(modifier.targets.isEmpty())
            throw new IllegalArgumentException("Modifier '" + modifier.location + "' must have at least one target!");
        JsonArray targets = new JsonArray();
        modifier.targets.stream()
            .sorted()
            .map(ResourceLocation::toString)
            .forEach(targets::add);
        json.add("targets", targets);
        // Default model
        if(modifier.defaultModel != null)
            json.addProperty("default_model", modifier.defaultModel.toString());
        // Conditional models
        JsonArray models = new JsonArray();
        for(Pair<ResourceLocation,ItemModelPredicate> pair : modifier.conditionalModels){
            JsonObject model = new JsonObject();
            model.addProperty("model", pair.left().toString());
            JsonArray conditions = new JsonArray();
            List<ItemModelPredicate> predicates = pair.right() instanceof AndItemModelPredicate ? ((AndItemModelPredicate)pair.right()).getPredicates() : List.of(pair.right());
            predicates.stream()
                .map(ItemPredicateRegistry::serializeItemPredicate)
                .forEach(conditions::add);
            model.add("conditions", conditions);
            models.add(model);
        }
        json.add("models", models);
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
        private final List<Pair<ResourceLocation,ItemModelPredicate>> conditionalModels = new ArrayList<>();
        private ResourceLocation defaultModel = null;

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
         * Sets the default model to use when none of the conditional models are applicable.
         */
        public ModifierBuilder defaultModel(ResourceLocation location){
            this.defaultModel = location;
            return this;
        }

        /**
         * Appends a conditional to this modifier.
         * Note that the order in which conditional models are added may be relevant.
         * The first conditional models for which its conditions are met will be used.
         * @see DefaultItemPredicates
         */
        public ModifierBuilder conditionalModel(ResourceLocation model, ItemModelPredicate condition){
            this.conditionalModels.add(Pair.of(model, condition));
            return this;
        }
    }
}

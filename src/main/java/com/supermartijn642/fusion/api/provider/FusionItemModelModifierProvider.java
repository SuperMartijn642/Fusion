package com.supermartijn642.fusion.api.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.predicates.item.AndItemModelPredicate;
import com.supermartijn642.fusion.model.predicates.item.ItemPredicateRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

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

    private final Map<ResourceLocation,ModifierBuilder> modifiers = new HashMap<>();
    private final String modName;
    private final PackOutput output;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionItemModelModifierProvider(String modid, PackOutput output){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.output = output;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache){
        this.generate();

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path output = this.output.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModifierBuilder> entry : this.modifiers.entrySet()){
            ResourceLocation location = entry.getKey();
            JsonObject json = this.toJson(entry.getValue());
            String extension = location.getPath().endsWith(".json") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "fusion/model_modifiers/items", location.getPath() + extension);
            tasks.add(DataProvider.saveStable(cache, json, output.resolve(path)));
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
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
            return this.target(BuiltInRegistries.ITEM.getKey(item));
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

package com.supermartijn642.fusion.model.modifiers.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.*;
import java.util.function.Function;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierReloadListener {

    private static final String LOCATION = "fusion/model_modifiers/items";
    private static final FileToIdConverter ID_CONVERTER = FileToIdConverter.json(LOCATION);
    private static final Matrix4fc IDENTITY_MATRIX = new Matrix4f().identity();
    private static final int DEFAULT_PRIORITY = 100;

    public static final ItemModelModifierReloadListener INSTANCE = new ItemModelModifierReloadListener();

    private final Map<Identifier,List<Properties>> modifiers = new HashMap<>();

    private ItemModelModifierReloadListener(){
    }

    public void registerModelDependencies(ResolvableModel.Resolver resolver){
        // Collect all model identifiers
        Set<Identifier> models = new HashSet<>();
        for(List<Properties> modifiers : this.modifiers.values()){ // Note: Even models that will technically never be used due to earlier conditions still need to be resolved for user warnings
            for(Properties modifier : modifiers){
                for(ModelEntry entry : modifier.defaultModelOverrides)
                    models.add(entry.model);
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    for(ModelEntry entry : conditionals){
                        models.add(entry.model);
                    }
                }
            }
        }
        // Mark the models
        models.forEach(resolver::markDependency);
    }

    public void applyModelModifiers(ModelBakery.BakingResult results, ItemModel.BakingContext bakingContext){
        Map<Identifier,ItemModel> bakedModels = results.itemStackModels();

        // Create model resolver
        Function<Identifier,ItemModel> modelResolver = new Function<>() {
            final Map<Identifier,ItemModel> models = new HashMap<>();

            @Override
            public ItemModel apply(Identifier identifier){
                ItemModel model = this.models.get(identifier);
                if(model != null)
                    return model;
                try{
                    model = new CuboidItemModelWrapper.Unbaked(identifier, Optional.empty(), List.of()).bake(bakingContext, IDENTITY_MATRIX);
                }catch(Exception e){
                    FusionClient.LOGGER.error("Caught exception baking model '{}':", identifier, e);
                    model = results.missingModels().item();
                }
                this.models.put(identifier, model);
                return model;
            }
        };

        // Create a modifier model for each target
        for(Map.Entry<Identifier,List<Properties>> entry : this.modifiers.entrySet()){
            Identifier target = entry.getKey();
            ItemModel targetModel = bakedModels.get(target);
            if(targetModel == null) continue;

            // Sort modifier properties by their priority
            List<Properties> modifiers = entry.getValue();
            modifiers.sort(Comparator.<Properties>comparingInt(p -> p.priority).thenComparing(p -> p.location));

            // Resolve default model overwrites
            List<ItemModelModifierItemModel.ConditionalModel> defaultModelOverrides = createConditionModels(
                modifiers.stream().flatMap(p -> p.defaultModelOverrides.stream()).toList(),
                modelResolver
            );

            // Resolve append models
            List<List<ItemModelModifierItemModel.ConditionalModel>> appendModels = new ArrayList<>();
            for(Properties modifier : modifiers){
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    List<ItemModelModifierItemModel.ConditionalModel> resolvedConditionals = createConditionModels(
                        conditionals,
                        modelResolver
                    );
                    if(!resolvedConditionals.isEmpty())
                        appendModels.add(resolvedConditionals);
                }
            }
            appendModels = List.copyOf(appendModels);

            // Create the modifier model
            ItemModelModifierItemModel modifierModel = new ItemModelModifierItemModel(
                targetModel,
                defaultModelOverrides,
                appendModels
            );
            bakedModels.put(target, modifierModel);
        }

        // Clear modifier data
        this.modifiers.clear();
    }

    private static List<ItemModelModifierItemModel.ConditionalModel> createConditionModels(List<ModelEntry> modelEntries, Function<Identifier,ItemModel> modelResolver){
        List<ItemModelModifierItemModel.ConditionalModel> conditionals = new ArrayList<>();
        for(ModelEntry entry : modelEntries){
            if(entry.conditions != null && entry.conditions.alwaysFalse()) // Skip models for which the condition is always false
                continue;
            conditionals.add(new ItemModelModifierItemModel.ConditionalModel(
                modelResolver.apply(entry.model),
                entry.conditions
            ));
            if(entry.conditions == null || entry.conditions.alwaysTrue()) // If the condition is always true, any later entries will never be reached
                break;
        }
        return List.copyOf(conditionals);
    }

    public void reload(ResourceManager resourceManager){
        this.modifiers.clear();

        // Find all item model modifier files
        Map<Identifier,JsonElement> resources = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, ID_CONVERTER, JsonOps.INSTANCE, new Codec<>() {
            @Override
            public <T> DataResult<Pair<JsonElement,T>> decode(DynamicOps<T> ops, T input){
                return DataResult.success(com.mojang.datafixers.util.Pair.of(ops.convertTo(JsonOps.INSTANCE, input), input));
            }

            @Override
            public <T> DataResult<T> encode(JsonElement input, DynamicOps<T> ops, T prefix){
                return DataResult.success(JsonOps.INSTANCE.convertTo(ops, input));
            }
        }, resources);

        // Parse all the item model modifier files
        for(Map.Entry<Identifier,JsonElement> entry : resources.entrySet()){
            Identifier location = entry.getKey();
            if(!entry.getValue().isJsonObject()){
                FusionClient.LOGGER.error("Item model modifier '{}' must contain a json object!", location);
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                this.parseResource(json, location);
            }catch(JsonParseException e){
                LoggingHelper.logUserError(e, "Failed to parse item model modifier '%s':", location);
            }
        }
    }

    private void parseResource(JsonObject json, Identifier location){
        // Get whether to ignore missing target entries
        boolean ignoreMissingTargets = false;
        if(json.has("ignore_missing_targets")){
            if(!json.get("ignore_missing_targets").isJsonPrimitive() || !json.getAsJsonPrimitive("ignore_missing_targets").isBoolean())
                throw new JsonParseException("Property 'ignore_missing_targets' must be a boolean!");
            ignoreMissingTargets = json.get("ignore_missing_targets").getAsBoolean();
        }

        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model modifier must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<Identifier> targets = new HashSet<>();
        try{
            for(JsonElement element : targetsJson){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'targets' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!");
                Identifier identifier = Identifier.parse(element.getAsString());
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(identifier);
                if(item.isEmpty()){
                    if(ignoreMissingTargets)
                        continue;
                    throw new JsonParseException("Could not find an item for target '" + identifier + "'!");
                }
                targets.add(identifier);
            }
        }catch(JsonParseException e){
            throw new JsonParseException("Failed to parse a 'targets' entry", e);
        }
        if(targets.isEmpty())
            return;

        // Priority
        int priority = DEFAULT_PRIORITY;
        if(json.has("priority")){
            if(!json.get("priority").isJsonPrimitive() || !json.getAsJsonPrimitive("priority").isNumber())
                throw new JsonParseException("Property 'priority' must be a number!");
            priority = json.getAsJsonPrimitive("priority").getAsInt();
        }

        // Get default model overrides
        List<ModelEntry> defaultModel = List.of();
        if(json.has("default_model_overrides")){
            try{
                defaultModel = parseModelEntry(json.get("default_model_overrides"));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse 'default_model_overrides'", e);
            }
        }

        // Read legacy 'default_model' entries
        if(json.has("default_model")){
            if(json.has("default_model_overrides"))
                throw new JsonParseException("Cannot use legacy 'default_model' and new 'default_model_overrides' properties at the same time!");
            if(!json.get("default_model").isJsonPrimitive() || !json.getAsJsonPrimitive("default_model").isString())
                throw new JsonParseException("Property 'default_model' must be a string!");
            if(!IdentifierUtil.isValidIdentifier(json.get("default_model").getAsString()))
                throw new JsonParseException("Default model must be a valid identifier, not '" + json.get("default_model").getAsString() + "'!");
            defaultModel = List.of(ModelEntry.simple(Identifier.parse(json.get("default_model").getAsString())));
        }

        // Get append models
        List<List<ModelEntry>> appendModels = new ArrayList<>();
        if(json.has("append_models")){
            if(!json.get("append_models").isJsonArray())
                throw new JsonParseException("Property 'append_models' must be an array!");
            try{
                for(JsonElement e : json.getAsJsonArray("append_models"))
                    appendModels.add(parseModelEntry(e));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse 'append_models' entry", e);
            }
        }

        // Read legacy 'models' entries
        if(json.has("models")){
            if(json.has("append_models"))
                throw new JsonParseException("Cannot use legacy 'models' and new 'append_models' properties at the same time!");
            if(!json.get("models").isJsonArray())
                throw new JsonParseException("Property 'models' must be an array!");
            JsonArray modelsJson = json.getAsJsonArray("models");
            for(JsonElement e : modelsJson){
                if(!e.isJsonObject())
                    throw new JsonParseException("Array property 'models' must only contain objects!");
                appendModels.add(parseModelEntry(e));
            }
        }

        // Put the properties into the map
        Properties properties = new Properties(priority, location, defaultModel, appendModels);
        for(Identifier target : targets)
            this.modifiers.computeIfAbsent(target, t -> new ArrayList<>(8)).add(properties);
    }

    private static List<ModelEntry> parseModelEntry(JsonElement element){
        if(!element.isJsonArray() && !element.isJsonObject() && (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()))
            throw new JsonParseException("Must be an array, object, or string!");

        // Handle arrays
        if(element.isJsonArray()){
            List<ModelEntry> entries = new ArrayList<>(element.getAsJsonArray().size());
            try{
                for(JsonElement e : element.getAsJsonArray())
                    entries.addAll(parseModelEntry(e));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse entry", e);
            }
            return entries;
        }

        // Handle simple strings
        if(element.isJsonPrimitive()){
            String identifier = element.getAsString();
            if(!IdentifierUtil.isValidIdentifier(identifier))
                throw new JsonParseException("String must be a valid identifier, not '" + identifier + "'!");
            return List.of(ModelEntry.simple(Identifier.parse(identifier)));
        }

        JsonObject object = element.getAsJsonObject();

        // Model identifier
        if(!object.has("model") || !object.get("model").isJsonPrimitive() || !object.getAsJsonPrimitive("model").isString())
            throw new JsonParseException("Object must have string property 'model'!");
        if(!IdentifierUtil.isValidIdentifier(object.get("model").getAsString()))
            throw new JsonParseException("Property 'model' must be a valid identifier, not '" + object.get("model").getAsString() + "'!");
        Identifier model = Identifier.parse(object.get("model").getAsString());

        // Conditions
        ItemModelPredicate conditions = null;
        if(object.has("conditions")){
            try{
                if(object.get("conditions").isJsonObject())
                    conditions = FusionItemModelPredicateRegistry.deserializeItemModelPredicate(object.get("conditions").getAsJsonObject());
                else if(object.get("conditions").isJsonArray()){
                    List<ItemModelPredicate> predicates = new ArrayList<>();
                    for(JsonElement entry : object.get("conditions").getAsJsonArray()){
                        if(!entry.isJsonObject())
                            throw new JsonParseException("Array entry must be an object!");
                        predicates.add(FusionItemModelPredicateRegistry.deserializeItemModelPredicate(entry.getAsJsonObject()));
                    }
                    conditions = DefaultItemModelPredicates.and(predicates.toArray(new ItemModelPredicate[0]));
                }else
                    throw new JsonParseException("Value must an object or array of objects!");
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse conditions for model '" + model + "'", e);
            }
            conditions = conditions.simplify();
        }

        return List.of(new ModelEntry(model, conditions));
    }

    private record Properties(int priority, Identifier location, List<ModelEntry> defaultModelOverrides, List<List<ModelEntry>> appendModels) {
    }

    private record ModelEntry(Identifier model, @Nullable ItemModelPredicate conditions) {
        static ModelEntry simple(Identifier model){
            return new ModelEntry(model, null);
        }
    }
}

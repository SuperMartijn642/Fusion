package com.supermartijn642.fusion.model.modifiers.item;

import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.ItemModelPredicate;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.*;
import java.util.function.Function;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/items";
    private static final int DEFAULT_PRIORITY = 100;

    public static final ItemModelModifierReloadListener INSTANCE = new ItemModelModifierReloadListener();

    private final Map<ModelResourceLocation,List<Properties>> modifiers = new HashMap<>();

    private ItemModelModifierReloadListener(){
    }

    public void registerModelDependencies(ModelBakery bakery){
        // Collect all model identifiers
        Set<ResourceLocation> models = new HashSet<>();
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
        for(ResourceLocation model : models){
            UnbakedModel unbakedModel = bakery.getModel(model);
            bakery.unbakedCache.put(model, unbakedModel);
            bakery.topLevelModels.put(model, unbakedModel);
            unbakedModel.resolveParents(bakery::getModel);
        }
    }

    public void applyModelModifiers(ModelBakery modelBakery){
        Map<ResourceLocation,BakedModel> bakedModels = modelBakery.getBakedTopLevelModels();

        // Create model resolver
        Function<ResourceLocation,BakedModel> modelResolver = bakedModels::get;

        // Create a modifier model for each target
        for(Map.Entry<ModelResourceLocation,List<Properties>> entry : this.modifiers.entrySet()){
            ModelResourceLocation target = entry.getKey();
            BakedModel targetModel = bakedModels.get(target);
            if(targetModel == null) continue;

            // Sort modifier properties by their priority
            List<Properties> modifiers = entry.getValue();
            modifiers.sort(Comparator.<Properties>comparingInt(p -> p.priority).thenComparing(p -> p.location));

            // Resolve default model overwrites
            List<ItemModelModifierBakedModel.ConditionalModel> defaultModelOverrides = createConditionModels(
                modifiers.stream().flatMap(p -> p.defaultModelOverrides.stream()).toList(),
                modelResolver
            );

            // Resolve append models
            List<List<ItemModelModifierBakedModel.ConditionalModel>> appendModels = new ArrayList<>();
            for(Properties modifier : modifiers){
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    List<ItemModelModifierBakedModel.ConditionalModel> resolvedConditionals = createConditionModels(
                        conditionals,
                        modelResolver
                    );
                    if(!resolvedConditionals.isEmpty())
                        appendModels.add(resolvedConditionals);
                }
            }
            appendModels = List.copyOf(appendModels);

            // Create the modifier model
            ItemModelModifierBakedModel modifierModel = new ItemModelModifierBakedModel(
                targetModel,
                defaultModelOverrides,
                appendModels
            );
            bakedModels.put(target, modifierModel);
        }

        // Clear modifier data
        this.modifiers.clear();
    }

    private static List<ItemModelModifierBakedModel.ConditionalModel> createConditionModels(List<ModelEntry> modelEntries, Function<ResourceLocation,BakedModel> modelResolver){
        List<ItemModelModifierBakedModel.ConditionalModel> conditionals = new ArrayList<>();
        for(ModelEntry entry : modelEntries){
            if(entry.conditions != null && entry.conditions.alwaysFalse()) // Skip models for which the condition is always false
                continue;
            conditionals.add(new ItemModelModifierBakedModel.ConditionalModel(
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
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        FileToIdConverter fileToId = FileToIdConverter.json(LOCATION);
        for(Map.Entry<ResourceLocation,Resource> entry : fileToId.listMatchingResources(resourceManager).entrySet()){
            ResourceLocation location = fileToId.fileToId(entry.getKey());
            try(Reader reader = entry.getValue().openAsReader()){
                JsonElement element = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                resources.put(location, element);
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse json from item model predicates file '{}'!", entry.getKey(), e);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading item model predicates file at '{}'!", entry.getKey(), e);
            }
        }

        // Parse all the item model modifier files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
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

    private void parseResource(JsonObject json, ResourceLocation location){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model modifier must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<ResourceLocation> targets = new HashSet<>();
        try{
            for(JsonElement element : targetsJson){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'targets' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!");
                ResourceLocation identifier = new ResourceLocation(element.getAsString());
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(identifier);
                if(item.isEmpty())
                    throw new JsonParseException("Could not find an item for target '" + identifier + "'!");
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
            defaultModel = List.of(ModelEntry.simple(new ResourceLocation(json.get("default_model").getAsString())));
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
        for(ResourceLocation target : targets)
            this.modifiers.computeIfAbsent(new ModelResourceLocation(target, "inventory"), t -> new ArrayList<>(8)).add(properties);
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
            return List.of(ModelEntry.simple(new ResourceLocation(identifier)));
        }

        JsonObject object = element.getAsJsonObject();

        // Model identifier
        if(!object.has("model") || !object.get("model").isJsonPrimitive() || !object.getAsJsonPrimitive("model").isString())
            throw new JsonParseException("Object must have string property 'model'!");
        if(!IdentifierUtil.isValidIdentifier(object.get("model").getAsString()))
            throw new JsonParseException("Property 'model' must be a valid identifier, not '" + object.get("model").getAsString() + "'!");
        ResourceLocation model = new ResourceLocation(object.get("model").getAsString());

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

    private record Properties(int priority, ResourceLocation location, List<ModelEntry> defaultModelOverrides, List<List<ModelEntry>> appendModels) {
    }

    private record ModelEntry(ResourceLocation model, @Nullable ItemModelPredicate conditions) {
        static ModelEntry simple(ResourceLocation model){
            return new ModelEntry(model, null);
        }
    }
}

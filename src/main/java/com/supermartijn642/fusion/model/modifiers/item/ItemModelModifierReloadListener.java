package com.supermartijn642.fusion.model.modifiers.item;

import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.modifier.item.ItemPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.predicates.AndItemPredicate;
import com.supermartijn642.fusion.model.modifiers.item.predicates.ItemPredicateRegistry;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 20/09/2024 by SuperMartijn642
 */
public class ItemModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/items";

    public static final ItemModelModifierReloadListener INSTANCE = new ItemModelModifierReloadListener();

    private final Map<ModelResourceLocation,ItemModelPredicatesProperties> models = new HashMap<>();

    private ItemModelModifierReloadListener(){
    }

    public List<ModelResourceLocation> registerPredicateModels(){
        Set<ResourceLocation> models = new HashSet<>();
        for(ItemModelPredicatesProperties properties : this.models.values())
            models.addAll(properties.dependencies());
        return models.stream().map(ItemModelModifierReloadListener::predicateModelLocation).collect(Collectors.toList());
    }

    public void applyPredicateModels(ModelBakery bakery){
        RegistrySimple<ModelResourceLocation,IBakedModel> bakedModels = bakery.bakedRegistry;
        for(Map.Entry<ModelResourceLocation,ItemModelPredicatesProperties> entry : this.models.entrySet()){
            ModelResourceLocation target = entry.getKey();
            ItemModelPredicatesProperties properties = entry.getValue();
            if(!bakedModels.containsKey(target)) continue;
            IBakedModel defaultModel = properties.defaultModel == null ? bakedModels.getObject(target) : bakedModels.getObject(predicateModelLocation(properties.defaultModel));
            List<Pair<ItemPredicate,IBakedModel>> models = properties.models.stream()
                .map(pair -> pair.mapRight(ItemModelModifierReloadListener::predicateModelLocation).mapRight(bakedModels::getObject))
                .collect(Collectors.toList());
            bakedModels.putObject(target, new ItemModelModifierBakedModel(defaultModel, models));
        }
    }

    private static ModelResourceLocation predicateModelLocation(ResourceLocation modelLocation){
        return new ModelResourceLocation(modelLocation, "fusion_predicate_model");
    }

    public void reload(IResourceManager resourceManager){
        this.models.clear();

        // Find all item model predicate files
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        for(ResourceLocation fullLocation : BlockModelModifierReloadListener.listResources(resourceManager, LOCATION, s -> s.endsWith(".json"))){
            ResourceLocation location = new ResourceLocation(fullLocation.getResourceDomain(), fullLocation.getResourcePath().substring(LOCATION.length() + 1, fullLocation.getResourcePath().length() - ".json".length()));
            try(IResource resource = resourceManager.getResource(fullLocation)){
                Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                JsonElement element = JsonUtils.fromJson(GSON, reader, JsonElement.class);
                resources.put(location, element);
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse json from item model predicates file '{}'!", fullLocation, e);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading item model predicates file at '{}'!", fullLocation, e);
            }
        }

        // Parse all the item model predicate files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
            if(!entry.getValue().isJsonObject()){
                FusionClient.LOGGER.error("Item model modifier file '{}' must contain a json object!", location);
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                this.parseResource(json);
            }catch(JsonParseException e){
                LoggingHelper.logUserError(e, "Failed to parse item model modifier file '%s':", location);
            }
        }
    }

    private void parseResource(JsonObject json){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Item model modifier file must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<ModelResourceLocation> targets = new HashSet<>();
        for(JsonElement element : targetsJson){
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new JsonParseException("Array property 'targets' must only contain strings!");
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!!");
            ResourceLocation identifier = new ResourceLocation(element.getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(identifier);
            if(item == null || item == Items.AIR)
                throw new JsonParseException("Could not find an item for target '" + identifier + "'!");
            targets.add(new ModelResourceLocation(identifier, "inventory"));
        }
        if(targets.isEmpty())
            return;

        // Get the default model
        ResourceLocation defaultModel = null;
        if(json.has("default_model")){
            if(!json.get("default_model").isJsonPrimitive() || !json.getAsJsonPrimitive("default_model").isString())
                throw new JsonParseException("Property 'default_model' must be a string!");
            if(!IdentifierUtil.isValidIdentifier(json.get("default_model").getAsString()))
                throw new JsonParseException("Default model must be a valid identifier, not '" + json.get("default_model").getAsString() + "'!");
            defaultModel = new ResourceLocation(json.get("default_model").getAsString());
        }

        // Get the models
        if(!json.has("models") || !json.get("models").isJsonArray())
            throw new JsonParseException("Item model modifier file must have array property 'models'!");
        JsonArray modelsJson = json.getAsJsonArray("models");
        List<Pair<ItemPredicate,ResourceLocation>> models = new ArrayList<>();
        for(JsonElement element : modelsJson){
            if(!element.isJsonObject())
                throw new JsonParseException("Array property 'models' must only contain objects!");
            models.add(this.parseModelEntry(element.getAsJsonObject()));
        }
        if(defaultModel == null && models.isEmpty())
            return;

        // Put everything into the map
        ItemModelPredicatesProperties properties = new ItemModelPredicatesProperties(defaultModel, models);
        for(ModelResourceLocation target : targets)
            this.models.put(target, properties);
    }

    private Pair<ItemPredicate,ResourceLocation> parseModelEntry(JsonObject json){
        // Model
        if(!json.has("model") || !json.get("model").isJsonPrimitive() || !json.getAsJsonPrimitive("model").isString())
            throw new JsonParseException("Models entry must have string property 'model'!");
        if(!IdentifierUtil.isValidIdentifier(json.get("model").getAsString()))
            throw new JsonParseException("Model must be a valid identifier, not '" + json.get("model").getAsString() + "'!");
        ResourceLocation model = new ResourceLocation(json.get("model").getAsString());

        // Conditions
        if(!json.has("conditions") || !json.get("conditions").isJsonArray())
            throw new JsonParseException("Models entry must have array property 'conditions'!");
        JsonArray conditionsJson = json.getAsJsonArray("conditions");
        if(conditionsJson.size() == 0)
            throw new JsonParseException("Model entry property 'conditions' must not be empty!");
        List<ItemPredicate> predicates = new ArrayList<>();
        for(JsonElement element : conditionsJson){
            if(!element.isJsonObject())
                throw new JsonParseException("Model entry property 'conditions' must only contain objects!");
            predicates.add(ItemPredicateRegistry.deserializeItemPredicate(element.getAsJsonObject()));
        }
        ItemPredicate predicate = predicates.size() == 1 ? predicates.get(0) : new AndItemPredicate(predicates);

        return Pair.of(predicate, model);
    }

    private static class ItemModelPredicatesProperties {
        final ResourceLocation defaultModel;
        final List<Pair<ItemPredicate,ResourceLocation>> models;

        private ItemModelPredicatesProperties(ResourceLocation defaultModel, List<Pair<ItemPredicate,ResourceLocation>> models){
            this.defaultModel = defaultModel;
            this.models = models;
        }

        Collection<ResourceLocation> dependencies(){
            Set<ResourceLocation> models = new HashSet<>(this.models.size() + 1);
            if(this.defaultModel != null)
                models.add(this.defaultModel);
            for(Pair<ItemPredicate,ResourceLocation> entry : this.models)
                models.add(entry.right());
            return models;
        }
    }
}

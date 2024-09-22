package com.supermartijn642.fusion.model.overlays;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.state.IProperty;
import net.minecraft.state.Property;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelOverlayReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/block_model_overlays";

    public static final BlockModelOverlayReloadListener INSTANCE = new BlockModelOverlayReloadListener();

    private final Map<ModelResourceLocation,List<ResourceLocation>> models = new HashMap<>();

    private BlockModelOverlayReloadListener(){
    }

    public void registerOverlays(ModelBakery bakery){
        Set<ResourceLocation> models = new HashSet<>();
        for(List<ResourceLocation> list : this.models.values())
            models.addAll(list);
        for(ResourceLocation model : models){
            IUnbakedModel unbakedModel = bakery.getModel(model);
            bakery.unbakedCache.put(model, unbakedModel);
            bakery.topLevelModels.put(model, unbakedModel);
        }
    }

    public void applyOverlays(ModelBakery bakery){
        Map<ResourceLocation,IBakedModel> bakedModels = bakery.getBakedTopLevelModels();
        for(Map.Entry<ModelResourceLocation,List<ResourceLocation>> entry : this.models.entrySet()){
            ModelResourceLocation target = entry.getKey();
            IBakedModel targetModel = bakedModels.get(target);
            List<ResourceLocation> overlays = entry.getValue();
            List<IBakedModel> overlayModels = overlays.stream().map(bakedModels::get).collect(Collectors.toList());
            bakedModels.put(target, new BlockModelOverlayBakedModel(targetModel, overlayModels));
        }
    }

    public void reload(IResourceManager resourceManager){
        this.models.clear();

        // Find all overlay files
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        for(ResourceLocation fullLocation : resourceManager.listResources(LOCATION, s -> s.endsWith(".json"))){
            ResourceLocation location = new ResourceLocation(fullLocation.getNamespace(), fullLocation.getPath().substring(LOCATION.length() + 1, fullLocation.getPath().length() - ".json".length()));
            try{
                IResource resource = resourceManager.getResource(fullLocation);
                try(Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))){
                    JsonElement element = JSONUtils.fromJson(GSON, reader, JsonElement.class);
                    resources.put(location, element);
                }
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse json from block model overlay '{}'!", fullLocation, e);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading block model overlay at '{}'!", fullLocation, e);
            }
        }

        // Parse all the model overlay files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
            if(!entry.getValue().isJsonObject())
                throw new IllegalArgumentException("Block model overlay '" + location + "' must contain a json object!");
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                this.parseResource(json);
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse block model overlay '{}': {}", location, e.getMessage());
            }
        }
    }

    private void parseResource(JsonObject json){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model overlay must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<ModelResourceLocation> targets = new HashSet<>();
        for(JsonElement element : targetsJson){
            if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){ // Handle simple strings
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!!");
                ResourceLocation identifier = new ResourceLocation(element.getAsString());
                Block block = Registry.BLOCK.get(identifier);
                //noinspection ConstantValue
                if(block == null || block == Blocks.AIR)
                    throw new JsonParseException("Could not find a block for model overlay target '" + identifier + "'!");
                block.getStateDefinition().getPossibleStates().stream()
                    .map(BlockModelShapes::stateToModelLocation)
                    .forEach(targets::add);
            }else if(element.isJsonObject()){ // Handle blocks with specific state properties
                this.parseTarget(element.getAsJsonObject())
                    .map(BlockModelShapes::stateToModelLocation)
                    .forEach(targets::add);
            }else
                throw new JsonParseException("Model overlay 'targets' array must only contain objects and strings!");
        }
        if(targets.isEmpty())
            return;

        // Get the models
        if(!json.has("append") || !json.get("append").isJsonArray())
            throw new JsonParseException("Model overlay must have array property 'append'!");
        JsonArray appendJson = json.getAsJsonArray("append");
        Set<ResourceLocation> models = new LinkedHashSet<>(); // This should maintain order
        for(JsonElement element : appendJson){
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new JsonParseException("Array property 'append' must only contain strings!");
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Model must be a valid identifier, not '" + element.getAsString() + "'!!");
            models.add(new ResourceLocation(element.getAsString()));
        }
        if(models.isEmpty())
            return;

        // Put the overlays into the map
        for(ModelResourceLocation target : targets){
            List<ResourceLocation> overlays = this.models.computeIfAbsent(target, t -> new ArrayList<>());
            overlays.addAll(models);
        }
    }

    private Stream<BlockState> parseTarget(JsonObject json){
        // Block
        if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
            throw new JsonParseException("Target must have string property 'block'!");
        if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
            throw new JsonParseException("Target property 'block' must be a valid identifier, not '" + json.get("block").getAsString() + "'!!");
        ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
        Block block = Registry.BLOCK.get(identifier);
        //noinspection ConstantValue
        if(block == null || block == Blocks.AIR)
            throw new JsonParseException("Could not find a block for model overlay target '" + identifier + "'!");

        // Properties
        Map<IProperty<?>,Set<?>> properties = new HashMap<>();
        if(!json.has("properties") || !json.get("properties").isJsonObject())
            throw new JsonParseException("Match block predicate must have object property 'properties'!");
        if(json.getAsJsonObject("properties").size() == 0)
            throw new JsonParseException("At least one property must be specified for match state predicate!");
        for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
            // Parse the property
            IProperty<?> property = block.getStateDefinition().getProperty(entry.getKey());
            if(property == null)
                throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
            // Parse the values
            ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
            if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                Optional<?> value = property.getValue(entry.getValue().getAsString());
                if(!value.isPresent())
                    throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                builder.add(value.get());
            }else if(entry.getValue().isJsonArray()){
                if(entry.getValue().getAsJsonArray().size() == 0)
                    throw new JsonParseException("Valid values for property '" + property.getName() + "' cannot be empty!");
                for(JsonElement element : entry.getValue().getAsJsonArray()){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                    Optional<?> value = property.getValue(element.getAsString());
                    if(!value.isPresent())
                        throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }
            }else
                throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
            properties.put(property, builder.build());
        }

        // Find all matching states
        Stream<BlockState> states = Stream.of(block.getStateDefinition().any());
        for(IProperty<?> property : block.getStateDefinition().getProperties()){
            if(properties.containsKey(property)){
                Set<?> values = properties.get(property);
                states = states.flatMap(state -> values.stream().map(value -> stateWithValue(state, property, value)));
            }else
                states = states.flatMap(state -> property.getPossibleValues().stream().map(value -> stateWithValue(state, property, value)));
        }
        return states;
    }

    private static <T extends Comparable<T>> BlockState stateWithValue(BlockState state, IProperty<?> property, Object value){
        //noinspection unchecked
        return state.setValue((Property<T>)property, (T)value);
    }
}

package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.ResourcePackExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.resources.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/blocks";

    public static final BlockModelModifierReloadListener INSTANCE = new BlockModelModifierReloadListener();

    private final Map<ModelResourceLocation,Properties> models = new HashMap<>();

    private BlockModelModifierReloadListener(){
    }

    public List<ModelResourceLocation> registerOverlays(){
        Set<ResourceLocation> models = new HashSet<>();
        for(Properties properties : this.models.values())
            models.addAll(properties.appendModels);
        return models.stream().map(BlockModelModifierReloadListener::overlayModelLocation).collect(Collectors.toList());
    }

    public void applyOverlays(ModelBakery bakery){
        RegistrySimple<ModelResourceLocation,IBakedModel> bakedModels = bakery.bakedRegistry;
        for(Map.Entry<ModelResourceLocation,Properties> entry : this.models.entrySet()){
            ModelResourceLocation target = entry.getKey();
            IBakedModel targetModel = bakedModels.getObject(target);
            if(targetModel == null) continue;
            Properties properties = entry.getValue();
            List<ResourceLocation> overlays = properties.appendModels;
            List<IBakedModel> overlayModels = overlays.stream().map(BlockModelModifierReloadListener::overlayModelLocation).map(bakedModels::getObject).collect(Collectors.toList());
            IBakedModel model = new BlockModelModifierBakedModel(targetModel, overlayModels, properties.showBreakingOverlay);
            if(properties.paneCullingFix)
                model = new PaneCullingBakedModel(model);
            bakedModels.putObject(target, model);
        }
    }

    private static ModelResourceLocation overlayModelLocation(ResourceLocation modelLocation){
        return new ModelResourceLocation(modelLocation, "fusion_overlay_model");
    }

    public void reload(IResourceManager resourceManager){
        this.models.clear();

        // Find all overlay files
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        for(ResourceLocation fullLocation : listResources(resourceManager, LOCATION, s -> s.endsWith(".json"))){
            ResourceLocation location = new ResourceLocation(fullLocation.getResourceDomain(), fullLocation.getResourcePath().substring(LOCATION.length() + 1, fullLocation.getResourcePath().length() - ".json".length()));
            try(IResource resource = resourceManager.getResource(fullLocation)){
                Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                JsonElement element = JsonUtils.fromJson(GSON, reader, JsonElement.class);
                resources.put(location, element);
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse json from block model overlay '{}'!", fullLocation, e);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading block model overlay at '{}'!", fullLocation, e);
            }
        }

        // Parse all the model overlay files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
            if(!entry.getValue().isJsonObject()){
                FusionClient.LOGGER.error("Block model overlay '{}' must contain a json object!", location);
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                this.parseResource(json);
            }catch(JsonParseException e){
                LoggingHelper.logUserError(e, "Failed to parse block model overlay '%s':", location);
            }
        }
    }

    private void parseResource(JsonObject json){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model overlay must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<ModelResourceLocation> targets = new HashSet<>();
        BlockStateMapper blockStateMapper = Minecraft.getMinecraft().modelManager.getBlockModelShapes().getBlockStateMapper();
        for(JsonElement element : targetsJson){
            if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){ // Handle simple strings
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!!");
                ResourceLocation identifier = new ResourceLocation(element.getAsString());
                Block block = ForgeRegistries.BLOCKS.getValue(identifier);
                if(block == null || block == Blocks.AIR)
                    throw new JsonParseException("Could not find a block for model overlay target '" + identifier + "'!");
                block.getBlockState().getValidStates().stream()
                    .map(state -> blockStateMapper.getVariants(state.getBlock()).get(state))
                    .forEach(targets::add);
            }else if(element.isJsonObject()){ // Handle blocks with specific state properties
                this.parseTarget(element.getAsJsonObject())
                    .map(state -> blockStateMapper.getVariants(state.getBlock()).get(state))
                    .forEach(targets::add);
            }else
                throw new JsonParseException("Model overlay 'targets' array must only contain objects and strings!");
        }
        if(targets.isEmpty())
            return;

        // Give warning when json is empty as user likely misspelled something
        if(!json.has("append") && !json.has("pane_culling_fix"))
            throw new JsonParseException("Must have either 'append' or 'pane_culling_fix' property!");

        // Get the models
        Set<ResourceLocation> models = new LinkedHashSet<>(); // This should maintain order
        if(json.has("append")){
            if(!json.get("append").isJsonArray())
                throw new JsonParseException("Property 'append' must be an array!");
            JsonArray appendJson = json.getAsJsonArray("append");
            for(JsonElement element : appendJson){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'append' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Model must be a valid identifier, not '" + element.getAsString() + "'!!");
                models.add(new ResourceLocation(element.getAsString()));
            }
        }

        // Get whether to use the appended models for breaking overlay
        Boolean showBreakingOverlay = null;
        if(json.has("show_breaking_overlay")){
            if(!json.get("show_breaking_overlay").isJsonPrimitive() || !json.get("show_breaking_overlay").getAsJsonPrimitive().isBoolean())
                throw new JsonParseException("Property 'show_breaking_overlay' must be a boolean!");
            showBreakingOverlay = json.get("show_breaking_overlay").getAsBoolean();
        }

        // Pane culling option
        Boolean paneCullingFix = null;
        if(json.has("pane_culling_fix")){
            if(!json.get("pane_culling_fix").isJsonPrimitive() || !json.getAsJsonPrimitive("pane_culling_fix").isBoolean())
                throw new JsonParseException("Property 'pane_culling_fix' must be a boolean!");
            paneCullingFix = json.get("pane_culling_fix").getAsBoolean();
        }

        if(models.isEmpty() && paneCullingFix != Boolean.TRUE)
            return;

        // Put the properties into the map
        for(ModelResourceLocation target : targets){
            Properties properties = this.models.computeIfAbsent(target, t -> new Properties());
            properties.appendModels.addAll(models);
            if(showBreakingOverlay != null)
                properties.showBreakingOverlay = showBreakingOverlay;
            if(paneCullingFix != null)
                properties.paneCullingFix = paneCullingFix;
        }
    }

    private Stream<IBlockState> parseTarget(JsonObject json){
        // Block
        if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
            throw new JsonParseException("Target must have string property 'block'!");
        if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
            throw new JsonParseException("Target property 'block' must be a valid identifier, not '" + json.get("block").getAsString() + "'!!");
        ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
        Block block = ForgeRegistries.BLOCKS.getValue(identifier);
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
            IProperty<?> property = block.getBlockState().getProperty(entry.getKey());
            if(property == null)
                throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
            // Parse the values
            ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
            if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                Optional<?> value = property.parseValue(entry.getValue().getAsString()).toJavaUtil();
                if(!value.isPresent())
                    throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                builder.add(value.get());
            }else if(entry.getValue().isJsonArray()){
                if(entry.getValue().getAsJsonArray().size() == 0)
                    throw new JsonParseException("Valid values for property '" + property.getName() + "' cannot be empty!");
                for(JsonElement element : entry.getValue().getAsJsonArray()){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                        throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
                    Optional<?> value = property.parseValue(element.getAsString()).toJavaUtil();
                    if(!value.isPresent())
                        throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }
            }else
                throw new JsonParseException("Property '" + entry.getKey() + "' must be a string or an array of strings!");
            properties.put(property, builder.build());
        }

        // Find all matching states
        Stream<IBlockState> states = Stream.of(block.getBlockState().getBaseState());
        for(IProperty<?> property : block.getBlockState().getProperties()){
            if(properties.containsKey(property)){
                Set<?> values = properties.get(property);
                states = states.flatMap(state -> values.stream().map(value -> stateWithValue(state, property, value)));
            }else
                states = states.flatMap(state -> property.getAllowedValues().stream().map(value -> stateWithValue(state, property, value)));
        }
        return states;
    }

    private static <T extends Comparable<T>> IBlockState stateWithValue(IBlockState state, IProperty<?> property, Object value){
        //noinspection unchecked
        return state.withProperty((IProperty<T>)property, (T)value);
    }

    /**
     * Copies the listResources behaviour from 1.14+
     */
    public static Collection<ResourceLocation> listResources(IResourceManager resourceManager, String folder, Predicate<String> predicate){
        if(resourceManager instanceof SimpleReloadableResourceManager){
            Set<ResourceLocation> resources = Sets.newHashSet();

            for(FallbackResourceManager domainManager : ((SimpleReloadableResourceManager)resourceManager).domainResourceManagers.values()){
                resources.addAll(listResources(domainManager, folder, predicate));
            }

            List<ResourceLocation> list = Lists.newArrayList(resources);
            Collections.sort(list);
            return list;
        }
        if(resourceManager instanceof FallbackResourceManager){
            List<ResourceLocation> resources = Lists.newArrayList();

            for(IResourcePack pack : ((FallbackResourceManager)resourceManager).resourcePacks){
                if(pack instanceof ResourcePackExtension)
                    resources.addAll(((ResourcePackExtension)pack).fusionGetResources(folder, Integer.MAX_VALUE, predicate));
            }

            Collections.sort(resources);
            return resources;
        }
        return Collections.emptyList();
    }

    private static class Properties {
        final List<ResourceLocation> appendModels = new ArrayList<>();
        boolean paneCullingFix;
        boolean showBreakingOverlay = true;
    }
}

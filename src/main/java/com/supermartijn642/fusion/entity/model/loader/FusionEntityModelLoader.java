package com.supermartijn642.fusion.entity.model.loader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created 17/09/2024 by SuperMartijn642
 */
public class FusionEntityModelLoader {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final List<Pair<String,EntityModelLoader>> LOADERS = List.of(
        Pair.of(".geo.json", new BedrockEntityModelLoader()),
        Pair.of(".jem", new OptifineEntityModelLoader())
    );

    public static Map<ResourceLocation,ModelPart> MODELS = Map.of();

    public static ResourceLocation locationForLayer(ModelLayerLocation layer){
        String layerFormatted = layer.getLayer().toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9/._-]","");
        return layer.getModel().withSuffix("/" + layerFormatted);
    }

    public static void loadModels(List<ResourceLocation> identifiers, ResourceManager resourceManager){
        // Find the resource packs order
        Map<PackResources,Integer> packOrder = Collections.emptyMap();
        if(resourceManager instanceof MultiPackResourceManager || (resourceManager instanceof ReloadableResourceManager && ((ReloadableResourceManager)resourceManager).resources instanceof MultiPackResourceManager)){
            List<PackResources> packs = resourceManager instanceof ReloadableResourceManager ? ((MultiPackResourceManager)((ReloadableResourceManager)resourceManager).resources).packs : ((MultiPackResourceManager)resourceManager).packs;
            packOrder = new HashMap<>(packs.size());
            for(int index = 0; index < packs.size(); index++)
                packOrder.put(packs.get(index), index);
        }

        // Load the resources
        ImmutableMap.Builder<ResourceLocation,ModelPart> models = ImmutableMap.builder();
        for(ResourceLocation identifier : identifiers){
            // Find the resource with the highest pack index
            Resource resource = null;
            EntityModelLoader modelLoader = null;
            ResourceLocation location = null;
            int packIndex = -1;
            for(Pair<String,EntityModelLoader> loader : LOADERS){
                ResourceLocation l = new ResourceLocation(
                    identifier.getNamespace(),
                    "fusion/entity_models/" + identifier.getPath() + loader.left()
                );
                Optional<Resource> optional = resourceManager.getResource(l);
                if(optional.isPresent()){
                    Resource r = optional.get();
                    int index = packOrder.get(r.source());
                    if(index > packIndex){
                        resource = r;
                        modelLoader = loader.right();
                        location = l;
                        packIndex = index;
                    }
                }
            }
            // If a resource was found for the identifier, load it
            if(resource != null){
                ModelPart model = loadModel(location, resource, modelLoader);
                if(model != null)
                    models.put(identifier, model);
            }
        }
        MODELS = models.build();
    }

    private static ModelPart loadModel(ResourceLocation location, Resource resource, EntityModelLoader loader){
        // Parse the file as JSON
        JsonObject json = new JsonObject();
        try(Reader input = resource.openAsReader()){
            json = GSON.fromJson(input, JsonObject.class);
        }catch(IOException e){
            //noinspection resource
            throw new RuntimeException("Failed to read resource for '" + location + "' from pack '" + resource.source().location().title(), e);
        }catch(JsonSyntaxException e){
            FusionClient.LOGGER.error("Failed to parse json for '{}' from pack '{}': {}", location, resource.source().location().title(), e.getMessage());
        }
        if(json == null)
            return null;

        // Load the model
        try{
            return loader.loadModel(json);
        }catch(JsonParseException e){
            LoggingHelper.logUserError(e, "Failed to load entity model for '%s' from pack '%s':", location, resource.source().location().title().getString());
        }
        return null;
    }
}

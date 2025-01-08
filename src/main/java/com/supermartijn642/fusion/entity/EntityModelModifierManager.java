package com.supermartijn642.fusion.entity;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.entity.model.EntityLayerProperties;
import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.entity.model.ModelTransformer;
import com.supermartijn642.fusion.entity.model.loader.FusionEntityModelLoader;
import com.supermartijn642.fusion.entity.model.predicates.EntityModelPredicate;
import com.supermartijn642.fusion.extensions.EntityRendererExtension;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public class EntityModelModifierManager {

    private static final ThreadLocal<Boolean> trackingBakedModels = ThreadLocal.withInitial(() -> false);
    private static final Set<ModelLayerLocation> currentModifierLayers = new HashSet<>();
    private static final Map<ModelLayerLocation,FusionModelPart> overwrittenModelParts = new HashMap<>();
    private static int currentLayerIndex = 0;

    private static final Map<EntityType<?>,EntityModelModifier> MODEL_PROPERTIES = new HashMap<>();
    public static int reloadCounter = 0;

    public static void bakeModels(Map<ModelLayerLocation,LayerDefinition> originalModels){
        MODEL_PROPERTIES.clear();
        reloadCounter++;

        // Put all vanilla models into a map to allow referencing them
        Map<ResourceLocation,Supplier<ModelPart>> vanillaModels = new HashMap<>();
        originalModels.forEach((layer, layerDefinition) -> vanillaModels.put(FusionEntityModelLoader.locationForLayer(layer), layerDefinition::bakeRoot));

        // Get the raw modifier info from the reload listener
        Map<EntityType<?>,EntityModelModifierReloadListener.Modifier> modifiers = EntityModelModifierReloadListener.getModifiers();
        // Handle each model modifier
        for(Map.Entry<EntityType<?>,EntityModelModifierReloadListener.Modifier> entry : modifiers.entrySet()){
            EntityType<?> entity = entry.getKey();
            EntityModelModifierReloadListener.Modifier modifier = entry.getValue();
            // Bake the layers
            ImmutableMap.Builder<ModelLayerLocation,EntityLayerProperties> layers = ImmutableMap.builder();
            for(Map.Entry<ModelLayerLocation,EntityModelModifierReloadListener.Layer> layerEntry : modifier.layers.entrySet()){
                ModelLayerLocation layerIdentifier = layerEntry.getKey();
                EntityModelModifierReloadListener.Layer rawLayer = layerEntry.getValue();
                Set<ResourceLocation> missingModels = new HashSet<>();
                // Get the default models and textures
                List<EntityLayerProperties.ModelOption> defaultModels = rawLayer.defaultModel.model == null ?
                    bakeModelOptions(layerIdentifier, List.of(new EntityModelModifierReloadListener.ModelOption(Either.left(FusionEntityModelLoader.locationForLayer(layerIdentifier)), null, null, null, null, null, null, null, null, 1)), null, rawLayer.defaultModel, vanillaModels, missingModels) :
                    bakeModelOptions(layerIdentifier, List.of(rawLayer.defaultModel), null, rawLayer.defaultModel, vanillaModels, missingModels);
                if(defaultModels == null)
                    continue;
                // Bake the conditional models
                List<Pair<EntityModelPredicate,List<EntityLayerProperties.ModelOption>>> conditionals = new ArrayList<>();
                for(Pair<EntityModelPredicate,EntityModelModifierReloadListener.ModelOption> conditional : rawLayer.conditionals){
                    EntityModelPredicate predicate = conditional.left();
                    EntityModelModifierReloadListener.ModelOption rawModel = conditional.right();
                    List<EntityLayerProperties.ModelOption> options = bakeModelOptions(layerIdentifier, List.of(rawModel), defaultModels, rawLayer.defaultModel, vanillaModels, missingModels);
                    if(options == null)
                        return;
                    conditionals.add(Pair.of(predicate, options));
                }
                // Finally, add the layer
                layers.put(layerIdentifier, new EntityLayerProperties(
                    layerIdentifier,
                    defaultModels,
                    conditionals
                ));
            }
            // Create the model properties
            MODEL_PROPERTIES.put(entity, new EntityModelModifier(layers.build()));
        }
    }

    private static List<EntityLayerProperties.ModelOption> bakeModelOptions(ModelLayerLocation layer, List<EntityModelModifierReloadListener.ModelOption> rawOptions, List<EntityLayerProperties.ModelOption> defaultModels, EntityModelModifierReloadListener.ModelOption defaults, Map<ResourceLocation,Supplier<ModelPart>> vanillaModels, Set<ResourceLocation> missingModels){
        List<EntityLayerProperties.ModelOption> options = new ArrayList<>(rawOptions.size());
        for(EntityModelModifierReloadListener.ModelOption option : rawOptions){
            if(option.model == null || option.model.isRight()){
                List<EntityLayerProperties.ModelOption> models;
                if(option.model == null)
                    models = defaultModels;
                else{
                    models = bakeModelOptions(
                        layer,
                        option.model.right(),
                        defaultModels,
                        defaults,
                        vanillaModels,
                        missingModels
                    );
                    if(models == null)
                        return null;
                }
                if(models == null){
                    FusionClient.LOGGER.error("No model defined for an entry in layer '{}'", layer);
                    return null;
                }
                double totalWeight = models.stream().mapToDouble(EntityLayerProperties.ModelOption::weight).sum();
                for(EntityLayerProperties.ModelOption defaultModel : models){
                    List<ResourceLocation> textures = option.textures == null ? defaultModel.textures() == null ? defaults.textures : defaultModel.textures() : option.textures;
                    Float scale = option.scale == null ? defaultModel.scaling() == null ? defaults.scale : defaultModel.scaling() : option.scale;
                    options.add(new EntityLayerProperties.ModelOption(defaultModel.model(), defaultModel.isVanillaModel(), textures, defaultModel.weight() / totalWeight * option.weight, scale));
                }
            }else{
                Pair<ModelPart,Boolean> baked = bakeModel(layer, option.model.left(), vanillaModels, missingModels);
                if(baked == null)
                    return null;
                ModelPart model = baked.left();
                float offset = option.offsetX == null ? defaults.offsetX == null ? 0 : defaults.offsetX : option.offsetX;
                if(offset != 0)
                    model = ModelTransformer.translateX(model, offset);
                offset = option.offsetY == null ? defaults.offsetY == null ? 0 : defaults.offsetY : option.offsetY;
                if(offset != 0)
                    model = ModelTransformer.translateY(model, offset);
                offset = option.offsetZ == null ? defaults.offsetZ == null ? 0 : defaults.offsetZ : option.offsetZ;
                if(offset != 0)
                    model = ModelTransformer.translateZ(model, offset);
                if(option.flipX == null ? defaults.flipX == Boolean.TRUE : option.flipX)
                    model = ModelTransformer.flipX(model);
                if(option.flipY == null ? defaults.flipY == Boolean.TRUE : option.flipY)
                    model = ModelTransformer.flipY(model);
                if(option.flipZ == null ? defaults.flipZ == Boolean.TRUE : option.flipZ)
                    model = ModelTransformer.flipZ(model);
                List<ResourceLocation> textures = option.textures == null ? defaults.textures : option.textures;
                Float scale = option.scale == null ? defaults.scale : option.scale;
                options.add(new EntityLayerProperties.ModelOption(model, baked.right(), textures, option.weight, scale));
            }
        }
        // Scale the weights so they add up to 1
        double totalWeight = options.stream().mapToDouble(EntityLayerProperties.ModelOption::weight).sum();
        if(totalWeight != 1){
            for(int i = 0; i < options.size(); i++){
                EntityLayerProperties.ModelOption option = options.get(i);
                options.set(i, new EntityLayerProperties.ModelOption(option.model(), option.isVanillaModel(), option.textures(), option.weight() / totalWeight, option.scaling()));
            }
        }
        // Sort the options by weight, so when picking random option, it is most likely to be near the front of the list
        options.sort(Comparator.comparingDouble(EntityLayerProperties.ModelOption::weight).reversed());
        return options;
    }

    private static Pair<ModelPart,Boolean> bakeModel(ModelLayerLocation layer, ResourceLocation modelLocation, Map<ResourceLocation,Supplier<ModelPart>> vanillaModels, Set<ResourceLocation> missingModels){
        ModelPart model = FusionEntityModelLoader.MODELS.get(modelLocation);
        boolean isVanillaModel = false;
        if(model == null){
            Supplier<ModelPart> vanillaSupplier = vanillaModels.get(modelLocation);
            if(vanillaSupplier != null){
                model = vanillaSupplier.get();
                isVanillaModel = true;
            }
        }
        if(model == null){
            if(!missingModels.add(modelLocation))
                FusionClient.LOGGER.error("Missing model '{}' for layer '{}'!", modelLocation, layer);
            return null;
        }
        return Pair.of(model, isVanillaModel);
    }

    public static FusionModelPart handleModelBake(ModelLayerLocation location, ModelPart original){
        if(!trackingBakedModels.get())
            return null;
        if(!currentModifierLayers.contains(location) && !FusionEntityModelLoader.MODELS.containsKey(FusionEntityModelLoader.locationForLayer(location)))
            return null;
        FusionModelPart fusionModelPart = new FusionModelPart(currentLayerIndex++, original);
        overwrittenModelParts.put(location, fusionModelPart);
        return fusionModelPart;
    }

    public static EntityRenderer<?,?> handleRendererCreation(EntityType<?> entityType, EntityRendererProvider<?> rendererProvider, EntityRendererProvider.Context context){
        // Gather the layers which should be overwritten from the model modifier
        EntityModelModifier properties = MODEL_PROPERTIES.get(entityType);
        if(properties != null)
            currentModifierLayers.addAll(properties.getLayers().keySet());

        // Create the original renderer
        trackingBakedModels.set(true);
        EntityRenderer<?,?> renderer;
        try{
            renderer = rendererProvider.create(context);
        }catch(Exception e){
            overwrittenModelParts.clear();
            throw e;
        }finally{
            trackingBakedModels.set(false);
            currentModifierLayers.clear();
            currentLayerIndex = 0;
        }

        // Report layers defined in the model modifier, but which did not get baked
        if(properties != null){
            for(ModelLayerLocation location : properties.getLayers().keySet()){
                if(!overwrittenModelParts.containsKey(location))
                    FusionClient.LOGGER.warn("An entity model modifier for entity '{}' overwrites layer '{}', but no such layer was found!", BuiltInRegistries.ENTITY_TYPE.getKey(entityType), location);
            }
        }

        // Check if any of the models baked during renderer creation had Fusion overwrites
        if(!overwrittenModelParts.isEmpty()){
            for(Map.Entry<ModelLayerLocation,FusionModelPart> entry : overwrittenModelParts.entrySet()){
                ModelLayerLocation layer = entry.getKey();
                FusionModelPart fusionModelPart = entry.getValue();
                fusionModelPart.finish();

                // Find the properties for the layer
                EntityLayerProperties layerProperties;
                if(properties != null && properties.getLayers().containsKey(layer))
                    layerProperties = properties.getLayers().get(layer);
                else{
                    // If model was from a simple model file rather than a model modifier, just create some dummy properties
                    ModelPart model = FusionEntityModelLoader.MODELS.get(FusionEntityModelLoader.locationForLayer(layer));
                    layerProperties = new EntityLayerProperties(
                        layer,
                        List.of(new EntityLayerProperties.ModelOption(model, false, null, 1, null)),
                        List.of()
                    );
                }

                // Gather all models used in the layer
                Collection<ModelPart> models = new HashSet<>();
                layerProperties.gatherModels(models::add);
                // Validate the models have the required parts
                Set<String> missingParts = new HashSet<>();
                boolean hasMissingParts = false;
                for(ModelPart model : models){
                    fusionModelPart.validateModelHasImportantChildren(model, missingParts::add);
                    if(!missingParts.isEmpty()){
                        FusionClient.LOGGER.error("A model for layer '{}' on entity '{}' is missing required parts: {}!", layer, BuiltInRegistries.ENTITY_TYPE.getKey(entityType), missingParts.stream().map(s -> '\'' + s + '\'').collect(Collectors.joining(",")));
                        missingParts.clear();
                        hasMissingParts = true;
                    }
                }
                if(hasMissingParts)
                    fusionModelPart.setProperties(null, null);

                // Get vanilla properties for the layer
                VanillaModelLayerProperties vanillaProperties = VanillaModelLayerProperties.get(layer, renderer);
                layerProperties = layerProperties.transformed(vanillaProperties);

                // Set the properties for the fusion model part
                fusionModelPart.setProperties(layerProperties, vanillaProperties);
            }

            // Store all fusion model parts on the renderer
            ((EntityRendererExtension)renderer).setFusionModelParts(new ArrayList<>(overwrittenModelParts.values()));

            overwrittenModelParts.clear();
        }
        return renderer;
    }
}

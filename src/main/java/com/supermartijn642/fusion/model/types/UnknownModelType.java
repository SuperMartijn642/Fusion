package com.supermartijn642.fusion.model.types;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

    public static Map<String,Either<String,ModelMaterial>> convertTextureSlots(TextureSlots.Data textureSlots){
        if(textureSlots.values().isEmpty())
            return Map.of();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builderWithExpectedSize(textureSlots.values().size());
        for(Map.Entry<String,TextureSlots.SlotContents> entry : textureSlots.values().entrySet()){
            if(entry.getValue() instanceof TextureSlots.Reference(String key))
                builder.put(entry.getKey(), Either.left(key));
            else if(entry.getValue() instanceof TextureSlots.Value(Material material))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(material)));
        }
        return builder.build();
    }

    @Override
    public Collection<Identifier> getDependencies(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<Identifier,ModelInstance<?>>> getParents(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.ambientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(T data){
        return data.guiLight();
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        ItemTransforms transforms = data.transforms();
        if(transforms == null)
            return null;
        ItemTransform transform = type.isModded() ?
            transforms.moddedTransforms().get(type) :
            transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return convertTextureSlots(data.textureSlots());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        UnbakedGeometry geometry = data.geometry();
        return geometry == null ? null : ModelGeometry.of(geometry);
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public BlockStateModel bakeBlockStateModel(BlockStateModelBakingContext context, T data){
        // Bake geometry
        List<BlockStateModelPart> parts = new ArrayList<>();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelGeometry geometry = modelInstance.getGeometry();
            if(geometry == null)
                return ModelWalker.Result.proceed();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> findPropertyInStackAndParents(context, stack, m -> m.getMaterial(key), null),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + stack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = stack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + stack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = findPropertyInStackAndParents(context, stack, ModelInstance::getAmbientOcclusion, null);
            Boolean shade = findPropertyInStackAndParents(context, stack, ModelInstance::getShade, null);
            Boolean emissive = findPropertyInStackAndParents(context, stack, ModelInstance::getEmissive, null);
            if(ambientOcclusion != null || shade != null || emissive != null){
                quads = quads.mutateQuads((side, quad) -> {
                    if(ambientOcclusion != null)
                        quad.ambientOcclusion(ambientOcclusion);
                    if(shade != null)
                        quad.shade(shade);
                    if(emissive != null)
                        quad.emissive(emissive);
                    return true;
                });
            }
            // Resolve particle material
            ModelMaterial.Resolved particleMaterial = materialResolver.get("particle");
            if(particleMaterial.isMissing())
                context.pushWarning("Could not resolve 'particle' material for model stack (" + stack + ")!");
            // Create a new part
            parts.add(new SimpleModelWrapper(
                quads.toQuadCollection(),
                true, // Ambient occlusion is handled by quads themselves
                particleMaterial.toBakedMaterial()
            ));
            return ModelWalker.Result.endBranch();
        });

        // Find particle sprite
        ModelMaterial particleMaterial = context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelMaterial material = stack.findMaterialRecursive(
                "particle",
                l -> {}
            );
            return material == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(material);
        }).orElse(null);
        if(particleMaterial == null){
            context.pushWarning("Could not resolve 'particle' material!");
            particleMaterial = ModelMaterial.missing();
        }
        ModelMaterial.Resolved resolvedParticleMaterial = context.getMaterial(particleMaterial);

        // Calculate material flags
        int materialFlags = parts.stream().mapToInt(BlockStateModelPart::materialFlags).reduce(0, (a, b) -> a | b);

        // Create the model
        List<BlockStateModelPart> finalParts = List.copyOf(parts);
        return new BlockStateModel() {
            @Override
            public void collectParts(RandomSource random, List<BlockStateModelPart> output){
                output.addAll(finalParts);
            }

            @Override
            public Material.Baked particleMaterial(){
                return resolvedParticleMaterial.toBakedMaterial();
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return materialFlags;
            }
        };
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, T data){
        // Create a submodel for each geometry
        List<ItemModel> subModels = new ArrayList<>();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelGeometry geometry = modelInstance.getGeometry();
            if(geometry == null)
                return ModelWalker.Result.proceed();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> findPropertyInStackAndParents(context, stack, m -> m.getMaterial(key), null),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + stack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = stack.composeTransforms();
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + stack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = findPropertyInStackAndParents(context, stack, ModelInstance::getAmbientOcclusion, null);
            Boolean shade = findPropertyInStackAndParents(context, stack, ModelInstance::getShade, null);
            Boolean emissive = findPropertyInStackAndParents(context, stack, ModelInstance::getEmissive, null);
            if(ambientOcclusion != null || shade != null || emissive != null){
                quads = quads.mutateQuads((side, quad) -> {
                    if(ambientOcclusion != null)
                        quad.ambientOcclusion(ambientOcclusion);
                    if(shade != null)
                        quad.shade(shade);
                    if(emissive != null)
                        quad.emissive(emissive);
                    return true;
                });
            }
            // Resolve particle material
            ModelMaterial.Resolved particleMaterial = materialResolver.get("particle");
            if(particleMaterial.isMissing())
                context.pushWarning("Could not resolve 'particle' material for model stack (" + stack + ")!");
            // Resolve gui light
            UnbakedModel.GuiLight guiLight = stack.findGuiLight();
            if(guiLight == null)
                guiLight = UnbakedModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) ->
                findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ImmutableMap.Builder<ItemDisplayContext,ItemTransform> moddedTransforms = ImmutableMap.builder();
            for(ItemDisplayContext type : ItemDisplayContext.values()){
                if(type.isModded()){
                    ItemTransform transform = itemTransformResolver.apply(type, null);
                    if(transform != null)
                        moddedTransforms.put(type, transform);
                }
            }
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.ON_SHELF, ItemTransform.NO_TRANSFORM),
                moddedTransforms.build()
            );
            // Create the item model
            subModels.add(new CuboidItemModelWrapper(
                context.getTintSources(),
                quads.toQuadCollection(),
                new ModelRenderProperties(
                    guiLight.lightLikeBlock(),
                    particleMaterial.toBakedMaterial(),
                    itemTransforms
                ),
                context.getTransformation().matrix()
            ));
            return ModelWalker.Result.endBranch();
        });

        // Create one composite model of all the sub-models
        return new CompositeModel(subModels);
    }

    public static <T> T findPropertyInStackAndParents(BlockStateModelBakingContext context, ModelWalker.ModelStack currentStack, Function<ModelInstance<?>,T> property, T defaultValue){
        // First check the current stack
        for(ModelInstance<?> modelInstance : currentStack){
            T value = property.apply(modelInstance);
            if(value != null)
                return value;
        }
        // Check parents of the last model in the stack
        Optional<T> result = context.walkModelTree(
            currentStack.get(currentStack.size() - 1),
            (modelInstance, stack) -> {
                T value = property.apply(modelInstance);
                return value == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(value);
            }
        );
        return result.orElse(defaultValue);
    }

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }
}

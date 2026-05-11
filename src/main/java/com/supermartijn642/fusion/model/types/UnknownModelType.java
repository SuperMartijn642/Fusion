package com.supermartijn642.fusion.model.types;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

    public static final Map<Direction,List<BakedQuad>> EMPTY_CULLED_QUADS;

    static{
        ImmutableMap.Builder<Direction,List<BakedQuad>> builder = ImmutableMap.builder();
        for(Direction direction : Direction.values())
            builder.put(direction, List.of());
        EMPTY_CULLED_QUADS = builder.build();
    }

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
    public Collection<ResourceLocation> getDependencies(T data){
        return List.of();
    }

    @Override
    public List<Either<ResourceLocation,ModelInstance<?>>> getParents(T data){
        return List.of();
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.getAmbientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(T data){
        return data.getGuiLight();
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        ItemTransforms transforms = data.getTransforms();
        if(transforms == null)
            return null;
        ItemTransform transform = transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return convertTextureSlots(data.getTextureSlots());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        return ModelGeometry.of(data);
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
    public <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, T data){
        // Bake geometry
        CullableQuads.Builder allQuads = CullableQuads.builder();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            if(modelInstance.getGeometry() == null)
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
            CullableQuads quads = modelInstance.getGeometry().bake(transforms, materialResolver);
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
            // Add the quads
            allQuads.add(quads);
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
            particleMaterial = ModelMaterial.missingBlockAtlas();
        }
        TextureAtlasSprite resolvedParticleMaterial = context.getMaterial(particleMaterial);

        // Convert quads to baked quads
        CullableQuads finishedQuads = allQuads.build();
        List<BakedQuad> unculledBakedQuads = finishedQuads.get(null).stream().map(QuadAccess::toBakedQuad).toList();
        Map<Direction,List<BakedQuad>> culledBakedQuads = new EnumMap<>(Direction.class);
        for(Direction cullDirection : Direction.values())
            culledBakedQuads.put(cullDirection, finishedQuads.get(cullDirection).stream().map(QuadAccess::toBakedQuad).toList());

        // Create the model
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
                if(cullDirection == null)
                    return unculledBakedQuads;
                return culledBakedQuads.get(cullDirection);
            }

            @Override
            public TextureAtlasSprite getParticleIcon(){
                return resolvedParticleMaterial;
            }

            @Override
            public boolean useAmbientOcclusion(){
                return true; // Ambient occlusion is handled by quads themselves
            }

            @Override
            public boolean isGui3d(){
                return true; // Only relevant to items
            }

            @Override
            public boolean usesBlockLight(){
                return true; // Only relevant to items
            }

            @Override
            public ItemTransforms getTransforms(){
                return ItemTransforms.NO_TRANSFORMS; // Only relevant to items
            }
        };
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, T data){
        // Create a submodel for each geometry
        List<ItemModel> subModels = new ArrayList<>();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            if(modelInstance.getGeometry() == null)
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
            CullableQuads quads = modelInstance.getGeometry().bake(transforms, materialResolver);
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
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + stack + ")!");
            // Resolve gui light
            UnbakedModel.GuiLight guiLight = stack.findGuiLight();
            if(guiLight == null)
                guiLight = UnbakedModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) ->
                findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM)
            );
            // Create the item model
            List<BakedQuad> bakedQuads = quads.all().stream().map(QuadAccess::toBakedQuad).toList();
            subModels.add(new BlockModelWrapper(
                new SimpleBakedModel(
                    bakedQuads,
                    EMPTY_CULLED_QUADS,
                    true,
                    guiLight.lightLikeBlock(),
                    true,
                    particleSprite,
                    itemTransforms
                ),
                context.getTintSources()
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

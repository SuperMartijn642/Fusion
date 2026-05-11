package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType implements ModelType<ModelBlock> {

    public static final Map<EnumFacing,List<BakedQuad>> EMPTY_CULLED_QUADS;

    static{
        ImmutableMap.Builder<EnumFacing,List<BakedQuad>> builder = ImmutableMap.builder();
        for(EnumFacing direction : EnumFacing.values())
            builder.put(direction, Collections.emptyList());
        EMPTY_CULLED_QUADS = builder.build();
    }

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,String> materials){
        if(materials.isEmpty())
            return Collections.emptyMap();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builder();
        for(Map.Entry<String,String> entry : materials.entrySet()){
            if(IdentifierUtil.isValidIdentifier(entry.getValue()))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(new ResourceLocation(entry.getValue()))));
            else{
                String reference = entry.getValue();
                if(reference.startsWith("#"))
                    reference = reference.substring(1);
                builder.put(entry.getKey(), Either.left(reference));
            }
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,ModelInstance<?>>> getParents(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(ModelBlock data){
        return data.ambientOcclusion ? null : false;
    }

    @Override
    public @Nullable Boolean getIsGui3d(ModelBlock data){
        return data.isGui3d();
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, ModelBlock data){
        ItemTransformVec3f transform = data.cameraTransforms.getTransform(type);
        return transform == ItemTransformVec3f.DEFAULT ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(ModelBlock data){
        return convertMaterials(data.textures);
    }

    @Override
    public ModelGeometry getGeometry(ModelBlock data){
        return data.elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(ModelBlock data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(ModelBlock data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context, ModelBlock data){
        return Optional.empty();
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, ModelBlock data){
        // Bake geometry
        CullableQuads.Builder blockQuads = CullableQuads.builder();
        List<IBakedModel> itemModels = new ArrayList<>();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelGeometry geometry = modelInstance.getGeometry();
            if(geometry == null)
                return ModelWalker.Result.proceed();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getMaterial(key), null),
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
            Boolean shade = UnknownModelType.findPropertyInStackAndParents(context, stack, ModelInstance::getShade, null);
            Boolean emissive = UnknownModelType.findPropertyInStackAndParents(context, stack, ModelInstance::getEmissive, null);
            if(shade != null || emissive != null){
                quads = quads.mutateQuads((side, quad) -> {
                    if(shade != null)
                        quad.shade(shade);
                    if(emissive != null)
                        quad.emissive(emissive);
                    return true;
                });
            }
            // Add the block quads
            blockQuads.add(quads);
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + stack + ")!");
            // Resolve gui 3d
            Boolean isGui3d = stack.findIsGui3d();
            if(isGui3d == null)
                isGui3d = true;
            // Resolve item transforms
            BiFunction<ItemCameraTransforms.TransformType,ItemTransformVec3f,ItemTransformVec3f> itemTransformResolver = (type, fallback) ->
                UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ItemCameraTransforms itemTransforms = new ItemCameraTransforms(
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.DEFAULT)
            );
            // Create the item model
            List<BakedQuad> bakedQuads = quads.all().stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList());
            itemModels.add(new SimpleBakedModel(
                bakedQuads,
                EMPTY_CULLED_QUADS,
                true,
                isGui3d,
                particleSprite,
                itemTransforms,
                ItemOverrideList.NONE
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
        TextureAtlasSprite resolvedParticleMaterial = context.getMaterial(particleMaterial);
        // Find ambient occlusion
        boolean ambientOcclusion = context.walkModelTree(
            ModelInstance.of(this, data),
            (modelInstance, stack) -> {
                Boolean v = modelInstance.getAmbientOcclusion();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(true);

        // Convert quads to baked quads
        CullableQuads finishedQuads = blockQuads.build();
        List<BakedQuad> unculledBakedQuads = finishedQuads.get(null).stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList());
        Map<EnumFacing,List<BakedQuad>> culledBakedQuads = new EnumMap<>(EnumFacing.class);
        for(EnumFacing cullDirection : EnumFacing.values())
            culledBakedQuads.put(cullDirection, finishedQuads.get(cullDirection).stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList()));

        // Create the model
        IBakedModel firstItemModel = itemModels.isEmpty() ? null : itemModels.get(0);
        return new IBakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
                if(cullDirection == null)
                    return unculledBakedQuads;
                return culledBakedQuads.get(cullDirection);
            }

            @Override
            public TextureAtlasSprite getParticleTexture(){
                return resolvedParticleMaterial;
            }

            @Override
            public boolean isAmbientOcclusion(){
                return ambientOcclusion;
            }

            @Override
            public boolean isGui3d(){
                return firstItemModel != null && firstItemModel.isGui3d();
            }

            @Override
            public boolean isBuiltInRenderer(){
                return false;
            }

            @Override
            public ItemCameraTransforms getItemCameraTransforms(){
                return firstItemModel == null ? ItemCameraTransforms.DEFAULT : firstItemModel.getItemCameraTransforms();
            }

            @Override
            public ItemOverrideList getOverrides(){
                return ItemOverrideList.NONE;
            }
        };
    }

    @Override
    public ModelBlock deserialize(JsonObject json) throws JsonParseException{
        return ModelBlock.SERIALIZER.fromJson(json, ModelBlock.class);
    }

    @Override
    public JsonObject serialize(ModelBlock value){
        return (JsonObject)CuboidModelSerializer.GSON.toJsonTree(value);
    }
}

package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<BlockModel> {

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,com.mojang.datafixers.util.Either<RenderMaterial,String>> materials){
        if(materials.isEmpty())
            return Collections.emptyMap();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builder();
        for(Map.Entry<String,com.mojang.datafixers.util.Either<RenderMaterial,String>> entry : materials.entrySet()){
            entry.getValue()
                .ifLeft(material -> builder.put(entry.getKey(), Either.right(ModelMaterial.of(material))))
                .ifRight(reference -> builder.put(entry.getKey(), Either.left(reference)));
        }
        return builder.build();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(BlockModel data){
        return data.hasAmbientOcclusion;
    }

    @Override
    public BlockModel.@Nullable GuiLight getGuiLight(BlockModel data){
        return data.guiLight;
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, BlockModel data){
        ItemTransformVec3f transform = data.transforms.getTransform(type);
        return transform == ItemTransformVec3f.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(BlockModel data){
        return convertMaterials(data.textureMap);
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockPart> elements = data.getElements();
        return elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, BlockModel data){
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
            Boolean shade = findPropertyInStackAndParents(context, stack, UntypedModelInstance::getShade, null);
            Boolean emissive = findPropertyInStackAndParents(context, stack, UntypedModelInstance::getEmissive, null);
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
            // Resolve gui light
            BlockModel.GuiLight guiLight = stack.findGuiLight();
            if(guiLight == null)
                guiLight = BlockModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemCameraTransforms.TransformType,ItemTransformVec3f,ItemTransformVec3f> itemTransformResolver = (type, fallback) ->
                findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ItemCameraTransforms itemTransforms = new ItemCameraTransforms(
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.NO_TRANSFORM)
            );
            // Create the item model
            List<BakedQuad> bakedQuads = quads.all().stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList());
            itemModels.add(new SimpleBakedModel(
                bakedQuads,
                EMPTY_CULLED_QUADS,
                true,
                guiLight.lightLikeBlock(),
                geometry.isGui3d(),
                particleSprite,
                itemTransforms,
                ItemOverrideList.EMPTY
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
            particleMaterial = ModelMaterial.missingBlockAtlas();
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
        Map<Direction,List<BakedQuad>> culledBakedQuads = new EnumMap<>(Direction.class);
        for(Direction cullDirection : Direction.values())
            culledBakedQuads.put(cullDirection, finishedQuads.get(cullDirection).stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList()));

        // Create the model
        List<IBakedModel> finalItemModels = ImmutableList.copyOf(itemModels);
        IBakedModel firstItemModel = finalItemModels.isEmpty() ? null : itemModels.get(0);
        return new IBakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
                if(cullDirection == null)
                    return unculledBakedQuads;
                return culledBakedQuads.get(cullDirection);
            }

            @Override
            public List<Pair<IBakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
                // Get default render type
                RenderType defaultRenderType = Atlases.translucentItemSheet();
                if(stack.getItem() instanceof BlockItem && !ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(((BlockItem)stack.getItem()).getBlock().defaultBlockState(), RenderType.translucent()))
                    defaultRenderType = Atlases.cutoutBlockSheet();
                // Add models
                List<Pair<IBakedModel,RenderType>> models = new ArrayList<>(finalItemModels.size());
                for(IBakedModel model : finalItemModels)
                    models.add(new Pair<>(model, defaultRenderType));
                return models;
            }

            @Override
            public boolean isLayered(){
                return true;
            }

            @Override
            public TextureAtlasSprite getParticleIcon(){
                return resolvedParticleMaterial;
            }

            @Override
            public boolean useAmbientOcclusion(){
                return ambientOcclusion;
            }

            @Override
            public boolean isGui3d(){
                return firstItemModel != null && firstItemModel.isGui3d();
            }

            @Override
            public boolean usesBlockLight(){
                return firstItemModel == null || firstItemModel.usesBlockLight(); // Only relevant to items
            }

            @Override
            public boolean isCustomRenderer(){
                return false;
            }

            @Override
            public ItemCameraTransforms getTransforms(){
                return firstItemModel == null ? ItemCameraTransforms.NO_TRANSFORMS : firstItemModel.getTransforms();
            }

            @Override
            public ItemOverrideList getOverrides(){
                return ItemOverrideList.EMPTY;
            }
        };
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)CuboidModelSerializer.GSON.toJsonTree(value);
    }
}

package com.supermartijn642.fusion.model.types.cuboid;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<BlockModel> {

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,com.mojang.datafixers.util.Either<Material,String>> materials){
        if(materials.isEmpty())
            return Map.of();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builder();
        for(Map.Entry<String,com.mojang.datafixers.util.Either<Material,String>> entry : materials.entrySet()){
            entry.getValue()
                .ifLeft(material -> builder.put(entry.getKey(), Either.right(ModelMaterial.of(material))))
                .ifRight(reference -> builder.put(entry.getKey(), Either.left(reference)));
        }
        return builder.build();
    }

    @Override
    public List<Either<ResourceLocation,ModelInstance<?>>> getParents(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? List.of() : List.of(Either.left(parent));
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
    public @Nullable ItemTransform getItemTransform(ItemTransforms.TransformType type, BlockModel data){
        ItemTransform transform = data.transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(BlockModel data){
        return convertMaterials(data.textureMap);
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockElement> elements = data.getElements();
        return elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public BakedModel bakeModel(ModelBakingContext context, BlockModel data){
        // Bake geometry
        CullableQuads.Builder blockQuads = CullableQuads.builder();
        List<BakedModel> itemModels = new ArrayList<>();
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
            Boolean shade = findPropertyInStackAndParents(context, stack, ModelInstance::getShade, null);
            Boolean emissive = findPropertyInStackAndParents(context, stack, ModelInstance::getEmissive, null);
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
            BiFunction<ItemTransforms.TransformType,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) ->
                findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemTransforms.TransformType.FIXED, ItemTransform.NO_TRANSFORM)
            );
            // Create the item model
            List<BakedQuad> bakedQuads = quads.all().stream().map(QuadAccess::toBakedQuad).toList();
            itemModels.add(new SimpleBakedModel(
                bakedQuads,
                EMPTY_CULLED_QUADS,
                true,
                guiLight.lightLikeBlock(),
                geometry.isGui3d(),
                particleSprite,
                itemTransforms,
                ItemOverrides.EMPTY
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
        List<BakedQuad> unculledBakedQuads = finishedQuads.get(null).stream().map(QuadAccess::toBakedQuad).toList();
        Map<Direction,List<BakedQuad>> culledBakedQuads = new EnumMap<>(Direction.class);
        for(Direction cullDirection : Direction.values())
            culledBakedQuads.put(cullDirection, finishedQuads.get(cullDirection).stream().map(QuadAccess::toBakedQuad).toList());

        // Create the model
        List<BakedModel> finalItemModels = List.copyOf(itemModels);
        BakedModel firstItemModel = finalItemModels.isEmpty() ? null : itemModels.get(0);
        return new BakedModel() {
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
                if(cullDirection == null)
                    return unculledBakedQuads;
                return culledBakedQuads.get(cullDirection);
            }

            @Override
            public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
                // Get default render type
                RenderType defaultRenderType = Sheets.translucentItemSheet();
                if(stack.getItem() instanceof BlockItem && !ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(((BlockItem)stack.getItem()).getBlock().defaultBlockState(), RenderType.translucent()))
                    defaultRenderType = Sheets.cutoutBlockSheet();
                // Add models
                List<Pair<BakedModel,RenderType>> models = new ArrayList<>(finalItemModels.size());
                for(BakedModel model : finalItemModels)
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
            public ItemTransforms getTransforms(){
                return firstItemModel == null ? ItemTransforms.NO_TRANSFORMS : firstItemModel.getTransforms();
            }

            @Override
            public ItemOverrides getOverrides(){
                return ItemOverrides.EMPTY;
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

package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private static final ModelProperty<Long> SEED_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<IModelData[]> DATA_PROPERTY = new ModelProperty<>();

    private final IBakedModel original;
    private final List<IBakedModel> models;
    private final boolean showBreakingOverlay;
    private final boolean isOriginalSimpleModel;
    private final boolean hasNonSimpleModels;
    private final List<IBakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];

    public BlockModelModifierBakedModel(IBakedModel original, List<IBakedModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
        List<IBakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        Random random = new Random();
        for(IBakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class))
                nonSimpleModels.add(model);
            else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, random));
                for(Direction side : Direction.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, random));
            }
        }
        this.isOriginalSimpleModel = original.getClass().equals(SimpleBakedModel.class);
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : ImmutableList.copyOf(nonSimpleModels);
        this.quads = ImmutableList.copyOf(quads);
        for(Direction side : Direction.values())
            this.culledQuads[side.ordinal()] = ImmutableList.copyOf(culledQuads[side.ordinal()]);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData data){
        // Get model data properties
        Long seed = data.getData(SEED_PROPERTY);
        IModelData[] arr = data.getData(DATA_PROPERTY);
        // Check whether quads from simple models should be submitted
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean isDefaultRenderType = renderType == null || state == null || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel)
                return isDefaultRenderType ? this.original.getQuads(state, side, random, EmptyModelData.INSTANCE) : Collections.emptyList();
            if(ModelRenderTypeHelper.canRenderInLayer(this.original, state, RenderType.leash(), isDefaultRenderType)){
                IModelData subData = arr == null || arr[0] == null ? EmptyModelData.INSTANCE : arr[0];
                if(seed != null)
                    random.setSeed(seed);
                return this.original.getQuads(state, side, random, subData);
            }
            return Collections.emptyList();
        }
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return isDefaultRenderType ? side == null ? this.quads : this.culledQuads[side.ordinal()] : Collections.emptyList();
        // Start with quads from simple models
        List<BakedQuad> quads = isDefaultRenderType ? new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]) : new ArrayList<>();
        // Gather quads from complex models
        for(int i = 0; i < this.nonSimpleModels.size(); i++){
            IBakedModel model = this.nonSimpleModels.get(i);
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType)){
                IModelData subData = arr == null || arr[i] == null ? EmptyModelData.INSTANCE : arr[i];
                if(seed != null)
                    random.setSeed(seed);
                quads.addAll(model.getQuads(state, side, random, subData));
            }
        }
        return quads;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random){
        // Check whether quads from simple models should be submitted
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean isDefaultRenderType = renderType == null || state == null || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
                return this.original.getQuads(state, side, random, EmptyModelData.INSTANCE);
            return Collections.emptyList();
        }
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return isDefaultRenderType ? side == null ? this.quads : this.culledQuads[side.ordinal()] : Collections.emptyList();
        // Start with quads from simple models
        List<BakedQuad> quads = isDefaultRenderType ? new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]) : new ArrayList<>();
        // Gather quads from complex models
        for(IBakedModel model : this.nonSimpleModels){
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                quads.addAll(model.getQuads(state, side, random, EmptyModelData.INSTANCE));
        }
        return quads;
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return ModelRenderTypeHelper.canRenderInLayer(this.original, state, layer, isDefaultRenderType);
        // Check if any of the models can render in the layer
        for(IBakedModel model : this.models){
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                return true;
        }
        return false;
    }

    @Override
    public IModelData getModelData(ILightReader level, BlockPos pos, BlockState state, IModelData data){
        // If there's only simple models, no need for model data
        if(!this.hasNonSimpleModels)
            return EmptyModelData.INSTANCE;
        // Add seed
        ModelDataMap.Builder builder = new ModelDataMap.Builder()
            .withInitial(SEED_PROPERTY, state.getSeed(pos));
        // Gather model data for complex models
        IModelData[] arr = new IModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return builder.withInitial(DATA_PROPERTY, arr).build();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public boolean isAmbientOcclusion(BlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public boolean doesHandlePerspectives(){
        return this.original.doesHandlePerspectives();
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType transformType, MatrixStack poseStack){
        return this.original.handlePerspective(transformType, poseStack);
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data){
        if(this.isOriginalSimpleModel)
            return this.original.getParticleTexture(EmptyModelData.INSTANCE);
        // Get appropriate model data
        IModelData[] arr = data.getData(DATA_PROPERTY);
        IModelData subData = arr == null || arr[0] == null ? EmptyModelData.INSTANCE : arr[0];
        return this.original.getParticleTexture(subData);
    }
}

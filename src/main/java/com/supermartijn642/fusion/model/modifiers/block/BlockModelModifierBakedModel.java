package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final IBakedModel original;
    private final List<IBakedModel> models;
    private final boolean showBreakingOverlay;
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
        for(IBakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class))
                nonSimpleModels.add(model);
            else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, 42));
                for(EnumFacing side : EnumFacing.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, 42));
            }
        }
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : ImmutableList.copyOf(nonSimpleModels);
        this.quads = ImmutableList.copyOf(quads);
        for(EnumFacing side : EnumFacing.values())
            this.culledQuads[side.ordinal()] = ImmutableList.copyOf(culledQuads[side.ordinal()]);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long random){
        // Check whether quads from simple models should be submitted
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean isDefaultRenderType = renderType == null || state == null || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(ModelRenderTypeHelper.canRenderInLayer(this.original, state, renderType, isDefaultRenderType))
                return this.original.getQuads(state, side, random);
            return Collections.emptyList();
        }
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return isDefaultRenderType ? side == null ? this.quads : this.culledQuads[side.ordinal()] : Collections.emptyList();
        // Start with quads from simple models
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        // Gather quads from complex models
        for(IBakedModel model : this.nonSimpleModels){
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                quads.addAll(model.getQuads(state, side, random));
        }
        return quads;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.original.getItemCameraTransforms();
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer){
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
    public boolean isAmbientOcclusion(IBlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType){
        return this.original.handlePerspective(transformType);
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.original.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer(){
        return this.original.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.original.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }
}

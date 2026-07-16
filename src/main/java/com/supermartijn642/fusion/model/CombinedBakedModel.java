package com.supermartijn642.fusion.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public abstract class CombinedBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    public static IBakedModel of(List<IBakedModel> models){
        return new CombinedBakedModel() {
            @Override
            protected List<IBakedModel> getModels(){
                return models;
            }
        };
    }

    protected abstract List<IBakedModel> getModels();

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long seed){
        List<BakedQuad> quads = new ArrayList<>();
        for(IBakedModel model : this.getModels())
            quads.addAll(model.getQuads(state, side, seed));
        return quads;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer renderType){
        // Check whether the render type is a default one for the state
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        // Check models
        for(IBakedModel model : this.getModels()){
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                return true;
        }
        return false;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.getModels().get(0).isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.getModels().get(0).isGui3d();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.getModels().get(0).getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.getModels().get(0).getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.getModels().get(0).getOverrides();
    }

    @Override
    public boolean isBuiltInRenderer(){
        return this.getModels().get(0).isBuiltInRenderer();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state){
        return this.getModels().get(0).isAmbientOcclusion(state);
    }

    @Override
    public Pair<? extends IBakedModel,Matrix4f> handlePerspective(ItemCameraTransforms.TransformType displayContext){
        return this.getModels().get(0).handlePerspective(displayContext);
    }
}

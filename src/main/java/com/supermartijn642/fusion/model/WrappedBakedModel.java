package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    protected final IBakedModel original;

    public WrappedBakedModel(IBakedModel original){
        this.original = original;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        return Collections.emptyList();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.original.getItemCameraTransforms();
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

    @Override
    public Collection<BlockRenderLayer> getBlockRenderTypes(){
        return this.original instanceof CustomRenderTypeBakedModel ? ((CustomRenderTypeBakedModel)this.original).getBlockRenderTypes() : Collections.emptyList();
    }
}

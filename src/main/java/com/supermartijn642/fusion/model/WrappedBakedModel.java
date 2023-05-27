package com.supermartijn642.fusion.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements IBakedModel {

    protected final IBakedModel original;

    public WrappedBakedModel(IBakedModel original){
        this.original = original;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand){
        return this.original.getQuads(state, side, rand);
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
    public boolean isAmbientOcclusion(IBlockState state){
        return this.original.isAmbientOcclusion(state);
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
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.original.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }
}

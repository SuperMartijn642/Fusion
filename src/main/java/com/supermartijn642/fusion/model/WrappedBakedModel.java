package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements IBakedModel, IDynamicBakedModel {

    protected final IBakedModel original;

    public WrappedBakedModel(IBakedModel original){
        this.original = original;
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData){
        return this.original.getQuads(state, side, rand, extraData);
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
    public boolean isAmbientOcclusion(BlockState state){
        return this.original.isAmbientOcclusion(state);
    }

    @Override
    public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data){
        return this.original.getParticleTexture(data);
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
    public ItemCameraTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.original.getOverrides();
    }
}

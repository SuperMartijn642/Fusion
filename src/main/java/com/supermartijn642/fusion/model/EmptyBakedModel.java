package com.supermartijn642.fusion.model;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created 20/08/2026 by SuperMartijn642
 */
public class EmptyBakedModel implements IBakedModel {

    private final TextureAtlasSprite particleSprite;

    public EmptyBakedModel(TextureAtlasSprite particleSprite){
        this.particleSprite = particleSprite;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, Random randomSource){
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return true;
    }

    @Override
    public boolean isGui3d(){
        return true;
    }

    @Override
    public boolean usesBlockLight(){
        return true;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.EMPTY;
    }
}

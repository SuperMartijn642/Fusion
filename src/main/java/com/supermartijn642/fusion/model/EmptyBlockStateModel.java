package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Created 20/08/2026 by SuperMartijn642
 */
public class EmptyBlockStateModel implements BlockStateModel {

    private final TextureAtlasSprite particleSprite;

    public EmptyBlockStateModel(TextureAtlasSprite particleSprite){
        this.particleSprite = particleSprite;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }
}

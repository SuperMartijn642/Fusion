package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }
}

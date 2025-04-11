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
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements BlockStateModel {

    protected final BlockStateModel original;

    public WrappedBakedModel(BlockStateModel original){
        this.original = original;
    }

    @Override
    public List<BlockModelPart> collectParts(RandomSource random){
        return this.original.collectParts(random);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.original.collectParts(random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.original.particleIcon();
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this.original.createGeometryKey(level, pos, state, random);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts){
        this.original.collectParts(level, pos, state, random, parts);
    }

    @Override
    public List<BlockModelPart> collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this.original.collectParts(level, pos, state, random);
    }

    @Override
    public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.original.particleIcon(level, pos, state);
    }
}

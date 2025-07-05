package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return this.original.getModelData(level, pos, state, modelData);
    }

    @Override
    public TextureAtlasSprite particleIcon(@NotNull ModelData data){
        return this.original.particleIcon(data);
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data){
        return this.original.getRenderTypes(state, random, data);
    }

    @Override
    public List<BlockModelPart> collectParts(RandomSource random, ModelData data, @Nullable ChunkSectionLayer renderType){
        return this.original.collectParts(random, data, renderType);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> dest, ModelData data, @Nullable ChunkSectionLayer renderType){
        this.original.collectParts(random, dest, data, renderType);
    }
}

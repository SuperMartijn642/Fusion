package com.supermartijn642.fusion.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class WrappedBakedModel implements BlockStateModel {

    protected final BlockStateModel original;

    public WrappedBakedModel(BlockStateModel original){
        this.original = original;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        this.original.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random){
        return this.original.createGeometryKey(blockView, pos, state, random);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> list){
        this.original.collectParts(random, list);
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.original.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.original.particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.original.materialFlags();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this.original.materialFlags(level, pos, state, random);
    }
}

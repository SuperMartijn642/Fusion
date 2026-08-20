package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created 20/08/2026 by SuperMartijn642
 */
public class EmptyBlockStateModel implements BlockStateModel {

    private final Material.Baked particleMaterial;

    public EmptyBlockStateModel(Material.Baked particleMaterial){
        this.particleMaterial = particleMaterial;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output){
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this;
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return 0;
    }
}

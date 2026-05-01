package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

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
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        this.original.collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.original.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.original.materialFlags();
    }

    @Override
    public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag){
        return this.original.hasMaterialFlag(flag);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return this.original.getModelData(level, pos, state, modelData);
    }

    @Override
    public Material.Baked particleMaterial(@NotNull ModelData data){
        return this.original.particleMaterial(data);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> dest, ModelData data){
        this.original.collectParts(random, dest, data);
    }
}

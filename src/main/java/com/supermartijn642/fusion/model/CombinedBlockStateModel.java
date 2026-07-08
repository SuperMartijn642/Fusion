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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public abstract class CombinedBlockStateModel implements BlockStateModel {

    public static BlockStateModel of(List<BlockStateModel> models){
        return new CombinedBlockStateModel() {
            @Override
            protected List<BlockStateModel> getModels(){
                return models;
            }
        };
    }

    protected abstract List<BlockStateModel> getModels();

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            model.collectParts(random, parts);
        }
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.getModels().getFirst().particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        int materialFlags = 0;
        for(BlockStateModel model : this.getModels())
            materialFlags |= model.materialFlags();
        return materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag){
        for(BlockStateModel model : this.getModels()){
            if(model.hasMaterialFlag(flag))
                return true;
        }
        return false;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            model.emitQuads(emitter, level, pos, state, random, cullTest);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<BlockStateModel> models = this.getModels();
        List<Object> keys = new ArrayList<>(models.size());
        long seed = random.nextLong();
        for(BlockStateModel model : models){
            random.setSeed(seed);
            Object key = model.createGeometryKey(level, pos, state, random);
            if(key == null)
                return null;
            keys.add(key);
        }
        return keys;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.getModels().getFirst().particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        int materialFlags = 0;
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            materialFlags |= model.materialFlags(level, pos, state, random);
        }
        return materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, @BakedQuad.MaterialFlags int flag){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            if(model.hasMaterialFlag(level, pos, state, random, flag))
                return true;
        }
        return false;
    }
}

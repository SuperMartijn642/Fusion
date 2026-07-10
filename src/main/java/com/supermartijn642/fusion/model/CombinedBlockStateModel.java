package com.supermartijn642.fusion.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
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
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            model.collectParts(random, parts);
        }
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.getModels().getFirst().particleIcon();
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
    public TextureAtlasSprite particleSprite(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.getModels().getFirst().particleSprite(level, pos, state);
    }
}

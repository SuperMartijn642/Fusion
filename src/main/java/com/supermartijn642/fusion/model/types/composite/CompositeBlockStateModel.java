package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBlockStateModel implements BlockStateModel {

    private final BlockStateModel defaultModel;
    private final List<ConditionalList> entries;

    public CompositeBlockStateModel(BlockStateModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model != null){
                random.setSeed(seed);
                model.emitQuads(emitter, level, pos, state, random, cullTest);
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(null, null, null);
            if(model != null){
                random.setSeed(seed);
                model.collectParts(random, parts);
            }
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<Object> keys = new ArrayList<>();
        keys.add(this);
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate != null){
                    if(!entry.predicate.testForBlockState(level, pos, state)){
                        keys.add(false);
                        continue;
                    }
                    keys.add(true);
                }
                random.setSeed(seed);
                Object key = entry.model.createGeometryKey(level, pos, state, random);
                if(key == null)
                    return null;
                keys.add(key);
            }
        }
        return keys;
    }

    @Override
    public TextureAtlasSprite particleSprite(BlockAndTintGetter level, BlockPos pos, BlockState state){
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model != null)
                return model.particleSprite(level, pos, state);
        }
        return this.particleIcon();
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.defaultModel.particleIcon();
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        BlockStateModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BlockStateModel model, ModelPredicate predicate) {
    }
}
